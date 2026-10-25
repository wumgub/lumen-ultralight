package com.lumen.ultralight

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.lumen.ultralight.library.LibraryViewModel
import com.lumen.ultralight.model.Track
import com.lumen.ultralight.playback.PlaybackService
import com.lumen.ultralight.playback.PlayerState

private val LumenBg = Color(0xFF0A0A12)
private val LumenBg2 = Color(0xFF14141F)
private val LumenFg = Color(0xFFE8E8F0)
private val LumenDim = Color(0xFF9A9AB0)
private val LumenAccent = Color(0xFF5BE9D6)

private val LumenColors = darkColorScheme(
    background = LumenBg,
    surface = LumenBg,
    primary = LumenAccent,
    onPrimary = LumenBg,
    onBackground = LumenFg,
    onSurface = LumenFg,
)

class MainActivity : ComponentActivity() {

    private val playerState = PlayerState()

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playerState.isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            pushCurrent(mediaItem)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = LumenColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Root(
                        playerState = playerState,
                        onPlay = ::play,
                        onToggle = ::togglePlay,
                        onNext = ::next,
                        onPrev = ::prev,
                    )
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
            pushCurrent(c.currentMediaItem)
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

    private fun next() {
        controller?.seekToNext()
    }

    private fun prev() {
        controller?.seekToPrevious()
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

private val audioPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

@Composable
private fun Root(
    playerState: PlayerState,
    onPlay: (List<Track>, Int) -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
    val context = LocalContext.current
    val vm: LibraryViewModel = viewModel()

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok -> granted = ok }

    LaunchedEffect(granted) { if (granted) vm.scan() }

    val tracks by vm.tracks.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val title by playerState.title.collectAsState()
    val currentId by playerState.currentId.collectAsState()
    val isPlaying by playerState.isPlaying.collectAsState()
    val artist by playerState.artist.collectAsState()
    val art by playerState.artUri.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                !granted -> PermissionScreen { launcher.launch(audioPermission) }
                scanning && tracks.isEmpty() -> CenterText("Scanning your music…")
                tracks.isEmpty() -> EmptyScreen { vm.scan() }
                else -> SongsList(tracks = tracks, currentId = currentId, onPlay = onPlay)
            }
        }
        if (title != null) {
            MiniPlayer(
                title = title ?: "",
                artist = artist ?: "",
                artUri = art,
                isPlaying = isPlaying,
                onToggle = onToggle,
                onNext = onNext,
                onPrev = onPrev,
            )
        }
    }
}

@Composable
private fun SongsList(
    tracks: List<Track>,
    currentId: Long?,
    onPlay: (List<Track>, Int) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items = tracks, key = { _, t -> t.id }) { index, track ->
            SongRow(
                track = track,
                active = track.id == currentId,
                onClick = { onPlay(tracks, index) },
            )
        }
    }
}

@Composable
private fun SongRow(track: Track, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(uri = track.artUri, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title.ifEmpty { "Unknown title" },
                color = if (active) LumenAccent else LumenFg,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist.ifEmpty { "Unknown artist" },
                color = LumenDim,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text = formatDuration(track.durationMs), color = LumenDim, fontSize = 12.sp)
    }
}

@Composable
private fun Artwork(uri: android.net.Uri?, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(LumenBg2),
        contentAlignment = Alignment.Center,
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text("♪", color = LumenDim, fontSize = (size.value / 2).sp)
        }
    }
}

@Composable
private fun MiniPlayer(
    title: String,
    artist: String,
    artUri: String?,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(LumenBg2)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(uri = artUri?.let { android.net.Uri.parse(it) }, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LumenFg, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, color = LumenDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onPrev) {
            Text("⏮", color = LumenFg, fontSize = 18.sp)
        }
        IconButton(onClick = onToggle) {
            Text(if (isPlaying) "⏸" else "▶", color = LumenAccent, fontSize = 18.sp)
        }
        IconButton(onClick = onNext) {
            Text("⏭", color = LumenFg, fontSize = 18.sp)
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Lumen Ultralight", color = LumenAccent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "To play your music, Lumen needs permission to read the audio on your phone.",
            color = LumenDim,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequest) { Text("Grant access") }
    }
}

@Composable
private fun EmptyScreen(onRescan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No music found", color = LumenFg, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text("Nothing turned up in your music folders.", color = LumenDim, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRescan) { Text("Scan again") }
    }
}

@Composable
private fun CenterText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = LumenDim, fontSize = 15.sp)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
