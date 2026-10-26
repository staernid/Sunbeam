package io.github.staernid.sunbeam.binding;

import android.content.Context;

import io.github.staernid.sunbeam.binding.audio.AndroidAudioRenderer;
import io.github.staernid.sunbeam.binding.crypto.AndroidCryptoProvider;
import io.github.staernid.sunbeam.nvstream.av.audio.AudioRenderer;
import io.github.staernid.sunbeam.nvstream.http.LimelightCryptoProvider;

public class PlatformBinding {
    public static LimelightCryptoProvider getCryptoProvider(Context c) {
        return new AndroidCryptoProvider(c);
    }
}
