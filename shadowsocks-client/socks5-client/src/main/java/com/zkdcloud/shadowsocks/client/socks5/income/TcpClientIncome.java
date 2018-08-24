package com.zkdcloud.shadowsocks.client.socks5.income;

import com.zkdcloud.shadowsocks.client.socks5.channelHandler.inbound.Socks5AnalysisInbound;
import com.zkdcloud.shadowsocks.client.socks5.channelHandler.inbound.Socks5AuthenticateInbound;
import com.zkdcloud.shadowsocks.common.income.AbstractIncome;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * description
 *
 * @author zk
 * @since 2018/8/20
 */
public class TcpClientIncome extends AbstractIncome {
    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(TcpClientIncome.class);
    
    /**
     * boosLoopGroup
     */
    private EventLoopGroup bossLoopGroup = new NioEventLoopGroup(1,new DefaultThreadFactory("boss"));
    /**
     * worksLoopGroup
     */
    private EventLoopGroup worksLoopGroup = new NioEventLoopGroup(1,new DefaultThreadFactory("works"));
    /**
     * serverBootstrap
     */
    private ServerBootstrap serverBootstrap = new ServerBootstrap();

    @Override
    public void startup() throws InterruptedException {
        ChannelFuture channelFuture = serverBootstrap.group(bossLoopGroup,worksLoopGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 5120)
                .option(ChannelOption.SO_RCVBUF, 32 * 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_LINGER, 1)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new IdleStateHandler(0,0,3,TimeUnit.MINUTES){
                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                logger.error("channelId: {} has exception, cause : ",cause.getMessage());
                                ctx.channel().close();
                            }
                        })
                        .addLast(new Socks5AuthenticateInbound())
                        .addLast(new Socks5AnalysisInbound());
                    }
                }).bind(1081).sync();
        channelFuture.channel();
        channelFuture.channel().closeFuture().sync();
    }
}
