package com.exoplayer.dfp.example.exoimaplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

public class MainActivity extends AppCompatActivity {

    VideoController videoController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Switch switchUI = (Switch) findViewById(R.id.premiumSwitch);
        switchUI.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("TEST", "Switch: " + isChecked);
                videoController.setPrerollAdState(isChecked);
            }
        });

        ExoImaPlayerApplication app = (ExoImaPlayerApplication)getApplication();
        videoController = app.getVideoController();
        videoController.configureVideoPlayer((ViewGroup) findViewById(R.id.videoContainer), switchUI.isChecked());

        if (savedInstanceState == null) {
            videoController.prepareVideoAtUri(getString(R.string.content_url));
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
