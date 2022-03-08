This is a video & string player based on ijkplayer.
All the codes are refactor with androidx, no need to import other JAVA packages.



基于ijkplayer 0.8.8版本(github 2021年11月版本)编译的全平台全协议JNI libijkffmpeg.so。
支持open ssl, h265, rm, rmvb, rtsp rtmp。直播与本地播放全部支持。
编译的平台有：arm64-v8a armeabi armeabi-v7a x86 x86_64
已经用gradle打包成一个android module，可以直接编译成一个aar,提供其他java或者kotlin代码调用。
如果已经有项目，直接把文件夹里面的lib拷贝出来即可。

支持文件浏览与打开流媒体链接。播放器支持调速，快进退，悬浮框


DEMO APK:
https://download.csdn.net/download/suixin______/78259690
