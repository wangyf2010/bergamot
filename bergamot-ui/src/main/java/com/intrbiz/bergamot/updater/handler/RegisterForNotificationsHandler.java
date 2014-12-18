package com.intrbiz.bergamot.updater.handler;

import org.apache.log4j.Logger;

import com.intrbiz.bergamot.model.message.api.APIRequest;
import com.intrbiz.bergamot.model.message.api.error.APIError;
import com.intrbiz.bergamot.model.message.api.notification.NotificationEvent;
import com.intrbiz.bergamot.model.message.api.notification.RegisterForNotifications;
import com.intrbiz.bergamot.model.message.api.notification.RegisteredForNotifications;
import com.intrbiz.bergamot.model.message.notification.CheckNotification;
import com.intrbiz.bergamot.model.message.notification.Notification;
import com.intrbiz.bergamot.queue.NotificationQueue;
import com.intrbiz.bergamot.updater.context.ClientContext;
import com.intrbiz.queue.Consumer;
import com.intrbiz.queue.QueueException;

public class RegisterForNotificationsHandler extends RequestHandler
{
    private Logger logger = Logger.getLogger(RegisterForNotificationsHandler.class);
    
    public RegisterForNotificationsHandler()
    {
        super(new Class<?>[] { RegisterForNotifications.class });
    }

    @Override
    public void onRequest(ClientContext context, APIRequest request)
    {
        RegisterForNotifications rfns = (RegisterForNotifications) request;
        // validate the site id
        if (! context.getSite().getId().equals(rfns.getSiteId()))
        {
            context.send(new APIError("Invalid site id given"));
        }
        else if (context.var("notificationConsumer") == null)
        {
            // listen for notifications
            logger.info("Registering for notifications, for site: " + rfns.getSiteId());
            try
            {
                NotificationQueue notificationQueue = context.var("notificationQueue", NotificationQueue.open());
                context.var("notificationConsumer", notificationQueue.consumeNotifications((n) -> {
                    if (n instanceof CheckNotification)
                    {
                        logger.debug("Sending notification to client: " + n);
                        context.send(new NotificationEvent((CheckNotification) n));
                    }
                }, rfns.getSiteId()));
                // on close
                context.onClose((ctx) -> {
                    Consumer<Notification> c = ctx.var("notificationConsumer");
                    if (c != null) c.close();
                    NotificationQueue q = ctx.var("notificationQueue");
                    if (q != null) q.close();
                });
                // done
                context.send(new RegisteredForNotifications(rfns));
            }
            catch (QueueException e)
            {
                context.var("notificationQueue", null);
                context.var("notificationConsumer", null);
                logger.error("Failed to setup notification queue", e);
                context.send(new APIError("Failed to setup notification queue"));
            }
        }
        else
        {
            // done
            context.send(new RegisteredForNotifications(rfns));
        }
    }
}