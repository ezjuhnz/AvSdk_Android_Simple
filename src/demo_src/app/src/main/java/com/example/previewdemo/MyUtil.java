package com.example.previewdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MyUtil {

    /**
     * 将NV21原始数据流转成bitmap
     * @param nv21
     * @param width
     * @param height
     * @param context
     * @return
     */
    public static Bitmap nv21ToBitmap(byte[] nv21, int width, int height, Context context) {
        RenderScript rs;
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
        Type.Builder yuvType, rgbaType;
        Allocation in, out;
        rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
        in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(nv21);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);

        Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);
        //remember to destroy renderScript context, else the number of thread and graphic memory rise continuously
        rs.destroy();
        return bmpout;
    }


    public static void writeBytesToFile(byte[] bs, String fileName) throws IOException {
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        String savePath = dir.concat(fileName);
        OutputStream out = new FileOutputStream(savePath);
        InputStream is = new ByteArrayInputStream(bs);
        byte[] buff = new byte[1024];
        int len = 0;
        while ((len = is.read(buff)) != -1) {
            out.write(buff, 0, len);
        }
        is.close();
        out.close();
    }

}
