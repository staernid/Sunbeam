package io.github.staernid.sunbeam.binding.input;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.BatteryState;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.hardware.lights.LightsManager;
import android.hardware.lights.LightsRequest;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.CombinedVibration;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.widget.Toast;

import io.github.staernid.sunbeam.GameMenu;
import io.github.staernid.sunbeam.LimeLog;
import io.github.staernid.sunbeam.R;
import io.github.staernid.sunbeam.binding.input.driver.AbstractController;
import io.github.staernid.sunbeam.binding.input.driver.UsbDriverListener;
import io.github.staernid.sunbeam.binding.input.driver.UsbDriverService;
import io.github.staernid.sunbeam.nvstream.NvConnection;
import io.github.staernid.sunbeam.nvstream.input.ControllerPacket;
import io.github.staernid.sunbeam.nvstream.input.MouseButtonPacket;
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge;
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration;
import io.github.staernid.sunbeam.ui.GameGestures;
import io.github.staernid.sunbeam.utils.Vector2d;

import org.cgutman.shieldcontrollerextensions.SceChargingState;
import org.cgutman.shieldcontrollerextensions.SceConnectionType;
import org.cgutman.shieldcontrollerextensions.SceManager;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ControllerHandler implements InputManager.InputDeviceListener, UsbDriverListener {

    private static final int MAXIMUM_BUMPER_UP_DELAY_MS = 100;

    private static final int START_DOWN_TIME_MOUSE_MODE_MS = 750;

    private static final int MINIMUM_BUTTON_DOWN_TIME_MS = 25;

    private static final int QUICK_MENU_FIRST_STAGE_MS = 200;

    private static final int EMULATING_SPECIAL = 0x1;
    private static final int EMULATING_SELECT = 0x2;
    private static final int EMULATING_TOUCHPAD = 0x4;

    private final Vector2d inputVector = new Vector2d();

    private final NvConnection conn;
    private final Activity activityContext;
    private final double stickDeadzone;
    private final GameGestures gestures;
    private final InputManager inputManager;
    private final Vibrator deviceVibrator;
    private final VibratorManager deviceVibratorManager;
    private final SensorManager deviceSensorManager;
    private final SceManager sceManager;
    private final Handler mainThreadHandler;
    private final HandlerThread backgroundHandlerThread;
    private final Handler backgroundThreadHandler;
    private final ControllerRumbleManager controllerRumbleManager;
    private final ControllerSensorManager controllerSensorManager;
    private final ControllerDeviceRegistry deviceRegistry;
    private boolean stopped = false;

    private final PreferenceConfiguration prefConfig;

    public ControllerHandler(Activity activityContext, NvConnection conn, GameGestures gestures, PreferenceConfiguration prefConfig) {
        this.activityContext = activityContext;
        this.conn = conn;
        this.gestures = gestures;
        this.prefConfig = prefConfig;
		
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.deviceVibratorManager = (VibratorManager) activityContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            this.deviceVibrator = (Vibrator) this.deviceVibratorManager.getDefaultVibrator();
        }
        else {
            this.deviceVibratorManager = null;
            this.deviceVibrator = (Vibrator) activityContext.getSystemService(Context.VIBRATOR_SERVICE);
        }
		
        this.deviceSensorManager = (SensorManager) activityContext.getSystemService(Context.SENSOR_SERVICE);
        this.inputManager = (InputManager) activityContext.getSystemService(Context.INPUT_SERVICE);
        this.mainThreadHandler = new Handler(Looper.getMainLooper());

        // Create a HandlerThread to process battery state updates. These can be slow enough
        // that they lead to ANRs if we do them on the main thread.
        this.backgroundHandlerThread = new HandlerThread("ControllerHandler");
        this.backgroundHandlerThread.start();
        this.backgroundThreadHandler = new Handler(backgroundHandlerThread.getLooper());

        this.sceManager = new SceManager(activityContext);
        this.sceManager.start();

        this.controllerRumbleManager = new ControllerRumbleManager(this.deviceVibrator, this.prefConfig, this.sceManager);
        this.controllerSensorManager = new ControllerSensorManager(
                () -> activityContext.getWindowManager().getDefaultDisplay().getRotation(),
                (controllerNumber, motionType, x, y, z) -> conn.sendControllerMotionEvent(controllerNumber, motionType, x, y, z)
        );

        this.stickDeadzone = (double) prefConfig.deadzonePercentage / 100.0;

        this.deviceRegistry = new ControllerDeviceRegistry(
                activityContext,
                new NvControllerPacketSender(conn),
                prefConfig,
                this.deviceVibrator,
                this.deviceVibratorManager,
                this.deviceSensorManager,
                this.sceManager,
                this.inputManager,
                this.mainThreadHandler,
                this.backgroundThreadHandler,
                this.controllerRumbleManager,
                this.controllerSensorManager,
                (controllerNumber, motionType, reportRateHz) -> {
                    handleSetMotionEventState(controllerNumber, motionType, reportRateHz);
                    return kotlin.Unit.INSTANCE;
                }
        );
    }

    public boolean hasController() {
        return deviceRegistry.hasGameController;
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        deviceRegistry.onInputDeviceAdded(deviceId);
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        deviceRegistry.onInputDeviceRemoved(deviceId);
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        deviceRegistry.onInputDeviceChanged(deviceId);
    }

    public void stop() {
        if (stopped) {
            return;
        }

        stopped = true;
        controllerRumbleManager.setStopped(true);
        deviceRegistry.stop();
    }

    public void destroy() {
        if (!stopped) {
            stop();
        }

        deviceRegistry.destroy();
        backgroundHandlerThread.quit();
    }

    public void disableSensors() {
        deviceRegistry.disableSensors();
    }

    public void enableSensors() {
        if (stopped) {
            return;
        }

        deviceRegistry.enableSensors();
    }

    public static boolean isGameControllerDevice(InputDevice device) {
        return ControllerDeviceRegistry.isGameControllerDevice(device);
    }

    public static short getAttachedControllerMask(Context context) {
        return ControllerDeviceRegistry.getAttachedControllerMask(context);
    }

    private void releaseControllerNumber(GenericControllerContext context) {
        deviceRegistry.releaseControllerNumber(context);
    }

    private void assignControllerNumberIfNeeded(GenericControllerContext context) {
        deviceRegistry.assignControllerNumberIfNeeded(context);
    }

    private InputDeviceContext getContextForEvent(InputEvent event) {
        return deviceRegistry.getContextForEvent(event);
    }

    private byte maxByMagnitude(byte a, byte b) {
        int absA = Math.abs(a);
        int absB = Math.abs(b);
        if (absA > absB) {
            return a;
        }
        else {
            return b;
        }
    }

    private short maxByMagnitude(short a, short b) {
        int absA = Math.abs(a);
        int absB = Math.abs(b);
        if (absA > absB) {
            return a;
        }
        else {
            return b;
        }
    }

    private short getActiveControllerMask() {
        return deviceRegistry.getActiveControllerMask();
    }

    private void sendControllerInputPacket(GenericControllerContext originalContext) {
        assignControllerNumberIfNeeded(originalContext);

        // Take the context's controller number and fuse all inputs with the same number
        short controllerNumber = originalContext.getControllerNumber();
        int inputMap = 0;
        byte leftTrigger = 0;
        byte rightTrigger = 0;
        short leftStickX = 0;
        short leftStickY = 0;
        short rightStickX = 0;
        short rightStickY = 0;

        // In order to properly handle controllers that are split into multiple devices,
        // we must aggregate all controllers with the same controller number into a single
        // device before we send it.
        for (int i = 0; i < deviceRegistry.inputDeviceContexts.size(); i++) {
            GenericControllerContext context = deviceRegistry.inputDeviceContexts.valueAt(i);
            if (context.assignedControllerNumber &&
                    context.getControllerNumber() == controllerNumber &&
                    context.mouseEmulationActive == originalContext.mouseEmulationActive) {
                inputMap |= context.inputMap;
                leftTrigger |= maxByMagnitude(leftTrigger, context.leftTrigger);
                rightTrigger |= maxByMagnitude(rightTrigger, context.rightTrigger);
                leftStickX |= maxByMagnitude(leftStickX, context.leftStickX);
                leftStickY |= maxByMagnitude(leftStickY, context.leftStickY);
                rightStickX |= maxByMagnitude(rightStickX, context.rightStickX);
                rightStickY |= maxByMagnitude(rightStickY, context.rightStickY);
            }
        }
        for (int i = 0; i < deviceRegistry.usbDeviceContexts.size(); i++) {
            GenericControllerContext context = deviceRegistry.usbDeviceContexts.valueAt(i);
            if (context.assignedControllerNumber &&
                    context.getControllerNumber() == controllerNumber &&
                    context.mouseEmulationActive == originalContext.mouseEmulationActive) {
                inputMap |= context.inputMap;
                leftTrigger |= maxByMagnitude(leftTrigger, context.leftTrigger);
                rightTrigger |= maxByMagnitude(rightTrigger, context.rightTrigger);
                leftStickX |= maxByMagnitude(leftStickX, context.leftStickX);
                leftStickY |= maxByMagnitude(leftStickY, context.leftStickY);
                rightStickX |= maxByMagnitude(rightStickX, context.rightStickX);
                rightStickY |= maxByMagnitude(rightStickY, context.rightStickY);
            }
        }
        if (deviceRegistry.defaultContext.getControllerNumber() == controllerNumber) {
            inputMap |= deviceRegistry.defaultContext.inputMap;
            leftTrigger |= maxByMagnitude(leftTrigger, deviceRegistry.defaultContext.leftTrigger);
            rightTrigger |= maxByMagnitude(rightTrigger, deviceRegistry.defaultContext.rightTrigger);
            leftStickX |= maxByMagnitude(leftStickX, deviceRegistry.defaultContext.leftStickX);
            leftStickY |= maxByMagnitude(leftStickY, deviceRegistry.defaultContext.leftStickY);
            rightStickX |= maxByMagnitude(rightStickX, deviceRegistry.defaultContext.rightStickX);
            rightStickY |= maxByMagnitude(rightStickY, deviceRegistry.defaultContext.rightStickY);
        }

        if (originalContext.mouseEmulationActive) {
            int changedMask = inputMap ^  originalContext.mouseEmulationLastInputMap;

            boolean aDown = (inputMap & ControllerPacket.A_FLAG) != 0;
            boolean bDown = (inputMap & ControllerPacket.B_FLAG) != 0;

            boolean xDown = (inputMap & ControllerPacket.X_FLAG) != 0;
            boolean yDown = (inputMap & ControllerPacket.Y_FLAG) != 0;

            originalContext.mouseEmulationLastInputMap = inputMap;

            // Set the flag for the fixed pixel mouse movement while X_FLAG button is pressed
            if((changedMask & ControllerPacket.X_FLAG) != 0)
            {
                // Set true when pressed
                if( xDown ) {
                    originalContext.mouseEmulationXDown = true;
                }
                // Set false when released
                else
                {
                    originalContext.mouseEmulationXDown = false;
                }
            }

            if((changedMask & ControllerPacket.Y_FLAG) != 0)
            {
                if( yDown )
                {
                    // Double the pixel multiplier every button press
                    originalContext.mouseEmulationPixelMultiplier *= 2;
                    if( originalContext.mouseEmulationPixelMultiplier > 255 )
                    {
                        // Reset the multiplier back to 1 if it gets too big
                        originalContext.mouseEmulationPixelMultiplier = 1;
                    }
                }
                else {
                    // Do nothing as this is when the button is released; Holding the button will not continuously increase the pixel multiplier
                }
            }
            if ((changedMask & ControllerPacket.A_FLAG) != 0) {
                if (aDown) {
                    conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
                }
                else {
                    conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
                }
            }
            if ((changedMask & ControllerPacket.B_FLAG) != 0) {
                if (bDown) {
                    conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT);
                }
                else {
                    conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT);
                }
            }
            if ((changedMask & ControllerPacket.UP_FLAG) != 0) {
                if ((inputMap & ControllerPacket.UP_FLAG) != 0) {
                    conn.sendMouseScroll((byte) 1);
                }
            }
            if ((changedMask & ControllerPacket.DOWN_FLAG) != 0) {
                if ((inputMap & ControllerPacket.DOWN_FLAG) != 0) {
                    conn.sendMouseScroll((byte) -1);
                }
            }
            if ((changedMask & ControllerPacket.RIGHT_FLAG) != 0) {
                if ((inputMap & ControllerPacket.RIGHT_FLAG) != 0) {
                    conn.sendMouseHScroll((byte) 1);
                }
            }
            if ((changedMask & ControllerPacket.LEFT_FLAG) != 0) {
                if ((inputMap & ControllerPacket.LEFT_FLAG) != 0) {
                    conn.sendMouseHScroll((byte) -1);
                }
            }

            conn.sendControllerInput(controllerNumber, getActiveControllerMask(),
                    (short)0, (byte)0, (byte)0, (short)0, (short)0, (short)0, (short)0);
        }
        else {
            conn.sendControllerInput(controllerNumber, getActiveControllerMask(),
                    inputMap,
                    leftTrigger, rightTrigger,
                    leftStickX, leftStickY,
                    rightStickX, rightStickY);
        }
    }

    private final int REMAP_IGNORE = -1;
    private final int REMAP_CONSUME = -2;

    // Return a valid keycode, -2 to consume, or -1 to not consume the event
    // Device MAY BE NULL
    private int handleRemapping(InputDeviceContext context, KeyEvent event) {
        // Don't capture the back button if configured
        if (context.ignoreBack) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                return REMAP_IGNORE;
            }
        }

        // If we know this gamepad has a share button and receive an unmapped
        // KEY_RECORD event, report that as a share button press.
        if (context.hasShare) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN &&
                    event.getScanCode() == 167) {
                return KeyEvent.KEYCODE_MEDIA_RECORD;
            }
        }

        // The Shield's key layout files map the DualShock 4 clickpad button to
        // BUTTON_SELECT instead of something sane like BUTTON_1 as the standard AOSP
        // mapping does. If we get a button from a Sony device reported as BUTTON_SELECT
        // that matches the keycode used by hid-sony for the clickpad or it's from the
        // separate touchpad input device, remap it to BUTTON_1 to match the current AOSP
        // layout and trigger our touchpad button logic.
        if (context.vendorId == 0x054c &&
                event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_SELECT &&
                (event.getScanCode() == 317 || context.isDualShockStandaloneTouchpad)) {
            return KeyEvent.KEYCODE_BUTTON_1;
        }

        // Override mode button for 8BitDo controllers
        if (context.vendorId == 0x2dc8 && event.getScanCode() == 306) {
            return KeyEvent.KEYCODE_BUTTON_MODE;
        }

        // This mapping was adding in Android 10, then changed based on
        // kernel changes (adding hid-nintendo) in Android 11. If we're
        // on anything newer than Pie, just use the built-in mapping.
        if ((context.vendorId == 0x057e && context.productId == 0x2009 && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) || // Switch Pro controller
                (context.vendorId == 0x0f0d && context.productId == 0x00c1)) { // HORIPAD for Switch
            switch (event.getScanCode()) {
                case 0x130://304
                    return KeyEvent.KEYCODE_BUTTON_A;
                case 0x131:
                    return KeyEvent.KEYCODE_BUTTON_B;
                case 0x132:
                    return KeyEvent.KEYCODE_BUTTON_X;
                case 0x133:
                    return KeyEvent.KEYCODE_BUTTON_Y;
                case 0x134:
                    return KeyEvent.KEYCODE_BUTTON_L1;
                case 0x135:
                    return KeyEvent.KEYCODE_BUTTON_R1;
                case 0x136:
                    return KeyEvent.KEYCODE_BUTTON_L2;
                case 0x137:
                    return KeyEvent.KEYCODE_BUTTON_R2;
                case 0x138:
                    return KeyEvent.KEYCODE_BUTTON_SELECT;
                case 0x139:
                    return KeyEvent.KEYCODE_BUTTON_START;
                case 0x13A:
                    return KeyEvent.KEYCODE_BUTTON_THUMBL;
                case 0x13B:
                    return KeyEvent.KEYCODE_BUTTON_THUMBR;
                case 0x13D:
                    return KeyEvent.KEYCODE_BUTTON_MODE;
            }
        }


        //fix joycon-left 十字键
        if(prefConfig.enableJoyConFix&&context.vendorId == 0x057e && context.productId == 0x2006){
            switch (event.getScanCode())
            {
                case 546://十字键
                    return KeyEvent.KEYCODE_DPAD_LEFT;
                case 547:
                    return KeyEvent.KEYCODE_DPAD_RIGHT;
                case 544:
                    return KeyEvent.KEYCODE_DPAD_UP;
                case 545:
                    return KeyEvent.KEYCODE_DPAD_DOWN;
                case 309://截图键
                    return KeyEvent.KEYCODE_BUTTON_MODE;
                case 310:
                    return KeyEvent.KEYCODE_BUTTON_L1;
                case 312:
                    return KeyEvent.KEYCODE_BUTTON_L2;
                case 314:
                    return KeyEvent.KEYCODE_BUTTON_SELECT;
                case 317:
                    return KeyEvent.KEYCODE_BUTTON_THUMBL;
            }
        }
        //fix JoyCon-right xy互换
        if(prefConfig.enableJoyConFix&&context.vendorId == 0x057e && context.productId == 0x2007){
            switch (event.getScanCode())
            {
                case 307://XY相反
                    return KeyEvent.KEYCODE_BUTTON_Y;
                case 308:
                    return KeyEvent.KEYCODE_BUTTON_X;
                case 304:
                    return KeyEvent.KEYCODE_BUTTON_A;
                case 305:
                    return KeyEvent.KEYCODE_BUTTON_B;
                case 311:
                    return KeyEvent.KEYCODE_BUTTON_R1;
                case 313:
                    return KeyEvent.KEYCODE_BUTTON_R2;
                case 315:
                    return KeyEvent.KEYCODE_BUTTON_START;
                case 316:
                    return KeyEvent.KEYCODE_BUTTON_MODE;
                case 318:
                    return KeyEvent.KEYCODE_BUTTON_THUMBR;
            }
        }


        if (context.usesLinuxGamepadStandardFaceButtons) {
            // Android's Generic.kl swaps BTN_NORTH and BTN_WEST
            switch (event.getScanCode()) {
                case 304:
                    return KeyEvent.KEYCODE_BUTTON_A;
                case 305:
                    return KeyEvent.KEYCODE_BUTTON_B;
                case 307:
                    return KeyEvent.KEYCODE_BUTTON_Y;
                case 308:
                    return KeyEvent.KEYCODE_BUTTON_X;
            }
        }

        if (context.isNonStandardDualShock4) {
            switch (event.getScanCode()) {
                case 304:
                    return KeyEvent.KEYCODE_BUTTON_X;
                case 305:
                    return KeyEvent.KEYCODE_BUTTON_A;
                case 306:
                    return KeyEvent.KEYCODE_BUTTON_B;
                case 307:
                    return KeyEvent.KEYCODE_BUTTON_Y;
                case 308:
                    return KeyEvent.KEYCODE_BUTTON_L1;
                case 309:
                    return KeyEvent.KEYCODE_BUTTON_R1;
                /*
                **** Using analog triggers instead ****
                case 310:
                    return KeyEvent.KEYCODE_BUTTON_L2;
                case 311:
                    return KeyEvent.KEYCODE_BUTTON_R2;
                */
                case 312:
                    return KeyEvent.KEYCODE_BUTTON_SELECT;
                case 313:
                    return KeyEvent.KEYCODE_BUTTON_START;
                case 314:
                    return KeyEvent.KEYCODE_BUTTON_THUMBL;
                case 315:
                    return KeyEvent.KEYCODE_BUTTON_THUMBR;
                case 316:
                    return KeyEvent.KEYCODE_BUTTON_MODE;
                default:
                    return REMAP_CONSUME;
            }
        }
        // If this is a Serval controller sending an unknown key code, it's probably
        // the start and select buttons
        else if (context.isServal && event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
            switch (event.getScanCode())  {
                case 314:
                    return KeyEvent.KEYCODE_BUTTON_SELECT;
                case 315:
                    return KeyEvent.KEYCODE_BUTTON_START;
            }
        }
        else if (context.isNonStandardXboxBtController) {
            switch (event.getScanCode()) {
                case 306:
                    return KeyEvent.KEYCODE_BUTTON_X;
                case 307:
                    return KeyEvent.KEYCODE_BUTTON_Y;
                case 308:
                    return KeyEvent.KEYCODE_BUTTON_L1;
                case 309:
                    return KeyEvent.KEYCODE_BUTTON_R1;
                case 310:
                    return KeyEvent.KEYCODE_BUTTON_SELECT;
                case 311:
                    return KeyEvent.KEYCODE_BUTTON_START;
                case 312:
                    return KeyEvent.KEYCODE_BUTTON_THUMBL;
                case 313:
                    return KeyEvent.KEYCODE_BUTTON_THUMBR;
                case 139:
                    return KeyEvent.KEYCODE_BUTTON_MODE;
                default:
                    // Other buttons are mapped correctly
            }

            // The Xbox button is sent as MENU
            if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
                return KeyEvent.KEYCODE_BUTTON_MODE;
            }
        }
        else if (context.vendorId == 0x0b05 && // ASUS
                     (context.productId == 0x7900 || // Kunai - USB
                      context.productId == 0x7902)) // Kunai - Bluetooth
        {
            // ROG Kunai has special M1-M4 buttons that are accessible via the
            // joycon-style detachable controllers that we should map to Start
            // and Select.
            switch (event.getScanCode()) {
                case 264:
                case 266:
                    return KeyEvent.KEYCODE_BUTTON_START;

                case 265:
                case 267:
                    return KeyEvent.KEYCODE_BUTTON_SELECT;
            }
        }

        if (context.hatXAxis == -1 &&
                 context.hatYAxis == -1 &&
                 /* FIXME: There's no good way to know for sure if xpad is bound
                    to this device, so we won't use the name to validate if these
                    scancodes should be mapped to DPAD

                    context.isXboxController &&
                  */
                 event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
            // If there's not a proper Xbox controller mapping, we'll translate the raw d-pad
            // scan codes into proper key codes
            switch (event.getScanCode())
            {
            case 704:
                return KeyEvent.KEYCODE_DPAD_LEFT;
            case 705:
                return KeyEvent.KEYCODE_DPAD_RIGHT;
            case 706:
                return KeyEvent.KEYCODE_DPAD_UP;
            case 707:
                return KeyEvent.KEYCODE_DPAD_DOWN;
            }
        }

        // Past here we can fixup the keycode and potentially trigger
        // another special case so we need to remember what keycode we're using
        int keyCode = event.getKeyCode();

        // This is a hack for (at least) the "Tablet Remote" app
        // which sends BACK with META_ALT_ON instead of KEYCODE_BUTTON_B
        if (keyCode == KeyEvent.KEYCODE_BACK &&
                !event.hasNoModifiers() &&
                (event.getFlags() & KeyEvent.FLAG_SOFT_KEYBOARD) != 0)
        {
            keyCode = KeyEvent.KEYCODE_BUTTON_B;
        }

        if (keyCode == KeyEvent.KEYCODE_BUTTON_START ||
                keyCode == KeyEvent.KEYCODE_MENU) {
            // Ensure that we never use back as start if we have a real start
            context.backIsStart = false;
        }
        else if (keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) {
            // Don't use mode as select if we have a select
            context.modeIsSelect = false;
        }
        else if (context.backIsStart && keyCode == KeyEvent.KEYCODE_BACK) {
            // Emulate the start button with back
            return KeyEvent.KEYCODE_BUTTON_START;
        }
        else if (context.modeIsSelect && keyCode == KeyEvent.KEYCODE_BUTTON_MODE) {
            // Emulate the select button with mode
            return KeyEvent.KEYCODE_BUTTON_SELECT;
        }
        else if (context.searchIsMode && keyCode == KeyEvent.KEYCODE_SEARCH) {
            // Emulate the mode button with search
            return KeyEvent.KEYCODE_BUTTON_MODE;
        }

        return keyCode;
    }

    private int handleFlipFaceButtons(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
                return KeyEvent.KEYCODE_BUTTON_B;
            case KeyEvent.KEYCODE_BUTTON_B:
                return KeyEvent.KEYCODE_BUTTON_A;
            case KeyEvent.KEYCODE_BUTTON_X:
                return KeyEvent.KEYCODE_BUTTON_Y;
            case KeyEvent.KEYCODE_BUTTON_Y:
                return KeyEvent.KEYCODE_BUTTON_X;
            default:
                return keyCode;
        }
    }

    private Vector2d populateCachedVector(float x, float y) {
        // Reinitialize our cached Vector2d object
        inputVector.initialize(x, y);
        return inputVector;
    }

    private void handleDeadZone(Vector2d stickVector, float deadzoneRadius) {
        if (deadzoneRadius > 0) {
            // We're not normalizing here because we let the computer handle the deadzones.
            // Normalizing can make the deadzones larger than they should be after the computer also
            // evaluates the deadzone.
            if (stickVector.getMagnitude() <= deadzoneRadius) {
                // Deadzone
                stickVector.initialize(0, 0);
            }
        } else {
            double currentMagnitude = stickVector.getMagnitude();
            if (currentMagnitude < 0.01) {
                // Keep a 1% actual deadzone for actual centering
                stickVector.initialize(0, 0);
                return;
            }
            double remainingMagnitude = 1 + deadzoneRadius;
            double normalizedMagnitude = -deadzoneRadius + currentMagnitude * remainingMagnitude;
            if (normalizedMagnitude >= 1) {
                return;
            }
            double scaleFactor = normalizedMagnitude / currentMagnitude;
            stickVector.setX((float) (stickVector.getX() * scaleFactor));
            stickVector.setY((float) (stickVector.getY() * scaleFactor));
        }
    }

    private void handleAxisSet(InputDeviceContext context, float lsX, float lsY, float rsX,
                               float rsY, float lt, float rt, float hatX, float hatY) {

        if (context.leftStickXAxis != -1 && context.leftStickYAxis != -1) {
            Vector2d leftStickVector = populateCachedVector(lsX, lsY);

            handleDeadZone(leftStickVector, context.leftStickDeadzoneRadius);

            context.leftStickX = (short) (leftStickVector.getX() * 0x7FFE);
            context.leftStickY = (short) (-leftStickVector.getY() * 0x7FFE);
        }

        if (context.rightStickXAxis != -1 && context.rightStickYAxis != -1) {
            Vector2d rightStickVector = populateCachedVector(rsX, rsY);

            handleDeadZone(rightStickVector, context.rightStickDeadzoneRadius);

            context.rightStickX = (short) (rightStickVector.getX() * 0x7FFE);
            context.rightStickY = (short) (-rightStickVector.getY() * 0x7FFE);
        }

        if (context.leftTriggerAxis != -1 && context.rightTriggerAxis != -1) {
            // Android sends an initial 0 value for trigger axes even if the trigger
            // should be negative when idle. After the first touch, the axes will go back
            // to normal behavior, so ignore triggersIdleNegative for each trigger until
            // first touch.
            if (lt != 0) {
                context.leftTriggerAxisUsed = true;
            }
            if (rt != 0) {
                context.rightTriggerAxisUsed = true;
            }
            if (context.triggersIdleNegative) {
                if (context.leftTriggerAxisUsed) {
                    lt = (lt + 1) / 2;
                }
                if (context.rightTriggerAxisUsed) {
                    rt = (rt + 1) / 2;
                }
            }

            if (lt <= context.triggerDeadzone) {
                lt = 0;
            }
            if (rt <= context.triggerDeadzone) {
                rt = 0;
            }

            context.leftTrigger = (byte)(lt * 0xFF);
            context.rightTrigger = (byte)(rt * 0xFF);
        }

        if (context.hatXAxis != -1 && context.hatYAxis != -1) {
            context.inputMap &= ~(ControllerPacket.LEFT_FLAG | ControllerPacket.RIGHT_FLAG);
            if (hatX < -0.5) {
                context.inputMap |= ControllerPacket.LEFT_FLAG;
                context.hatXAxisUsed = true;
            }
            else if (hatX > 0.5) {
                context.inputMap |= ControllerPacket.RIGHT_FLAG;
                context.hatXAxisUsed = true;
            }

            context.inputMap &= ~(ControllerPacket.UP_FLAG | ControllerPacket.DOWN_FLAG);
            if (hatY < -0.5) {
                context.inputMap |= ControllerPacket.UP_FLAG;
                context.hatYAxisUsed = true;
            }
            else if (hatY > 0.5) {
                context.inputMap |= ControllerPacket.DOWN_FLAG;
                context.hatYAxisUsed = true;
            }
        }

        sendControllerInputPacket(context);
    }

    // Normalize the given raw float value into a 0.0-1.0f range
    private float normalizeRawValueWithRange(float value, InputDevice.MotionRange range) {
        value = Math.max(value, range.getMin());
        value = Math.min(value, range.getMax());

        value -= range.getMin();

        return value / range.getRange();
    }

    private boolean sendTouchpadEventForPointer(InputDeviceContext context, MotionEvent event, byte touchType, int pointerIndex) {
        float normalizedX = normalizeRawValueWithRange(event.getX(pointerIndex), context.touchpadXRange);
        float normalizedY = normalizeRawValueWithRange(event.getY(pointerIndex), context.touchpadYRange);
        float normalizedPressure = context.touchpadPressureRange != null ?
                normalizeRawValueWithRange(event.getPressure(pointerIndex), context.touchpadPressureRange)
                : 0;

        return conn.sendControllerTouchEvent((byte)context.getControllerNumber(), touchType,
                event.getPointerId(pointerIndex),
                normalizedX, normalizedY, normalizedPressure) != MoonBridge.LI_ERR_UNSUPPORTED;
    }

    public boolean tryHandleTouchpadEvent(MotionEvent event) {
        // Bail if this is not a touchpad or mouse event
        if (event.getSource() != InputDevice.SOURCE_TOUCHPAD &&
                event.getSource() != InputDevice.SOURCE_MOUSE) {
            return false;
        }

        // Only get a context if one already exists. We want to ensure we don't report non-gamepads.
        InputDeviceContext context = deviceRegistry.inputDeviceContexts.get(event.getDeviceId());
        if (context == null) {
            return false;
        }

        // When we're working with a mouse source instead of a touchpad, we're quite limited in
        // what useful input we can provide via the controller API. The ABS_X/ABS_Y values are
        // screen coordinates rather than touchpad coordinates. For now, we will just support
        // the clickpad button and nothing else.
        if (event.getSource() == InputDevice.SOURCE_MOUSE) {
            // Unlike the touchpad where down and up refer to individual touches on the touchpad,
            // down and up on a mouse indicates the state of the left mouse button.
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    context.inputMap |= ControllerPacket.TOUCHPAD_FLAG;
                    sendControllerInputPacket(context);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    context.inputMap &= ~ControllerPacket.TOUCHPAD_FLAG;
                    sendControllerInputPacket(context);
                    break;
                default:
                    break;
            }

            return !prefConfig.gamepadTouchpadAsMouse;
        }

        byte touchType;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                touchType = MoonBridge.LI_TOUCH_EVENT_DOWN;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if ((event.getFlags() & MotionEvent.FLAG_CANCELED) != 0) {
                    touchType = MoonBridge.LI_TOUCH_EVENT_CANCEL;
                }
                else {
                    touchType = MoonBridge.LI_TOUCH_EVENT_UP;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                touchType = MoonBridge.LI_TOUCH_EVENT_MOVE;
                break;

            case MotionEvent.ACTION_CANCEL:
                // ACTION_CANCEL applies to *all* pointers in the gesture, so it maps to CANCEL_ALL
                // rather than CANCEL. For a single pointer cancellation, that's indicated via
                // FLAG_CANCELED on a ACTION_POINTER_UP.
                // https://developer.android.com/develop/ui/views/touch-and-input/gestures/multi
                touchType = MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL;
                break;

            case MotionEvent.ACTION_BUTTON_PRESS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && event.getActionButton() == MotionEvent.BUTTON_PRIMARY) {
                    context.inputMap |= ControllerPacket.TOUCHPAD_FLAG;
                    sendControllerInputPacket(context);
                    return !prefConfig.gamepadTouchpadAsMouse; // Report as unhandled event to trigger mouse handling
                }
                return false;

            case MotionEvent.ACTION_BUTTON_RELEASE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && event.getActionButton() == MotionEvent.BUTTON_PRIMARY) {
                    context.inputMap &= ~ControllerPacket.TOUCHPAD_FLAG;
                    sendControllerInputPacket(context);
                    return !prefConfig.gamepadTouchpadAsMouse; // Report as unhandled event to trigger mouse handling
                }
                return false;

            default:
                return false;
        }

        // Bail if the user wants gamepad touchpads to control the mouse
        //
        // NB: We do this after processing ACTION_BUTTON_PRESS and ACTION_BUTTON_RELEASE
        // because we want to still send the touchpad button via the gamepad even when
        // configured to use the touchpad for mouse control.
        if (prefConfig.gamepadTouchpadAsMouse) {
            return false;
        }

        // If we don't have X and Y ranges, we can't process this event
        if (context.touchpadXRange == null || context.touchpadYRange == null) {
            return false;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            // Move events may impact all active pointers
            for (int i = 0; i < event.getPointerCount(); i++) {
                if (!sendTouchpadEventForPointer(context, event, touchType, i)) {
                    // Controller touch events are not supported by the host
                    return false;
                }
            }
            return true;
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            // Cancel impacts all active pointers
            return conn.sendControllerTouchEvent((byte)context.getControllerNumber(), MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL,
                    0, 0, 0, 0) != MoonBridge.LI_ERR_UNSUPPORTED;
        }
        else {
            // Down and Up events impact the action index pointer
            return sendTouchpadEventForPointer(context, event, touchType, event.getActionIndex());
        }
    }

    public boolean handleMotionEvent(MotionEvent event) {
        InputDeviceContext context = getContextForEvent(event);
        if (context == null) {
            return true;
        }

        float lsX = 0, lsY = 0, rsX = 0, rsY = 0, rt = 0, lt = 0, hatX = 0, hatY = 0;

        // We purposefully ignore the historical values in the motion event as it makes
        // the controller feel sluggish for some users.

        if (context.leftStickXAxis != -1 && context.leftStickYAxis != -1) {
            lsX = event.getAxisValue(context.leftStickXAxis);
            lsY = event.getAxisValue(context.leftStickYAxis);
        }

        if (context.rightStickXAxis != -1 && context.rightStickYAxis != -1) {
            rsX = event.getAxisValue(context.rightStickXAxis);
            rsY = event.getAxisValue(context.rightStickYAxis);
        }

        if (context.leftTriggerAxis != -1 && context.rightTriggerAxis != -1) {
            lt = event.getAxisValue(context.leftTriggerAxis);
            rt = event.getAxisValue(context.rightTriggerAxis);
        }

        if (context.hatXAxis != -1 && context.hatYAxis != -1) {
            hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
            hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        }

        handleAxisSet(context, lsX, lsY, rsX, rsY, lt, rt, hatX, hatY);

        return true;
    }

    private List<RumbleTarget> getActiveInputRumbleTargets() {
        return deviceRegistry.getActiveInputRumbleTargets();
    }

    private List<RumbleTarget> getActiveUsbRumbleTargets() {
        return deviceRegistry.getActiveUsbRumbleTargets();
    }

    private List<SensorTarget> getActiveSensorTargets() {
        return deviceRegistry.getActiveSensorTargets();
    }

    public void handleRumble(short controllerNumber, short lowFreqMotor, short highFreqMotor) {
        controllerRumbleManager.handleRumble(
                controllerNumber,
                lowFreqMotor,
                highFreqMotor,
                getActiveInputRumbleTargets(),
                getActiveUsbRumbleTargets()
        );
    }

    public void handleRumbleTriggers(short controllerNumber, short leftTrigger, short rightTrigger) {
        controllerRumbleManager.handleRumbleTriggers(
                controllerNumber,
                leftTrigger,
                rightTrigger,
                getActiveInputRumbleTargets(),
                getActiveUsbRumbleTargets()
        );
    }

    public void handleSetMotionEventState(final short controllerNumber, final byte motionType, short reportRateHz) {
        if (stopped) {
            return;
        }

        controllerSensorManager.handleSetMotionEventState(
                controllerNumber,
                motionType,
                reportRateHz,
                getActiveSensorTargets(),
                sm -> sm == deviceSensorManager
        );
    }

    public void handleSetControllerLED(short controllerNumber, byte r, byte g, byte b) {
        if (stopped) {
            return;
        }

        deviceRegistry.handleSetControllerLED(controllerNumber, r, g, b);
    }

    public boolean handleButtonUp(KeyEvent event) {
        InputDeviceContext context = getContextForEvent(event);
        if (context == null) {
            return true;
        }

        int keyCode = handleRemapping(context, event);
        if (keyCode < 0) {
            return (keyCode == REMAP_CONSUME);
        }

        if (prefConfig.flipFaceButtons) {
            keyCode = handleFlipFaceButtons(keyCode);
        }

        // If the button hasn't been down long enough, sleep for a bit before sending the up event
        // This allows "instant" button presses (like OUYA's virtual menu button) to work. This
        // path should not be triggered during normal usage.
        int buttonDownTime = (int)(event.getEventTime() - event.getDownTime());
        if (buttonDownTime < ControllerHandler.MINIMUM_BUTTON_DOWN_TIME_MS)
        {
            // Since our sleep time is so short (<= 25 ms), it shouldn't cause a problem doing this
            // in the UI thread.
            try {
                Thread.sleep(ControllerHandler.MINIMUM_BUTTON_DOWN_TIME_MS - buttonDownTime);
            } catch (InterruptedException e) {
                LimeLog.warning("Interrupted during button down delay");
                Thread.currentThread().interrupt();
            }
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_BUTTON_MODE:
            context.inputMap &= ~ControllerPacket.SPECIAL_BUTTON_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_START:
        case KeyEvent.KEYCODE_MENU:
            context.startUpTime = event.getEventTime();
            // Sometimes we'll get a spurious key up event on controller disconnect.
            // Make sure it's real by checking that the key is actually down before taking
            // any action.
            if ((context.inputMap & ControllerPacket.PLAY_FLAG) != 0 &&
                    context.startUpTime - context.startDownTime > ControllerHandler.START_DOWN_TIME_MOUSE_MODE_MS) {
                if (prefConfig.enableBackMenu && context.backMenuPending){
                    //todo 展示快捷菜单
                    context.backMenuPending = false;
                    gestures.showGameMenu(context);
                } else if (prefConfig.mouseEmulation) {
                    context.toggleMouseEmulation();
                }
            }
            context.inputMap &= ~ControllerPacket.PLAY_FLAG;
            break;
        case KeyEvent.KEYCODE_BACK:
            if (prefConfig.backAsGuide) {
                context.inputMap &= ~ControllerPacket.SPECIAL_BUTTON_FLAG;
                break;
            }
        case KeyEvent.KEYCODE_BUTTON_SELECT:
            context.inputMap &= ~ControllerPacket.BACK_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
            if (context.hatXAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~ControllerPacket.LEFT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            if (context.hatXAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~ControllerPacket.RIGHT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_UP:
            if (context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~ControllerPacket.UP_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
            if (context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~ControllerPacket.DOWN_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_UP_LEFT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~(ControllerPacket.UP_FLAG | ControllerPacket.LEFT_FLAG);
            break;
        case KeyEvent.KEYCODE_DPAD_UP_RIGHT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~(ControllerPacket.UP_FLAG | ControllerPacket.RIGHT_FLAG);
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~(ControllerPacket.DOWN_FLAG | ControllerPacket.LEFT_FLAG);
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap &= ~(ControllerPacket.DOWN_FLAG | ControllerPacket.RIGHT_FLAG);
            break;
        case KeyEvent.KEYCODE_BUTTON_B:
            context.inputMap &= ~ControllerPacket.B_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_BUTTON_A:
            context.inputMap &= ~ControllerPacket.A_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_X:
            context.inputMap &= ~ControllerPacket.X_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_Y:
            context.inputMap &= ~ControllerPacket.Y_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_L1:
            context.inputMap &= ~ControllerPacket.LB_FLAG;
            context.lastLbUpTime = event.getEventTime();
            break;
        case KeyEvent.KEYCODE_BUTTON_R1:
            context.inputMap &= ~ControllerPacket.RB_FLAG;
            context.lastRbUpTime = event.getEventTime();
            break;
        case KeyEvent.KEYCODE_BUTTON_THUMBL:
            context.inputMap &= ~ControllerPacket.LS_CLK_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_THUMBR:
            context.inputMap &= ~ControllerPacket.RS_CLK_FLAG;
            break;
        case KeyEvent.KEYCODE_MEDIA_RECORD: // Xbox Series X Share button
            context.inputMap &= ~ControllerPacket.MISC_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_1: // PS4/PS5 touchpad button (prior to 4.10)
            context.inputMap &= ~ControllerPacket.TOUCHPAD_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_L2:
            if (context.leftTriggerAxisUsed) {
                // Suppress this digital event if an analog trigger is active
                return true;
            }
            context.leftTrigger = 0;
            break;
        case KeyEvent.KEYCODE_BUTTON_R2:
            if (context.rightTriggerAxisUsed) {
                // Suppress this digital event if an analog trigger is active
                return true;
            }
            context.rightTrigger = 0;
            break;
        case KeyEvent.KEYCODE_UNKNOWN:
            // Paddles aren't mapped in any of the Android key layout files,
            // so we need to handle the evdev key codes directly.
            if (context.hasPaddles) {
                switch (event.getScanCode()) {
                    case 0x2c4: // BTN_TRIGGER_HAPPY5
                        context.inputMap &= ~ControllerPacket.PADDLE1_FLAG;
                        break;
                    case 0x2c5: // BTN_TRIGGER_HAPPY6
                        context.inputMap &= ~ControllerPacket.PADDLE2_FLAG;
                        break;
                    case 0x2c6: // BTN_TRIGGER_HAPPY7
                        context.inputMap &= ~ControllerPacket.PADDLE3_FLAG;
                        break;
                    case 0x2c7: // BTN_TRIGGER_HAPPY8
                        context.inputMap &= ~ControllerPacket.PADDLE4_FLAG;
                        break;
                    default:
                        return false;
                }
            }
            else {
                return false;
            }
            break;
        default:
            return false;
        }

        // Check if we're emulating the select button
        if ((context.emulatingButtonFlags & ControllerHandler.EMULATING_SELECT) != 0)
        {
            // If either start or LB is up, select comes up too
            if ((context.inputMap & ControllerPacket.PLAY_FLAG) == 0 ||
                (context.inputMap & ControllerPacket.LB_FLAG) == 0)
            {
                context.inputMap &= ~ControllerPacket.BACK_FLAG;

                context.emulatingButtonFlags &= ~ControllerHandler.EMULATING_SELECT;
            }
        }

        // Check if we're emulating the special button
        if ((context.emulatingButtonFlags & ControllerHandler.EMULATING_SPECIAL) != 0)
        {
            // If either start or select and RB is up, the special button comes up too
            if ((context.inputMap & ControllerPacket.PLAY_FLAG) == 0 ||
                ((context.inputMap & ControllerPacket.BACK_FLAG) == 0 &&
                 (context.inputMap & ControllerPacket.RB_FLAG) == 0))
            {
                context.inputMap &= ~ControllerPacket.SPECIAL_BUTTON_FLAG;

                context.emulatingButtonFlags &= ~ControllerHandler.EMULATING_SPECIAL;
            }
        }

        // Check if we're emulating the touchpad button
        if ((context.emulatingButtonFlags & ControllerHandler.EMULATING_TOUCHPAD) != 0)
        {
            // If either select or LB is up, touchpad comes up too
            if ((context.inputMap & ControllerPacket.BACK_FLAG) == 0 ||
                    (context.inputMap & ControllerPacket.LB_FLAG) == 0)
            {
                context.inputMap &= ~ControllerPacket.TOUCHPAD_FLAG;

                context.emulatingButtonFlags &= ~ControllerHandler.EMULATING_TOUCHPAD;
            }
        }

        sendControllerInputPacket(context);

        if (context.pendingExit && context.inputMap == 0) {
            // All buttons from the quit combo are lifted. Finish the activity now.
            activityContext.finish();
        }

        return true;
    }

    public boolean handleButtonDown(KeyEvent event) {
        InputDeviceContext context = getContextForEvent(event);
        if (context == null) {
            return true;
        }

        int keyCode = handleRemapping(context, event);
        if (keyCode < 0) {
            return (keyCode == REMAP_CONSUME);
        }

        if (prefConfig.flipFaceButtons) {
            keyCode = handleFlipFaceButtons(keyCode);
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_BUTTON_MODE:
            context.hasMode = true;
            context.inputMap |= ControllerPacket.SPECIAL_BUTTON_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_START:
        case KeyEvent.KEYCODE_MENU:
            if (event.getRepeatCount() == 0) {
                context.startDownTime = event.getEventTime();
                if (context.startDownTime - context.startUpTime <= ControllerHandler.QUICK_MENU_FIRST_STAGE_MS) {
                    context.backMenuPending = true;
                } else {
                    context.backMenuPending = false;
                }
            }
            context.inputMap |= ControllerPacket.PLAY_FLAG;
            break;
        case KeyEvent.KEYCODE_BACK:
            if (prefConfig.backAsGuide) {
                context.hasSelect = true;
                context.inputMap |= ControllerPacket.SPECIAL_BUTTON_FLAG;
                break;
            }
        case KeyEvent.KEYCODE_BUTTON_SELECT:
            context.hasSelect = true;
            context.inputMap |= ControllerPacket.BACK_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
            if (context.hatXAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.LEFT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            if (context.hatXAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.RIGHT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_UP:
            if (context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.UP_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
            if (context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.DOWN_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_UP_LEFT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.UP_FLAG | ControllerPacket.LEFT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_UP_RIGHT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.UP_FLAG | ControllerPacket.RIGHT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.DOWN_FLAG | ControllerPacket.LEFT_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
            if (context.hatXAxisUsed && context.hatYAxisUsed) {
                // Suppress this duplicate event if we have a hat
                return true;
            }
            context.inputMap |= ControllerPacket.DOWN_FLAG | ControllerPacket.RIGHT_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_B:
            context.inputMap |= ControllerPacket.B_FLAG;
            break;
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_BUTTON_A:
            context.inputMap |= ControllerPacket.A_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_X:
            context.inputMap |= ControllerPacket.X_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_Y:
            context.inputMap |= ControllerPacket.Y_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_L1:
            context.inputMap |= ControllerPacket.LB_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_R1:
            context.inputMap |= ControllerPacket.RB_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_THUMBL:
            context.inputMap |= ControllerPacket.LS_CLK_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_THUMBR:
            context.inputMap |= ControllerPacket.RS_CLK_FLAG;
            break;
        case KeyEvent.KEYCODE_MEDIA_RECORD: // Xbox Series X Share button
            context.inputMap |= ControllerPacket.MISC_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_1: // PS4/PS5 touchpad button (prior to 4.10)
            context.inputMap |= ControllerPacket.TOUCHPAD_FLAG;
            break;
        case KeyEvent.KEYCODE_BUTTON_L2:
            if (context.leftTriggerAxisUsed) {
                // Suppress this digital event if an analog trigger is active
                return true;
            }
            context.leftTrigger = (byte)0xFF;
            break;
        case KeyEvent.KEYCODE_BUTTON_R2:
            if (context.rightTriggerAxisUsed) {
                // Suppress this digital event if an analog trigger is active
                return true;
            }
            context.rightTrigger = (byte)0xFF;
            break;
        case KeyEvent.KEYCODE_UNKNOWN:
            // Paddles aren't mapped in any of the Android key layout files,
            // so we need to handle the evdev key codes directly.
            if (context.hasPaddles) {
                switch (event.getScanCode()) {
                    case 0x2c4: // BTN_TRIGGER_HAPPY5
                        context.inputMap |= ControllerPacket.PADDLE1_FLAG;
                        break;
                    case 0x2c5: // BTN_TRIGGER_HAPPY6
                        context.inputMap |= ControllerPacket.PADDLE2_FLAG;
                        break;
                    case 0x2c6: // BTN_TRIGGER_HAPPY7
                        context.inputMap |= ControllerPacket.PADDLE3_FLAG;
                        break;
                    case 0x2c7: // BTN_TRIGGER_HAPPY8
                        context.inputMap |= ControllerPacket.PADDLE4_FLAG;
                        break;
                    default:
                        return false;
                }
            }
            else {
                return false;
            }
            break;
        default:
            return false;
        }

        // Start+Back+LB+RB is the quit combo
        if (context.inputMap == (ControllerPacket.BACK_FLAG | ControllerPacket.PLAY_FLAG |
                                 ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG)) {
            // Wait for the combo to lift and then finish the activity
            context.pendingExit = true;
        }

        // Start+LB acts like select for controllers with one button
        if (!context.hasSelect) {
            if (context.inputMap == (ControllerPacket.PLAY_FLAG | ControllerPacket.LB_FLAG) ||
                    (context.inputMap == ControllerPacket.PLAY_FLAG &&
                            event.getEventTime() - context.lastLbUpTime <= MAXIMUM_BUMPER_UP_DELAY_MS))
            {
                context.inputMap &= ~(ControllerPacket.PLAY_FLAG | ControllerPacket.LB_FLAG);
                context.inputMap |= ControllerPacket.BACK_FLAG;

                context.emulatingButtonFlags |= ControllerHandler.EMULATING_SELECT;
            }
        }
        else if (context.needsClickpadEmulation) {
            // Select+LB acts like the clickpad when we're faking a PS4 controller for motion support
            if (context.inputMap == (ControllerPacket.BACK_FLAG | ControllerPacket.LB_FLAG) ||
                    (context.inputMap == ControllerPacket.BACK_FLAG &&
                            event.getEventTime() - context.lastLbUpTime <= MAXIMUM_BUMPER_UP_DELAY_MS))
            {
                context.inputMap &= ~(ControllerPacket.BACK_FLAG | ControllerPacket.LB_FLAG);
                context.inputMap |= ControllerPacket.TOUCHPAD_FLAG;

                context.emulatingButtonFlags |= ControllerHandler.EMULATING_TOUCHPAD;
            }
        }

        // If there is a physical select button, we'll use Start+Select as the special button combo
        // otherwise we'll use Start+RB.
        if (!context.hasMode) {
            if (context.hasSelect) {
                if (context.inputMap == (ControllerPacket.PLAY_FLAG | ControllerPacket.BACK_FLAG)) {
                    context.inputMap &= ~(ControllerPacket.PLAY_FLAG | ControllerPacket.BACK_FLAG);
                    context.inputMap |= ControllerPacket.SPECIAL_BUTTON_FLAG;

                    context.emulatingButtonFlags |= ControllerHandler.EMULATING_SPECIAL;
                }
            }
            else {
                if (context.inputMap == (ControllerPacket.PLAY_FLAG | ControllerPacket.RB_FLAG) ||
                        (context.inputMap == ControllerPacket.PLAY_FLAG &&
                                event.getEventTime() - context.lastRbUpTime <= MAXIMUM_BUMPER_UP_DELAY_MS))
                {
                    context.inputMap &= ~(ControllerPacket.PLAY_FLAG | ControllerPacket.RB_FLAG);
                    context.inputMap |= ControllerPacket.SPECIAL_BUTTON_FLAG;

                    context.emulatingButtonFlags |= ControllerHandler.EMULATING_SPECIAL;
                }
            }
        }

        // We don't need to send repeat key down events, but the platform
        // sends us events that claim to be repeats but they're from different
        // devices, so we just send them all and deal with some duplicates.
        sendControllerInputPacket(context);
        return true;
    }

    public void reportOscState(int buttonFlags,
                               short leftStickX, short leftStickY,
                               short rightStickX, short rightStickY,
                               byte leftTrigger, byte rightTrigger) {
        deviceRegistry.defaultContext.leftStickX = leftStickX;
        deviceRegistry.defaultContext.leftStickY = leftStickY;

        deviceRegistry.defaultContext.rightStickX = rightStickX;
        deviceRegistry.defaultContext.rightStickY = rightStickY;

        deviceRegistry.defaultContext.leftTrigger = leftTrigger;
        deviceRegistry.defaultContext.rightTrigger = rightTrigger;

        deviceRegistry.defaultContext.inputMap = buttonFlags;

        sendControllerInputPacket(deviceRegistry.defaultContext);
    }

    @Override
    public void reportControllerState(int controllerId, int buttonFlags,
                                      float leftStickX, float leftStickY,
                                      float rightStickX, float rightStickY,
                                      float leftTrigger, float rightTrigger) {
        GenericControllerContext context = deviceRegistry.usbDeviceContexts.get(controllerId);
        if (context == null) {
            return;
        }

        Vector2d leftStickVector = populateCachedVector(leftStickX, leftStickY);

        handleDeadZone(leftStickVector, context.leftStickDeadzoneRadius);

        context.leftStickX = (short) (leftStickVector.getX() * 0x7FFE);
        context.leftStickY = (short) (-leftStickVector.getY() * 0x7FFE);

        Vector2d rightStickVector = populateCachedVector(rightStickX, rightStickY);

        handleDeadZone(rightStickVector, context.rightStickDeadzoneRadius);

        context.rightStickX = (short) (rightStickVector.getX() * 0x7FFE);
        context.rightStickY = (short) (-rightStickVector.getY() * 0x7FFE);

        if (leftTrigger <= context.triggerDeadzone) {
            leftTrigger = 0;
        }
        if (rightTrigger <= context.triggerDeadzone) {
            rightTrigger = 0;
        }

        context.leftTrigger = (byte)(leftTrigger * 0xFF);
        context.rightTrigger = (byte)(rightTrigger * 0xFF);

        context.inputMap = buttonFlags;

        sendControllerInputPacket(context);
    }

    @Override
    public void reportControllerMotion(int controllerId, byte motionType, float motionX, float motionY, float motionZ) {
        GenericControllerContext context = deviceRegistry.usbDeviceContexts.get(controllerId);
        if (context == null) {
            return;
        }

        conn.sendControllerMotionEvent((byte)context.getControllerNumber(), motionType, motionX, motionY, motionZ);
    }

    @Override
    public void deviceRemoved(AbstractController controller) {
        deviceRegistry.onUsbDeviceRemoved(controller);
    }

    @Override
    public void deviceAdded(AbstractController controller) {
        deviceRegistry.onUsbDeviceAdded(controller);
    }
}
