package com.buildud.bean;


import com.buildud.queue.BudByteQueue;
import com.buildud.queue.RtpBudQueue;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;

public abstract class AbstractQueueBean<E>{
    public abstract void init();
    public abstract void clean();
    public abstract void createRtpQueue();
    public abstract void createNaluQueue();
    public abstract void createByteQueue();
    public abstract boolean cleanRtpQueue();
    public abstract RtpBudQueue getRtpQueue();
    public abstract boolean cleanNaluQueue();
    public abstract BudByteQueue getNaluQueue();
    public abstract boolean cleanByteQueue();
    public abstract BudByteQueue getByteQueue();

    public abstract Short getSeqNum();
    public abstract void setSeqNum(Short seqNum);
    public abstract void autoIncrementSeqNum();
    public abstract int getSsrc();
    public abstract void setSsrc(int ssrc);
    public abstract Integer getTime();
    public abstract void setTime(Integer time);
    public abstract void autoIncrementTime();
    public abstract void autoIncrement();

    public abstract boolean isNaluCodeWriteEnd();
    public abstract void setNaluCodeWriteEnd(boolean naluCodeWriteEnd);
    public abstract boolean isNaluCodeReadEnd();
    public abstract void setNaluCodeReadEnd(boolean naluCodeReadEnd);
    public abstract boolean isRtpCodeWriteEnd();
    public abstract void setRtpCodeWriteEnd(boolean rtpCodeWriteEnd);
    public abstract boolean isRtpCodeReadEnd();
    public abstract void setRtpCodeReadEnd(boolean rtpCodeReadEnd);

    public abstract boolean isStopSate();
    public abstract void setStopSate(boolean stopSate);

    public abstract InetSocketAddress getDstVideoRtpAddr();
    public abstract Channel getSendChannel();
    public abstract String getCanelType();
}
