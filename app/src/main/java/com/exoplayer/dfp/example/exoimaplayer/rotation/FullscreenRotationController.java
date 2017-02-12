package com.exoplayer.dfp.example.exoimaplayer.rotation;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.View;

public class FullscreenRotationController implements AbsoluteOrientationEventListener.AbsoluteOrientationChangedCallback, FullscreenToggledListener {

    private final Context applicationContext;
    private FullscreenToggleCallback fullscreenToggleCallback;
    private AbsoluteOrientationEventListener rotationEventListener;
    private boolean fullscreenRotationEnabled;
    private boolean isFullscreen;

    public FullscreenRotationController(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public final void onAbsoluteOrientationChanged(int which) {
        onOrientationChanged(which);
    }

    synchronized private void onOrientationChanged(int which) {
        doFullscreenToggleCallback(which, false);
    }

    private void doFullscreenToggleCallback(int orientation, boolean override) {
        if (fullscreenRotationEnabled) {
            switch (orientation) {
                case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                    if (fullscreenToggleCallback != null) {
                        fullscreenToggleCallback.onToggleFullscreenRequested(orientation);
                    }
            }
        } else if (isFullscreen) {
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                if (fullscreenToggleCallback != null) {
                    fullscreenToggleCallback.onToggleFullscreenRequested(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        } else if (override) {
            if (fullscreenToggleCallback != null) {
                fullscreenToggleCallback.onToggleFullscreenRequested(orientation);
            }
        }
    }

    public boolean isFullscreenRotationEnabled() {
        return fullscreenRotationEnabled;
    }

// Following methods called by owner:

    public boolean isFullscreen() {
        return isFullscreen;
    }

    /**
     * Must be called by this instance's owner to set root controller/observer of the fullscreen callback; eg the hosting <code>Activity</code> of the view using this controller instance.
     */
    public final void setFullscreenToggleCallback(FullscreenToggleCallback c) {
        fullscreenToggleCallback = c;
    }

    /**
     * Call to enable device rotation triggering change in fullscreen.
     */
    public final void enableAutoRotation() {
        fullscreenRotationEnabled = true;
        if (rotationEventListener == null) {
            rotationEventListener = new AbsoluteOrientationEventListener(applicationContext);
            rotationEventListener.setAbsoluteOrientationChangedCallback(this);
        }
        rotationEventListener.enable();
    }

    /**
     * Call to enable device rotation triggering change in fullscreen.
     */
    public final void disableAutoRotation() {
        fullscreenRotationEnabled = false;
    }

    /**
     * Triggers the fullscreen callback for default landscape orientation.
     */
    public final void enterFullscreen() {
        doFullscreenToggleCallback(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, true);
    }

    /**
     * Triggers the (non) fullscreen callback for default portrait orientation.
     */
    public final void exitFullscreen() {
        doFullscreenToggleCallback(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, true);
    }

    /**
     * Resets to initial state: {@link #isFullscreen} and {@link #fullscreenRotationEnabled} set to <b>false</b>.
     */
    public final void resetFullscreenFlags() {
        isFullscreen = false;
        fullscreenRotationEnabled = false;
    }

    /**
     * Must be called when, after fullscreen callback is triggered, fullscreen toggled implementation callback chain reaches this instance's owner.
     */
    @Override
    public final void onFullscreenToggled(boolean shouldFullscreen) {
        isFullscreen = shouldFullscreen;
    }

    /**
     * Releases context and disables the rotation event listener; call from eg {@link View#onDetachedFromWindow()}.
     */
    public void release() {
        if (rotationEventListener != null) {
            rotationEventListener.disable();
            rotationEventListener = null;
        }
        fullscreenToggleCallback = null;
    }

    public interface FullscreenToggleCallback {
        void onToggleFullscreenRequested(int whichOrientation);
    }
}
