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
import io.github.staernid.sunbeam.binding.input.virtual_controller.VirtualController;
import io.github.staernid.sunbeam.binding.input.virtual_controller.keyboard.KeyBoardController;
import io.github.staernid.sunbeam.binding.input.virtual_controller.keyboard.KeyBoardLayoutController;
import io.github.staernid.sunbeam.binding.video.CrashListener;
import io.github.staernid.sunbeam.binding.video.MediaCodecDecoderRenderer;
import io.github.staernid.sunbeam.binding.video.MediaCodecHelper;
import io.github.staernid.sunbeam.binding.video.PerfOverlayListener;
import androidx.annotation.NonNull;
import io.github.staernid.sunbeam.nvstream.NvConnection;
import io.github.staernid.sunbeam.stream.DefaultStreamConnectivityDriver;
import io.github.staernid.sunbeam.stream.StreamErrorResolver;
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
import io.github.staernid.sunbeam.profiles.ProfilesManager;
import io.github.staernid.sunbeam.ui.ExternalControllerView;
import io.github.staernid.sunbeam.ui.GameGestures;
import io.github.staernid.sunbeam.ui.StreamContainer;
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
import androidx.preference.PreferenceManager;

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
    private VirtualController virtualController;

    private KeyBoardController keyBoardController;

    private KeyBoardLayoutController keyBoardLayoutController;

    private PreferenceConfiguration prefConfig;
    private SharedPreferences tombstonePrefs;

    private int displayWidth;
    private int displayHeight;
    private int currentOrientation;

    public NvConnection conn;
    private StreamLifecycleManager lifecycleManager;
    private SpinnerDialog spinner;
    public boolean connected = false;
    private boolean autoEnterPip = false;
    private boolean surfaceCreated = false;
    private int suppressPipRefCount = 0;
    private String pcName;
    private String appName;
    private NvApp app;
    private float desiredRefreshRate;

    private InputCaptureProvider inputCaptureProvider;
    private KeyboardInputHandler keyboardInputHandler;
    private PointerInputHandler pointerInputHandler;
    private boolean grabbedInput = true;
    private boolean cursorVisible = false;
    private boolean isPanZoomMode = false;
    private StreamContainer streamContainer;

    private boolean quitOnStop = false;
    private boolean isHidingOverlays;
    private boolean floatingButtonShown;
    private boolean overlayToggleZoomButtonShown;
    private TextView notificationOverlayView;
    private int requestedNotificationOverlayVisibility = View.GONE;
    private View performanceOverlayView;

    private TextView performanceOverlayLite;

    private TextView performanceOverlayBig;

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
    private ImageButton floatingMenuButton;
    private ImageButton overlayToggleButton;
    private float floatingButtonDX, floatingButtonDY;
    private boolean isButtonMoving = false;
    private static final float CLICK_ACTION_THRESHOLD = 5;
    private float floatingButtonStartX, floatingButtonStartY;

    // Zoom button drag state
    private float zoomButtonDX, zoomButtonDY;
    private boolean isZoomButtonMoving = false;
    private float zoomButtonStartX, zoomButtonStartY;

    // Queue for batching commitText payloads
    private static final int UTF8_CHUNK_SIZE = 512;
    private final Queue<String> commitTextQueue = new ArrayDeque<>();
    private final Handler commitTextHandler = new Handler(Looper.getMainLooper());

    private final Runnable flushCommitTextQueue = new Runnable() {
        @Override
        public void run() {
            if (commitTextQueue.isEmpty()) {
                return;
            }
            String chunk = commitTextQueue.poll();
            if (conn != null) {
                conn.sendUtf8Text(chunk);
            }
            if (!commitTextQueue.isEmpty()) {
                commitTextHandler.postDelayed(this, 15);
            }
        }
    };

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
                && prefConfig.renderMode == 0 // For 3D we want to maintain configured resolution
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
            if (prefConfig.renderMode != 0) {
                prefConfig.videoScaleMode = PreferenceConfiguration.ScaleMode.STRETCH;
            }

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
            setPreferredOrientationForActivity();
        }


        if (
                prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.STRETCH ||
                        shouldIgnoreInsetsForResolution(displayWidth, displayHeight)
        ) {
            // Allow the activity to layout under notches if the fill-screen option
            // was turned on by the user or it's a full-screen native resolution
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
        }

        //光标是否显示
        cursorVisible = prefConfig.enableMouseLocalCursor;

        // Listen for non-touch events on the game surface
        streamContainer = findViewById(R.id.streamContainer);
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

        notificationOverlayView = findViewById(R.id.notificationOverlay);

        performanceOverlayView = findViewById(R.id.performanceOverlay);

        performanceOverlayLite = findViewById(R.id.performanceOverlayLite);

        performanceOverlayBig = findViewById(R.id.performanceOverlayBig);

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
        boolean willStreamHdr = false;
        if (prefConfig.enableHdr) {
            if (onExternelDisplay) {
                // Enforce HDR on unsupported hardware can still enable 10bit streaming for better quality
                willStreamHdr = true;
            } else {
                // Start our HDR checklist
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Display.HdrCapabilities hdrCaps = currentDisplay.getHdrCapabilities();

                    // We must now ensure our display is compatible with HDR10
                    if (hdrCaps != null) {
                        // getHdrCapabilities() returns null on Lenovo Lenovo Mirage Solo (vega), Android 8.0
                        for (int hdrType : hdrCaps.getSupportedHdrTypes()) {
                            if (hdrType == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                                willStreamHdr = true;
                                break;
                            }
                        }
                    }

                    if (!willStreamHdr) {
                        // Nope, no HDR for us :(
                        Toast.makeText(this, "Display does not support HDR10", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(this, "HDR requires Android 7.0 or later", Toast.LENGTH_LONG).show();
                }
            }
        }

        // Check if the user has enabled performance stats overlay
        if (prefConfig.enablePerfOverlay) {
            performanceOverlayView.setVisibility(View.VISIBLE);
            if (prefConfig.enablePerfOverlayLite) {
                performanceOverlayLite.setVisibility(View.VISIBLE);
                if(prefConfig.enablePerfOverlayLiteDialog){
                    performanceOverlayLite.setOnClickListener(v -> showGameMenu(null));
                }
            } else {
                performanceOverlayBig.setVisibility(View.VISIBLE);
            }
            if (prefConfig.enablePerfOverlayBottom) {
                //performanceOverlayView.getLayoutParams().layout_gravity = Gravity.BOTTOM;
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) performanceOverlayView.getLayoutParams();
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                performanceOverlayView.setLayoutParams(params);
            }
        }

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

// --- Force tight thresholds (prefConfig.forceTightThresholds) ---
        try {
            boolean forceTight = false;
            if (prefConfig != null) {
                try {
                    java.lang.reflect.Field f = prefConfig.getClass().getDeclaredField("forceTightThresholds");
                    f.setAccessible(true);
                    Object v = f.get(prefConfig);
                    if (v instanceof Boolean) forceTight = (Boolean) v;
                } catch (Throwable ignored) {}
            }
            try { decoderRenderer.setForceTightThresholds(forceTight); } catch (Throwable ignored) {}
            if (forceTight) {
                LimeLog.info("ForceTightThresholds enabled: using vsync-based thresholds on all devices");
            }
        } catch (Throwable ignored) {}

// --- latency profile selection ---
        try {
            if (prefConfig != null && prefConfig.preferLowerDelays) {
                // Intermediate: more responsive than Balanced but not 0 µs
                decoderRenderer.setPreferLowerDelays(true);
                decoderRenderer.setPreferLowerDelaysTimeoutUs(500);  // 0.5 ms
                prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                LimeLog.info("PreferLowerDelays: preferLowerDelays=true, timeout=500us, pacing=BALANCED");
            } else {
                // Balanced default
                decoderRenderer.setPreferLowerDelays(false);
                decoderRenderer.setPreferLowerDelaysTimeoutUs(2000); // 2 ms
                prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                LimeLog.info("Balanced: preferLowerDelays=false, timeout=2000us, pacing=BALANCED");
            }
        } catch (Throwable ignored) {}

// Don't stream HDR if the decoder can't support it
        if (willStreamHdr && !decoderRenderer.isHevcMain10Hdr10Supported() && !decoderRenderer.isAv1Main10Supported()) {
            willStreamHdr = false;
            Toast.makeText(this, "Decoder does not support HDR10 profile", Toast.LENGTH_LONG).show();
        }
        // Display a message to the user if HEVC was forced on but we still didn't find a decoder
        if (prefConfig.videoFormat == PreferenceConfiguration.FormatOption.FORCE_HEVC && !decoderRenderer.isHevcSupported()) {
            Toast.makeText(this, "No HEVC decoder found", Toast.LENGTH_LONG).show();
        }

        // Display a message to the user if AV1 was forced on but we still didn't find a decoder
        if (prefConfig.videoFormat == PreferenceConfiguration.FormatOption.FORCE_AV1 && !decoderRenderer.isAv1Supported()) {
            Toast.makeText(this, "No AV1 decoder found", Toast.LENGTH_LONG).show();
        }

        // H.264 is always supported
        int supportedVideoFormats = MoonBridge.VIDEO_FORMAT_H264;
        if (decoderRenderer.isHevcSupported()) {
            supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_H265;
            if (willStreamHdr && decoderRenderer.isHevcMain10Hdr10Supported()) {
                supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_H265_MAIN10;
            }
        }
        if (decoderRenderer.isAv1Supported()) {
            supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN8;
            if (willStreamHdr && decoderRenderer.isAv1Main10Supported()) {
                supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN10;
            }
        }

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
        float displayRefreshRate = prepareDisplayForRendering(currentDisplay);
        LimeLog.info("Display refresh rate: "+displayRefreshRate);

        // If the user requested frame pacing using a capped FPS, we will need to change our
        // desired FPS setting here in accordance with the active display refresh rate.
        int roundedRefreshRate = Math.round(displayRefreshRate);
        float chosenFrameRate = prefConfig.fps;
        if (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS) {
            if (prefConfig.fps >= roundedRefreshRate) {
                if (prefConfig.fps > roundedRefreshRate + 3) {
                    // Use frame drops when rendering above the screen frame rate
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                    LimeLog.info("Using drop mode for FPS > Hz");
                } else if (roundedRefreshRate <= 49) {
                    // Let's avoid clearly bogus refresh rates and fall back to legacy rendering
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                    LimeLog.info("Bogus refresh rate: " + roundedRefreshRate);
                }
                else {
                    chosenFrameRate = roundedRefreshRate - 1;
                    LimeLog.info("Adjusting FPS target for screen to " + chosenFrameRate);
                }
            }
        }

        if (prefConfig.framePacingWarpFactor > 0) {
            chosenFrameRate *= prefConfig.framePacingWarpFactor;
        }

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

        pointerInputHandler = new PointerInputHandler(
                conn,
                prefConfig,
                inputCaptureProvider,
                streamContainer,
                this,
                controllerHandler,
                () -> grabbedInput,
                () -> virtualController != null && (
                        virtualController.getControllerMode() == VirtualController.ControllerMode.MoveButtons ||
                        virtualController.getControllerMode() == VirtualController.ControllerMode.ResizeButtons
                ),
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

        if (prefConfig.onscreenController) {
            // create virtual onscreen controller
            if (prefConfig.hideOSCWhenHasGamepad) {
                if (!controllerHandler.hasController()) {
                    initVirtualController();
                }
            } else {
                initVirtualController();
            }
        }

        //特殊按键屏幕布局
        if(prefConfig.enableKeyboard){
            initKeyboardController();
        }

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

        gameMenuCallbacks = new GameMenu(this);

        floatingMenuButton = findViewById(R.id.floatingMenuButton);
        updateFloatingButtonVisibility(prefConfig.enableBackMenu && prefConfig.enableFloatingButton);
        initFloatingButton();

        overlayToggleButton = findViewById(R.id.overlayToggleZoomButton);
        setupOverlayToggleButton();

        //fixed size + pacing without back-pressure on MTK
        try {
            View root = findViewById(android.R.id.content);
            // Niente getIdentifier: troviamo la prima SurfaceView nel layout
            SurfaceView streamSurfaceView = findFirstSurfaceViewFrom(root);

            if (streamSurfaceView != null) {
                // Avoid resizes/glitches that break the compositor
                int vw = (prefConfig != null && prefConfig.width > 0) ? prefConfig.width : displayWidth;
                int vh = (prefConfig != null && prefConfig.height > 0) ? prefConfig.height : displayHeight;
                try { streamSurfaceView.getHolder().setFixedSize(vw, vh); } catch (Throwable ignored) {}
                try { streamSurfaceView.setZOrderOnTop(false); } catch (Throwable ignored) {}
                try { streamSurfaceView.setZOrderMediaOverlay(false); } catch (Throwable ignored) {}

                // 2) setFrameRate via reflection (compat < 30)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    float displayHz = 60f;
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            displayHz = currentDisplay.getMode().getRefreshRate();
                        } else {
                            displayHz = currentDisplay.getRefreshRate();
                        }
                    } catch (Throwable ignored) {}

                    float targetFps = (prefConfig != null && prefConfig.fps > 0) ? prefConfig.fps : displayHz;

                    boolean isMTKDevice;
                    try {
                        String sum = (android.os.Build.MANUFACTURER + " " + android.os.Build.HARDWARE + " " + android.os.Build.BOARD)
                                .toLowerCase(java.util.Locale.US);
                        isMTKDevice = sum.contains("mtk") || sum.contains("mediatek");
                    } catch (Throwable t) { isMTKDevice = false; }

                    int compat = isMTKDevice
                            ? Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
                            : Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE;

                    try {
                        java.lang.reflect.Method m = SurfaceView.class.getMethod("setFrameRate", float.class, int.class);
                        m.invoke(streamSurfaceView, Math.min(targetFps, displayHz), compat);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupOverlayToggleButton() {
        if (overlayToggleButton != null) {
            if (prefConfig.showOverlayZoomToggleButton) {
                overlayToggleButton.setVisibility(View.VISIBLE);

                // Set initial appearance based on current state
                updateZoomButtonAppearance();

                // Touch listener for drag and click
                overlayToggleButton.setOnTouchListener((view, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            zoomButtonStartX = event.getRawX();
                            zoomButtonStartY = event.getRawY();
                            zoomButtonDX = view.getX() - event.getRawX();
                            zoomButtonDY = view.getY() - event.getRawY();
                            isZoomButtonMoving = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float newX = event.getRawX() + zoomButtonDX;
                            float newY = event.getRawY() + zoomButtonDY;

                            // Check if it's a move or just a tap
                            if (Math.abs(event.getRawX() - zoomButtonStartX) > CLICK_ACTION_THRESHOLD ||
                                    Math.abs(event.getRawY() - zoomButtonStartY) > CLICK_ACTION_THRESHOLD) {
                                isZoomButtonMoving = true;
                            }

                            // Ensure the button stays within screen bounds
                            if (newX < 0) newX = 0;
                            if (newY < 0) newY = 0;

                            int maxOffsetX = getWindow().getDecorView().getWidth() - view.getWidth();
                            if (newX > maxOffsetX) {
                                newX = maxOffsetX;
                            }

                            int maxOffsetY = getWindow().getDecorView().getHeight() - view.getHeight();
                            if (newY > maxOffsetY) {
                                newY = maxOffsetY;
                            }

                            view.setX(newX);
                            view.setY(newY);
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (!isZoomButtonMoving) {
                                // It's a click event, toggle zoom mode
                                toggleZoomMode();
                                updateZoomButtonAppearance();
                            }
                            isZoomButtonMoving = false;
                            return true;
                        default:
                            return false;
                    }
                });
            } else {
                overlayToggleButton.setVisibility(View.GONE);
            }
        }
    }

    private void updateZoomButtonAppearance() {
        if (overlayToggleButton != null) {
            // Change background based on pan/zoom mode state
            overlayToggleButton.setBackgroundResource(isPanZoomMode ?
                    R.drawable.floating_menu_button_active : R.drawable.floating_menu_button);
            // No need for alpha changes since the color indicates the state
            overlayToggleButton.setAlpha(1.0f);
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

    @SuppressLint("ClickableViewAccessibility")
    private void initFloatingButton() {
        // Touch listener for drag and click
        if (floatingMenuButton != null) {
            floatingMenuButton.setOnTouchListener((view, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        floatingButtonStartX = event.getRawX();
                        floatingButtonStartY = event.getRawY();
                        floatingButtonDX = view.getX() - event.getRawX();
                        floatingButtonDY = view.getY() - event.getRawY();
                        isButtonMoving = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + floatingButtonDX;
                        float newY = event.getRawY() + floatingButtonDY;

                        // Check if it's a move or just a tap
                        if (Math.abs(event.getRawX() - floatingButtonStartX) > CLICK_ACTION_THRESHOLD ||
                                Math.abs(event.getRawY() - floatingButtonStartY) > CLICK_ACTION_THRESHOLD) {
                            isButtonMoving = true;
                        }

                        // Ensure the button stays within screen bounds
                        if (newX < 0) newX = 0;
                        if (newY < 0) newY = 0;

                        int maxOffsetX = getWindow().getDecorView().getWidth() - view.getWidth();
                        if (newX > maxOffsetX) {
                            newX = maxOffsetX;
                        }

                        int maxOffsetY = getWindow().getDecorView().getHeight() - view.getHeight();
                        if (newY > maxOffsetY) {
                            newY = maxOffsetY;
                        }

                        view.setX(newX);
                        view.setY(newY);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!isButtonMoving) {
                            // It's a click event, show menu
                            showGameMenu(null);
                        }
                        isButtonMoving = false;
                        return true;
                    default:
                        return false;
                }
            });
        }
    }

    private void initKeyboardController(){
        keyBoardController = new KeyBoardController(conn,(FrameLayout)rootView, this);
        keyBoardController.refreshLayout();
        keyBoardController.show();
    }

    public Boolean isKeyboardLayoutVisible() {
        return keyBoardLayoutController != null && keyBoardLayoutController.shown;
    }

    private void initVirtualController(){
        virtualController = new VirtualController(controllerHandler, (FrameLayout)rootView, this);
        virtualController.refreshLayout();
        virtualController.show();
    }

    private void initkeyBoardLayoutController(){
        keyBoardLayoutController = new KeyBoardLayoutController((FrameLayout)rootView, this, prefConfig);
        keyBoardLayoutController.refreshLayout();
        keyBoardLayoutController.show();
    }

    //显示隐藏虚拟特殊按键
    public void toggleKeyboardController(){
        if (keyBoardController==null) {
            initKeyboardController();
            return;
        }
        keyBoardController.toggleVisibility();
    }

    public void toggleFullKeyboard() {
        if (isOnExternalDisplay()) {
            ExternalDisplayControlActivity.toggleFullKeyboard();
            return;
        }
        if (keyBoardLayoutController == null) {
            initkeyBoardLayoutController();
            return;
        }
        keyBoardLayoutController.toggleVisibility();
    }

    //显示隐藏虚拟手柄控制器
    public void toggleVirtualController(){
        if (virtualController==null) {
            initVirtualController();
            prefConfig.onscreenController=true;
            return;
        }
        prefConfig.onscreenController= virtualController.switchShowHide() != 0;
    }

    private void setPreferredOrientationForActivity() {
        Display display = getActiveDisplay(Game.this, prefConfig);

        // For semi-square displays, we use more complex logic to determine which orientation to use (if any)
        if (PreferenceConfiguration.isSquarishScreen(display)) {
            int desiredOrientation = Configuration.ORIENTATION_UNDEFINED;

            // OSC doesn't properly support portrait displays, so don't use it in portrait mode by default
            if (prefConfig.onscreenController) {
                desiredOrientation = Configuration.ORIENTATION_LANDSCAPE;
            }

            // For native resolution, we will lock the orientation to the one that matches the specified resolution
            if (PreferenceConfiguration.isNativeResolution(prefConfig.width, prefConfig.height)) {
                if (displayWidth > displayHeight) {
                    desiredOrientation = Configuration.ORIENTATION_LANDSCAPE;
                }
                else {
                    desiredOrientation = Configuration.ORIENTATION_PORTRAIT;
                }
            }

            if (desiredOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            }
            else if (desiredOrientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            }
            else {
                // If we don't have a reason to lock to portrait or landscape, allow any orientation
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
            }
        }
        else {
            // Lock to current orientation
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Set requested orientation for possible new screen size
        setPreferredOrientationForActivity();

        if (virtualController != null) {
            // Refresh layout of OSC for possible new screen size
            virtualController.refreshLayout();
        }

        if(keyBoardController != null){
            keyBoardController.refreshLayout();
        }

        if(keyBoardLayoutController != null){
            keyBoardLayoutController.refreshLayout();
        }

        // Hide on-screen overlays in PiP mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isInPictureInPictureMode()) {
                isHidingOverlays = true;

                floatingButtonShown = floatingMenuButton.isShown();

                if (floatingButtonShown) {
                    floatingMenuButton.setVisibility(View.GONE);
                }

                overlayToggleZoomButtonShown = overlayToggleButton != null && overlayToggleButton.isShown();

                if (overlayToggleZoomButtonShown) {
                    overlayToggleButton.setVisibility(View.GONE);
                }

                if (virtualController != null) {
                    virtualController.hide();
                }

                if (keyBoardController != null && keyBoardController.shown) {
                    keyBoardController.hide(true);
                }

                if (keyBoardLayoutController!=null && keyBoardLayoutController.shown) {
                    keyBoardLayoutController.hide(true);
                }

                hideGameMenu();

                performanceOverlayView.setVisibility(View.GONE);
                notificationOverlayView.setVisibility(View.GONE);

                // Disable sensors while in PiP mode
                controllerHandler.disableSensors();

                // Update GameManager state to indicate we're in PiP (still gaming, but interruptible)
                UiHelper.notifyStreamEnteringPiP(this);
            }
            else {
                isHidingOverlays = false;

                if (floatingButtonShown) {
                    floatingMenuButton.setVisibility(View.VISIBLE);
                }

                if (overlayToggleZoomButtonShown) {
                    overlayToggleButton.setVisibility(View.VISIBLE);
                }

                // Restore overlays to previous state when leaving PiP

                if (virtualController != null) {
                    virtualController.show();
                }

                if (keyBoardController != null && keyBoardController.shown) {
                    keyBoardController.show();
                }

                if(keyBoardLayoutController!=null && keyBoardLayoutController.shown){
                    keyBoardLayoutController.show();
                }

                if (prefConfig.enablePerfOverlay) {
                    performanceOverlayView.setVisibility(View.VISIBLE);
                }

                notificationOverlayView.setVisibility(requestedNotificationOverlayVisibility);

                // Enable sensors again after exiting PiP
                controllerHandler.enableSensors();

                // Update GameManager state to indicate we're out of PiP (gaming, non-interruptible)
                UiHelper.notifyStreamExitingPiP(this);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private PictureInPictureParams getPictureInPictureParams(boolean autoEnter) {
        View view;
        Rect hint;
        if (prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.FIT && streamContainer.getScaleX() == 1) {
            view = streamContainer;
        } else {
            view = (View)rootView;
        }

        int[] viewLocation = new int[2];

        view.getLocationOnScreen(viewLocation);

        int left = viewLocation[0];
        int top = viewLocation[1];
        int width = view.getWidth();
        int height = view.getHeight();
        Rational aspectRatio = new Rational(width, height);
        hint = new Rect(left, top, left + width, top + height);

        PictureInPictureParams.Builder builder =
                new PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .setSourceRectHint(hint);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter);
            builder.setSeamlessResizeEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (appName != null) {
                builder.setTitle(appName);
                if (pcName != null) {
                    builder.setSubtitle(pcName);
                }
            }
            else if (pcName != null) {
                builder.setTitle(pcName);
            }
        }

        return builder.build();
    }

    public void updatePipAutoEnter() {
        if (!prefConfig.enablePip || isOnExternalDisplay()) {
            return;
        }

        boolean autoEnter = connected && suppressPipRefCount == 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setPictureInPictureParams(getPictureInPictureParams(autoEnter));
        }
        else {
            autoEnterPip = autoEnter;
        }
    }

    public void setMetaKeyCaptureState(boolean enabled) {
        // Android has native keyboard capture support starting in API 36.1
        boolean keyboardCaptureSupported = false;
        try {
            WindowManager.LayoutParams windowLayoutParams = getWindow().getAttributes();
            java.lang.reflect.Method setCaptureMethod = windowLayoutParams.getClass().getMethod("setKeyboardCaptureEnabled", boolean.class);
            setCaptureMethod.invoke(windowLayoutParams, enabled);
            getWindow().setAttributes(windowLayoutParams);
            keyboardCaptureSupported = true;
        } catch (Throwable ignored) {
        }

        if (!keyboardCaptureSupported) {
            // This uses custom APIs present on some Samsung devices to allow capture of
            // meta key events while streaming.
            try {
                Class<?> semWindowManager = Class.forName("com.samsung.android.view.SemWindowManager");
                Method getInstanceMethod = semWindowManager.getMethod("getInstance");
                Object manager = getInstanceMethod.invoke(null);

                if (manager != null) {
                    Class<?>[] parameterTypes = new Class<?>[2];
                    parameterTypes[0] = ComponentName.class;
                    parameterTypes[1] = boolean.class;
                    Method requestMetaKeyEventMethod = semWindowManager.getDeclaredMethod("requestMetaKeyEvent", parameterTypes);
                    requestMetaKeyEventMethod.invoke(manager, this.getComponentName(), enabled);
                }
                else {
                    LimeLog.warning("SemWindowManager.getInstance() returned null");
                }
            } catch (ClassNotFoundException ignored) {
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();

        // PiP is only supported on Oreo and later, and we don't need to manually enter PiP on
        // Android S and later. On Android R, we will use onPictureInPictureRequested() instead.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (autoEnterPip && !isOnExternalDisplay()) {
                try {
                    // This has thrown all sorts of weird exceptions on Samsung devices
                    // running Oreo. Just eat them and close gracefully on leave, rather
                    // than crashing.
                    enterPictureInPictureMode(getPictureInPictureParams(false));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.R)
    public boolean onPictureInPictureRequested() {
        // Enter PiP when requested unless we're on Android 12 which supports auto-enter.
        if (autoEnterPip && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            enterPictureInPictureMode(getPictureInPictureParams(false));
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

    private boolean isRefreshRateEqualMatch(float refreshRate) {
        return refreshRate >= prefConfig.fps &&
                refreshRate <= prefConfig.fps + 3;
    }

    private boolean isRefreshRateGoodMatch(float refreshRate) {
        return refreshRate >= prefConfig.fps &&
                Math.round(refreshRate) % prefConfig.fps <= 3;
    }

    private boolean shouldIgnoreInsetsForResolution(int width, int height) {
        // Never ignore insets for non-native resolutions
        if (!PreferenceConfiguration.isNativeResolution(width, height)) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display display = getActiveDisplay(Game.this, prefConfig);
            for (Display.Mode candidate : display.getSupportedModes()) {
                // Ignore insets if this is an exact match for the display resolution
                if ((width == candidate.getPhysicalWidth() && height == candidate.getPhysicalHeight()) ||
                        (height == candidate.getPhysicalWidth() && width == candidate.getPhysicalHeight())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean mayReduceRefreshRate() {
        return prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS ||
                prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS ||
                (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_BALANCED && prefConfig.reduceRefreshRate);
    }

    public boolean isOnExternalDisplay() {
        return onExternelDisplay;
    }

    private float prepareDisplayForRendering(Display currentDisplay) {
        WindowManager.LayoutParams windowLayoutParams = getWindow().getAttributes();
        float displayRefreshRate;

        // On M, we can explicitly set the optimal display mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode bestMode = currentDisplay.getMode();
            boolean isNativeResolutionStream = PreferenceConfiguration.isNativeResolution(prefConfig.width, prefConfig.height);
            boolean refreshRateIsGood = isRefreshRateGoodMatch(bestMode.getRefreshRate());
            boolean refreshRateIsEqual = isRefreshRateEqualMatch(bestMode.getRefreshRate());

            LimeLog.info("Current display mode: "+bestMode.getPhysicalWidth()+"x"+
                    bestMode.getPhysicalHeight()+"x"+bestMode.getRefreshRate());

            for (Display.Mode candidate : currentDisplay.getSupportedModes()) {
                boolean refreshRateReduced = candidate.getRefreshRate() < bestMode.getRefreshRate();
                boolean resolutionReduced = candidate.getPhysicalWidth() < bestMode.getPhysicalWidth() ||
                        candidate.getPhysicalHeight() < bestMode.getPhysicalHeight();
                boolean resolutionFitsStream = candidate.getPhysicalWidth() >= prefConfig.width &&
                        candidate.getPhysicalHeight() >= prefConfig.height;

                LimeLog.info("Examining display mode: "+candidate.getPhysicalWidth()+"x"+
                        candidate.getPhysicalHeight()+"x"+candidate.getRefreshRate());

                if (candidate.getPhysicalWidth() > 4096 && prefConfig.width <= 4096) {
                    // Avoid resolutions options above 4K to be safe
                    continue;
                }

                // On non-4K streams, we force the resolution to never change unless it's above
                // 60 FPS, which may require a resolution reduction due to HDMI bandwidth limitations,
                // or it's a native resolution stream.
                if (prefConfig.width < 3840 && prefConfig.fps <= 60 && !isNativeResolutionStream) {
                    if (currentDisplay.getMode().getPhysicalWidth() != candidate.getPhysicalWidth() ||
                            currentDisplay.getMode().getPhysicalHeight() != candidate.getPhysicalHeight()) {
                        continue;
                    }
                }

                // Make sure the resolution doesn't regress unless if it's over 60 FPS
                // where we may need to reduce resolution to achieve the desired refresh rate.
                if (resolutionReduced && !(prefConfig.fps > 60 && resolutionFitsStream)) {
                    continue;
                }

                if (mayReduceRefreshRate() && refreshRateIsEqual && !isRefreshRateEqualMatch(candidate.getRefreshRate())) {
                    // If we had an equal refresh rate and this one is not, skip it. In min latency
                    // mode, we want to always prefer the highest frame rate even though it may cause
                    // microstuttering.
                    continue;
                }
                else if (refreshRateIsGood) {
                    // We've already got a good match, so if this one isn't also good, it's not
                    // worth considering at all.
                    if (!isRefreshRateGoodMatch(candidate.getRefreshRate())) {
                        continue;
                    }

                    if (mayReduceRefreshRate()) {
                        // User asked for the lowest possible refresh rate, so don't raise it if we
                        // have a good match already
                        if (candidate.getRefreshRate() > bestMode.getRefreshRate()) {
                            continue;
                        }
                    }
                    else {
                        // User asked for the highest possible refresh rate, so don't reduce it if we
                        // have a good match already
                        if (refreshRateReduced) {
                            continue;
                        }
                    }
                }
                else if (!isRefreshRateGoodMatch(candidate.getRefreshRate())) {
                    // We didn't have a good match and this match isn't good either, so just don't
                    // reduce the refresh rate.
                    if (refreshRateReduced) {
                        continue;
                    }
                } else {
                    // We didn't have a good match and this match is good. Prefer this refresh rate
                    // even if it reduces the refresh rate. Lowering the refresh rate can be beneficial
                    // when streaming a 60 FPS stream on a 90 Hz device. We want to select 60 Hz to
                    // match the frame rate even if the active display mode is 90 Hz.
                }

                bestMode = candidate;
                refreshRateIsGood = isRefreshRateGoodMatch(candidate.getRefreshRate());
                refreshRateIsEqual = isRefreshRateEqualMatch(candidate.getRefreshRate());
            }

            LimeLog.info("Best display mode: "+bestMode.getPhysicalWidth()+"x"+
                    bestMode.getPhysicalHeight()+"x"+bestMode.getRefreshRate());

            // Only apply new window layout parameters if we've actually changed the display mode
            if (currentDisplay.getMode().getModeId() != bestMode.getModeId()) {
                // If we only changed refresh rate and we're on an OS that supports Surface.setFrameRate()
                // use that instead of using preferredDisplayModeId to avoid the possibility of triggering
                // bugs that can cause the system to switch from 4K60 to 4K24 on Chromecast 4K.
                if (prefConfig.enforceDisplayMode ||
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        currentDisplay.getMode().getPhysicalWidth() != bestMode.getPhysicalWidth() ||
                        currentDisplay.getMode().getPhysicalHeight() != bestMode.getPhysicalHeight()) {
                    // Apply the display mode change
                    windowLayoutParams.preferredDisplayModeId = bestMode.getModeId();
                    getWindow().setAttributes(windowLayoutParams);
                }
                else {
                    LimeLog.info("Using setFrameRate() instead of preferredDisplayModeId due to matching resolution");
                }
            }
            else {
                LimeLog.info("Current display mode is already the best display mode");
            }

            displayRefreshRate = bestMode.getRefreshRate();
        }
        // On L, we can at least tell the OS that we want a refresh rate
        else {
            float bestRefreshRate = currentDisplay.getRefreshRate();
            for (float candidate : currentDisplay.getSupportedRefreshRates()) {
                LimeLog.info("Examining refresh rate: "+candidate);

                if (candidate > bestRefreshRate) {
                    // Ensure the frame rate stays around 60 Hz for <= 60 FPS streams
                    if (prefConfig.fps <= 60) {
                        if (candidate >= 63) {
                            continue;
                        }
                    }

                    bestRefreshRate = candidate;
                }
            }

            LimeLog.info("Selected refresh rate: "+bestRefreshRate);
            windowLayoutParams.preferredRefreshRate = bestRefreshRate;
            displayRefreshRate = bestRefreshRate;

            // Apply the refresh rate change
            getWindow().setAttributes(windowLayoutParams);
        }

        // Until Marshmallow, we can't ask for a 4K display mode, so we'll
        // need to hint the OS to provide one.
        boolean aspectRatioMatch = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // We'll calculate whether we need to scale by aspect ratio. If not, we'll use
            // setFixedSize so we can handle 4K properly. The only known devices that have
            // >= 4K screens have exactly 4K screens, so we'll be able to hit this good path
            // on these devices. On Marshmallow, we can start changing to 4K manually but no
            // 4K devices run 6.0 at the moment.
            Point screenSize = new Point(0, 0);
            currentDisplay.getSize(screenSize);

            double screenAspectRatio = ((double)screenSize.y) / screenSize.x;
            double streamAspectRatio = ((double)displayHeight) / displayWidth;
            if (Math.abs(screenAspectRatio - streamAspectRatio) < 0.001|| isOnExternalDisplay()) {
                LimeLog.info("Stream has compatible aspect ratio with output display");
                aspectRatioMatch = true;
            }
        }

        // Don't do setFixedSize since it might not update the view dimensions correctly when entering PiP mode
        if (!(prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.STRETCH || aspectRatioMatch)) {
            // Set the surface to scale based on the aspect ratio of the stream
            streamContainer.setDesiredAspectRatio((double)displayWidth / (double)displayHeight);
            streamContainer.setFillDisplay(prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.FILL);
            LimeLog.info("surfaceChanged-->"+(double)displayWidth / (double)displayHeight);
            LimeLog.info("scaleMode-->"+prefConfig.videoScaleMode);
        }

        // Set the desired refresh rate that will get passed into setFrameRate() later
        desiredRefreshRate = displayRefreshRate;

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                || isOnExternalDisplay()) {// TVs may take a few moments to switch refresh rates, and we can probably assume
            // it will be eventually activated.
            // external displays cant be compared with displaymanager currents display refreshrate
            // TODO: Improve this
            return displayRefreshRate;
        }
        else {
            // Use the lower of the current refresh rate and the selected refresh rate.
            // The preferred refresh rate may not actually be applied (ex: Battery Saver mode).
            return Math.min(currentDisplay.getRefreshRate(), displayRefreshRate);
        }
    }

    @SuppressLint("InlinedApi")
    private final Runnable hideSystemUi = new Runnable() {
        @Override
        public void run() {
            // TODO: Do we want to use WindowInsetsController here on R+ instead of
            // SYSTEM_UI_FLAG_IMMERSIVE_STICKY? They seem to do the same thing as of S...

            // In multi-window mode on N+, we need to drop our layout flags or we'll
            // be drawing underneath the system UI.
            if (!prefConfig.fullScreen || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode())) {
                Game.this.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
            else {
                // Use immersive mode
                Game.this.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    };

    private void hideSystemUi(int delay) {
        Handler h = getWindow().getDecorView().getHandler();
        if (h != null) {
            h.removeCallbacks(hideSystemUi);
            h.postDelayed(hideSystemUi, delay);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);

        // In multi-window, we don't want to use the full-screen layout
        // flag. It will cause us to collide with the system UI.
        // This function will also be called for PiP so we can cover
        // that case here too.
        if (isInMultiWindowMode) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decoderRenderer.notifyVideoBackground();
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decoderRenderer.notifyVideoForeground();
        }

        // Correct the system UI visibility flags
        hideSystemUi(50);
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
            SharedPreferences basePrefs = PreferenceManager.getDefaultSharedPreferences(this);
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

        if (virtualController != null) {
            virtualController.hide();
        }
        if (keyBoardController != null) {
            keyBoardController.hide();
        }

        if(keyBoardLayoutController!=null){
            keyBoardLayoutController.hide();
        }

        if (conn != null) {
            int videoFormat = decoderRenderer.getActiveVideoFormat();

            if (lifecycleManager != null) {
                lifecycleManager.setHasDisplayedFailureDialog(true);
            }
            stopConnection();
            String message = null;
            String selectedVideoFormat = "";

            int averageEndToEndLat = decoderRenderer.getAverageEndToEndLatency();
            int averageDecoderLat = decoderRenderer.getAverageDecoderLatency();

            if (averageEndToEndLat > 0) {
                message = getResources().getString(R.string.conn_client_latency) + " " + averageEndToEndLat + " ms";
                if (averageDecoderLat > 0) {
                    message += " (" + getResources().getString(R.string.conn_client_latency_hw) + " " + averageDecoderLat + " ms)";
                }
            } else if (averageDecoderLat > 0) {
                message = getResources().getString(R.string.conn_hardware_latency) + " " + averageDecoderLat + " ms";
            }

            // Add the video codec to the post-stream toast
            selectedVideoFormat += " [";

            if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_H264) != 0) {
                selectedVideoFormat += "H.264";
            } else if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_H265) != 0) {
                selectedVideoFormat += "HEVC";
            } else if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_AV1) != 0) {
                selectedVideoFormat += "AV1";
            }
            else {
                selectedVideoFormat += "UNKNOWN";
            }

            if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_10BIT) != 0) {
                selectedVideoFormat += " HDR";
            }

            selectedVideoFormat += "]";

            if (message != null) {
                message += selectedVideoFormat;
            }

            if (message != null) {
                if (prefConfig.enableLatencyToast) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }
            }

            // Clear the tombstone count if we terminated normally
            if (!reportedCrash && tombstonePrefs.getInt("CrashCount", 0) != 0) {
                tombstonePrefs.edit()
                        .putInt("CrashCount", 0)
                        .putInt("LastNotifiedCrashCount", 0)
                        .apply();
            }
            if(prefConfig.enablePerfLogging && decoderRenderer.performanceWasTracked()) {
                new PerformanceDataTracker().savePerformanceStatistics(
                        getBaseContext(),
                        Build.MODEL,
                        Build.VERSION.SDK_INT + "",
                        BuildConfig.VERSION_NAME,
                        selectedVideoFormat,
                        decoderRenderer.getMinDecoderLatency(),
                        decoderRenderer.getMinDecoderLatencyFullLog(),
                        String.valueOf((prefConfig.bitrate / 1000)),
                        displayWidth + "x" + displayHeight,
                        prefConfig.fps + " hz",
                        decoderRenderer.getAverageDecoderLatency() + " ms",
                        PreferenceConfiguration.getSelectedFramePacingName(getBaseContext()),
                        formatCurrentTime(System.currentTimeMillis())
                );
            }

        }

        finish();
    }

    public static String formatCurrentTime(long currentTimeMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = new Date(currentTimeMillis);
        return dateFormat.format(date);
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
        if (connectionStatus == MoonBridge.CONN_STATUS_POOR) {
            if (prefConfig.bitrate > 5000) {
                notificationOverlayView.setText(getResources().getString(R.string.slow_connection_msg));
            } else {
                notificationOverlayView.setText(getResources().getString(R.string.poor_connection_msg));
            }

            requestedNotificationOverlayVisibility = View.VISIBLE;
        } else if (connectionStatus == MoonBridge.CONN_STATUS_OKAY) {
            requestedNotificationOverlayVisibility = View.GONE;
        }

        if (!isHidingOverlays) {
            notificationOverlayView.setVisibility(requestedNotificationOverlayVisibility);
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
        setupOverlayToggleButton();

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
        Toast.makeText(Game.this, getResources().getString(R.string.applist_quit_success) + " " + appName, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAppQuitFailure(@NonNull String message) {
        Toast.makeText(Game.this, message, Toast.LENGTH_LONG).show();
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
        float desiredFrameRate;

        surfaceCreated = true;

        // Android will pick the lowest matching refresh rate for a given frame rate value, so we want
        // to report the true FPS value if refresh rate reduction is enabled. We also report the true
        // FPS value if there's no suitable matching refresh rate. In that case, Android could try to
        // select a lower refresh rate that avoids uneven pull-down (ex: 30 Hz for a 60 FPS stream on
        // a display that maxes out at 50 Hz).
        if (mayReduceRefreshRate() || desiredRefreshRate < prefConfig.fps) {
            desiredFrameRate = prefConfig.fps;
        }
        else {
            // Otherwise, we will pretend that our frame rate matches the refresh rate we picked in
            // prepareDisplayForRendering(). This will usually be the highest refresh rate that our
            // frame rate evenly divides into, which ensures the lowest possible display latency.
            desiredFrameRate = desiredRefreshRate;
        }

        // Tell the OS about our frame rate to allow it to adapt the display refresh rate appropriately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // We want to change frame rate even if it's not seamless, since prepareDisplayForRendering()
            // will not set the display mode on S+ if it only differs by the refresh rate. It depends
            // on us to trigger the frame rate switch here.
            holder.getSurface().setFrameRate(desiredFrameRate,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                    Surface.CHANGE_FRAME_RATE_ALWAYS);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            holder.getSurface().setFrameRate(desiredFrameRate,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(prefConfig.enablePerfOverlayLite){
                    performanceOverlayLite.setText(text);
                }else{
                    performanceOverlayBig.setText(text);
                }
            }
        });
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
        if (this.isPanZoomMode) {
            Toast.makeText(this, getString(R.string.pan_zoom_mode_enabled), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.pan_zoom_mode_disabled), Toast.LENGTH_SHORT).show();
        }
        updateZoomButtonAppearance();

        if (ExternalDisplayControlActivity.instance != null) {
            ExternalDisplayControlActivity.instance.toggleZoomMode(false);
        }
    }

    public void rotateScreen() {
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            currentOrientation = Configuration.ORIENTATION_PORTRAIT;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        } else {
            currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        }
    }

    /**
     * Initializes and applies the appropriate mouse mode based on user preferences
     * and whether the app is running in secondary display mode (such as Samsung DeX).
     *
     * Behavior:
     * - If the app is in secondary display mode:
     *   - Applies the user's saved mouse mode if it's one of the supported modes:
     *     "Trackpad Natural", "Trackpad Gaming", or "Disabled".
     *   - Otherwise, defaults to applying the "Trackpad Natural" mode.
     *
     * - If the app is not in secondary display mode:
     *   - Applies the user's saved mouse mode as is.
     *
     * This ensures the correct input mode is applied depending on the environment,
     * improving compatibility with desktop-like multi-display modes.
     */
    private void initMouseMode() {
        String[] mouseModes = getResources().getStringArray(R.array.mouse_mode_names);

        String savedMouseModeIndexStr = ProfilesManager.getInstance()
                .getOverlayingSharedPreferences(this)
                .getString("mouse_mode_list", "0");

        int savedMouseModeIndex;
        try {
            savedMouseModeIndex = Integer.parseInt(savedMouseModeIndexStr);
        } catch (NumberFormatException e) {
            savedMouseModeIndex = 0;
        }

        String savedMouseModeString = (savedMouseModeIndex >= 0 && savedMouseModeIndex < mouseModes.length)
                ? mouseModes[savedMouseModeIndex]
                : null;

        String natural = getString(R.string.mouse_mode_track_pad_natural);
        String gaming = getString(R.string.mouse_mode_track_pad_gaming);
        String disabled = getString(R.string.mouse_mode_disabled);

        int naturalIndex = 2; //fallback natural mode for secondary screen
        for (int i = 0; i < mouseModes.length; i++) {
            if (mouseModes[i].equals(natural)) {
                naturalIndex = i;
                break;
            }
        }
        // We only want to temporary override the mouse mode to work with external, but not store it
        if (isOnExternalDisplay()) {
            if (savedMouseModeString != null &&
                    (savedMouseModeString.equals(natural) ||
                            savedMouseModeString.equals(gaming) ||
                            savedMouseModeString.equals(disabled))) {
                applyMouseMode(savedMouseModeIndex);
            } else {
                applyMouseMode(naturalIndex);
            }
        } else {
            applyMouseMode(savedMouseModeIndex);
        }
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
    public void selectMouseMode(Context context){
        String[] allModes = getResources().getStringArray(R.array.mouse_mode_names);

        Set<String> allowedLabels = new HashSet<>(Arrays.asList(
                getString(R.string.mouse_mode_track_pad_natural),
                getString(R.string.mouse_mode_track_pad_gaming),
                getString(R.string.mouse_mode_disabled)
        ));

        List<MouseModeOption> options = new ArrayList<>();

        for (int i = 0; i < allModes.length; i++) {
            String label = allModes[i];
            boolean isAllowed = !isOnExternalDisplay() || allowedLabels.contains(label);
            if (isAllowed) {
                options.add(new MouseModeOption(i, label));
            }
        }

        options.add(new MouseModeOption(-1, getString(R.string.toggle_local_mouse_cursor)));

        String[] labels = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            labels[i] = options.get(i).label;
        }
        final MouseModeOption[] optionArray = options.toArray(new MouseModeOption[0]);

        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.game_menu_select_mouse_mode))
                .setItems(labels, (dialog, which) -> {
                    dialog.dismiss();
                    MouseModeOption selected = optionArray[which];
                    if (selected.index == -1) {
                        toggleMouseLocalCursor();
                    } else {
                        applyMouseMode(selected.index);
                        if (prefConfig.rememberMouseMode) {
                            ProfilesManager.getInstance().getOverlayingSharedPreferences(this)
                                    .edit()
                                    .putString("mouse_mode_list", String.valueOf(selected.index))
                                    .apply();
                        }
                    }
                })
                .create()
                .show();
    }

    //本地鼠标光标切换
    private void toggleMouseLocalCursor(){
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

    private void applyMouseMode(int mode) {
        if (pointerInputHandler != null) {
            pointerInputHandler.applyMouseMode(mode);
        }

        // Always exit zoom mode if mouse mode has changed
        isPanZoomMode = false;
        updateZoomButtonAppearance();
    }

    public void toggleHUD() {
        prefConfig.enablePerfOverlay = !prefConfig.enablePerfOverlay;
        if (prefConfig.enablePerfOverlay) {
            performanceOverlayView.setVisibility(View.VISIBLE);
            if(prefConfig.enablePerfOverlayLite){
                performanceOverlayLite.setVisibility(View.VISIBLE);
            }else{
                performanceOverlayBig.setVisibility(View.VISIBLE);
            }
        } else {
            performanceOverlayView.setVisibility(View.GONE);
        }
    }

    //切换触控灵敏度开关
    public void switchTouchSensitivity(){
        prefConfig.enableTouchSensitivity = !prefConfig.enableTouchSensitivity;
    }

    public void disconnect() {
        finish();
    }

    public void quit() {
        Context context;
        if (isOnExternalDisplay() && ExternalDisplayControlActivity.instance != null) {
            context = ExternalDisplayControlActivity.instance;
        } else {
            context = this;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.game_dialog_title_quit_confirm);
        builder.setMessage(R.string.game_dialog_message_quit_confirm);

        builder.setPositiveButton(getString(R.string.yes), (dialog, which) -> {
            quitOnStop = true;
            dialog.dismiss();
            finish();
        });

        builder.setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void showGameMenu(GameInputDevice device) {
        if(isOnExternalDisplay()) {
            ExternalDisplayControlActivity.toggleGameMenu();
        } else {
            if (gameMenuCallbacks != null) {
                gameMenuCallbacks.showMenu(device);
            }
        }
    }

    public void hideGameMenu() {
        if (gameMenuCallbacks != null) {
            gameMenuCallbacks.hideMenu();
        }
    }

    private void updateFloatingButtonVisibility(boolean show) {
        floatingMenuButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void toggleFloatingButtonVisibility() {
        if (floatingMenuButton != null) {
            updateFloatingButtonVisibility(floatingMenuButton.getVisibility() == View.GONE);
        }
    }


    // 设置surfaceView的圆角 setSurfaceviewCorner(UiHelper.dpToPx(this,24));
    private void setSurfaceviewCorner(final float radius) {

        streamContainer.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                Rect rect = new Rect();
                view.getGlobalVisibleRect(rect);
                int leftMargin = 0;
                int topMargin = 0;
                Rect selfRect = new Rect(leftMargin, topMargin, rect.right - rect.left - leftMargin, rect.bottom - rect.top - topMargin);
                outline.setRoundRect(selfRect, radius);
            }
        });
        streamContainer.setClipToOutline(true);
    }

    @Override
    public boolean handleCommitText(CharSequence text) {
        if (!prefConfig.enableCommitText || conn == null) {
            return false;
        }
        enqueueCommitText(text.toString());
        return true;
    }

    @Override
    public boolean handleDeleteSurroundingText(int beforeLength, int afterLength) {
        if (!prefConfig.enableCommitText || conn == null) {
            return false;
        }
        // Send backspace events for deleted preceding characters
        if (beforeLength > 0) {
            short backspaceCode = keyboardTranslator.translate(KeyEvent.KEYCODE_DEL, 0, -1);
            for (int i = 0; i < beforeLength; i++) {
                conn.sendKeyboardInput(backspaceCode, io.github.staernid.sunbeam.nvstream.input.KeyboardPacket.KEY_DOWN, (byte)0, (byte)0);
                conn.sendKeyboardInput(backspaceCode, io.github.staernid.sunbeam.nvstream.input.KeyboardPacket.KEY_UP, (byte)0, (byte)0);
            }
        }
        return true;
    }

    private void enqueueCommitText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        int offset = 0;
        while (offset < utf8.length) {
            int end = Math.min(offset + UTF8_CHUNK_SIZE, utf8.length);
            // Ensure we don't cut inside a multi-byte sequence
            while (end < utf8.length && (utf8[end] & 0xC0) == 0x80) {
                end--; // step back until we are at start of code point
            }
            String chunk = new String(utf8, offset, end - offset, StandardCharsets.UTF_8);
            commitTextQueue.add(chunk);
            offset = end;
        }
        // Kick off flushing if not already scheduled
        if (commitTextQueue.size() == 1) {
            commitTextHandler.post(flushCommitTextQueue);
        }
    }

    /** Helper ricorsivo per trovare la prima SurfaceView nel layout corrente */
    private SurfaceView findFirstSurfaceViewFrom(View v) {
        if (v instanceof SurfaceView) return (SurfaceView) v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                SurfaceView found = findFirstSurfaceViewFrom(g.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

}