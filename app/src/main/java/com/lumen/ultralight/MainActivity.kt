package com.lumen.ultralight

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.lumen.ultralight.model.Track
import com.lumen.ultralight.playback.PlaybackService
import com.lumen.ultralight.playback.PlayerActions
import com.lumen.ultralight.playback.PlayerState
import com.lumen.ultralight.ui.LumenApp
import com.lumen.ultralight.ui.LumenColorScheme
import kotlin.math.log10

class MainActivity : ComponentActivity() {

    private val playerState = PlayerState()
    private lateinit var prefs: SharedPreferences

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playerState.isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            pushCurrent(mediaItem)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            playerState.shuffle.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            playerState.repeatMode.value = repeatMode
        }
    }

    private val actions = PlayerActions(
        play = ::play,
        toggle = ::togglePlay,
        next = { controller?.seekToNext() },
        prev = { controller?.seekToPrevious() },
        seek = { controller?.seekTo(it) },
        toggleShuffle = { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } },
        cycleRepeat = ::cycleRepeat,
        setVolume = ::setVolume,
        position = { controller?.currentPosition ?: 0L },
        duration = { controller?.duration ?: 0L },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("lumen", Context.MODE_PRIVATE)
        playerState.volume.value = prefs.getFloat("volume", 0.9f).coerceIn(0f, 1.5f)

        setContent {
            MaterialTheme(colorScheme = LumenColorScheme) {
                Surface(Modifier.fillMaxSize()) {
                    LumenApp(playerState = playerState, actions = actions)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture.addListener({
            val c = controllerFuture.get()
            controller = c
            c.addListener(playerListener)
            playerState.isPlaying.value = c.isPlaying
            playerState.shuffle.value = c.shuffleModeEnabled
            playerState.repeatMode.value = c.repeatMode
            pushCurrent(c.currentMediaItem)
            setVolume(playerState.volume.value) // apply saved volume + gain
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        controller?.removeListener(playerListener)
        controller = null
        MediaController.releaseFuture(controllerFuture)
        super.onStop()
    }

    private fun pushCurrent(item: MediaItem?) {
        playerState.currentId.value = item?.mediaId?.toLongOrNull()
        playerState.title.value = item?.mediaMetadata?.title?.toString()
        playerState.artist.value = item?.mediaMetadata?.artist?.toString()
        playerState.artUri.value = item?.mediaMetadata?.artworkUri?.toString()
    }

    private fun play(tracks: List<Track>, index: Int) {
        val c = controller ?: return
        c.setMediaItems(tracks.map { it.toMediaItem() }, index, 0L)
        c.prepare()
        c.play()
    }

    private fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    private fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private fun setVolume(raw: Float) {
        val v = raw.coerceIn(0f, 1.5f)
        playerState.volume.value = v
        prefs.edit().putFloat("volume", v).apply()

        val c = controller ?: return
        c.volume = v.coerceAtMost(1f)
        // Above 100%, boost with the service-side LoudnessEnhancer. +amplitude in mB.
        val gainMb = if (v > 1f) (20.0 * log10(v.toDouble()) * 100).toInt() else 0
        val args = Bundle().apply { putInt(PlaybackService.KEY_GAIN_MB, gainMb) }
        c.sendCustomCommand(SessionCommand(PlaybackService.CMD_SET_GAIN, Bundle.EMPTY), args)
    }
}

private fun Track.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .apply { artUri?.let { setArtworkUri(it) } }
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}
