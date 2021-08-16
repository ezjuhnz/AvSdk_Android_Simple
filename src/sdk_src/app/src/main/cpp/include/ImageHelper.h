//
// Created by ezjuhnz on 2021/5/19.
//

#ifndef IMAGEHELPER_H
#define IMAGEHELPER_H
#ifdef __cplusplus
extern "C" {
#endif

#define FORMAT_NV21 1
#define FORMAT_NV12 2
#define FORMAT_YV12 3
#define FORMAT_I420 4

/**
 * 将rgb类型的图片转换成I420字节流
 * @param rgba
 * @param lineStride
 * @param width
 * @param height
 * @param i420Data
 * @return
 */
int rgbToI420(unsigned char *rgba, int lineStride, int width, int height, unsigned char *i420Data);

/**
 * 将rgb类型的图片转换成NV21字节流
 * @param rgba
 * @param lineStride
 * @param width
 * @param height
 * @param nv21Data
 * @return
 */
int rgbToNV21(unsigned char *rgba, int lineStride, int width, int height, unsigned char *nv21Data);

/**
 * 给YUV数据添加水印
 * @param yuvType
 * @param startX
 * @param startY
 * @param waterMarkData
 * @param waterMarkW
 * @param waterMarkH
 * @param yuvData
 * @param yuvW
 * @param yuvH
 */
void yuvAddWaterMark(int yuvType, int startX, int startY, unsigned char *waterMarkData,
                int waterMarkW, int waterMarkH, unsigned char *yuvData, int yuvW, int yuvH);

/**
 * 将NV21转成NV12
 * @param pNv21
 * @param pNv12
 * @param width
 * @param height
 */
void Nv21ToNv12(unsigned char* pNv21,unsigned char* pNv12,int width,int height);

/**
 * I420转NV21
 * @param i420Data
 * @param width
 * @param height
 * @param nv21Data
 * @return
 */
I420ToNV21(unsigned char* i420Data, int width, int height, unsigned char* nv21Data);

#ifdef __cplusplus
}
#endif
#endif //VIDEO_EDITOR_ANDROID_COPY_IMAGEHELPER_H
