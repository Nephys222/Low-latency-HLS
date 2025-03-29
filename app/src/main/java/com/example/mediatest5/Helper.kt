package com.example.mediatest5

import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import org.chromium.net.CronetEngine

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object Helper {

    val cronetEngine: CronetEngine = CronetEngine.Builder(MyApp.instance)
        .enableHttp2(true)
        .enableQuic(true)
        .enableBrotli(true)
        .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, 1024L * 1024L)
        .build()


    fun DefaultTrackSelector.updateParameters(
        actions: DefaultTrackSelector.Parameters.Builder.() -> Unit
    ) = apply {
        val params = buildUponParameters().apply(actions)
        setParameters(params)
    }
}