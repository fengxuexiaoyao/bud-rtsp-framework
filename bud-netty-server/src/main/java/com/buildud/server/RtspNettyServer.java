package com.buildud.server;

import com.buildud.config.RtspConfig;
import com.buildud.service.IBudCodeService;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

import com.buildud.handler.*;

@Configuration
public class RtspNettyServer implements ApplicationListener<ApplicationStartedEvent> {

    public static final Logger log = LoggerFactory.getLogger(RtspNettyServer.class);

    private static Bootstrap udpRtpstrap = new Bootstrap();
    private static Bootstrap udpRtcpstrap = new Bootstrap();
    public static Channel rtpChannel;
    public static Channel rtcpChannel;

    @Autowired
    private IBudCodeService codeService;

    public static void initUdp(EventLoopGroup group)
    {
        udpRtpstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_SNDBUF, 1024*1024*2)
                .option(ChannelOption.SO_RCVBUF, 1024*1024*2)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel nioDatagramChannel) throws Exception {
                        nioDatagramChannel.pipeline().addLast(new RtpHandler());
                    }
                })
                .option(ChannelOption.SO_BROADCAST, false);

        udpRtcpstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_SNDBUF, 1024*1024)
                .option(ChannelOption.SO_RCVBUF, 1024*1024)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel nioDatagramChannel) throws Exception {
                        nioDatagramChannel.pipeline().addLast(new RtcpHandler());
                    }
                })
                .option(ChannelOption.SO_BROADCAST, false);
    }

    public static void createUdp(int port)
    {
        try
        {
            log.info("start udp bind {} ", port);
            rtpChannel = udpRtpstrap.bind(port).sync().channel();
            rtcpChannel = udpRtcpstrap.bind(port+1).sync().channel();

            log.info("end udp bind {}", port);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationStartedEvent) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);

        EventLoopGroup listenGrp  = new NioEventLoopGroup(1);
        EventLoopGroup workGrp = new NioEventLoopGroup(RtspConfig.workerGroup);
        initUdp(workGrp);
        createUdp(RtspConfig.rtpPort);
        /**
         * ServerBootstrap 是一个启动NIO服务的辅助启动类
         */
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        /**
         * 设置group，将bossGroup， workerGroup线程组传递到ServerBootstrap
         */
        serverBootstrap = serverBootstrap.group(listenGrp, workGrp);
        /**
         * ServerSocketChannel是以NIO的selector为基础进行实现的，用来接收新的连接，这里告诉Channel通过NioServerSocketChannel获取新的连接
         */
        serverBootstrap = serverBootstrap.channel(NioServerSocketChannel.class);
        /**
         * option是设置 bossGroup，childOption是设置workerGroup
         * netty 默认数据包传输大小为1024字节, 设置它可以自动调整下一次缓冲区建立时分配的空间大小，避免内存的浪费    最小  初始化  最大 (根据生产环境实际情况来定)
         * 使用对象池，重用缓冲区
         */
        serverBootstrap = serverBootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(64, 10496, 1048576));
        serverBootstrap = serverBootstrap.childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(64, 10496, 1048576));

        serverBootstrap = serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline()
                        .addLast(new IdleStateHandler(0, 0, RtspConfig.rtspIdletime, TimeUnit.SECONDS))//5秒内既没有读，也没有写，则关闭连接
                        .addLast(new RtspDecoder())
                        .addLast(new RtspEncoder())
                        .addLast(new HttpObjectAggregator(64 * 1024))
                        .addLast(new BudRtspHandler(codeService,rtpChannel))
                        .addLast(new HeartBeatServerHandler());
            }
        });

        try {
            /**
             * 绑定端口，同步等待成功
             */
            ChannelFuture f = serverBootstrap.bind(RtspConfig.rtspPort).sync();
            /**
             * 等待服务器监听端口关闭
             */
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            /**
             * 退出，释放线程池资源
             */
            listenGrp.shutdownGracefully();
            workGrp.shutdownGracefully();
        }
    }
}
