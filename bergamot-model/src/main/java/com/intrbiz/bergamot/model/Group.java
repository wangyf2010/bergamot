package com.intrbiz.bergamot.model;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.intrbiz.Util;
import com.intrbiz.bergamot.config.model.GroupCfg;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.model.message.CheckMO;
import com.intrbiz.bergamot.model.message.GroupMO;
import com.intrbiz.bergamot.model.state.GroupState;
import com.intrbiz.data.db.compiler.meta.SQLColumn;
import com.intrbiz.data.db.compiler.meta.SQLTable;
import com.intrbiz.data.db.compiler.meta.SQLUnique;
import com.intrbiz.data.db.compiler.meta.SQLVersion;

@SQLTable(schema = BergamotDB.class, name = "group", since = @SQLVersion({ 1, 0, 0 }))
@SQLUnique(name = "name_unq", columns = { "site_id", "name" })
public class Group extends NamedObject<GroupMO, GroupCfg> implements Commented
{
    private static final long serialVersionUID = 1L;
    
    /**
     * The groups this group is a member of
     */
    @SQLColumn(index = 1, name = "group_ids", type = "UUID[]", since = @SQLVersion({ 1, 0, 0 }))
    protected List<UUID> groupIds = new LinkedList<UUID>();

    public Group()
    {
        super();
    }

    @Override
    public void configure(GroupCfg cfg)
    {
        super.configure(cfg);
        GroupCfg rcfg = cfg.resolve();
        this.name = rcfg.getName();
        this.summary = Util.coalesceEmpty(rcfg.getSummary(), this.name);
        this.description = Util.coalesceEmpty(rcfg.getDescription(), "");
    }

    public GroupState getState()
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.computeGroupState(this.getId());
        }
    }

    public List<UUID> getGroupIds()
    {
        return groupIds;
    }

    public void setGroupIds(List<UUID> groupIds)
    {
        this.groupIds = groupIds;
    }

    public List<Group> getGroups()
    {
        List<Group> r = new LinkedList<Group>();
        if (this.getGroupIds() != null)
        {
            try (BergamotDB db = BergamotDB.connect())
            {
                for (UUID id : this.getGroupIds())
                {
                    r.add(db.getGroup(id));
                }
            }
        }
        return r;
    }

    public void addParent(Group parent)
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            db.addGroupChild(parent, this);
        }
    }

    public void removeParent(Group parent)
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            db.removeGroupChild(parent, this);
        }
    }

    public List<Group> getChildren()
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getGroupsInGroup(this.getId());
        }
    }

    public void removeChild(Group child)
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            db.removeGroupChild(this, child);
        }
    }

    public void addChild(Group child)
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            db.addGroupChild(this, child);
        }
    }

    public Collection<Check<?,?>> getChecks()
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getChecksInGroup(this.getId());
        }
    }

    public void addCheck(Check<?,?> check)
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            db.addCheckToGroup(this, check);
        }
    }

    public void removeCheck(Check<?,?> check)
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            db.removeCheckFromGroup(this, check);
        }
    }
    
    /**
     * Get comments against this downtime
     * @param limit the maximum number of comments to get
     */
    @Override
    public List<Comment> getComments(int limit)
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getCommentsForObject(this.getId(), 0, limit);
        }
    }

    /**
     * Get comments against this downtime
     */
    @Override
    public List<Comment> getComments()
    {
        return this.getComments(5);
    }

    @Override
    public GroupMO toMO(boolean stub)
    {
        GroupMO mo = new GroupMO();
        super.toMO(mo, stub);
        mo.setState(this.getState().toMO());
        if (!stub)
        {
            mo.setChecks(this.getChecks().stream().map((c) -> {return (CheckMO) c.toStubMO();}).collect(Collectors.toList()));
            mo.setGroups(this.getGroups().stream().map(Group::toStubMO).collect(Collectors.toList()));
            mo.setChildren(this.getChildren().stream().map(Group::toStubMO).collect(Collectors.toList()));
        }
        return mo;
    }
}
