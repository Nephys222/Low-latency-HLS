package com.example.mediatest5;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.ui.PlayerView;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private EditText streamIdEditText;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.player_view);
        streamIdEditText = findViewById(R.id.stream_id_edit_text);
        submitButton = findViewById(R.id.submit_button);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String streamId = streamIdEditText.getText().toString();
                if (!streamId.isEmpty()) {
                    playStream(streamId);
                } else {
                    // Handle empty stream ID
                }
            }
        });
    }

    @OptIn(markerClass = UnstableApi.class)
    private void playStream(String streamId) {
        String hlsUrl = "https://smloven.b-cdn.net/app/" + streamId + "/llhls.m3u8";

        // Configure LoadControl for low latency
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(3000, 4000, 1000, 2000)
                .setTargetBufferBytes(DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        // Configure DefaultTrackSelector for adaptive streaming
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setMaxVideoBitrate(2000000) // Adjust based on your stream quality
                .setForceHighestSupportedBitrate(true));

        // Create ExoPlayer instance with custom configurations
        player = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .setBandwidthMeter(new DefaultBandwidthMeter.Builder(this).build())
                .build();

        playerView.setPlayer(player);

        // Configure HlsMediaSource for low latency
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(10000)
                .setTransferListener(new DefaultBandwidthMeter.Builder(this).build());

        // Create HlsMediaSource with proper factory
        HlsMediaSource mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(false)
                .createMediaSource(MediaItem.fromUri(hlsUrl));

        player.setMediaSource(mediaSource);
        player.prepare();

        // Enable low latency playback
        player.setPlaybackParameters(new PlaybackParameters(1.0f, 1.0f));

        player.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }
}
