package io.github.staernid.sunbeam;


import static io.github.staernid.sunbeam.StartExternalDisplayControlReceiver.requestFocusToExternalDisplayControl;
import static io.github.staernid.sunbeam.utils.ExternalDisplayControlActivity.SECONDARY_SCREEN_NOTIFICATION_ID;
import static io.github.staernid.sunbeam.utils.ExternalDisplayControlActivity.closeExternalDisplayControl;
import static io.github.staernid.sunbeam.utils.ServerHelper.getActiveDisplay;
import static io.github.staernid.sunbeam.utils.ServerHelper.getSecondaryDisplay;

import io.github.staernid.sunbeam.binding.PlatformBinding;
import io.github.staernid.sunbeam.binding.audio.AndroidAudioRenderer;
import io.github.staernid.sunbeam.binding.input.ControllerHandler;
import io.github.staernid.sunbeam.binding.input.GameInputDevice;
import io.github.staernid.sunbeam.binding.input.KeyboardActionListener;
import io.github.staernid.sunbeam.binding.input.KeyboardInputHandler;
import io.github.staernid.sunbeam.binding.input.KeyboardTranslator;
import io.github.staernid.sunbeam.binding.input.PointerGestureListener;
import io.github.staernid.sunbeam.binding.input.PointerInputHandler;
import io.github.staernid.sunbeam.binding.input.capture.InputCaptureManager;
import io.github.staernid.sunbeam.binding.input.capture.InputCaptureProvider;
import io.github.staernid.sunbeam.binding.input.driver.UsbDriverService;
import io.github.staernid.sunbeam.binding.input.evdev.EvdevListener;
import io.github.staernid.sunbeam.binding.video.CrashListener;
import io.github.staernid.sunbeam.binding.video.MediaCodecDecoderRenderer;
import io.github.staernid.sunbeam.binding.video.MediaCodecHelper;
import io.github.staernid.sunbeam.binding.video.PerfOverlayListener;
import io.github.staernid.sunbeam.stream.controls.VirtualControlsManager;
import androidx.annotation.NonNull;
import io.github.staernid.sunbeam.nvstream.NvConnection;
import io.github.staernid.sunbeam.stream.DefaultStreamConnectivityDriver;
import io.github.staernid.sunbeam.stream.StreamDisplayCoordinator;
import io.github.staernid.sunbeam.stream.StreamErrorResolver;
import io.github.staernid.sunbeam.stream.StreamIntentParser;
import io.github.staernid.sunbeam.stream.StreamLifecycleListener;
import io.github.staernid.sunbeam.stream.StreamLifecycleManager;
import io.github.staernid.sunbeam.stream.TerminationResolution;
import io.github.staernid.sunbeam.nvstream.StreamConfiguration;
import io.github.staernid.sunbeam.nvstream.http.ComputerDetails;
import io.github.staernid.sunbeam.nvstream.http.NvApp;
import io.github.staernid.sunbeam.nvstream.http.NvHTTP;
import io.github.staernid.sunbeam.nvstream.input.KeyboardPacket;
import io.github.staernid.sunbeam.nvstream.input.MouseButtonPacket;
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge;
import io.github.staernid.sunbeam.preferences.GlPreferences;
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration;
import androidx.compose.ui.platform.ComposeView;
import android.preference.PreferenceManager;
import io.github.staernid.sunbeam.features.stream.StreamViewModel;
import io.github.staernid.sunbeam.ui.ExternalControllerView;
import io.github.staernid.sunbeam.ui.GameGestures;
import io.github.staernid.sunbeam.ui.StreamContainer;
import io.github.staernid.sunbeam.stream.overlay.StreamMenuCoordinator;
import io.github.staernid.sunbeam.stream.overlay.StreamOverlayManager;
import io.github.staernid.sunbeam.stream.overlay.StreamToastHelper;
import io.github.staernid.sunbeam.utils.Dialog;
import io.github.staernid.sunbeam.utils.ExternalDisplayControlActivity;
import io.github.staernid.sunbeam.utils.MouseModeOption;
import io.github.staernid.sunbeam.utils.PanZoomHandler;
import io.github.staernid.sunbeam.utils.PerformanceDataTracker;
import io.github.staernid.sunbeam.utils.ServerHelper;
import io.github.staernid.sunbeam.utils.ShortcutHelper;
import io.github.staernid.sunbeam.utils.SpinnerDialog;
import io.github.staernid.sunbeam.utils.UiHelper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Rational;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import android.os.Looper;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.ArrayDeque;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import android.view.SurfaceView;
import android.view.ViewGroup;


public class Game extends AppCompatActivity implements SurfaceHolder.Callback,
        OnGenericMotionListener, OnTouchListener, StreamLifecycleListener, EvdevListener,
        KeyboardActionListener,
        OnSystemUiVisibilityChangeListener, GameGestures, StreamContainer.InputCallbacks,
        ExternalControllerView.InputCallbacks,
        PerfOverlayListener, UsbDriverService.UsbDriverStateListener, View.OnKeyListener,
        PointerGestureListener {
    public static Game instance;

    private PanZoomHandler panZoomHandler;
    private Handler timerHandler;

    private ControllerHandler controllerHandler;
    private KeyboardTranslator keyboardTranslator;
    private VirtualControlsManager controlsManager;

    private PreferenceConfiguration prefConfig;
    private SharedPreferences tombstonePrefs;

    private int displayWidth;
    private int displayHeight;
    private int currentOrientation;

    public NvConnection conn;
    private StreamLifecycleManager lifecycleManager;
    private SpinnerDialog spinner;
    public boolean connected = false;
    private boolean surfaceCreated = false;
    private int suppressPipRefCount = 0;
    private String pcName;
    private String appName;
    private NvApp app;

    private InputCaptureProvider inputCaptureProvider;
    private KeyboardInputHandler keyboardInputHandler;
    private PointerInputHandler pointerInputHandler;
    private boolean grabbedInput = true;
    private boolean cursorVisible = false;
    private boolean isPanZoomMode = false;
    private StreamContainer streamContainer;
    private StreamDisplayCoordinator displayCoordinator;

    private boolean quitOnStop = false;
    private StreamOverlayManager overlayManager;
    private StreamMenuCoordinator menuCoordinator;

    private MediaCodecDecoderRenderer decoderRenderer;
    private boolean reportedCrash;

    private WifiManager.WifiLock highPerfWifiLock;
    private WifiManager.WifiLock lowLatencyWifiLock;

    private boolean connectedToUsbDriverService = false;
    private ServiceConnection usbDriverServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            UsbDriverService.UsbDriverBinder binder = (UsbDriverService.UsbDriverBinder) iBinder;
            binder.setListener(controllerHandler);
            binder.setStateListener(Game.this);
            binder.start();
            connectedToUsbDriverService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            connectedToUsbDriverService = false;
        }
    };

    public static final String EXTRA_HOST = "Host";
    public static final String EXTRA_PORT = "Port";
    public static final String EXTRA_HTTPS_PORT = "HttpsPort";
    public static final String EXTRA_APP_NAME = "AppName";
    public static final String EXTRA_APP_UUID = "AppUUID";
    public static final String EXTRA_APP_ID = "AppId";
    public static final String EXTRA_UNIQUEID = "UniqueId";
    public static final String EXTRA_PC_UUID = "UUID";
    public static final String EXTRA_PC_NAME = "PcName";
    public static final String EXTRA_APP_HDR = "HDR";
    public static final String EXTRA_SERVER_CERT = "ServerCert";
    public static final String EXTRA_DISPLAY_ID = "DisplayID";

    private String appUUID;
    private String host;
    private int port;
    private int httpsPort;
    private int appId;
    private String uniqueId;
    private X509Certificate serverCert;

    private ViewParent rootView;

    private NvHTTP httpConn;

    public interface GameMenuCallbacks {
        void showMenu(GameInputDevice devic);
        void hideMenu();
        boolean isMenuOpen();
    }

    public GameMenuCallbacks gameMenuCallbacks;

    public boolean isInputOnly = true;
    public boolean allowChangeMouseMode = true;
    private boolean onExternelDisplay = false;

    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;
        timerHandler = new Handler(Looper.getMainLooper());

        UiHelper.setLocale(this);

        // We don't want a title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Read the stream preferences
        prefConfig = PreferenceConfiguration.readPreferences(this);
        StreamIntentParser.applyStreamExtras(getIntent(), prefConfig);
        tombstonePrefs = Game.this.getSharedPreferences("DecoderTombstone", 0);

        if (prefConfig.fullScreen) {
            // Full-screen
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            // If we're going to use immersive mode, we want to have
            // the entire screen
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        // Listen for UI visibility events
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);

        // Change volume button behavior
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Inflate the content
        setContentView(R.layout.activity_game);
        streamContainer = findViewById(R.id.streamContainer);
        displayCoordinator = new StreamDisplayCoordinator(this, prefConfig, streamContainer);

        // Start the spinner
        spinner = SpinnerDialog.displayDialog(this, getResources().getString(R.string.conn_establishing_title),
                getResources().getString(R.string.conn_establishing_msg), true);


        Display currentDisplay = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int displayId = getIntent().getIntExtra(EXTRA_DISPLAY_ID, Display.DEFAULT_DISPLAY);
            currentDisplay = getSystemService(DisplayManager.class).getDisplay(displayId);
        }

        if (currentDisplay == null) {
            currentDisplay = getWindowManager().getDefaultDisplay();
        }

        onExternelDisplay = currentDisplay.getDisplayId() != Display.DEFAULT_DISPLAY;

        boolean shouldInvertDecoderResolution = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && onExternelDisplay
        ) {
            Display.Mode currentMode = currentDisplay.getMode();
            displayWidth = currentMode.getPhysicalWidth();
            displayHeight = currentMode.getPhysicalHeight();
            prefConfig.width = displayWidth;
            prefConfig.height = displayHeight;
            prefConfig.fps = currentMode.getRefreshRate();
            prefConfig.videoScaleMode = PreferenceConfiguration.ScaleMode.STRETCH;
            prefConfig.enableFloatingButton = false;
            prefConfig.showOverlayZoomToggleButton = false;
            prefConfig.enablePip = false;
            currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        } else {
            if (prefConfig.autoOrientation) {
                currentOrientation = getResources().getConfiguration().orientation;
            } else {
                currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
            }

            boolean portraitMode = currentOrientation == Configuration.ORIENTATION_PORTRAIT;
            shouldInvertDecoderResolution = portraitMode && prefConfig.autoInvertVideoResolution;

            displayWidth = shouldInvertDecoderResolution ? prefConfig.height : prefConfig.width;
            displayHeight = shouldInvertDecoderResolution ? prefConfig.width : prefConfig.height;

            // Enter landscape unless we're on a square screen
            displayCoordinator.setPreferredOrientationForActivity(currentOrientation, displayWidth, displayHeight);
        }

        displayCoordinator.applyWindowCutoutMode(displayWidth, displayHeight);

        //光标是否显示
        cursorVisible = prefConfig.enableMouseLocalCursor;

        // Listen for non-touch events on the game surface
        streamContainer.init(this, prefConfig);
        streamContainer.setOnGenericMotionListener(this);
        streamContainer.setOnKeyListener(this);
        streamContainer.setInputCallbacks(this);
        streamContainer.setCommitTextEnabled(prefConfig.enableCommitText);

        rootView = streamContainer.getParent();

        //串流画面 顶部居中显示
        if(prefConfig.alignDisplayTopCenter){
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) streamContainer.getLayoutParams();
            params.gravity = Gravity.CENTER_HORIZONTAL|Gravity.TOP;
        }
        // Listen for touch events on the background touch view to enable trackpad mode
        // to work on areas outside of the StreamView itself. We use a separate View
        // for this rather than just handling it at the Activity level, because that
        // allows proper touch splitting, which the OSC relies upon.
        View backgroundTouchView = findViewById(R.id.backgroundTouchView);
        backgroundTouchView.setOnTouchListener(this);


        panZoomHandler = new PanZoomHandler(
                getApplicationContext(),
                this,
                streamContainer.getSurfaceView(),
                streamContainer,
                prefConfig
        );

        // Restore previous zoom & pan if enabled and saved
        if (prefConfig.rememberZoomPan) {
            streamContainer.post(() -> panZoomHandler.setInitialZoomAndPan(
                    prefConfig.zoomScale,
                    prefConfig.panOffsetX,
                    prefConfig.panOffsetY
            ));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Request unbuffered input event dispatching for all input classes we handle here.
            // Without this, input events are buffered to be delivered in lock-step with VBlank,
            // artificially increasing input latency while streaming.
            streamContainer.requestUnbufferedDispatch(
                    InputDevice.SOURCE_CLASS_BUTTON | // Keyboards
                            InputDevice.SOURCE_CLASS_JOYSTICK | // Gamepads
                            InputDevice.SOURCE_CLASS_POINTER | // Touchscreens and mice (w/o pointer capture)
                            InputDevice.SOURCE_CLASS_POSITION | // Touchpads
                            InputDevice.SOURCE_CLASS_TRACKBALL // Mice (pointer capture)
            );
            backgroundTouchView.requestUnbufferedDispatch(
                    InputDevice.SOURCE_CLASS_BUTTON | // Keyboards
                            InputDevice.SOURCE_CLASS_JOYSTICK | // Gamepads
                            InputDevice.SOURCE_CLASS_POINTER | // Touchscreens and mice (w/o pointer capture)
                            InputDevice.SOURCE_CLASS_POSITION | // Touchpads
                            InputDevice.SOURCE_CLASS_TRACKBALL // Mice (pointer capture)
            );
        }

        inputCaptureProvider = InputCaptureManager.getInputCaptureProvider(this, this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            streamContainer.setOnCapturedPointerListener(new View.OnCapturedPointerListener() {
                @Override
                public boolean onCapturedPointer(View view, MotionEvent motionEvent) {
//                    LimeLog.info("onCapturedPointer="+motionEvent.toString());
//                    LimeLog.info("onCapturedPointer-Device="+motionEvent.getDevice().toString());
                    return handleMotionEvent(view, motionEvent);
                }
            });
        }

        // Warn the user if they're on a metered connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isMetered = connMgr.isActiveNetworkMetered();
        if (isMetered) {
            onDisplayTransientMessage(getResources().getString(R.string.conn_metered));
        }

        // Make sure Wi-Fi is fully powered up
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            highPerfWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Moonlight High Perf Lock");
            highPerfWifiLock.setReferenceCounted(false);
            highPerfWifiLock.acquire();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                lowLatencyWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Moonlight Low Latency Lock");
                lowLatencyWifiLock.setReferenceCounted(false);
                lowLatencyWifiLock.acquire();
            }
        } catch (SecurityException e) {
            // Some Samsung Galaxy S10+/S10e devices throw a SecurityException from
            // WifiLock.acquire() even though we have android.permission.WAKE_LOCK in our manifest.
            e.printStackTrace();
        }

        appName = Game.this.getIntent().getStringExtra(EXTRA_APP_NAME);
        pcName = Game.this.getIntent().getStringExtra(EXTRA_PC_NAME);

        host = Game.this.getIntent().getStringExtra(EXTRA_HOST);
        port = Game.this.getIntent().getIntExtra(EXTRA_PORT, NvHTTP.DEFAULT_HTTP_PORT);
        httpsPort = Game.this.getIntent().getIntExtra(EXTRA_HTTPS_PORT, 0); // 0 is treated as unknown
        appUUID = Game.this.getIntent().getStringExtra(EXTRA_APP_UUID);
        appId = Game.this.getIntent().getIntExtra(EXTRA_APP_ID, StreamConfiguration.INVALID_APP_ID);
        uniqueId = Game.this.getIntent().getStringExtra(EXTRA_UNIQUEID);
        boolean appSupportsHdr = Game.this.getIntent().getBooleanExtra(EXTRA_APP_HDR, false);
        byte[] derCertData = Game.this.getIntent().getByteArrayExtra(EXTRA_SERVER_CERT);

        app = new NvApp(appName != null ? appName : "app", appUUID, appId, appSupportsHdr);

        try {
            if (derCertData != null) {
                serverCert = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(new ByteArrayInputStream(derCertData));

                httpConn = new NvHTTP(new ComputerDetails.AddressTuple(host, port), httpsPort, uniqueId, serverCert, PlatformBinding.getCryptoProvider(this));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (appId == StreamConfiguration.INVALID_APP_ID) {
            finish();
            return;
        }

        // Initialize the MediaCodec helper before creating the decoder
        GlPreferences glPrefs = GlPreferences.readPreferences(this);
        MediaCodecHelper.initialize(this, glPrefs.glRenderer);

        // Check if the user has enabled HDR
        boolean willStreamHdr = displayCoordinator.resolveHdrSupport(currentDisplay, onExternelDisplay);

        decoderRenderer = new MediaCodecDecoderRenderer(
                this,
                prefConfig,
                new CrashListener() {
                    @Override
                    public void notifyCrash(Exception e) {
                        // The MediaCodec instance is going down due to a crash
                        // let's tell the user something when they open the app again

                        // We must use commit because the app will crash when we return from this function
                        tombstonePrefs.edit().putInt("CrashCount", tombstonePrefs.getInt("CrashCount", 0) + 1).commit();
                        reportedCrash = true;
                    }
                },
                tombstonePrefs.getInt("CrashCount", 0),
                connMgr.isActiveNetworkMetered(),
                willStreamHdr,
                shouldInvertDecoderResolution,
                glPrefs.glRenderer,
                this);

        int supportedVideoFormats =
                displayCoordinator.configureDecoderAndResolveVideoFormats(decoderRenderer, willStreamHdr);

        int gamepadMask = ControllerHandler.getAttachedControllerMask(this);
        if (!prefConfig.multiController) {
            // Always set gamepad 1 present for when multi-controller is
            // disabled for games that don't properly support detection
            // of gamepads removed and replugged at runtime.
            gamepadMask = 1;
        }
        if (prefConfig.onscreenController) {
            // If we're using OSC, always set at least gamepad 1.
            gamepadMask |= 1;
        }

        // Set to the optimal mode for streaming
        float displayRefreshRate =
                displayCoordinator.prepareDisplayForRendering(currentDisplay, displayWidth, displayHeight, onExternelDisplay);
        LimeLog.info("Display refresh rate: "+displayRefreshRate);

        float chosenFrameRate = displayCoordinator.computeStreamFrameRate(displayRefreshRate);

        StreamConfiguration config = new StreamConfiguration.Builder()
                .setResolution(
                        displayWidth,
                        displayHeight
                )
                .setLaunchRefreshRate(prefConfig.fps)
                .setRefreshRate(chosenFrameRate)
                .setApp(app)
                .setEnableUltraLowLatency(prefConfig.enableUltraLowLatency)
                .setBitrate(isMetered ? prefConfig.meteredBitrate: prefConfig.bitrate)
                .setEnableSops(prefConfig.enableSops)
                .enableLocalAudioPlayback(prefConfig.playHostAudio)
                .setMaxPacketSize(1392)
                .setRemoteConfiguration(StreamConfiguration.STREAM_CFG_AUTO) // NvConnection will perform LAN and VPN detection
                .setSupportedVideoFormats(supportedVideoFormats)
                .setAttachedGamepadMask(gamepadMask)
                .setClientRefreshRateX100((int)(displayRefreshRate * 100))
                .setAudioConfiguration(prefConfig.audioConfiguration)
                .setColorSpace(decoderRenderer.getPreferredColorSpace())
                .setColorRange(decoderRenderer.getPreferredColorRange())
                .setPersistGamepadsAfterDisconnect(!prefConfig.multiController)
                .build();

        // Initialize the connection
        conn = new NvConnection(getApplicationContext(),
                new ComputerDetails.AddressTuple(host, port),
                httpsPort, uniqueId, config,
                PlatformBinding.getCryptoProvider(this), serverCert);
        controllerHandler = new ControllerHandler(this, conn, this, prefConfig);
        lifecycleManager = new StreamLifecycleManager(
                conn,
                controllerHandler,
                decoderRenderer,
                prefConfig,
                httpConn,
                quitOnStop,
                appName,
                new StreamErrorResolver(
                        resId -> getResources().getString(resId),
                        DefaultStreamConnectivityDriver.INSTANCE
                ),
                this,
                () -> {
                    Surface surface = streamContainer.getSurface();
                    return surface != null && surface.isValid();
                }
        );
        keyboardTranslator = new KeyboardTranslator(prefConfig);
        keyboardInputHandler = new KeyboardInputHandler(
                conn,
                keyboardTranslator,
                prefConfig,
                this,
                controllerHandler,
                () -> grabbedInput
        );

        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(keyboardTranslator, null);

        controlsManager = new VirtualControlsManager(
                this,
                (FrameLayout) rootView,
                prefConfig,
                controllerHandler,
                () -> conn,
                this::isOnExternalDisplay,
                ExternalDisplayControlActivity::toggleFullKeyboard
        );

        pointerInputHandler = new PointerInputHandler(
                conn,
                prefConfig,
                inputCaptureProvider,
                streamContainer,
                this,
                controllerHandler,
                () -> grabbedInput,
                controlsManager::isVirtualControllerEditing,
                () -> isPanZoomMode,
                event -> {
                    if (panZoomHandler != null) {
                        panZoomHandler.handleTouchEvent(event);
                    }
                },
                () -> getResources().getDisplayMetrics().widthPixels
        );

        if (Objects.equals(appUUID, NvApp.REMOTE_INPUT_UUID)) {
            // Force trackpad mode since we won't see anything on the screen
            isInputOnly = true;
            allowChangeMouseMode = false;
            applyMouseMode(2);
        } else {
            if (prefConfig.enableFullExDisplay && onExternelDisplay) {
                requestFocusToExternalDisplayControl(this);
                listenForExternalDisplayRemoval();
            }

            // Initialize touch contexts based on preferences
            // The mouse mode preference is also read in PreferenceConfiguration to set the boolean flags
            initMouseMode();
        }

        controlsManager.setupInitialControls();

        if (!decoderRenderer.isAvcSupported()) {
            if (spinner != null) {
                spinner.dismiss();
                spinner = null;
            }

            // If we can't find an AVC decoder, we can't proceed
            Dialog.displayDialog(this, getResources().getString(R.string.conn_error_title),
                    "This device or ROM doesn't support hardware accelerated H.264 playback.", true);
            return;
        }

        // The connection will be started when the surface gets created
        //streamContainer.getHolder().addCallback(this);

        streamContainer.setOnSurfaceAvailable(() -> {
            if (lifecycleManager != null && !lifecycleManager.getHasAttemptedConnection()) {
                LimeLog.info("Surface is available, starting connection...");

                // Der Decoder erhält die jeweils aktive Oberfläche vom Container
                decoderRenderer.setRenderTarget(streamContainer.getSurface());

                // Starten Sie die NvConnection
                lifecycleManager.startConnection(
                        new AndroidAudioRenderer(Game.this, prefConfig.playHostAudio),
                        decoderRenderer
                );
            }
        });

        ComposeView composeView = findViewById(R.id.streamComposeOverlay);
        int initialMouseMode = Objects.equals(appUUID, NvApp.REMOTE_INPUT_UUID)
                ? 2
                : StreamMenuCoordinator.resolveInitialMouseMode(this, isOnExternalDisplay());

        overlayManager = new StreamOverlayManager(
                prefConfig,
                isWidth -> isWidth ? getWindow().getDecorView().getWidth() : getWindow().getDecorView().getHeight(),
                findViewById(R.id.performanceOverlay),
                (TextView) findViewById(R.id.performanceOverlayLite),
                (TextView) findViewById(R.id.performanceOverlayBig),
                (TextView) findViewById(R.id.notificationOverlay),
                (ImageButton) findViewById(R.id.floatingMenuButton),
                (ImageButton) findViewById(R.id.overlayToggleZoomButton),
                () -> showGameMenu(null),
                this::toggleZoomMode,
                new Handler(Looper.getMainLooper()),
                composeView,
                new StreamViewModel(),
                this::disconnect,
                this::quitNow,
                this::sendKeys,
                this::applyMouseMode,
                this::toggleMouseLocalCursor,
                this::toggleKeyboard,
                this::toggleVirtualController,
                controlsManager::cycleVirtualControllerEditMode,
                initialMouseMode
        );

        gameMenuCallbacks = new GameMenu(this);
        menuCoordinator = new StreamMenuCoordinator(
                this::isOnExternalDisplay,
                () -> gameMenuCallbacks,
                overlayManager::showMenu,
                overlayManager::hideMenu,
                overlayManager::isMenuOpen
        );

        displayCoordinator.configureSurfaceViewForDisplay(
                findViewById(android.R.id.content),
                currentDisplay,
                displayWidth,
                displayHeight
        );
    }

    private void updateZoomButtonAppearance() {
        if (overlayManager != null) {
            overlayManager.updateZoomButtonAppearance(isPanZoomMode);
        }
    }

    private void listenForExternalDisplayRemoval() {
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
            }

            @Override
            public void onDisplayRemoved(int displayId) {
                if (getSecondaryDisplay(getBaseContext()) == null) {
                    handleDisplayRemoved();
                    finish();
                }
            }

            @Override
            public void onDisplayChanged(int displayId) {
            }
        }, null);
    }

    private void handleDisplayRemoved() {
        NotificationManagerCompat.from(getBaseContext()).cancel(SECONDARY_SCREEN_NOTIFICATION_ID);
        closeExternalDisplayControl();
    }

    public Boolean isKeyboardLayoutVisible() {
        return controlsManager != null && controlsManager.isKeyboardLayoutVisible();
    }

    // 显示隐藏虚拟特殊按键
    public void toggleKeyboardController() {
        if (controlsManager != null) {
            controlsManager.toggleKeyboardController();
        }
    }

    public void toggleFullKeyboard() {
        if (controlsManager != null) {
            controlsManager.toggleFullKeyboard();
        }
    }

    // 显示隐藏虚拟手柄控制器
    public void toggleVirtualController() {
        if (controlsManager != null) {
            controlsManager.toggleVirtualController();
            if (overlayManager != null) {
                overlayManager.updateVirtualControllerState(prefConfig.onscreenController);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Set requested orientation for possible new screen size
        displayCoordinator.setPreferredOrientationForActivity(currentOrientation, displayWidth, displayHeight);

        if (controlsManager != null) {
            controlsManager.onConfigurationChanged();
        }

        // Hide on-screen overlays in PiP mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isInPictureInPictureMode()) {
                if (overlayManager != null) {
                    overlayManager.onEnterPiP();
                }

                if (controlsManager != null) {
                    controlsManager.onEnterPiP();
                }

                hideGameMenu();

                // Disable sensors while in PiP mode
                controllerHandler.disableSensors();

                // Update GameManager state to indicate we're in PiP (still gaming, but interruptible)
                UiHelper.notifyStreamEnteringPiP(this);
            }
            else {
                if (overlayManager != null) {
                    overlayManager.onExitPiP();
                }

                // Restore overlays to previous state when leaving PiP
                if (controlsManager != null) {
                    controlsManager.onExitPiP();
                }

                // Enable sensors again after exiting PiP
                controllerHandler.enableSensors();

                // Update GameManager state to indicate we're out of PiP (gaming, non-interruptible)
                UiHelper.notifyStreamExitingPiP(this);
            }
        }
    }

    public void updatePipAutoEnter() {
        if (displayCoordinator != null && rootView instanceof View) {
            displayCoordinator.updatePipAutoEnter(
                    connected,
                    suppressPipRefCount,
                    isOnExternalDisplay(),
                    (View) rootView,
                    appName,
                    pcName
            );
        }
    }

    public void setMetaKeyCaptureState(boolean enabled) {
        if (displayCoordinator != null) {
            displayCoordinator.setMetaKeyCaptureState(enabled);
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (displayCoordinator != null && rootView instanceof View) {
            displayCoordinator.onUserLeaveHint(isOnExternalDisplay(), (View) rootView, appName, pcName);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.R)
    public boolean onPictureInPictureRequested() {
        if (displayCoordinator != null && rootView instanceof View) {
            return displayCoordinator.onPictureInPictureRequested((View) rootView, appName, pcName);
        }
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // We can't guarantee the state of modifiers keys which may have
        // lifted while focus was not on us. Clear the modifier state.
        if (keyboardInputHandler != null) {
            keyboardInputHandler.resetState();
        }

        // With Android native pointer capture, capture is lost when focus is lost,
        // so it must be requested again when focus is regained.
        inputCaptureProvider.onWindowFocusChanged(hasFocus);
    }

    public boolean isOnExternalDisplay() {
        return onExternelDisplay;
    }

    private void hideSystemUi(int delay) {
        if (displayCoordinator != null) {
            displayCoordinator.hideSystemUi(delay);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        if (displayCoordinator != null && decoderRenderer != null) {
            displayCoordinator.onMultiWindowModeChanged(isInMultiWindowMode, decoderRenderer);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        instance = null;
        timerHandler.removeCallbacksAndMessages(null);

        if (prefConfig.enableFullExDisplay) handleDisplayRemoved();

        if (controllerHandler != null) {
            controllerHandler.destroy();
        }
        if (keyboardTranslator != null) {
            InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
            inputManager.unregisterInputDeviceListener(keyboardTranslator);
        }

        if (lowLatencyWifiLock != null) {
            lowLatencyWifiLock.release();
        }
        if (highPerfWifiLock != null) {
            highPerfWifiLock.release();
        }

        // Save zoom/pan before other cleanup
        if (prefConfig != null && prefConfig.rememberZoomPan && panZoomHandler != null) {
            SharedPreferences basePrefs = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
            basePrefs.edit()
                    .putFloat("number_zoom_scale", panZoomHandler.getScaleFactor())
                    .putFloat("number_pan_offset_x", panZoomHandler.getChildX())
                    .putFloat("number_pan_offset_y", panZoomHandler.getChildY())
                    .apply();
        }

        if (connectedToUsbDriverService) {
            // Unbind from the discovery service
            unbindService(usbDriverServiceConnection);
        }

        // Destroy the capture provider
        inputCaptureProvider.destroy();
        streamContainer.onDestroy();

        if (controlsManager != null) {
            controlsManager.destroy();
        }
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            // Stop any further input device notifications before we lose focus (and pointer capture)
            if (controllerHandler != null) {
                controllerHandler.stop();
            }

            // Ungrab input to prevent further input device notifications
            setInputGrabState(false);
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        SpinnerDialog.closeDialogs(this);
        Dialog.closeDialogs();

        if (controlsManager != null) {
            controlsManager.onStop();
        }

        if (conn != null) {
            if (lifecycleManager != null) {
                lifecycleManager.setHasDisplayedFailureDialog(true);
            }
            stopConnection();
            if (displayCoordinator != null && decoderRenderer != null) {
                displayCoordinator.reportPostStreamTelemetry(
                        decoderRenderer,
                        tombstonePrefs,
                        reportedCrash,
                        displayWidth,
                        displayHeight
                );
            }
        }

        finish();
    }

    private void setInputGrabState(boolean grab) {
        // Grab/ungrab the mouse cursor
        if (grab) {
            inputCaptureProvider.enableCapture();

            // Enabling capture may hide the cursor again, so
            // we will need to show it again.
            if (cursorVisible) {
                inputCaptureProvider.showCursor();
            }
        }
        else {
            inputCaptureProvider.disableCapture();
        }

        // Grab/ungrab system keyboard shortcuts
        setMetaKeyCaptureState(grab);

        grabbedInput = grab;
    }

    @Override
    public void onToggleInputGrab() {
        Handler h = getWindow().getDecorView().getHandler();
        if (h != null) {
            h.postDelayed(() -> setInputGrabState(!grabbedInput), 250);
        }
    }

    @Override
    public void onExitRequested() {
        finish();
    }

    @Override
    public void onToggleCursorVisibility() {
        if (!grabbedInput) {
            inputCaptureProvider.enableCapture();
            grabbedInput = true;
        }
        cursorVisible = !cursorVisible;
        if (cursorVisible) {
            inputCaptureProvider.showCursor();
        } else {
            inputCaptureProvider.hideCursor();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return handleKeyDown(event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean handleKeyDown(KeyEvent event) {
        return keyboardInputHandler != null && keyboardInputHandler.handleKeyDown(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return handleKeyUp(event) || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean handleKeyUp(KeyEvent event) {
        return keyboardInputHandler != null && keyboardInputHandler.handleKeyUp(event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return handleKeyMultiple(event) || super.onKeyMultiple(keyCode, repeatCount, event);
    }

    public boolean handleKeyMultiple(KeyEvent event) {
        return keyboardInputHandler != null && keyboardInputHandler.handleKeyMultiple(event);
    }

    public void sendKeys(short[] keys) {
        if (keyboardInputHandler != null) {
            keyboardInputHandler.sendKeys(keys);
        }
    }

    public boolean handleFocusChange(boolean hasFocus) {
        return false;
    }



    @Override
    public void toggleKeyboard() {
        if (isOnExternalDisplay()) {
            ExternalDisplayControlActivity.toggleKeyboard();
        } else {
            LimeLog.info("Toggling keyboard overlay");
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.toggleSoftInput(0, 0);
        }
    }

    @Override
    public void onToggleKeyboard() {
        toggleKeyboard();
    }

    @Override
    public void onToggleFullKeyboard() {
        toggleFullKeyboard();
    }

    @Override
    public void onShowGameMenu() {
        showGameMenu(null);
    }

    @Override
    public void onVibrate(long durationMs, int amplitude) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude));
            } else {
                vibrator.vibrate(durationMs);
            }
        }
    }

    public boolean handleMotionEvent(View view, MotionEvent event) {
        return pointerInputHandler != null && pointerInputHandler.handleMotionEvent(view, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return handleMotionEvent(null, event) || super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onGenericMotion(View view, MotionEvent event) {
        return handleMotionEvent(view, event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Tell the OS not to buffer input events for us
            //
            // NB: This is still needed even when we call the newer requestUnbufferedDispatch()!
            view.requestUnbufferedDispatch(event);
        }

        return handleMotionEvent(view, event);
    }

    @Override
    public void onStageStarting(@NonNull String stage) {
        if (spinner != null) {
            spinner.setMessage(getResources().getString(R.string.conn_starting) + " " + stage);
        }
    }

    @Override
    public void onStageComplete(@NonNull String stage) {
    }

    @Override
    public void onStageFailedRetryUnlocking() {
        if (spinner != null) {
            spinner.setMessage(getResources().getString(R.string.unlocking_or_starting));
        }
    }

    @Override
    public void onStageFailedError(@NonNull String title, @NonNull String message, boolean isVideoDecoderFailure) {
        if (spinner != null) {
            spinner.dismiss();
            spinner = null;
        }

        if (isVideoDecoderFailure) {
            Toast.makeText(Game.this, getResources().getText(R.string.video_decoder_init_failed), Toast.LENGTH_LONG).show();
        }

        Dialog.displayDialog(Game.this, title, message, true);
        finishSecondScreen();
    }

    private void finishSecondScreen() {
        // Otherwise screen stays connected but not working with no way of quitting it
        if (prefConfig.enableFullExDisplay) {
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 2000);
        }
    }

    @Override
    public void onConnectionTerminated(@NonNull TerminationResolution resolution) {
        // Let the display go to sleep now
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Stop processing controller input
        controllerHandler.stop();
        timerHandler.removeCallbacksAndMessages(null);

        // Ungrab input
        setInputGrabState(false);

        if (resolution instanceof TerminationResolution.ShowErrorDialog) {
            TerminationResolution.ShowErrorDialog dialog = (TerminationResolution.ShowErrorDialog) resolution;
            Dialog.displayDialog(Game.this, dialog.getTitle(), dialog.getMessage(), true);
        } else {
            finish();
        }
    }

    @Override
    public void onConnectionStatusUpdate(int connectionStatus) {
        if (overlayManager != null) {
            overlayManager.onConnectionStatusUpdate(connectionStatus);
        }
    }

    @Override
    public void onConnectionStarted() {
        if (spinner != null) {
            spinner.dismiss();
            spinner = null;
        }

        connected = true;
        updatePipAutoEnter();

        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setInputGrabState(true);
            }
        }, 500);

        // Keep the display on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Update GameManager state to indicate we're in game
        UiHelper.notifyStreamConnected(Game.this);

        // Ensure overlay toggle button visibility is properly set
        if (overlayManager != null) {
            overlayManager.refreshOverlayZoomButtonVisibility();
        }

        hideSystemUi(1000);

        if (prefConfig.usbDriver) {
            // Start the USB driver
            bindService(new Intent(this, UsbDriverService.class),
                    usbDriverServiceConnection, Service.BIND_AUTO_CREATE);
        }

        // Report this shortcut being used (off the main thread to prevent ANRs)
        ComputerDetails computer = new ComputerDetails();
        computer.name = pcName;
        computer.uuid = Game.this.getIntent().getStringExtra(EXTRA_PC_UUID);
        ShortcutHelper shortcutHelper = new ShortcutHelper(this);
        shortcutHelper.reportComputerShortcutUsed(computer);
        if (appName != null) {
            // This may be null if launched from the "Resume Session" PC context menu item
            shortcutHelper.reportGameLaunched(computer, app);
        }
    }

    @Override
    public void onDisplayMessage(@NonNull String message) {
        Toast.makeText(Game.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisplayTransientMessage(@NonNull String message) {
        Toast.makeText(Game.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStreamEnding() {
        connected = false;
        updatePipAutoEnter();

        controllerHandler.stop();

        // Update GameManager state to indicate we're no longer in game
        UiHelper.notifyStreamEnded(this);
    }

    @Override
    public void onAppQuitSuccess(@NonNull String appName) {
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.applist_quit_success) + " " + appName, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAppQuitFailure(@NonNull String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void stopConnection() {
        if (lifecycleManager != null) {
            lifecycleManager.stopConnection();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!surfaceCreated) {
            throw new IllegalStateException("Surface changed before creation!");
        }

        LimeLog.info("surfaceChanged-->"+width+" x "+height + "----"+displayWidth+" x "+displayHeight);

        panZoomHandler.handleSurfaceChange();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isInPictureInPictureMode()) {
                updatePipAutoEnter();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceCreated = true;
        if (displayCoordinator != null) {
            displayCoordinator.applySurfaceFrameRate(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (!surfaceCreated) {
            throw new IllegalStateException("Surface destroyed before creation!");
        }

        if (lifecycleManager != null && lifecycleManager.getHasAttemptedConnection()) {
            // Let the decoder know immediately that the surface is gone
            decoderRenderer.prepareForStop();

            if (connected) {
                stopConnection();
            }
        }
    }

    @Override
    public void mouseMove(int deltaX, int deltaY) {
        if (pointerInputHandler != null) {
            pointerInputHandler.handleMouseMove(deltaX, deltaY);
        }
    }

    @Override
    public void mouseButtonEvent(int buttonId, boolean down) {
        if (pointerInputHandler != null) {
            pointerInputHandler.handleMouseButtonEvent(buttonId, down);
        }
    }

    @Override
    public void mouseVScroll(byte amount) {
        if (pointerInputHandler != null) {
            pointerInputHandler.handleMouseVScroll(amount);
        }
    }

    @Override
    public void mouseHScroll(byte amount) {
        if (pointerInputHandler != null) {
            pointerInputHandler.handleMouseHScroll(amount);
        }
    }

    @Override
    public void keyboardEvent(boolean buttonDown, short keyCode) {
        if (keyboardInputHandler != null) {
            keyboardInputHandler.handleVirtualKeyEvent(keyCode, buttonDown);
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        // Don't do anything if we're not connected
        if (!connected) {
            return;
        }

        // This flag is set for all devices
        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            hideSystemUi(2000);
        }
        else if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
            hideSystemUi(2000);
        }
    }

    @Override
    public void onPerfUpdate(final String text) {
        if (overlayManager != null) {
            overlayManager.onPerfUpdate(text);
        }
    }

    @Override
    public void onUsbPermissionPromptStarting() {
        // Disable PiP auto-enter while the USB permission prompt is on-screen. This prevents
        // us from entering PiP while the user is interacting with the OS permission dialog.
        suppressPipRefCount++;
        updatePipAutoEnter();
    }

    @Override
    public void onUsbPermissionPromptCompleted() {
        suppressPipRefCount--;
        updatePipAutoEnter();
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        switch (keyEvent.getAction()) {
            case KeyEvent.ACTION_DOWN:
                return handleKeyDown(keyEvent);
            case KeyEvent.ACTION_UP:
                return handleKeyUp(keyEvent);
            case KeyEvent.ACTION_MULTIPLE:
                return handleKeyMultiple(keyEvent);
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (menuCoordinator != null && menuCoordinator.isMenuOpen()) {
            menuCoordinator.hideGameMenu();
            return;
        }
        if(prefConfig.enableBackMenu){
            showGameMenu(null);
            return;
        }
        super.onBackPressed();
    }

    public boolean isZoomModeEnabled() {
        return isPanZoomMode;
    }
    public void toggleZoomMode() {
        this.isPanZoomMode = !this.isPanZoomMode;
        StreamToastHelper.showPanZoomToast(this, this.isPanZoomMode);
        updateZoomButtonAppearance();

        if (ExternalDisplayControlActivity.instance != null) {
            ExternalDisplayControlActivity.instance.toggleZoomMode(false);
        }
    }

    public void rotateScreen() {
        if (displayCoordinator != null) {
            currentOrientation = displayCoordinator.rotateScreen(currentOrientation);
        }
    }

    private void initMouseMode() {
        applyMouseMode(StreamMenuCoordinator.resolveInitialMouseMode(this, isOnExternalDisplay()));
    }

    /**
     * Displays a dialog allowing the user to select a mouse input mode.
     *
     * Behavior:
     * - On regular displays, all available mouse modes are shown.
     * - On secondary displays (e.g. Samsung DeX), only a limited set of modes are allowed:
     *   "Trackpad Natural", "Trackpad Gaming", and "Disabled".
     * - An additional option to toggle the local mouse cursor is always included.
     *
     * When the user selects a mode:
     * - If it's the toggle option, the local mouse cursor mode is toggled.
     * - Otherwise, the selected mode is applied and saved to shared preferences.
     *
     * @param context The context to use to decide where to show the dialog.
     */
    public void selectMouseMode(Context context) {
        if (menuCoordinator != null) {
            menuCoordinator.showSelectMouseModeDialog(
                    context,
                    prefConfig,
                    this::applyMouseMode,
                    this::toggleMouseLocalCursor
            );
        }
    }

    //本地鼠标光标切换
    public void toggleMouseLocalCursor(){
        if (!grabbedInput) {
            inputCaptureProvider.enableCapture();
            grabbedInput = true;
        }
        cursorVisible = !cursorVisible;
        if (cursorVisible) {
            inputCaptureProvider.showCursor();
        } else {
            inputCaptureProvider.hideCursor();
        }
        if (overlayManager != null) {
            overlayManager.updateLocalCursorState(cursorVisible);
        }
    }

    public void applyMouseMode(int mode) {
        if (pointerInputHandler != null) {
            pointerInputHandler.applyMouseMode(mode);
        }

        // Always exit zoom mode if mouse mode has changed
        isPanZoomMode = false;
        updateZoomButtonAppearance();
    }

    public void toggleHUD() {
        if (overlayManager != null) {
            overlayManager.toggleHUD();
        }
    }

    //切换触控灵敏度开关
    public void switchTouchSensitivity(){
        prefConfig.enableTouchSensitivity = !prefConfig.enableTouchSensitivity;
    }

    public void disconnect() {
        finish();
    }

    public void quitNow() {
        quitOnStop = true;
        if (lifecycleManager != null) {
            lifecycleManager.setQuitOnStop(true);
        }
        finish();
    }

    public void quit() {
        if (overlayManager != null && overlayManager.hasComposeOverlay()) {
            overlayManager.showQuitConfirmation();
        } else if (menuCoordinator != null) {
            menuCoordinator.showQuitConfirmDialog(this, this::quitNow);
        }
    }

    @Override
    public void showGameMenu(GameInputDevice device) {
        if (menuCoordinator != null) {
            menuCoordinator.showGameMenu(device);
        }
    }

    public void hideGameMenu() {
        if (menuCoordinator != null) {
            menuCoordinator.hideGameMenu();
        }
    }

    public void toggleFloatingButtonVisibility() {
        if (overlayManager != null) {
            overlayManager.toggleFloatingButtonVisibility();
        }
    }


    @Override
    public boolean handleCommitText(CharSequence text) {
        return keyboardInputHandler != null && keyboardInputHandler.handleCommitText(text);
    }

    @Override
    public boolean handleDeleteSurroundingText(int beforeLength, int afterLength) {
        return keyboardInputHandler != null &&
                keyboardInputHandler.handleDeleteSurroundingText(beforeLength, afterLength);
    }
}