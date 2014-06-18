package com.intrbiz.bergamot.model.virtual;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.intrbiz.bergamot.model.Check;
import com.intrbiz.bergamot.model.Status;

public abstract class VirtualCheckOperator implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    public abstract boolean computeOk();
    
    public abstract Status computeStatus();
    
    public final Set<Check<?,?>> computeDependencies()
    {
        Set<Check<?,?>> checks = new HashSet<Check<?,?>>();
        this.computeDependencies(checks);
        return checks;
    }
    
    public abstract void computeDependencies(Set<Check<?,?>> checks);
}