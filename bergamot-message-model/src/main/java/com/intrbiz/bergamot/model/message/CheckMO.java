package com.intrbiz.bergamot.model.message;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.intrbiz.bergamot.model.message.state.CheckStateMO;

public abstract class CheckMO extends NamedObjectMO
{
    @JsonProperty("state")
    protected CheckStateMO state;

    @JsonProperty("suppressed")
    protected boolean suppressed;

    @JsonProperty("enabled")
    protected boolean enabled;
    
    @JsonProperty("groups")
    protected List<GroupMO> groups = new LinkedList<GroupMO>();
    
    @JsonProperty("referenced_by")
    protected List<? extends VirtualCheckMO> referencedBy = new LinkedList<VirtualCheckMO>();
    
    @JsonProperty("contacts")
    protected List<ContactMO> contacts = new LinkedList<ContactMO>();
    
    @JsonProperty("teams")
    protected List<TeamMO> teams = new LinkedList<TeamMO>();
    
    @JsonProperty("notifications")
    protected NotificationsMO notifications;

    public CheckMO()
    {
        super();
    }
    
    @JsonIgnore
    public abstract String getCheckType();

    public CheckStateMO getState()
    {
        return state;
    }

    public void setState(CheckStateMO state)
    {
        this.state = state;
    }

    public boolean isSuppressed()
    {
        return suppressed;
    }

    public void setSuppressed(boolean suppressed)
    {
        this.suppressed = suppressed;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public List<GroupMO> getGroups()
    {
        return groups;
    }

    public void setGroups(List<GroupMO> groups)
    {
        this.groups = groups;
    }

    public List<? extends VirtualCheckMO> getReferencedBy()
    {
        return referencedBy;
    }

    public void setReferencedBy(List<? extends VirtualCheckMO> referencedBy)
    {
        this.referencedBy = referencedBy;
    }

    public List<ContactMO> getContacts()
    {
        return contacts;
    }

    public void setContacts(List<ContactMO> contacts)
    {
        this.contacts = contacts;
    }

    public List<TeamMO> getTeams()
    {
        return teams;
    }

    public void setTeams(List<TeamMO> teams)
    {
        this.teams = teams;
    }

    public NotificationsMO getNotifications()
    {
        return notifications;
    }

    public void setNotifications(NotificationsMO notifications)
    {
        this.notifications = notifications;
    }
}