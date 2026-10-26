package io.github.staernid.sunbeam.binding.input.evdev;


import android.app.Activity;

import io.github.staernid.sunbeam.BuildConfig;
import io.github.staernid.sunbeam.binding.input.capture.InputCaptureProvider;

public class EvdevCaptureProviderShim {
    public static boolean isCaptureProviderSupported() {
        return false;
    }

    // We need to construct our capture provider using reflection because it isn't included in non-root builds
    public static InputCaptureProvider createEvdevCaptureProvider(Activity activity, EvdevListener listener) {
        try {
            Class providerClass = Class.forName("io.github.staernid.sunbeam.binding.input.evdev.EvdevCaptureProvider");
            return (InputCaptureProvider) providerClass.getConstructors()[0].newInstance(activity, listener);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
