package com.camera.sdk;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Camera1Helper {
    private static final String TAG = "Camera1Helper";
    private Context mContext;
    private static final int FORMAT_NV21 = 1;
    private static final int FORMAT_NV12 = 2;
    private static final int FORMAT_YV12 = 3;
    private static final int FORMAT_I420 = 4;

    //水印图片(YUV格式)
    private byte[] wmBuff;
    private static int wmWidth = 0; //水印宽度
    private static int wmHeight = 0; //水印高度
    private int wmPosX;
    private int wmPosY;

    /**相机实体*/
    private Camera mCamera;

    private Point mPreSize ;
    private Point mPicSize ;

    private int mWidth;     //宽度
    private int mHeight;    //高度
    private int mFps;       //帧率
    private SurfaceView mSurfaceView;
    private int mWaterMarkId;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private CallbackFactory.VideoCaptureCallback mVideoCaptureCallback; //内部回调:用于将原始数据添加到队列中,编码时会用到
    long beginMs = 0;
    private String mYuvPath;
    private boolean isStart;

    private int test = 0;

    public void setContext(Context context){
        mContext = context;
    }

    public void initCameraCapture(SurfaceView view,int width, int height, int fps, String encPath){
        if(view == null){
            LogUtil.e(TAG,"error surfaceView can not be null");
            return;
        }
        mSurfaceView = view;
        mWidth = width;
        mHeight = height;
        mFps = fps;
        mYuvPath = encPath.replace(".h264",".yuv");
    }

    public void uninitCameraCapture(){
        stopCameraCapture();
    }
    /**
     * 开始采集数据
     */
    public void startCameraCapture(){
        //采集摄像头数据应在新线程中执行
        startCaptureThread();
    }

    public void stopCameraCapture(){
        stopCaptureThread();
    }


    public void switchCamera(){
        mCameraId = (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) ?
                Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    private void startCaptureThread(){
        CaptureThread captureThread = new CaptureThread();
        captureThread.start();
    }

    private void stopCaptureThread(){
        isStart = false;
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Camera.Size getPropPictureSize(List<Camera.Size> list, float th, int minWidth){
        Collections.sort(list, sizeComparator);
        int i = 0;
        for(Camera.Size s:list){
            if((s.height >= minWidth) && equalRate(s, th)){
                break;
            }
            i++;
        }
        if(i == list.size()){
            i = 0;
        }
        return list.get(i);
    }

    private Camera.Size getPropPreviewSize(List<Camera.Size> list, float th, int minWidth){
        Collections.sort(list, sizeComparator);

        int i = 0;
        for(Camera.Size s:list){
            if((s.height >= minWidth) && equalRate(s, th)){
                break;
            }
            i++;
        }
        if(i == list.size()){
            i = 0;
        }
        return list.get(i);
    }

    private static boolean equalRate(Camera.Size s, float rate){
        float r = (float)(s.width)/(float)(s.height);
        if(Math.abs(r - rate) <= 0.03) {
            return true;
        }else{
            return false;
        }
    }

    public void setWaterMark(byte[] wmData, int start_x, int start_y, int width, int height){
        LogUtil.d(TAG,"hello  start_x,start_y, width,height=" + start_x + "," + start_y + ","
                + width + "," + height );
        LogUtil.d(TAG,"wmData length=" + wmData.length);
        wmPosX = start_x;
        wmPosY = start_y;
        wmWidth = width;
        wmHeight = height;
        wmBuff = wmData;
    }
    private Comparator<Camera.Size> sizeComparator = new Comparator<Camera.Size>(){
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if(lhs.height == rhs.height){
                return 0;
            }else if(lhs.height > rhs.height){
                return 1;
            }else{
                return -1;
            }
        }
    };

    public class CaptureThread extends Thread{
        public CaptureThread(){

        }
        @Override
        public void run(){
            if(isStart){
                return;
            }
            isStart = true;
            open();
            preview();
            LogUtil.d(TAG,"hello CaptureThread 摄像头采集线程退出...");
        }
    }

    private void open() {
        LogUtil.d(TAG,"hello CameraController open start==");
        mCamera = Camera.open(mCameraId);
        if (mCamera != null){
            /**选择当前设备允许的预览尺寸*/
            Camera.Parameters param = mCamera.getParameters();

            param.setPictureSize(mWidth, mHeight);
            param.setPreviewSize(mWidth, mHeight); //设置预览图像分辨率
            LogUtil.d(TAG,"hello picSize=" + mWidth + "*" + mHeight);
            param.setPreviewFormat(ImageFormat.NV21);           //设置预览格式
            List<int[]> frameRates = param.getSupportedPreviewFpsRange();
            int l_first = 0;
            int l_last = frameRates.size() - 1;
            int minFps = (frameRates.get(l_first))[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
            int maxFps = (frameRates.get(l_last))[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
            LogUtil.d(TAG,"hello mFps= " + mFps + "," + maxFps);
            param.setPreviewFrameRate(mFps);
            param.setPreviewFpsRange(mFps*1000, mFps*1000);

            mCamera.setParameters(param);
            Camera.Size pre = param.getPreviewSize();
            Camera.Size pic = param.getPictureSize();

            //因为手机默认是竖屏,而预览分辨率只支持横屏,所以要调换宽高
            mPicSize = new Point(pic.height,pic.width);
            mPreSize = new Point(pre.height,pre.width);
            //设置

            try {
                mCamera.setPreviewDisplay(mSurfaceView.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.setPreviewCallback(new Camera.PreviewCallback(){
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    LogUtil.e(TAG,"hello capture one frame takes " + (System.currentTimeMillis()-beginMs) + " ms");
                    beginMs = System.currentTimeMillis();
                    long tmp = System.currentTimeMillis();
                    if(wmBuff != null){
                        JniImageUtil.yuvAddWaterMark(FORMAT_NV21, wmPosX, wmPosY, wmBuff, wmWidth, wmHeight, data,
                                mWidth, mHeight);
                    }
                    if(test == 5){
                        try {
                            ImageFormatUtil.writeBytesToFile(data, mYuvPath);
                        } catch (IOException e) {
                            LogUtil.e(TAG,"error writeBytesToFile failed==");
                            e.printStackTrace();
                        }
                    }
                    test++;
                    LogUtil.e(TAG,"hello yuvAddWaterMark takes " + (System.currentTimeMillis()-tmp) + " ms");
                    //将 NV21 数据放进队列中,编码时会从队列中获取数据
                    mVideoCaptureCallback.onRawData(data);
                }
            });
        }
    }

    private void preview() {
        if (mCamera != null){
            mCamera.startPreview();
        }
    }
    public void setCallback(CallbackFactory.VideoCaptureCallback callback) {
        this.mVideoCaptureCallback = callback;
    }
}
