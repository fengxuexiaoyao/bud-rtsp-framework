package com.buildud.javacv;

import com.buildud.tools.ScreenShotUtils;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.springframework.boot.CommandLineRunner;

import java.io.OutputStream;


public class BudScreenRecordFrameRecorder extends FFmpegFrameRecorder{

    public BudScreenRecordFrameRecorder(OutputStream outputStream, int imageWidth, int imageHeight) {
        this(outputStream, imageWidth, imageHeight,25);
    }

    public BudScreenRecordFrameRecorder(OutputStream outputStream, int imageWidth, int imageHeight,int frameRate) {
        super(outputStream, imageWidth, imageHeight);
        setDefaultParams(frameRate);
    }

    public BudScreenRecordFrameRecorder(String outputStream, int imageWidth, int imageHeight,int frameRate) {
        super(outputStream, imageWidth, imageHeight);
        setDefaultParams(frameRate);
    }

    private void setDefaultParams(int frameRate){
        this.setVideoCodec(avcodec.AV_CODEC_ID_H264); // 28
        this.setFormat("h264");
        this.setSampleRate(44100);
        this.setFrameRate(frameRate);
        this.setVideoQuality(0);
        this.setVideoOption("crf", "23");
        // 2000 kb/s, 720P视频的合理比特率范围
        this.setVideoBitrate(4*1024*1024);
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
        this.setVideoOption("preset", "ultrafast");
        this.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); // yuv420p
    }
}
