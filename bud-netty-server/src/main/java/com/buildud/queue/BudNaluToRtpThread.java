package com.buildud.queue;


import com.buildud.bean.AbstractQueueBean;
import com.buildud.exception.BudNettyServerException;
import com.buildud.service.IBudCodeService;
import com.buildud.tools.RtpUtils;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BudNaluToRtpThread extends Thread {

    public static final Logger log = LoggerFactory.getLogger(BudNaluToRtpThread.class);

    private AbstractQueueBean<byte[]> dudQueueBean;

    public BudNaluToRtpThread(AbstractQueueBean<byte[]> dudQueueBean) {
        this.dudQueueBean = dudQueueBean;
    }

    @Override
    public void run() {
        if (dudQueueBean==null){
            log.error("plase create dudQueueBean!");
            return;
        }
        if (dudQueueBean.getNaluQueue()==null){
            log.error("plase create naluQueue!");
            return;
        }
        byte[] buffer = null;
        try {
            log.info("nalu data read start!");
            while (!dudQueueBean.isStopSate()){
                if (dudQueueBean.getNaluQueue().isEmpty()){
                    continue;
                }

                buffer = dudQueueBean.getNaluQueue().poll();
                if (buffer==null){
                    continue;
                }
                RtpUtils.naluToRtp(buffer,dudQueueBean);
            }
            log.info("rtp data write start!");
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage(),e.getCause());
        }finally {
            dudQueueBean.setRtpCodeWriteEnd(true);
            log.debug("rtp data write end!");
        }
    }
}
