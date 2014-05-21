package com.intrbiz.bergamot.model;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.intrbiz.Util;
import com.intrbiz.bergamot.config.model.ContactCfg;
import com.intrbiz.bergamot.model.message.ContactMO;
import com.intrbiz.configuration.Configurable;

public class Contact extends NamedObject<ContactMO> implements Configurable<ContactCfg>
{
    private ContactCfg config;

    private List<Team> teams = new LinkedList<Team>();

    private String password;

    private String email;

    private String pager;

    private String mobile;

    private String phone;

    /**
     * When and how notifications may be send
     */
    protected Notifications notifications;

    public Contact()
    {
        super();
    }

    @Override
    public void configure(ContactCfg cfg)
    {
        this.config = cfg;
        ContactCfg rcfg = cfg.resolve();
        this.name = rcfg.getName();
        this.summary = Util.coalesceEmpty(rcfg.getSummary(), this.name);
        // email
        this.email = rcfg.lookupEmail("work");
        // phones
        this.mobile = rcfg.lookupPhone("mobile");
        this.pager = rcfg.lookupPhone("pager");
        this.phone = rcfg.lookupPhone("work");
    }

    @Override
    public ContactCfg getConfiguration()
    {
        return this.config;
    }

    public List<Team> getTeams()
    {
        return teams;
    }

    public void setTeams(List<Team> teams)
    {
        this.teams = teams;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getPager()
    {
        return pager;
    }

    public void setPager(String pager)
    {
        this.pager = pager;
    }

    public String getMobile()
    {
        return mobile;
    }

    public void setMobile(String mobile)
    {
        this.mobile = mobile;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public Notifications getNotifications()
    {
        return notifications;
    }

    public void setNotifications(Notifications notifications)
    {
        this.notifications = notifications;
    }

    @Override
    public ContactMO toMO(boolean stub)
    {
        ContactMO mo = new ContactMO();
        super.toMO(mo, stub);
        mo.setEmail(this.getEmail());
        mo.setMobile(this.getMobile());
        mo.setPager(this.getPager());
        mo.setPhone(this.getPhone());
        if (!stub)
        {
            mo.setTeams(this.getTeams().stream().map(Team::toStubMO).collect(Collectors.toList()));
            mo.setNotifications(Util.nullable(this.getNotifications(), Notifications::toStubMO));
        }
        return mo;
    }
}
