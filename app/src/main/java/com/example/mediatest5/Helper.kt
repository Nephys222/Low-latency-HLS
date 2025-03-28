package com.example.mediatest5

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import org.chromium.net.CronetEngine
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object Helper {

    val cronetEngine: CronetEngine = CronetEngine.Builder(MyApp.instance)
        .enableHttp2(true)
        .enableQuic(true)
        .enableBrotli(true)
        .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, 1024L * 1024L)
        .build()

    fun createPlayer(
        context: Context,
        trackSelector: DefaultTrackSelector
    ): ExoPlayer {
        val cronetDataSourceFactory = CronetDataSource.Factory(
            cronetEngine,
            Executors.newCachedThreadPool()
        )

        val dataSourceFactory = DefaultDataSource.Factory(context, cronetDataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        return ExoPlayer.Builder(context)
            .setUsePlatformDiagnostics(false)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory)
                    .setLiveTargetOffsetMs(1000)
            )
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(getLoadControl())
            .setAudioAttributes(audioAttributes, true)
            .build()
    }

    private fun getLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBackBuffer(120_000, true)
            .setBufferDurationsMs(
                30_000,
                30_000,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()
    }


    fun DefaultTrackSelector.updateParameters(
        actions: DefaultTrackSelector.Parameters.Builder.() -> Unit
    ) = apply {
        val params = buildUponParameters().apply(actions)
        setParameters(params)
    }
}