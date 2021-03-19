package com.buildud.config;

import com.buildud.tools.ScreenShotUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix="netty")
public class RtspConfig {
    public static Integer rtspPort=554;
    public static Integer rtspIdletime=600;
    public static Integer rtpPort=54000;
    public static Integer rtpIdletime=10;
    public static Integer rtcpIdletime=1000;
    public static Integer workerGroup=5;							//worker的线程数
    public static String playFilePath="D:\\\\";  //播放文件路径
    public static int fps = 20;								//默认帧率
    public static int mtu=1442;                             //最大传输单元
    public static int rtpSendSleepTime = 1000/fps;                //rtp包发送间隔休眠时间

    public static int screenWidth=0;
    public static int screenHeigth=0;

    public static void setRtspPort(Integer rtspPort) {
        RtspConfig.rtspPort = rtspPort;
    }

    public static void setRtspIdletime(Integer rtspIdletime) {
        RtspConfig.rtspIdletime = rtspIdletime;
    }

    public static void setRtpPort(Integer rtpPort) {
        RtspConfig.rtpPort = rtpPort;
    }

    public static void setRtpIdletime(Integer rtpIdletime) {
        RtspConfig.rtpIdletime = rtpIdletime;
    }

    public static void setRtcpIdletime(Integer rtcpIdletime) {
        RtspConfig.rtcpIdletime = rtcpIdletime;
    }

    public static void setWorkerGroup(Integer workerGroup) {
        RtspConfig.workerGroup = workerGroup;
    }

    public static void setPlayFilePath(String playFilePath) {
        RtspConfig.playFilePath = playFilePath;
    }

    public static void setFps(int fps) {
        RtspConfig.fps = fps;
        RtspConfig.rtpSendSleepTime = 1000/fps;
    }

    public static void setMtu(int mtu) {
        RtspConfig.mtu = mtu;
    }

    public static void setRtpSendSleepTime() {
        rtpSendSleepTime = 1000/fps;
    }

    public static int getRtpSendSleepTime() {
        return rtpSendSleepTime;
    }

    public static int getScreenWidth() {
        return screenWidth;
    }

    public static void setScreenWidth(int screenWidth) {
        RtspConfig.screenWidth = screenWidth;
    }

    public static int getScreenHeigth() {
        return screenHeigth;
    }

    public static void setScreenHeigth(int screenHeigth) {
        RtspConfig.screenHeigth = screenHeigth;
    }
}
