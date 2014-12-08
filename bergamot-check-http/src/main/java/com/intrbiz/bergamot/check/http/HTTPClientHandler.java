package com.intrbiz.bergamot.check.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;

public class HTTPClientHandler extends ChannelInboundHandlerAdapter
{
    private final Logger logger = Logger.getLogger(HTTPClientHandler.class);
    
    private volatile long start;
    
    private final FullHttpRequest request;
    
    private final Consumer<HTTPCheckResponse> responseHandler;
    
    private final Consumer<Throwable> errorHandler;
    
    private final SSLEngine sslEngine;
    
    private final Timer timer;
    
    private volatile TimerTask timeoutTask;
    
    private volatile boolean timedOut = false;
    
    public HTTPClientHandler(Timer timer, SSLEngine sslEngine, FullHttpRequest request, Consumer<HTTPCheckResponse> responseHandler, Consumer<Throwable> errorHandler)
    {
        super();
        this.sslEngine = sslEngine;
        this.request = request;
        this.responseHandler = responseHandler;
        this.errorHandler = errorHandler;
        this.timer = timer;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        if (! this.timedOut)
        {
            FullHttpResponse response = (FullHttpResponse) msg;
            long runtime = System.currentTimeMillis() - this.start;
            logger.debug("Got HTTP response: " + response.getStatus() + " in: " + runtime + "ms");
            if (logger.isTraceEnabled()) logger.trace("Response:\n" + response);
            // cancel the timeout
            if (this.timeoutTask != null) this.timeoutTask.cancel();
            // SSL shit
            TLSInfo tlsInfo = null;
            if (this.sslEngine != null)
            {
                try
                {
                    tlsInfo = new TLSInfo(this.sslEngine);
                }
                catch (Exception e)
                {
                    logger.error("Failed to get TLS info", e);
                }
            }
            // invoke the callback
            if (this.responseHandler != null)
                this.responseHandler.accept(new HTTPCheckResponse(runtime, response, tlsInfo));
        }
        // close
        if (ctx.channel().isActive()) ctx.close();
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx)
    {
        logger.debug("Sending HTTP request");
        this.start = System.currentTimeMillis();
        // schedule timeout for 60 seconds
        this.timeoutTask = new TimerTask() {
            @Override
            public void run()
            {
                HTTPClientHandler.this.onTimeout();
            }
        };
        this.timer.schedule(this.timeoutTask, this.start + 60_000L);
        // send the request
        if (logger.isTraceEnabled()) logger.trace("Request:\n" + this.request);
        ctx.writeAndFlush(this.request);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        logger.debug("Error processing HTTP request: " + cause);
        // invoke the callback
        this.errorHandler.accept(cause);
    }
    
    protected void onTimeout()
    {
        // invoke the error timeout
        this.timedOut = true;
        // invoke the error handler
        if (this.errorHandler != null) 
            this.errorHandler.accept(new TimeoutException("Timeout getting response from server"));
    }
}
