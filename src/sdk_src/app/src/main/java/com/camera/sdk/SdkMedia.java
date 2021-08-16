package com.camera.sdk;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.SurfaceView;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Time: 2021/3/2
 * Author: zhongjunhong
 * LastEdit: 2021/3/2
 * Description:对外提供的接口,主要功能有:
 * 1.音视频参数配置
 * 2.摄像头数据采集:调用者可通过回调获取原始摄像头数据
 * 3.屏幕数据采集 :调用者可通过回调获取原始屏幕数据
 * 4.声音数据采集 :调用者可通过回调获取原始音频数据
 * 5.视频编码 : 调用者可通过回调获取编码后的视频数据
 * 6.音频编码 : 调用者可通过回调获取编码后的音频数据
 * 7.音频,视频混合
 */
public class SdkMedia {
    String[] mPermissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SYSTEM_ALERT_WINDOW
    };

    private static SdkMedia _instance;
    private static final String TAG = "SdkMedia";

    public ConfigData avConfig;              //音视频参数配置
    public static Context mContext = null;
    public static int mDeviceIdx;

    private Camera1Helper camera1Helper;
    private ScreenHelper screenHelper;
    private AudioHelper audioHelper;

    private VideoEncoder cameraEncoder;
    private VideoEncoder screenEncoder;
    private AudioEncoder audioEncoder;
    private AVmediaMuxer cameraMuxer;
    private AVmediaMuxer screenMuxer;
    private SurfaceView mSurfaceView;

    private CallbackFactory.OuterCallback mOuterCallback;

    /*
    1.获取SdkMedia单例
     */
    public static synchronized SdkMedia getInstance()
    {
        //开启Log
        LogUtil.d(TAG,"---hello getInstance---");
        if(null == _instance)
        {
            _instance = new SdkMedia();
        }
        return _instance;
    }

    /*
    2.设置上下文环境
     */
    public void initSDK(ConfigData params) {
        LogUtil.d(TAG,"---hello initSDK---");

        avConfig = params;
        if(avConfig != null){
            mSurfaceView = params.mSurfaceView;
            mContext = params.mContext;
            //TODO:先做权限判断
            PermissionUtil.requestAllPermission(mPermissions,(Activity) mContext);

            LogUtil.d(TAG,"---hello applyAVConfig 配置音视频参数---");
            avConfig = params;
            LogUtil.d(TAG,"------ 音频参数配置如下:");
            LogUtil.d(TAG,"采样率 sampleRate= " + avConfig.mSampleRate);
            LogUtil.d(TAG,"通道类型 channelConfig= " + avConfig.channelConfig);
            LogUtil.d(TAG,"码率 audioBitRate= " + avConfig.audioBitRate);
            LogUtil.d(TAG,"编码格式 audioEncFormat=" + avConfig.audioEncFormat);

            LogUtil.d(TAG,"------ 视频参数配置如下:");
            LogUtil.d(TAG,"分辨率 width*height= " + avConfig.mVideoWidth + "*" + avConfig.mVideoHeight);
            LogUtil.d(TAG,"帧率 fps= " + avConfig.mFps);
            LogUtil.d(TAG,"码率 videoBitRate =" + avConfig.mVideoBitRate);

            LogUtil.d(TAG,"------ 文件配置如下:");
            LogUtil.d(TAG,"camera mp4 path= " + avConfig.mCameraMuxPath);
            LogUtil.d(TAG,"camera h264 path= " + avConfig.mCameraEncPath);
            LogUtil.d(TAG,"screen mp4 path= " + avConfig.mScreenMuxPath);
            LogUtil.d(TAG,"screen h264 path= " + avConfig.mScreenEncPath);
        }
        if(cameraMuxer == null){
            if(avConfig.mCameraMuxPath != null && !avConfig.mCameraMuxPath.trim().equals("")){
                cameraMuxer = new AVmediaMuxer();
                LogUtil.d(TAG,"hello avConfig.cameraMuxPath= " + avConfig.mCameraMuxPath);
                cameraMuxer.initMuxer(avConfig.mCameraMuxPath);
            }
        }

        if(screenMuxer == null){
            if(avConfig.mScreenMuxPath != null && !avConfig.mScreenMuxPath.trim().equals("")){
                screenMuxer = new AVmediaMuxer();
                LogUtil.d(TAG,"hello avConfig.screenMuxPath= " + avConfig.mScreenMuxPath);
                screenMuxer.initMuxer(avConfig.mScreenMuxPath);
            }
        }

        if(cameraEncoder == null){
            cameraEncoder = new VideoEncoder();
            LogUtil.d(TAG,"hello avConfig.mCameraEncPath= " + avConfig.mCameraEncPath);
            cameraEncoder.initVideoEncoder(avConfig.mVideoWidth, avConfig.mVideoHeight, avConfig.mVideoEncFormat,avConfig.mFps, avConfig.mVideoBitRate,
                    avConfig.mKeyFrameInterval, avConfig.mCameraEncPath);
            cameraEncoder.setVideoEncCallback(mCameraEncCallback); //视频编码数据回调: 用于获取编码好的视频数据
        }

        //启动视频编码器-Screen
        if(screenEncoder == null){
            LogUtil.d(TAG,"hello new screenEncoder==");
            screenEncoder = new VideoEncoder();
            screenEncoder.initVideoEncoder(avConfig.mVideoWidth, avConfig.mVideoHeight, avConfig.mVideoEncFormat,avConfig.mFps, avConfig.mVideoBitRate,
                    avConfig.mKeyFrameInterval, avConfig.mScreenEncPath);
            screenEncoder.setVideoEncCallback(mScreenEncCallback); //视频编码数据回调: 用于获取编码好的视频数据
        }

        if(audioHelper == null){
            audioHelper = new AudioHelper();
            audioHelper.initAudioCapture(avConfig.mSampleRate, avConfig.channelConfig, avConfig.audioRawPath);
            audioHelper.setCallback(mAudioRawCallback);
        }

        if(audioEncoder == null){
            audioEncoder = new AudioEncoder();
            audioEncoder.initAudioEncoder(avConfig.mSampleRate, avConfig.channelConfig, avConfig.audioBitRate,
                    avConfig.audioEncFormat,avConfig.audioEncPath);
            audioEncoder.setCallback(mAudioEncCallback);
        }

        if(screenHelper == null){
            screenHelper = new ScreenHelper();
            screenHelper.setContext(mContext);
            screenHelper.setCallback(mScreenRawCallback);
        }

        if(camera1Helper == null){
            camera1Helper = new Camera1Helper();
            camera1Helper.setContext(mContext);
            camera1Helper.initCameraCapture(mSurfaceView, avConfig.mVideoWidth, avConfig.mVideoHeight, avConfig.mFps,avConfig.mCameraEncPath);
            camera1Helper.setCallback(mVideoCaptureCallback); //原始数据:摄像头
        }
    }

    /**
     * 3.初始化摄像头采集需要的view
     * @param view
     */
    public void initView(SurfaceView view){
        mSurfaceView = view;
    }

    /*
    4.保存音视频配置参数
     */
    public void applyAVConfig(ConfigData params)
    {

    }


    /**
     * 5.设置数据回调函数,包括
     * 1.原始音频数据回调
     * 2.原始摄像头数据回调
     * 3.原始屏幕数据回调
     * 4.编码后的摄像头数据回调
     * 5.编码后的屏幕数据回调
     * @param callback
     */
    public void setDataCallback(CallbackFactory.OuterCallback callback)
    {
        LogUtil.d(TAG,"hello setRawDataCallback==");
        mOuterCallback = callback;
    }

    /*
    6.开始采集:根据设备ID,采集摄像头或屏幕数据
     */
    public void startVideoCapture(int deviceIdx) {
        LogUtil.d(TAG,"hello sdk startPreview==");
        mDeviceIdx = deviceIdx;
        if(deviceIdx == ConfigData.CameraIdx){ //1.采集摄像头数据
            //1.根据配置的分辨率打开摄像机
            camera1Helper.startCameraCapture();
        } else if(deviceIdx == ConfigData.ScreenIdx){ //2.采集屏幕数据
            screenHelper.initScreenCapture(avConfig.mVideoWidth, avConfig.mVideoHeight,avConfig.mScreenEncPath);
        }
    }

    /**
     * 7.采集声音
     */
    public void startAudioCapture(){
        LogUtil.d(TAG,"hello startAudioGather start==");
        if(audioHelper != null){
            audioHelper.startAudioCapture();
        }
    }

    /**
     * 8.视频编码
     */
    public void startVideoEncoder(){
        LogUtil.d(TAG,"hello startVideoEncoder start==");
        //启动视频编码器-Camera
        if(cameraEncoder != null){
            cameraEncoder.startVideoEncoder();
        }

        if(screenEncoder != null){
            screenEncoder.startVideoEncoder();
        }

    }

    /**
     * 9.音频编码
     */
    public void startAudioEncoder(){
        LogUtil.d(TAG,"hello startAudioEncoder start==");
        if(audioEncoder != null){
            audioEncoder.startAudioEncoder();
        }
    }

    /**
     * 10.开始封装音频和视频
     */
    public void startMuxer(){
        LogUtil.d(TAG,"hello startMuxer start==");
        //初始化混合器
        if(cameraMuxer != null ){
            cameraMuxer.startMuxer();
        }
        if(screenMuxer != null ){
            screenMuxer.startMuxer();
        }
    }

    /**
     * 11.停止声音采集
     */
    public void stopAudioCapture(){
        LogUtil.d(TAG,"hello stopAudioGahter start==");
        if(audioHelper != null){
            audioHelper.stopAudioCapture();
        }
    }

    /**
     * 12.停止视频编码
     */
    public void stopVideoEncoder(){
        LogUtil.d(TAG,"hello stopVideoEncoder start==");
        if(cameraEncoder != null){
            cameraEncoder.stopVideoEncoder();
            cameraEncoder = null;
        }
        if(screenEncoder != null){
            screenEncoder.stopVideoEncoder();
            screenEncoder = null;
        }
    }

    /**
     * 13.停止音频编码
     */
    public void stopAudioEncoder(){
        LogUtil.d(TAG,"hello stopAudioEncode start==");
        if(audioEncoder != null){
            audioEncoder.stopAudioEncoder(); //停止音频编码线程
        }
    }

    /**
     * 14.停止封装音频和视频
     */
    public void stopMuxer(){
        LogUtil.d(TAG,"hello stopMuxer start==");
        if(cameraMuxer != null){
            cameraMuxer.stopMuxer();
            cameraMuxer = null;
        }
        if(screenMuxer != null){
            screenMuxer.stopMuxer();
            screenMuxer = null;
        }
    }

    /**
     * 15.停止采集摄像头数据和屏幕数据
     */
    public void stopVideoCapture(int deviceId){
        LogUtil.d(TAG,"hello sdk stopPreview==");
        if(deviceId == ConfigData.CameraIdx){
            if(camera1Helper != null){
                camera1Helper.stopCameraCapture();    //停止采集摄像机数据
            }
        } else if(deviceId == ConfigData.ScreenIdx){
            screenHelper.stopScreenCapture();     //停止采集屏幕数据
        }
    }

    /**
     * 16.开始采集屏幕数据,由于屏幕采集需要与用户交互,经用户同意后方可进行采集;
     * 所以采集分两步:一是发起请求,二是进行采集
     * @param resultCode
     * @param data
     */
    public void startScreenCapture(int resultCode, Intent data){
        screenHelper.startScreenCapture(resultCode, data);
    }

    public void switchCamera()
    {

    }

    public void setWaterMark(int waterMarkId, int start_x, int start_y){
        if(mContext == null){
            LogUtil.e(TAG,"error mContext is null");
        }
        if(waterMarkId <= 0){
            return ;
        }
        InputStream in = mContext.getResources().openRawResource(waterMarkId);
        Bitmap bm = BitmapFactory.decodeStream(in);
        int wmWidth = bm.getWidth();
        int wmHeight = bm.getHeight();
        if(wmWidth >= avConfig.mVideoWidth || wmHeight >= avConfig.mVideoHeight){
            LogUtil.e(TAG,"hello 水印图片width,height= " + wmWidth + "," + wmHeight);
            LogUtil.e(TAG," 不能大于原始图片的宽高: " + avConfig.mVideoWidth + "," + avConfig.mVideoHeight);
        }

        int[] pixels = new int[wmWidth*wmHeight];
        bm.getPixels(pixels,0, wmWidth,0,0, wmWidth, wmHeight);
        //将int[]转成byte[]
        //argb转yuv
        byte[] yuvBuffer = new byte[wmWidth* wmHeight * 3/2];
        yuvBuffer = ImageFormatUtil.colorconvertRGB_YUV_NV21(pixels, wmWidth, wmHeight);

        if(camera1Helper != null){
            camera1Helper.setWaterMark(yuvBuffer, start_x, start_y, wmWidth, wmHeight);
        }
        if(screenHelper != null){
            screenHelper.setWaterMark(yuvBuffer,  start_x, start_y, wmWidth, wmHeight);
        }
    }

    public void uninitSdk(){

        if(audioEncoder != null){
            LogUtil.d(TAG,"junhong audioEncoder uninitAudioEncoder 555 start==");
            audioEncoder.uninitAudioEncoder();
            audioEncoder = null;
            LogUtil.d(TAG,"junhong audioEncoder uninitAudioEncoder 555 end==");
        }

        if(audioHelper != null){
            LogUtil.d(TAG,"junhong audioHelper uninitAudioCapture 666 start==");
            audioHelper.uninitAudioCapture();
            audioHelper = null;
            LogUtil.d(TAG,"junhong audioHelper uninitAudioCapture 666 end==");
        }

        if(screenHelper != null){
            LogUtil.d(TAG,"junhong screenHelper uninitScreenCapture 777 start==");
            screenHelper.uninitScreenCapture();
            screenHelper = null;
            LogUtil.d(TAG,"junhong screenHelper uninitScreenCapture 777 end==");
        }
        if(camera1Helper != null){
            LogUtil.d(TAG,"junhong camera1Helper uninitCameraCapture 888 start==");
            camera1Helper.uninitCameraCapture();
            camera1Helper = null;
            LogUtil.d(TAG,"junhong camera1Helper uninitCameraCapture 888 end==");
        }

        if(cameraEncoder != null){
            LogUtil.d(TAG,"junhong cameraEncoder uninitVideoEncoder 333 start==");
            cameraEncoder.uninitVideoEncoder();
            cameraEncoder = null;
            LogUtil.d(TAG,"junhong cameraEncoder uninitVideoEncoder 333 end==");
        }

        if(screenEncoder != null){
            LogUtil.d(TAG,"junhong screenEncoder uninitVideoEncoder 444 start==");
            screenEncoder.uninitVideoEncoder();
            screenEncoder = null;
            LogUtil.d(TAG,"junhong screenEncoder uninitVideoEncoder 444 start==");
        }

        //释放资源,依次释放合成器,编码器,采集
        if(cameraMuxer != null){
            LogUtil.d(TAG,"junhong cameraMuxer uninitMuxer 111 start==");
            cameraMuxer.uninitMuxer();
            cameraMuxer = null;
            LogUtil.d(TAG,"junhong cameraMuxer uninitMuxer 111 end==");
        }

        if(screenMuxer != null){
            LogUtil.d(TAG,"junhong screenMuxer uninitMuxer 222 start==");
            screenMuxer.uninitMuxer();
            screenMuxer = null;
            LogUtil.d(TAG,"junhong screenMuxer uninitMuxer 222 end==");
        }
    }


    /*************************回调函数定义:内部************************/
    //1.Camera原始数据回调,该回调由Camera1Helper调用,返回采集的摄像头原始数据
    private CallbackFactory.VideoCaptureCallback mVideoCaptureCallback = new CallbackFactory.VideoCaptureCallback(){

        @Override
        public void onRawData(byte[] data) {
            LogUtil.d(TAG,"hello VideoCaptureCallback onRawData==");
            if(data != null){
                LogUtil.e(TAG,"hello raw video data length= " + data.length);
                if(cameraEncoder != null){
                    //将摄像头 raw data传递给视频编码器
                    cameraEncoder.onVideoEncodeData(data);
                }
                //将原始数据返回给客户端
                mOuterCallback.onCameraRawData(data);
            }
        }
    };

    //2.摄像头编码数据回调,该回调由AvEncoder中的CameraEncoder调用,返回编码好的摄像头数据
    private CallbackFactory.VideoEncCallback mCameraEncCallback = new CallbackFactory.VideoEncCallback(){

        @Override
        public void onVideoData(int trackIndex, ByteBuffer outBuf, MediaCodec.BufferInfo bufferInfo,MediaFormat mediaFormat) {
            LogUtil.d(TAG,"hello CameraEncCallback onVideoData data length=" + outBuf.capacity());
            //将编码好的视频数据 传递给混合器
            if(cameraMuxer != null){
                cameraMuxer.onVideoMuxData(trackIndex, outBuf, bufferInfo, mediaFormat);
            }
        }

        @Override
        public void onOuterVideoData(byte[] data) {
            LogUtil.d(TAG,"hello camera onOuterVideoData length=" + data.length);
            //将编码好的数据传递给客户端
            mOuterCallback.onCameraEncData(data);
        }

        @Override
        public void onMediaFormat(int trackIndex, MediaFormat mediaFormat) {
            LogUtil.d(TAG,"hello camera enc onMediaFormat==");
            //该回调只调用一次,用来通知混合器,当前数据时音频格式还是视频格式
            if(cameraMuxer != null){
                cameraMuxer.onMediaFormat(trackIndex, mediaFormat);
            }
        }
    };
    //3.Screen原始数据回调,该回调由ScreenHelper调用,返回采集的屏幕原始数据
    private CallbackFactory.ScreenRawCallback mScreenRawCallback = new CallbackFactory.ScreenRawCallback(){

        @Override
        public void onRawData(byte[] data) {
            LogUtil.d(TAG,"hello ScreenRawCallback onRawData==");
            //将屏幕 raw data传递给Screen编码器
            if(data != null){
                LogUtil.e(TAG,"hello raw video data length= " + data.length);
                if(screenEncoder != null){
                    //将摄像头 raw data传递给视频编码器
                    screenEncoder.onVideoEncodeData(data);
                }
                //将原始数据返回给客户端
                mOuterCallback.onScreenRawData(data);
            }
        }
    };

    //4.Screen编码数据回调,该回调由AvEncoder中的ScreenEncoder调用,返回编码好的屏幕数据
    private CallbackFactory.VideoEncCallback mScreenEncCallback = new CallbackFactory.VideoEncCallback(){

        @Override
        public void onVideoData(int trackIndex, ByteBuffer outBuf, MediaCodec.BufferInfo bufferInfo, MediaFormat mediaFormat) {
            LogUtil.d(TAG,"hello ScreenEncCallback onVideoData=");
            //将屏幕 encoded data传递给混合器
            if(screenMuxer != null){
                screenMuxer.onVideoMuxData(trackIndex, outBuf, bufferInfo,mediaFormat);
            }
            LogUtil.d(TAG,"hello video encode data= " + outBuf.capacity());
            ByteBuffer newOutBuf;
            newOutBuf = outBuf.duplicate();
            newOutBuf.clear();
            LogUtil.d(TAG,"hello video newOutBuf.capacity, remaining= " + newOutBuf.capacity() + "," + newOutBuf.remaining());
            byte[] result = new byte[newOutBuf.capacity()];
            newOutBuf.get(result,0, newOutBuf.capacity());
        }

        @Override
        public void onOuterVideoData(byte[] data) {
            LogUtil.d(TAG,"hello screen onOuterVideoData length= " + data.length);
            mOuterCallback.onScreenEncData(data);
        }

        @Override
        public void onMediaFormat(int trackIndex, MediaFormat mediaFormat) {
            //该回调只调用一次,用来通知混合器,当前数据时音频格式还是视频格式
            LogUtil.d(TAG,"hello screen encoder onMediaFormat trackIndex=" + trackIndex);
            if(screenMuxer != null){
                LogUtil.d(TAG,"hello screen encoder onMediaFormat==");
                screenMuxer.onMediaFormat(trackIndex, mediaFormat);
            }
        }
    };

    //5.原始音频数据回调,该回调由AudioHelper中调用,返回采集的原始音频数据
    private CallbackFactory.AudioCaptureCallback mAudioRawCallback = new CallbackFactory.AudioCaptureCallback(){
        @Override
        public void onRawData(byte[] data, long pts) {
            LogUtil.d(TAG,"hello AudioRawCallback onRawData==");
            if(data != null){
                LogUtil.d(TAG,"hello audio raw data length= " + data.length);
                //将原始音频数据PCM 传递给音频编码器
                if(audioEncoder != null){
                    audioEncoder.onAudioEncodeData(data);
                }
                //将原始音频数据PCM返回给客户端
                mOuterCallback.onAudioRawData(data);
            }
        }
    };
    //6.编码音频数据回调,该回调由AvEncoder中的AudioEnc调用,返回编码好的音频数据
    private CallbackFactory.AudioEncCallback mAudioEncCallback = new CallbackFactory.AudioEncCallback(){

        @Override
        public void onAudioData(int trackIndex, ByteBuffer outBuf, MediaCodec.BufferInfo bufferInfo) {
            LogUtil.d(TAG,"hello AudioEncCallback onAudioData==");
            //将音频 encoded data传递给混合器
            if(cameraMuxer != null){
                cameraMuxer.onAudioMuxData(trackIndex, outBuf, bufferInfo);
            }
            if(screenMuxer != null){
                screenMuxer.onAudioMuxData(trackIndex, outBuf, bufferInfo);
            }

        }

        @Override
        public void onOuterAudioData(byte[] data) {
            LogUtil.d(TAG,"hello audio onOuterAudioData length=" + data.length);
            mOuterCallback.onAudioEncData(data);
        }

        @Override
        public void onMediaFormat(int trackIndex, MediaFormat mediaFormat) {
            LogUtil.d(TAG,"hello audio enc onMediaFormat==");
            //将数据传递给混合器
            if(cameraMuxer != null){
                cameraMuxer.onMediaFormat(trackIndex, mediaFormat);
            }
            if(screenMuxer != null){
                screenMuxer.onMediaFormat(trackIndex, mediaFormat);
            }
        }
    };

    /**
     * 是否保存原始音频文件
     * @param flag
     */
    public void saveAudioFile(boolean flag) {
        if(flag){
            if(audioHelper != null){
                audioHelper.openStream();
            }
        }else {
            if (audioHelper != null) {
                audioHelper.closeStream();
            }
        }
    }

    /**
     * 是否保存编码音频文件
     * @param flag
     */
    public void saveAudioEncFile(boolean flag){
        if(flag){
            if(audioEncoder != null){
                audioEncoder.openStream();
            }
        }else{
            if(audioEncoder != null){
                audioEncoder.closeStream();
            }
        }
    }

    /**
     * 是否保存屏幕编码文件
     * @param flag
     */
    public void saveVideoEncFile(boolean flag){
        if(flag){
            if(screenEncoder != null){
                screenEncoder.openStream();
            }
            if(cameraEncoder != null){
                cameraEncoder.openStream();
            }
        } else{
            if(screenEncoder != null){
                screenEncoder.closeStream();
            }
            if(cameraEncoder != null){
                cameraEncoder.closeStream();
            }
        }
    }

    public void setLog(boolean isShow){
        LogUtil.setLogLevel(LogUtil.DEBUG_LEVEL);
        if(isShow){
            LogUtil.showLog();
        }
    }

}
