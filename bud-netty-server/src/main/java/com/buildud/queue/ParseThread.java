package com.buildud.queue;


import com.buildud.bean.AbstractQueueBean;
import com.buildud.exception.BudNettyServerException;
import com.buildud.service.IBudCodeService;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ParseThread extends Thread {

    public static final Logger log = LoggerFactory.getLogger(ParseThread.class);

    private String filePath;
    private AbstractQueueBean<byte[]> dudQueueBean;
    private IBudCodeService dudCodeService;
    private Channel sendChannel;

    public ParseThread(String filePath, IBudCodeService dudCodeService, AbstractQueueBean<byte[]> dudQueueBean, Channel sendChannel) {
        this.filePath = filePath;
        this.dudCodeService = dudCodeService;
        this.dudQueueBean = dudQueueBean;
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
            dudCodeService.readFileToQueue(filePath,dudQueueBean,sendChannel);
            Thread.sleep(1000L);
        } catch (BudNettyServerException e) {
            e.printStackTrace();
            log.error(e.getMessage(),e.getCause());
        } catch (IOException e) {
            e.printStackTrace();
            log.error(e.getMessage(),e.getCause());
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error(e.getMessage(),e.getCause());
        }
        log.info("rtp queue write end!");
        dudQueueBean.setRtpCodeWriteEnd(true);
    }
}
