package com.infinite.mediacodecdemo.camera;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraProxy implements Camera.AutoFocusCallback,
        SurfaceHolder.Callback, Camera.PreviewCallback{
    private static final String TAG = "CameraProxy";

    private Activity mActivity;
    private Camera mCamera;
    private Camera.Parameters mParameters;
    private Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int mPreviewWidth = 1920; // default 1440
    private int mPreviewHeight = 1080; // default 1080
    private float mPreviewScale = mPreviewHeight * 1f / mPreviewWidth;
    private Camera.PreviewCallback mPreviewCallback; // 相机预览的数据回调
    private OrientationEventListener mOrientationEventListener;
    private int mLatestRotation = 0;

    private byte[] mPreviewBuffer;
    private byte[] mBuffer; // 存储回调数据
    private OnChangedSizeListener mOnChangedSizeListener; // 你的宽和高发生改变，就会回调此接口
    private OnPreviewListener onPreviewListener;
    private SurfaceHolder mSurfaceHolder; // Surface画面的帮助类

    public CameraProxy(Activity activity, int cameraId, int width, int height) {
        mActivity = activity;
        mCameraId = cameraId;
        mPreviewWidth = width;
        mPreviewHeight = height;
        mOrientationEventListener = new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int orientation) {
                setPictureRotate(orientation);
            }
        };
    }
    public void setOnChangedSizeListener(OnChangedSizeListener listener) {
        mOnChangedSizeListener = listener;
    }

    public void setOnPreviewListener(OnPreviewListener onPreviewListener) {
        this.onPreviewListener = onPreviewListener;
    }

    /**
     * 与Surface绑定 == surfaceView.getHolder()
     * @param surfaceHolder
     */
    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.d(TAG,"surfaceCreated ");
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG,"surfaceChanged width:"+width+" height:"+height);
        // 释放摄像头
        stopPreview();
        Log.d(TAG,"surfaceChanged startPreview ");
        // 开启摄像头
        startPreview();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.d(TAG,"surfaceDestroyed ");
        stopPreview(); // 只要画面不可见，就必须释放，因为预览耗电 耗资源
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(onPreviewListener !=null){
            onPreviewListener.onPreviewData(data, mPreviewWidth, mPreviewHeight);
        }
        camera.addCallbackBuffer(mBuffer);
    }

    public interface OnChangedSizeListener {
        void onChanged(int width, int height);
    }

    public interface OnPreviewListener {
        void onPreviewData(byte[] data, int width, int height);
    }

    public void openCamera() {
        Log.d(TAG, "openCamera cameraId: " + mCameraId);
        mCamera = Camera.open(mCameraId);
        Camera.getCameraInfo(mCameraId, mCameraInfo);
        initConfig();
        setDisplayOrientation();
        Log.d(TAG, "openCamera enable mOrientationEventListener");
        mOrientationEventListener.enable();
    }

    public void releaseCamera() {
        if (mCamera != null) {
            Log.v(TAG, "releaseCamera");
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        mOrientationEventListener.disable();
    }

//    public void startPreview(SurfaceHolder holder) {
//        if (mCamera != null) {
//            Log.v(TAG, "startPreview");
//            try {
//                mCamera.setPreviewDisplay(holder);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            mCamera.startPreview();
//        }
//    }

    public void startPreview() {
        if (mCamera != null) {
            Log.v(TAG, "startPreview");
            try {
                Log.d(TAG,"startPreview width:"+mPreviewWidth+" height:"+mPreviewHeight);
                mBuffer = new byte[mPreviewWidth * mPreviewHeight * 3 / 2];
                // 数据缓存区
                mCamera.addCallbackBuffer(mBuffer);
                mCamera.setPreviewCallbackWithBuffer(this);

                //                mCamera.setPreviewTexture(surface);
                mCamera.setPreviewDisplay(mSurfaceHolder); // SurfaceView 和 Camera绑定
                if (mOnChangedSizeListener != null) { // 如果宽和高发生改变，就会回调此接口
                    mOnChangedSizeListener.onChanged(mPreviewWidth, mPreviewHeight);
                }
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            Log.v(TAG, "stopPreview");
            // 预览数据回调接口
            mCamera.setPreviewCallback(null);
            // 停止预览
            mCamera.stopPreview();
            // 释放摄像头
            mCamera.release();
            mCamera = null;
        }
    }

    public boolean isFrontCamera() {
        return mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    private void initConfig() {
        Log.v(TAG, "initConfig");
        try {
            mParameters = mCamera.getParameters();
            // 如果摄像头不支持这些参数都会出错的，所以设置的时候一定要判断是否支持
            List<String> supportedFlashModes = mParameters.getSupportedFlashModes();
            if (supportedFlashModes != null && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF); // 设置闪光模式
            }
            List<String> supportedFocusModes = mParameters.getSupportedFocusModes();
            if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO); // 设置聚焦模式
            }
            mParameters.setPreviewFormat(ImageFormat.NV21); // 设置预览图片格式
            mParameters.setPictureFormat(ImageFormat.JPEG); // 设置拍照图片格式
            mParameters.setExposureCompensation(0); // 设置曝光强度
            Camera.Size previewSize = getSuitableSize(mParameters.getSupportedPreviewSizes());
            mPreviewWidth = previewSize.width;
            mPreviewHeight = previewSize.height;
            mParameters.setPreviewSize(mPreviewWidth, mPreviewHeight); // 设置预览图片大小
            Log.d(TAG, "previewWidth: " + mPreviewWidth + ", previewHeight: " + mPreviewHeight);
            Camera.Size pictureSize = getSuitableSize(mParameters.getSupportedPictureSizes());
            mParameters.setPictureSize(pictureSize.width, pictureSize.height);
            Log.d(TAG, "pictureWidth: " + pictureSize.width + ", pictureHeight: " + pictureSize.height);
            mCamera.setParameters(mParameters); // 将设置好的parameters添加到相机里
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Camera.Size getSuitableSize(List<Camera.Size> sizes) {
        int minDelta = Integer.MAX_VALUE; // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        int index = 0; // 最小的差值对应的索引坐标
        for (int i = 0; i < sizes.size(); i++) {
            Camera.Size size = sizes.get(i);
            Log.v(TAG, "SupportedSize, width: " + size.width + ", height: " + size.height);
            // 先判断比例是否相等
            if (size.width * mPreviewScale == size.height) {
                int delta = Math.abs(mPreviewWidth - size.width);
                if (delta == 0) {
                    return size;
                }
                if (minDelta > delta) {
                    minDelta = delta;
                    index = i;
                }
            }
        }
        return sizes.get(index);
    }

    /**
     * 设置相机显示的方向，必须设置，否则显示的图像方向会错误
     */
    private void setDisplayOrientation() {
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (mCameraInfo.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    private void setPictureRotate(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return;
        orientation = (orientation + 45) / 90 * 90;
        int rotation;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (mCameraInfo.orientation - orientation + 360) % 360;
        } else {  // back-facing camera
            rotation = (mCameraInfo.orientation + orientation) % 360;
        }
        mLatestRotation = rotation;
    }

    public int getLatestRotation() {
        return mLatestRotation;
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
        if (mPreviewBuffer == null) {
            mPreviewBuffer = new byte[mPreviewWidth * mPreviewHeight * 3 / 2];
        }
        mCamera.addCallbackBuffer(mPreviewBuffer);
        mCamera.setPreviewCallbackWithBuffer(mPreviewCallback); // 设置预览的回调
    }

    public void takePicture(Camera.PictureCallback pictureCallback) {
        mCamera.takePicture(null, null, pictureCallback);
    }

    public void switchCamera() {
        mCameraId ^= 1; // 先改变摄像头朝向
        releaseCamera();
        openCamera();
    }

    public void focusOnPoint(int x, int y, int width, int height) {
        Log.v(TAG, "touch point (" + x + ", " + y + ")");
        if (mCamera == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        // 1.先要判断是否支持设置聚焦区域
        if (parameters.getMaxNumFocusAreas() > 0) {
            // 2.以触摸点为中心点，view窄边的1/4为聚焦区域的默认边长
            int length = Math.min(width, height) >> 3; // 1/8的长度
            int left = x - length;
            int top = y - length;
            int right = x + length;
            int bottom = y + length;
            // 3.映射，因为相机聚焦的区域是一个(-1000,-1000)到(1000,1000)的坐标区域
            left = left * 2000 / width - 1000;
            top = top * 2000 / height - 1000;
            right = right * 2000 / width - 1000;
            bottom = bottom * 2000 / height - 1000;
            // 4.判断上述矩形区域是否超过边界，若超过则设置为临界值
            left = left < -1000 ? -1000 : left;
            top = top < -1000 ? -1000 : top;
            right = right > 1000 ? 1000 : right;
            bottom = bottom > 1000 ? 1000 : bottom;
            Log.d(TAG, "focus area (" + left + ", " + top + ", " + right + ", " + bottom + ")");
            ArrayList<Camera.Area> areas = new ArrayList<>();
            areas.add(new Camera.Area(new Rect(left, top, right, bottom), 600));
            parameters.setFocusAreas(areas);
        }
        try {
            mCamera.cancelAutoFocus(); // 先要取消掉进程中所有的聚焦功能
            mCamera.setParameters(parameters);
            mCamera.autoFocus(this); // 调用聚焦
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleZoom(boolean isZoomIn) {
        if (mParameters.isZoomSupported()) {
            int maxZoom = mParameters.getMaxZoom();
            int zoom = mParameters.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            Log.d(TAG, "handleZoom: zoom: " + zoom);
            mParameters.setZoom(zoom);
            mCamera.setParameters(mParameters);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }

    public Camera getCamera() {
        return mCamera;
    }

    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        Log.d(TAG, "onAutoFocus: " + success);
    }
}
