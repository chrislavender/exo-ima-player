package com.exoplayer.dfp.example.exoimaplayer;

import android.app.Activity;
import android.app.Application;


public class ExoImaPlayerApplication extends Application {

    private VideoController videoController;

    public VideoController getVideoController() {
        if (videoController == null) {
            videoController = VideoController.newVideoControllerInstance(this);
        }
        return videoController;
    }

    public void destroyVideoController() {
        videoController.destroy();
        videoController = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

}
