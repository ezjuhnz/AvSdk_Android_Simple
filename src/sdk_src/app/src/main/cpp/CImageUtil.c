//
// Created by ezjuhnz on 2021/5/19.
//
#include <jni.h>
#include "ImageHelper.h"

JNIEXPORT jint JNICALL
Java_com_camera_sdk_JniImageUtil_rgbToI420(JNIEnv *env, jclass clazz, jbyteArray rgb,
                                           jint line_stride, jint width, jint height,
                                           jbyteArray i420_buf) {
   //1.将java类型的数据转换成C语言的指针
    unsigned char* srcData = (unsigned char*)(*env)->GetByteArrayElements(env, rgb, JNI_FALSE);
    unsigned char* dstData = (unsigned char*)(*env)->GetByteArrayElements(env, i420_buf, JNI_FALSE);

    rgbToI420(srcData,line_stride,width,height,dstData);
    //返回数据给java端
    (*env)->ReleaseByteArrayElements(env, rgb, (jbyte*)srcData, JNI_OK);
    (*env)->ReleaseByteArrayElements(env, i420_buf, (jbyte*)dstData, JNI_OK);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_camera_sdk_JniImageUtil_rgbToNV21(JNIEnv *env, jclass clazz, jbyteArray rgb,
                                           jint line_stride, jint width, jint height,
                                           jbyteArray i420_buf) {
    // TODO: implement rgbToNV21()
    return 0;
}


JNIEXPORT void JNICALL
Java_com_camera_sdk_JniImageUtil_yuvAddWaterMark(JNIEnv *env, jclass clazz, jint yuv_type,
                                                 jint start_x, jint start_y,
                                                 jbyteArray water_mark_data, jint water_mark_w,
                                                 jint water_mark_h, jbyteArray yuv_data, jint yuv_w,
                                                 jint yuv_h) {
    // TODO: implement yuvAddWaterMark()
    jbyte* jwaterMark = (*env)->GetByteArrayElements(env,water_mark_data, NULL);
    jbyte* jyuv = (*env)->GetByteArrayElements(env,yuv_data, NULL);

    unsigned char* pWaterMark = (unsigned char*)jwaterMark;
    unsigned char* pYuv = (unsigned char*)jyuv;


    yuvAddWaterMark((int)yuv_type,(int)start_x,(int)start_y,pWaterMark,
                    (int)water_mark_w,(int)water_mark_h,pYuv,(int)yuv_w, (int)yuv_h);
    (*env)->ReleaseByteArrayElements(env, water_mark_data, pWaterMark, 0);
    (*env)->ReleaseByteArrayElements(env, yuv_data, pYuv, 0);
}

JNIEXPORT void JNICALL
Java_com_camera_sdk_JniImageUtil_Nv21ToNv12(JNIEnv *env, jclass clazz, jbyteArray jNv21Data,
                                            jbyteArray jNv12Data, jint jwidth, jint jheight) {
    // TODO: implement Nv21ToNv12()
    jbyte* jNv21 = (*env)->GetByteArrayElements(env, jNv21Data, NULL);
    jbyte* jNv12 = (*env)->GetByteArrayElements(env, jNv12Data, NULL);

    unsigned char* pNv21 = (unsigned char*)jNv21;
    unsigned char* pNv12 = (unsigned char*)jNv12;


    Nv21ToNv12(pNv21,pNv12,(int)jwidth, (int)jheight);
    (*env)->ReleaseByteArrayElements(env, jNv21Data, jNv21, 0);
    (*env)->ReleaseByteArrayElements(env, jNv12Data, jNv12, 0);
}

JNIEXPORT jint JNICALL
Java_com_camera_sdk_JniImageUtil_I420ToNV21(JNIEnv *env, jclass clazz, jbyteArray i420_data,
                                            jint width, jint height, jbyteArray nv21_data) {
    unsigned char* srcData = (unsigned char*)(*env)->GetByteArrayElements(env, i420_data, JNI_FALSE);
    unsigned char* dstData = (unsigned char*)(*env)->GetByteArrayElements(env, nv21_data, JNI_FALSE);

    I420ToNV21(srcData,width,height,dstData);
    //返回数据给java端
    (*env)->ReleaseByteArrayElements(env, i420_data, (jbyte*)srcData, JNI_OK);
    (*env)->ReleaseByteArrayElements(env, nv21_data, (jbyte*)dstData, JNI_OK);
    return 0;
}