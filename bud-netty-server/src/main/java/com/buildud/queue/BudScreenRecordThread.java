package com.buildud.queue;


import com.buildud.bean.AbstractQueueBean;
import com.buildud.config.RtspConfig;
import com.buildud.io.BudByteArrayOutputStream;
import com.buildud.javacv.BudScreenRecordFrameGrabber;
import com.buildud.javacv.BudScreenRecordFrameRecorder;
import com.buildud.tools.ScreenShotUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;


public class BudScreenRecordThread extends Thread {

    public static final Logger log = LoggerFactory.getLogger(BudScreenRecordThread.class);

    private AbstractQueueBean<byte[]> dudQueueBean;


    public BudScreenRecordThread(AbstractQueueBean<byte[]> dudQueueBean) {
        this.dudQueueBean = dudQueueBean;
    }

    @Override
    public void run() {
        if (dudQueueBean==null){
            log.error("plase create dudQueueBean!");
            return;
        }
        if (dudQueueBean.getNaluQueue()==null){
            dudQueueBean.createNaluQueue();
        }
        BudScreenRecordFrameGrabber grabber = null;
        BudScreenRecordFrameRecorder recorder = null;
        try {
            grabber = new BudScreenRecordFrameGrabber(RtspConfig.fps_);
            grabber.start();

            BudByteArrayOutputStream out = new BudByteArrayOutputStream(dudQueueBean);
            recorder = new BudScreenRecordFrameRecorder(out,RtspConfig.screenWidth,RtspConfig.screenHeigth,RtspConfig.fps_);
            recorder.start();

            Frame frame = null;
            while (!dudQueueBean.isStopSate()) {
                if((frame = grabber.grabFrame()) != null){
                    recorder.record(frame);
                }
            }
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        } catch (FrameRecorder.Exception e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (grabber!=null){
                    grabber.stop();
                }
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
            try {
                if (recorder!=null){
                    recorder.stop();
                }
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }
    }
}
