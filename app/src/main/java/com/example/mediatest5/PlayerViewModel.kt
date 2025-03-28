package com.example.mediatest5

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerViewModel(
    val player: ExoPlayer,
    val trackSelector: DefaultTrackSelector,
) : ViewModel() {

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val context = this[APPLICATION_KEY]!!
                val trackSelector = DefaultTrackSelector(context)
                PlayerViewModel(
                    player = Helper.createPlayer(context, trackSelector),
                    trackSelector = trackSelector,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }

}