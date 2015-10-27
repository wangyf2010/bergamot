package com.intrbiz.bergamot.check.jmx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

public class MBeanWrapper
{
    private final JMXConnection connection;
    
    private final ObjectName name;
    
    private final MBeanInfo info;
    
    private Map<String, MBeanAttributeWrapper> attributes;
    
    private Map<String, MBeanOperationWrapper> operations;

    public MBeanWrapper(JMXConnection connection, ObjectName name, MBeanInfo info)
    {
        super();
        this.connection = connection;
        this.name = name;
        this.info = info;
    }
    
    private Map<String, MBeanAttributeWrapper> buildAttributes()
    {
        if (this.attributes == null)
        {
            this.attributes = new HashMap<String, MBeanAttributeWrapper>();
            for (MBeanAttributeInfo attr : this.info.getAttributes())
            {
                this.attributes.put(attr.getName(), new MBeanAttributeWrapper(this, attr));
            }
        }
        return this.attributes;
    }
    
    private Map<String, MBeanOperationWrapper> buildOperations()
    {
        if (this.operations == null)
        {
            this.operations = new HashMap<String, MBeanOperationWrapper>();
            for (MBeanOperationInfo attr : this.info.getOperations())
            {
                this.operations.put(attr.getName(), new MBeanOperationWrapper(this, attr));
            }
        }
        return this.operations;
    }
    
    public String getDomain()
    {
        return this.name.getDomain();
    }
    
    public String getName()
    {
        return this.name.getCanonicalName();
    }
    
    public String getDescription()
    {
        return this.info.getDescription();
    }
    
    public List<MBeanAttributeWrapper> getAttributes()
    {
        return new ArrayList<MBeanAttributeWrapper>(this.buildAttributes().values());
    }
    
    public MBeanAttributeWrapper getAttribute(String name)
    {
        return this.buildAttributes().get(name);
    }
    
    public List<MBeanOperationWrapper> getOperations()
    {
        return new ArrayList<MBeanOperationWrapper>(this.buildOperations().values());
    }
    
    public MBeanOperationWrapper getOperation(String name)
    {
        return this.buildOperations().get(name);
    }
    
    public MBeanOperationWrapper getOperation(String name, String[] signature)
    {
        return this.buildOperations().get(MBeanOperationWrapper.operationId(name, signature));
    }
    
    public MBeanOperationWrapper getOperation(String name, List<String> signature)
    {
        return this.getOperation(name, signature.toArray(new String[signature.size()]));
    }
    
    // get stuff
    
    @SuppressWarnings("unchecked")
    public <T> T getAttributeValue(String attribute)
    {
        try
        {
            return (T) this.convertValue(this.connection.getMBeanServer().getAttribute(this.name, attribute));
        }
        catch (Exception e)
        {
            throw new JMXException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T invokeOperation(String operation, String[] signature, Object[] parameters)
    {
        try
        {
            return (T) this.convertValue(this.connection.getMBeanServer().invoke(this.name, operation, parameters, signature));
        }
        catch (Exception e)
        {
            throw new JMXException(e);
        }
    }
    
    public <T> T invokeOperation(String operation, List<String> signature, List<Object> parameters)
    {
        return this.invokeOperation(operation, signature.toArray(new String[signature.size()]), parameters.toArray());
    }
    
    // handle data
    
    private Object convertValue(Object value)
    {
        if (value instanceof CompositeData)
        {
            CompositeData cd = (CompositeData) value;
            Map<String, Object> data = new HashMap<String, Object>();
            for (String name : cd.getCompositeType().keySet())
            {
                data.put(name, this.convertValue(cd.get(name)));
            }
            return data;
        }
        return value;
    }
}
