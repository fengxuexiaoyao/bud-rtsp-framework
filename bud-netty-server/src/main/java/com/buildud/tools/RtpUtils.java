package com.buildud.tools;

import com.buildud.bean.AbstractQueueBean;
import com.buildud.config.RtspConfig;
import com.buildud.handler.BudRtspHandler;
import com.buildud.queue.RtpBudQueue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class RtpUtils {
	public static final Logger log = LoggerFactory.getLogger(RtpUtils.class);

    public static byte[] makeH264Rtp(byte[] pcData, boolean mark, int seqNum, int timestamp, int ssrc){
        byte b;
        if (mark) {
            b = (byte) 0xE0; //1110 0000
        } else {
            b = (byte) 0x60; //0110 0000
        }

        ByteBuffer bb = ByteBuffer.allocate(pcData.length + 12);
        //rtp头一个字节包含 V、P、X、CC
        //V版号占2bit(默认为2)，对应二进制10
        //P填充位占1bit(不用管)，对应二进制0
        //X扩展位占1bit(不用管)，对应二进制0
        //CC是CSRC的计数位占4bit，不是RTP必须的，对应二进制0000
        //以上bit位拼接后为1000 0000转为16进制为0x80
        bb.put((byte) 0x80);
        //rtp第二个字节包含M、PT
        //M标记位占1bit，没有进行分包值为1.如果当前rtp包为malu的分片的结束那么该位值为1，否则为0
        //PT有效荷载类型
        //PT值可以查看对应表获取https://blog.csdn.net/qq_40732350/article/details/88374707，https://www.ietf.org/assignments/rtp-parameters/rtp-parameters.xml
        //由于H264没有对应的PT类型，只能使用动态PT值(96-127),所以默认H264的PT值为96，转二进制110 0000
        //以上bit位拼接后为1110 0000(转为16进制为0xE0)或者0110 0000(转为16进制为0x60)
        bb.put(b);
        //rtp第三和第四字节为SequenceNum，表明对应的nalu的序列号。
        //由于该位置占用两个字节，所以用short进行转码，直接转成两个字节。
        bb.putShort((short) seqNum);
        //rtp第5至第8字节为Timestamp时间戳，表明对应的nalu的播放时间序列。
        //由于该位置占用四个字节，所以直接使用int型就。
        bb.putInt(timestamp);
        //rtp第9至第12字节字节为SSRC标识同步信源
        bb.putInt(ssrc);
        bb.put(pcData);
        return bb.array();
    }

    /**
     * 通过nalu转为rtp包
     * @param nalu
     * @param dudQueueBean
     */
    public static void naluToRtp(byte[] nalu, AbstractQueueBean<byte[]> dudQueueBean){
        if (nalu==null||nalu.length<0){
            return;
        }

        int iLen = nalu.length;

        int sliceNum = iLen/ RtspConfig.mtu;
        int endSliceLength = iLen%RtspConfig.mtu;
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

            sendRtpQueue(rtpPackage,dudQueueBean);
            //设置时间自增
            if (isAutoTime){
                dudQueueBean.autoIncrementTime();
            }
            try {
                Thread.sleep(1000/ RtspConfig.fps);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }
        //分片处理nalu
        naluSliceToRtp(nalu,dudQueueBean);
    }

    /**
     * 通过nalu转为rtp包
     * 由于nalu太大超过mtu值进行分片处理
     * @param nalu   nalu数组
     * @param dudQueueBean  通道及播放序列相关值
     * @return
     */
    public static void naluSliceToRtp(byte[] nalu, AbstractQueueBean<byte[]> dudQueueBean) {
        int iLen = nalu.length;
        //由于fu_identifier和fu_header会替换原有的头部，所以nalu尾部剩余数需要减1
        iLen--;
        int sliceNum = iLen / RtspConfig.mtu;
        int endSliceLength = iLen % RtspConfig.mtu;
        if (endSliceLength > 0) {
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

        while (i < sliceNum) {
            int start = i * RtspConfig.mtu + 1;

            byte[] dest = null;
            if (i == 0) {
                //代表当前为所有分包的开始包
                //那么S、E、R三位为100，再补齐后5位变为1000 0000，转为16进制为0x80
                //那么0x80和nalu_type相加为当前包的fu_header真实值
                fu_header = (byte) (0x80 + nalu_type);
                //由于是分包的开始，所以这个分包的字节长度为mtu
                //再加上fu_identifier和fu_header两个字节，整个组成nalu分片包数据。
                bb = ByteBuffer.allocate(RtspConfig.mtu + 2);
                //用于保存nalu分包数据
                dest = new byte[RtspConfig.mtu];
            } else if ((i + 1) == sliceNum) {
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
            } else {
                //分片中间包，S、E、R三位为000，再补齐后5位变为0000 0000，转为16进制为0x00
                //那么0x00和nalu_type相加为当前包的fu_header真实值
                fu_header = (byte) (0x00 + nalu_type);
                //由于是中间包，所以这个分包的字节长度为mtu
                //再加上fu_identifier和fu_header两个字节，整个组成nalu分片包数据。
                bb = ByteBuffer.allocate(RtspConfig.mtu + 2);
                //用于保存nalu分包数据
                dest = new byte[RtspConfig.mtu];
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
                byte[] rtpPackage = RtpUtils.makeH264Rtp(bb.array(), mark, dudQueueBean.getSeqNum(), dudQueueBean.getTime(), dudQueueBean.getSsrc());

                sendRtpQueue(rtpPackage, dudQueueBean);
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage(), e.getCause());
            }
            i++;
        }

        //同一组分片包结束后自增
        //设置时间自增
        dudQueueBean.autoIncrementTime();

        try {
            Thread.sleep(1000 / RtspConfig.fps);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送rtp服务
     * @param rtpPack  要发送的rtp包
     * @param dudQueueBean  发送通道
     */
    public static void sendRtpQueue(byte[] rtpPack,AbstractQueueBean<byte[]> dudQueueBean){
        if (dudQueueBean.getRtpQueue()==null){
            return;
        }
        try{
            ByteBuf byteBuf = Unpooled.buffer(rtpPack.length);
            byteBuf.writeBytes(rtpPack);
            DatagramPacket datagramPacket = new DatagramPacket(byteBuf, dudQueueBean.getDstVideoRtpAddr());
//            dudQueueBean.getRtpQueue().put(datagramPacket);
            dudQueueBean.getSendChannel().writeAndFlush(datagramPacket);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage(),e.getCause());
        }
    }
}