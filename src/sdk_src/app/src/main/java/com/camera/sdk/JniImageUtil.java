package com.camera.sdk;

public class JniImageUtil {
    static{
        System.loadLibrary("cImageUtil");
    }
    public static native int rgbToI420(byte[] rgb, int lineStride, int width, int height, byte[] i420Buf);

    public static native int rgbToNV21(byte[] rgb, int lineStride, int width, int height, byte[] i420Buf);

    public native static void yuvAddWaterMark(int yuvType, int startX, int startY, byte[] waterMarkData,
                                              int waterMarkW, int waterMarkH,byte[] yuvData, int yuvW, int yuvH);

    public native static void Nv21ToNv12(byte[] pNv21,byte[] pNv12,int width,int height);

    public native static int I420ToNV21(byte[] i420Data, int width, int height, byte[] nv21Data);
}
