package com.intrbiz.bergamot.virtual.reference;

import com.intrbiz.bergamot.model.Cluster;
import com.intrbiz.bergamot.virtual.VirtualCheckExpressionContext;

public class ClusterByName implements ClusterReference
{
    private static final long serialVersionUID = 1L;

    private String name;

    public ClusterByName()
    {
        super();
    }

    public ClusterByName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public Cluster resolve(VirtualCheckExpressionContext context)
    {
        return context.lookupCluster(this.getName());
    }
    
    public String toString()
    {
        return "cluster '" + this.getName() + "'"; 
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ClusterByName other = (ClusterByName) obj;
        if (name == null)
        {
            if (other.name != null) return false;
        }
        else if (!name.equals(other.name)) return false;
        return true;
    }
}
