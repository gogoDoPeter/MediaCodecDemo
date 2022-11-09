package com.infinite.mediacodecdemo.encoder;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;

import com.infinite.mediacodecdemo.camera.Camera1Helper;
import com.infinite.mediacodecdemo.camera.CameraProxy;

import java.io.File;

public class AsyncEncoderCore implements Camera1Helper.OnPreviewListener,
        Camera1Helper.OnChangedSizeListener {
    private static final String TAG = "AsyncEncoderCore";
    private boolean mIsRecording;
    private MediaCodecEncoder mEncoder;
    private Context mContext;
    private File mOutFile;
    private int mFrameIndex;
    private static final String dumpPrefix = "/sdcard/DCIM/";
    private static final String dumpPrefix2 = "/data/vendor/media/";
    private String dumpPrefix3;
    //    private Camera1Helper mCamera1Helper;
    private CameraProxy mCameraProxy;


    public AsyncEncoderCore(Activity activity) {
        mContext = activity;
        mOutFile = new File(mContext.getExternalCacheDir(), "demo1.yuv");
        Log.d(TAG, "getExternalCacheDir:" + mOutFile);

//        mCamera1Helper = new Camera1Helper(activity, 0, 1920, 1080);
//        mCamera1Helper.setOnPreviewListener(this); // 设置 onPreviewData(nv21)数据的回调监听
//        mCamera1Helper.setOnChangedSizeListener(this); // 宽高发送改变的监听回调设置

        mIsRecording = false;
        mFrameIndex = 0;
        dumpPrefix3 = mContext.getExternalCacheDir().getAbsolutePath() + File.separator;
    }

    // 调用帮助类：与Surface绑定 == surfaceView.getHolder()
    public void setPreviewDisplay(SurfaceHolder holder) {
        if (mCameraProxy != null) {
            mCameraProxy.setPreviewDisplay(holder);
        }
    }

    public void switchCamera() {
//        mCamera1Helper.switchCamera();
    }

    public void release() {
//        mCamera1Helper.stopPreview();
    }

    @Override
    public void onChanged(int width, int height) {
        Log.d(TAG, "onChanged width=" + width + ", height=" + height);
    }

    @Override
    public void onPreviewData(byte[] data, int width, int height) {
        if (mIsRecording) {
            startMediaCodecRecord(data, width, height);
        } else {
            endMediaCodecRecord();
        }
    }

    private void endMediaCodecRecord() {
        if (mEncoder != null) {
            mEncoder.stopEncoder();
            mEncoder = null;
        }

        mIsRecording = false;
    }

    private void startMediaCodecRecord(byte[] data, int width, int height) {
        if (mEncoder == null) {
            mEncoder = new MediaCodecEncoder(width, height);
            Log.d(TAG, "dumpPrefix3:" + dumpPrefix3);
            mEncoder.setOutputPath(dumpPrefix3 + "mc_sync.mp4");
            mEncoder.startEncoder();
        }
        if (mFrameIndex < 5) {
            Log.d(TAG, "my-tag startMediaCodecRecord width:" + width + " height:" + height);
        }
//        String fileNameIn = dumpPrefix3 +"in_" + String.format("%d_%dx%d.nv21", mFrameIndex, width, height); //get NV21 data
//        FileUtils.dumpData(data, width, height, fileNameIn);
        byte[] outI420 = new byte[width * height * 3 / 2];
        NV21ToI420(data, outI420, width, height);     //NV21 convert to NV12
//        String fileNameOut = dumpPrefix3 +"out_" + String.format("%d_%dx%d.i420", mFrameIndex, width, height); //get I420 data
//        FileUtils.dumpData(outI420, width, height, fileNameOut);
        mFrameIndex++;
        if (mFrameIndex > 100000) {
            mFrameIndex = 0;
        }

        mEncoder.putData(outI420);
    }

    private void NV21ToI420(byte[] input, byte[] out, int width, int height) {
        if (input == null || out == null || width <= 0 || height <= 0) {
            Log.d(TAG, "input param error");
            return;
        }
        int yDataSize = width * height;
        int uvDataSize = yDataSize / 4;
        //Copy y data
        System.arraycopy(input, 0, out, 0, yDataSize);
        //Copy uv data
        for (int i = 0; i < uvDataSize; ++i) {
            out[yDataSize + i] = input[yDataSize + 1 + i * 2]; //u
            out[yDataSize + uvDataSize + i] = input[yDataSize + i * 2]; //v
        }
    }

    //TODO 如果RedMi k40 device, use this interface
    private void NV21ToNV12(byte[] input, byte[] out, int width, int height) {
        if (input == null || out == null || width <= 0 || height <= 0) {
            Log.d(TAG, "input param error");
            return;
        }
        int yDataSize = width * height;
        int uvDataSize = yDataSize / 4;
        //Copy y data
        System.arraycopy(input, 0, out, 0, yDataSize);
        //Copy uv data
        for (int i = 0; i < uvDataSize; ++i) {
            out[yDataSize + i * 2] = input[yDataSize + 1 + i * 2]; //u
            out[yDataSize + 1 + i * 2] = input[yDataSize + i * 2]; //v
        }
    }

    //TODO 待确认. 导致uv数据不对，花屏
    private void NV21ToYUV420(byte[] nv21, byte[] yuv420, int width, int height) {
        /*if (nv21 == null || yuv420 == null) return
        val framesize = width * height
        var i = 0
        var j = 0
        System.arraycopy(nv21, 0, yuv420, 0, framesize)
        i = 0
        while (i < framesize / 4) { //copy v
            yuv420[framesize + framesize / 4 + i] = nv21[i * 2 + framesize]
            i++
        }
        j = 0
        while (j < framesize / 4) { //copy u
            yuv420[framesize + j] = nv21[j * 2 + 1 + framesize]
            j++
        }*/
    }

    public void startRecord() {
        mIsRecording = true;
    }

    public void stopRecord() {
        mIsRecording = false;
    }

    public void setCameraProxy(CameraProxy cameraProxy) {
        if (cameraProxy == null) {
            Log.e(TAG, "cameraProxy is null");
        }
        mCameraProxy = cameraProxy;
    }
}
