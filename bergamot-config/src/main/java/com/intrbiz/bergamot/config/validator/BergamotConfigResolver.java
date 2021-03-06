package com.intrbiz.bergamot.config.validator;

import org.apache.log4j.Logger;

import com.codahale.metrics.Timer;
import com.intrbiz.bergamot.config.model.TemplatedObjectCfg;
import com.intrbiz.bergamot.config.model.TimePeriodCfg;
import com.intrbiz.gerald.witchcraft.Witchcraft;

public class BergamotConfigResolver
{   
    private Logger logger = Logger.getLogger(BergamotConfigResolver.class);
    
    private final BergamotObjectLocator locator;
    
    private final Timer lookupTimer = Witchcraft.get().source("com.intrbiz.config.bergamot").getRegistry().timer(Witchcraft.name(BergamotConfigResolver.class, "lookup"));
    
    private final Timer computeInheritenanceTimer = Witchcraft.get().source("com.intrbiz.config.bergamot").getRegistry().timer(Witchcraft.name(BergamotConfigResolver.class, "computeInheritenance"));
    
    private final Timer resolveInheritTimer = Witchcraft.get().source("com.intrbiz.config.bergamot").getRegistry().timer(Witchcraft.name(BergamotConfigResolver.class, "resolveInherit"));
    
    private final Timer resolveChildInheritTimer = Witchcraft.get().source("com.intrbiz.config.bergamot").getRegistry().timer(Witchcraft.name(BergamotConfigResolver.class, "resolveChildInherit"));
    
    private final Timer lookupChildTemplateTimer = Witchcraft.get().source("com.intrbiz.config.bergamot").getRegistry().timer(Witchcraft.name(BergamotConfigResolver.class, "lookupChildTemplate"));
    
    private final Timer resolveExcludesTimer = Witchcraft.get().source("com.intrbiz.config.bergamot").getRegistry().timer(Witchcraft.name(BergamotConfigResolver.class, "resolveExcludes"));
    
    public BergamotConfigResolver(BergamotObjectLocator locator)
    {
        super();
        this.locator = locator;
    }
    
    public BergamotObjectLocator getLocator()
    {
        return this.locator;
    }
    
    private String stipSmartMergePrefix(String name)
    {
        return (name != null && name.length() > 1 && (name.startsWith("-") || name.startsWith("+"))) ? name.substring(1) : name; 
    }
    
    public <T extends TemplatedObjectCfg<T>> T lookup(Class<T> type, String name)
    {
        try (Timer.Context tctx = lookupTimer.time())
        {
            // sanitize the name
            name = this.stipSmartMergePrefix(name);
            T inherited = this.locator.lookup(type, name);
            logger.debug("Looked up inherited object: " + type.getSimpleName() + "::" + name + " ==> " + inherited);
            return inherited;
        }
    }
    
    public <T extends TemplatedObjectCfg<T>> T computeInheritenance(T object)
    {
        this.computeInheritenance(object, null);
        return object;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends TemplatedObjectCfg<T>> T computeInheritenance(T object, BergamotValidationReport report)
    {
        try (Timer.Context tctx = computeInheritenanceTimer.time())
        {
            logger.debug("Computing inheritenance for: " + object.getClass().getSimpleName() + " " + object.getName());
            // resolve the object
            this.resolveInherit(object, report);
            // now resolve the children
            for (TemplatedObjectCfg<?> child : object.getTemplatedChildObjects())
            {
                this.resolveChildInherit(object, (TemplatedObjectCfg) child, report);
            }
            return object;
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void resolveInherit(TemplatedObjectCfg<?> object, BergamotValidationReport report)
    {
        try (Timer.Context tctx = resolveInheritTimer.time())
        {
            if (object.getInherits().isEmpty())
            {
                logger.debug("Resolving inheritenance for: " + object.getClass().getSimpleName() + " " + object.getName());
                for (String inheritsFrom : object.getInheritedTemplates())
                {
                    TemplatedObjectCfg<?> superObject = this.lookup(object.getClass(), inheritsFrom);
                    if (superObject != null)
                    {
                        // we need to recursively ensure that the inherited object is resolved
                        this.computeInheritenance((TemplatedObjectCfg) superObject, report);
                        // add the inherited object
                        ((TemplatedObjectCfg) object).addInheritedObject(superObject);
                    }
                    else
                    {
                        // error
                        if (report != null) report.logError("Error: Cannot find the inherited " + object.getClass().getSimpleName() + " named '" + inheritsFrom + "' which is inherited by " + object);
                    }
                }
                // special cases
                if (object instanceof TimePeriodCfg)
                {
                    resolveExcludes((TimePeriodCfg) object, report);
                }
            }
        }
    }
    
    /**
     * Search recursively up the template chain
     * @param parent the parent object
     * @param name the template we are looking for
     * @return the template if found, or null
     */
    protected <C extends TemplatedObjectCfg<C>> TemplatedObjectCfg<?> lookupChildTemplate(C container, String name)
    {
        try (Timer.Context tctx = lookupChildTemplateTimer.time())
        {
            for (C containerTemplate : container.getInherits())
            {
                // search for the child object in the inherited template
                for (TemplatedObjectCfg<?> child : containerTemplate.getTemplatedChildObjects())
                {
                    if (name.equals(child.getName())) return child;
                }
                // recurse up the template chain
                TemplatedObjectCfg<?> template = this.lookupChildTemplate(containerTemplate, name);
                if (template != null) return template;
            }
            return null;
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <T extends TemplatedObjectCfg<T>, C extends TemplatedObjectCfg<C>> void resolveChildInherit(C container, T object, BergamotValidationReport report)
    {
        try (Timer.Context tctx = resolveChildInheritTimer.time())
        {
            if (object.getInherits().isEmpty())
            {
                logger.debug("Resolving child inheritenance for: " + object.getClass().getSimpleName() + " " + object.getName() + " <<<< " + container.getClass().getSimpleName() + " " + container.getName());
                // Firstly look for are we inheriting from a child object of the parent object's templates
                for (String inheritsFrom : object.getInheritedTemplates())
                {
                    // lookup in the containers inheritance tree
                    TemplatedObjectCfg<?> superObject = this.lookupChildTemplate(container, inheritsFrom);
                    // fallback to a global template
                    if (superObject == null) superObject = this.lookup(object.getClass(), inheritsFrom);
                    // yay or nay
                    if (superObject != null)
                    {
                        // resolve the super object
                        this.resolveInherit(superObject, report);
                        // add the inherited object
                        ((TemplatedObjectCfg) object).addInheritedObject(superObject);
                    }
                    else
                    {
                        if (report != null) report.logError("Error: Cannot find the inherited " + object.getClass().getSimpleName() + " named '" + inheritsFrom + "' which is inherited by " + object);
                    }
                }
            }
        }
    }
    
    protected TimePeriodCfg resolveExcludes(TimePeriodCfg object, BergamotValidationReport report)
    {
        try (Timer.Context tctx = resolveExcludesTimer.time())
        {
            for (String exclude : object.getExcludes())
            {
                TimePeriodCfg excludedTimePeriod = this.lookup(TimePeriodCfg.class, exclude);
                if (excludedTimePeriod == null)
                {
                    // error
                    if (report != null) report.logError("Cannot find the excluded time period named '" + exclude + "' which is excluded by " + object);
                }
            }
            return object;
        }
    }
}
