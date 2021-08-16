package com.camera.sdk;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AudioHelper {
    private static final String TAG = "AudioHelper";
    private AudioRecord audioRecord;
    private int mChannelCount;
    private int mSampleRate;
    private int pcmFormat;
    private String mAudioRawPath;
    private byte[] audioBuf;


    private Thread workThread;
    private volatile boolean loop = false;
    private BufferedOutputStream bos_pcm;
    private CallbackFactory.AudioCaptureCallback mAudioCaptureCallback;  //内部回调:用于将原始数据添加到队列中,编码时会用到
    private long presentTimeUs;
    private long pts;

    //1.
    public void initAudioCapture(int sampleRate, int channelConfig, String audioRawPath) {
        LogUtil.d(TAG,"hello prepareAudioRecord start==");

        mAudioRawPath = audioRawPath;
        mSampleRate = sampleRate;
        mChannelCount = channelConfig == AudioFormat.CHANNEL_IN_STEREO ? 2 : 1;
        pcmFormat = 16;
        LogUtil.d(TAG, "hello 音频采集参数配置:aSampleRate: " + mSampleRate + " aChannelCount: " +
                mChannelCount + "   channelConfig: " + channelConfig);
        //音频采样率，44100是目前的标准，但是某些设备仍然支持22050,16000,11025,8000,4000
//        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        try {
            // stereo 立体声,mono单声道
            final int min_buffer_size = 2 * AudioRecord.getMinBufferSize(mSampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT, min_buffer_size);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                audioRecord = null;
                LogUtil.e(TAG, "error: initialized the mic failed");
            }
            LogUtil.d(TAG,"hello min_buffer_size= " + min_buffer_size);
            int buffSize = min_buffer_size;
            audioBuf = new byte[buffSize];


        } catch (final Exception e) {
            LogUtil.e(TAG, "AudioThread#run", e);
        }
    }


    /**
     * 2.开始录音
     */
    public void startAudioCapture() {
        LogUtil.d(TAG,"hello startAudioCapture start==");
        if(loop)
            return;
        workThread = new Thread() {
            @Override
            public void run() {

                if (audioRecord != null) {
                    audioRecord.startRecording();
                }
                presentTimeUs = System.nanoTime() / 1000;
                while (loop) {
                    //读取音频数据保存到audioBuf中
                    int size = audioRecord.read(audioBuf,0, audioBuf.length);
                    if (size > 0) {
                        LogUtil.d(TAG, "hello audio data size 录音字节数:" + size);
                        if (mAudioCaptureCallback != null) {
                            pts = System.nanoTime() / 1000 - presentTimeUs;
                            mAudioCaptureCallback.onRawData(audioBuf, pts); //原始音频数据(PCM)回调函数
                        }
                        if (bos_pcm != null) {
                            LogUtil.d(TAG,"hello bos_pcm is not null");
                            try {
                                long beginMS = System.currentTimeMillis();
                                bos_pcm.write(audioBuf, 0, audioBuf.length);
                                LogUtil.e(TAG,"hello2 write PCM  takes " + (System.currentTimeMillis()-beginMS) + " ms");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                LogUtil.d(TAG, "hello======Audio录音线程退出...");
            }
        };

        loop = true;
        workThread.start();
    }

    /**
     * 3.
     */
    public void stopAudioCapture(){
        LogUtil.d(TAG,"hello stopAudioThread start==");
        //关闭文件流
        closeStream();
        loop = false;
    }


    /**
     * 4.
     */
    public void uninitAudioCapture() {
        //1.停止线程
        stopAudioCapture();
        //2.释放audioRecord资源
        if(audioRecord != null){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        mAudioCaptureCallback = null;
    }

    public void closeStream(){
        if(bos_pcm != null){
            try {
                bos_pcm.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        bos_pcm = null;
        //将pcm文件转成wav文件
        makePcmFileToWavFile(mAudioRawPath,mAudioRawPath.replace(".pcm",".wav"),
                false, mChannelCount,mSampleRate,16);
    }

    public void openStream(){
        createFile(mAudioRawPath);
    }

    public void setCallback(CallbackFactory.AudioCaptureCallback callback) {
        this.mAudioCaptureCallback = callback;
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
                if(bos_pcm == null){
                    LogUtil.d(TAG,"hello bos_pcm start==");
                    bos_pcm = new BufferedOutputStream(new FileOutputStream(file));
                    if(bos_pcm == null){
                        LogUtil.e(TAG,"error bos_pcm is null");
                    }
                }
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

    //
    public static boolean makePcmFileToWavFile(String srcPcmPath, String destWavPath, boolean deletePcmFile,
                                               int numChannels, int sampleRate, int bitPerSample) {
        File file = new File(srcPcmPath); //原始pcm文件
        if (!file.exists()) {
            return false;
        }

        int totalSize = (int) file.length();
        WaveHeader header = new WaveHeader();
        header.ChunkSize = totalSize + 36; //从下个地址到文件尾的总字节数
        header.AudioFormat = 1; // 1为线性pcm编码,其它数字表示某种形式的压缩
        header.NumChannels = (short) numChannels; // 单通道 1，双通道 2
        header.SampleRate = sampleRate; // 采样率
        header.BitsPerSample = bitPerSample; // 每样本数据位数,16 位
        header.Subchunk2Size = totalSize; // 音频数据的长度

        byte[] h;
        try {
            h = header.getHeader();
        } catch (IOException e) {
            return false;
        }

        if (h.length != 44) { // WAV标准，头部应该是44字节,如果不是44个字节则不进行转换文件
            return false;
        }

        File destFile = new File(destWavPath);
        if (destFile.exists()) {
            destFile.delete();
        }

        InputStream inStream = null;
        OutputStream outStream = null;
        byte[] buffer = new byte[8196];
        try {
            outStream = new BufferedOutputStream(new FileOutputStream(destWavPath));
            outStream.write(h);                                             //1.写wav头
            inStream = new BufferedInputStream(new FileInputStream(file));  //2.读取pcm文件
            int size = inStream.read(buffer);                               //3.将数据存储在buffer中等待写文件
            while (size != -1) {
                outStream.write(buffer, 0, size);                      //4.写wav文件,
                size = inStream.read(buffer);
            }
        } catch (IOException ioe) {
            return false;
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    // ignored
                }
            }
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
        if (deletePcmFile) {
            file.delete();
        }
        return true;
    }
}
