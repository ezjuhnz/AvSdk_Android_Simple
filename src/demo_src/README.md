Bug记录
1. java.lang.RuntimeException: Failure delivering result ResultInfo{who=null, request=1000, result=-1, data=Intent { (has extras) }}
 to activity {com.example.previewdemo/com.example.previewdemo.MainActivity}: java.lang.SecurityException: Media projections
 require a foreground service of type ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
在使用华为手机进行录屏时出现该错误.
解决方法: 将targetSdkVersion改成28及以下

2.E/MPEG4Writer: do not support out of order frames (timestamp: 4708360 < last: 4709468 for Audio track
      Dumping Audio track's last 10 frames timestamp and frame type
音轨时间戳混乱: 时间戳是在编码时通过当前时间计算出来的.经测试,该时间没有出现混乱的情况.而出现混乱的是另一个时间戳-编码后的时间戳


//测试所有参数
//写文件解耦
//音频,视频参数提供
