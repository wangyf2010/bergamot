package com.intrbiz.bergamot.result;

import java.util.UUID;

import com.intrbiz.bergamot.model.message.check.ExecuteCheck;
import com.intrbiz.bergamot.model.message.result.Result;


/**
 * Process the result queue and update the object states
 */
public interface ResultProcessor
{
    int getThreads();
    
    void setThreads(int threads);
    
    void start();
    
    /**
     * Process the result of a check which executed
     * @param result
     */
    void processExecuted(Result result);
    
    /**
     * Process a check execution which has died, did not 
     * get executed within its TTL.
     * @param check
     */
    void processDead(ExecuteCheck check);
    
    /**
     * Make this result processor responsible for the given pool
     * @param site the site id
     * @param pool the per site pool id
     */
    void ownPool(UUID site, int pool);
    
    /**
     * Make this result processor not responsible for the given pool
     * @param site the site id
     * @param pool the per site pool id
     */
    void disownPool(UUID site, int pool);
}
