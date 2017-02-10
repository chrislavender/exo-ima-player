package com.exoplayer.dfp.example.exoimaplayer;

import android.app.Activity;
import android.app.Application;


public class ExoImaPlayerApplication extends Application {

    private VideoController videoController;

    public VideoController getVideoController(Activity activity) {
        if (videoController == null) {
            videoController = VideoController.newVideoControllerInstance(this, activity);
        }
        return videoController;
    }

    ;

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
