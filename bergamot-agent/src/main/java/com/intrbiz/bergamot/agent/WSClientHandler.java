package com.intrbiz.bergamot.agent;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.intrbiz.bergamot.io.BergamotAgentTranscoder;
import com.intrbiz.bergamot.model.message.agent.AgentMessage;
import com.intrbiz.bergamot.model.message.agent.error.GeneralError;
import com.intrbiz.bergamot.model.message.agent.hello.AgentHello;
import com.intrbiz.bergamot.model.message.agent.ping.AgentPing;
import com.intrbiz.bergamot.util.AgentUtil;
import com.intrbiz.gerald.polyakov.Node;

public abstract class WSClientHandler extends ChannelInboundHandlerAdapter
{
    private Logger logger = Logger.getLogger(WSClientHandler.class);

    private final WebSocketClientHandshaker handshaker;
    
    private final Timer timer;
    
    private final BergamotAgentTranscoder transcoder = BergamotAgentTranscoder.getDefaultInstance();
    
    private final Node node;
    
    private AgentHello hello;

    public WSClientHandler(Timer timer, URI server, Node node)
    {
        super();
        this.timer = timer;
        this.node = node;
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaders.Names.USER_AGENT, "BergamotAgent/1.0.0 (Java)");
        this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(server, WebSocketVersion.V13, null, false, headers);
    }
    
    protected AgentHello getHello()
    {
        if (this.hello == null)
        {
            this.hello = new AgentHello(UUID.randomUUID().toString());
            hello.setHostId(this.node.getHostId());
            hello.setHostName(this.node.getHostName());
            hello.setServiceId(this.node.getServiceId());
            hello.setServiceName(this.node.getServiceName());
            hello.setAgentName("BergamotAgent");
            hello.setAgentVariant("Java");
            hello.setAgentVersion("1.0.0");
            hello.setNonce(AgentUtil.newNonce());
            hello.setTimestamp(System.currentTimeMillis());
            hello.setProtocolVersion(1);
        }
        return this.hello;
    }
    
    protected abstract AgentMessage processMessage(final ChannelHandlerContext ctx, final AgentMessage request);

    @Override
    public void channelActive(ChannelHandlerContext ctx)
    {
        logger.debug("Connected, starting handshake");
        handshaker.handshake(ctx.channel());
    }

    public void channelHandshaked(ChannelHandlerContext ctx)
    {
        logger.debug("Handshake done");
        final Channel channel = ctx.channel();
        // hello
        logger.debug("Sending hello to server");
        channel.writeAndFlush(new TextWebSocketFrame(this.transcoder.encodeAsString(this.getHello())));
        // schedule ping
        this.timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                if (channel.isActive())
                {
                    logger.debug("Sending ping to server");
                    channel.writeAndFlush(new TextWebSocketFrame(transcoder.encodeAsString(new AgentPing(UUID.randomUUID().toString()))));
                }
                else
                {
                    this.cancel();
                }
            }
        }, 15_000L, 15_000L);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        // complete the handshake
        if (!handshaker.isHandshakeComplete())
        {
            handshaker.finishHandshake(ctx.channel(), (FullHttpResponse) msg);
            this.channelHandshaked(ctx);
            return;
        }
        // check we only expect a websocket frame
        if (msg instanceof WebSocketFrame)
        {
            // process the frame
            WebSocketFrame frame = (WebSocketFrame) msg;
            if (frame instanceof TextWebSocketFrame)
            {
                logger.info("Message: " + ((TextWebSocketFrame) frame).text());
                try
                {
                    AgentMessage request = this.transcoder.decodeFromString(((TextWebSocketFrame) frame).text(), AgentMessage.class);
                    // process the request
                    if (request instanceof AgentMessage)
                    {
                        // process the message and respond
                        AgentMessage response = this.processMessage(ctx, request);
                        if (response != null)
                        {
                            ctx.channel().writeAndFlush(new TextWebSocketFrame(this.transcoder.encodeAsString(response)));
                        }
                    }
                    else
                    {
                        ctx.channel().writeAndFlush(new TextWebSocketFrame(this.transcoder.encodeAsString(new GeneralError(request, "Bad request"))));
                    }
                }
                catch (Exception e)
                {
                    logger.error("Failed to decode request", e);
                    ctx.channel().writeAndFlush(new TextWebSocketFrame(this.transcoder.encodeAsString(new GeneralError("Failed to decode request"))));
                }
            }
            else if (frame instanceof PongWebSocketFrame)
            {
                logger.debug("Got pong, whoop");
            }
            else if (frame instanceof CloseWebSocketFrame)
            {
                logger.debug("Closing connection");
                ctx.close();
            }
        }
        else
        {
            throw new IllegalStateException("Expected WebSocketFrame, got: " + msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e)
    {
        logger.error("Unhandled error communicating with Bergamot server", e);
        ctx.close();
    }
}
