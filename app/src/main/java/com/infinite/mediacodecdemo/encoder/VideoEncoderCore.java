package com.infinite.mediacodecdemo.encoder;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoEncoderCore {
    private static final String TAG = "VideoEncoderCore";
    private static final boolean VERBOSE = true;

    private MediaCodec mEncoder;
    private Surface mInputSurface;
    private MediaCodec.BufferInfo mBufferInfo;
    private boolean mMuxerStarted;
    private MediaMuxer mMuxer;
    private int mTrackIndex;
    public byte[] configBytes;
    private int mFrameIndex;

    private static final String MIME_TYPE = "video/avc";// H.264 Advanced Video Coding
    //    private static final String MIME_TYPE = "video/hevc";  // H.265 High Efficiency Video Coding
    private static final int FRAME_RATE = 30;
    private static final int IFRAME_INTERVAL = 2;

    //Configures encoder and muxer state, and prepares the input Surface.
    public VideoEncoderCore(int width, int height, int bitRate, File outputFile)
            throws IOException {
        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        Log.d(TAG, "format: " + format + " frameRate:" + FRAME_RATE + " width:" + width + " height:" + height);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();

        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        mMuxer = new MediaMuxer(outputFile.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        Log.d(TAG, "outputFile:" + outputFile);
        mTrackIndex = -1;
        mMuxerStarted = false;
    }

    public Surface getInputSurface() {
        return mInputSurface;
    }

    //Releases encoder resources.
    public void release() {
        Log.d(TAG, "releasing encoder objects");
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mMuxer != null) {
            // TODO: stop() throws an exception if you haven't fed it any data.  Keep track
            //       of frames submitted, and don't call stop() if we haven't written anything.
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p>
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    public void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
        if (VERBOSE) {
            Log.d(TAG, "endOfStream " + endOfStream);
        }
        if (endOfStream) {
            Log.d(TAG, "sending EOS to encoder");
            mEncoder.signalEndOfInputStream();
        }
        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;
                } else {
                    Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format change twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);
                // now start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result :" + encoderStatus);
            } else {
                ByteBuffer encodeData = encoderOutputBuffers[encoderStatus];
                if (encodeData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                byte[] outBuffer = new byte[mBufferInfo.size];
                //将encodedData中byte数组数据复制给outBuffer
                encodeData.get(outBuffer);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    configBytes = new byte[mBufferInfo.size];
                    Log.e(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG, new configByte, bufferInfo.size:" + mBufferInfo.size);
                    configBytes = outBuffer;
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    Log.d(TAG, "current mBufferInfo.size=" + mBufferInfo.size + " offset=" + mBufferInfo.offset);
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodeData.position(mBufferInfo.offset); //把起始指针放在指定索引处
                    encodeData.limit(mBufferInfo.offset + mBufferInfo.size); //设置新的limit

                    //TODO dump encodedData == H.264 compressed elementary stream
                    /*if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                        String fileName = "/sdcard/DCIM/out_" + String.format("%d.h264", mFrameIndex);
                        Log.d(TAG, "dump fileName:" + fileName);
                        mFrameIndex++;
                        if (mFrameIndex > 100000) {
                            mFrameIndex = 0;
                        }
                        try {
                            BufferedOutputStream bos = null;
                            bos = new BufferedOutputStream(new FileOutputStream(fileName));
                            byte[] keyframe = new byte[mBufferInfo.size + configBytes.length];
                            Log.e(TAG, "write key frame, bufferInfo.size:" + mBufferInfo.size + ", configbyte.length:" + configBytes.length);
                            System.arraycopy(configBytes, 0, keyframe, 0, configBytes.length);
                            System.arraycopy(outBuffer, 0, keyframe, configBytes.length, outBuffer.length);
                            bos.write(keyframe, 0, keyframe.length);
                            bos.flush();
                            bos.close();
                            Log.d(TAG, "dump done fileName:" + fileName + " mFrameIndex:" + mFrameIndex);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }*/

                    mMuxer.writeSampleData(mTrackIndex, encodeData, mBufferInfo);
                    if (VERBOSE) {
                        Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                mBufferInfo.presentationTimeUs);
                    }
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        Log.d(TAG, "end of stream reached");
                    }
                    break; // out of while
                }
            }
        }
    }

    private void dumpH264Data(ByteBuffer encodedData, int size, String fileName) {
        Log.d(TAG, "dump size:" + size);
        byte[] buffer = new byte[size];
        encodedData.get(buffer); //将byteBuffer中数据复制给buffer中
        //写入 .h264 文件
        try {
            BufferedOutputStream bos = null;
            bos = new BufferedOutputStream(new FileOutputStream(fileName));
//            Bitmap bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
//            bmp.copyPixelsFromBuffer(data); //从Buffer中复制像素到Bitmap
//            bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
//            bmp.recycle();

//            bos.write(buffer);

            Bitmap bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(encodedData); //从Buffer中复制像素到Bitmap
            bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
            bos.close();
            Log.d(TAG, "dump done fileName:" + fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dumpEncodeData2(ByteBuffer data, String fileName) {
        BufferedOutputStream bos = null;
        try {
//            int width = 1280; //getWidth();
//            int height = 720; //getHeight();
//            ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);

            bos = new BufferedOutputStream(new FileOutputStream(fileName));
            Bitmap bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(data);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
//            bmp.compress(Bitmap.CompressFormat.PNG, 99, bos);
            bmp.recycle();
            if (bos != null) {
                bos.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
