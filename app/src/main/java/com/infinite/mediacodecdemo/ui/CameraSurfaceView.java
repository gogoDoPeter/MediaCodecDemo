package com.infinite.mediacodecdemo.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.infinite.mediacodecdemo.camera.CameraProxy;

public class CameraSurfaceView extends SurfaceView {
    private static final String TAG = "CameraSurfaceView";
    private CameraProxy mCameraProxy;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private float mOldDistance;

    public CameraSurfaceView(Context context) {
        this(context, null);
        Log.d(TAG, "CameraSurfaceView 1 param");
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        Log.d(TAG, "CameraSurfaceView 2 params");
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
        Log.d(TAG, "CameraSurfaceView 3 params");
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        Log.d(TAG, "CameraSurfaceView 4 params");
        init(context);
    }

    private void init(Context context) {
        Log.d(TAG," init +");
//        getHolder().addCallback(mSurfaceHolderCallback);
//        mCameraProxy = new CameraProxy((Activity) context);
        Log.d(TAG," init -");
    }

//    private final SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
//        @Override
//        public void surfaceCreated(SurfaceHolder holder) {
//            Log.d(TAG, "surfaceCreated");
//            mCameraProxy.openCamera();
//        }
//
//        @Override
//        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//            int previewWidth = mCameraProxy.getPreviewWidth();
//            int previewHeight = mCameraProxy.getPreviewHeight();
//            Log.d(TAG, "surfaceChanged width=" + width + " height=" + height + " previewWidth=" + previewWidth + " previewHeight=" + previewHeight);
//            if (width > height) {
//                setAspectRatio(previewWidth, previewHeight);
//            } else {
//                setAspectRatio(previewHeight, previewWidth);
//            }
//            Log.d(TAG, "surfaceChanged start preview");
//            mCameraProxy.startPreview(holder);
//        }
//
//        @Override
//        public void surfaceDestroyed(SurfaceHolder holder) {
//            Log.d(TAG, "surfaceDestroyed");
//            mCameraProxy.releaseCamera();
//        }
//    };

    private void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

//    public CameraProxy getCameraProxy() {
//        return mCameraProxy;
//    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Log.d(TAG, "onMeasure width=" + width + ", height=" + height);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (event.getPointerCount() == 1) {
//            // 点击聚焦
//            mCameraProxy.focusOnPoint((int) event.getX(), (int) event.getY(), getWidth(), getHeight());
//            return true;
//        }
//        switch (event.getAction() & MotionEvent.ACTION_MASK) {
//            case MotionEvent.ACTION_POINTER_DOWN:
//                mOldDistance = getFingerSpacing(event);
//                break;
//            case MotionEvent.ACTION_MOVE:
//                float newDistance = getFingerSpacing(event);
//                if (newDistance > mOldDistance) {
//                    mCameraProxy.handleZoom(true);
//                } else if (newDistance < mOldDistance) {
//                    mCameraProxy.handleZoom(false);
//                }
//                mOldDistance = newDistance;
//                break;
//            default:
//                break;
//        }
//        return super.onTouchEvent(event);
//    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

}
