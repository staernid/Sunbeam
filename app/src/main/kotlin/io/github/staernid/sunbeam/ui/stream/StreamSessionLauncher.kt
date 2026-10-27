@file:Suppress(
    "LongParameterList",
    "CyclomaticComplexMethod",
    "ReturnCount",
)

package io.github.staernid.sunbeam.ui.stream

import android.content.Context
import android.content.Intent
import io.github.staernid.sunbeam.Game
import io.github.staernid.sunbeam.computers.IdentityManager
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.core.data.repository.AppRepository
import io.github.staernid.sunbeam.core.data.repository.HostRepository
import io.github.staernid.sunbeam.core.data.repository.ProfileRepository
import io.github.staernid.sunbeam.core.preferences.PreferencesRepository
import io.github.staernid.sunbeam.core.preferences.models.AudioConfiguration
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.stream.model.AudioChannelConfiguration
import io.github.staernid.sunbeam.core.stream.model.StreamConfig
import io.github.staernid.sunbeam.core.stream.model.VideoFormat
import io.github.staernid.sunbeam.features.profiles.model.bitrateKbps
import io.github.staernid.sunbeam.features.profiles.model.fps
import io.github.staernid.sunbeam.features.profiles.model.height
import io.github.staernid.sunbeam.features.profiles.model.width
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Data and lifecycle bridge wiring [HostRepository], [AppRepository], [PreferencesRepository],
 * and [ProfileRepository] into the streaming session launcher.
 */
class StreamSessionLauncher(
    private val hostRepository: HostRepository,
    private val appRepository: AppRepository,
    private val preferencesRepository: PreferencesRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend fun resolveStreamConfig(
        hostUuid: String,
        appId: Int,
    ): Result<StreamConfig> {
        val hosts = hostRepository.hostsFlow.first()
        val host = hosts.find { it.uuid == hostUuid }
            ?: return Result.failure(IllegalArgumentException("Host not found for uuid: $hostUuid"))

        val targetAddress = host.localAddress ?: host.remoteAddress ?: host.manualAddress ?: host.ipv6Address
            ?: return Result.failure(IllegalStateException("No reachable address configured for host ${host.name}"))

        val apps = appRepository.getApps(hostUuid, showHidden = true).first()
        val app = apps.find { it.id == appId }
            ?: return Result.failure(IllegalArgumentException("App not found for id: $appId"))

        val settings = preferencesRepository.getSettings()
        val activeProfile = profileRepository.activeProfileFlow.firstOrNull()

        val width = activeProfile?.width ?: settings.width
        val height = activeProfile?.height ?: settings.height
        val fps = activeProfile?.fps ?: settings.fps
        val bitrateKbps = activeProfile?.bitrateKbps ?: settings.bitrateKbps

        val audioConfig =
            when (settings.audioConfig) {
                AudioConfiguration.STEREO -> AudioChannelConfiguration.STEREO
                AudioConfiguration.SURROUND_51 -> AudioChannelConfiguration.SURROUND_51
                AudioConfiguration.SURROUND_71 -> AudioChannelConfiguration.SURROUND_71
            }

        val videoFormat =
            when (settings.videoFormat) {
                VideoFormatOption.AUTO -> VideoFormat.AUTO
                VideoFormatOption.FORCE_H264 -> VideoFormat.H264
                VideoFormatOption.FORCE_HEVC -> VideoFormat.HEVC
                VideoFormatOption.FORCE_AV1 -> VideoFormat.AV1
            }

        val enableHdr = settings.enableHdr && app.isHdrSupported

        val serverCert: X509Certificate? =
            host.serverCertDer?.let { certBytes ->
                try {
                    val certFactory = CertificateFactory.getInstance("X.509")
                    certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as? X509Certificate
                } catch (_: Exception) {
                    null
                }
            }

        val config =
            StreamConfig(
                hostAddress = targetAddress.address,
                httpsPort = targetAddress.port,
                width = width,
                height = height,
                fps = fps,
                bitrateKbps = bitrateKbps,
                audioConfiguration = audioConfig,
                videoFormat = videoFormat,
                enableHdr = enableHdr,
                clientCert = serverCert,
            )

        return Result.success(config)
    }

    fun createGameActivityIntent(
        context: Context,
        host: Host,
        app: HostApp,
    ): Intent {
        val targetAddress = host.localAddress ?: host.remoteAddress ?: host.manualAddress ?: host.ipv6Address
        val uniqueId = IdentityManager(context).uniqueId

        return Intent(context, Game::class.java).apply {
            if (targetAddress != null) {
                putExtra(Game.EXTRA_HOST, targetAddress.address)
                putExtra(Game.EXTRA_PORT, targetAddress.port)
                putExtra(Game.EXTRA_HTTPS_PORT, targetAddress.port)
            }
            putExtra(Game.EXTRA_APP_NAME, app.title)
            putExtra(Game.EXTRA_APP_ID, app.id)
            putExtra(Game.EXTRA_APP_UUID, app.uuid)
            putExtra(Game.EXTRA_APP_HDR, app.isHdrSupported)
            putExtra(Game.EXTRA_PC_UUID, host.uuid)
            putExtra(Game.EXTRA_PC_NAME, host.name)
            putExtra(Game.EXTRA_UNIQUEID, uniqueId)
            host.serverCertDer?.let { putExtra(Game.EXTRA_SERVER_CERT, it) }
        }
    }

    fun launchGameActivity(
        context: Context,
        host: Host,
        app: HostApp,
    ) {
        val intent = createGameActivityIntent(context, host, app)
        context.startActivity(intent)
    }
}
