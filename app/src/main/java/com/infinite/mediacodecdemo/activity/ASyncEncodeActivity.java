package com.infinite.mediacodecdemo.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.infinite.mediacodecdemo.R;
import com.infinite.mediacodecdemo.camera.CameraProxy;
import com.infinite.mediacodecdemo.encoder.AsyncEncoderCore;
import com.infinite.mediacodecdemo.ui.CameraSurfaceView;

public class ASyncEncodeActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ASyncEncodeActivity";
    private CameraSurfaceView mCameraView;
    private CameraProxy mCameraProxy;
    private Button mBtSwitchCamera;
    private Button mBtStartRecord;
    private AsyncEncoderCore mEncoderCore;
    private boolean mIsRecording;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_sync_encode);

        initView();
        initData();
    }

    private void initView() {
        Log.d(TAG, "initView +");
        mBtSwitchCamera = (Button) findViewById(R.id.bt_switch_camera);
        mBtStartRecord = (Button) findViewById(R.id.bt_start_record);
        mCameraView = findViewById(R.id.camera_view);

        //cameraProxy was created in CameraSurfaceView
        mCameraProxy = mCameraView.getCameraProxy();

        Log.d(TAG, "initView -");
    }

    private void initData() {
        Log.d(TAG, "initData +");
        mIsRecording = false;
        mEncoderCore = new AsyncEncoderCore(this);
        mEncoderCore.setCameraProxy(mCameraProxy);
        mEncoderCore.setPreviewDisplay(mCameraView.getHolder());
        Log.d(TAG, "initData -");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_start_record:
                Log.d(TAG,"start record async");
                break;
            case R.id.bt_switch_camera:
                break;
        }
    }

    private void startRecording() {
        mIsRecording = !mIsRecording;

        if (mIsRecording) {
            mEncoderCore.startRecord();
            mBtStartRecord.setText("MediaCodec停止");
        } else {
            mEncoderCore.stopRecord();
            mBtStartRecord.setText("MediaCodec开始");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }
}
