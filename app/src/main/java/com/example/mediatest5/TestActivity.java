package com.example.mediatest5;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.SoftwareKeyboardControllerCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.cronet.CronetDataSource;
import androidx.media3.exoplayer.DefaultLivePlaybackSpeedControl;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LivePlaybackSpeedControl;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import java.util.concurrent.Executors;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class TestActivity extends AppCompatActivity {

    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private PlayerView playerView;
    private EditText streamIdEditText;
    private Button submitButton;

    CronetDataSource.Factory cronetDataSourceFactory = new CronetDataSource.Factory(
            Helper.INSTANCE.getCronetEngine(),
            Executors.newCachedThreadPool()
    );

    Handler handler = new Handler(Looper.getMainLooper());

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Player.Listener.super.onPlayerError(error);
            Log.d("Latency", String.format("Error: %s",  error.getLocalizedMessage()));
            try {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition();
                    player.prepare();
                } else {
                    player.prepare();
                    player.play();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.player_view);
        streamIdEditText = findViewById(R.id.stream_id_edit_text);
        submitButton = findViewById(R.id.submit_button);

        createPlayer(this);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String streamId = streamIdEditText.getText().toString();
                if (!streamId.isEmpty()) {
                    playStream(streamId);
                } else {
                    // Handle empty stream ID
                }
                new SoftwareKeyboardControllerCompat(submitButton).hide();
            }
        });
    }


    private void playStream(String streamId) {
        String hlsUrl = "https://smloven.b-cdn.net/app/" + streamId + "/llhls.m3u8";

        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setMaxVideoSize(Integer.MAX_VALUE, Integer.MAX_VALUE)
                .setMinVideoSize(Integer.MIN_VALUE, Integer.MIN_VALUE)
        );

        playerView.setPlayer(player);
        playerView.setControllerShowTimeoutMs(3_000);
        player.setPlaybackParameters(new PlaybackParameters(1.0f, 1.0f));

        /*
         * Test stream "LL Mux" from Theo-player
         * https://stream.mux.com/v69RSHhFelSm4701snP22dYz2jICy4E4FUyk02rW4gxRM.m3u8
         */
        player.setMediaItem(
                new MediaItem.Builder()
                        .setUri(hlsUrl)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                        .build()
        );

        player.prepare();
        player.play();

        handler.postDelayed(latencyRunnable, 5_000);
    }

    private void createPlayer(Context context) {

        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context, cronetDataSourceFactory);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build();

        RenderersFactory renderersFactory = new DefaultRenderersFactory(context)
                .setEnableDecoderFallback(true);

        LoadControl loadControl = new DefaultLoadControl.Builder()
//                .setBackBuffer(20_000, true)
                .setBufferDurationsMs(
                    500, // minBufferMs: Minimum buffer before playback resumes after re-buffering
                    2000, // maxBufferMs: Maximum buffer to avoid loading too far ahead
                    250, // bufferForPlaybackMs: Buffer needed to start playback
                    500 // bufferForPlaybackAfterRebufferMs: Buffer needed after re-buffering
                )
                .setPrioritizeTimeOverSizeThresholds(true) // Prioritize time-based buffering
                .build();

        trackSelector = new DefaultTrackSelector(this);

        LivePlaybackSpeedControl liveSpeedControl = new DefaultLivePlaybackSpeedControl.Builder()
                .setMinUpdateIntervalMs(500) // Check live offset more frequently
                .setTargetLiveOffsetIncrementOnRebufferMs(100) // Small adjustment after re-buffering
                .setMaxLiveOffsetErrorMsForUnitSpeed(500) // Tolerate up to 500 ms deviation before speeding up/slowing down
                .build();

        player = new ExoPlayer.Builder(context)
                .setUsePlatformDiagnostics(false)
                .setMediaSourceFactory(
                        new DefaultMediaSourceFactory(dataSourceFactory)
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
                .build();

        player.setWakeMode(C.WAKE_MODE_NETWORK);
        player.addListener(playerListener);
    }

    Runnable latencyRunnable = new Runnable() {
        @Override
        public void run() {
            logLatency();
            handler.postDelayed(this,10_000);
        }
    };

    private void logLatency() {
        if (player != null && player.isCurrentMediaItemLive()) {
            Log.d("Latency", String.format("Current live latency: %d ms",  player.getCurrentLiveOffset()));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player.removeListener(playerListener);
        }
        handler.removeCallbacks(latencyRunnable);
    }
}