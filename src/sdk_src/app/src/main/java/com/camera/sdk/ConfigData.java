package com.camera.sdk;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaFormat;
import android.os.Environment;
import android.view.SurfaceView;

/*
   音视频参数配置
    */
public class ConfigData
{
    /**
     * Time: 2021/3/16
     * Author: zhongjunhong
     * LastEdit: 2021/3/16
     * Description:定义参数配置的键,包括音视频参数:帧率,码率,分辨率等
     * 1-20:视频参数,包括分辨率,帧率,码率
     * 21-40:音频参数
     * 40以上:其它参数,如路径,文件名
     */
    static class Key {

        public static final int VIDEO_FPS = 1;      //帧率
        public static final int VIDEO_BITRATE = 2;  //码率
        public static final int VIDEO_RATIO = 3;    //视频宽高比例
        public static final int VIDEO_WIDTH = 4;    //视频宽度
        public static final int VIDEO_HEIGHT = 5;   //视频高度
        public static final int IMAGE_WIDTH = 6;    //图片宽度
        public static final int IMAGE_HEIGHT = 7;   //图片高度
        public static final int PREVIEW_WIDTH = 8;  //预览宽度
        public static final int PREVIEW_HEIGHT = 9; //预览高度
        public static final int PREVIEW_FORMAT = 10; //预览格式
        public static final int OUTPUT_FORMAT = 11;
        public static final int VIDEO_QUALITY = 12;  //视频质量
        public static final int DEVICE_IDX = 13;     //采集设备,摄像机或屏幕

        public static final int AUDIO_SAMPLERATE = 21;
        public static final int AUDIO_CHANNEL = 22;
        public static final int AUDIO_BITRATE = 23;
        public static final int AUDIO_OUTPUT_FORMAT = 24;
        public static final int AUDIO_QUALITY= 25; //音频质量

        public static final int VIDEO_PATH = 41;
        public static final int AUDIO_PATH = 42;
    }
    public Context mContext;
    public SurfaceView mSurfaceView;

    public final static int CameraIdx = 0;
    public final static int ScreenIdx = 1;

    public static final String BASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/";


    /*********************视频参数**********************/
    public int mFps;           //视频帧率
    public int mVideoBitRate;  //视频码率
    public float mVideoRatio;          //宽高比16:9
    public int mVideoWidth;     //视频宽度
    public int mVideoHeight;    //视频高度
    public int mVideoEncFormat;                    //视频编码格式
    public int mKeyFrameInterval;
    public int mDeviceIdx;                   //0(默认):摄像机采集; 1:屏幕采集

    /*********************文件参数**********************/
    public String mCameraMuxPath;  //Camera采集的数据与音频数据混合后的文件名
    public String mScreenMuxPath;  //Screen采集的数据与音频数据混合后的文件名
    public String mCameraEncPath;  //Camera视频数据编码后的文件名
    public String mScreenEncPath;  //Screen视频数据编码后的文件名
    public String audioRawPath;   //原始音频数据文件名
    public String audioEncPath;   //编码后音频数据文件名

    /*********************音频参数**********************/
    public int mSampleRate;         //采样率:用于原始数据和编码数据
    public int channelConfig;        //声道,如立体声 :用于原始数据,AudioFormat.CHANNEL_IN_STEREO
    public int audioBitRate;       //码率: 用于编码
    public int audioEncFormat;       //编码格式:用于编码,MediaFormat.MIMETYPE_AUDIO_AAC


    public ConfigData(){
        mFps = 15;
        mVideoBitRate = 1000 * 1000;
        mVideoRatio = 1.778f;
        mVideoWidth = 1280;
        mVideoHeight = 720;
        mVideoEncFormat = 0;
        mKeyFrameInterval = 1;
        mDeviceIdx = 0;

        //
        mCameraMuxPath = "";
        mScreenMuxPath = "";
        mCameraEncPath = "";
        mScreenEncPath = "";
        audioRawPath = "";
        audioEncPath = "";

        mSampleRate = 44100;
        channelConfig = AudioFormat.CHANNEL_IN_MONO;
        audioBitRate = 64000;
        audioEncFormat = 0;
    }
}
