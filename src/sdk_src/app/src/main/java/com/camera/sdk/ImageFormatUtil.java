package com.camera.sdk;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.os.Environment;
import android.util.Log;
import android.util.Size;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ImageFormatUtil {
    public static final int COLOR_FormatI420 = 1;
    public static final int COLOR_FormatNV21 = 2;
    private final static String TAG = "ImageFormatUtil";

    /**
     *
     * @param image
     * @return
     */
    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    /**
     *将YVU_420_888格式的image转换成I420或NV21字节流,目前只支持这两种格式
     * @param image
     * @param colorFormat
     * @return
     */
    public static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Log.e(TAG,"hello crop.width, height=" + width + "," + height);
        Log.e(TAG,"ImageFormat.getBitsPerPixel(format)= " + ImageFormat.getBitsPerPixel(format));
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        Log.e(TAG,"planes[0].getRowStride() =" +planes[0].getRowStride());

        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();

            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            Log.e(TAG,"crop.top, crop.left=" + crop.top +"," + crop.left);
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }

        }
        return data;
    }


    /**
     * 将rgb字节流转换成I420字节流
     * @param rgba
     * @param width
     * @param height
     * @param yuv
     */
    public static void rgbToI420(byte[] rgba, int width, int height, byte[] yuv) {
        Log.e("TAG","hello rgba length=" + rgba.length);
        final int frameSize = width * height;
        int yIndex = 0;
        int uIndex = frameSize;
        int vIndex = frameSize + frameSize / 4;

        int R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                index = j * width + i; //第几个像素
                //the byte will never great than 127 and less than -128
//                if (rgba[index * 4] > 127 || rgba[index * 4] < -128) {
//                    Log.e("color", "-->" + rgba[index * 4]);
//                }
                //每个像素有4个字节,分别代表R,G,B,A
                R = rgba[index * 4] & 0xFF; //将byte变成int,范围从-128~127变成0~255
                G = rgba[index * 4 + 1] & 0xFF;
                B = rgba[index * 4 + 2] & 0xFF;

                //根据每个像素的RGB 计算出对应的YUV
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                yuv[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                //这种方法并不是标准的采样,标准采样应该是U和V隔行采样,此处U和V都采样奇数行
                if (j % 2 == 0 && index % 2 == 0) {
                    U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                    V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;
                    yuv[uIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                    yuv[vIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                }
            }
        }
    }

    /**
     * 将rgba类型的图片转换成I420字节流,该方法可处理带line padding的图片,输出有效像素的YUV
     * @param rgba
     * @param lineStride
     * @param width
     * @param height
     * @param yuvData
     */
    public static void rgbaToI420(byte[] rgba, int lineStride, int width, int height, byte[] yuvData){
        int imageSize = width * height;
        int pixelIdx = 0; //像素索引,填充像素也计算在内
        int yStartIdx = 0;
        int uStartIdx = imageSize;
        int vStartIdx = imageSize + imageSize / 4;
        int R,G,B,Y,U,V;
        for(int j = 0; j < height; j++){
            for(int i = 0; i < width; i++){
                //获取R,G,B
                pixelIdx = j * lineStride + i;
                R = rgba[pixelIdx * 4] & 0xFF;//每个像素有4个字节,分别是R,G,B,A
                G = rgba[pixelIdx * 4 + 1] & 0xFF;
                B = rgba[pixelIdx * 4 + 2] & 0xFF;

                //将RGB转成YVU
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                yuvData[yStartIdx++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if(j % 2 == 0 && i % 2 == 0) //奇数行,采样U
                {
                    U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                    yuvData[uStartIdx++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }
                else if(j % 2 == 1 && i % 2 == 0) { //偶数行,采样V
                    V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;
                    yuvData[vStartIdx++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                }

            }
        }
    }

    /**
     *将I420转成NV21
     * @param i420Data
     * @param width
     * @param height
     * @param nv21Data
     */
    public static void I420ToNV21(byte[] i420Data, int width, int height, byte[] nv21Data){
        int total = width * height;

        ByteBuffer bufferY = ByteBuffer.wrap(nv21Data, 0, total);
        ByteBuffer bufferVU = ByteBuffer.wrap(nv21Data, total, total / 2);

        bufferY.put(i420Data, 0, total);
        for (int i = 0; i < total / 4; i += 1) {
            bufferVU.put(i420Data[i + total + total / 4]);
            bufferVU.put(i420Data[total + i]);
        }
    }

    /**
     * 读取YUV文件,保存到字节数组中
     * @param filePath
     * @param width
     * @param height
     * @return
     * @throws IOException
     */
    public static byte[] getYUVDataFromFile(String filePath,int width,int height) throws IOException {
        byte[] wmBuffer = new byte[width * height * 3 / 2];

        //LogUtil.e(TAG,"hello len =" + len);
        return wmBuffer;
    }

    /**
     * 将字节流写到文件中
     * @param bs
     * @param fileName
     * @throws IOException
     */
    public static void writeBytesToFile(byte[] bs, String fileName) throws IOException {
        LogUtil.d(TAG,"hello writeBytesToFile fileName=" + fileName);
        OutputStream out = new FileOutputStream(fileName);
        InputStream is = new ByteArrayInputStream(bs);
        byte[] buff = new byte[1024];
        int len = 0;
        while ((len = is.read(buff)) != -1) {
            out.write(buff, 0, len);
        }
        is.close();
        out.close();
    }

    public static byte[] colorconvertRGB_YUV_NV21(int[] aRGB, int width, int height) {
        final int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        byte[] yuv = new byte[width * height * 3 / 2];

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                //a = (aRGB[index] & 0xff000000) >> 24; //not using it right now
                R = (aRGB[index] & 0xff0000) >> 16;
                G = (aRGB[index] & 0xff00) >> 8;
                B = (aRGB[index] & 0xff) >> 0;

                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                yuv[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));

                if (j % 2 == 0 && index % 2 == 0) {
                    yuv[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }
                index++;
            }
        }
        return yuv;
    }

    public static void NV21ToNV12(byte[] nv21,byte[] nv12,int width,int height){
        if(nv21 == null || nv12 == null)return;
        int framesize = width*height;
        int i = 0,j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for(i = 0; i < framesize; i++){
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize/2; j+=2)
        {
            nv12[framesize + j-1] = nv21[j+framesize];
        }
        for (j = 0; j < framesize/2; j+=2)
        {
            nv12[framesize + j] = nv21[j+framesize-1];
        }
    }
}
