package com.buildud.tools;

import com.buildud.exception.BudScreenShotException;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class ScreenShotUtils {

    public static BufferedImage screenShot(Robot robot, Rectangle rectangle){
        return robot.createScreenCapture(rectangle);
    }

    public static void screenShot(String filePath,Robot robot, Rectangle rectangle,String format) throws IOException {
        BufferedImage screenCapture = screenShot(robot,rectangle);
        File f = new File(filePath);
        ImageIO.write(screenCapture, format, f);
    }

    public static void screenShot(String filePath,Robot robot) throws IOException {
        Rectangle rectangle = getRectangle();
        BufferedImage screenCapture = screenShot(robot,rectangle);
        File f = new File(filePath);
        ImageIO.write(screenCapture, "JPEG", f);
    }

    /**
     * 在文件路径下面，每秒生成相应数量及格式的图片文件
     * @param filePath 保存文件路径
     * @param shot_num 每秒截屏次数
     * @param format 文件格式
     * @return 返回所有的文件名称
     */
    public static List<String> screenShot(String filePath,int shot_num,String format) throws BudScreenShotException {
        if (filePath==null||"".equals(filePath.trim())){
            throw new BudScreenShotException("file path not null!");
        }
        if (shot_num<1){
            throw new BudScreenShotException("shot num params error,set >0 value!");
        }
        File file = new File(filePath);
        if (!file.exists()){
            file.mkdirs();
        }
        Robot robot = null;
        Rectangle rectangle = null;
        List<String> list = null;
        try {
            robot = new Robot();
            rectangle = getRectangle();
            list = new ArrayList<String>();
            for (int i=0;i<shot_num;i++){
                String filePathName = filePath+File.separator+System.currentTimeMillis()+"."+format;
                screenShot(filePathName,robot,rectangle,format);
                list.add(filePathName);
                try {
                    Thread.sleep(1000/shot_num);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return list;
        } catch (AWTException e) {
            deleteExceptionFile(list);
            e.printStackTrace();
        } catch (IOException e) {
            deleteExceptionFile(list);
            e.printStackTrace();
        }
        return null;
    }

    private static void deleteExceptionFile(List<String> list){
        if (list==null||list.size()<1){
            return ;
        }
        File file = null;
        for (String filePathName:list){
            file = new File(filePathName);
            if (file.exists()){
                file.delete();
            }
        }
    }

    public static boolean screenShot(String filePath){
        return screenShot(filePath,"JPEG");
    }

    public static boolean screenShot(String filePath,String format){
        Robot robot = null;
        try {
            robot = new Robot();
            Rectangle rectangle = getRectangle();
            BufferedImage screenCapture = screenShot(robot,rectangle);
            File f = new File(filePath);
            ImageIO.write(screenCapture, format, f);
            return true;
        } catch (AWTException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Rectangle getRectangle(){
        int[] wh = getScreenWidthAndHeight();
        return new Rectangle(wh[0],wh[1]);
    }

    public static int[] getScreenWidthAndHeight(){
        Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int)screensize.getWidth();
        int height = (int)screensize.getHeight();
        int[] wh = new int[2];
        wh[0] = width;
        wh[1] = height;
        return wh;
    }

    public static FFmpegFrameGrabber createGrabber(int frameRate){
        return createGrabber(null,null,0,0,true,frameRate);
    }

    public static FFmpegFrameGrabber createGrabber(String offset_x,String offset_y,int width,int height,boolean draw_mouse,int frameRate){
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("desktop");
        grabber.setFormat("gdigrab");
        // 证确设置帧率方法，直接设置60帧每秒的高帧率
        grabber.setOption("framerate", String.valueOf(frameRate));
        if (offset_x!=null&&!"".equals(offset_x.trim())){
            // 截屏起始点X，全屏录制不设置此参数
            grabber.setOption("offset_x", offset_x);
        }
        if (offset_y!=null&&!"".equals(offset_y.trim())){
            // 截屏起始点Y，全屏录制不设置此参数
            grabber.setOption("offset_y", "100");
        }
        if (width>0){
            // 截取的画面宽度，不设置此参数默认为全屏
            grabber.setImageWidth(width);
        }
        if (height>0){
            grabber.setImageHeight(height);// 截取的画面高度，不设置此参数默认为全屏
        }
        if (draw_mouse){
            grabber.setOption("draw_mouse", "1");//绘制鼠标
        }else{
            grabber.setOption("draw_mouse", "0");//隐藏鼠标
        }
        return grabber;
    }

    public static FFmpegFrameRecorder createRecorder(String bOut,int frameRate,int width,int height){
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(bOut,width,height);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // 28
        recorder.setFormat("h264");
        recorder.setSampleRate(44100);
        recorder.setFrameRate(frameRate);
        recorder.setVideoQuality(0);
        recorder.setVideoOption("crf", "23");
        // 2000 kb/s, 720P视频的合理比特率范围
        recorder.setVideoBitrate(1000000);
        recorder.setVideoOption("preset", "slow");
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); // yuv420p
        return recorder;
    }

    public static FFmpegFrameRecorder createRecorder(OutputStream bOut,int frameRate,int width,int height){
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(bOut,width,height);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // 28
        recorder.setFormat("h264");
        recorder.setSampleRate(44100);
        recorder.setFrameRate(frameRate);
        recorder.setVideoQuality(0);
        recorder.setVideoOption("crf", "23");
        // 2000 kb/s, 720P视频的合理比特率范围
        recorder.setVideoBitrate(4*1024*1024);
        /**
         * 权衡quality(视频质量)和encode speed(编码速度) values(值)： ultrafast(终极快),superfast(超级快),
         * veryfast(非常快), faster(很快), fast(快), medium(中等), slow(慢), slower(很慢),
         * veryslow(非常慢)
         * ultrafast(终极快)提供最少的压缩（低编码器CPU）和最大的视频流大小；而veryslow(非常慢)提供最佳的压缩（高编码器CPU）的同时降低视频流的大小
         * 参考：https://trac.ffmpeg.org/wiki/Encode/H.264 官方原文参考：-preset ultrafast as the
         * name implies provides for the fastest possible encoding. If some tradeoff
         * between quality and encode speed, go for the speed. This might be needed if
         * you are going to be transcoding multiple streams on one machine.
         */
        recorder.setVideoOption("preset", "veryfast");
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); // yuv420p
        return recorder;
    }

}
