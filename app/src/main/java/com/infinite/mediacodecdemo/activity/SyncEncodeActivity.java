package com.infinite.mediacodecdemo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.infinite.mediacodecdemo.R;
import com.infinite.mediacodecdemo.camera.CameraProxy;
import com.infinite.mediacodecdemo.ui.CameraSurfaceView;

public class SyncEncodeActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG="SyncEncodeActivity";
    private CameraSurfaceView mCameraView;
    private CameraProxy mCameraProxy;
    private Button mSwitchCamera;
    private Button mStartRecord;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate +");
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_sync_encode);
        
        initView();
    }

    private void initView() {
        Log.d(TAG, "initView +");
        mSwitchCamera = findViewById(R.id.bt_switch_camera);
        mStartRecord = (Button) findViewById(R.id.bt_start_record);
        mCameraView = findViewById(R.id.camera_view);
        mCameraProxy = mCameraView.getCameraProxy();
        Log.d(TAG, "initView -");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_start_record:
                break;
            case R.id.bt_switch_camera:
                break;
        }
    }
}
