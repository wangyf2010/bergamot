package com.intrbiz.bergamot.config;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.intrbiz.configuration.Configuration;
import com.intrbiz.queue.name.GenericKey;

@XmlType(name = "worker")
@XmlRootElement(name = "worker")
public class WorkerCfg extends Configuration
{
    private int threads = -1;

    private List<RunnerCfg> runners = new LinkedList<RunnerCfg>();

    private ExchangeCfg exchange;

    private QueueCfg queue;

    private List<String> bindings = new LinkedList<String>();

    public WorkerCfg()
    {
        super();
    }
    
    public WorkerCfg(ExchangeCfg exchange, QueueCfg queue, String[] bindings, RunnerCfg... runners)
    {
        super();
        for (RunnerCfg runner : runners)
        {
            this.runners.add(runner);
        }
        this.exchange = exchange;
        this.queue = queue;
        for (String binding : bindings)
        {
            this.bindings.add(binding);
        }
    }
    
    public WorkerCfg(ExchangeCfg exchange, QueueCfg queue, String binding, RunnerCfg... runners)
    {
        super();
        for (RunnerCfg runner : runners)
        {
            this.runners.add(runner);
        }
        this.exchange = exchange;
        this.queue = queue;
            this.bindings.add(binding);
    }
    
    public WorkerCfg(ExchangeCfg exchange, QueueCfg queue, RunnerCfg... runners)
    {
        super();
        for (RunnerCfg runner : runners)
        {
            this.runners.add(runner);
        }
        this.exchange = exchange;
        this.queue = queue;
    }

    @XmlAttribute(name = "threads")
    public int getThreads()
    {
        return threads;
    }

    public void setThreads(int threads)
    {
        this.threads = threads;
    }

    @XmlElementRef(type = RunnerCfg.class)
    public List<RunnerCfg> getRunners()
    {
        return runners;
    }

    public void setRunners(List<RunnerCfg> runners)
    {
        this.runners = runners;
    }

    @XmlElementRef(type = ExchangeCfg.class)
    public ExchangeCfg getExchange()
    {
        return exchange;
    }

    public void setExchange(ExchangeCfg exchange)
    {
        this.exchange = exchange;
    }

    @XmlElementRef(type = QueueCfg.class)
    public QueueCfg getQueue()
    {
        return queue;
    }

    public void setQueue(QueueCfg queue)
    {
        this.queue = queue;
    }

    @XmlElement(name = "bind")
    public List<String> getBindings()
    {
        return bindings;
    }

    public void setBindings(List<String> bindings)
    {
        this.bindings = bindings;
    }
    
    public GenericKey[] asBindings()
    {
        return this.getBindings().stream().map((e) -> { return new GenericKey(e); }).toArray((size) -> { return new GenericKey[size]; });
    }

    @Override
    public void applyDefaults()
    {
        // default number of threads
        if (this.getThreads() <= 0)
        {
            // default of 5 checks per processor
            this.setThreads(Runtime.getRuntime().availableProcessors() * 5);
        }
        // default binding
        if (this.bindings.isEmpty())
        {
            this.bindings.add("#");
        }
        // cascade
        for (RunnerCfg runner : this.runners)
        {
            runner.applyDefaults();
        }
    }
}
