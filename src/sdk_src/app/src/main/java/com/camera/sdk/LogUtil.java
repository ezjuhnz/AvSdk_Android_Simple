package com.camera.sdk;

import android.util.Log;

public class LogUtil {

    public static int ERROR_LEVEL = 5;
    public static int WARN_LEVEL = 4;
    public static int DEBUG_LEVEL = 3;
    public static int INFO_LEVEL = 2;
    private static int  logLevel = 0;
    private static boolean isShow = false;

    public static void showLog(){
        isShow = true;
    }

    public static void hideLog(){
        isShow = false;
    }

    public static void setLogLevel(int level){
        logLevel = level;
    }

    public static void d(String tag, String msg){
        if(DEBUG_LEVEL < logLevel){
            return;
        }
        if(isShow){
            Log.d(tag, msg);
        }
    }

    public static void e(String tag, String msg){
        if(ERROR_LEVEL < logLevel){
            return;
        }
        if(isShow){
            Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr){
        if(ERROR_LEVEL < logLevel){
            return;
        }
        if(isShow){
            Log.e(tag, msg, tr);
        }
    }
}
