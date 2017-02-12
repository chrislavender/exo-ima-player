package com.exoplayer.dfp.example.exoimaplayer.rotation;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import java.util.concurrent.locks.ReentrantLock;


class AbsoluteOrientationEventListener extends OrientationEventListener {

    private static final int CONFIGURATION_ORIENTATION_UNDEFINED = Configuration.ORIENTATION_UNDEFINED;
    private Context applicationContext;
    private volatile int defaultScreenOrientation = CONFIGURATION_ORIENTATION_UNDEFINED;
    private int prevOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private ReentrantLock lock = new ReentrantLock(true);
    private boolean enabled = false;
    private AbsoluteOrientationChangedCallback callback;

    AbsoluteOrientationEventListener(Context applicationContext) {
        super(applicationContext, SensorManager.SENSOR_DELAY_NORMAL);
        this.applicationContext = applicationContext;
    }

    void setAbsoluteOrientationChangedCallback(AbsoluteOrientationChangedCallback c) {
        callback = c;
    }

    @Override
    public void enable() {
        if (!enabled) {
            super.enable();
            enabled = true;
        }
    }

    @Override
    public void disable() {
        if (enabled) {
            super.disable();
            enabled = false;
        }
    }

    @Override
    public void onOrientationChanged(final int orientation) {
        int currentOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
        if (orientation >= 330 || orientation < 30) {
            currentOrientation = Surface.ROTATION_0;
        } else if (orientation >= 60 && orientation < 120) {
            currentOrientation = Surface.ROTATION_90;
        } else if (orientation >= 150 && orientation < 210) {
            currentOrientation = Surface.ROTATION_180;
        } else if (orientation >= 240 && orientation < 300) {
            currentOrientation = Surface.ROTATION_270;
        }

        if (prevOrientation != currentOrientation && orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            prevOrientation = currentOrientation;
            if (currentOrientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
                reportOrientationChanged(currentOrientation);
            }
        }
    }

    private void reportOrientationChanged(final int currentOrientation) {
        int defaultOrientation = getDeviceDefaultOrientation();

        int toReportOrientation;

        if (currentOrientation == Surface.ROTATION_0 || currentOrientation == Surface.ROTATION_180) {
            if (defaultOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (currentOrientation == Surface.ROTATION_0) {
                    toReportOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    toReportOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                if (currentOrientation == Surface.ROTATION_0) {
                    toReportOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    toReportOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }
            }
        } else {
            if (defaultOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (currentOrientation == Surface.ROTATION_90) {
                    toReportOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                } else {
                    toReportOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                }
            } else {
                if (currentOrientation == Surface.ROTATION_90) {
                    toReportOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                } else {
                    toReportOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                }

            }
        }

        if (callback != null) {
            callback.onAbsoluteOrientationChanged(toReportOrientation);
        }
    }

    private int getDeviceDefaultOrientation() {
        if (defaultScreenOrientation == CONFIGURATION_ORIENTATION_UNDEFINED) {
            lock.lock();
            defaultScreenOrientation = initDeviceDefaultOrientation(applicationContext);
            lock.unlock();
        }
        return defaultScreenOrientation;
    }

    private int initDeviceDefaultOrientation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Configuration config = context.getResources().getConfiguration();
        int rotation = windowManager.getDefaultDisplay().getRotation();

        boolean isLand = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean isDefaultAxis = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180;

        int result;
        if ((isDefaultAxis && isLand) || (!isDefaultAxis && !isLand)) {
            result = Configuration.ORIENTATION_LANDSCAPE;
        } else {
            result = Configuration.ORIENTATION_PORTRAIT;
        }
        return result;
    }

    interface AbsoluteOrientationChangedCallback {
        void onAbsoluteOrientationChanged(int which);
    }
}