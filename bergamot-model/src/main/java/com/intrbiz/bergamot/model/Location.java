package com.intrbiz.bergamot.model;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.intrbiz.Util;
import com.intrbiz.bergamot.config.model.LocationCfg;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.model.message.LocationMO;
import com.intrbiz.bergamot.model.state.GroupState;
import com.intrbiz.data.db.compiler.meta.Action;
import com.intrbiz.data.db.compiler.meta.SQLColumn;
import com.intrbiz.data.db.compiler.meta.SQLForeignKey;
import com.intrbiz.data.db.compiler.meta.SQLTable;
import com.intrbiz.data.db.compiler.meta.SQLUnique;
import com.intrbiz.data.db.compiler.meta.SQLVersion;

/**
 * The physical (probably) location of a host
 */
@SQLTable(schema = BergamotDB.class, name = "location", since = @SQLVersion({ 1, 0, 0 }))
@SQLUnique(name = "name_unq", columns = { "site_id", "name" })
public class Location extends NamedObject<LocationMO, LocationCfg>
{
    private static final long serialVersionUID = 1L;

    @SQLColumn(index = 1, name = "location_id", since = @SQLVersion({ 1, 0, 0 }))
    @SQLForeignKey(references = Location.class, on = "id", onDelete = Action.SET_NULL, onUpdate = Action.RESTRICT)
    protected UUID locationId;

    @SQLColumn(index = 2, name = "worker_pool", since = @SQLVersion({ 1, 0, 0 }))
    protected String workerPool;

    public Location()
    {
        super();
    }

    @Override
    public void configure(LocationCfg cfg)
    {
        super.configure(cfg);
        LocationCfg rcfg = cfg.resolve();
        this.name = rcfg.getName();
        this.summary = Util.coalesceEmpty(rcfg.getSummary(), this.name);
        this.description = Util.coalesceEmpty(rcfg.getDescription(), "");
        this.workerPool = rcfg.getWorkerPool();
    }

    public List<Host> getHosts()
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getHostsInLocation(this.getId());
        }
    }

    public void addHost(Host host)
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            db.addLocationHost(this, host);
        }
    }

    public void removeHost(Host host)
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            db.removeLocationHost(this, host);
        }
    }

    public List<Location> getChildren()
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getLocationsInLocation(this.getId());
        }
    }

    public void addChild(Location child)
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            db.addLocationChild(this, child);
        }
    }

    public void removeChild(Location child)
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            db.removeLocationChild(this, child);
        }
    }

    public Location getLocation()
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getLocation(this.getLocationId());
        }
    }

    public UUID getLocationId()
    {
        return locationId;
    }

    public void setLocationId(UUID locationId)
    {
        this.locationId = locationId;
    }

    public String getWorkerPool()
    {
        return workerPool;
    }

    public void setWorkerPool(String workerPool)
    {
        this.workerPool = workerPool;
    }
    
    public String resolveWorkerPool()
    {
        String workerPool = this.getWorkerPool();
        if (workerPool == null)
        {
            workerPool = Util.nullable(this.getLocation(), Location::resolveWorkerPool);
        }
        return workerPool;
    }

    public GroupState getState()
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.computeLocationState(this.getId());
        }
    }

    @Override
    public LocationMO toMO(boolean stub)
    {
        LocationMO mo = new LocationMO();
        super.toMO(mo, stub);
        mo.setState(this.getState().toMO());
        if (!stub)
        {
            mo.setLocation(Util.nullable(this.getLocation(), Location::toStubMO));
            mo.setChildren(this.getChildren().stream().map(Location::toStubMO).collect(Collectors.toList()));
            mo.setHosts(this.getHosts().stream().map(Host::toStubMO).collect(Collectors.toList()));
        }
        return mo;
    }
}