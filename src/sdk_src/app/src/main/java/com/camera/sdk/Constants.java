package com.camera.sdk;

import android.media.MediaFormat;

public class Constants {
    public static final int TRACK_VIDEO = 0;
    public static final int TRACK_AUDIO = 1;

    //视频编码类型
    public static final int VIDEO_ENC_H264 = 0;
    public static final int VIDEO_ENC_H265 = 1;

    //音频编码类型
    public static final int AUDIO_ENC_AAC = 0;
    public static final int AUDIO_ENC_AMR_NB = 1;
    public static final int AUDIO_ENC_AMR_WB = 2;

    public static final String[] videoEncFormats = {
            MediaFormat.MIMETYPE_VIDEO_AVC, //h264
            MediaFormat.MIMETYPE_VIDEO_HEVC,//h265
    };

    public static final String[] audioEncFormats = {
            MediaFormat.MIMETYPE_AUDIO_AAC, //aac
            MediaFormat.MIMETYPE_AUDIO_AMR_NB,//3gpp
            MediaFormat.MIMETYPE_AUDIO_AMR_WB,//amr宽带
    };

    public static final int[] ADTSFreqArray = {
            96000,
            88200,
            64000,
            48000,
            44100,
            32000,
            24000,
            22050,
            16000,
            12000,
            11025,
            8000,
            7350,
    };
}
