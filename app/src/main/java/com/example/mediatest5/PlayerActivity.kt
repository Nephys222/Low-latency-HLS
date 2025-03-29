package com.example.mediatest5

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.SoftwareKeyboardControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.DefaultLivePlaybackSpeedControl
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.example.mediatest5.Helper.cronetEngine
import com.example.mediatest5.Helper.updateParameters
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerActivity: AppCompatActivity() {
    private var playerView: PlayerView? = null

    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector

    private val cronetDataSourceFactory = CronetDataSource.Factory(
        cronetEngine,
        Executors.newCachedThreadPool()
    )

    private val handler = Handler(Looper.getMainLooper())

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Log.d("Latency", "Error: ${error.localizedMessage}")
            try {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.apply {
                        seekToDefaultPosition()
                        prepare()
                    }
                } else {
                    player.apply {
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

        createPlayer(this)

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

        trackSelector.updateParameters {
            setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            setMinVideoSize(Int.MIN_VALUE, Int.MIN_VALUE)
        }

        playerView?.player = player
        playerView?.controllerShowTimeoutMs = 3_000
        player.playbackParameters = PlaybackParameters(1.0f, 1.0f)

        player.setMediaItem(
            MediaItem.Builder()
                .setUri(hlsUrl)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
        )

        player.prepare()
        player.play()

        handler.postDelayed(latencyRunnable, 5_000)
    }

    private fun createPlayer(context: Context) {
        val dataSourceFactory = DefaultDataSource.Factory(context, cronetDataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val loadControl = DefaultLoadControl.Builder()
//            .setBackBuffer(10_000, true)
            .setBufferDurationsMs(
                500, // minBufferMs: Minimum buffer before playback resumes after re-buffering
                2000, // maxBufferMs: Maximum buffer to avoid loading too far ahead
                250, // bufferForPlaybackMs: Buffer needed to start playback
                500 // bufferForPlaybackAfterRebufferMs: Buffer needed after re-buffering
            )
            .setPrioritizeTimeOverSizeThresholds(true) // Prioritize time-based buffering
            .build()

        trackSelector = DefaultTrackSelector(this)

        val liveSpeedControl = DefaultLivePlaybackSpeedControl.Builder()
            .setMinUpdateIntervalMs(500) // Check live offset more frequently
            .setTargetLiveOffsetIncrementOnRebufferMs(100) // Small adjustment after re-buffering
            .setMaxLiveOffsetErrorMsForUnitSpeed(500) // Tolerate up to 500 ms deviation before speeding up/slowing down
            .build()

        player = ExoPlayer.Builder(context)
            .setUsePlatformDiagnostics(false)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory)
                    .setLiveMinOffsetMs(500) // Minimum allowable offset
                    .setLiveMaxOffsetMs(4000) // Maximum allowable offset before forcing a jump
                    .setLiveTargetOffsetMs(1000) // Allow playback as close as 1 second to the live edge
            )
            .setLivePlaybackSpeedControl(liveSpeedControl)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .build()

        player.setWakeMode(C.WAKE_MODE_NETWORK)
        player.addListener(playerListener)
    }

    private val latencyRunnable = object : Runnable {
        override fun run() {
            logLatency()
            handler.postDelayed(this,10_000)
        }
    }

    private fun logLatency() {
        if (this::player.isInitialized && player.isCurrentMediaItemLive) {
            Log.d("Latency", "Current live latency: ${player.currentLiveOffset} ms")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        player.removeListener(playerListener)
        handler.removeCallbacks(latencyRunnable)
    }
}