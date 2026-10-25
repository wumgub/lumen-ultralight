package com.lumen.ultralight.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumen.ultralight.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val scanner = MediaStoreScanner(app)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks = _tracks.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()

    /** Scan once; safe to call repeatedly (ignored while a scan is in flight). */
    fun scan() {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            val list = withContext(Dispatchers.IO) { scanner.scan() }
            _tracks.value = list
            _scanning.value = false
        }
    }
}
