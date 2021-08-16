package com.camera.sdk;

import android.app.Activity;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class PermissionUtil {
    public static void requestAllPermission(String[] permissions, Activity context)
    {
        Log.e("TAG","hello requestAllPermission");
        int mRequestCode = 101;
        ActivityCompat.requestPermissions(context, permissions, mRequestCode);
    }

    public static boolean checkPermissions(String[] permissions, Activity context){
        boolean allAllowed = true;
        Log.e("TAG","hello checkPermissions");
        for(String permission : permissions)
        {
            if(ActivityCompat.checkSelfPermission(context,permission) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED ){
                allAllowed = false;
                return allAllowed;
            }
        }
        return allAllowed;
    }

}
