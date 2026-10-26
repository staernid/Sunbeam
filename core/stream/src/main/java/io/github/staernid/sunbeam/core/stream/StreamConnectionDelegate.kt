package io.github.staernid.sunbeam.core.stream

import io.github.staernid.sunbeam.core.stream.model.StreamConfig
import io.github.staernid.sunbeam.nvstream.NvConnectionListener

fun interface StreamConnectionDelegate {
    suspend fun start(
        config: StreamConfig,
        listener: NvConnectionListener,
    ): Result<Unit>
}
