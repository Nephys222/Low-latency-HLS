package com.example.mediatest5

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.SoftwareKeyboardControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.mediatest5.Helper.updateParameters

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerActivity: AppCompatActivity() {
    private var playerView: PlayerView? = null

    private val viewModel: PlayerViewModel by viewModels { PlayerViewModel.Factory }

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            try {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    viewModel.player.apply {
                        seekToDefaultPosition()
                        prepare()
                    }
                } else {
                    viewModel.player.apply {
                        prepare()
                        play()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.player_view)
        val streamIdEditText = findViewById<EditText>(R.id.stream_id_edit_text)
        val submitButton = findViewById<Button>(R.id.submit_button)

        submitButton.setOnClickListener {
            val streamId = streamIdEditText.getText().toString()
            if (streamId.isNotEmpty()) {
                playStream(streamId)
            } else {
                // Handle empty stream ID
            }
            SoftwareKeyboardControllerCompat(submitButton).hide()
        }
    }

    private fun playStream(streamId: String) {
        val hlsUrl = "https://smloven.b-cdn.net/app/$streamId/llhls.m3u8"

        viewModel.player.setWakeMode(C.WAKE_MODE_NETWORK)
        viewModel.player.addListener(playerListener)

        viewModel.trackSelector.updateParameters {
            setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            setMinVideoSize(Int.MIN_VALUE, Int.MIN_VALUE)
        }

        playerView?.player = viewModel.player
        viewModel.player.playbackParameters = PlaybackParameters(1.0f, 1.0f)

        viewModel.player.setMediaItem(
            MediaItem.Builder()
                .setUri(hlsUrl)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
        )

        viewModel.player.prepare()
        viewModel.player.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.player.release()
        viewModel.player.removeListener(playerListener)
    }
}