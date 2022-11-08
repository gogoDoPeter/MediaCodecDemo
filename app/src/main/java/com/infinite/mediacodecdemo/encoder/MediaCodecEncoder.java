package com.infinite.mediacodecdemo.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.infinite.mediacodecdemo.util.FileUtils;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MediaCodec同步模式
 */
public class MediaCodecEncoder extends BaseEncoder {
    private static final String TAG = "MediaCodecEncoder";
//    public static final String MIMETYPE_VIDEO_AVC = "video/avc";
    public static final String MIMETYPE_VIDEO_AVC = "video/hevc";
    public static final int H264_ENCODER = 1;
    private static final int TIMEOUT_S = 10000;
    private MediaCodec mMediaCodec;
    private volatile boolean isRunning = false;

    private ArrayBlockingQueue<byte[]> yuv420Queue = new ArrayBlockingQueue<>(10);
    private MediaMuxer mMuxer;
    private int mTrackIndex;
    private boolean mMuxerStarted;
    private volatile EncoderCallback mEncoderCallback;

    private byte[] configByte;
    private int mFrameIndex;
    private MediaCodec.BufferInfo mBufferInfo;

    public void setEncoderCallback(EncoderCallback encoderCallback) {
        mEncoderCallback = encoderCallback;
    }

    public MediaCodecEncoder(int width, int height) {
        mFrameIndex = 0;
        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible); //设置COLOR_FormatYUV420Flexible要给mediaCode传NV12数据来编码

        int bitRate = width * height * 4;
        Log.d(TAG,"bitRate:"+bitRate +", width:"+width+" height:"+height);
        /** Constant quality mode */
//        public static final int BITRATE_MODE_CQ = 0;
        /** Variable bitrate mode */
//        public static final int BITRATE_MODE_VBR = 1;
        /** Constant bitrate mode */
//        public static final int BITRATE_MODE_CBR = 2;

//        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ); //crash
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR); //
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);//FPS
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); //2~3s插入一个关键帧比较合理

        try {
            mMediaCodec = MediaCodec.createEncoderByType(MIMETYPE_VIDEO_AVC);
            mMediaCodec.configure(mediaFormat,
                    null, //inputSurface
                    null,   //加密相关的
                    MediaCodec.CONFIGURE_FLAG_ENCODE);

            mMediaCodec.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOutputPath(String outputPath) {
        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        try {
            // /storage/emulated/0/Android/data/com.infinite.mediacodecdemo/cache/mc_sync.mp4
            Log.d(TAG, "outputPath " + outputPath);
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }

        mTrackIndex = -1;
        mMuxerStarted = false;
    }

    public void putData(byte[] buffer) {
        if (yuv420Queue.size() >= 10) { //保持队列size不超过10,因为初始化分配大小就是10
            yuv420Queue.poll();//获取队列开头元素并删除
        }
        yuv420Queue.add(buffer); //给队列(末尾)插入元素
    }

    public void startEncoder() {
        isRunning = true;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                byte[] input = null;
                while (isRunning) {
                    if (!isRunning) {
                        Log.d(TAG, "exit encoder");
                        break;
                    }
                    if (yuv420Queue.size() > 0) {
                        input = yuv420Queue.poll(); //获取队列开头元素并删除
                    }
                    if (input != null) {
                        try {
                            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
                            if (inputBufferIndex >= 0) {
                                ByteBuffer inputBuffer = null;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                    inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                                } else {
                                    inputBuffer = mMediaCodec.getInputBuffers()[inputBufferIndex];
                                }
                                inputBuffer.clear();
                                //  /storage/emulated/0/Android/data/com.infinite.mediacodecdemo/cache/
//                                FileUtils.dumpData(input,1920,1080,"/sdcard/DCIM/input_10.i420");
                                inputBuffer.put(input); //将数据设置给inputBuffer
                                mMediaCodec.queueInputBuffer(inputBufferIndex, 0,
                                        input.length, getPTSUs(), 0);
                            }

                            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
                            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                                if (null != mEncoderCallback) { //mEncoderCallback now is null
                                    mEncoderCallback.outputMediaFormatChanged(H264_ENCODER, newFormat);
                                }
                                if (mMuxer != null) {
                                    if (mMuxerStarted) {
                                        throw new RuntimeException("format changed twice");
                                    }
                                    // now that we have the Magic Goodies, start the muxer
                                    mTrackIndex = mMuxer.addTrack(newFormat);
                                    mMuxer.start();

                                    mMuxerStarted = true;
                                }
                            }

                            while (outputBufferIndex >= 0) {
                                ByteBuffer outputBuffer = null;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                    outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                                } else {
                                    outputBuffer = mMediaCodec.getOutputBuffers()[outputBufferIndex];
                                }
                                byte[] outBuffer2 = new byte[mBufferInfo.size];
                                outputBuffer.get(outBuffer2); //将outputBuffer 中byte数组数据复制给 outBuffer2

                                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                    // The codec config data was pulled out and fed to the muxer when we got
                                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                                    configByte = new byte[mBufferInfo.size];
                                    Log.e(TAG, "new configByte, mBufferInfo.size:" + mBufferInfo.size);
                                    configByte = outBuffer2;

                                    mBufferInfo.size = 0;
                                }

                                if (mBufferInfo.size > 0) {

                                    // adjust the ByteBuffer values to match mBufferInfo (not needed?)
                                    outputBuffer.position(mBufferInfo.offset); //把起始指针放在指定索引处
                                    outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                                    //dump encodedData == H.264 compressed elementary stream
                                    /*if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                                        String fileName = "/sdcard/DCIM/out_" + String.format("%d.h264", mFrameIndex);
                                        Log.d(TAG, "dump fileName:" + fileName);
                                        try {
                                            BufferedOutputStream bos = null;
                                            bos = new BufferedOutputStream(new FileOutputStream(fileName));
                                            byte[] keyframe = new byte[mBufferInfo.size + configByte.length];
                                            Log.e(TAG, "dump keyframe, mBufferInfo.size:" + mBufferInfo.size + ", configbyte.length:" + configByte.length);
                                            System.arraycopy(configByte, 0, keyframe, 0, configByte.length);
                                            System.arraycopy(outBuffer2, 0, keyframe, configByte.length, outBuffer2.length);
                                            bos.write(keyframe, 0, keyframe.length);
                                            bos.flush();
                                            bos.close();
                                            Log.d(TAG, "dump done fileName:" + fileName + " mFrameIndex:" + mFrameIndex);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        mFrameIndex++;
                                        if (mFrameIndex > 100000) {
                                            mFrameIndex = 0;
                                        }
                                    }*/

                                    // write encoded data to muxer(need to adjust presentationTimeUs.
                                    mBufferInfo.presentationTimeUs = getPTSUs();

                                    if (mEncoderCallback != null) { //mEncoderCallback now is null
                                        mEncoderCallback.onEncodeOutput(H264_ENCODER, outputBuffer, mBufferInfo);
                                    }

                                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                                    if (mMuxer != null) {
                                        if (!mMuxerStarted) {
                                            throw new RuntimeException("muxer hasn't started");
                                        }
                                        mMuxer.writeSampleData(mTrackIndex, outputBuffer, mBufferInfo);
                                    }
                                }

                                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);

                                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
                            }
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    } else {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public void stopEncoder() {
        if (mEncoderCallback != null) {
            mEncoderCallback.onStop(H264_ENCODER); //回调
        }
        isRunning = false;
        try {
            if (mMediaCodec != null) {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            }
            if (mMuxer != null) {
                mMuxer.stop();
                mMuxer.release();
                mMuxer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}