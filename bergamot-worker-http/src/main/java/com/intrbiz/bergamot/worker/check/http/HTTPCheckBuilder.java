package com.intrbiz.bergamot.worker.check.http;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Map.Entry;
import java.util.function.Consumer;

/**
 * Fluent interface to construct a HTTP check
 */
public abstract class HTTPCheckBuilder
{
    private String address;
    
    private int port = -1;
    
    private int connectTimeout = -1;
    
    private int requestTimeout = -1;
    
    private boolean ssl = false;
    
    private boolean permitInvalidCerts = false;
    
    private HttpVersion version = HttpVersion.HTTP_1_1;
    
    private HttpMethod method = HttpMethod.GET;
    
    private String virtualHost;
    
    private String path = "/";
    
    private HttpHeaders headers = new DefaultHttpHeaders();
    
    private Consumer<HTTPCheckResponse> responseHandler;
    
    private Consumer<Throwable> errorHandler;
    
    public HTTPCheckBuilder()
    {
        super();
    }
    
    public HTTPCheckBuilder connect(String address)
    {
        this.address = address;
        return this;
    }
    
    public HTTPCheckBuilder port(int port)
    {
        this.port = port;
        return this;
    }
    
    public HTTPCheckBuilder connect(String address, int port)
    {
        this.address = address;
        this.port = port;
        return this;
    }
    
    public HTTPCheckBuilder connectTimeout(int connectTimeout)
    {
        this.connectTimeout = connectTimeout;
        return this;
    }
    
    public HTTPCheckBuilder requestTimeout(int requestTimeout)
    {
        this.requestTimeout = requestTimeout;
        return this;
    }
    
    public HTTPCheckBuilder timeout(int connectTimeout, int requestTimeout)
    {
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        return this;
    }
    
    public HTTPCheckBuilder http()
    {
        this.ssl = false;
        return this;
    }
    
    public HTTPCheckBuilder https()
    {
        this.ssl = true;
        return this;
    }
    
    public HTTPCheckBuilder ssl()
    {
        this.ssl = true;
        return this;
    }
    
    public HTTPCheckBuilder ssl(boolean ssl)
    {
        this.ssl = ssl;
        return this;
    }
    
    public HTTPCheckBuilder permitInvalidCerts()
    {
        this.permitInvalidCerts = true;
        return this;
    }
    
    public HTTPCheckBuilder permitInvalidCerts(boolean permitInvalidCerts)
    {
        this.permitInvalidCerts = permitInvalidCerts;
        return this;
    }
    
    public HTTPCheckBuilder version(HttpVersion version)
    {
        this.version = version;
        return this;
    }
    
    public HTTPCheckBuilder http1_0()
    {
        this.version = HttpVersion.HTTP_1_0;
        return this;
    }
    
    public HTTPCheckBuilder http1_1()
    {
        this.version = HttpVersion.HTTP_1_1;
        return this;
    }
    
    public HTTPCheckBuilder method(HttpMethod method)
    {
        this.method = method;
        return this;
    }
    
    public HTTPCheckBuilder path(String path)
    {
        this.path = path;
        return this;
    }
    
    public HTTPCheckBuilder get(String path)
    {
        this.method = HttpMethod.GET;
        this.path = path;
        return this;
    }
    
    public HTTPCheckBuilder get()
    {
        this.method = HttpMethod.GET;
        this.path = "/";
        return this;
    }
    
    public HTTPCheckBuilder host(String virtualHost)
    {
        this.virtualHost = virtualHost;
        return this;
    }
    
    public HTTPCheckBuilder header(String name, Object value)
    {
        this.headers.add(name, value);
        return this;
    }
    
    public HTTPCheckBuilder onResponse(Consumer<HTTPCheckResponse> responseHandler)
    {
        this.responseHandler = responseHandler;
        return this;
    }
    
    public HTTPCheckBuilder onError(Consumer<Throwable> errorHandler)
    {
        this.errorHandler = errorHandler;
        return this;
    }
    
    /* Executors */
    
    public void execute(Consumer<HTTPCheckResponse> responseHandler, Consumer<Throwable> errorHandler)
    {
        this.responseHandler = responseHandler;
        this.errorHandler = errorHandler;
        this.execute();
    }
    
    public void execute()
    {
        // default port
        if (this.port == -1) this.port = this.ssl ? 443 : 80;
        // default virtual host
        if (this.virtualHost == null) this.virtualHost = this.address;
        // build the request
        FullHttpRequest request = new DefaultFullHttpRequest(this.version, this.method, this.path);
        // default to connection close
        request.headers().add(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        // virtual host
        if (this.port == 80) request.headers().add(HttpHeaders.Names.HOST, this.virtualHost);
        else request.headers().add(HttpHeaders.Names.HOST, this.virtualHost + ":" + this.port);
        // user agent
        request.headers().add(HttpHeaders.Names.USER_AGENT, "Bergamot Monitoring Check HTTP 1.0.0");
        // add headers
        for (Entry<String, String> e : this.headers)
        {
            request.headers().add(e.getKey(), e.getValue());
        }
        // submit the check
        this.submit(this.address, this.port, this.connectTimeout, this.requestTimeout, this.ssl, this.permitInvalidCerts, request, this.responseHandler, this.errorHandler);
    }
    
    protected abstract void submit(final String address, final int port, final int connectTimeout, final int requestTimeout, final boolean ssl, final boolean permitInvalidCerts, final FullHttpRequest request, final Consumer<HTTPCheckResponse> responseHandler, final Consumer<Throwable> errorHandler);
}