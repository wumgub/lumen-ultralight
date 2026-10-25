package com.lumen.ultralight.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lumen.ultralight.R
import com.lumen.ultralight.library.LibraryViewModel
import com.lumen.ultralight.model.Track
import com.lumen.ultralight.playback.PlayerActions
import com.lumen.ultralight.playback.PlayerState
import kotlinx.coroutines.delay

private sealed interface Detail {
    data class Artist(val name: String) : Detail
    data class Album(val albumId: Long, val name: String) : Detail
}

private data class ArtistItem(val name: String, val count: Int)
private data class AlbumItem(
    val albumId: Long,
    val name: String,
    val artist: String,
    val artUri: Uri?,
    val count: Int,
)

private const val UNKNOWN_ARTIST = "Unknown artist"
private const val UNKNOWN_ALBUM = "Unknown album"

@Composable
fun LumenApp(playerState: PlayerState, actions: PlayerActions) {
    val vm: LibraryViewModel = viewModel()
    val context = LocalContext.current

    var granted by remember { mutableStateOf(hasAudioPermission(context)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }
    LaunchedEffect(granted) { if (granted) vm.scan() }

    if (!granted) {
        PermissionScreen { launcher.launch(audioPermission()) }
        return
    }

    val tracks by vm.tracks.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val currentId by playerState.currentId.collectAsState()
    val title by playerState.title.collectAsState()

    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<Detail?>(null) }
    var showNowPlaying by remember { mutableStateOf(false) }

    BackHandler(showNowPlaying) { showNowPlaying = false }
    BackHandler(!showNowPlaying && query.isNotEmpty()) { query = "" }
    BackHandler(!showNowPlaying && query.isEmpty() && detail != null) { detail = null }

    Box(Modifier.fillMaxSize().background(LumenBg)) {
        Scaffold(
            containerColor = LumenBg,
            bottomBar = {
                Column {
                    if (title != null) {
                        MiniPlayer(playerState, actions, onExpand = { showNowPlaying = true })
                    }
                    BottomTabs(selected = tab) {
                        tab = it
                        detail = null
                        query = ""
                    }
                }
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                SearchBar(query) { query = it }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    val d = detail
                    when {
                        query.isNotBlank() ->
                            SongList(filterTracks(tracks, query), currentId, actions)
                        d is Detail.Artist -> {
                            val list = remember(tracks, d) {
                                tracks.filter { (it.artist.ifBlank { UNKNOWN_ARTIST }) == d.name }
                                    .sortedWith(compareBy({ it.album.lowercase() }, { it.trackNo }))
                            }
                            DetailScreen(d.name, "${list.size} songs", list, currentId, actions) {
                                detail = null
                            }
                        }
                        d is Detail.Album -> {
                            val list = remember(tracks, d) {
                                tracks.filter { it.albumId == d.albumId }.sortedBy { it.trackNo }
                            }
                            DetailScreen(d.name, list.firstOrNull()?.artist?.ifBlank { UNKNOWN_ARTIST } ?: "",
                                list, currentId, actions) { detail = null }
                        }
                        scanning && tracks.isEmpty() -> CenterText("Scanning your music…")
                        tracks.isEmpty() -> EmptyScreen { vm.scan() }
                        tab == 0 -> SongList(tracks, currentId, actions)
                        tab == 1 -> ArtistsScreen(tracks) { detail = Detail.Artist(it) }
                        else -> AlbumsScreen(tracks) { detail = Detail.Album(it.albumId, it.name) }
                    }
                }
            }
        }

        if (showNowPlaying) {
            NowPlaying(playerState, actions) { showNowPlaying = false }
        }
    }
}

@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        placeholder = { Text("Search", color = LumenDim) },
        leadingIcon = {
            Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = LumenDim)
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = LumenBg2,
            unfocusedContainerColor = LumenBg2,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = LumenFg,
            unfocusedTextColor = LumenFg,
            cursorColor = LumenAccent,
        ),
    )
}

@Composable
private fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = LumenBg2) {
        NavItem(selected == 0, { onSelect(0) }, R.drawable.ic_songs, "Songs")
        NavItem(selected == 1, { onSelect(1) }, R.drawable.ic_artists, "Artists")
        NavItem(selected == 2, { onSelect(2) }, R.drawable.ic_albums, "Albums")
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    iconRes: Int,
    label: String,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(painterResource(iconRes), contentDescription = label) },
        label = { Text(label, fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = LumenBg,
            indicatorColor = LumenAccent,
            selectedTextColor = LumenAccent,
            unselectedIconColor = LumenDim,
            unselectedTextColor = LumenDim,
        ),
    )
}

@Composable
private fun SongList(tracks: List<Track>, currentId: Long?, actions: PlayerActions) {
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
            SongRow(track, track.id == currentId) { actions.play(tracks, index) }
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
        Artwork(track.artUri, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title.ifBlank { "Unknown title" },
                color = if (active) LumenAccent else LumenFg,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artist.ifBlank { UNKNOWN_ARTIST },
                color = LumenDim,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(formatDuration(track.durationMs), color = LumenDim, fontSize = 12.sp)
    }
}

@Composable
private fun ArtistsScreen(tracks: List<Track>, onOpen: (String) -> Unit) {
    val artists = remember(tracks) {
        tracks.groupBy { it.artist.ifBlank { UNKNOWN_ARTIST } }
            .map { (name, list) -> ArtistItem(name, list.size) }
            .sortedBy { it.name.lowercase() }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(artists, key = { _, a -> a.name }) { _, a ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpen(a.name) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_artists),
                    contentDescription = null,
                    tint = LumenDim,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.name, color = LumenFg, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${a.count} songs", color = LumenDim, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AlbumsScreen(tracks: List<Track>, onOpen: (AlbumItem) -> Unit) {
    val albums = remember(tracks) {
        tracks.groupBy { it.albumId to it.album.ifBlank { UNKNOWN_ALBUM } }
            .map { (key, list) ->
                AlbumItem(
                    albumId = key.first,
                    name = key.second,
                    artist = list.first().artist.ifBlank { UNKNOWN_ARTIST },
                    artUri = list.firstOrNull { it.artUri != null }?.artUri,
                    count = list.size,
                )
            }
            .sortedBy { it.name.lowercase() }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        items(albums, key = { it.albumId.toString() + it.name }) { album ->
            Column(
                Modifier.padding(6.dp).clickable { onOpen(album) },
            ) {
                Artwork(album.artUri, Modifier.fillMaxWidth().aspectRatio(1f), cornerDp = 10.dp, decodePx = 320)
                Spacer(Modifier.height(6.dp))
                Text(album.name, color = LumenFg, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(album.artist, color = LumenDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DetailScreen(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    currentId: Long?,
    actions: PlayerActions,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), contentDescription = "Back", tint = LumenFg)
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = LumenFg, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) Text(subtitle, color = LumenDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        SongList(tracks, currentId, actions)
    }
}

@Composable
private fun MiniPlayer(playerState: PlayerState, actions: PlayerActions, onExpand: () -> Unit) {
    val title by playerState.title.collectAsState()
    val artist by playerState.artist.collectAsState()
    val art by playerState.artUri.collectAsState()
    val isPlaying by playerState.isPlaying.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(LumenBg3)
            .clickable(onClick = onExpand)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(art?.let { Uri.parse(it) }, Modifier.size(44.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title ?: "", color = LumenFg, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist ?: "", color = LumenDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = actions.prev) {
            Icon(painterResource(R.drawable.ic_prev), contentDescription = "Previous", tint = LumenFg)
        }
        IconButton(onClick = actions.toggle) {
            Icon(
                painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = "Play/pause",
                tint = LumenAccent,
            )
        }
        IconButton(onClick = actions.next) {
            Icon(painterResource(R.drawable.ic_next), contentDescription = "Next", tint = LumenFg)
        }
    }
}

@Composable
private fun NowPlaying(playerState: PlayerState, actions: PlayerActions, onClose: () -> Unit) {
    val title by playerState.title.collectAsState()
    val artist by playerState.artist.collectAsState()
    val art by playerState.artUri.collectAsState()
    val isPlaying by playerState.isPlaying.collectAsState()
    val currentId by playerState.currentId.collectAsState()
    val shuffle by playerState.shuffle.collectAsState()
    val repeatMode by playerState.repeatMode.collectAsState()

    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableStateOf(0f) }

    LaunchedEffect(currentId) {
        while (true) {
            if (!dragging) {
                position = actions.position()
                duration = actions.duration().coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    Surface(color = LumenBg, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(painterResource(R.drawable.ic_back), contentDescription = "Close", tint = LumenFg)
                }
                Text("Now playing", color = LumenDim, fontSize = 13.sp)
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Artwork(art?.let { Uri.parse(it) }, Modifier.fillMaxWidth(0.82f).aspectRatio(1f), cornerDp = 16.dp, decodePx = null)
            }

            Text(title ?: "", color = LumenFg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist ?: "", color = LumenDim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(Modifier.height(12.dp))

            val frac = when {
                dragging -> dragFrac
                duration > 0 -> (position.toFloat() / duration).coerceIn(0f, 1f)
                else -> 0f
            }
            Slider(
                value = frac,
                onValueChange = { dragging = true; dragFrac = it },
                onValueChangeFinished = {
                    actions.seek((dragFrac * duration).toLong())
                    dragging = false
                },
                colors = sliderColors(),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatDuration(if (dragging) (dragFrac * duration).toLong() else position), color = LumenDim, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text(formatDuration(duration), color = LumenDim, fontSize = 11.sp)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = actions.toggleShuffle) {
                    Icon(painterResource(R.drawable.ic_shuffle), contentDescription = "Shuffle", tint = if (shuffle) LumenAccent else LumenDim)
                }
                IconButton(onClick = actions.prev) {
                    Icon(painterResource(R.drawable.ic_prev), contentDescription = "Previous", tint = LumenFg, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = actions.toggle, modifier = Modifier.size(64.dp)) {
                    Icon(
                        painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = "Play/pause",
                        tint = LumenAccent,
                        modifier = Modifier.size(44.dp),
                    )
                }
                IconButton(onClick = actions.next) {
                    Icon(painterResource(R.drawable.ic_next), contentDescription = "Next", tint = LumenFg, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = actions.cycleRepeat) {
                    Icon(
                        painterResource(if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat),
                        contentDescription = "Repeat",
                        tint = if (repeatMode == Player.REPEAT_MODE_OFF) LumenDim else LumenAccent,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = LumenAccent,
    activeTrackColor = LumenAccent,
    inactiveTrackColor = LumenBg3,
)

@Composable
private fun Artwork(uri: Uri?, modifier: Modifier, cornerDp: Dp = 6.dp, decodePx: Int? = 128) {
    Box(
        modifier.clip(RoundedCornerShape(cornerDp)).background(LumenBg2),
        contentAlignment = Alignment.Center,
    ) {
        if (uri != null) {
            val context = LocalContext.current
            val model = remember(uri, decodePx) {
                ImageRequest.Builder(context).data(uri).apply {
                    if (decodePx != null) size(decodePx, decodePx)
                }.build()
            }
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text("♪", color = LumenDim, fontSize = 20.sp)
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(LumenBg).padding(32.dp),
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
        Modifier.fillMaxSize().padding(32.dp),
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
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = LumenDim, fontSize = 15.sp)
    }
}

private fun filterTracks(tracks: List<Track>, query: String): List<Track> {
    val q = query.trim()
    return tracks.filter {
        it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
