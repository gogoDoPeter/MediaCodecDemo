package com.infinite.mediacodecdemo.activity;

import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.infinite.mediacodecdemo.R;
import com.infinite.mediacodecdemo.camera.CameraHandler;
import com.infinite.mediacodecdemo.encoder.TextureMovieEncoder;
import com.infinite.mediacodecdemo.renderer.CameraSurfaceRenderer;
import com.infinite.mediacodecdemo.ui.AspectFrameLayout;
import com.infinite.mediacodecdemo.util.CameraUtils;
import com.infinite.mediacodecdemo.util.Constants;
import com.infinite.mediacodecdemo.util.PermissionHelper;

import java.io.File;
import java.io.IOException;

public class EncodeActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "EncodeActivity";
    private static final boolean VERBOSE = true;
    private TextView fileText;
    private Spinner spinner;
    private CameraHandler mCameraHandler;
    // this is static so it survives activity restarts
    private static TextureMovieEncoder sVideoEncoder = new TextureMovieEncoder();
    private boolean mRecordingEnabled; // controls button state
    private GLSurfaceView mGLView;
    private CameraSurfaceRenderer mRenderer;
    private int mDesiredWidth;
    private int mDesiredHeight;

    private Camera mCamera;
    private int mPreviewWidth, mPreviewHeight;
    private Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate +");
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera_encode);

//        File outputFile = new File(getFilesDir(), "camera-test.mp4");
        File outputFile = new File("/sdcard/DCIM/cam-test.mp4");
        fileText = ((TextView) findViewById(R.id.cameraOutputFile_text));
        fileText.setText(outputFile.toString());

        spinner = ((Spinner) findViewById(R.id.cameraFilter_spinner));
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.cameraFilterNames, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        spinner.setAdapter(adapter);
        spinner.setOnItemClickListener((AdapterView.OnItemClickListener) this);

        // Define a handler that receives camera-control messages from other threads.  All calls
        // to Camera must be made on the same thread.  Note we create this before the renderer
        // thread, so we know the fully-constructed object will be visible.
        mCameraHandler = new CameraHandler(this);
        mRecordingEnabled = sVideoEncoder.isRecording();
        Log.d(TAG, "onCreate mRecordingEnabled " + mRecordingEnabled);

        // Configure the GLSurfaceView.  This will start the Renderer thread, with an
        // appropriate EGL context.
        mGLView = ((GLSurfaceView) findViewById(R.id.cameraPreview_surfaceView));
        // select GLES 2.0
        mGLView.setEGLContextClientVersion(2);

        mRenderer = new CameraSurfaceRenderer(mCameraHandler, sVideoEncoder, outputFile);
        mGLView.setRenderer(mRenderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mDesiredWidth = Constants.PREVIEW_WIDTH_720P;
        mDesiredHeight = Constants.PREVIEW_HEIGHT_720P;
        Log.d(TAG, "onCreate -");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume -- acquiring camera");

        updateControls();
        if (PermissionHelper.hasCameraPermission(this)) {
            if (mCamera == null) {
                openCamera(mDesiredWidth, mDesiredHeight);
            }
        } else {
            PermissionHelper.requestCameraPermission(this, true);
        }

        mGLView.onResume();
        // TODO queueEvent 有什么用？ 使用queueEvent给OpenGL线程分发调用，属于线程间通信方式
        //  Android的UI运行在主线程，而OpenGL的GLSurfaceView运行在一个单独的线程中，因此需要使用线程安全的技术在两个线程间通信。
        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mPreviewWidth, mPreviewHeight);
            }
        });

        Log.d(TAG, "onResume complete: " + this);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause -- releasing camera");
        super.onPause();
        releaseCamera();
        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                // Tell the renderer that it's about to be paused so it can clean up.
                mRenderer.notifyPausing();
            }
        });
        mGLView.onPause();
        Log.d(TAG, "onPause complete");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        // paranoia
        mCameraHandler.invalidateHandler();
    }

    private void openCamera(int width, int height) {
        Log.d(TAG, "openCamera +");
        if (mCamera != null) {
            throw new RuntimeException("Camera already initialized");
        }
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                Log.d(TAG, "Open camera " + i);
                mCamera = Camera.open(i);
                Camera.getCameraInfo(i, mCameraInfo);
                break;
            }
        }
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parameters = mCamera.getParameters();
        CameraUtils.choosePreviewSize(parameters, width, height);

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parameters.setRecordingHint(true);

        // leave the frame rate set to default
        mCamera.setParameters(parameters);

        int[] fpsRange = new int[2];
        Camera.Size mPreviewSize = parameters.getPreviewSize();
        parameters.getPreviewFpsRange(fpsRange);
        String previewFacts = mPreviewSize.width + "x" + mPreviewSize.height;
        if (fpsRange[0] == fpsRange[1]) {
            previewFacts += "@" + (fpsRange[0] / 1000.0) + "fps";
        } else {
            previewFacts += "@[" + (fpsRange[0] / 1000.0) + "~" + (fpsRange[1] / 1000.0) + "] fps";
        }
        TextView text = ((TextView) findViewById(R.id.cameraParams_text));
        text.setText(previewFacts);

        mPreviewWidth = mPreviewSize.width;
        mPreviewHeight = mPreviewSize.height;

        AspectFrameLayout layout = ((AspectFrameLayout) findViewById(R.id.cameraPreview_afl));

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Log.d(TAG, "rotation " + display.getRotation() + " W " + mPreviewWidth + " H " + mPreviewHeight);
        if (display.getRotation() == Surface.ROTATION_0) {
            mCamera.setDisplayOrientation(90);
            layout.setAspectRatio((double) mPreviewHeight / mPreviewWidth);
        } else {
            layout.setAspectRatio((double) mPreviewWidth / mPreviewHeight);
        }
    }

    /**
     * Updates the on-screen controls to reflect the current state of the app.
     */
    private void updateControls() {
        Button toggleRelease = ((Button) findViewById(R.id.toggleRecording_button));
        int id = mRecordingEnabled ? R.string.toggleRecordingOff : R.string.toggleRecordingOn;
        toggleRelease.setText(id);
        if (mRecordingEnabled) {
            toggleRelease.setBackgroundColor(Color.RED);
        } else {
            toggleRelease.setBackgroundColor(Color.WHITE);
        }
    }

    //Stops camera preview, and releases the camera to the system.
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera");
        }
    }

    //onClick handler for "record" button.
    public void clickToggleRecording(View view) {
        Log.d(TAG, "clickToggleRecording");
        mRecordingEnabled = !mRecordingEnabled;
        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                // notify the renderer that we want to change the encoder's state
                mRenderer.changeRecordingState(mRecordingEnabled);
            }
        });
        updateControls();//update record Button UI
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner spinner = (Spinner) parent;
        final int filterNum = spinner.getSelectedItemPosition();
        Log.d(TAG, "onItemSelected pos " + position + " id " + id + " filterNum " + filterNum);
        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                // notify the renderer that we want to change the encoder's state
                mRenderer.changeFilterMode(filterNum);
            }
        });
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.d(TAG, "onNothingSelected");
    }

    /**
     * Connects the SurfaceTexture to the Camera preview output, and starts the preview.
     *
     * @param surfaceTexture
     */
    public void handleSetSurfaceTexture(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "handleSetSurfaceTexture");
        surfaceTexture.setOnFrameAvailableListener(this);// 设置对frame可用的监听
        try {
            // 将st设置给camera关联后，camera返回的数据给到这里，而这个surfaceTexture是和纹理id绑定过的
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException ioe) {
//            e.printStackTrace();
            throw new RuntimeException(ioe);
        }
        Log.d(TAG, "handleSetSurfaceTexture startPreview");
        mCamera.startPreview();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // The SurfaceTexture uses this to signal the availability of a new frame.  The
        // thread that "owns" the external texture associated with the SurfaceTexture (which,
        // by virtue of the context being shared, *should* be either one) needs to call
        // updateTexImage() to latch the buffer.
        //
        // Once the buffer is latched, the GLSurfaceView thread can signal the encoder thread.
        // This feels backward -- we want recording to be prioritized over rendering -- but
        // since recording is only enabled some of the time it's easier to do it this way.
        //
        // Since GLSurfaceView doesn't establish a Looper, this will *probably* execute on
        // the main UI thread.  Fortunately, requestRender() can be called from any thread,
        // so it doesn't really matter.

//        if (VERBOSE) Log.d(TAG, "ST onFrameAvailable surfaceTexture:"+surfaceTexture);
        //do onDraw 通知GLSurfaceView draw来更新预览

        /*Request that the renderer render a frame. This method is typically used when the render mode
         has been set to RENDERMODE_WHEN_DIRTY, so that frames are only rendered on demand.
        May be called from any thread. Must not be called before a renderer has been set.*/
        mGLView.requestRender();
    }
}
