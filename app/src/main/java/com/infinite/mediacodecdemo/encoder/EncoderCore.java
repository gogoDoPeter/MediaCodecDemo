package com.infinite.mediacodecdemo.encoder;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;

import com.infinite.mediacodecdemo.camera.Camera1Helper;
import com.infinite.mediacodecdemo.util.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class EncoderCore implements Camera1Helper.OnPreviewListener, Camera1Helper.OnChangedSizeListener {
    private static final String TAG = "MediaEncoder";
    private Camera1Helper mCamera1Helper;
    private boolean mIsRecording;
    private MediaCodecEncoder mEncoder;
    private Context mContext;
    private File mOutFile;
    private int mFrameIndex;

    public EncoderCore(Activity activity) {
        mContext = activity;
        mOutFile = new File(mContext.getExternalCacheDir(), "demo1.yuv");

        mCamera1Helper = new Camera1Helper(activity, 0, 1920, 1080);
        mCamera1Helper.setOnPreviewListener(this); // 设置 onPreviewData(nv21)数据的回调监听
        mCamera1Helper.setOnChangedSizeListener(this); // 宽高发送改变的监听回调设置

        mIsRecording = false;
        mFrameIndex = 0;
    }

    // 调用帮助类：与Surface绑定 == surfaceView.getHolder()
    public void setPreviewDisplay(SurfaceHolder holder) {
        mCamera1Helper.setPreviewDisplay(holder);
    }

    public void switchCamera() {
        mCamera1Helper.switchCamera();
    }

    public void release() {
        mCamera1Helper.stopPreview();
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
            mEncoder.setOutputPath(mContext.getExternalCacheDir().getAbsolutePath() + File.separator + "mc_sync.mp4");
            mEncoder.startEncoder();
        }
        String fileNameIn = "/sdcard/DCIM/in_" + String.format("%d_%dx%d.nv21", mFrameIndex, width, height); //get NV21 data
//        FileUtils.dumpData(data, width, height, fileNameIn);
        byte[] outI420 = new byte[width * height * 3 / 2];
        NV21ToYUV420(data, outI420, width, height);     //NV21 convert to I420
        String fileNameOut = "/sdcard/DCIM/out_" + String.format("%d_%dx%d.i420", mFrameIndex, width, height); //get I420 data
//        FileUtils.dumpData(outI420, width, height, fileNameOut);
        mFrameIndex++;
        if (mFrameIndex > 100000) {
            mFrameIndex = 0;
        }
        //NV21ToYUV420(data,nv12,width,height);

        mEncoder.putData(outI420);
    }

    private void NV21ToYUV420(byte[] input, byte[] out, int width, int height) {
        /*if (nv21 == null || yuv420 == null) return
        val framesize = width * height
        var i = 0
        var j = 0
        System.arraycopy(nv21, 0, yuv420, 0, framesize)
        i = 0
        while (i < framesize / 4) {
            yuv420[framesize + framesize / 4 + i] = nv21[i * 2 + framesize]
            i++
        }
        j = 0
        while (j < framesize / 4) {
            yuv420[framesize + j] = nv21[j * 2 + 1 + framesize]
            j++
        }*/
        if (input == null || out == null) {
            Log.d(TAG, "input param error");
            return;
        }
        int yDataSize = width * height;
        int uvDataSize = yDataSize / 4;
        System.arraycopy(input, 0, out, 0, yDataSize);
        //Copy uv data
        for (int i = 0; i < uvDataSize; ++i) {
            out[yDataSize + i] = input[yDataSize + 1 + i * 2];
            out[yDataSize + uvDataSize + i] = input[yDataSize + i * 2];
        }
    }

    public void startRecord() {
        mIsRecording = true;
    }

    public void stopRecord() {
        mIsRecording = false;
    }
}
