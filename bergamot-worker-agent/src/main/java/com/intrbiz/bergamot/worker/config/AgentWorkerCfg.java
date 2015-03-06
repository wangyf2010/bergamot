package com.intrbiz.bergamot.worker.config;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.intrbiz.bergamot.agent.server.config.BergamotAgentServerCfg;
import com.intrbiz.bergamot.config.EngineCfg;
import com.intrbiz.bergamot.config.ExecutorCfg;
import com.intrbiz.bergamot.config.WorkerCfg;
import com.intrbiz.bergamot.worker.engine.agent.AgentEngine;
import com.intrbiz.bergamot.worker.engine.agent.CPUExecutor;
import com.intrbiz.bergamot.worker.engine.agent.MemoryExecutor;
import com.intrbiz.bergamot.worker.engine.agent.PresenceExecutor;

@XmlType(name = "worker")
@XmlRootElement(name = "worker")
public class AgentWorkerCfg extends WorkerCfg
{
    private static final long serialVersionUID = 1L;

    private BergamotAgentServerCfg agentServer;

    public AgentWorkerCfg()
    {
        super();
    }

    @XmlElementRef(type = BergamotAgentServerCfg.class)
    public BergamotAgentServerCfg getAgentServer()
    {
        return agentServer;
    }

    public void setAgentServer(BergamotAgentServerCfg agentServer)
    {
        this.agentServer = agentServer;
    }

    @Override
    public void applyDefaults()
    {
        // add our default engines to avoid needing to configure them
        this.getEngines().add(new EngineCfg(AgentEngine.class, new ExecutorCfg(PresenceExecutor.class), new ExecutorCfg(CPUExecutor.class), new ExecutorCfg(MemoryExecutor.class)));
        // apply defaults from super class
        super.applyDefaults();
    }
}