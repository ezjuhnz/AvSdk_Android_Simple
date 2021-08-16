package com.camera.sdk;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Time: 2021/7/1
 * Author: zhongjunhong
 * LastEdit: 2021/7/1
 * Description:用于视频,音频的合成
 */
public class AVmediaMuxer {
    private final static String TAG = "AVmediaMuxer";
    private final Object lock = new Object();
    private final static int AUDIO_TRACK = 0;
    private final static int VIDEO_TRACK = 1;
    private final static int MUXER_END = 2;
    private MediaMuxer mediaMuxer;

    //缓冲传输过来的数据
    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;
    private boolean isVideoAdd;
    private boolean isAudioAdd;
    private boolean isStarted;
    private boolean muxFlag;

    private HandlerThread handlerThread;
    private Handler handler;

    public AVmediaMuxer() {

    }

    public void initMuxer(String outfile){
        LogUtil.d(TAG,"hello initMuxer outfile=" + outfile);
        try {
            createFile(outfile);
            mediaMuxer = new MediaMuxer(outfile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            Log.d(TAG,"hello muxer created==");
        }catch (IOException e){
            e.printStackTrace();
            LogUtil.e(TAG, "hello 创建媒体混合器 error: "+e.toString());
        }
        //创建HandlerThread,并启动它
        if(handlerThread == null){
            handlerThread = new HandlerThread("MuxerThread");
            handlerThread.start();
        }

        //创建Handler
        Looper looper = handlerThread.getLooper();
        handler = new Handler(looper){
            public void handleMessage(Message msg){
                LogUtil.d(TAG,"hello get msg from some where");
                if(!isStarted) return;
                MuxerData muxerData;
                switch (msg.what){
                    case AUDIO_TRACK:
                        muxerData = (MuxerData)msg.obj;
                        mediaMuxer.writeSampleData(audioTrackIndex, muxerData.byteBuf, muxerData.bufferInfo);
                        break;
                    case VIDEO_TRACK:
                        muxerData = (MuxerData)msg.obj;
                        mediaMuxer.writeSampleData(videoTrackIndex, muxerData.byteBuf, muxerData.bufferInfo);
                        break;
                    case MUXER_END:
                        synchronized (lock){
                            if(mediaMuxer != null){
                                LogUtil.d(TAG,"hello video mediaMuxer stop==" + System.currentTimeMillis());
                                mediaMuxer.stop();
                                mediaMuxer.release();
                                mediaMuxer = null;
                            }
                            isStarted = false;
                        }
                    default:
                        break;
                }
            }
        };
    }

    public void uninitMuxer(){
        stopMuxer();
        LogUtil.d(TAG,"uninitMuxer start==");
        handlerThread.quitSafely();
        handlerThread = null;
        handler = null;
        videoTrackIndex = -1;
        audioTrackIndex = -1;
        isAudioAdd = false;
        isVideoAdd = false;
        LogUtil.d(TAG,"uninitMuxer end==");
    }

    /**
     * 当视频数据到来时触发
     * @param trackIndex
     * @param outBuf
     * @param bufferInfo
     * @param mediaFormat
     */
    public void onVideoMuxData(int trackIndex, ByteBuffer outBuf, MediaCodec.BufferInfo bufferInfo,MediaFormat mediaFormat){
        LogUtil.d(TAG,"hello onVideoEncData start outBuf.length=" + outBuf.capacity());
        MuxerData muxerData = new MuxerData(trackIndex, outBuf,bufferInfo);
        if(isStarted && videoTrackIndex > -1){
            if(handler != null){
                Message msg = handler.obtainMessage(VIDEO_TRACK, muxerData);
                handler.sendMessage(msg);
            }
        }
    }

    /**
     * 当音频数据到来时触发
     * @param trackIndex
     * @param outBuf
     * @param bufferInfo
     */
    public void onAudioMuxData(int trackIndex, ByteBuffer outBuf, MediaCodec.BufferInfo bufferInfo){
        LogUtil.d(TAG,"hello onAudioEncData outBuf.length=" + outBuf.capacity());
        MuxerData muxerData = new MuxerData(trackIndex, outBuf,bufferInfo);
        if(isStarted && audioTrackIndex > -1){
            if(handler != null){
                Message msg = handler.obtainMessage(AUDIO_TRACK, muxerData);
                handler.sendMessage(msg);
            }
        }
    }

    public void startMuxer() {
        synchronized (lock){
            muxFlag = true;
            lock.notify();
        }
    }

    public void stopMuxer() {
        LogUtil.d(TAG, "hello Muxer 停止媒体混合器 start=====");
        muxFlag = false;
        if(handler != null){
            Message msg = handler.obtainMessage(MUXER_END, "mux end flag");
            handler.sendMessage(msg);
        }
        while(isStarted){

        }
    }

    /**
     * 封装需要传输的数据类型
     */
    public static class MuxerData {
        int trackIndex;
        ByteBuffer byteBuf;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuf = byteBuf;
            this.bufferInfo = bufferInfo;
        }
    }

    //通知混合器将要到来的是音频数据还是视频数据
    public void onMediaFormat(int trackIndex, MediaFormat mediaFormat) {
        LogUtil.d(TAG,"zhujiali onMediaFormat start==");
        LogUtil.e(TAG,"hello onMediaFormat Thread=" + Thread.currentThread().getId());
        if (trackIndex == Constants.TRACK_AUDIO) {
            //声音数据是共用的,但混合器是单独使用;当检测到音频数据时,混合器可能没有初始化
            if (mediaMuxer != null) {
                audioTrackIndex = mediaMuxer.addTrack(mediaFormat);
                LogUtil.d(TAG,"hello audioTrackIndex= " + audioTrackIndex);
                isAudioAdd = true;
            }
        } else if (trackIndex == Constants.TRACK_VIDEO) {
            if (mediaMuxer != null) {
                videoTrackIndex = mediaMuxer.addTrack(mediaFormat);
                LogUtil.d(TAG,"hello videoTrackIndex= " + videoTrackIndex);
                isVideoAdd = true;
            }
        }
        //muxFlag is set by client which belongs to main thread, onMediaFormat is trigger by new thread.
        //we have no idea which thread finished their work first,but in general main thread would be the first one.
        if (isAudioAdd && isVideoAdd) {
            LogUtil.d(TAG,"hello muxer is going to start");
            if(muxFlag == false){
                synchronized (lock){
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            mediaMuxer.start(); //start()方法调用必须在 addTrack() 之后, writeSampleData之前
            LogUtil.d(TAG, "hello====启动媒体混合器成功!=====");
            isStarted = true;
        }
    }


    //
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
