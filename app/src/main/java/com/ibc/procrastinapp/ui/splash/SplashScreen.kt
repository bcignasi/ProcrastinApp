/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.splash

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@OptIn(UnstableApi::class)
@Composable
fun SplashScreen(
    videoResId: Int,
    onVideoComplete: () -> Unit
) {

    // Estado para controlar si el video ha finalizado
    var isVideoComplete by remember { mutableStateOf(false) }
    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2B4074))
            .clickable {
                if (!isVideoComplete) {
                    isVideoComplete = true
                    exoPlayer?.stop() // 🔴 Detenemos el vídeo
                    onVideoComplete()
                }
            }
    ) {
        // Usamos AndroidView para integrar ExoPlayer
        AndroidView(
            factory = { ctx ->
                // Creamos el ExoPlayer
                val player = ExoPlayer.Builder(ctx).build().apply {
                    // Convertimos el ID del recurso a Uri
                    val videoUri = "android.resource://${ctx.packageName}/$videoResId".toUri()
                    // Creamos el MediaItem a partir del Uri
                    val mediaItem = MediaItem.fromUri(videoUri)
                    // Establecemos el MediaItem en el reproductor
                    setMediaItem(mediaItem)
                    // Preparamos el reproductor
                    prepare()
                    // Reproducimos el video
                    playWhenReady = true
                    // Repetición desactivada
                    repeatMode = Player.REPEAT_MODE_OFF
                    
                    // Listener para detectar cuando el video ha terminado
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_ENDED && !isVideoComplete) {
                                isVideoComplete = true
                                onVideoComplete()
                            }
                        }
                    })
                }

                exoPlayer = player  // 🔴 Guardamos la referencia

                // Creamos la vista del reproductor
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false // Ocultamos los controles de reproducción
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
        )
        // Este texto se superpone encima del video
        Text(
            text = "Click to skip",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .zIndex(1f), // Asegura que esté encima
            color = Color.White
        )
    }
}
