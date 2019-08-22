package com.example.server;

import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @author lmh
 * @Title:
 * @date 2019/8/22 0022
 */
public class EventTriggered extends ChannelInboundHandlerAdapter{
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    //    重写channelRead0() 事件处理方法。每当从服务端读到客户端写入信息时，将信息转发给其他客户端的 Channel。其中如果你使用的是 Netty 5.x 版本时，需要把 channelRead0() 重命名为messageReceived()
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)  {
        Channel channel = ctx.channel();
        TextWebSocketFrame msg1=(TextWebSocketFrame)msg;
        if(msg1.text().contains("心跳续约")){
            System.out.println("用户:"+channel.id() + ": " + msg1.text());
        }else {
            System.out.println("用户:"+channel.id() + ": " + msg1.text());
            for(Channel channel1:channels
            ){
                channel1.writeAndFlush(new TextWebSocketFrame("来自"+channel.id()+": " + msg1.text()));
            }
        }
    }
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
//            读超时事件
            if (state == IdleState.READER_IDLE) {
                System.out.println("已经10秒未收到"+ctx.channel().id()+"的消息了！");
                if(ctx.channel()!=null){
                    System.out.println("服务器进行续约"+ctx.channel().id());
                    ChannelFuture cha = ctx.channel().writeAndFlush(new TextWebSocketFrame("服务器进行续约"+ctx.channel().id()));
                    boolean isSuccess = cha.isSuccess();
                    if(isSuccess){
                        System.out.println("续约成功");

                    }else {
                        System.out.println("续约失败");
                        ctx.channel().close();
                    }
                }else {
                    ctx.channel().close();
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
    //   每当从服务端收到新的客户端连接时，客户端的 Channel 存入 ChannelGroup 列表中，并通知列表中的其他客户端 Channel
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {  // (2)
        Channel incoming = ctx.channel();
        for (Channel channel : channels) {
            channel.writeAndFlush(new TextWebSocketFrame("[SERVER] - " + incoming.id() + " 加入"));
        }
        channels.add(ctx.channel());
    }
    //   重写handlerRemoved() 事件处理方法。每当从服务端收到客户端断开时，客户端的 Channel 移除 ChannelGroup 列表中，并通知列表中的其他客户端 Channel
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("用户下线: " + ctx.channel().id());
    }
    //   exceptionCaught() 事件处理方法是当出现 Throwable 对象才会被调用，即当 Netty 由于 IO 错误或者处理器在处理事件时抛出的异常时。在大部分情况下，捕获的异常应该被记录下来并且把关联的 channel 给关闭掉。然而这个方法的处理方式会在遇到不同异常的情况下有不同的实现，比如你可能想在关闭连接之前发送一个错误码的响应消息。
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel incoming = ctx.channel();
        System.out.println("SimpleChatClient:"+incoming.id()+"异常");
        if(ctx!=null){
            ctx.channel().close();
        }
        if(cause!=null){
            cause.printStackTrace();
        }
    }
    //    重写channelActive() 事件处理方法。服务端监听到客户端活动
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        Channel channel = ctx.channel();
        System.out.println("用户上线:"+channel.id());
    }
    //    重写channelInactive() 事件处理方法。服务端监听到客户端不活动
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        System.out.println("SimpleChatClient:"+incoming.id()+"掉线");
    }
}
