package com.intrbiz.bergamot.notification.engine.sms;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.intrbiz.Util;
import com.intrbiz.accounting.Accounting;
import com.intrbiz.bergamot.accounting.model.SendNotificationToContactAccountingEvent;
import com.intrbiz.bergamot.health.HealthTracker;
import com.intrbiz.bergamot.health.model.KnownDaemon;
import com.intrbiz.bergamot.model.message.ContactMO;
import com.intrbiz.bergamot.model.message.notification.CheckNotification;
import com.intrbiz.bergamot.model.message.notification.Notification;
import com.intrbiz.bergamot.notification.AbstractNotificationEngine;
import com.intrbiz.configuration.CfgParameter;
import com.intrbiz.gerald.source.IntelligenceSource;
import com.intrbiz.gerald.witchcraft.Witchcraft;
import com.intrbiz.queue.QueueException;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;

public class SMSEngine extends AbstractNotificationEngine
{
    public static final String NAME = "sms";

    private Logger logger = Logger.getLogger(SMSEngine.class);

    private String accountSid;

    private String authToken;

    private String from;

    private TwilioRestClient client;

    private MessageFactory messageFactory;
    
    private final Timer smsSendTimer;
    
    private final Counter smsSendErrors;
    
    private Accounting accounting = Accounting.create(SMSEngine.class);
    
    private List<String> healthcheckAdmins = new LinkedList<String>();

    public SMSEngine()
    {
        super(NAME);
        // setup metrics
        IntelligenceSource source = Witchcraft.get().source("com.intrbiz.bergamot.sms");
        this.smsSendTimer = source.getRegistry().timer("sms-sent");
        this.smsSendErrors = source.getRegistry().counter("sms-errors");
    }

    @Override
    protected void configure() throws Exception
    {
        super.configure();
        // auth details
        this.accountSid = this.config.getStringParameterValue("twilio.account", "");
        this.authToken = this.config.getStringParameterValue("twilio.token", "");
        // from number
        this.from = this.config.getStringParameterValue("from", "");
        // setup the client
        logger.info("Using the Twilio account: " + this.accountSid + ", from: " + this.from);
        this.client = new TwilioRestClient(this.accountSid, this.authToken);
        this.messageFactory = client.getAccount().getMessageFactory();
        // who to contact in the event we get a warning from the healthcheck subsystem
        for (CfgParameter param : this.config.getParameters())
        {
            if ("healthcheck.admin".equals(param.getName()) && (! Util.isEmpty(param.getValueOrText())))
                this.healthcheckAdmins.add(param.getValueOrText());
        }
        logger.info("Healthcheck alerts will be sent to " + this.healthcheckAdmins);
        // setup healthchecking
        HealthTracker.getInstance().addAlertHandler(this::raiseHealthcheckAlert);
    }
    
    public void raiseHealthcheckAlert(KnownDaemon failed)
    {
        logger.error("Got healthcheck alert for " + failed.getDaemonName() + " [" + failed.getInstanceId() + "] on host " + failed.getHostName() + " [" + failed.getHostId() + "]");
        if (! this.healthcheckAdmins.isEmpty())
        {
            // really try to send
            for (int i = 0; i < 10; i++)
            {
                try
                {
                    String message = this.buildMessage(failed);
                    // send the SMSes
                    for (String admin : this.healthcheckAdmins)
                    {
                        try
                        {
                            // send the SMS
                            List<NameValuePair> params = new ArrayList<NameValuePair>();
                            params.add(new BasicNameValuePair("To", admin));
                            params.add(new BasicNameValuePair("From", this.from));
                            params.add(new BasicNameValuePair("Body", message));
                            Message sms = this.messageFactory.create(params);
                            logger.info("Sent SMS, Id: " + sms.getSid());
                        }
                        catch (Exception e)
                        {
                            logger.error("Failed to send SMS notification to " + admin);
                        }
                    }
                    // successfully sent
                    break;
                }
                catch (Exception e)
                {
                    logger.error("Failed to send SMS healthcheck notification", e);
                }
            }
        }
    }

    @Override
    public void sendNotification(Notification notification)
    {
        logger.info("Sending SMS notification for " + notification.getNotificationType() + " to " + notification.getTo().stream().map(ContactMO::getPager).filter((e) -> { return e != null; }).collect(Collectors.toList()));
        Timer.Context tctx = this.smsSendTimer.time();
        try
        {
            try
            {
                if (!this.checkAtLeastOneRecipient(notification)) return;
                // build the message
                String message = this.buildMessage(notification);
                if (Util.isEmpty(message)) throw new RuntimeException("Failed to build message, not sending notifications");
                // send the SMSes
                for (ContactMO contact : notification.getTo())
                {
                    if ((!Util.isEmpty(contact.getPager())) && contact.hasEngine(this.getName()))
                    {
                        try
                        {
                            // send the SMS
                            List<NameValuePair> params = new ArrayList<NameValuePair>();
                            params.add(new BasicNameValuePair("To", contact.getPager()));
                            params.add(new BasicNameValuePair("From", this.from));
                            params.add(new BasicNameValuePair("Body", message));
                            Message sms = this.messageFactory.create(params);
                            logger.info("Sent SMS, Id: " + sms.getSid());
                            // accounting
                            this.accounting.account(new SendNotificationToContactAccountingEvent(
                                notification.getSite().getId(),
                                notification.getId(),
                                getObjectId(notification),
                                getNotificationType(notification),
                                contact.getId(),
                                this.getName(),
                                "sms",
                                contact.getPager(),
                                sms.getSid()
                            ));
                        }
                        catch (Exception e)
                        {
                            this.smsSendErrors.inc();
                            logger.error("Failed to send SMS notification to " + contact.getPager() + " - " + contact.getName());
                        }
                    }
                }
            }
            catch (Exception e)
            {
                this.smsSendErrors.inc();
                logger.error("Failed to send SMS notification", e);
                throw new QueueException("Failed to send email notification", e);
            }
        }
        finally
        {
            tctx.stop();
        }
    }

    protected boolean checkAtLeastOneRecipient(Notification notification)
    {
        for (ContactMO contact : notification.getTo())
        {
            if ((!Util.isEmpty(contact.getPager())) && contact.hasEngine(this.getName())) { return true; }
        }
        return false;
    }

    protected String buildMessage(Notification notification) throws Exception
    {
        if (notification instanceof CheckNotification)
        {
            return this.applyTemplate(((CheckNotification)notification).getCheck().getCheckType() + "." + notification.getNotificationType() + ".message", notification);
        }
        else
        {
            return this.applyTemplate(notification.getNotificationType() + ".message", notification);
        }
    }
    
    protected String buildMessage(KnownDaemon daemon) throws Exception
    {
        return this.applyTemplate("healthcheck.alert.message", daemon);
    }
}
