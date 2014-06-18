package com.intrbiz.bergamot.model.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.intrbiz.bergamot.model.BergamotObject;
import com.intrbiz.bergamot.model.message.ParameterMO;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonTypeName("parameter")
public class Parameter extends BergamotObject<ParameterMO>
{
    private static final long serialVersionUID = 1L;
    
    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private String value;

    public Parameter()
    {
        super();
    }

    public Parameter(String name, String value)
    {
        super();
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
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
        Parameter other = (Parameter) obj;
        if (name == null)
        {
            if (other.name != null) return false;
        }
        else if (!name.equals(other.name)) return false;
        return true;
    }

    @Override
    public ParameterMO toMO(boolean stub)
    {
        return new ParameterMO(this.getName(), this.getValue());
    }

    public String toString()
    {
        return this.name + " => " + this.value;
    }
}