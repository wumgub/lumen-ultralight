package com.lumen.ultralight.playback

import com.lumen.ultralight.model.Track

/** Callbacks the UI uses to drive playback, supplied by MainActivity. */
class PlayerActions(
    val play: (tracks: List<Track>, index: Int) -> Unit,
    val toggle: () -> Unit,
    val next: () -> Unit,
    val prev: () -> Unit,
    val seek: (positionMs: Long) -> Unit,
    val toggleShuffle: () -> Unit,
    val cycleRepeat: () -> Unit,
    val position: () -> Long,
    val duration: () -> Long,
)
