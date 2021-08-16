package com.example.previewdemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.camera.sdk.CallbackFactory;
import com.camera.sdk.ConfigData;
import com.camera.sdk.Constants;
import com.camera.sdk.ScreenHelper;
import com.camera.sdk.SdkMedia;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenDisplayActivity extends AppCompatActivity {
    private static final String TAG = "ScreenDisplayActivity";
    String[] mPermissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SYSTEM_ALERT_WINDOW
    };
    SdkMedia sdkMedia;
    private SurfaceView previewSurface;
    private ImageView screenImg;

    private Context mContext;
    private int mDeviceInx = ConfigData.ScreenIdx;         //设备ID,0:摄像机; 1:屏幕
    public static Bitmap mBitmap;
    private Button startBtn;
    private Button endBtn;
    private Button backBtn;

    //视频配置参数
    private int mWidth = 1280;  //宽度
    private int mHeight = 720;  //高度
    //文件保存路径
    private static final String basePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
    long beginMS;
    Intent mIntent;
    private int test;
    private BufferedOutputStream outputStream;
    //摄像机回调,在此处获取yuv数据
    CallbackFactory.OuterCallback dataCallback = new CallbackFactory.OuterCallback() {
        @Override
        public void onCameraRawData(byte[] bytes) {
            long ms = System.currentTimeMillis();
            //yuv转bitmap耗费时间较长,如果设置了高帧率,则可能无法保证帧率达到预期值
            //建议将转换操作放在新建的线程中执行以保证帧率
            mBitmap = MyUtil.nv21ToBitmap(bytes, mWidth, mHeight, mContext);
            Log.e("TAG","hello nv21ToBitmap takes " + (System.currentTimeMillis()-ms) + " ms");

            Log.e("TAG","hello onCameraRawData takes " + (System.currentTimeMillis() - beginMS) + " ms");
            beginMS = System.currentTimeMillis();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    screenImg.setImageBitmap(mBitmap);
                }
            });
        }

        @Override
        public void onScreenRawData(byte[] bytes) {
            long ms = System.currentTimeMillis();
            //yuv转bitmap耗费时间较长,如果设置了高帧率(25~30),则可能无法保证帧率达到预期值
            //建议将转换操作放在新建的线程中执行以保证帧率
            mBitmap = MyUtil.nv21ToBitmap(bytes, mWidth, mHeight, mContext);
            Log.e("TAG","hello nv21ToBitmap takes " + (System.currentTimeMillis()-ms) + " ms");

            Log.e("TAG","hello onScreenRawData takes " + (System.currentTimeMillis() - beginMS) + " ms");
            beginMS = System.currentTimeMillis();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    screenImg.setImageBitmap(mBitmap);
                }
            });
        }

        @Override
        public void onCameraEncData(byte[] bytes) {
            //Log.e("TAG","hello onCameraEncData size=" + bytes.length);
            Log.e("TAG","hello onCameraEncData takes " + (System.currentTimeMillis() - beginMS) + " ms");
            //beginMS = System.currentTimeMillis();
        }

        @Override
        public void onScreenEncData(byte[] bytes) {
            Log.d(TAG,"hello onScreenEncData length=" + bytes.length);
        }

        @Override
        public void onAudioRawData(byte[] bytes) {
            Log.d("TAG","hello onAudioRawData length=" + bytes.length);
        }

        @Override
        public void onAudioEncData(byte[] bytes) {
            Log.d("TAG","hello onAudioEncData length=" + bytes.length);
            if(outputStream != null){
                try {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            test++;
            if(test == 500){
                try {
                    outputStream.close();
                    outputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onError(byte[] bytes) {
            Log.d("TAG","hello PreviewCallback onError");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
//        createFile(basePath.concat("demo2.aac"));
        printUEInfo();
        Log.d("TAG","hello onCreate start==");

        setContentView(R.layout.activity_screen_display);
        mContext = this;
        previewSurface = findViewById(R.id.preview_surface2);

        screenImg = findViewById(R.id.screenImg2);
        startBtn = findViewById(R.id.start_btn2);
        endBtn = findViewById(R.id.end_btn2);
        backBtn = findViewById(R.id.back_btn2);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //是否生成文件
                sdkMedia.saveAudioFile(true);
                sdkMedia.saveAudioEncFile(true);
                sdkMedia.saveVideoEncFile(true);
            }
        });

        endBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                //关闭文件流
                sdkMedia.saveAudioFile(false);
                sdkMedia.saveAudioEncFile(false);
                sdkMedia.saveVideoEncFile(false);
            }
        });

        backBtn .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToMainActivity();
            }
        });

        startFloatingImageDisplayService();    //开启悬浮窗
        //开启Log,设置输出等级ERROR
        initSdk();              //初始化SDK
    }

    //初始化SDK
    private void initSdk() {
        Log.d("TAG","hello initsdk===");
        sdkMedia = SdkMedia.getInstance();
        ConfigData avConfig = new ConfigData();
        avConfig.mContext = this;
        avConfig.mSurfaceView = previewSurface;
        //测试文件是否生成
        long timeStamp = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String strDate = sdf.format(new Date(timeStamp));
        Log.d(TAG,"hello str date =" + strDate);

        //视频参数
        avConfig.mVideoWidth = mWidth;
        avConfig.mVideoHeight = mHeight;
        avConfig.mFps = 30;
        avConfig.mVideoBitRate = 2000 * 1000; //视频码率;
        avConfig.mVideoEncFormat = Constants.VIDEO_ENC_H264;;
        avConfig.mKeyFrameInterval = 1;

        avConfig.mScreenEncPath = basePath.concat("previewSdk/screen/" + strDate +"/previewSdk_screen.h264");
        avConfig.mScreenMuxPath = basePath.concat("previewSdk/screen/" + strDate +"/previewSdk_screen.mp4");
        avConfig.mCameraEncPath = basePath.concat("previewSdk/camera/" + strDate +"/previewSdk_camera.h264");
        avConfig.mCameraMuxPath = basePath.concat("previewSdk/camera/" + strDate +"/previewSdk_camera.mp4");
        avConfig.audioRawPath = basePath.concat("previewSdk/screen/" + strDate +"/previewSdk.pcm");   //原始音频数据保存路径
        avConfig.audioEncPath = basePath.concat("previewSdk/screen/" + strDate +"/previewSdk.aac");   //编码音频数据保存路径


        //音频参数
        avConfig.mSampleRate = 44100;      //采样率:44100HZ是目前的采样标准,支持大多数设备
        avConfig.channelConfig = AudioFormat.CHANNEL_IN_MONO; //单声道mono或立体声stereo
        avConfig.audioEncFormat = Constants.AUDIO_ENC_AAC;   //音频编码格式;
        avConfig.audioBitRate = 96000;
        avConfig.mDeviceIdx = ConfigData.CameraIdx;

        sdkMedia.initSDK(avConfig);
        sdkMedia.setDataCallback(dataCallback);
        sdkMedia.setWaterMark(R.raw.msbank, 100, 100); //设置水印,可选
        sdkMedia.setLog(true); //开启log,可选

        sdkMedia.startAudioCapture();           //1.开始采集声音
        sdkMedia.startVideoCapture(mDeviceInx); //2.开始视频采集
        sdkMedia.startAudioEncoder();           //3.开始音频编码
        sdkMedia.startVideoEncoder();           //4.开始视频编码
        sdkMedia.startMuxer();                  //5.初始化混合器
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("TAG","requestCode, resultCode= " + requestCode + "," + resultCode);
        if (requestCode == ScreenHelper.SCREEN_CAPTURE_REQUEST) {
            if(resultCode == RESULT_OK)
                sdkMedia.startScreenCapture(resultCode, data);
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    //使用悬浮窗展示图片
    public void startFloatingImageDisplayService() {
        if (FloatViewService.isStarted) {
            return;
        }
        Log.d("TAG","hello startFloatingImageDisplayService 111");
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT);
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 1);
        } else {
            Log.d(TAG,"hello start floating service==");
            mIntent = new Intent(ScreenDisplayActivity.this, FloatViewService.class);
            mIntent.putExtra("index","2");
            startService(mIntent);
        }
    }

    @Override
    protected void onDestroy(){
        Log.d(TAG,"onDestroy 111");
        sdkMedia.uninitSdk();
        Log.d(TAG,"backToMainActivity backToMainActivity onDestroy 222");
        super.onDestroy();
        Log.d(TAG,"backToMainActivity backToMainActivity onDestroy 333");
    }

    private void backToMainActivity(){
        Log.d(TAG,"hello backToMainActivity start==");
        stopFloatingWindow();
        Intent intent = new Intent();
        intent.setClass(ScreenDisplayActivity.this, MainActivity.class);
        startActivity(intent);
        Log.d(TAG,"zjh backToMainActivity backToMainActivity finish start==");
        ScreenDisplayActivity.this.finish();
        Log.d(TAG,"zjh screenDisplayActivity backToMainActivity finish end==");
    }

    private void stopFloatingWindow(){
        stopService(mIntent);
    }

    private void printUEInfo(){
        String phoneInfo = "Product: " + android.os.Build.PRODUCT;
        phoneInfo += ", CPU_ABI: " + android.os.Build.CPU_ABI;
        phoneInfo += ", TAGS: " + android.os.Build.TAGS;
        phoneInfo += ", VERSION_CODES.BASE: " + android.os.Build.VERSION_CODES.BASE;
        phoneInfo += ", MODEL: " + android.os.Build.MODEL;
        phoneInfo += ", SDK: " + android.os.Build.VERSION.SDK;
        phoneInfo += ", VERSION.RELEASE: " + android.os.Build.VERSION.RELEASE;
        phoneInfo += ", DEVICE: " + android.os.Build.DEVICE;
        phoneInfo += ", DISPLAY: " + android.os.Build.DISPLAY;
        phoneInfo += ", BRAND: " + android.os.Build.BRAND;
        phoneInfo += ", BOARD: " + android.os.Build.BOARD;
        phoneInfo += ", FINGERPRINT: " + android.os.Build.FINGERPRINT;
        phoneInfo += ", ID: " + android.os.Build.ID;
        phoneInfo += ", MANUFACTURER: " + android.os.Build.MANUFACTURER;
        phoneInfo += ", USER: " + android.os.Build.USER;

        Log.e(TAG,"UE info=" + phoneInfo);
    }


    private boolean createFile(String fileName){
        Log.d(TAG,"hello createFile name=" + fileName);
        if(fileName == null || fileName.trim().equals("")){
            Log.e(TAG,"error fileName is null");
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
                outputStream = new BufferedOutputStream(new FileOutputStream(file));
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
