package com.buildud.queue;


import com.buildud.bean.AbstractQueueBean;
import com.buildud.service.IBudCodeService;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class BudSendRtpThread extends Thread {

    public static final Logger log = LoggerFactory.getLogger(BudSendRtpThread.class);

    private AbstractQueueBean<byte[]> dudQueueBean;
    private InetSocketAddress dstVideoRtpAddr;
    private Channel sendChannel;

    public BudSendRtpThread(AbstractQueueBean<byte[]> dudQueueBean, InetSocketAddress dstVideoRtpAddr, Channel sendChannel) {
        this.dudQueueBean = dudQueueBean;
        this.dstVideoRtpAddr = dstVideoRtpAddr;
        this.sendChannel = sendChannel;
    }

    @Override
    public void run() {
        if (dudQueueBean==null){
            log.error("plase create dudQueueBean!");
            return;
        }
        byte[] buffer = null;
        while (!dudQueueBean.isStopSate()){
            if (dudQueueBean.getRtpQueue().isEmpty()){
                try {
                    Thread.sleep(10L);
                }catch (Exception e){
                    e.printStackTrace();
                }
                continue;
            }
            try {
                DatagramPacket datagramPacket = dudQueueBean.getRtpQueue().poll();
                if (datagramPacket==null){
                    continue;
                }
                sendChannel.writeAndFlush(datagramPacket);
            }catch (Exception e){
                e.printStackTrace();
                log.error(e.getMessage(),e.getCause());
            }
        }
    }

}
