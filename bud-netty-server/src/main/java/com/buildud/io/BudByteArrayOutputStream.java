package com.buildud.io;

import com.buildud.bean.AbstractQueueBean;
import com.buildud.queue.BudByteQueue;
import com.buildud.tools.FileUtils;
import com.buildud.tools.RtspUtils;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class BudByteArrayOutputStream extends ByteArrayOutputStream {

    private BudByteQueue naluQueue;
    private byte[] upEndData = null;
    private AbstractQueueBean<byte[]> dudQueueBean;

    public BudByteArrayOutputStream(){}

    public BudByteArrayOutputStream(BudByteQueue naluQueue) {
        this.naluQueue = naluQueue;
    }

    public BudByteArrayOutputStream(AbstractQueueBean<byte[]> dudQueueBean) {
        this.dudQueueBean = dudQueueBean;
    }

    @Override
    public void write(int b){
        super.write(b);
    }

    @Override
    public synchronized void write(byte b[], int off, int len) {
        System.out.println("start time:"+System.currentTimeMillis());
        byte[] decodeByte = b;
        if (upEndData!=null&&upEndData.length>0){
            int length = upEndData.length+b.length;
            decodeByte = new byte[length];
            System.arraycopy(upEndData, 0, decodeByte, 0, upEndData.length);
            System.arraycopy(b, 0, decodeByte, upEndData.length, b.length);
            upEndData = null;
        }
        upEndData = RtspUtils.pareNalu(decodeByte,dudQueueBean);
        System.out.println("  end time:"+System.currentTimeMillis());
    }
}
