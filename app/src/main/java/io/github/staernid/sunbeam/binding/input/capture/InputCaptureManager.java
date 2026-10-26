package io.github.staernid.sunbeam.binding.input.capture;

import android.app.Activity;

import io.github.staernid.sunbeam.BuildConfig;
import io.github.staernid.sunbeam.LimeLog;
import io.github.staernid.sunbeam.R;
import io.github.staernid.sunbeam.binding.input.evdev.EvdevCaptureProviderShim;
import io.github.staernid.sunbeam.binding.input.evdev.EvdevListener;

public class InputCaptureManager {
    public static InputCaptureProvider getInputCaptureProvider(Activity activity, EvdevListener rootListener) {
        if (AndroidNativePointerCaptureProvider.isCaptureProviderSupported()) {
            LimeLog.info("Using Android O+ native mouse capture");
            return new AndroidNativePointerCaptureProvider(activity, activity.findViewById(R.id.streamContainer));
        }
        else if (ShieldCaptureProvider.isCaptureProviderSupported()) {
            LimeLog.info("Using NVIDIA mouse capture extension");
            return new ShieldCaptureProvider(activity);
        }
        else if (AndroidPointerIconCaptureProvider.isCaptureProviderSupported()) {
            // Android N's native capture can't capture over system UI elements
            // so we want to only use it if there's no other option.
            LimeLog.info("Using Android N+ pointer hiding");
            return new AndroidPointerIconCaptureProvider(activity, activity.findViewById(R.id.streamContainer));
        }
        else {
            LimeLog.info("Mouse capture not available");
            return new NullCaptureProvider();
        }
    }
}
