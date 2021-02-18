package com.buildud.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class RtpHandler extends SimpleChannelInboundHandler<DatagramPacket>
{
	public static final Logger log = LoggerFactory.getLogger(RtpHandler.class);
	//key是ssrc。同一个channel内，audio和video的ssrc不同，但是Queue是同一个
	public static Map<String, BudRtspHandler> rtspHandlerMap = new ConcurrentHashMap<String, BudRtspHandler>(5000);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg){
        ByteBuf content = msg.content();
        byte[] dst = new byte[content.readableBytes()];
        content.readBytes(dst);

        String destIp = msg.sender().getAddress().getHostAddress();
        int destPort = msg.sender().getPort();

        byte sign = dst[0];
//        int ssrc = ((dst[1]&0xFF)<<24) + ((dst[2]&0xFF)<<16) + ((dst[3]&0xFF)<<8) + (dst[4]&0xFF);
//        InetSocketAddress dstAddr = new InetSocketAddress(destIp, destPort);
//        DudRtspHandler rtspHandler = rtspHandlerMap.get(String.valueOf(ssrc));
//        if (rtspHandler != null && destIp.equals(rtspHandler.strremoteip)) {
//            if (sign == 0x0) {				//视频RTP探测
//                rtspHandler.dstVideoRtpAddr = dstAddr;
//                rtspHandler.isVideoRtpDetected = true;
//            } else if (sign == 0x1) {		//音频RTP探测
//
//            }
//        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelActive(ctx);
        log.info("rtp handler active {}", ctx.channel().id().asShortText());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelInactive(ctx);
        log.info("rtp handler inactive {}", ctx.channel().id().asShortText());
    }
    
    private static final char Hex_Char_Arr[] = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

    public static String byteArrToHex(byte[] btArr) {
        char strArr[] = new char[btArr.length * 2];
        int i = 0;
        for (byte bt : btArr) {
            strArr[i++] = Hex_Char_Arr[bt>>>4 & 0xf];
            strArr[i++] = Hex_Char_Arr[bt & 0xf];
        }
        return new String(strArr);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        log.error("", cause.getMessage());
        ctx.channel().close();
    }
}
