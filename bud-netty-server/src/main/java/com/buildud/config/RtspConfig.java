package com.buildud.config;

import com.buildud.handler.BudRtspHandler;
import com.buildud.tools.ScreenShotUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;


@Component
@ConfigurationProperties(prefix="netty",ignoreUnknownFields = false)
public class RtspConfig {
    public static final Logger log = LoggerFactory.getLogger(RtspConfig.class);

    public static final int BIZGROUPSIZE = Runtime.getRuntime().availableProcessors() * 2;

//    private Integer rtspPort=554;
//    private Integer rtspTcpPort=6793;
//    private Integer rtspIdletime=600;
//    private Integer rtpPort=54000;
//    private Integer rtpIdletime=10;
//    private Integer rtcpIdletime=1000;
//    private Integer listenGroup=2;  //用于分配处理业务线程的线程组个数
//    private Integer workerGroup=4;							//worker的线程数
//    private String playFilePath="D:\\\\";  //播放文件路径
//
//    private int fps = 20;								//默认帧率
//    private int mtu=1442;                             //最大传输单元
//    private int rtpSendSleepTime = 1000/fps;                //rtp包发送间隔休眠时间

    private Integer rtspPort;
    private Integer rtspTcpPort;
    private Integer rtspIdletime;
    private Integer rtpPort;
    private Integer rtpIdletime;
    private Integer rtcpIdletime;
    private Integer listenGroup;  //用于分配处理业务线程的线程组个数
    private Integer workerGroup;							//worker的线程数
    private String playFilePath;  //播放文件路径

    private int fps = 20;								//默认帧率
    private int mtu=1442;                             //最大传输单元
    private int rtpSendSleepTime = 1000/fps;                //rtp包发送间隔休眠时间

    public static Integer rtcpIdletime_;
    public static Integer rtpPort_;
    public static String playFilePath_;
    public static int fps_;
    public static int mtu_;

    public static int screenWidth=0;
    public static int screenHeigth=0;

    public RtspConfig() {
    }

    public String localHostIp;


    public void setRtspIdletime(Integer rtspIdletime) {
        this.rtspIdletime = rtspIdletime;
    }

    public void setRtpPort(Integer rtpPort) {
        this.rtpPort = rtpPort;
        RtspConfig.rtpPort_=rtpPort;
    }

    public void setRtpIdletime(Integer rtpIdletime) {
        this.rtpIdletime = rtpIdletime;
    }

    public void setRtcpIdletime(Integer rtcpIdletime) {
        this.rtcpIdletime = rtcpIdletime;
        RtspConfig.rtcpIdletime_ = rtcpIdletime;
    }

    public  void setWorkerGroup(Integer workerGroup) {
        this.workerGroup = workerGroup;
    }

    public  void setPlayFilePath(String playFilePath) {
        this.playFilePath = playFilePath;
        RtspConfig.playFilePath_ = playFilePath;
    }

    public  void setFps(int fps) {
        this.fps = fps;
        this.rtpSendSleepTime = 1000/fps;
        RtspConfig.fps_ = fps;
    }

    public  void setMtu(int mtu) {
        this.mtu = mtu;
        RtspConfig.mtu_ = mtu;
    }

    public  void setRtpSendSleepTime() {
        rtpSendSleepTime = 1000/fps;
    }

    public  int getRtpSendSleepTime() {
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

    public  Integer getListenGroup() {
        if (listenGroup>BIZGROUPSIZE){
            listenGroup = BIZGROUPSIZE;
        }
        return listenGroup;
    }

    public  void setListenGroup(Integer listenGroup) {
        this.listenGroup = listenGroup;
        if (listenGroup>BIZGROUPSIZE){
            this.listenGroup = BIZGROUPSIZE;
        }
    }

    public  void setLocalHostIp(String localHostIp) {
        this.localHostIp = localHostIp;
    }

    public Integer getRtspPort() {
        return rtspPort;
    }

    public Integer getRtspTcpPort() {
        return rtspTcpPort;
    }

    public Integer getRtspIdletime() {
        return rtspIdletime;
    }

    public Integer getRtpPort() {
        return rtpPort;
    }

    public Integer getRtpIdletime() {
        return rtpIdletime;
    }

    public Integer getRtcpIdletime() {
        return rtcpIdletime;
    }

    public Integer getWorkerGroup() {
        return workerGroup;
    }

    public String getPlayFilePath() {
        return playFilePath;
    }

    public int getFps() {
        return fps;
    }

    public int getMtu() {
        return mtu;
    }

    public void setRtspPort(Integer rtspPort) {
        this.rtspPort = rtspPort;
    }

    public void setRtspTcpPort(Integer rtspTcpPort) {
        this.rtspTcpPort = rtspTcpPort;
    }

    public void setRtpSendSleepTime(int rtpSendSleepTime) {
        this.rtpSendSleepTime = rtpSendSleepTime;
    }

    public static Integer getRtcpIdletime_() {
        return rtcpIdletime_;
    }

    public static void setRtcpIdletime_(Integer rtcpIdletime_) {
        RtspConfig.rtcpIdletime_ = rtcpIdletime_;
    }

    public static Integer getRtpPort_() {
        return rtpPort_;
    }

    public static void setRtpPort_(Integer rtpPort_) {
        RtspConfig.rtpPort_ = rtpPort_;
    }

    public static String getPlayFilePath_() {
        return playFilePath_;
    }

    public static void setPlayFilePath_(String playFilePath_) {
        RtspConfig.playFilePath_ = playFilePath_;
    }

    public static int getFps_() {
        return fps_;
    }

    public static void setFps_(int fps_) {
        RtspConfig.fps_ = fps_;
    }

    public static int getMtu_() {
        return mtu_;
    }

    public static void setMtu_(int mtu_) {
        RtspConfig.mtu_ = mtu_;
    }

    public String getLocalHostIp() {
        return localHostIp;
    }
}
