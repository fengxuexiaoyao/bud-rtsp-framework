package com.buildud.service.impl;

import com.buildud.bean.AbstractQueueBean;
import com.buildud.config.RtspConfig;
import com.buildud.exception.BudNettyServerException;
import com.buildud.handler.BudRtspHandler;
import com.buildud.queue.RtpBudQueue;
import com.buildud.service.IBudCodeService;
import com.buildud.tools.RtpUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@Service
public class BudCodeServiceImpl implements IBudCodeService {

    public static final Logger log = LoggerFactory.getLogger(BudCodeServiceImpl.class);

    private static int fileCacheSize = 64*1024;     //每次读取文件的缓冲区大小

    @Override
    public boolean readFileToQueue(String filePath, AbstractQueueBean<byte[]> dudQueueBean,Channel sendChannel) throws BudNettyServerException, IOException, InterruptedException {
        if (filePath==null||"".equals(filePath.trim())){
            log.error("file path not null!");
            throw new BudNettyServerException("file path not null!");
        }
        File videoFile = new File(filePath);
        if (!videoFile.exists()){
            log.error("video file no exists!");
            throw new BudNettyServerException("video file no exists!");
        }
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(videoFile));
        return readFileToQueue(in,dudQueueBean,sendChannel);
    }

    /**
     * 对解析出来的nalu单元进行脱壳操作
     * @param nalu
     * @return
     */
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

    /**
     * 解析出当前数组中的所有nalu单元
     * @param decodeByte   需要解析的数组
     * @return
     */
    private byte[] pareNalu(byte[] decodeByte,AbstractQueueBean<byte[]> dudQueueBean,Channel sendChannel){
        if (decodeByte==null||decodeByte.length<0){
            return null;
        }
        int len = decodeByte.length;
        int start = 0;                             //下一个nalu的开始位置
        int offset = 0;                            //当前循环中的偏移量
        while (offset<len){
            // 循环到起始符位置
            if (offset+3<len&&
                    decodeByte[offset] == 0x00 &&
                    decodeByte[offset + 1] == 0x00 &&
                    decodeByte[offset + 2] == 0x00 &&
                    decodeByte[offset + 3] == 0x01 ) {
                if (offset==0) {
                    start = 4;
                    offset = 4;
                    continue;
                }

                byte[] nalu = new byte[offset -start];
                System.arraycopy(decodeByte, start, nalu, 0, nalu.length);
//                nalu = unshellNalu(nalu);
                naluToRtp(nalu,dudQueueBean,sendChannel);
                start = offset+4;
                offset += 4;

                continue;
            } else if(offset+2<len&&
                    decodeByte[offset] == 0x00 &&
                    decodeByte[offset + 1] == 0x00 &&
                    decodeByte[offset + 2] == 0x01 ){
                if (offset==0) {
                    start = 3;
                    offset = 3;
                    continue;
                }

                byte[] nalu = new byte[offset -start];
                System.arraycopy(decodeByte, start, nalu, 0, nalu.length);
//                nalu = unshellNalu(nalu);
                naluToRtp(nalu,dudQueueBean,sendChannel);
                start = offset+3;
                offset += 3;

                continue;
            }
            else {
                offset++;
            }
        }
        if (start==0){
            return decodeByte;
        }else if (start<len){
            byte[] upEndData = new byte[len-start];
            System.arraycopy(decodeByte, start, upEndData, 0, upEndData.length);
            return upEndData;
        }
        return null;
    }

    @Override
    public boolean readFileToQueue(BufferedInputStream in, AbstractQueueBean<byte[]> dudQueueBean,Channel sendChannel) throws BudNettyServerException, IOException, InterruptedException {
        if (in==null){
            log.error("input stream is null!");
            throw new BudNettyServerException("input stream is null!");
        }
        int len = 0;
        byte[] upEndData = null;
        try {
            byte[]  buffer = new byte[fileCacheSize];
            log.info("read file stream start!");

            int alllength = 0;
            while (!dudQueueBean.isStopSate()&&-1 != (len = in.read(buffer, 0, fileCacheSize))) {
                alllength+=len;
                byte[] decodeByte = null;
                if (upEndData!=null&&upEndData.length>0){
                    int length = upEndData.length+len;
                    decodeByte = new byte[length];
                    System.arraycopy(upEndData, 0, decodeByte, 0, upEndData.length);
                    System.arraycopy(buffer, 0, decodeByte, upEndData.length, len);
                    upEndData = null;
                    buffer = new byte[fileCacheSize];
                }else{
                    if (len<fileCacheSize){
                        decodeByte = new byte[len];
                        System.arraycopy(buffer, 0, decodeByte, 0, len);
                    }else{
                        decodeByte = buffer;
                    }
                }
                upEndData = pareNalu(decodeByte,dudQueueBean,sendChannel);
            }
            //最后遗留的nalu单元
            if (upEndData!=null&&upEndData.length>0){
//                byte[] nalu = unshellNalu(upEndData);
                naluToRtp(upEndData,dudQueueBean,sendChannel);
            }
            log.debug("file size:"+alllength);
            log.info("read file stream end!");
        } catch (IOException e) {
            log.error(e.getMessage(),e.getCause());
            throw e;
        } finally {
            try {
                in.close();
                log.info("file stream close!");
            } catch (IOException e) {
                log.error(e.getMessage(),e.getCause());
            }
        }
        return true;
    }

    /**
     * 通过nalu转为rtp包
     * 由于nalu太大超过mtu值进行分片处理
     * @param nalu   nalu数组
     * @param dudQueueBean  通道及播放序列相关值
     * @return
     */
    private void naluSliceToRtp(byte[] nalu, AbstractQueueBean<byte[]> dudQueueBean,Channel sendChannel){
        int iLen = nalu.length;
        //由于fu_identifier和fu_header会替换原有的头部，所以nalu尾部剩余数需要减1
        iLen--;
        int sliceNum = iLen/ RtspConfig.mtu_;
        int endSliceLength = iLen%RtspConfig.mtu_;
        if (endSliceLength>0){
            sliceNum++;
        }
        //如果nalu的长度大于mtu，那么需要分包
        //nalu分包后每个包需要包含FU Indicator分片标识和FU header分片头

        //获取nalu的header字节
        byte header = nalu[0];
        //获取NALU的2bit帧重要程度
        //0x60转为二进制为 01100000
        //如果头部为的16进制数值为0x67==二进制01100111
        // 01100000 & 01100111 两个数值进行与运算得到01100000,那么针程度的值为11
        byte nal_ref_idc = (byte) (header & 0x60);
        //获取nalu的类型数据
        //0x1f转为二进制为 00011111
        //如果头部为的16进制数值为0x67==二进制 01100111
        // 00011111 & 01100111 两个数值进行与运算得到00000111,那么帧类型为7
        byte nalu_type = (byte) (header & 0x1f);
        //FU Indicator包含F、NRI、Type
        //F、NRI取nalu的前3位
        //该nalu是分片所以Type为FU-A，FU-A对应的是28。详细可以参考NAL单元类型对应https://blog.csdn.net/heker2010/article/details/75419137
        byte fu_identifier = (byte) (nal_ref_idc + 28);
        //FU header包含S、E、R、Type
        //S为分包开始标识位，1代表所有分包的开始包，如果不是开始包为0
        //E为分包结束标识位，1代表所有分包的结束包，如果不是结束包为0
        //R为保留位，默认为0
        //Type取nalu中的Type
        byte fu_header = nalu_type;

        int i = 0;
        ByteBuffer bb = null;
        boolean mark = false;

        while (i<sliceNum){
            int start = i*RtspConfig.mtu_+1;

            byte[] dest = null;
            if (i==0) {
                //代表当前为所有分包的开始包
                //那么S、E、R三位为100，再补齐后5位变为1000 0000，转为16进制为0x80
                //那么0x80和nalu_type相加为当前包的fu_header真实值
                fu_header = (byte) (0x80 + nalu_type);
                //由于是分包的开始，所以这个分包的字节长度为mtu
                //再加上fu_identifier和fu_header两个字节，整个组成nalu分片包数据。
                bb = ByteBuffer.allocate(RtspConfig.mtu_ + 2);
                //用于保存nalu分包数据
                dest = new byte[RtspConfig.mtu_];
            }else if((i+1)==sliceNum){
                //当前分包为结束包，设置mark为true。
                mark = true;
                //分片结束包，S、E、R三位为010，再补齐后5位变为0100 0000，转为16进制为0x40
                //那么0x40和nalu_type相加为当前包的fu_header真实值
                fu_header = (byte) (0x40 + nalu_type);
                //由于是分包的结束，所以这个分包的字节长度为整个包与mtu取余数后的值
                //再加上fu_identifier和fu_header两个字节，整个组成nalu分片包数据。
                bb = ByteBuffer.allocate(endSliceLength + 2);
                //用于保存nalu分包数据
                dest = new byte[endSliceLength];
            }else {
                //分片中间包，S、E、R三位为000，再补齐后5位变为0000 0000，转为16进制为0x00
                //那么0x00和nalu_type相加为当前包的fu_header真实值
                fu_header = (byte) (0x00 + nalu_type);
                //由于是中间包，所以这个分包的字节长度为mtu
                //再加上fu_identifier和fu_header两个字节，整个组成nalu分片包数据。
                bb = ByteBuffer.allocate(RtspConfig.mtu_ + 2);
                //用于保存nalu分包数据
                dest = new byte[RtspConfig.mtu_];
            }
            //设置fu_identifier
            bb.put(fu_identifier);
            //设置fu_header
            bb.put(fu_header);
            try {
                dudQueueBean.autoIncrementSeqNum();
                //从nalu中拷贝出当前分包数据至分包数组dest中
                System.arraycopy(nalu, start, dest, 0, dest.length);
                //放置分包数据
                bb.put(dest);
                //生成rtp包
                byte[] rtpPackage = RtpUtils.makeH264Rtp(bb.array(),mark,dudQueueBean.getSeqNum(), dudQueueBean.getTime(), dudQueueBean.getSsrc());
//                this.sendRtp(rtpPackage,sendChannel);
                this.sendRtpQueue(rtpPackage,dudQueueBean.getRtpQueue());
            }catch (Exception e){
                e.printStackTrace();
                log.error(e.getMessage(),e.getCause());
            }
            i++;
        }

        //同一组分片包结束后自增
        //设置时间自增
        dudQueueBean.autoIncrementTime();

        try {
            Thread.sleep(1000/RtspConfig.fps_);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * 通过nalu转为rtp包
     * @param nalu
     * @param dudQueueBean
     */
    private void naluToRtp(byte[] nalu,AbstractQueueBean<byte[]> dudQueueBean,Channel sendChannel){
        if (nalu==null||nalu.length<0){
            return;
        }

        int iLen = nalu.length;

        int sliceNum = iLen/ RtspConfig.mtu_;
        int endSliceLength = iLen%RtspConfig.mtu_;
        if (endSliceLength>0){
            sliceNum++;
        }

        if (sliceNum==1){
            byte header = nalu[0];
            byte nalu_type = (byte) (header & 0x1f);
            boolean mark = false;
            boolean isAutoTime = false;
            if (nalu_type==0x07||nalu_type==0x08||nalu_type==0x06){
                dudQueueBean.autoIncrementSeqNum();
            }else if(nalu_type==0x05){
                mark = true;
                dudQueueBean.autoIncrementSeqNum();
            }else{
                mark = true;
                dudQueueBean.autoIncrementSeqNum();
                isAutoTime = true;
            }

            //单一RTP包，mark设置为false
            //生成rtp包
            byte[] rtpPackage = RtpUtils.makeH264Rtp(nalu,mark,dudQueueBean.getSeqNum(), dudQueueBean.getTime(), dudQueueBean.getSsrc());
//            sendRtp(rtpPackage,sendChannel);
            this.sendRtpQueue(rtpPackage,dudQueueBean.getRtpQueue());
            //设置时间自增
            if (isAutoTime){
                dudQueueBean.autoIncrementTime();
                try {
                    Thread.sleep(1000/RtspConfig.fps_);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return;
        }
        //分片处理nalu
        naluSliceToRtp(nalu,dudQueueBean,sendChannel);
    }

    /**
     * 发送rtp服务
     * @param rtpPack  要发送的rtp包
     * @param sendChannel  发送通道
     */
    private void sendRtp(byte[] rtpPack,Channel sendChannel){
        try{
            ByteBuf byteBuf = Unpooled.buffer(rtpPack.length);
            byteBuf.writeBytes(rtpPack);
            sendChannel.writeAndFlush(new DatagramPacket(byteBuf, BudRtspHandler.dstVideoRtpAddr));
            log.debug("send rtp pack:"+rtpPack.length);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage(),e.getCause());
        }
    }

    /**
     * 发送rtp服务
     * @param rtpPack  要发送的rtp包
     * @param rtpQueue  发送通道
     */
    private void sendRtpQueue(byte[] rtpPack,RtpBudQueue rtpQueue){
        if (rtpQueue==null){
            return;
        }
        try{
            ByteBuf byteBuf = Unpooled.buffer(rtpPack.length);
            byteBuf.writeBytes(rtpPack);
            DatagramPacket datagramPacket = new DatagramPacket(byteBuf, BudRtspHandler.dstVideoRtpAddr);
            rtpQueue.put(datagramPacket);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage(),e.getCause());
        }
    }

    @Override
    public void sendRtp(AbstractQueueBean<byte[]> dudQueueBean, Channel sendChannel) throws Exception {
        byte[] buffer = null;
        while (!dudQueueBean.isStopSate()){
            if (dudQueueBean.getRtpQueue().isEmpty()){
                try {
                    Thread.sleep(10L);
                }catch (Exception e){
                    e.printStackTrace();
                }
                continue;
            }
            try {
                DatagramPacket datagramPacket = dudQueueBean.getRtpQueue().poll();
                if (datagramPacket==null){
                    continue;
                }
                sendChannel.writeAndFlush(datagramPacket);
                log.debug("send rtp pack:"+System.currentTimeMillis());
            }catch (Exception e){
                e.printStackTrace();
                log.error(e.getMessage(),e.getCause());
            }
        }
    }

    @Override
    public void screenShotStream(AbstractQueueBean<byte[]> dudQueueBean) throws BudNettyServerException {

    }

}
