package com.vsk.orders.audio

import android.media.MediaPlayer

class VoicePlayer {

    private var player: MediaPlayer? = null

    fun play(source: String, onComplete: () -> Unit) {
        stop()
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        mediaPlayer.setDataSource(source)
        mediaPlayer.setOnPreparedListener { it.start() }
        mediaPlayer.setOnCompletionListener {
            stop()
            onComplete()
        }
        mediaPlayer.prepareAsync()
    }

    fun stop() {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: Exception) {
                // player wasn't in a playable state; nothing to stop
            }
            it.release()
        }
        player = null
    }

    val isPlaying: Boolean get() = player?.isPlaying == true
}
