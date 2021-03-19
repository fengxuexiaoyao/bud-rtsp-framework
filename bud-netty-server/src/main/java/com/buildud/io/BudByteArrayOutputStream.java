package com.buildud.io;

import com.buildud.bean.AbstractQueueBean;
import com.buildud.handler.BudRtspHandler;
import com.buildud.queue.BudByteQueue;
import com.buildud.tools.RtspUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BudByteArrayOutputStream extends ByteArrayOutputStream {

    public static final Logger log = LoggerFactory.getLogger(BudByteArrayOutputStream.class);

    private byte[] upEndData = null;
    private AbstractQueueBean<byte[]> dudQueueBean;

    private List<byte[]> list = new ArrayList<>();
    private int size=0;

    public BudByteArrayOutputStream(){}


    public BudByteArrayOutputStream(AbstractQueueBean<byte[]> dudQueueBean) {
        this.dudQueueBean = dudQueueBean;
    }

    @Override
    public void write(int b){
        super.write(b);
    }

    @Override
    public synchronized void write(byte b[], int off, int len) {
        log.debug("length>>"+b.length);
        if (b.length==4096){
            list.add(b);
            size+=4096;
            return;
        }
        size += b.length;
        list.add(b);
        byte[] decodeByte = new byte[size];
        int i = 0;
        for (byte[] by:list){
            System.arraycopy(by, 0, decodeByte, i, by.length);
            i+=by.length;
        }

        list.clear();
        size=0;
        try {
            dudQueueBean.getByteQueue().put(decodeByte);
        } catch (InterruptedException e) {
            log.debug(e.getMessage(),e.getCause());
        }
    }
}
