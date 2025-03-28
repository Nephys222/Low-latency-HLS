package com.example.mediatest5;

import android.content.Context;
import android.os.Bundle;
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
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
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

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Player.Listener.super.onPlayerError(error);
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
        playerView.setControllerShowTimeoutMs(4000);

        player.setMediaItem(
                new MediaItem.Builder()
                        .setUri(hlsUrl)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                        .build()
        );

        player.setPlaybackParameters(new PlaybackParameters(1.0f, 1.0f));

        player.prepare();
        player.play();
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
                .setBackBuffer(20_000, true)
                .setBufferDurationsMs(
                        30_000,
                        30_000,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                .build();

        trackSelector = new DefaultTrackSelector(this);

        player = new ExoPlayer.Builder(context)
                .setUsePlatformDiagnostics(false)
                .setMediaSourceFactory(
                        new DefaultMediaSourceFactory(dataSourceFactory)
                                .setLiveTargetOffsetMs(1000)
                )
                .setRenderersFactory(renderersFactory)
                .setTrackSelector(trackSelector)
                .setHandleAudioBecomingNoisy(true)
                .setLoadControl(loadControl)
                .setAudioAttributes(audioAttributes, true)
                .build();

        player.setWakeMode(C.WAKE_MODE_NETWORK);
        player.addListener(playerListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player.removeListener(playerListener);
        }
    }
}
