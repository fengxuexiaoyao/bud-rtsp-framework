package com.buildud.bean;


import com.buildud.config.RtspConfig;
import com.buildud.queue.BudByteQueue;
import com.buildud.queue.RtpBudQueue;
import com.buildud.tools.DateUtils;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;

public class BudRtspBean<E> extends AbstractQueueBean<E>{

    private BudByteQueue byteQueue;
    private BudByteQueue naluQueue;
    private RtpBudQueue rtpQueue;

    private Short seqNum=0;
    private int ssrc=0;
    private Integer time=0;

    private InetSocketAddress dstVideoRtpAddr;
    private Channel sendChannel;

    private final String canelType = "UDP";

    /**
     * naluQueue 通道数据是否写入结束
     */
    private boolean naluCodeWriteEnd=false;
    /**
     * naluQueue 通道数据是否读取结束
     */
    private boolean naluCodeReadEnd=false;
    /**
     * rtpQueue 通道数据是否写入结束
     */
    private boolean rtpCodeWriteEnd=false;
    /**
     * rtpQueue 通道数据是否读取结束
     */
    private boolean rtpCodeReadEnd=false;
    /**
     * 是否结束播放标识
     */
    private boolean stopSate = false;

    public BudRtspBean(InetSocketAddress dstVideoRtpAddr,Channel sendChannel) {
        this.dstVideoRtpAddr = dstVideoRtpAddr;
        this.sendChannel = sendChannel;
        init();
    }

    @Override
    public void init(){
        createRtpQueue();
        createNaluQueue();
        createByteQueue();
        initTime();
    }

    @Override
    public void clean(){
        cleanRtpQueue();
        cleanNaluQueue();
        cleanByteQueue();
        cleanTime();
    }

    public void initTime(){
        time = DateUtils.getMillisecondToInt();
    }

    public void cleanTime(){
        time = null;
    }

    @Override
    public void createRtpQueue(){
        rtpQueue = new RtpBudQueue();
    }

    @Override
    public boolean cleanRtpQueue() {
        if (rtpQueue!=null&&rtpQueue.isEmpty()){
            rtpQueue.clear();
            rtpQueue = null;
            return true;
        }
        return false;
    }

    @Override
    public void createNaluQueue(){
        naluQueue = new BudByteQueue();
    }

    @Override
    public boolean cleanNaluQueue() {
        if (naluQueue!=null&&naluQueue.isEmpty()){
            naluQueue.clear();
            naluQueue = null;
            return true;
        }
        return false;
    }

    @Override
    public void createByteQueue(){
        byteQueue = new BudByteQueue();
    }

    @Override
    public boolean cleanByteQueue() {
        if (byteQueue!=null&&byteQueue.isEmpty()){
            byteQueue.clear();
            byteQueue = null;
            return true;
        }
        return false;
    }

    @Override
    public RtpBudQueue getRtpQueue() {
        return rtpQueue;
    }


    @Override
    public BudByteQueue getNaluQueue() {
        return naluQueue;
    }

    @Override
    public BudByteQueue getByteQueue() {
        return byteQueue;
    }

    @Override
    public boolean isNaluCodeWriteEnd() {
        return naluCodeWriteEnd;
    }

    @Override
    public void setNaluCodeWriteEnd(boolean naluCodeWriteEnd) {
        this.naluCodeWriteEnd = naluCodeWriteEnd;
    }

    @Override
    public boolean isNaluCodeReadEnd() {
        return naluCodeReadEnd;
    }

    @Override
    public void setNaluCodeReadEnd(boolean naluCodeReadEnd) {
        this.naluCodeReadEnd = naluCodeReadEnd;
    }

    @Override
    public boolean isRtpCodeWriteEnd() {
        return rtpCodeWriteEnd;
    }

    @Override
    public void setRtpCodeWriteEnd(boolean rtpCodeWriteEnd) {
        this.rtpCodeWriteEnd = rtpCodeWriteEnd;
    }

    @Override
    public boolean isRtpCodeReadEnd() {
        return rtpCodeReadEnd;
    }

    @Override
    public void setRtpCodeReadEnd(boolean rtpCodeReadEnd) {
        this.rtpCodeReadEnd = rtpCodeReadEnd;
    }

    @Override
    public Short getSeqNum() {
        return seqNum;
    }

    @Override
    public void setSeqNum(Short seqNum) {
        this.seqNum = seqNum;
    }

    @Override
    public void autoIncrementSeqNum(){
        if (seqNum == 65535) {
            //由于seqNum会对应rtp包的序列号2字节
            //所以如果seqNum值超过2个字节上限重置为1，重新计数
            seqNum = (short)1;
        }else{
            this.seqNum++;
        }
    }

    @Override
    public int getSsrc() {
        return ssrc;
    }

    @Override
    public void setSsrc(int ssrc) {
        this.ssrc = ssrc;
    }

    @Override
    public Integer getTime() {
        return time;
    }

    @Override
    public void setTime(Integer time) {
        this.time = time;
    }

    @Override
    public void autoIncrementTime(){
        //90000为H264编码的时钟频率
        //时钟频率除以帧率，用于计算时间推进长度。
        this.time+=(90000/ RtspConfig.fps_);
    }

    @Override
    public void autoIncrement() {
        this.autoIncrementSeqNum();
        this.autoIncrementTime();
    }

    @Override
    public boolean isStopSate() {
        return stopSate;
    }

    @Override
    public void setStopSate(boolean stopSate) {
        this.stopSate = stopSate;
    }

    @Override
    public InetSocketAddress getDstVideoRtpAddr() {
        return dstVideoRtpAddr;
    }

    @Override
    public Channel getSendChannel() {
        return sendChannel;
    }

    @Override
    public String getCanelType() {
        return canelType;
    }
}
