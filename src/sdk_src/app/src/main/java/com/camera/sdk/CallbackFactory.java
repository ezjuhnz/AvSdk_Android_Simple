package com.camera.sdk;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/*
该类有两种用途:
1.给用户提供回调获取数据,外部使用
2.用于在各个类之间传递数据,内部使用
 */
public abstract class CallbackFactory {

    public static abstract class OuterCallback{
        /**
         * 获取摄像头原始采集数据的回调,一般为NV21格式
         * @param rawData
         */
        public abstract void onCameraRawData(byte[] rawData); // Must implement

        /**
         * 获取屏幕原始采集数据的回调,一般为RGB格式
         * @param rawData
         */
        public abstract void onScreenRawData(byte[] rawData); // Must implement

        /**
         * 获取编码后的摄像头数据的回调,如H264
         * @param encData
         */
        public abstract void onCameraEncData(byte[] encData); // Must implement

        /**
         * 获取编码后的屏幕数据的回调,如H264
         * @param encData
         */
        public abstract void onScreenEncData(byte[] encData); // Must implement

        /**
         * 原始PCM音频数据回调,用于客户端获取原始音频数据
         * @param rawData
         */
        public abstract void onAudioRawData(byte[] rawData);

        /**
         * 编码后AAC音频数据回调,用于客户端获取编码后音频数据
         * @param encData
         */
        public abstract void onAudioEncData(byte[] encData);

        public abstract void onError(byte[] rawData); // Must implement
    }

    /**
     * 1.摄像头原始数据回调
     */
    public interface VideoCaptureCallback{
        void onRawData(byte[] data);
    }

    /**
     * 2.摄像头编码后的数据回调
     */
    public interface VideoEncCallback{
        void onVideoData(final int trackIndex, final ByteBuffer outBuf,
                         final MediaCodec.BufferInfo bufferInfo, final MediaFormat mediaFormat);
        void onOuterVideoData(byte[] data);
        void onMediaFormat(int trackIndex, MediaFormat mediaFormat);
    }

    /**
     * 3.屏幕原始数据回调
     */
    public interface ScreenRawCallback{
        void onRawData(byte[] data);
    }

    /**
     * 4.屏幕编码后数据回调
     */
    public interface ScreenEncCallback{
        void onVideoData(final int trackIndex, final ByteBuffer outBuf, final MediaCodec.BufferInfo bufferInfo);
        void onMediaFormat(int trackIndex, MediaFormat mediaFormat);
    }

    /**
     *5.原始音频数据回调
     */
    public interface AudioCaptureCallback {
        void onRawData(byte[] data, long timeStamp);
    }

    /**
     * 6.编码后的音频数据回调
     */
    public interface AudioEncCallback{
        void onAudioData(final int trackIndex, final ByteBuffer outBuf, final MediaCodec.BufferInfo bufferInfo);
        void onOuterAudioData(byte[] data);
        void onMediaFormat(int trackIndex, MediaFormat mediaFormat);
    }

    public interface AVEncodeCallback{
        void onCameraFormat(final int trackIndex, MediaFormat mediaFormat);
        void onScreenFormat(final int trackIndex, MediaFormat mediaFormat);
    }
}
