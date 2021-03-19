package com.buildud.queue;


import com.buildud.bean.AbstractQueueBean;
import com.buildud.config.RtspConfig;
import com.buildud.io.BudByteArrayOutputStream;
import com.buildud.javacv.BudScreenRecordFrameGrabber;
import com.buildud.javacv.BudScreenRecordFrameRecorder;
import com.buildud.tools.RtpUtils;
import com.buildud.tools.RtspUtils;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BudByteToNaluThread extends Thread {

    public static final Logger log = LoggerFactory.getLogger(BudByteToNaluThread.class);

    private AbstractQueueBean<byte[]> dudQueueBean;


    public BudByteToNaluThread(AbstractQueueBean<byte[]> dudQueueBean) {
        this.dudQueueBean = dudQueueBean;
    }

    @Override
    public void run() {
        if (dudQueueBean==null){
            log.error("plase create dudQueueBean!");
            return;
        }
        byte[] buffer = null;
        try {
            log.info("byte data read start!");
            while (!dudQueueBean.isStopSate()){
                if (dudQueueBean.getByteQueue().isEmpty()){
                    continue;
                }
                buffer = dudQueueBean.getByteQueue().poll();
                if (buffer==null||buffer.length<1){
                    continue;
                }
                System.out.println("Send start time:"+System.currentTimeMillis());
                RtspUtils.pareNaluNoReturn(buffer,dudQueueBean);
                System.out.println("Send   end time:"+System.currentTimeMillis());
            }
            log.info("byte data read end!");
        }catch (Exception e){
            log.error(e.getMessage(),e.getCause());
        }
    }
}
