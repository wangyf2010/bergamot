package com.intrbiz.bergamot.queue;


import com.intrbiz.bergamot.model.message.event.control.ControlEvent;
import com.intrbiz.bergamot.queue.impl.RabbitControlQueue;
import com.intrbiz.queue.Consumer;
import com.intrbiz.queue.DeliveryHandler;
import com.intrbiz.queue.Producer;
import com.intrbiz.queue.QueueAdapter;
import com.intrbiz.queue.QueueManager;

/**
 * Publish and consume control events
 * 
 * Control events are sent from various components into the 
 * Bergamot master cluster.
 * 
 */
public abstract class ControlQueue extends QueueAdapter
{    
    static
    {
        RabbitControlQueue.register();
    }
    
    public static ControlQueue open()
    {
        return QueueManager.getInstance().queueAdapter(ControlQueue.class);
    }
    
    // control events
    
    public abstract Producer<ControlEvent> publishControlEvents();
    
    public abstract Consumer<ControlEvent> consumeControlEvents(DeliveryHandler<ControlEvent> handler);
}