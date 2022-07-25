package com.infinite.mediacodecdemo.camera;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.infinite.mediacodecdemo.activity.EncodeActivity;

import java.lang.ref.WeakReference;

public class CameraHandler extends Handler {
    private static final String TAG="CameraHandler";
    public static final int MSG_SET_SURFACE_TEXTURE = 0;

    // Weak reference to the Activity; only access this from the UI thread.
    private WeakReference<EncodeActivity> mWeakActivity;

    public CameraHandler(EncodeActivity activity) {
        mWeakActivity = new WeakReference<EncodeActivity>(activity);
    }

    /**
     * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
     * attempts to access a stale Activity through a handler are caught.
     */
    public void invalidateHandler() {
        mWeakActivity.clear();
    }

    @Override  // runs on UI thread
    public void handleMessage(Message inputMessage) {
        int what = inputMessage.what;
        Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);

        EncodeActivity activity = mWeakActivity.get();
        if (activity == null) {
            Log.w(TAG, "CameraHandler.handleMessage: activity is null");
            return;
        }

        switch (what) {
            case MSG_SET_SURFACE_TEXTURE:
                activity.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                break;
            default:
                throw new RuntimeException("unknown msg " + what);
        }
    }
}
