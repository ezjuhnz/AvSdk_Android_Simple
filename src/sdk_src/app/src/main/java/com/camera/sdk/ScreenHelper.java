package com.camera.sdk;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenHelper {
    private final static String TAG = "ScreenHelper";
    private Surface mImageSurface;
    private Handler mScreenHandler;
    HandlerThread mHandlerThread;
    public static final int SCREEN_CAPTURE_REQUEST = 1000;

    private ImageReader mImageReader; //用于获取屏幕原始数据
    MediaProjectionManager mProjectionManager; //屏幕录制
    MediaProjection mMediaProjection;
    private CallbackFactory.ScreenRawCallback mScreenRawCallback;

    private static final int FORMAT_NV21 = 1;
    private static final int FORMAT_NV12 = 2;
    private static final int FORMAT_YV12 = 3;
    private static final int FORMAT_I420 = 4;

    private Context mContext;
    private int mWaterMarkId;
    private int wmWidth;    //水印图片宽度
    private int wmHeight;   //水印图片高度
    private int wmPosX;
    private int wmPosY;
    private int mWidth;     //原始图片宽度
    private int mHeight;    //原始图片高度
    private byte[] wmBuff;
    private String mYuvPath;

    ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            LogUtil.d(TAG,"hello imageAvailableListener--");
            //4.在ImageReader 的回调函数中返回数据
            handleScreenImage(reader);
        }
    };

    public void initScreenCapture(int width, int height,String filePath) {
        mWidth = width;
        mHeight = height;
        mYuvPath = filePath.replace(".h264",".yuv");
        if(mImageReader == null) {
            mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888,1);
            mImageSurface = mImageReader.getSurface(); //使用ImageReader的surface
            mImageReader.setOnImageAvailableListener(imageAvailableListener, mScreenHandler);
        }
        LogUtil.d(TAG,"hello prepareScreenCapture---");
        mProjectionManager = (MediaProjectionManager) mContext.getSystemService(mContext.MEDIA_PROJECTION_SERVICE);
        Intent intent = mProjectionManager.createScreenCaptureIntent();
        ((AppCompatActivity)mContext).startActivityForResult(intent, SCREEN_CAPTURE_REQUEST);
    }

    public void uninitScreenCapture(){
        stopScreenThread();
        stopScreenCapture();
    }

    public void startScreenCapture(int resultCode, Intent data){
        LogUtil.d(TAG,"hello startScreenCapture---");
        startScreenThread();
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.createVirtualDisplay("screen-mirror",
                mWidth,
                mHeight,
                mContext.getResources().getSystem().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageSurface, null, null);

    }

    public void stopScreenCapture() {
        LogUtil.d(TAG,"hello screen stopCapture==");
        if(mMediaProjection != null){
            mMediaProjection.stop();
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    /**
     * 处理屏幕采集的数据,从屏幕采集的数据一般是RGB
     * @param reader
     */
    int test = 0;
    private void handleScreenImage(ImageReader reader) {
        LogUtil.d(TAG,"hello handleScreenImage--");
        //TODO:此处直接将image转成NV21,没有根据视频配置参数转成指定的格式,待完善..
        Image image = reader.acquireLatestImage();
        if(image != null){
            final Image.Plane[] planes = image.getPlanes();
            mWidth = image.getWidth();
            mHeight = image.getHeight();
            ByteBuffer imgBuffer = planes[0].getBuffer();

            //将image数据放到byte[]中
            byte[] rgbaArray = new byte[imgBuffer.capacity()]; //4669440
            imgBuffer.get(rgbaArray);  //将ByteBuffer中的数据copy到byte[]
            int pixelStride = planes[0].getPixelStride(); //相邻像素之间的间隔,大小为bytesPerPixel
            int rowStride = planes[0].getRowStride();    //两行开始像素之间的间隔,大小: width*bytesPerPixel
            //Log.e("TAG", "hello pixelStride,rowStride=" + pixelStride + "," + rowStride);
            int rowPadding = rowStride - pixelStride * mWidth; //当前行的最后一个像素到下一行的第一个像素的填充量:单位byte
            int aStride = mWidth * 4 + rowPadding;
            int aWidth = mWidth + rowPadding / pixelStride;

            byte[] yuvbuffer = new byte[mWidth * mHeight * 3 / 2];
            byte[] nv21Buffer = new byte[mWidth * mHeight * 3 / 2];
            //ImageFormatUtil.rgbToI420(rgbaArray,aWidth,height,yuvbuffer);

            long begTime = System.currentTimeMillis();
//            ImageFormatUtil.rgbaToI420(rgbaArray,aWidth, mWidth, mHeight,yuvbuffer);
            JniImageUtil.rgbToI420(rgbaArray, aWidth, mWidth, mHeight, yuvbuffer); //bad performance!!
            LogUtil.e(TAG,"hello JniImageUtil.rgbToI420 takes time: " + (System.currentTimeMillis() - begTime) + "ms");

            begTime = System.currentTimeMillis();
//            ImageFormatUtil.I420ToNV21(yuvbuffer, mWidth, mHeight, nv21Buffer);
            JniImageUtil.I420ToNV21(yuvbuffer, mWidth, mHeight, nv21Buffer);
            LogUtil.e(TAG,"hello I420ToNV21 takes time: " + (System.currentTimeMillis() - begTime) + "ms");

            if(wmBuff != null){
                JniImageUtil.yuvAddWaterMark(FORMAT_NV21, wmPosX, wmPosY, wmBuff, wmWidth, wmHeight, nv21Buffer,
                        mWidth, mHeight);
            }
            if(test == 5){
                try {
                    ImageFormatUtil.writeBytesToFile(nv21Buffer,mYuvPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            test++;
            mScreenRawCallback.onRawData(nv21Buffer);

            image.close();
        }

    }

    private void startScreenThread() {
        LogUtil.d(TAG,"hello startScreenThread start==");
        mHandlerThread = new HandlerThread("screen");
        mHandlerThread.start();
        mScreenHandler = new Handler(mHandlerThread.getLooper());
    }

    private void stopScreenThread() {
        LogUtil.d(TAG,"hello stopScreenThread start==");
        if(mHandlerThread != null){
            mHandlerThread.quitSafely();
            try {
                mHandlerThread.join();
                mHandlerThread = null;
                mScreenHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setContext(Context context){
        mContext = context;
    }

    public void setCallback(CallbackFactory.ScreenRawCallback callback){
        mScreenRawCallback = callback;
    }

    public void setWaterMark(byte[] wmData,  int start_x, int start_y,int width, int height){
        wmPosX = start_x;
        wmPosY = start_y;
        wmWidth = width;
        wmHeight = height;
        wmBuff = wmData;
    }
}
