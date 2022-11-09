package com.infinite.mediacodecdemo.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.infinite.mediacodecdemo.R;
import com.infinite.mediacodecdemo.encoder.EncoderCore;

public class SyncEncodeActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "SyncEncodeActivity";
    private EncoderCore mEncoderCore;
    private boolean mIsRecording;
    private SurfaceView mSurfaceView;
    private Button mBtStartRecord;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_sync_encode2);

        initView();
        initData();
    }

    private void initData() {
        mBtStartRecord.setOnClickListener(this);
        mIsRecording = false;
        mEncoderCore = new EncoderCore(this);
        mEncoderCore.setPreviewDisplay(mSurfaceView.getHolder());
    }

    private void initView() {
        mBtStartRecord = (Button) findViewById(R.id.bt_start_record2);
        findViewById(R.id.bt_switch_camera2).setOnClickListener(this);
        mSurfaceView = findViewById(R.id.surface_view2);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_start_record2:
                Log.d(TAG,"start record");
                startRecording();
                break;
            case R.id.bt_switch_camera2:
                Log.d(TAG,"switch camera");
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
