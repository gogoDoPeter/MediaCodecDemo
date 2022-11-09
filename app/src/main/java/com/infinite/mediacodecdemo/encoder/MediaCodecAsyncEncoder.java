package com.infinite.mediacodecdemo.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MediaCodec异步模式
 */
public class MediaCodecAsyncEncoder extends BaseEncoder {
    private static final String TAG = "MCAsyncEncoder";
    public static final String MIMETYPE_VIDEO_AVC = "video/avc";
//    public static final String MIMETYPE_VIDEO_AVC = "video/hevc";
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

    public MediaCodecAsyncEncoder(int width, int height) {
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

            mMediaCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    Log.i(TAG, "onInputBufferAvailable:" + index);
                    byte[] input = null;
                    if (isRunning) {
                        if (yuv420Queue.size() > 0) {
                            input = yuv420Queue.poll();
                        }
                        if (input != null) {
                            ByteBuffer inputBuffer = codec.getInputBuffer(index);
                            inputBuffer.clear();
                            inputBuffer.put(input);
                            codec.queueInputBuffer(index, 0, input.length, getPTSUs(), 0);
                        }
                    }
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                    Log.i(TAG, "onOutputBufferAvailable:" + index);
                    ByteBuffer outputBuffer = codec.getOutputBuffer(index);

                    if (info.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        info.size = 0;
                    }

                    if (info.size > 0) {
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        outputBuffer.position(info.offset);
                        outputBuffer.limit(info.offset + info.size);
                        // write encoded data to muxer(need to adjust presentationTimeUs.
                        info.presentationTimeUs = getPTSUs();

                        if (mEncoderCallback != null) {
                            //回调
                            mEncoderCallback.onEncodeOutput(H264_ENCODER, outputBuffer, info);
                        }
                        prevOutputPTSUs = info.presentationTimeUs;
                        if (mMuxer != null) {
                            if (!mMuxerStarted) {
                                throw new RuntimeException("muxer hasn't started");
                            }
                            mMuxer.writeSampleData(mTrackIndex, outputBuffer, info);
                        }

                    }
                    codec.releaseOutputBuffer(index, false);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    Log.e(TAG, "onError");
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    Log.i(TAG, "onOutputFormatChanged format:" + format);
                    if (null != mEncoderCallback) {
                        mEncoderCallback.outputMediaFormatChanged(H264_ENCODER, format);
                    }
                    if (mMuxer != null) {
                        if (mMuxerStarted) {
                            throw new RuntimeException("format changed twice");
                        }
                        // now that we have the Magic Goodies, start the muxer
                        mTrackIndex = mMuxer.addTrack(format);
                        mMuxer.start();

                        mMuxerStarted = true;
                    }
                }
            });


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