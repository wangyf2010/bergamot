package com.intrbiz.bergamot.notification.config;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.intrbiz.bergamot.config.NotificationEngineCfg;
import com.intrbiz.bergamot.config.NotifierCfg;
import com.intrbiz.bergamot.notification.engine.slack.SlackEngine;

@XmlType(name = "notifier")
@XmlRootElement(name = "notifier")
public class SlackNotifierCfg extends NotifierCfg
{
    private static final long serialVersionUID = 1L;

    @Override
    public void applyDefaults()
    {
        super.applyDefaults();
        if (this.getEngines().isEmpty())
        {
            this.getEngines().add(
                    new NotificationEngineCfg(SlackEngine.class)
            );
        }

    }
}
