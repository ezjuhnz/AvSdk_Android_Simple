//
// Created by ezjuhnz on 2021/5/19.
//
#include "ImageHelper.h"
#include "string.h"
#ifdef __cplusplus
extern "C"
{
#endif
//rgb转I420: version 1
int rgbToI420(unsigned char* rgba, int lineStride, int width, int height, unsigned char* i420Data) {
    int imageSize = width * height;
    //像素索引:一个像素有4个字节,分别存储R,G,B,A;我们并不关心A,所以忽略.
    int pixelIdx = 0;
    int yStartIdx = 0;
    int uStartIdx = imageSize;
    int vStartIdx = imageSize + imageSize / 4;
    int R,G,B,Y,U,V;
    int index, i, j;
    unsigned char* i420_y = i420Data;
    unsigned char* i420_u = i420_y + imageSize;
    unsigned char* i420_v = i420_u + imageSize / 4;

    //=============================方式1 开始================================//
    //1.Y Plane
    for(index = 0; index < imageSize; index++){
        *(i420_y++) = ((66 * rgba[index * 4] + 129 * rgba[index * 4 + 1] + 25 * rgba[index * 4 + 2] + 128) >> 8) + 16;
    }
    //2.U Plane
    for(j = 0; j < height-1; j+=2){
        for(i = 0; i < width; i+=2){
            const int idx = j * width + i;
            *(i420_u++) = ((-38 * rgba[idx*4] - 74 * rgba[idx*4+1] + 112 * rgba[idx*4+2] + 128) >> 8) + 128;
        }
    }
    //3.V Plane
    for(j = 1; j < height; j+=2){
        for(i = 0; i < width; i+=2){
            const int idx = j * width + i;
            *(i420_v++) = ((112 * rgba[idx*4] - 94 * rgba[idx*4+1] - 18 * rgba[idx*4+2] + 128) >> 8) + 128;
        }
    }
    return 0;
    //=============================方式1 结束================================//

    //=============================方式2 开始===============================//
//    for(int j = 0; j < height; j++){
//        for(int i = 0; i < width; i++){
//            //获取R,G,B
//            pixelIdx = j * lineStride + i;
//            R = rgba[pixelIdx * 4];    //获取R
//            G = rgba[pixelIdx * 4 + 1];//获取G
//            B = rgba[pixelIdx * 4 + 2];//获取B
//
//            //采样Y
//            Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
//            i420Data[yStartIdx++] = (unsigned char) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
//            //采样U:I420是YUV420的一种,U的采样是从第一行开始隔行采样,每一行中又从第一列开始隔列采样
//            if(j % 2 == 0 && i % 2 == 0)
//            {
//                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
//                i420Data[uStartIdx++] = (unsigned char) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
//            }
//            //采样V:I420是YUV420的一种,V的采样是从第二行开始隔行采样,每一行中又从第一列开始隔列采样
//            else if(j % 2 == 1 && i % 2 == 0) {
//                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;
//                i420Data[vStartIdx++] = (unsigned char) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
//            }
//
//        }
//    }
//    return 0;
    //==========================方式2 结束====================================//
}

int rgbToNV21(unsigned char* rgba, int lineStride, int width, int height, unsigned char* nv21Data) {

}

//I420转NV21
int I420ToNV21(unsigned char* i420Data, int width, int height, unsigned char* nv21Data){
    //I420转NV21共分3步,分别是复制Y, 复制U, 复制V
    int i = 0;

    int u_start = width * height;
    int v_start = width * height * 5 / 4 ;
    unsigned char* i420_u = i420Data + u_start;
    unsigned char* i420_v = i420Data + v_start;
    unsigned char* nv21_u = nv21Data + u_start;
    //1.复制Y
    memcpy(nv21Data, i420Data,width * height);

    //2.复制U:0123 => 1357
    for(i = 0; i < width * height / 4; i++){
        *(nv21_u + i*2+1) = *(i420_u + i);
    }
    //3.复制V:4567 => 0246
    for(i = 0; i < width * height / 4; i++){
        *(nv21_u + i*2) = *(i420_v + i);
    }
    return 0;
}

void Nv21ToNv12(unsigned char* pNv21,unsigned char* pNv12,int width,int height){
    if(pNv21 == NULL || pNv12 == NULL){
//        printf("%s: pNv21 is null or pNv12 is null",__FUNCTION__);
        return;
    }

    int frameSize = width * height;
    if(frameSize <= 0){
//        printf("%s: frameSize <= 0",__FUNCTION__);
        return;
    }

    //拷贝Y分量
    memcpy(pNv12,pNv21,frameSize);

    int i = 0;
    for (i = 0; i < frameSize / 4; i++) {
        pNv12[frameSize + i * 2] = pNv21[frameSize + i * 2 + 1]; //U
        pNv12[frameSize + i * 2 + 1] = pNv21[frameSize + i * 2]; //V
    }
}

void yuvAddWaterMark(int yuvType, int startX, int startY, unsigned char *waterMarkData,
                                int waterMarkW, int waterMarkH, unsigned char *yuvData, int yuvW, int yuvH)
{
    if(waterMarkData == NULL || yuvData == NULL){
        return;
    }

    if(yuvW < startX + waterMarkW || (yuvH < startY + waterMarkH)){
        return;
    }

    switch(yuvType) {
        case FORMAT_NV21:
        case FORMAT_NV12:{
            int i = 0;
            int j = 0;
            int k = 0;
            for(i = startY; i < waterMarkH+startY; i++) {
                //逐行拷贝Y分量
                memcpy(yuvData+startX+i*yuvW, waterMarkData+j*waterMarkW, waterMarkW);
                j++;
            }

            for(i = startY/2; i < (waterMarkH+startY)/2; i++) {
                //UV分量高度是Y分量的一半,逐行拷贝UV分量
                memcpy(yuvData+startX+yuvW*yuvH+i*yuvW, waterMarkData+waterMarkW*waterMarkH+k*waterMarkW, waterMarkW);
                k++;
            }

#ifdef DUMP_OUTPUT
            char output[256];
          memset(output,0,sizeof(output));
          sprintf(output,"water_nv21_%dx%d.yuv",yuvW,yuvH);
          FILE *outPutFp = fopen(output, "w+");
          fwrite(yuvData, 1, yuvW*yuvH*3/2, outPutFp);
          fclose(outPutFp);
#endif
            break;
        }
        case FORMAT_I420:{
            int i = 0;
            unsigned char* waterY = waterMarkData;
            unsigned char* waterU = waterY + waterMarkW * waterMarkH;
            unsigned char* waterV = waterU + waterMarkW * waterMarkH / 4;

            unsigned char* destY = yuvData;
            unsigned char* destU = destY + yuvW*yuvH;
            unsigned char* destV = destU + yuvW * yuvH / 4;

            //拷贝Y分量
            for (i = 0; i < waterMarkH; i++){ //每次循环一次，扫描一行数据
                memcpy(destY+startX+(i+startY)*yuvW, waterY+i*waterMarkW, waterMarkW); //y值覆盖
            }

            for (i = 0; i < waterMarkH/2; i++){ //每次循环一次，扫描一行数据
                //拷贝U分量
                memcpy(destU+(i+startY/2)*(yuvW/2)+startX/2, waterU+i*(waterMarkW/2), waterMarkW/2);
                //拷贝V分量
                memcpy(destV+(i+startY/2)*(yuvW/2)+startX/2, waterV+i*(waterMarkW/2), waterMarkW/2);
            }

#ifdef DUMP_OUTPUT
            char output[256];
         memset(output,0,sizeof(output));
         sprintf(output,"water_i420_%dx%d.yuv",yuvW,yuvH);
         FILE *outPutFp = fopen(output, "w+");
         fwrite(yuvData, 1, yuvW*yuvH*3/2, outPutFp);
         fclose(outPutFp);
#endif
            break;
        }
        case FORMAT_YV12:{
            int i = 0;
            unsigned char* waterY = waterMarkData;
            unsigned char* waterV = waterY + waterMarkW * waterMarkH;
            unsigned char* waterU = waterV + waterMarkW * waterMarkH / 4;

            unsigned char* destY = yuvData;
            unsigned char* destV = destY + yuvW*yuvH;
            unsigned char* destU = destV + yuvW * yuvH / 4;

            //拷贝Y分量
            for (i = 0; i < waterMarkH; i++){ //每次循环一次，扫描一行数据
                memcpy(destY+startX+(i+startY)*yuvW, waterY+i*waterMarkW, waterMarkW); //y值覆盖
            }

            for (i = 0; i < waterMarkH/2; i++){ //每次循环一次，扫描一行数据
                //拷贝V分量
                memcpy(destV+(i+startY/2)*(yuvW/2)+startX/2, waterV+i*(waterMarkW/2), waterMarkW/2);
                //拷贝U分量
                memcpy(destU+(i+startY/2)*(yuvW/2)+startX/2, waterU+i*(waterMarkW/2), waterMarkW/2);
            }

#ifdef DUMP_OUTPUT
            char output[256];
         memset(output,0,sizeof(output));
         sprintf(output,"water_yv12_%dx%d.yuv",yuvW,yuvH);
         FILE *outPutFp = fopen(output, "w+");
         fwrite(yuvData, 1, yuvW*yuvH*3/2, outPutFp);
         fclose(outPutFp);
#endif
            break;
        }
        default:
            break;
    }
}


#ifdef __cpluscplus
}
#endif