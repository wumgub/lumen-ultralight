package com.lumen.ultralight.playback

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Hosts the ExoPlayer in a MediaSessionService so playback keeps running in the
 * background with lockscreen / notification controls. A LoudnessEnhancer bolted
 * onto a fixed audio session lets volume go past 100% (up to ~150%), driven from
 * the UI via a custom session command.
 */
class PlaybackService : MediaSessionService() {

    companion object {
        const val CMD_SET_GAIN = "com.lumen.ultralight.SET_GAIN"
        const val KEY_GAIN_MB = "mb"
    }

    private var session: MediaSession? = null
    private var player: ExoPlayer? = null
    private var enhancer: LoudnessEnhancer? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Fix the audio session id up front so we can attach the gain effect once.
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val sessionId = am.generateAudioSessionId()
            exo.setAudioSessionId(sessionId)
            enhancer = LoudnessEnhancer(sessionId).apply { setEnabled(false) }
        } catch (_: Exception) {
            // Some devices may refuse the effect; volume simply caps at 100%.
        }

        player = exo
        session = MediaSession.Builder(this, exo)
            .setCallback(GainCallback())
            .build()
    }

    private inner class GainCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(CMD_SET_GAIN, Bundle.EMPTY))
                    .build()
            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CMD_SET_GAIN) {
                applyGain(args.getInt(KEY_GAIN_MB, 0))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    private fun applyGain(gainMb: Int) {
        val fx = enhancer ?: return
        try {
            fx.setTargetGain(gainMb)
            fx.setEnabled(gainMb > 0)
        } catch (_: Exception) {
            // Ignore; leave at unity gain.
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        enhancer?.release()
        enhancer = null
        session?.run {
            player.release()
            release()
        }
        session = null
        player = null
        super.onDestroy()
    }
}
