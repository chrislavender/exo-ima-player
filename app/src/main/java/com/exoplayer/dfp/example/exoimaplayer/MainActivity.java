package com.exoplayer.dfp.example.exoimaplayer;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Switch;

import com.exoplayer.dfp.example.exoimaplayer.rotation.FullscreenRotationController;
import com.exoplayer.dfp.example.exoimaplayer.rotation.FullscreenToggledListener;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;

public class MainActivity extends AppCompatActivity implements
        FullscreenRotationController.FullscreenToggleCallback, FullscreenToggledListener {

    static final int videoHeight = 224;

    VideoController videoController;
    FullscreenRotationController rotationController;
    Switch switchUI;
    boolean adFree = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setShowHideAnimationEnabled(true);
        }

        setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        rotationController = new FullscreenRotationController(getApplicationContext());
        rotationController.setFullscreenToggleCallback(this);
        rotationController.enableAutoRotation();

        setContentView(R.layout.activity_main);
        switchUI = (Switch) findViewById(R.id.premiumSwitch);

        if (savedInstanceState != null) {
            adFree = savedInstanceState.getBoolean("switch");
        }

        if (switchUI != null) {
            switchUI.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.d("TEST", "Switch: " + isChecked);
                    adFree = isChecked;
                    videoController.setPrerollAdState(adFree);
                }
            });
        }

        videoController = new VideoController(MainActivity.this, adFree);

        final ImageButton initialPlay = (ImageButton) findViewById(R.id.fake_play_button);
        if (initialPlay != null) {
            initialPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(videoHeight));
                    ViewGroup vg = (ViewGroup) findViewById(R.id.videoContainer);
                    vg.setLayoutParams(lps);

                    videoController.prepareVideoAtUri(getString(R.string.content_url), vg);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        videoController.setPrerollAdState(adFree);
        videoController.resumeVideoIfNecessary();
        super.onResume();
    }

    @Override
    protected void onPause() {
        videoController.pauseVideoIfNecessary();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        videoController.destroy();
        super.onDestroy();
    }

    @Override
    public void onFullscreenToggled(final boolean shouldFullscreen) {
        Log.d("ROTATION_TEST", "Fullscreen");

        if (shouldFullscreen) {
            setViewStateForFullscreenVideo();
        } else {
            setViewStateForDefaultViewing();
        }

    }

    private void setViewStateForDefaultViewing() {
        Log.d("ROTATION_TEST", "Default");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.show();
        }

        switchUI.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(videoHeight));
        ViewGroup vg = (ViewGroup) findViewById(R.id.videoContainer);
        vg.setLayoutParams(lps);

    }

    private void setViewStateForFullscreenVideo() {
        Log.d("ROTATION_TEST", "Fullscreen");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setShowHideAnimationEnabled(true);
            bar.hide();
        }

        switchUI.setVisibility(View.GONE);

        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        ViewGroup vg = (ViewGroup) findViewById(R.id.videoContainer);
        vg.setLayoutParams(lps);

    }

    @Override
    public void onToggleFullscreenRequested(int whichOrientation) {
        Log.d("ROTATION_TEST", "whichOrientation:" + whichOrientation);
        setRequestedOrientation(whichOrientation);

        switch (whichOrientation) {
            case SCREEN_ORIENTATION_LANDSCAPE:
            case SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                setViewStateForFullscreenVideo();
                break;
            case SCREEN_ORIENTATION_PORTRAIT:
                setViewStateForDefaultViewing();
                break;

        }
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
