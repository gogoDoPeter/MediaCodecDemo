package com.infinite.mediacodecdemo.encoder;

public class TextureMovieEncoder implements Runnable {

    private Object mReadyFence = new Object(); // guards ready/running
    private boolean mRunning;

    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    @Override
    public void run() {

    }
}
