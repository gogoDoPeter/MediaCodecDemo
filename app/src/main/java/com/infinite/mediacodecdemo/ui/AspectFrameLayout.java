package com.infinite.mediacodecdemo.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AspectFrameLayout extends FrameLayout {
    private double mTargetAspect = -1.0;
    private static final String TAG = "AspectFrameLayout";

    public AspectFrameLayout(@NonNull Context context) {
        super(context);
    }

    public AspectFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

//    public AspectFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//    }
//
//    public AspectFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//    }

    public void setAspectRatio(double aspectRatio) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }
        Log.d(TAG, "mTargetAspect " + mTargetAspect + ", aspectRatio " + aspectRatio);
        if (mTargetAspect != aspectRatio) {
            mTargetAspect = aspectRatio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "onMeasure target=" + mTargetAspect +
                "input width=[" + MeasureSpec.toString(widthMeasureSpec) +
                "] height=[" + View.MeasureSpec.toString(heightMeasureSpec) + "]");
        if (mTargetAspect > 0) {

            int initW = MeasureSpec.getSize(widthMeasureSpec);
            int initH = MeasureSpec.getSize(heightMeasureSpec);
            int horizPadding = getPaddingLeft() + getPaddingRight();
            int vertPadding = getPaddingTop() + getPaddingBottom();
            Log.d(TAG, "onMeasure initialWidth "+initW+", initialHeight "+initH
                    +" horizPadding "+horizPadding +" vertPadding "+vertPadding);
            initW -= horizPadding;
            initH -= vertPadding;
            double viewAspectRatio = (double) initW / initH;
            double aspectDiff = mTargetAspect / viewAspectRatio - 1;
            Log.d(TAG, "onMeasure initialWidth "+initW+", initialHeight "+initH
                    +" viewAspectRatio "+viewAspectRatio +" aspectDiff "+aspectDiff);
            if (Math.abs(aspectDiff) < 0.01) {
                Log.d(TAG, "Very close already");
            } else {
                if (aspectDiff > 0) { //initialWidth和initialHeight谁小，限制另外一个,这里表示宽小
                    // limited by narrow width; restrict height 受限于狭窄的宽度；限制高度
                    initH = (int)(initW/mTargetAspect);
                }else{
                    initW=(int)(initH/mTargetAspect);
                }
                initW +=horizPadding;
                initH+=vertPadding;
                widthMeasureSpec=MeasureSpec.makeMeasureSpec(initW,MeasureSpec.EXACTLY);
                heightMeasureSpec=MeasureSpec.makeMeasureSpec(initH,MeasureSpec.EXACTLY);
            }
        }
        Log.d(TAG,"output width:"+MeasureSpec.toString(widthMeasureSpec)+", height:"+MeasureSpec.toString(heightMeasureSpec));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
