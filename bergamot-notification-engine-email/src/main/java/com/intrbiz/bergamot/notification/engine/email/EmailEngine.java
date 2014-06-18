package com.intrbiz.bergamot.notification.engine.email;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.intrbiz.Util;
import com.intrbiz.bergamot.model.message.ContactMO;
import com.intrbiz.bergamot.model.message.notification.Notification;
import com.intrbiz.bergamot.model.message.notification.SendRecovery;
import com.intrbiz.bergamot.notification.AbstractNotificationEngine;

public class EmailEngine extends AbstractNotificationEngine
{
    public static final String NAME = "email";

    private Logger logger = Logger.getLogger(EmailEngine.class);

    private Properties properties = new Properties();

    private Session session;

    private String user;

    private String password;

    private String fromAddress;

    public EmailEngine()
    {
        super(NAME);
    }

    @Override
    protected void configure() throws Exception
    {
        super.configure();
        // setup the JavaMail properties
        this.properties = new Properties();
        this.properties.put("mail.smtp.host", this.config.getStringParameterValue("mail.host", "127.0.0.1"));
        this.properties.put("mail.smtp.port", this.config.getStringParameterValue("mail.port", "25"));
        if (this.config.getBooleanParameterValue("mail.tls", false))
        {
            this.properties.put("mail.smtp.port", this.config.getStringParameterValue("mail.port", "587"));
            this.properties.put("mail.smtp.starttls.enable", true);
            this.properties.put("mail.smtp.auth", true);
        }
        this.properties.put("mail.smtp.from", this.config.getStringParameterValue("from", "bergamot@localhost"));
        // auth details
        this.user = this.config.getStringParameterValue("mail.user", "");
        this.password = this.config.getStringParameterValue("mail.password", "");
        // from address
        this.fromAddress = this.config.getStringParameterValue("from", "bergamot@localhost");
        // setup the JavaMail session
        this.session = Session.getDefaultInstance(properties);
        this.session.setDebug(true);
    }

    @Override
    public void sendNotification(Notification notification)
    {
        if (logger.isTraceEnabled()) logger.trace(notification.toString());
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        logger.info("Sending email notification for " + notification.getCheck() + " to " + notification.getTo());
        try
        {
            if (! this.checkAtLeastOneRecipient(notification)) return;
            // build the message
            Message message = this.buildMessage(this.session, notification);
            message.writeTo(System.out);
            logger.debug("Built message");
            // send the message
            Transport transport = null;
            try
            {
                transport = session.getTransport("smtp");
                // connect
                if (Util.isEmpty(this.user))
                {
                    logger.debug("Transport connecting to: " + this.properties.getProperty("mail.smtp.host"));
                    transport.connect();
                    logger.debug("Transport connected");
                }
                else
                {
                    logger.debug("Transport connecting to: " + this.properties.getProperty("mail.smtp.host") + " with username: " + this.user);
                    transport.connect(this.user, this.password);
                    logger.debug("Transport connected");
                }
                // send the message
                logger.info("Sending message...");
                transport.sendMessage(message, message.getAllRecipients());
                logger.info("Message sent");
            }
            finally
            {
                if (transport != null) transport.close();
            }
        }
        catch (Throwable e)
        {
            logger.error("Failed to send email notification", e);
        }

    }
    
    protected boolean checkAtLeastOneRecipient(Notification notification)
    {
        for (ContactMO contact : notification.getTo())
        {
            if ((!Util.isEmpty(contact.getEmail())) && contact.hasEngine(this.getName()))
            {
                return true;
            }
        }
        return false;
    }

    protected Message buildMessage(Session session, Notification notification) throws Exception
    {
        Message message = new MimeMessage(session) {
            @Override
            protected void updateMessageID() throws MessagingException
            {
                // do not alter the message id
            }
        };
        // from
        message.setFrom(new InternetAddress(this.fromAddress));
        // to address
        for (ContactMO contact : notification.getTo())
        {
            if ((!Util.isEmpty(contact.getEmail())) && contact.hasEngine(this.getName()))
            {
                message.addRecipient(RecipientType.TO, new InternetAddress(contact.getEmail()));
            }
        }
        // headers
        message.setHeader("X-Bergamot-Alert-Id", notification.getAlertId().toString());
        message.setHeader("Message-ID", this.messageId(notification));
        message.setHeader("X-Bergamot-Notification-Id", notification.getId().toString());
        message.setHeader("X-Bergamot-Check-Type", notification.getCheck().getType());
        message.setHeader("X-Bergamot-Check-Id", notification.getCheck().getId().toString());
        message.setHeader("X-Bergamot-Check-Status", notification.getCheck().getState().getStatus().toString());
        // in reply to?
        if (notification instanceof SendRecovery)
        {
            message.setHeader("References", this.messageId(notification));
            message.setHeader("In-Reply-To", this.messageId(notification));
        }
        // sent date
        message.setSentDate(new Date(notification.getRaised()));
        // content
        this.buildMessageContent(message, notification);
        return message;
    }
    
    protected String messageId(Notification notification)
    {
        return "<" + notification.getAlertId().toString() + ".bergamot>";
    }

    protected void buildMessageContent(Message message, Notification notification) throws Exception
    {
        String templatePrefix = notification.getCheck().getCheckType() + "." + notification.getNotificationType() + ".";
        message.setSubject(this.applyTemplate(templatePrefix + "subject", notification));
        message.setContent(this.applyTemplate(templatePrefix + "content", notification), "text/plain");
    }
    
    @Override
    protected Map<String, String> getDefaultTemplates()
    {        
        Map<String, String> templates = new HashMap<String, String>();
        // host
        templates.put("host.alert.subject", "Alert for host #{host.summary} is #{if(host.state.ok, 'UP', 'DOWN')}");
        templates.put("host.alert.content", "Alert for host #{host.summary} is #{if(host.state.ok, 'UP', 'DOWN')}\r\n"
                + "\r\n"
                + "Check output: #{host.state.output}\r\n"
                + "\r\n"
                + "Last checked at #{dateformat('HH:mm:ss', host.state.lastCheckTime)} on #{dateformat('EEEE dd/MM/yyyy', host.state.lastCheckTime)}\r\n"
                + "\r\n"
                + "For more information: http://bergamot/host/id/#{host.id}\r\n"
                + "\r\n"
                + "Thank you, Bergamot");
        templates.put("host.recovery.subject", "Recovery for host #{host.summary} is #{if(host.state.ok, 'UP', 'DOWN')}");
        templates.put("host.recovery.content", "Recovery for host #{host.summary} is #{if(host.state.ok, 'UP', 'DOWN')}\r\n"
                + "\r\n"
                + "Check output: #{host.state.output}\r\n"
                + "\r\n"
                + "Last checked at #{dateformat('HH:mm:ss', host.state.lastCheckTime)} on #{dateformat('EEEE dd/MM/yyyy', host.state.lastCheckTime)}\r\n"
                + "\r\n"
                + "For more information: http://bergamot/host/id/#{host.id}\r\n"
                + "\r\n"
                + "Thank you, Bergamot");
        // cluster
        templates.put("cluster.alert.subject", "Alert for cluster #{cluster.summary} is #{if(cluster.state.ok, 'UP', 'DOWN')}");
        templates.put("cluster.alert.content", "Alert for cluster #{cluster.summary} is #{if(cluster.state.ok, 'UP', 'DOWN')}\r\n"
                + "\r\n"
                + "Last checked at #{dateformat('HH:mm:ss', cluster.state.lastCheckTime)} on #{dateformat('EEEE dd/MM/yyyy', cluster.state.lastCheckTime)}\r\n"
                + "\r\n"
                + "For more information: http://bergamot/cluster/id/#{cluster.id}\r\n"
                + "\r\n"
                + "Thank you, Bergamot");
        templates.put("cluster.recovery.subject", "Recovery for cluster #{cluster.summary} is #{if(cluster.state.ok, 'UP', 'DOWN')}");
        templates.put("cluster.recovery.content", "Recovery for cluster #{cluster.summary} is #{if(cluster.state.ok, 'UP', 'DOWN')}\r\n"
                + "\r\n"
                + "Last checked at #{dateformat('HH:mm:ss', cluster.state.lastCheckTime)} on #{dateformat('EEEE dd/MM/yyyy', cluster.state.lastCheckTime)}\r\n"
                + "\r\n"
                + "For more information: http://bergamot/cluster/id/#{cluster.id}\r\n"
                + "\r\n"
                + "Thank you, Bergamot");
        // service
        templates.put("service.alert.subject", "Alert for service #{service.summary} on the host #{host.summary} is #{service.state.status}");
        templates.put("service.alert.content", "Alert for service #{service.summary} on the host #{host.summary} is #{service.state.status}\r\n"
                + "\r\n"
                + "Check output: #{service.state.output}\r\n"
                + "\r\n"
                + "Last checked at #{dateformat('HH:mm:ss', service.state.lastCheckTime)} on #{dateformat('EEEE dd/MM/yyyy', service.state.lastCheckTime)}\r\n"
                + "\r\n"
                + "For more information: http://bergamot/service/id/#{service.id}\r\n"
                + "\r\n"
                + "Thank you, Bergamot");
        templates.put("service.recovery.subject", "Recovery for service #{service.summary} on the host #{host.summary} is #{service.state.status}");
        templates.put("service.recovery.content", "Recovery for service #{service.summary} on the host #{host.summary} is #{service.state.status}\r\n"
                + "\r\n"
                + "Check output: #{service.state.output}\r\n"
                + "\r\n"
                + "Last checked at #{dateformat('HH:mm:ss', service.state.lastCheckTime)} on #{dateformat('EEEE dd/MM/yyyy', service.state.lastCheckTime)}\r\n"
                + "\r\n"
                + "For more information: http://bergamot/service/id/#{service.id}\r\n"
                + "\r\n"
                + "Thank you, Bergamot");
        // trap
        templates.put("trap.alert.subject", "Alert for trap #{trap.summary} on the host #{host.summary} is #{trap.state.status}");
        templates.put("trap.alert.content", "Alert for trap #{trap.summary} on the host #{host.summary} is #{trap.state.status}\r\n"
                + "\r\n"
                + "Check output: #{trap.state.output}\r\n"
                + "\r\n"
                + "Last checked at #{dateformat('HH:mm:ss', trap.state.lastCheckTime)} on #{dateformat('EEEE dd/MM/yyyy', trap.state.lastCheckTime)}\r\n"
                + "\r\n"
                + "For more information: http://bergamot/trap/id/#{trap.id}\r\n"
                + "\r\n"
                + "Thank you, Bergamot");
        templates.put("trap.recovery.subject", "Recovery for trap #{trap.summary} on the host #{host.summary} is #{trap.state.status}");
        templates.put("trap.recovery.content", "Recovery for trap #{trap.summary} on the host #{host.summary} is #{trap.state.status}\r\n"
                + "\r\n"
                + "Check output: #{trap.state.output}\r\n"
                + "\r\n"
                + "Last checked at #{dateformat('HH:mm:ss', trap.state.lastCheckTime)} on #{dateformat('EEEE dd/MM/yyyy', trap.state.lastCheckTime)}\r\n"
                + "\r\n"
                + "For more information: http://bergamot/trap/id/#{trap.id}\r\n"
                + "\r\n"
                + "Thank you, Bergamot");
        // resource
        templates.put("resource.alert.subject", "Alert for resource #{resource.summary} on the cluster #{cluster.summary} is #{resource.state.status}");
        templates.put("resource.alert.content", "Alert for resource #{resource.summary} on the cluster #{cluster.summary} is #{resource.state.status}\r\n"
                + "\r\n"
                + "Last checked at #{dateformat('HH:mm:ss', resource.state.lastCheckTime)} on #{dateformat('EEEE dd/MM/yyyy', resource.state.lastCheckTime)}\r\n"
                + "\r\n"
                + "For more information: http://bergamot/resource/id/#{resource.id}\r\n"
                + "\r\n"
                + "Thank you, Bergamot");
        templates.put("resource.recovery.subject", "Recovery for resource #{resource.summary} on the cluster #{cluster.summary} is #{resource.state.status}");
        templates.put("resource.recovery.content", "Recovery for resource #{resource.summary} on the cluster #{cluster.summary} is #{resource.state.status}\r\n"
                + "\r\n"
                + "Last checked at #{dateformat('HH:mm:ss', resource.state.lastCheckTime)} on #{dateformat('EEEE dd/MM/yyyy', resource.state.lastCheckTime)}\r\n"
                + "\r\n"
                + "For more information: http://bergamot/resource/id/#{resource.id}\r\n"
                + "\r\n"
                + "Thank you, Bergamot");
        return templates;
    }
}