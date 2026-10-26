# Keep JNI entry points and native callbacks
-keep class io.github.staernid.sunbeam.nvstream.jni.MoonBridge { *; }
-keep class io.github.staernid.sunbeam.nvstream.NvConnectionListener { *; }
-keep class io.github.staernid.sunbeam.nvstream.av.audio.AudioRenderer { *; }
-keep class io.github.staernid.sunbeam.nvstream.av.video.VideoDecoderRenderer { *; }
-keep class io.github.staernid.sunbeam.nvstream.av.ByteBufferDescriptor { *; }
