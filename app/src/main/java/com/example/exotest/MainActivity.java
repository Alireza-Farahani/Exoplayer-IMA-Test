package com.example.exotest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class MainActivity extends AppCompatActivity implements
        AdEvent.AdEventListener,
        AdErrorEvent.AdErrorListener {

    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";
    private static final String KEY_AUTO_PLAY = "auto_play";
    private PlayerView playerView;
    private SimpleExoPlayer player;
    private int currentWindow;
    private long playbackPosition;
    private boolean playWhenReady;
    private ImaAdsLoader adsLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.exo_video_view);
        if (adsLoader == null) {
//            adsLoader = new ImaAdsLoader(this, Uri.parse(getString(R.string.ad_tag_url)));
            ImaSdkSettings settings = ImaSdkFactory.getInstance().createImaSdkSettings();
            settings.setLanguage("fa"); // NOTE: get it from `LocalizationUtil` in Toolbox App
            adsLoader = new ImaAdsLoader.Builder(this)
                    .setAdEventListener(this)
                    .setImaSdkSettings(settings)
                    .buildForAdTag(Uri.parse(getString(R.string.ad_deema_url)));
            adsLoader.getAdsLoader().addAdErrorListener(this);
            //adsLoader.getAdsLoader().addAdsLoadedListener(this);
        }

        if (savedInstanceState != null) {
            currentWindow = savedInstanceState.getInt(KEY_WINDOW);
            playbackPosition = savedInstanceState.getLong(KEY_POSITION);
            playWhenReady = savedInstanceState.getBoolean(KEY_AUTO_PLAY, true);
        }
    }

    private MediaSource buildMediaSource(Uri uri, DataSource.Factory dataSourceFactory) {
        return new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
    }

    private MediaSource buildAdMediaSource(MediaSource contentMediaSource, DataSource.Factory dataSourceFactory) {
        // Create the AdsMediaSource using the AdsLoader and the MediaSource.
        return new AdsMediaSource(contentMediaSource, dataSourceFactory, adsLoader, playerView);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    private void initializePlayer() {
        player = ExoPlayerFactory.newSimpleInstance(this);
        playerView.setPlayer(player);

        if (adsLoader != null)
            adsLoader.setPlayer(player);

        Uri contentUri = Uri.parse(getString(R.string.media_url_mp4));
        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(this, "exoplayer-codelab");
        MediaSource contentMediaSource = buildMediaSource(contentUri, dataSourceFactory);
        MediaSource adMediaSource = buildAdMediaSource(contentMediaSource, dataSourceFactory);

        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        player.prepare(adMediaSource, false, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT >= 24) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        hideSystemUi();
        if ((Util.SDK_INT < 24 || player == null)) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    private void hideSystemUi() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseAdsLoader();
    }

    private void releasePlayer() {
        if (player != null) {
            updateStartPosition();
            player.release();
            player = null;
        }
        if (adsLoader != null) {
            adsLoader.setPlayer(null);
        }
    }

    private void releaseAdsLoader() {
        if (adsLoader != null) {
            adsLoader.release();
            adsLoader = null;
            //loadedAdTagUri = null;
            if (playerView.getOverlayFrameLayout() != null)
                playerView.getOverlayFrameLayout().removeAllViews();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        updateStartPosition();
        outState.putBoolean(KEY_AUTO_PLAY, playWhenReady);
        outState.putInt(KEY_WINDOW, currentWindow);
        outState.putLong(KEY_POSITION, playbackPosition);
    }

    private void updateStartPosition() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            currentWindow = player.getCurrentWindowIndex();
            playbackPosition = Math.max(0, player.getContentPosition());
        }
    }

    // AdEvent.AdEventListener
    @Override
    public void onAdEvent(AdEvent adEvent) {
        // TODO
        switch (adEvent.getType()) {

        }
    }

    // AdErrorEvent.AdErrorListener
    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {
        Log.e(this.getClass().getName(), "Error on loading ad", adErrorEvent.getError());
    }
}