package com.exoplayer.dfp.example.exoimaplayer;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.Locale;

import static com.google.android.exoplayer2.ExoPlayer.STATE_BUFFERING;
import static com.google.android.exoplayer2.ExoPlayer.STATE_ENDED;
import static com.google.android.exoplayer2.ExoPlayer.STATE_IDLE;
import static com.google.android.exoplayer2.ExoPlayer.STATE_READY;


class VideoController implements AdEvent.AdEventListener, AdErrorEvent.AdErrorListener, ExoPlayer.EventListener {

    //region Instance Variables
    private static final String IMA_LOG_TAG = "Video/Google IMA";
    private static final String EXO_EVENT_LOG_TAG = "Video/ExoEventListener";

    private static final int CAM_PLAY_INTERVAL = BuildConfig.DEBUG ? 5 : 30;

    private final Activity currentActivity;

    // Exoplayer
    private final SimpleExoPlayer videoPlayer;
    private final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
    private final TextView countdownView;
    private final SimpleExoPlayerView videoPlayerView;
    // The container for the ad's UI. Should also contain a SimpleExoPlayerView
    private final ViewGroup adUiContainer;
    private MediaSource mediaSource;
    // remember if we should automatically resume (i.e. for lifecycle event changes)
    private boolean shouldResumePlayback = false;
    private int countdownTime = CAM_PLAY_INTERVAL;
    private long previousCountdownTime = countdownTime;
    // The AdsLoader instance exposes the requestAds method.
    // If it exists we show ads. Set it to null for Ad Free mode.
    private AdsLoader adsLoader;
    // used to update the countdown timer
    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };
    // AdsManager exposes methods to control ad playback and listen to ad events.
    private AdsManager adsManager;
    //endregion

    // region Construction & Deconstruction
    VideoController(Activity activity, boolean addFree) {
        this.videoPlayer = ExoPlayerFactory.newSimpleInstance(activity, getABRTrackSelector(), new DefaultLoadControl());
        this.videoPlayer.addListener(this);

        View videoLayout = View.inflate(activity, R.layout.view_video_player, null);
        this.adUiContainer = (ViewGroup) videoLayout.findViewById(R.id.videoPlayerWithAdPlayback);
        this.videoPlayerView = (SimpleExoPlayerView) videoLayout.findViewById(R.id.simpleExoPlayerView);

        this.videoPlayerView.setPlayer(videoPlayer);
        this.countdownView = (TextView) adUiContainer.findViewById(R.id.videoCountdownView);
        this.countdownView.setVisibility(View.GONE);
        this.currentActivity = activity;

        if (!addFree) {
            configurePrerollAds();
        }
    }

    /**
     * Used to select the appropriate bit rate depending on network conditions. (i.e. for HLS)
     */
    private TrackSelector getABRTrackSelector() {
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);

        return new DefaultTrackSelector(videoTrackSelectionFactory);
    }

    /**
     * Clean up your mess!
     */
    void destroy() {
        // just in case....
        destroyAdComponents();
        videoPlayerView.removeCallbacks(updateProgressAction);
        // We're required to call release on the player.
        videoPlayer.release();
    }
    // endregion

    // region Business Logic

    /**
     * This method presumes an HLS stream.
     * However it should be modifiable to handle different Media Sources
     *
     * @param uriString a string representation of a URI
     */
    void prepareVideoAtUri(String uriString, ViewGroup container) {
        container.addView(adUiContainer);

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(currentActivity, bandwidthMeter,
                new DefaultHttpDataSourceFactory(Util.getUserAgent(currentActivity, currentActivity.getString(R.string.app_name)), bandwidthMeter));

        Uri m3u8 = Uri.parse(uriString);

        mediaSource = new HlsMediaSource(m3u8, dataSourceFactory, new Handler(), null);

        // prepareVideoAtUri() may be called due to network issues in an effort to reboot the stream.
        if (shouldShowAds() && (countdownTime <= 0 || countdownTime >= CAM_PLAY_INTERVAL)) {
            // If we're outside the play interval bounds, then we know we it's time to play an ad.
            requestAds(currentActivity.getString(R.string.ad_tag_url));

        } else {
            // Otherwise just go ahead and play.
            play();
        }
    }

    private void pause() {
        videoPlayer.setPlayWhenReady(false);
        videoPlayer.stop();
    }

    private void play() {
        if (videoPlayer.getPlaybackState() == STATE_IDLE) {
            videoPlayer.prepare(mediaSource);
        }

        videoPlayerView.hideController();
        videoPlayer.setPlayWhenReady(true);

    }

    void pauseVideoIfNecessary() {
        shouldResumePlayback = videoPlayer.getPlayWhenReady();
        if (shouldResumePlayback) {
            pause();
        }
    }

    void resumeVideoIfNecessary() {
        if (shouldResumePlayback) {
            play();
            shouldResumePlayback = false;
        }
    }

    private boolean shouldShowAds() {
        return (adsLoader != null);
    }

    /**
     * Utility method for updateProgress()
     *
     * @param timeMs player's current time in milliseconds
     * @return a seconds representation
     */
    private long timeInSeconds(long timeMs) {
        if (timeMs == C.TIME_UNSET) {
            timeMs = 0;
        }
        long totalSeconds = (timeMs + 500) / 1000;
        return totalSeconds % 60;
    }

    /**
     * This is modeled after ExoPlayer's PlaybackControlView.java's logic to update the progress bar.
     * https://github.com/google/ExoPlayer/blob/release-v2/library/src/main/java/com/google/android/exoplayer2/ui/PlaybackControlView.java
     */
    private void updateProgress() {
        if (videoPlayer == null || videoPlayer.getPlaybackState() != STATE_READY) {
            return;
        }

        long position = Math.abs(videoPlayer.getCurrentPosition());
        videoPlayerView.removeCallbacks(updateProgressAction);

        countdownView.setText(String.format(Locale.ENGLISH, "%ds", countdownTime));

        // Schedule an update if necessary.
        int playbackState = videoPlayer.getPlaybackState();
        if (playbackState != STATE_IDLE && playbackState != STATE_ENDED) {
            if (countdownTime <= 0 && shouldShowAds()) {
                // reset the countdown time & fetch a new ad
                countdownTime = CAM_PLAY_INTERVAL;
                previousCountdownTime = CAM_PLAY_INTERVAL;
                requestAds(currentActivity.getString(R.string.ad_tag_url));
                return;
            }

            long positivePosition = timeInSeconds(position);

            if (previousCountdownTime != positivePosition) {
                previousCountdownTime = positivePosition;
                countdownTime--;
            }

            videoPlayerView.postDelayed(updateProgressAction, 1000);
        }
    }
    // endregion

    //region ExoPlayer.EventListener Methods

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        Log.d(EXO_EVENT_LOG_TAG, "onTimelineChanged");
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        Log.d(EXO_EVENT_LOG_TAG, "onTracksChanged");
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Log.d(EXO_EVENT_LOG_TAG, "onLoadingChanged");
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(EXO_EVENT_LOG_TAG, "onPlayerStateChanged");
        if (adsLoader != null) {
            // pause the countdown timer
            videoPlayerView.removeCallbacks(updateProgressAction);
        }
        switch (playbackState) {
            /**
             * The player is able to immediately play from the current position. The player will be playing if
             * {@link #getPlayWhenReady()} returns true, and paused otherwise.
             */
            case STATE_READY:
                if (playWhenReady && adsLoader != null) {
                    // if the player is ready and mWithAds is true start the updateProgressAction
                    // this should be limited to one location.
                    videoPlayerView.post(updateProgressAction);
                }
                break;
            /**
             * The player does not have a source to play, so it is neither buffering nor ready to play.
             */
            case STATE_IDLE:
                break;
            /**
             * The player not able to immediately play from the current position. The cause is
             * {@link Renderer} specific, but this state typically occurs when more data needs to be
             * loaded to be ready to play, or more data needs to be buffered for playback to resume.
             */
            case STATE_BUFFERING:
                break;
            /**
             * The player has finished playing the media.
             */
            case STATE_ENDED:
                break;
        }

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.d(EXO_EVENT_LOG_TAG, "onPlayerError");
        pause();

        if (error.getCause() instanceof BehindLiveWindowException) {
            // http://stackoverflow.com/a/39851408/558776
            // temporary workaround for Google HLS bug in current ExoPlayer.
            // We'd like to use the newer version of the player as it includes many other
            // features prepackaged that we'd have to implement ourselves otherwise.
            play();
        }
    }

    @Override
    public void onPositionDiscontinuity() {
        Log.d(EXO_EVENT_LOG_TAG, "onPositionDiscontinuity");
    }
    //endregion

    //region Google IMA Events
    private void configurePrerollAds() {
        countdownView.setVisibility(View.VISIBLE);
        if (adsLoader == null) {
            initializeAdsLoader();
        }
    }

    private void configureAdFree() {
        destroyAdComponents();
        videoPlayerView.removeCallbacks(updateProgressAction);
        countdownView.setVisibility(View.GONE);
    }

    private void initializeAdsLoader() {
        // Create an AdsLoader.
        adsLoader = ImaSdkFactory.getInstance().createAdsLoader(currentActivity);
        // Add listeners for when ads are loaded and for errors.
        adsLoader.addAdErrorListener(this);
        adsLoader.addAdsLoadedListener(new AdsLoader.AdsLoadedListener() {
            @Override
            public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
                // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
                // events for ad playback and errors.
                adsManager = adsManagerLoadedEvent.getAdsManager();

                // Attach event and error event listeners.
                adsManager.addAdErrorListener(VideoController.this);
                adsManager.addAdEventListener(VideoController.this);
                adsManager.init();
            }
        });
    }

    private void destroyAdComponents() {
        adsLoader = null;
        destroyAdsManager();
    }

    private void destroyAdsManager() {
        if (adsManager != null) {
            adsManager.destroy();
            adsManager = null;
        }
    }

    /**
     * We may need to toggle the Preroll Ad State of a live Video Player
     * For example, if a user's entitlements are updated while the player is in memory?
     * If this assumption is false then we should remove this logic.
     *
     * @param adFree whether or not ads should be played
     */
    void setPrerollAdState(boolean adFree) {
        if (adFree) {
            configureAdFree();
        } else {
            configurePrerollAds();
        }
    }

    /**
     * Request video ads from the given VAST ad tag.
     *
     * @param adTagUrl URL of the ad's VAST XML
     */
    private void requestAds(String adTagUrl) {
        if (adsLoader == null) {
            return;
        }

        ImaSdkFactory factory = ImaSdkFactory.getInstance();
        AdDisplayContainer adDisplayContainer = factory.createAdDisplayContainer();
        adDisplayContainer.setAdContainer(adUiContainer);

        // Create the ads request.
        AdsRequest request = factory.createAdsRequest();
        request.setAdTagUrl(adTagUrl);
        request.setAdDisplayContainer(adDisplayContainer);

        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        adsLoader.requestAds(request);
    }

    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {
        //noinspection ThrowableResultOfMethodCallIgnored
        Log.d(IMA_LOG_TAG, "Error: " + adErrorEvent.getError().getMessage());

        //TODO: retry a few times with a fallback position (see iOS)
//        requestAds(currentActivity.getString(R.string.ad_tag_fallback_url));

        // For whatever reason the AdsLoader craps out and starts returning: "The VAST response document is empty."
        // Maybe it's tired? Just create a fresh one and see if that helps.
        initializeAdsLoader();

        if (videoPlayer.getPlaybackState() != STATE_READY) {
            // if we're not in a play state yet then start the player
            play();
        } else {
            // otherwise, just restart the countdown timer
            videoPlayerView.post(updateProgressAction);
        }
    }

    @Override
    public void onAdEvent(AdEvent adEvent) {
//        Log.d(IMA_LOG_TAG, "Event: " + adEvent.getType());

        // These are the suggested event types to handle. For full list of all ad event
        // types, see the documentation for AdEvent.AdEventType.
        switch (adEvent.getType()) {
            case LOADED:
                // AdEventType.LOADED will be fired when ads are ready to be played.
                // AdsManager.start() begins ad playback. This method is ignored for VMAP or
                // ad rules play lists, as the SDK will automatically start executing the
                // playlist.
                adsManager.start();
                break;
            case CONTENT_PAUSE_REQUESTED:
                // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
                // ad is played.
                pause();
                break;
            case CONTENT_RESUME_REQUESTED:
                // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
                // and you should start playing your content.
                play();
                break;
            case ALL_ADS_COMPLETED:
                destroyAdsManager();
                break;
            default:
                break;
        }
    }
    //endregion

}
