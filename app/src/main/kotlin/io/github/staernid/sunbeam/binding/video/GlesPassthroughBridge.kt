@file:Suppress(
    "TooManyFunctions",
    "CyclomaticComplexMethod",
    "MagicNumber",
    "NestedBlockDepth",
    "LongMethod",
    "TooGenericExceptionCaught",
    "SpreadOperator",
    "ReturnCount",
    "MaxLineLength",
)

package io.github.staernid.sunbeam.binding.video

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES31
import android.view.Surface
import io.github.staernid.sunbeam.LimeLog
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * High-performance video rendering stage bridging the MediaCodec decoder and the display surface.
 *
 * Implements a Two-Pass OpenGL ES 3.1 pipeline to support standard sampler2D shaders (such as SGSR1):
 * - Pass 1: Blits the hardware-decoded OES texture (samplerExternalOES) into a standard 2D texture via an FBO.
 * - Pass 2: Applies the custom post-processing shader (SGSR1 edge-direction upscaler) onto the display surface.
 *
 * Designed with:
 * - Lifecycle-safe EGL and surface recreation management.
 * - 10-bit / HDR wide color gamut support (BT.2020 PQ / GL_RGB10_A2).
 * - Monitored render latency diagnostics.
 * - Strictly zero allocations in per-frame code paths.
 */
class GlesPassthroughBridge(private val context: Context) : SurfaceTexture.OnFrameAvailableListener {
    companion object {
        private const val TAG = "GlesBridge"

        // EGL Colorspace constants for HDR (BT2020 PQ / HLG)
        private const val EGL_GL_COLORSPACE_KHR = 0x309D
        private const val EGL_GL_COLORSPACE_BT2020_PQ_EXT = 0x3340

        // GL_RGB10_A2 and GL_UNSIGNED_INT_2_10_10_10_REV for 10-bit FBO
        private const val GL_RGB10_A2 = 0x8059
        private const val GL_UNSIGNED_INT_2_10_10_10_REV = 0x8368

        // Full-screen quad vertices [PosX, PosY, TexU, TexV]
        private val VERTICES = floatArrayOf(
            -1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
        )

        private const val BLIT_VERTEX_SHADER =
            "#version 310 es\n" +
                "uniform mat4 uSTMatrix;\n" +
                "in vec4 aPosition;\n" +
                "in vec4 aTexCoord;\n" +
                "out vec2 vTexCoord;\n" +
                "void main() {\n" +
                "  gl_Position = aPosition;\n" +
                "  vTexCoord = (uSTMatrix * aTexCoord).xy;\n" +
                "}\n"

        private const val BLIT_FRAGMENT_SHADER =
            "#version 310 es\n" +
                "#extension GL_OES_EGL_image_external_essl3 : require\n" +
                "precision mediump float;\n" +
                "in vec2 vTexCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "out vec4 fragColor;\n" +
                "void main() {\n" +
                "  fragColor = texture(sTexture, vTexCoord);\n" +
                "}\n"

        private const val FALLBACK_POST_FRAGMENT_SHADER =
            "#version 310 es\n" +
                "precision mediump float;\n" +
                "in vec2 v_texCoord;\n" +
                "in vec2 v_imgCoord;\n" +
                "uniform sampler2D sTexture;\n" +
                "out vec4 fragColor;\n" +
                "void main() {\n" +
                "  fragColor = texture(sTexture, v_texCoord);\n" +
                "}\n"
    }

    // EGL State
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var eglConfig: EGLConfig? = null

    // Shader Programs
    private var blitProgram = 0
    private var postProgram = 0

    // Textures and Framebuffer
    private var oesTextureId = 0
    private var fboTextureId = 0
    private var fboId = 0

    // Decoder Surface management
    private var surfaceTexture: SurfaceTexture? = null
    private var decoderSurface: Surface? = null
    private val transformMatrix = FloatArray(16)
    private val vertexBuffer: FloatBuffer

    // Pass 1 Handles
    private var blitUSTMatrixHandle = 0
    private var blitAPositionHandle = 0
    private var blitATexCoordHandle = 0
    private var blitSTextureHandle = 0

    // Pass 2 Handles
    private var postAPositionHandle = 0
    private var postATexCoordHandle = 0
    private var postSTextureHandle = 0

    // Synchronization
    private val frameAvailableLock = Any()
    private var frameAvailable = false

    @Volatile
    private var isReleased = false

    // Dimensions
    private var streamWidth = 0
    private var streamHeight = 0
    private var displayWidth = 0
    private var displayHeight = 0
    private var is10BitMode = false

    // Latency Diagnostics (zero-allocation)
    var lastRenderLatencyNs: Long = 0L
        private set
    var averageRenderLatencyUs: Long = 0L
        private set

    init {
        vertexBuffer = ByteBuffer.allocateDirect(VERTICES.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(VERTICES).position(0)
    }

    /**
     * Initializes the EGL display, contexts, shaders, and textures for rendering.
     * Default overload (SDR 8-bit mode).
     */
    fun initialize(
        targetSurface: Surface,
        streamWidth: Int,
        streamHeight: Int,
    ) {
        initialize(targetSurface, streamWidth, streamHeight, is10Bit = false)
    }

    /**
     * Initializes the EGL display, contexts, shaders, and textures for rendering with optional 10-bit HDR support.
     */
    fun initialize(
        targetSurface: Surface,
        streamWidth: Int,
        streamHeight: Int,
        is10Bit: Boolean,
    ) {
        this.streamWidth = streamWidth
        this.streamHeight = streamHeight
        this.is10BitMode = is10Bit
        this.isReleased = false

        initEgl(targetSurface, is10Bit)
        setupProgramsAndBuffers(streamWidth, streamHeight, is10Bit)

        // Create SurfaceTexture to receive hardware decoded frames
        val st = SurfaceTexture(oesTextureId)
        st.setOnFrameAvailableListener(this)
        this.surfaceTexture = st
        this.decoderSurface = Surface(st)

        // Unbind context so it can be made current by the dedicated rendering thread
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)

        LimeLog.info("$TAG: Initialized 2-Pass pipeline (Stream=${streamWidth}x$streamHeight, Display=${displayWidth}x$displayHeight, 10Bit=$is10Bit)")
    }

    private fun initEgl(
        targetSurface: Surface,
        is10Bit: Boolean,
    ) {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)

        // Try 10-bit config if requested
        var chosenConfig: EGLConfig? = null
        if (is10Bit) {
            val attribs10Bit = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                EGL14.EGL_RED_SIZE, 10,
                EGL14.EGL_GREEN_SIZE, 10,
                EGL14.EGL_BLUE_SIZE, 10,
                EGL14.EGL_ALPHA_SIZE, 2,
                EGL14.EGL_NONE,
            )
            if (EGL14.eglChooseConfig(eglDisplay, attribs10Bit, 0, configs, 0, 1, numConfigs, 0) && numConfigs[0] > 0) {
                chosenConfig = configs[0]
                LimeLog.info("$TAG: Configured 10-bit RGBA_1010102 EGLConfig")
            }
        }

        // Standard 8-bit config fallback
        if (chosenConfig == null) {
            val attribs8Bit = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_NONE,
            )
            EGL14.eglChooseConfig(eglDisplay, attribs8Bit, 0, configs, 0, 1, numConfigs, 0)
            chosenConfig = configs[0] ?: error("eglChooseConfig failed to find compatible configuration")
        }
        this.eglConfig = chosenConfig

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, chosenConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

        // Create window surface (with optional BT2020 PQ colorspace attribute)
        eglSurface = createWindowSurface(targetSurface, chosenConfig, is10Bit)

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            val error = EGL14.eglGetError()
            LimeLog.severe("$TAG: eglMakeCurrent failed: $error")
            error("eglMakeCurrent failed with error $error")
        }

        EGL14.eglSwapInterval(eglDisplay, 0) // Disable VSync inside EGL to avoid rendering queue backlog

        val queryW = IntArray(1)
        val queryH = IntArray(1)
        EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_WIDTH, queryW, 0)
        EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_HEIGHT, queryH, 0)
        this.displayWidth = queryW[0]
        this.displayHeight = queryH[0]
    }

    private fun createWindowSurface(
        targetSurface: Surface,
        config: EGLConfig,
        is10Bit: Boolean,
    ): EGLSurface {
        if (is10Bit) {
            try {
                val hdrSurfaceAttribs = intArrayOf(
                    EGL_GL_COLORSPACE_KHR,
                    EGL_GL_COLORSPACE_BT2020_PQ_EXT,
                    EGL14.EGL_NONE,
                )
                val surface = EGL14.eglCreateWindowSurface(eglDisplay, config, targetSurface, hdrSurfaceAttribs, 0)
                if (surface != EGL14.EGL_NO_SURFACE) {
                    LimeLog.info("$TAG: Created HDR BT.2020 PQ EGLSurface")
                    return surface
                }
            } catch (t: Throwable) {
                LimeLog.warning("$TAG: Could not create HDR surface, falling back: ${t.message}")
            }
        }
        val surface = EGL14.eglCreateWindowSurface(eglDisplay, config, targetSurface, intArrayOf(EGL14.EGL_NONE), 0)
        if (surface == EGL14.EGL_NO_SURFACE) {
            val error = EGL14.eglGetError()
            LimeLog.severe("$TAG: eglCreateWindowSurface failed: $error")
            error("eglCreateWindowSurface failed with error $error")
        }
        return surface
    }

    private fun setupProgramsAndBuffers(
        streamW: Int,
        streamH: Int,
        is10Bit: Boolean,
    ) {
        // 1. Pass 1 Blit Program
        blitProgram = createProgram(BLIT_VERTEX_SHADER, BLIT_FRAGMENT_SHADER)
        blitUSTMatrixHandle = GLES31.glGetUniformLocation(blitProgram, "uSTMatrix")
        blitAPositionHandle = GLES31.glGetAttribLocation(blitProgram, "aPosition")
        blitATexCoordHandle = GLES31.glGetAttribLocation(blitProgram, "aTexCoord")
        blitSTextureHandle = GLES31.glGetUniformLocation(blitProgram, "sTexture")

        // 2. Pass 2 Post Program (SGSR1)
        setupPostProgram()

        // 3. Generate Textures
        val textures = IntArray(2)
        GLES31.glGenTextures(2, textures, 0)
        oesTextureId = textures[0]
        fboTextureId = textures[1]

        // Decoder OES Texture
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES31.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_NEAREST.toFloat())
        GLES31.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_NEAREST.toFloat())
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)

        // Standard 2D Intermediate Texture
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, fboTextureId)
        if (is10Bit) {
            GLES31.glTexImage2D(
                GLES31.GL_TEXTURE_2D, 0, GL_RGB10_A2, streamW, streamH,
                0, GLES31.GL_RGBA, GL_UNSIGNED_INT_2_10_10_10_REV, null,
            )
        } else {
            GLES31.glTexImage2D(
                GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA8, streamW, streamH,
                0, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE, null,
            )
        }
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)

        // 4. Framebuffer Object (FBO) for Pass 1
        val fbos = IntArray(1)
        GLES31.glGenFramebuffers(1, fbos, 0)
        fboId = fbos[0]
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fboId)
        GLES31.glFramebufferTexture2D(
            GLES31.GL_FRAMEBUFFER,
            GLES31.GL_COLOR_ATTACHMENT0,
            GLES31.GL_TEXTURE_2D,
            fboTextureId,
            0,
        )

        val status = GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER)
        if (status != GLES31.GL_FRAMEBUFFER_COMPLETE) {
            LimeLog.severe("$TAG: Framebuffer incomplete: $status")
        }
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
    }

    private fun setupPostProgram() {
        if (postProgram != 0) {
            GLES31.glDeleteProgram(postProgram)
            postProgram = 0
        }

        var postSource = loadAsset("sgsr1_shader_mobile_edge_direction.frag")
        val compileTimeDefines = "\n#define SRC_W $streamWidth.0\n" +
            "#define SRC_H $streamHeight.0\n" +
            "#define INV_SRC_W ${1.0f / streamWidth}\n" +
            "#define INV_SRC_H ${1.0f / streamHeight}\n"

        if (postSource.isNullOrBlank()) {
            LimeLog.warning("$TAG: sgsr1_shader_mobile_edge_direction.frag missing. Using fallback shader.")
            postSource = FALLBACK_POST_FRAGMENT_SHADER
        } else {
            postSource = postSource.replaceFirst("(#version 310 es)".toRegex(), "$1$compileTimeDefines")
        }

        val postVertexShader =
            "#version 310 es\n" +
                compileTimeDefines +
                "in vec4 aPosition;\n" +
                "in vec4 aTexCoord;\n" +
                "out highp vec2 v_texCoord;\n" +
                "out highp vec2 v_imgCoord;\n" +
                "void main() {\n" +
                "  gl_Position = aPosition;\n" +
                "  v_texCoord = aTexCoord.xy;\n" +
                "  v_imgCoord = (aTexCoord.xy * vec2(SRC_W, SRC_H)) + vec2(-0.5, 0.5);\n" +
                "}\n"

        postProgram = createProgram(postVertexShader, postSource)
        postAPositionHandle = GLES31.glGetAttribLocation(postProgram, "aPosition")
        postATexCoordHandle = GLES31.glGetAttribLocation(postProgram, "aTexCoord")

        postSTextureHandle = GLES31.glGetUniformLocation(postProgram, "sTexture")
        if (postSTextureHandle == -1) postSTextureHandle = GLES31.glGetUniformLocation(postProgram, "uTexture")
        if (postSTextureHandle == -1) postSTextureHandle = GLES31.glGetUniformLocation(postProgram, "Source")
        if (postSTextureHandle == -1) postSTextureHandle = GLES31.glGetUniformLocation(postProgram, "ps0")
    }

    /**
     * Updates the target display surface dynamically without destroying the decoder or shaders.
     */
    fun updateSurface(newSurface: Surface): Boolean {
        if (isReleased || eglDisplay == EGL14.EGL_NO_DISPLAY || eglConfig == null) return false
        val cfg = eglConfig ?: return false

        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
        }

        eglSurface = createWindowSurface(newSurface, cfg, is10BitMode)
        val queryW = IntArray(1)
        val queryH = IntArray(1)
        EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_WIDTH, queryW, 0)
        EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_HEIGHT, queryH, 0)
        displayWidth = queryW[0]
        displayHeight = queryH[0]

        LimeLog.info("$TAG: Updated display surface: ${displayWidth}x$displayHeight")
        return true
    }

    fun getDecoderSurface(): Surface? = decoderSurface

    fun getDisplayWidth(): Int = displayWidth

    override fun onFrameAvailable(st: SurfaceTexture) {
        synchronized(frameAvailableLock) {
            if (isReleased) return
            frameAvailable = true
            (frameAvailableLock as java.lang.Object).notifyAll()
        }
    }

    /**
     * Renders a single frame. Must be called from the dedicated rendering thread.
     * Guaranteed zero allocations.
     */
    fun renderFrame(presentationTimeNanos: Long) {
        if (isReleased || eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE) return

        synchronized(frameAvailableLock) {
            val deadlineNs = System.nanoTime() + 50_000_000L
            while (!frameAvailable && !isReleased) {
                val remainingNs = deadlineNs - System.nanoTime()
                if (remainingNs <= 0) break
                val ms = remainingNs / 1_000_000L
                val ns = (remainingNs % 1_000_000L).toInt()
                (frameAvailableLock as java.lang.Object).wait(ms, ns)
            }
            if (isReleased) return
            frameAvailable = false
        }

        val startNs = System.nanoTime()

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            LimeLog.severe("$TAG: eglMakeCurrent failed in renderFrame")
            return
        }

        val st = surfaceTexture ?: return
        st.updateTexImage()
        st.getTransformMatrix(transformMatrix)

        // PASS 1: Decode to 2D Texture (FBO)
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fboId)
        GLES31.glViewport(0, 0, streamWidth, streamHeight)
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)

        GLES31.glUseProgram(blitProgram)
        if (blitUSTMatrixHandle >= 0) GLES31.glUniformMatrix4fv(blitUSTMatrixHandle, 1, false, transformMatrix, 0)
        if (blitSTextureHandle >= 0) GLES31.glUniform1i(blitSTextureHandle, 0)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        drawQuad(blitAPositionHandle, blitATexCoordHandle)

        // PASS 2: Post-Process to Screen
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
        GLES31.glViewport(0, 0, displayWidth, displayHeight)
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)

        GLES31.glUseProgram(postProgram)
        if (postSTextureHandle >= 0) GLES31.glUniform1i(postSTextureHandle, 0)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, fboTextureId)
        drawQuad(postAPositionHandle, postATexCoordHandle)

        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, presentationTimeNanos)
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)

        // Latency tracking
        lastRenderLatencyNs = System.nanoTime() - startNs
        val currentAvg = averageRenderLatencyUs
        val sampleUs = lastRenderLatencyNs / 1000L
        averageRenderLatencyUs = if (currentAvg == 0L) sampleUs else currentAvg + (sampleUs - currentAvg) / 16
    }

    private fun drawQuad(
        posHandle: Int,
        texHandle: Int,
    ) {
        if (posHandle >= 0) {
            GLES31.glEnableVertexAttribArray(posHandle)
            vertexBuffer.position(0)
            GLES31.glVertexAttribPointer(posHandle, 2, GLES31.GL_FLOAT, false, 16, vertexBuffer)
        }
        if (texHandle >= 0) {
            GLES31.glEnableVertexAttribArray(texHandle)
            vertexBuffer.position(2)
            GLES31.glVertexAttribPointer(texHandle, 2, GLES31.GL_FLOAT, false, 16, vertexBuffer)
        }

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)

        if (posHandle >= 0) GLES31.glDisableVertexAttribArray(posHandle)
        if (texHandle >= 0) GLES31.glDisableVertexAttribArray(texHandle)
    }

    private fun createProgram(
        vertexSource: String,
        fragmentSource: String,
    ): Int {
        val vertexShader = loadShader(GLES31.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0
        val fragmentShader = loadShader(GLES31.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) return 0

        val program = GLES31.glCreateProgram()
        if (program != 0) {
            GLES31.glAttachShader(program, vertexShader)
            GLES31.glAttachShader(program, fragmentShader)
            GLES31.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES31.glGetProgramiv(program, GLES31.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES31.GL_TRUE) {
                LimeLog.severe("$TAG: Could not link program: ${GLES31.glGetProgramInfoLog(program)}")
                GLES31.glDeleteProgram(program)
                return 0
            }
        }
        return program
    }

    private fun loadShader(
        type: Int,
        shaderCode: String,
    ): Int {
        val shader = GLES31.glCreateShader(type)
        GLES31.glShaderSource(shader, shaderCode)
        GLES31.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            LimeLog.severe("$TAG: Could not compile shader $type: ${GLES31.glGetShaderInfoLog(shader)}")
            GLES31.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun loadAsset(fileName: String): String? {
        val sb = java.lang.StringBuilder()
        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                }
            }
        } catch (e: IOException) {
            LimeLog.severe("$TAG: Failed to load asset: $fileName - ${e.message}")
            return null
        }
        return sb.toString()
    }

    /**
     * Releases all OpenGL, EGL, and Surface resources cleanly without leaks.
     */
    fun release() {
        synchronized(frameAvailableLock) {
            if (isReleased) return
            isReleased = true
            (frameAvailableLock as java.lang.Object).notifyAll()
        }

        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)

            if (blitProgram != 0) {
                GLES31.glDeleteProgram(blitProgram)
                blitProgram = 0
            }
            if (postProgram != 0) {
                GLES31.glDeleteProgram(postProgram)
                postProgram = 0
            }
            if (fboId != 0) {
                GLES31.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
                fboId = 0
            }
            if (oesTextureId != 0 || fboTextureId != 0) {
                GLES31.glDeleteTextures(2, intArrayOf(oesTextureId, fboTextureId), 0)
                oesTextureId = 0
                fboTextureId = 0
            }

            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
            }
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }

        surfaceTexture?.release()
        surfaceTexture = null
        decoderSurface?.release()
        decoderSurface = null

        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
        eglConfig = null
    }
}
