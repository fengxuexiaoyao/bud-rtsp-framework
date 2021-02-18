package com.buildud.javacv;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BudScreenRecordFrameGrabber extends FFmpegFrameGrabber implements CommandLineRunner {

    private static volatile BudScreenRecordFrameGrabber INSTANCE;

    public BudScreenRecordFrameGrabber(){
        this(25);
    }

    public BudScreenRecordFrameGrabber(int frameRate){
        this(true,frameRate);
    }

    public BudScreenRecordFrameGrabber(boolean draw_mouse,int frameRate){
        this(null,null,0,0,draw_mouse,frameRate);
    }

    public BudScreenRecordFrameGrabber(String offset_x,String offset_y,int width,int height,boolean draw_mouse,int frameRate) {
        super("desktop");
        setDefaultParams(offset_x,offset_y,width,height,draw_mouse,frameRate);
    }

    private void setDefaultParams(String offset_x,String offset_y,int width,int height,boolean draw_mouse,int frameRate){
        this.setFormat("gdigrab");
        // 证确设置帧率方法，直接设置60帧每秒的高帧率
        this.setOption("framerate", String.valueOf(frameRate));
        if (offset_x!=null&&!"".equals(offset_x.trim())){
            // 截屏起始点X，全屏录制不设置此参数
            this.setOption("offset_x", offset_x);
        }
        if (offset_y!=null&&!"".equals(offset_y.trim())){
            // 截屏起始点Y，全屏录制不设置此参数
            this.setOption("offset_y", "100");
        }
        if (width>0){
            // 截取的画面宽度，不设置此参数默认为全屏
            this.setImageWidth(width);
        }
        if (height>0){
            this.setImageHeight(height);// 截取的画面高度，不设置此参数默认为全屏
        }
        if (draw_mouse){
            this.setOption("draw_mouse", "1");//绘制鼠标
        }else{
            this.setOption("draw_mouse", "0");//隐藏鼠标
        }
    }

    public synchronized static BudScreenRecordFrameGrabber getInstance() {
        return getInstance(25);
    }

    public synchronized static BudScreenRecordFrameGrabber getInstance(int frameRate){
        return getInstance(true,frameRate);
    }

    public synchronized static BudScreenRecordFrameGrabber getInstance(boolean draw_mouse,int frameRate) {
        return getInstance(null,null,0,0,draw_mouse,frameRate);
    }

    public synchronized static BudScreenRecordFrameGrabber getInstance(String offset_x,String offset_y,int width,int height,boolean draw_mouse,int frameRate) {
        if (null == INSTANCE) {
            INSTANCE = new BudScreenRecordFrameGrabber(offset_x,offset_y,width,height,draw_mouse,frameRate);
        }
        return INSTANCE;
    }

    @Override
    public void run(String... strings) throws java.lang.Exception {
        BudScreenRecordFrameGrabber.getInstance().start();
    }
}
