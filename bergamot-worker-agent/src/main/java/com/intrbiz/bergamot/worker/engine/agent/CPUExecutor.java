package com.intrbiz.bergamot.worker.engine.agent;

import java.text.DecimalFormat;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.intrbiz.bergamot.agent.server.BergamotAgentServerHandler;
import com.intrbiz.bergamot.model.message.agent.check.CheckCPU;
import com.intrbiz.bergamot.model.message.agent.stat.CPUStat;
import com.intrbiz.bergamot.model.message.check.ExecuteCheck;
import com.intrbiz.bergamot.model.message.result.ActiveResultMO;
import com.intrbiz.bergamot.model.message.result.ResultMO;
import com.intrbiz.bergamot.worker.engine.AbstractExecutor;

/**
 * Check the cpu usage of a Bergamot Agent
 */
public class CPUExecutor extends AbstractExecutor<AgentEngine>
{
    public static final String NAME = "cpu";
    
    private static final DecimalFormat DFMT = new DecimalFormat("0.00");
    
    private Logger logger = Logger.getLogger(CPUExecutor.class);

    public CPUExecutor()
    {
        super();
    }

    /**
     * Only execute Checks where the engine == "agent"
     */
    @Override
    public boolean accept(ExecuteCheck task)
    {
        return super.accept(task) && AgentEngine.NAME.equals(task.getEngine()) && NAME.equals(task.getExecutor());
    }

    @Override
    public void execute(ExecuteCheck executeCheck, Consumer<ResultMO> resultSubmitter)
    {
        logger.debug("Checking Bergamot Agent CPU Usage, agent id: " + executeCheck.getParameter("agent_id"));
        try
        {
            // check the host presence
            UUID agentId = UUID.fromString(executeCheck.getParameter("agent_id"));
            // lookup the agent
            BergamotAgentServerHandler agent = this.getEngine().getAgentServer().getRegisteredAgent(agentId);
            if (agent != null)
            {
                // get the CPU stats
                agent.sendMessageToAgent(new CheckCPU(), (response) -> {
                    CPUStat stat = (CPUStat) response;
                    logger.debug("Got CPU usage: " + stat);
                    // compute the result
                    ActiveResultMO result = new ActiveResultMO().fromCheck(executeCheck);
                    // check
                    result.ok("Load: " + DFMT.format(stat.getLoad1()) + " " + DFMT.format(stat.getLoad5()) + " " + DFMT.format(stat.getLoad15()) + ", Usage: " + DFMT.format(stat.getTotalUsage().getTotal() * 100) + "%");
                    // submit
                    resultSubmitter.accept(result);
                });
            }
            else
            {
                // raise an error
                resultSubmitter.accept(new ActiveResultMO().fromCheck(executeCheck).error("Bergamot Agent disconnected"));
            }
        }
        catch (Exception e)
        {
            resultSubmitter.accept(new ActiveResultMO().fromCheck(executeCheck).error(e));
        }
    }
}