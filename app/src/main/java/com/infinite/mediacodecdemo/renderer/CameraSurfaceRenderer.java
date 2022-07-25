package com.infinite.mediacodecdemo.renderer;

import android.opengl.GLSurfaceView;
import android.util.Log;

import com.infinite.mediacodecdemo.camera.CameraHandler;
import com.infinite.mediacodecdemo.encoder.TextureMovieEncoder;
import com.infinite.mediacodecdemo.gles.FullFrameRect;
import com.infinite.mediacodecdemo.util.Constants;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraSurfaceRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraSurfaceRenderer"; //MainActivity.TAG;
    private static final boolean VERBOSE = true;

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;

    private CameraHandler mCameraHandler;
    private TextureMovieEncoder mVideoEncoder;
    private File mOutputFile;
    private int mTextureId;
    private int mFrameCount;
    private boolean mRecordingEnabled;
    private int mRecordingStatus;
    // width/height of the incoming camera preview frames
    private boolean mIncomingSizeUpdated;
    private int mIncomingWidth;
    private int mIncomingHeight;

    private int mCurrentFilter;
    private int mNewFilter;

    private FullFrameRect mFullScreen;

    /**
     * Constructs CameraSurfaceRenderer.
     * <p>
     *
     * @param cameraHandler Handler for communicating with UI thread
     * @param movieEncoder  video encoder object
     * @param outputFile    output file for encoded video; forwarded to movieEncoder
     */
    public CameraSurfaceRenderer(CameraHandler cameraHandler, TextureMovieEncoder movieEncoder, File outputFile) {
        Log.d(TAG, "CameraSurfaceRenderer");
        mCameraHandler = cameraHandler;
        mVideoEncoder = movieEncoder;
        mOutputFile = outputFile;

        mTextureId = -1;
        mRecordingStatus=-1;
        mRecordingEnabled=false;
        mFrameCount = -1;

        mIncomingSizeUpdated=false;
        mIncomingWidth=mIncomingHeight=-1;

        // We could preserve the old filter mode, but currently not bothering.
        mCurrentFilter=-1;
        mNewFilter= Constants.FILTER_NONE;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(GL10 gl) {

    }

    public void setCameraPreviewSize(int width, int height) {

    }

    public void notifyPausing() {

    }

    public void changeRecordingState(boolean isRecording) {

    }

    public void changeFilterMode(int filter) {

    }
}
