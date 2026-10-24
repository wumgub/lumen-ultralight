package com.lumen.ultralight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Lumen's default "neon" palette, enough for the Phase 0 placeholder screen.
private val LumenBg = Color(0xFF0A0A12)
private val LumenFg = Color(0xFFE8E8F0)
private val LumenAccent = Color(0xFF5BE9D6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = LumenBg,
                    surface = LumenBg,
                    primary = LumenAccent,
                    onBackground = LumenFg,
                    onSurface = LumenFg,
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Placeholder()
                }
            }
        }
    }
}

@Composable
private fun Placeholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Lumen",
            color = LumenAccent,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Ultralight",
            color = LumenFg,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
        )
        Text(
            text = "Native build · coming to life",
            color = LumenFg.copy(alpha = 0.6f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
