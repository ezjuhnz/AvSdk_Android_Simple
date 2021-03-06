package com.example.previewdemo;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class FloatViewService extends Service{
    public static boolean isStarted = false;

    private static WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    private static View displayView;
    private ImageView mImageView;
    private int[] images;
    private int imageIndex = 0;
    private int mIndex;

    private Handler changeImageHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("TAG","hello onCreate 111");
        isStarted = true;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.width = 400;
        layoutParams.height = 600;

        layoutParams.x = 100;
        layoutParams.y = 800;


        changeImageHandler = new Handler(this.getMainLooper(), changeImageCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("TAG","hello onStartCommand 222");
        String msg = intent.getStringExtra("index");
        Log.e("TAG","hello msg= " + msg);
        if(msg != null && msg.equals("1")){
            mIndex = 1;
        }else if(msg != null && msg.equals("2")){
            mIndex = 2;
        }
        Log.d("TAG","hello index= " + mIndex);
        showFloatingWindow();
        return super.onStartCommand(intent, flags, startId);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            displayView = layoutInflater.inflate(R.layout.image_display, null);
            displayView.setOnTouchListener(new FloatingOnTouchListener());
            mImageView = displayView.findViewById(R.id.float_imageview);

            if(mIndex == 1){
                mImageView.setImageBitmap(CameraDisplayActivity.mBitmap);
            }else{
                mImageView.setImageBitmap(ScreenDisplayActivity.mBitmap);
            }

            windowManager.addView(displayView, layoutParams);
            changeImageHandler.sendEmptyMessageDelayed(0, 50);
        }
    }

    private Handler.Callback changeImageCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(isStarted){
                if (msg.what == 0) {
                    if (mImageView != null) {
                        if(mIndex == 1){
                            mImageView.setImageBitmap(CameraDisplayActivity.mBitmap);
                        }else if(mIndex == 2){
                            mImageView.setImageBitmap(ScreenDisplayActivity.mBitmap);
                        }
                    }
                    changeImageHandler.sendEmptyMessageDelayed(0, 50);
                }
            }
            return false;
        }
    };

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    @Override
    public void onDestroy(){
        Log.d("TAG","hello floating window destroy");
        if(displayView != null){
            windowManager.removeView(displayView);
        }
        isStarted = false;
    }

    public static void removeFloatingView(){
        if(displayView != null){
            windowManager.removeView(displayView);
        }

        isStarted = false;
    }
}
