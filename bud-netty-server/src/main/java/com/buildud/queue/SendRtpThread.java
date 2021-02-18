package com.buildud.queue;


import com.buildud.bean.AbstractQueueBean;
import com.buildud.service.IBudCodeService;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class SendRtpThread extends Thread {

    public static final Logger log = LoggerFactory.getLogger(SendRtpThread.class);

    private AbstractQueueBean<byte[]> dudQueueBean;
    private IBudCodeService dudCodeService;
    private InetSocketAddress dstVideoRtpAddr;
    private Channel sendChannel;

    public SendRtpThread(IBudCodeService dudCodeService, AbstractQueueBean<byte[]> dudQueueBean, InetSocketAddress dstVideoRtpAddr, Channel sendChannel) {
        this.dudCodeService = dudCodeService;
        this.dudQueueBean = dudQueueBean;
        this.dstVideoRtpAddr = dstVideoRtpAddr;
        this.sendChannel = sendChannel;
    }

    @Override
    public void run() {
        if (dudCodeService==null){
            log.error("plase set dudCodeService!");
            return;
        }
        if (dudQueueBean==null){
            log.error("plase create dudQueueBean!");
            return;
        }
        try {
            log.info("send rtp start!");
            dudCodeService.sendRtp(dudQueueBean,sendChannel);
            log.info("send rtp end!");
        } catch (Exception e) {
            log.error(e.getMessage(),e.getCause());
        }
        dudQueueBean.setRtpCodeReadEnd(true);
        log.info("rtp queue read end!");
        log.info("video play end!");
        dudQueueBean.cleanRtpQueue();
        log.info("rtp queue close!");
    }

}
