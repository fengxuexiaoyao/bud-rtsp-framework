package com.buildud.demo;

import com.buildud.io.BudByteArrayOutputStream;
import com.buildud.tools.ScreenShotUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.bytedeco.javacv.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Created by fengxuexiaoyao on 2021/1/19.
 */
public class Test {

    public static void main(String[] args) {
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;
        try {
            grabber = ScreenShotUtils.createGrabber(25);
            grabber.start();

            BudByteArrayOutputStream out = new BudByteArrayOutputStream();
            recorder = ScreenShotUtils.createRecorder(out,25,1366,768);
            recorder.start();

            boolean status = true;
            Frame frame = null;
            long start_time = System.currentTimeMillis();
            System.out.println(DateFormatUtils.format(new Date(),"yyyy-MM-ss HH:mm:ss"));
            while (status&& (frame = grabber.grabFrame()) != null) {
                recorder.record(frame);

                if ((System.currentTimeMillis()-start_time)>100000L){
                    break;
                }
            }
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        } catch (FrameRecorder.Exception e) {
            e.printStackTrace();
        } finally {
            try {
                grabber.stop();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
            try {
                recorder.stop();
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static byte[] unshellNalu(byte[] nalu){
        if (nalu==null||nalu.length<0){
            return null;
        }
        int len = nalu.length;
        int offset =0;
        byte[] unshell_nalu_zero = new byte[len];
        int unshell_offset = 0;
        while (offset<len){
            if(offset+2<len&&
                    nalu[offset] == 0x00 &&
                    nalu[offset + 1] == 0x00 &&
                    nalu[offset + 2] == 0x03 ){
                unshell_nalu_zero[unshell_offset]=nalu[offset];
                unshell_nalu_zero[unshell_offset+1]=nalu[offset+1];
                offset+=3;
                unshell_offset+=2;
            }else{
                unshell_nalu_zero[unshell_offset]=nalu[offset];
                offset++;
                unshell_offset++;
            }
        }
        byte[] unshell_nalu = new byte[unshell_offset];
        System.arraycopy(unshell_nalu_zero, 0, unshell_nalu, 0, unshell_nalu.length);
        return unshell_nalu;
    }
}
