package com.camera.sdk;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoEncoder {
    private static final String TAG = "VideoEncoder";
    private static final String defaultMime = MediaFormat.MIMETYPE_VIDEO_AVC; //h264是默认视频编码
    private String mPath; //编码数据存储的文件路径
    private int mWidth;
    private int mHeight;
    private int mFps;
    private int mKeyFrameInterval;
    private String mVideoEncPath;
    private String mVideoMime;
    private MediaCodec.BufferInfo mBufferInfo;
    private MediaFormat mVideoFormat;
    private MediaCodec mVideoEncoder; //编码器:将NV21数据编码成H264或H265
    private boolean encodeEnd;
    private boolean isStared;
    private long presentTimeUS;
    private int TIMEOUT_USEC = 12000;
    private Object lock = new Object();
    private byte[] configbyte;
    private boolean isKeyFrameAdded = false;

    private CallbackFactory.VideoEncCallback mVideoEncCallback;
    private BufferedOutputStream bos_h264;

    private MediaFormat newFormat;

    public void initVideoEncoder(int width, int height, int videoFmtIdx, int fps, int bitrate, int keyFrameInterval, String videoEncPath){
        LogUtil.d(TAG,"hello initCameraEncoder start==");
        LogUtil.e(TAG,"width,height,videoFmtIdx,fps,bitrate,keyFrameInterval=" +
                width + "," + height + "," + videoFmtIdx + "," + fps + "," + bitrate + "," + keyFrameInterval);
        mPath = videoEncPath;
        mWidth  = width;
        mHeight = height;
        mFps = fps;
        mKeyFrameInterval = keyFrameInterval;
        mVideoEncPath = videoEncPath;
        mVideoMime = Constants.videoEncFormats[videoFmtIdx];

        mBufferInfo = new MediaCodec.BufferInfo();
        //选择系统用于编码H264/H265的编码器信息
        //判断手机是否支持当前编码格式,如果不支持,则使用默认的编码格式
        boolean isSupport = isCodecSupport(mVideoMime);
        if(!isSupport){
            LogUtil.e(TAG,"hello current video encode format is not supported:" + mVideoMime);
            mVideoMime = defaultMime;
            LogUtil.e(TAG,"we will choose an alternative for you: " + defaultMime);
        }

        mVideoFormat = MediaFormat.createVideoFormat(mVideoMime, mWidth, mHeight);
        mVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        LogUtil.d(TAG,"hello initVideoEncoder mFps= " + mFps);
        mVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFps);
        mVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mKeyFrameInterval);
        LogUtil.d(TAG,"hello width,height,fps,bitrate,mVideoMime= " +
                mWidth + "," + mHeight + "," + mFps + "," +bitrate + "," + mVideoMime);
        //创建视频编码器,用于将NV21编码成H264
        try {
            mVideoEncoder = MediaCodec.createEncoderByType(mVideoMime);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void uninitVideoEncoder(){
        stopVideoEncoder();
        if(mVideoEncoder != null){
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
    }

    //开始编码,编码新线程中执行
    public void startVideoEncoder(){
        synchronized (lock){
            LogUtil.d(TAG,"hello startEncode start==");
            presentTimeUS = System.nanoTime() / 1000;
            isStared = true;
            encodeEnd = false;
            mVideoEncoder.configure(mVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mVideoEncoder.start();
        }
    }

    //结束编码
    public void stopVideoEncoder(){
        LogUtil.d(TAG,"hello video stopEncode==");
        closeStream();
        synchronized (lock){
            encodeEnd = true;
        }

        if(mVideoEncoder != null){
            mVideoEncoder.stop();
        }
    }

    //编码视频数据
    private void videoEncoding(byte[] input){
        if(mVideoEncoder == null) return;
        LogUtil.d(TAG,"hello encodeVideoData start==");
        ByteBuffer[] inputBuffers = mVideoEncoder.getInputBuffers();
        ByteBuffer[] outputBuffers = mVideoEncoder.getOutputBuffers();
        int inputBufferIndex = mVideoEncoder.dequeueInputBuffer(-1); //-1:无限等待
        if(input != null){
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(input);
                long pts = System.nanoTime() / 1000 -  presentTimeUS;
                LogUtil.d(TAG,"hello pts= " + pts);
                if(encodeEnd){
                    LogUtil.d(TAG,"hello receive vEncoderEnd ==");
                    mVideoEncoder.queueInputBuffer(inputBufferIndex, 0, input.length, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }else{
                    mVideoEncoder.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                }
            }
            int outputBufferIndex = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

            //mBufferInfo
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                outputBuffers = mVideoEncoder.getOutputBuffers();
            }else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                newFormat = mVideoEncoder.getOutputFormat();
                mVideoEncCallback.onMediaFormat(Constants.TRACK_VIDEO, newFormat);
            }
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                LogUtil.e(TAG,"hello mBufferInfo.flags= " + mBufferInfo.flags);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    LogUtil.d(TAG, "hello ignoring BUFFER_FLAG_CODEC_CONFIG");
                    //将编码好的视频数据返回给sdkMedia
                    mVideoEncCallback.onVideoData(Constants.TRACK_VIDEO, outputBuffer, mBufferInfo, newFormat);
                }
                //============//
                byte[] outData = new byte[mBufferInfo.size];
                LogUtil.e(TAG,"hello bufferInfo.size= " + mBufferInfo.size);
                outputBuffer.get(outData);
                //1.如果是视频配置信息
                if(mBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG){ //KEY_FRAME = 1, END_OF_STREAM = 4
                    Log.d("TAG","hello flag 1 config");
                    configbyte = new byte[mBufferInfo.size];
                    configbyte = outData;
                }else if(mBufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME){
                    //2.如果是视频关键帧:关键帧在帧的头部携带配置信息
                    Log.d(TAG,"hello flags 2 bufferInfo.size,configbyte.length= " +
                            mBufferInfo.size + "," + configbyte.length);
                    byte[] keyframe = new byte[mBufferInfo.size + configbyte.length];
                    System.arraycopy(configbyte, 0, keyframe, 0, configbyte.length);
                    System.arraycopy(outData, 0, keyframe, configbyte.length, outData.length);
                    if(mVideoEncCallback != null){
                        mVideoEncCallback.onOuterVideoData(keyframe);
                    }
                    try {
                        if(bos_h264 != null){
                            isKeyFrameAdded = true;
                            bos_h264.write(keyframe, 0, keyframe.length);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    //3.如果是其它数据
                    if(mVideoEncCallback != null){
                        mVideoEncCallback.onOuterVideoData(outData);
                    }
                    Log.d(TAG,"hello flag 3 normal frame==");
                    try {
                        if(bos_h264 != null){
                            if(isKeyFrameAdded){
                                bos_h264.write(outData, 0, outData.length);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                mVideoEncoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

                //编码结束的标志
                if(encodeEnd){
                    LogUtil.d(TAG, "hello Recv Video Encoder===BUFFER_FLAG_END_OF_STREAM=====" );
                    isStared = false;
                }
            }
        }
    }

    //设置回调
    public void setVideoEncCallback(CallbackFactory.VideoEncCallback callback){
        mVideoEncCallback = callback;
    }

    public void openStream(){
        createFile(mVideoEncPath);
    }

    public void closeStream(){

        if(bos_h264 != null){
            try {
                bos_h264.close();
                LogUtil.d(TAG,"hello view closeStream==");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        bos_h264 = null;
        isKeyFrameAdded = false;
    }

    /**
     * 判断设备是否支持当前多媒体类型
     * @param mimeType
     * @return
     */
    private boolean isCodecSupport(String mimeType){
        boolean isSupport = false;
        int numCodecs = MediaCodecList.getCodecCount();
        Log.e(TAG,"hello numCodecs= " + numCodecs);
        MediaCodecInfo codecInfo = null;
        for (int i = 0; i < numCodecs && codecInfo == null; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            Log.d("sakalog", "Codec : " + info.getName());
            if (!info.isEncoder()) {
                Log.d("sakalog", "not encoder");
                continue;
            }
            String[] types = info.getSupportedTypes();

            for (int j = 0; j < types.length && !isSupport; j++) {
                Log.e(TAG,"hello types[" + i + "]= " + types[j]);
                if (types[j].equals(mimeType)) {
                    Log.d("sakalog", types[j] + " found!");
                    isSupport = true;
                } else {
                    Log.d("sakalog", types[j]);
                }
            }
            if (!isSupport)
                continue;
            codecInfo = info;
        }
        return isSupport;
    }

    public void onVideoEncodeData(byte[] input){
        if(isStared && input != null){
            LogUtil.d(TAG,"hello capture data length= " + input.length);
            byte[] yuv420sp = new byte[mWidth * mHeight * 3 / 2];
            //NV21转NV12,编码时要使用NV12数据类型
//                        JniImageUtil.Nv21ToNv12(input, yuv420sp, mWidth, mHeight);
            ImageFormatUtil.NV21ToNV12(input, yuv420sp, mWidth, mHeight); //将摄像机采集到的NV21转成NV12
            input = yuv420sp;
            videoEncoding(input);
        }
    }

    private boolean createFile(String fileName){
        LogUtil.d(TAG,"hello createFile name=" + fileName);
        if(fileName == null || fileName.trim().equals("")){
            LogUtil.e(TAG,"error fileName is null");
            return false;
        }
        File file = new File(fileName);
        if (file.exists()) {
            Log.d(TAG, "创建单个文件" + fileName + "失败，目标文件已存在！");
            file.delete();
        }
        if (fileName.endsWith(File.separator)) {
            Log.d(TAG,"创建单个文件" + fileName + "失败，目标文件不能为目录！");
            return false;
        }
        //判断目标文件所在的目录是否存在
        if (!file.getParentFile().exists()) {
            //如果目标文件所在的目录不存在，则创建父目录
            Log.d(TAG,"目标文件所在目录不存在，准备创建它！");
            if (!file.getParentFile().mkdirs()) {
                System.out.println("创建目标文件所在目录失败！");
                return false;
            }
        }
        //创建目标文件
        try {
            if (file.createNewFile()) {
                Log.d(TAG,"创建单个文件" + fileName + "成功！");
                bos_h264 = new BufferedOutputStream(new FileOutputStream(file));
                return true;
            } else {
                Log.d(TAG,"创建单个文件" + fileName + "失败！");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,"创建单个文件" + fileName + "失败！" + e.getMessage());
            return false;
        }
    }
}
