package com.infinite.mediacodecdemo.encoder;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
//import javax.microedition.khronos.egl.EGLContext;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.infinite.mediacodecdemo.gles.EglCore;
import com.infinite.mediacodecdemo.gles.FullFrameRect;
import com.infinite.mediacodecdemo.gles.Texture2dProgram;
import com.infinite.mediacodecdemo.gles.WindowSurface;

public class TextureMovieEncoder implements Runnable {
    private static final String TAG = "TextureMovieEncoder";
    private static final boolean VERBOSE = true;

    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_QUIT = 5;

    // ----- accessed exclusively by encoder thread -----
    private int mTextureId;
    private int mFrameNum;
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private FullFrameRect mFullScreen;
    private VideoEncoderCore mVideoEncoder;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;

    private Object mReadyFence = new Object();      // guards ready/running
    private boolean mReady;
    private boolean mRunning;

    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    //Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
    @Override
    public void run() {
        Log.d(TAG, "Encoder thread run start");
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {

            mHandler = new EncoderHandler(this);
            mReady = true;
            Log.d(TAG, "Encoder thread before notify");
            mReadyFence.notify();
            Log.d(TAG, "Encoder thread after notify");
        }
        //wait here to get message and call handleMessage to deal with
        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }

    public void startRecording(EncoderConfig config) {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "TextureMovieEncoder").start();
            while (!mReady) {
                try {
                    Log.d(TAG, "Encoder: startRecording before wait");
                    mReadyFence.wait(); //等线程启动好了，再继续执行
                    Log.d(TAG, "Encoder: startRecording after wait");
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }

    //Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     * <p>
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     * <p>
     * TODO: have the encoder thread invoke a callback on the UI thread just before it shuts down
     * so we can provide reasonable status UI (and let the caller know that movie encoding
     * has completed).
     */
    public void stopRecording() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
        // We don't know when these will actually finish (or even start).  We don't want to
        // delay the UI thread though, so we return immediately.
    }

    /**
     * Tells the video recorder what texture name to use.  This is the external texture that
     * we're receiving camera previews in.  (Call from non-encoder thread.)
     * <p>
     * TODO: do something less clumsy
     */
    public void setTextureId(int textureId) { // Call from non-encoder thread and in non-encoder thread
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, textureId, 0, null));
    }

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     * <p>
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     * <p>
     * TODO: either block here until the texture has been rendered onto the encoder surface,
     * or have a separate "block if still busy" method that the caller can execute immediately
     * before it calls updateTexImage().  The latter is preferred because we don't want to
     * stall the caller while this thread does work.
     */
    public void frameAvailable(SurfaceTexture st) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        // TODO - avoid alloc every frame
        float[] transform = new float[16];
        st.getTransformMatrix(transform);
        long timestamp = st.getTimestamp();
        if (timestamp == 0) {
            // Seeing this after device is toggled off/on with power button.  The
            // first frame back has a zero timestamp.
            //
            // MPEG4Writer thinks this is cause to abort() in native code, so it's very
            // important that we just ignore the frame.
            Log.w(TAG, "Got SurfaceTexture with timestamp of zero");
            return;
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE, (int) (timestamp >> 32),
                ((int) timestamp), transform));
    }

    /**
     * Encoder configuration.
     * <p>
     * Object is immutable, which means we can safely pass it between threads without
     * explicit synchronization (and don't need to worry about it getting tweaked out from
     * under us).
     * <p>
     * TODO: make frame rate and iframe interval configurable?  Maybe use builder pattern
     *       with reasonable defaults for those and bit rate.
     */
    public static class EncoderConfig {
        final File mOutputFile;
        final int mWidth;
        final int mHeight;
        final int mBitrate;
        final EGLContext mEglContext;

        public EncoderConfig(File mOutputFile, int mWidth, int mHeight, int mBitrate, EGLContext sharedEglContext) {
            this.mOutputFile = mOutputFile;
            this.mWidth = mWidth;
            this.mHeight = mHeight;
            this.mBitrate = mBitrate;
            this.mEglContext = sharedEglContext;
        }

        @Override
        public String toString() {
            return "EncoderConfig{" +
                    "mOutputFile=" + mOutputFile +
                    ", mWidth=" + mWidth +
                    ", mHeight=" + mHeight +
                    ", mBitrate=" + mBitrate +
                    ", mEglContext=" + mEglContext +
                    '}';
        }
    }

    //Handles encoder state change requests.  The handler is created on the encoder thread.
    private static class EncoderHandler extends Handler {
        private WeakReference<TextureMovieEncoder> mWeakEncoder;

        public EncoderHandler(TextureMovieEncoder encoder) {
            this.mWeakEncoder = new WeakReference<TextureMovieEncoder>(encoder);
        }

        // TODO runs on encoder thread 因为编码比较耗时，需要放在单独一个线程做
        @Override
        public void handleMessage(@NonNull Message msg) {
            int what = msg.what;
            Object obj = msg.obj;
            TextureMovieEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "encoder is null");
                return;
            }
            switch (what) {
                case MSG_START_RECORDING:
                    Log.d(TAG, "handleMessage in MSG_START_RECORDING");
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    Log.d(TAG, "handleMessage in MSG_STOP_RECORDING");
                    encoder.handleStopRecording();
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timestamp = (((long) msg.arg1) << 32) | (((long) msg.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable((float[]) obj, timestamp);
                    break;
                case MSG_SET_TEXTURE_ID:
                    Log.d(TAG, "handleMessage in MSG_SET_TEXTURE_ID");
                    encoder.handleSetTexture(msg.arg1);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    Log.d(TAG, "handleMessage in MSG_UPDATE_SHARED_CONTEXT");
                    encoder.handleUpdateSharedContext((EGLContext) msg.obj);
                    break;
                case MSG_QUIT:
                    Log.d(TAG, "handleMessage in MSG_QUIT");
                    Looper.myLooper().quit();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    //Handles a request to stop encoding.
    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");
        mVideoEncoder.drainEncoder(true);
        releaseEncoder();
    }

    private void releaseEncoder() {
        Log.d(TAG, "releaseEncoder mInputWindowSurface:" + mInputWindowSurface);
        mVideoEncoder.release();

        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release(false);
            mFullScreen = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    /**
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     * <p>
     *
     * @param transform      The texture transform, from SurfaceTexture.
     * @param timestampNanos The frame's timestamp, from SurfaceTexture.
     */
    private void handleFrameAvailable(float[] transform, long timestampNanos) {
        if (VERBOSE) Log.d(TAG, "handleFrameAvailable tr=" + transform.toString()
                + ", mTextureId:" + mTextureId + ", mInputWindowSurface:" + mInputWindowSurface);

        //for debug to dump
        /*try {
            String fileName = "/sdcard/DCIM/in_"+mFrameNum+".rgb";
//            mInputWindowSurface.saveFrame(new File(fileName));
            mInputWindowSurface.saveFrame2Rgb(new File(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        mVideoEncoder.drainEncoder(false);
        Log.d(TAG, "handleFrameAvailable: encoder Draw frame mTextureId:" + mTextureId);
        // TODO 用于更新录像画面的. 这里的FullFrameRect和CameraSurfaceRender中FullFrameRect不是一个对象，为什么创建两个？
        mFullScreen.drawFrame(mTextureId, transform);

        mFrameNum++;

        mInputWindowSurface.setPresentationTime(timestampNanos); //单位应该是纳秒 ns
        mInputWindowSurface.swapBuffers();//TODO 刷新数据
    }

    //start recording
    private void handleStartRecording(EncoderConfig config) {
        Log.d(TAG, "handleStartRecording " + config);
        mFrameNum = 0;
        prepareEncoder(config.mEglContext, config.mWidth, config.mHeight,
                config.mBitrate, config.mOutputFile);
    }

    private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitrate, File outputFile) {

        Log.d(TAG, "prepareEncoder sharedContext:" + sharedContext + ", mInputWindowSurface:" + mInputWindowSurface);
        try {
            mVideoEncoder = new VideoEncoderCore(width, height, bitrate, outputFile);
        } catch (IOException ioe) {
//            e.printStackTrace();
            throw new RuntimeException(ioe);
        }
        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        //TODO 将编码的inputSurface设置给WindowSurface
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        // 将EGLDisplay、EGLSurface和 EGLContext 进行绑定
        mInputWindowSurface.makeCurrent();
        Log.d(TAG, "prepareEncoder new mInputWindowSurface:" + mInputWindowSurface);
        mFullScreen = new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        Log.d(TAG, "prepareEncoder mFullScreen:" + mFullScreen);
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     * <p>
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        mFullScreen.release(false);
        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();// 绑定EglContext和Surface到显示设备(mEGLDisplay)中

        // Create new programs and such for the new context.
        mFullScreen = new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
    }

    //on the encoder thread
    private void handleSetTexture(int textureId) {
        Log.d(TAG, "handleSetTexture " + textureId);
        mTextureId = textureId;
    }
}
