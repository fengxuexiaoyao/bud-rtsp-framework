package com.buildud.handler;

import com.buildud.bean.BudRtspBean;
import com.buildud.config.RtspConfig;
import com.buildud.exception.BudNettyServerException;
import com.buildud.queue.*;
import com.buildud.service.IBudCodeService;
import com.buildud.tools.RtspUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.*;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class BudRtspHandler extends SimpleChannelInboundHandler<FullHttpRequest>  {

    public static final Logger log = LoggerFactory.getLogger(BudRtspHandler.class);

    private Channel chn;								//RTSP channel

    private int remoteVideoRtpPort = 0;						//客户端Video的RTP端口
    private int remoteVideoRtcpPort = 0;					//客户端Video的RTCP端口
    public String strremoteip;								//客户端的IP地址
    public static InetSocketAddress dstVideoRtpAddr;
    public static InetSocketAddress dstVideoRtcpAddr;

    private int videoSsrc = 0;					//如果是record，则由客户端带上来。如果是play，则由服务器下发下去
    private BudRtspBean rtspQueueBean;

    private String session;

    private volatile int isRtspAlive = 1;						//rtsp连接是否存在，如果不存在，则停止发送udp

    private IBudCodeService codeService;

    public Channel rtpChannel;


    public Map<String, BudRtspHandler> rtspHandlerMap = new ConcurrentHashMap<String, BudRtspHandler>();

    public BudRtspHandler(IBudCodeService codeService, Channel rtpChannel) {
        this.codeService = codeService;
        this.rtpChannel = rtpChannel;

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        chn = ctx.channel();
        log.debug("{} new connection {}", chn.id(), Thread.currentThread().getName());

        isRtspAlive = 0;
    }

    public void closeThisClient()
    {
        if (this.chn.isActive())
        {
            log.debug("close this client {}", this.chn);
            this.chn.close();
        }
    }

    private void sendAnswer(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse rep)
    {
        final String cseq = req.headers().get(RtspHeaderNames.CSEQ);
        if (cseq != null)
        {
            rep.headers().add(RtspHeaderNames.CSEQ, cseq);
        }
        final String session = req.headers().get(RtspHeaderNames.SESSION);
        if (session != null)
        {
            rep.headers().add(RtspHeaderNames.SESSION, session);
        }
        if (!HttpUtil.isKeepAlive(req))
        {
            ctx.writeAndFlush(rep).addListener(ChannelFutureListener.CLOSE);
        } else
        {
            rep.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(rep);
        }
    }

    private void getVideoSsrc(){
        videoSsrc = RandomUtils.nextInt();
        //如果在ssrcMap中已存在该ssrc，则重新生成
        while (true){
            if (rtspHandlerMap.containsKey(String.valueOf(videoSsrc))) {
                videoSsrc = RandomUtils.nextInt();
                continue;
            }
            break;
        }
        rtspHandlerMap.put(String.valueOf(videoSsrc), this);
    }

    private void options(ChannelHandlerContext ctx,FullHttpResponse o , FullHttpRequest r){
        o.headers().add(RtspHeaderValues.PUBLIC, "DESCRIBE, SETUP, PLAY, TEARDOWN, ANNOUNCE, RECORD, GET_PARAMETER");
        sendAnswer(ctx, r, o);
    }

    private void describe(ChannelHandlerContext ctx,FullHttpResponse o , FullHttpRequest r) throws BudNettyServerException {
        InetSocketAddress addr = (InetSocketAddress) ctx.channel().localAddress();

        //默认是H264
        String sdp = String.format("c=IN IP4 0.0.0.0 \nm=video 0 RTP/AVP 96\na=rtpmap:96 H264/90000\n"
                        + "a=fmtp:96 packetization-mode=1;profile-level-id=640015;sprop-parameter-sets=Z2QAFazZQIA1sBEAAAMAAQAAAwA8DxYtlg==,a0vjyyLA\n"
                        + "a=control:track1\n"
//                        + "m=audio 0 RTP/AVP 97\na=rtpmap:97 MPEG4-GENERIC/16000\n"
//                        + "a=fmtp:97 streamtype=5; profile-level-id=15; mode=AAC-hbr; config=140856e500; sizeLength=13; indexLength=3; indexDeltaLength=3; Profile=1;\n"
//                        + "a=control:streamid=1\n"
                , addr.getHostString());

        o.headers().add(RtspHeaderNames.CONTENT_TYPE, "application/sdp");
        o.content().writeCharSequence(sdp, CharsetUtil.UTF_8);
        o.headers().add(RtspHeaderNames.CONTENT_LENGTH, o.content().writerIndex());
        sendAnswer(ctx, r, o);
    }

    private void setup(ChannelHandlerContext ctx,FullHttpResponse o , FullHttpRequest r) throws BudNettyServerException {
        String transport = r.headers().get(RtspHeaderNames.TRANSPORT);
        transport = transport.toLowerCase();
        String uri = r.uri();
        String[] strlist = transport.split(";");
        if (strlist!=null&&strlist.length>0&&strlist[0].toLowerCase().contains("rtp/avp")){
            for (String str:strlist){
                if (str.startsWith("client_port")){
//                            if ((mediaSdpInfoMap != null && mediaSdpInfoMap.containsKey("video") && uri.endsWith(mediaSdpInfoMap.get("video").getControl()))
//                                    || (mediaSdpInfoMap == null && uri.endsWith("streamid=0"))) {
                    String[] strclientport = str.split("=|-");

                    remoteVideoRtpPort = Integer.parseInt(strclientport[1]);
                    remoteVideoRtcpPort = Integer.parseInt(strclientport[2]);
                    strremoteip = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();

                    log.info("play ip:"+strremoteip);
                    log.info("play rtp port:"+remoteVideoRtpPort);
                    log.info("play rtcp port:"+remoteVideoRtcpPort);

                    dstVideoRtpAddr = new InetSocketAddress(strremoteip, remoteVideoRtpPort);
                    dstVideoRtcpAddr = new InetSocketAddress(strremoteip, remoteVideoRtcpPort);

                    getVideoSsrc();

                    //设置回复地址及参数
                    o.headers().add(RtspHeaderNames.TRANSPORT,
                            r.headers().get(RtspHeaderNames.TRANSPORT)+String.format(";source=127.0.0.1;server_port=%d-%d", RtspConfig.rtpPort, RtspConfig.rtpPort+1)+";ssrc=" + videoSsrc);

                    break;
//                            }
                }
            }
        }else{
            o.setStatus(RtspResponseStatuses.UNSUPPORTED_MEDIA_TYPE);	//不支持的媒体类型
            sendAnswer(ctx, r, o);
            throw new BudNettyServerException("error: SETUP , Unsupported RTP Type");
        }

        session = r.headers().get(RtspHeaderNames.SESSION);
        if (session != null && !session.equals(chn.id().toString())){
            o.setStatus(RtspResponseStatuses.SESSION_NOT_FOUND);	//Session未找到
            sendAnswer(ctx, r, o);
            throw new BudNettyServerException("error: SETUP session incorrect, 454, Session Not Found");
        }
        if (session == null){
            session = chn.id().toString();
            o.headers().add("Session", session+";timeout=600");
            log.debug("out Session>>"+o.headers().get("Session"));
        }
        sendAnswer(ctx, r, o);
    }

    public void play(ChannelHandlerContext ctx,FullHttpResponse o , FullHttpRequest r) throws BudNettyServerException {
        //效验session
        session = r.headers().get("Session");
        if (session == null || !session.equals(chn.id().toString())) {
            o.setStatus(RtspResponseStatuses.SESSION_NOT_FOUND);	//Session未找到
            sendAnswer(ctx, r, o);
            throw new BudNettyServerException("error: PLAY Session incorrect, 454, Session Not Found");
        }

        rtspQueueBean = new BudRtspBean(dstVideoRtpAddr,rtpChannel);
        rtspQueueBean.setSsrc(videoSsrc);

        o.headers().add(RtspHeaderNames.RTP_INFO,"url=rtsp://127.0.0.1/track1;seq="+rtspQueueBean.getSeqNum()+";rtptime="+rtspQueueBean.getTime());
        String fileName = RtspUtils.getPalyVideoPath(r);
        if (fileName==null||"".equals(fileName.trim())){
            o.setStatus(new HttpResponseStatus(552, "Play File Not Set"));
            sendAnswer(ctx, r, o);
            return;
        }
        if ("desktop".equals(fileName)){
            BudScreenRecordThread screenRecordThread = new BudScreenRecordThread(rtspQueueBean);
            screenRecordThread.start();

            BudByteToNaluThread byteToNaluThread = new BudByteToNaluThread(rtspQueueBean);
            byteToNaluThread.start();

//            BudNaluToRtpThread naluToRtpThread = new BudNaluToRtpThread(rtspQueueBean);
//            naluToRtpThread.start();
//
//            BudSendRtpThread sendRtpThread = new BudSendRtpThread(rtspQueueBean,dstVideoRtpAddr,rtpChannel);
//            sendRtpThread.start();
        }else{
            String filePath = RtspConfig.playFilePath+ File.separator+ RtspUtils.getPalyVideoPath(r);
            File f = new File(filePath);
            if (!f.exists()||!f.isFile()){
                o.setStatus(new HttpResponseStatus(553, "Play File Not Exists"));
                sendAnswer(ctx, r, o);
                return;
            }

            ParseThread parseThread = new ParseThread(filePath,codeService,rtspQueueBean,rtpChannel);
            parseThread.start();

            SendRtpThread sendRtpThread = new SendRtpThread(codeService,rtspQueueBean,dstVideoRtpAddr,rtpChannel);
            sendRtpThread.start();
        }
        sendAnswer(ctx, r, o);
    }

    public void teardown(ChannelHandlerContext ctx,FullHttpResponse o , FullHttpRequest r) throws BudNettyServerException {
        rtspQueueBean.setStopSate(true);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest r) {
        FullHttpResponse o = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
        try{
            if (!r.decoderResult().isSuccess())
            {
                throw new BudNettyServerException("error: decode error, invalid rtsp request");
            }

            if (r.method() == RtspMethods.OPTIONS) {
                log.debug("options");
                options(ctx,o,r);
            } else if (r.method() == RtspMethods.DESCRIBE) {
                log.debug("describe");
                describe(ctx,o,r);
            } else if (r.method() == RtspMethods.SETUP) {
                log.debug("setup");
                setup(ctx,o,r);
            } else if (r.method() == RtspMethods.PLAY) {
                log.debug("play");
                play(ctx,o,r);
            } else if (r.method() == RtspMethods.TEARDOWN) {
                log.debug("teardown");
                teardown(ctx,o,r);
            }  else if (r.method() == RtspMethods.GET_PARAMETER) {
                log.debug("get_parameter");

            } else if (r.method() == RtspMethods.ANNOUNCE) {
                log.debug("announce");

                ByteBuf content = r.content();
                byte[] sdp = new byte[content.readableBytes()];
                content.readBytes(sdp);

            } else if (r.method() == RtspMethods.RECORD) {
                log.debug("record");

            } else {
                o.setStatus(RtspResponseStatuses.METHOD_NOT_ALLOWED); //不允许的方法
                sendAnswer(ctx, r, o);
                throw new BudNettyServerException("error: unknown message, 405, Method Not Allowed");
            }
        } catch (BudNettyServerException e) {
            log.error(e.getMessage(),e.getCause());
            closeThisClient();
        }
    }
}
