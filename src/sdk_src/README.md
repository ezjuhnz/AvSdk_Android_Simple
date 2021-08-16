
/****************SDK封装*******************/
1.视频编码的帧率设置无效,一直是25fps
2.合成器提供单独的接口,可以在用户没调用采集API时自己采集数据来合成mp4
3.各组件解耦,类都可以作为单独组件使用

4.原始音频pcm文件,音频数据回调
5.屏幕数据

/*************BUG记录***************/
1.将音频采样率从16000HZ改成44100HZ, aac音频文件的时间变长.
原因:AAC ADTS的频率下标没有对应修改;16000HZ对应下标8, 而44100HZ对应下标4

2.长时间(15秒~1分钟)运行程序报错
 java.lang.IllegalStateException: writeSampleData returned an error
        at android.media.MediaMuxer.nativeWriteSampleData(Native Method)
        at android.media.MediaMuxer.writeSampleData(MediaMuxer.java:473)
        at com.camera.sdk.AVmediaMuxer$2.run(AVmediaMuxer.java:105)
根据错误可以看出是混合器在写入数据时出错;在跟踪日志后,发现是音频数据在写入时,当前帧的时间戳(以us为单位)小于
上一次写入的时间戳,这是错误的根源.而导致这种错误的原因可能是
(1)线程执行顺序混乱                 否
(2)数据添加到队列的顺序出错          否
(3)编码后的时间戳出现混乱            是
音轨时间戳混乱: 时间戳是在编码时通过当前时间计算出来的.经测试,该时间没有出现混乱的情况.而出现混乱的是另一个时间戳-编码后的时间戳

3.某些机型无法正确写aac文件
原因:ByteBuffer的大小无法容纳原始pcm音频数据.估计是手机硬件配置高,采集的音频数据也更大
增加ByteBuffer的大小到8192字节可解决问题

4.摄像机采集在低光环境帧率下降到15帧,即使已经配置帧率为30帧了.

5.摄像头采集帧率调整为15帧时,h264文件播放时长变短,播放速度变快

6.将音频编码器的dequeueInputBuffer和dequeueOutputBuffer的等待时间改成无限时,关闭muxer出错.

//============================华丽分割线===============================//
/****音频参数****/
(1)采样率,如16000HZ,44100HZ //finished
(2)声道类型,如单声道,立体声   //finished
(3)码率                     //finished
(4)编码格式,如AAC,MP3        //只支持AAC格式,其它格式报错

/****视频参数****/
(1)分辨率                  //finished
(2)帧率                    //bug:降低帧率导致H264文件时间变短,播放速度变快
(3)码率                    //finished
(4)关键帧间隔               //已测试1,2,3关键帧间隔
(5)编码格式,如H264,H265


//待完成任务
1.用户给定文件的生成目录,如果目录不存在则先建目录再建文件//
2.Log优化:增加view控件显示和文件存储功能
3.mp4音频/视频在开始播放时卡顿
4.屏幕采集//
5.插水印,增加jpg,bmp,png格式支持
6.关键帧间隔检验
7.水印插入,提供坐标接口//
8.混合器支持只有音频或视频时的封装,添加支持单独传递数据的接口
9.编码器混合器不能使用单例模式

10.合成器不能单独控制，合成器结束方式
11.头文件必须统一在sdk
12.缺少日志文件
13.水印资源统一在sdk初始化



======================测试设备信息记录=========================
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

1.Redmi8a:安卓
info=Product: olivelite, CPU_ABI: armeabi-v7a,
TAGS: release-keys,
VERSION_CODES.BASE: 1,
MODEL: Redmi 8A,
SDK: 28,
VERSION.RELEASE: 9,
DEVICE: olivelite,
 DISPLAY: ZQL2116-olivelite-build-20200527212852,
BRAND: Xiaomi,
BOARD: QC_Reference_Phone,
FINGERPRINT: Xiaomi/olivelite/olivelite:9/PKQ1.190319.001/V11.0.10.0.PCPCNXM:user/release-keys,
ID: PKQ1.190319.001,
MANUFACTURER: Xiaomi,
 USER: builder

2.荣耀magic2:鸿蒙


