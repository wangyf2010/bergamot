package com.intrbiz.bergamot.agent;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.intrbiz.bergamot.agent.handler.CPUInfoHandler;
import com.intrbiz.bergamot.model.message.agent.AgentMessage;
import com.intrbiz.bergamot.model.message.agent.error.GeneralError;
import com.intrbiz.bergamot.model.message.agent.ping.AgentPing;
import com.intrbiz.bergamot.model.message.agent.ping.AgentPong;
import com.intrbiz.gerald.polyakov.Node;
import com.intrbiz.util.IBThreadFactory;

/**
 */
public class BergamotAgent
{
    private Logger logger = Logger.getLogger(BergamotAgent.class);
    
    private URI server;

    private EventLoopGroup eventLoop;
    
    private Timer timer;
    
    private Node node;
    
    private ConcurrentMap<Class<?>, AgentHandler> handlers = new ConcurrentHashMap<Class<?>, AgentHandler>();
    
    private AgentHandler defaultHandler;

    public BergamotAgent(URI server, Node node)
    {
        this.server = server;
        this.node = node;
        this.eventLoop = new NioEventLoopGroup(1, new IBThreadFactory("bergamot-agent", false));
        this.timer = new Timer();
        // background GC task
        // we want to deliberately 
        // keep memory to a minimum
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run()
            {
                System.gc();
                Runtime rt = Runtime.getRuntime();
                logger.debug("Memory: " + rt.freeMemory() + " " + rt.totalMemory() + " " + rt.maxMemory());
            }
        }, 30_000L, 30_000L);
        // handlers
        this.registerHandler(new CPUInfoHandler());
    }
    
    public Node getNode()
    {
        return this.node;
    }
    
    public URI getServer()
    {
        return this.server;
    }
    
    public void registerHandler(AgentHandler handler)
    {
        for (Class<?> cls : handler.getMessages())
        {
            this.handlers.put(cls, handler);
        }
    }
    
    public AgentHandler getHandler(Class<?> messageType)
    {
        AgentHandler handler = this.handlers.get(messageType);
        return handler == null ? this.defaultHandler : handler;
    }

    public void connect() throws Exception
    {
        // configure the client
        Bootstrap b = new Bootstrap();
        b.group(this.eventLoop);
        b.channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<SocketChannel>()
        {
            @Override
            public void initChannel(SocketChannel ch) throws Exception
            {
                // HTTP handling
                ch.pipeline().addLast("codec",      new HttpClientCodec()); 
                ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
                ch.pipeline().addLast("handler",    new WSClientHandler(BergamotAgent.this.timer, BergamotAgent.this.server, BergamotAgent.this.node)
                {
                    @Override
                    protected AgentMessage processMessage(final ChannelHandlerContext ctx, final AgentMessage request)
                    {
                        if (request instanceof AgentPing)
                        {
                            logger.debug("Got ping from server");
                            return new AgentPong(UUID.randomUUID().toString());
                        }
                        else if (request != null)
                        {
                            AgentHandler handler = getHandler(request.getClass());
                            if (handler != null)
                            {
                                return handler.handle(request);
                            }
                        }
                        // unhandled
                        logger.warn("Unhandled message: " + request);
                        return new GeneralError(request, "Unimplemented");
                    }
                });
            }
        });
        // connect the client
        b.connect(this.server.getHost(), this.server.getPort()).addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception
            {
                final Channel channel = future.channel();
                if (future.isDone() && future.isSuccess())
                {
                    // setup close listener
                    channel.closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception
                        {
                            BergamotAgent.this.scheduleReconnect();
                        }
                    });
                }
                else
                {
                    // schedule reconnect
                    BergamotAgent.this.scheduleReconnect();
                }
            }
        });        
    }
    
    protected void scheduleReconnect()
    {
        this.logger.info("Scheduling reconnection shortly");
        this.timer.schedule(new TimerTask() {
            @Override
            public void run()
            {
                try
                {
                    BergamotAgent.this.connect();
                }
                catch (Exception e)
                {
                    BergamotAgent.this.logger.error("Error connecting to server", e);
                    BergamotAgent.this.scheduleReconnect();
                }
            }
        }, 15_000L);
    }

    public void shutdown()
    {
        try
        {
            this.eventLoop.shutdownGracefully().await();
        }
        catch (InterruptedException e)
        {
        }
    }

    public static void main(String[] args) throws Exception
    {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.TRACE);
        //
        BergamotAgent agent = new BergamotAgent(new URI("ws://127.0.0.1:8081/websocket"), Node.service("BergamotAgent"));
        agent.connect();
    }
}
