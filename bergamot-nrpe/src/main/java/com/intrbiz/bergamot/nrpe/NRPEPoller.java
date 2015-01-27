package com.intrbiz.bergamot.nrpe;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.intrbiz.bergamot.nrpe.model.NRPEPacket;
import com.intrbiz.bergamot.nrpe.model.NRPEResponse;
import com.intrbiz.bergamot.nrpe.netty.NRPEDecoder;
import com.intrbiz.bergamot.nrpe.netty.NRPEEncoder;
import com.intrbiz.bergamot.nrpe.netty.NRPEHandler;
import com.intrbiz.util.IBThreadFactory;

/**
 * A non-blocking, asynchronous NRPE client
 */
public class NRPEPoller
{
    private EventLoopGroup eventLoop;

    private int defaultRequestTimeoutSeconds;

    private int defaultConnectTimeoutSeconds;

    public NRPEPoller(int threads, int defaultConnectTimeoutSeconds, int defaultRequestTimeoutSeconds)
    {
        this.defaultRequestTimeoutSeconds = defaultRequestTimeoutSeconds;
        this.defaultConnectTimeoutSeconds = defaultConnectTimeoutSeconds;
        // setup the Netty event loop
        this.eventLoop = new NioEventLoopGroup(threads, new IBThreadFactory("NRPEPoller", false));
    }

    public NRPEPoller()
    {
        this(Runtime.getRuntime().availableProcessors(), 5, 60);
    }

    public int getDefaultRequestTimeoutSeconds()
    {
        return defaultRequestTimeoutSeconds;
    }

    public int getDefaultConnectTimeoutSeconds()
    {
        return defaultConnectTimeoutSeconds;
    }
    
    private SSLEngine createSSLEngine()
    {
        try
        {
            SSLEngine sslEngine = SSLContext.getDefault().createSSLEngine();
            sslEngine.setEnabledCipherSuites(NRPEClient.CIPHERS);
            sslEngine.setEnabledProtocols(NRPEClient.PROTOCOLS);
            sslEngine.setUseClientMode(true);
            return sslEngine;
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("Failed to init SSLEngine", e);
        }
    }

    /**
     * Connect to the given host and send the request packet, invoking the callback handlers
     * 
     * @param host
     *            the host of the NRPE daemon
     * @param port
     *            the port of the NRPE daemon
     * @param request
     *            the request packet
     * @param responseHandler
     *            the response handler to invoke
     * @param errorHandler
     *            the error handler to invoke should an error occur
     */
    public void submit(String host, int port, int connectTimeout, int requestTimeout, NRPEPacket request, Consumer<NRPEPacket> responseHandler, Consumer<Throwable> errorHandler)
    {
        Bootstrap b = new Bootstrap();
        b.group(this.eventLoop);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(connectTimeout));
        b.handler(new ChannelInitializer<SocketChannel>()
        {
            @Override
            public void initChannel(SocketChannel ch) throws Exception
            {
                ch.pipeline().addLast(
                        new ReadTimeoutHandler(requestTimeout /* seconds */), 
                        new WriteTimeoutHandler(requestTimeout /* seconds */), 
                        new SslHandler(createSSLEngine()), 
                        new NRPEDecoder(), 
                        new NRPEEncoder(),
                        new NRPEHandler(request, responseHandler, errorHandler)
                );
            }
        });
        // connect the client
        b.connect(host, port).addListener(new GenericFutureListener<ChannelFuture>()
        {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception
            {
                if (future.isDone() && (!future.isSuccess()))
                {
                    errorHandler.accept(future.cause());
                }
            }
        });
    }

    public <T> void hello(String host, T userContext, Consumer<NRPEResponse> responseHandler, Consumer<Throwable> errorHandler) throws IOException
    {
        this.hello(host, 5666, this.defaultConnectTimeoutSeconds, this.defaultRequestTimeoutSeconds, responseHandler, errorHandler);
    }

    public <T> void hello(String host, int port, int connectTimeout, int requestTimeout, Consumer<NRPEResponse> responseHandler, Consumer<Throwable> errorHandler) throws IOException
    {
        this.submit(host, port, connectTimeout, requestTimeout, new NRPEPacket().version2().hello(), (p) -> {
            responseHandler.accept(new NRPEResponse(p.getOutput(), p.getResponseCode(), p.getRuntime()));
        }, errorHandler);
    }

    public <T> void command(String host, Consumer<NRPEResponse> responseHandler, Consumer<Throwable> errorHandler, String command) throws IOException
    {
        this.command(host, 5666, this.defaultConnectTimeoutSeconds, this.defaultRequestTimeoutSeconds, responseHandler, errorHandler, command);
    }

    public <T> void command(String host, int port, int connectTimeout, int requestTimeout, Consumer<NRPEResponse> responseHandler, Consumer<Throwable> errorHandler, String command) throws IOException
    {
        this.submit(host, port, connectTimeout, requestTimeout, new NRPEPacket().version2().command(command), (p) -> {responseHandler.accept(new NRPEResponse(p.getOutput(), p.getResponseCode(), p.getRuntime()));}, errorHandler);
    }

    public <T> void command(String host, Consumer<NRPEResponse> responseHandler, Consumer<Throwable> errorHandler, String command, List<String> args) throws IOException
    {
        this.command(host, 5666, this.defaultConnectTimeoutSeconds, this.defaultRequestTimeoutSeconds, responseHandler, errorHandler, command, args);
    }

    public <T> void command(String host, int port, int connectTimeout, int requestTimeout, Consumer<NRPEResponse> responseHandler, Consumer<Throwable> errorHandler, String command, List<String> args) throws IOException
    {
        this.submit(host, port, connectTimeout, requestTimeout, new NRPEPacket().version2().command(command, args), (p) -> {responseHandler.accept(new NRPEResponse(p.getOutput(), p.getResponseCode(), p.getRuntime()));}, errorHandler);
    }

    public <T> void command(String host, Consumer<NRPEResponse> responseHandler, Consumer<Throwable> errorHandler, String command, String... args) throws IOException
    {
        this.command(host, 5666, this.defaultConnectTimeoutSeconds, this.defaultRequestTimeoutSeconds, responseHandler, errorHandler, command, args);
    }

    public <T> void command(String host, int port, int connectTimeout, int requestTimeout, Consumer<NRPEResponse> responseHandler, Consumer<Throwable> errorHandler, String command, String... args) throws IOException
    {
        this.submit(host, port, connectTimeout, requestTimeout, new NRPEPacket().version2().command(command, args), (p) -> {responseHandler.accept(new NRPEResponse(p.getOutput(), p.getResponseCode(), p.getRuntime()));}, errorHandler);
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
}
