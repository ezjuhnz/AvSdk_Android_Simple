package com.camera.sdk;

import android.media.AudioFormat;
import android.media.AudioRecord;
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

public class AudioEncoder {
    private static final String TAG = "AudioEncoder";
    private static int audioInc = 1;
    private int mSampleRate;
    private int mChannelConfig;
    private int mAudioRate;
    private String mAudioMime;
    private String mAudioEncPath;
    private int mChannelCount;
    private String defaultMime = MediaFormat.MIMETYPE_AUDIO_AAC;

    private MediaCodec.BufferInfo mBufferInfo;
    private MediaCodecInfo audioCodecInfo;

    private MediaCodec mAudioEnc;

    private Object lock = new Object();
    private boolean encodeEnd;
    private long presentTimeUS = 0;
    private int TIMEOUT_USEC = 1000;
    private boolean isStarted;

    private CallbackFactory.AudioEncCallback mAudioEncCallback;
    private BufferedOutputStream bos_aac;
    private MediaFormat newformat;

    public AudioEncoder(){

    }

    /**
     * 1.
     * @param sampleRate
     * @param channelConfig
     * @param bitRate
     * @param audioFmtIdx
     * @param audioEncPath
     */
    public void initAudioEncoder(int sampleRate, int channelConfig, int bitRate, int audioFmtIdx,String audioEncPath){
        mSampleRate = sampleRate;
        mChannelConfig = channelConfig;
        mAudioRate = bitRate;
        mAudioMime = Constants.audioEncFormats[audioFmtIdx];
        mAudioEncPath = audioEncPath;
        mChannelCount = (channelConfig == AudioFormat.CHANNEL_IN_STEREO) ? 2: 1 ;
        //判断手机是否支持当前编码格式,如果不支持,则使用默认的编码格式
        LogUtil.d(TAG,"hello audio isCodecSupport start==");
        boolean isSupport = isCodecSupport(mAudioMime);
        if(!isSupport){
            LogUtil.e(TAG,"error current audio encoder format is not supported:" + mAudioMime);
            mAudioMime = defaultMime;
            LogUtil.e(TAG,"we will choose an alternative for you: " + mAudioMime);
        }
        LogUtil.d(TAG,"=====hello 音频编码器配置参数:=====");
        LogUtil.d(TAG,"sampleRate,channelConfig,channelCount,mAudioRate= " +
                sampleRate + "," + channelConfig + "," + mChannelCount + "," + mAudioRate);
        mBufferInfo = new MediaCodec.BufferInfo();

        audioCodecInfo = selectCodec(mAudioMime);
        if (audioCodecInfo == null) {
            return;
        }
        MediaFormat audioFormat = MediaFormat.createAudioFormat(mAudioMime, mSampleRate, mChannelCount);      //创建音频的格式,参数 MIME,采样率,通道数
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC); //编码方式
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mAudioRate); //比特率,这是编码数据的比特率;注意与原始音频数据的比特率区分
        int maxSize = 2*AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, AudioFormat.ENCODING_PCM_16BIT);
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxSize);
        //1.创建音频编码器,用于将PCM编码成AAC
        try {
            mAudioEnc = MediaCodec.createEncoderByType(mAudioMime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAudioEnc.configure(audioFormat, null, null,
                MediaCodec.CONFIGURE_FLAG_ENCODE);

        LogUtil.d(TAG,"hello AudioEncoder end==");
    }

    /**
     * 4.
     */
    public void uninitAudioEncoder(){
        stopAudioEncoder();
        if(mAudioEnc != null){
            mAudioEnc.stop();
            mAudioEnc.release();
            mAudioEnc = null;
        }
        LogUtil.d(TAG,"hello stopAudioEncoder end==");
    }

    /**
     * 2.启动音频编码器并开始线程
     */
    public void startAudioEncoder(){
        LogUtil.d(TAG,"hello startAudioEncoder start==");
        presentTimeUS = System.nanoTime();

        mAudioEnc.start();
        isStarted = true;
    }

    /**
     * 3.结束音频编码器并挂起线程
     */
    public void stopAudioEncoder(){
        LogUtil.d(TAG,"hello stopAudioEncoder start==");
        synchronized (lock){
            closeStream();
            encodeEnd = true;
        }
    }

    public void openStream(){
        createFile(mAudioEncPath);
    }

    public void closeStream(){
        LogUtil.d(TAG,"hello raw audio closeStream start==");
        if(bos_aac != null){
            try {
                bos_aac.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        bos_aac = null;
        LogUtil.d(TAG,"hello raw audio closeStream end==");
    }


    private void audioEncoding(byte[] input){
        LogUtil.d(TAG,"hello encodeAudioData start==");
        if(presentTimeUS == 0){
            presentTimeUS = System.nanoTime();
        }
        Long beginMs = System.currentTimeMillis();
        long pts;
        try {
            int inputBufferIndex = mAudioEnc.dequeueInputBuffer(-1);
            LogUtil.d(TAG,"hello get input buffer takes " + (System.currentTimeMillis()-beginMs) + " ms");
            if (inputBufferIndex >= 0) { //输入缓冲区有效
                ByteBuffer inputBuffer = mAudioEnc.getInputBuffer(inputBufferIndex);

                LogUtil.d(TAG,"hello inputBuffer.capacity= " + inputBuffer.capacity());
                inputBuffer.clear();
                LogUtil.d(TAG,"hello input length=" + input.length);
                if(inputBuffer.capacity() < input.length){
                    inputBuffer.limit(input.length);
                }
                //往输入缓冲区写入数据
                inputBuffer.put(input); //如果input大小超过4096个字节,则报java.nio.BufferOverflowException
                //计算pts，这个值是一定要设置的
                pts = (System.nanoTime() - presentTimeUS) / 1000 ;
//                pts = audioInc++*(1024*2*1000000/mSampleRate);
                if (encodeEnd) {
                    //结束时，发送结束标志，在编码完成后结束
                    LogUtil.d(TAG, "hello send Audio Encoder BUFFER_FLAG_END_OF_STREAM====");
                    mAudioEnc.queueInputBuffer(inputBufferIndex, 0, input.length,
                            pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    //将缓冲区入队
                    mAudioEnc.queueInputBuffer(inputBufferIndex, 0, input.length,
                            pts, 0);
                }
            }

            //拿到输出缓冲区的索引
            long outMs = System.currentTimeMillis();
            int outputBufferIndex = mAudioEnc.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            LogUtil.d(TAG,"hello get output buffer takes " + (System.currentTimeMillis()-outMs) + " ms");
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                LogUtil.d(TAG, "hello Audio===INFO_OUTPUT_FORMAT_CHANGED===");
                newformat = mAudioEnc.getOutputFormat(); // API >= 16
                mAudioEncCallback.onMediaFormat(Constants.TRACK_AUDIO, newformat);
            }
            while (outputBufferIndex >= 0) { //>= 0
                //数据已经编码成AAC格式; outputBuffer保存的就是AAC数据
                ByteBuffer outputBuffer = mAudioEnc.getOutputBuffer(outputBufferIndex);
                if(outputBuffer == null){
                    throw new RuntimeException("error outputBuffer " +outputBufferIndex + " is null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    LogUtil.d(TAG, "hello Audio====drain:BUFFER_FLAG_CODEC_CONFIG===");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (null != mAudioEncCallback) { //将编码好的音频数据通过回调函数返回
//                        mBufferInfo.presentationTimeUs = pts;
                        mAudioEncCallback.onAudioData(Constants.TRACK_AUDIO, outputBuffer, mBufferInfo);
                    }
                    LogUtil.d(TAG,"hello output buffer timestamp= " + mBufferInfo.presentationTimeUs);
                    //写文件
                    LogUtil.d(TAG,"hello aacAssemble==");
                    byte[] outData = aacAssemble(outputBuffer, mBufferInfo, mSampleRate);
                    //TODO:将数据返回给客户端
                    mAudioEncCallback.onOuterAudioData(outData);
                    if(bos_aac != null){
                        LogUtil.d(TAG,"hello write aac length=" + outData.length);
                        bos_aac.write(outData, 0, outData.length);
                    }
                }
                //释放资源
                outputBuffer.position(mBufferInfo.offset);
                mAudioEnc.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mAudioEnc.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if(encodeEnd){
                    isStarted = false;
                }
            }

        } catch (Exception t) {
            LogUtil.e(TAG, "hello encodeAudioData=====error: " + t.toString());
        }
        LogUtil.d(TAG,"hello encode audio data takes " + (System.currentTimeMillis()-beginMs) + " ms");
    }

    public void setCallback(CallbackFactory.AudioEncCallback callback){
        LogUtil.d(TAG,"hello setAudioEncCallback start==");
        mAudioEncCallback = callback;
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

    //
    private MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    public void onAudioEncodeData(byte[] data){
        LogUtil.d(TAG,"hello onAudioCaptureData length=" + data.length);
        if(isStarted){
            audioEncoding(data);
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
                bos_aac = new BufferedOutputStream(new FileOutputStream(file));
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

    public static byte[] aacAssemble(ByteBuffer outBuffer, MediaCodec.BufferInfo aBufferInfo, int sampleRate) {
        LogUtil.d(TAG,"hello writeAACFile start==");
        ByteBuffer outputBuffer;
        int outPacketSize;
        outputBuffer = outBuffer;
        int outBitsSize = aBufferInfo.size;
        outPacketSize = outBitsSize + 7; // 7 is ADTS size

        outputBuffer.position(aBufferInfo.offset);
        outputBuffer.limit(aBufferInfo.offset + outBitsSize);

        //添加ADTS头
        byte[] outData = new byte[outPacketSize];
        addADTStoPacket(outData, outPacketSize,sampleRate);

        outputBuffer.get(outData, 7, outBitsSize);
        outputBuffer.position(aBufferInfo.offset);

        return outData;
    }

    private static void addADTStoPacket(byte[] packet, int packetLen,int sampleRate) {
        LogUtil.d(TAG,"hello entering addADTStoPacket==");

        int profile = 2;  //AAC LC
        /*freqIdx
        0: 96000 Hz
        1: 88200 Hz
        2: 64000 Hz
        3: 48000 Hz
        4: 44100 Hz
        5: 32000 Hz
        6: 24000 Hz
        7: 22050 Hz
        8: 16000 Hz
        9: 12000 Hz
        10: 11025 Hz
        11: 8000 Hz
        12: 7350 Hz
         */
        int freqIdx = getFreqIdx(sampleRate);  //44.1KHz
        LogUtil.e(TAG,"hello freqIdx= " + freqIdx);
        int chanCfg = 1;  //CPE
        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF1;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    private static int getFreqIdx(int sampleRate){
        int count = 0;
        for (int rate : Constants.ADTSFreqArray) {
            if(rate == sampleRate){
                break;
            }
            count++;
        }
        LogUtil.d(TAG,"hello count= " + count);
        return count;
    }

    public static class RawAudioData{
        byte[] data;
        long pts;
        public RawAudioData(byte[] data, long pts){
            this.data = data;
            this.pts = pts;
        }
    }
}


