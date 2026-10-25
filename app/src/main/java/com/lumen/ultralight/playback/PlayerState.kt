package com.lumen.ultralight.playback

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Snapshot of what's playing, pushed from the MediaController's listener and
 * observed by the Compose UI. Kept deliberately small for now.
 */
class PlayerState {
    val isPlaying = MutableStateFlow(false)
    val currentId = MutableStateFlow<Long?>(null)
    val title = MutableStateFlow<String?>(null)
    val artist = MutableStateFlow<String?>(null)
    val artUri = MutableStateFlow<String?>(null)
}
