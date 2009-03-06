package org.simbrain.workspace.updator;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.InvocationEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.simbrain.workspace.updator.TestInvocationEvent.Signal;

class TestEventQueue extends EventQueue {
    private static final Logger LOGGER = Logger.getLogger(TestEventQueue.class);
    
    private final WorkspaceUpdator updator;
    private Queue<AWTEvent> queue = new ConcurrentLinkedQueue<AWTEvent>();
    private volatile boolean paused = false;
    private Object lock = new Object();
    private final Latch latch = new Latch();
    
    TestEventQueue(final WorkspaceUpdator updator) {
        this.updator = updator;
    }
    
    void queueInvocationEvents() {
        synchronized(lock) {
            paused = true;
        }
    }
    
    void releaseInvocationEvents() {
        synchronized(lock) {
            paused = false;
        }
        
        runInvocationEvents();
    }
    
    void runInvocationEvents() {
        Collection<AWTEvent> events = new ArrayList<AWTEvent>();
        
        for (AWTEvent event; (event = queue.poll()) != null;) {
            events.add(event);
        }
        
        latch.counter = new CountDownLatch(events.size());
        
        for (AWTEvent event : events) {
            LOGGER.debug("event unqueued: " + event);
            super.postEvent(event);
        }
        
        latch.await();
        
        latch.counter = null;
    }
    
    static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    
    static void holdForInput(String out) {
        try {
            reader.readLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private final Signal pass = new Signal() {
        public void done() {
            /* no implementation */
        }
        
    };
    
    public void postEvent(AWTEvent event) {
        LOGGER.debug("event posted: " + event);
        
        if (event instanceof InvocationEvent) {
            synchronized(lock) {
                if (paused) {
                    event = new TestInvocationEvent((InvocationEvent) event, updator, latch);
                    LOGGER.debug("event queued: " + event);
                    queue.add(event);
                    return;
                } else {
                    LOGGER.debug("event passed: " + event);
                    event = new TestInvocationEvent((InvocationEvent) event, updator, pass);
                }
            }
        }

        super.postEvent(event);
    }
    
    private class Latch implements Signal {
        volatile CountDownLatch counter;
        
        public void done() {
            CountDownLatch counter = this.counter;
            
            counter.countDown();
        }
        
        public void await() {
            try {
                counter.await();
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}