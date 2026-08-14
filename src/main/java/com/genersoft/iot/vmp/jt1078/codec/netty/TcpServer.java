package com.genersoft.iot.vmp.jt1078.codec.netty;

import com.genersoft.iot.vmp.jt1078.codec.decode.Jt808Decoder;
import com.genersoft.iot.vmp.jt1078.codec.encode.Jt808Encoder;
import com.genersoft.iot.vmp.jt1078.codec.encode.Jt808EncoderCmd;
import com.genersoft.iot.vmp.jt1078.config.JT1078Config;
import com.genersoft.iot.vmp.jt1078.proc.factory.CodecFactory;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import org.springframework.context.ApplicationEventPublisher;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:01
 * @email qingtaij@163.com
 */

@Slf4j
public class TcpServer {

    private final Integer port;
    private boolean isRunning = false;
    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;
    private ApplicationEventPublisher applicationEventPublisher = null;
    private Ijt1078Service service = null;
    private final JT1078Config jt1078Config;

    private final ByteBuf DECODER_JT808 = Unpooled.wrappedBuffer(new byte[]{0x7e});

    public TcpServer(Integer port, ApplicationEventPublisher applicationEventPublisher, Ijt1078Service service, JT1078Config jt1078Config) {
        this.port = port;
        this.applicationEventPublisher = applicationEventPublisher;
        this.service = service;
        this.jt1078Config = jt1078Config;
    }

    private void startTcpServer() {
        try {
            CodecFactory.init();
            this.bossGroup = new NioEventLoopGroup();
            this.workerGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.group(bossGroup, workerGroup);

            bootstrap.option(NioChannelOption.SO_BACKLOG, 1024)
                    .option(NioChannelOption.SO_REUSEADDR, true)
                    .childOption(NioChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        public void initChannel(NioSocketChannel channel) {
                            channel.pipeline()
                                    .addLast(new IdleStateHandler(jt1078Config.getReaderIdleTime(), 0, 0, TimeUnit.MINUTES))
                                    .addLast(new DelimiterBasedFrameDecoder(1024 * 2, DECODER_JT808))
                                    .addLast(new Jt808Decoder(applicationEventPublisher, service))
                                    .addLast(new Jt808Encoder())
                                    .addLast(new Jt808EncoderCmd())
                                    .addLast(new Jt808Handler(applicationEventPublisher));
                        }
                    });
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            // Check whether the TCP port of the listening device is started successfully.
            channelFuture.addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("Binding port:{} fail!  cause: {}", port, future.cause().getCause(), future.cause());
                }
            });
            log.info("Service: JT808 Server started successfully, port:{}", port);
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.warn("Service: JT808 Server startup exception, port:{},{}", port, e.getMessage(), e);
        } finally {
            stop();
        }
    }

    /**
     * Start a new thread and pull it upNetty
     */
    public synchronized void start() {
        if (this.isRunning) {
            log.warn("Service: JT808 Server has been started, port:{}", port);
            return;
        }
        this.isRunning = true;
        new Thread(this::startTcpServer).start();
    }

    public synchronized void stop() {
        if (!this.isRunning) {
            log.warn("Service: JT808 Server has stopped, port:{}", port);
        }
        this.isRunning = false;
        Future<?> future = this.bossGroup.shutdownGracefully();
        if (!future.isSuccess()) {
            log.warn("bossGroup Unable to stop normally", future.cause());
        }
        future = this.workerGroup.shutdownGracefully();
        if (!future.isSuccess()) {
            log.warn("workerGroup Unable to stop normally", future.cause());
        }
        log.warn("Service: JT808 Server has stopped, port:{}", port);
    }
}
