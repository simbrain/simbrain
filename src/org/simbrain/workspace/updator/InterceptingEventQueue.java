package org.simbrain.workspace.updator;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.InvocationEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.updator.SynchronizingInvocationEvent.Signal;

/**
 * An event queue implementing the TaskSynchronizationManager interface.
 * When AWT events are received, each invocation event is wrapped in a
 * synchronizing object.  If the queueTasks toggle is on, each invocation
 * event is queued until the releaseTasks() method is called.  When runTasks()
 * is called, all queued events are executed.
 * 
 * @author Matt Watson
 */
public class InterceptingEventQueue extends EventQueue implements TaskSynchronizationManager {
    /** the static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(InterceptingEventQueue.class);
    
    /** The workspace this object is associated with. */
    private final Workspace workspace;
    /** Internal queue for invocation events. */
    private Queue<AWTEvent> queue = new ConcurrentLinkedQueue<AWTEvent>();
    /** Flag for event queue toggling. */
    private boolean paused = false;
    /** Lock for paused flag access. */
    private Object lock = new Object();
    /** Latch used for tracking when events are done. */
    private final Latch latch = new Latch();
    
    /**
     * Creates a new instance for the given workspace.
     * 
     * @param workspace The workspace to associate this event queue with.
     */
    public InterceptingEventQueue(final Workspace workspace) {
        this.workspace = workspace;
    }
    
    /**
     * starts queuing tasks.
     */
    public void queueTasks() {
        synchronized (lock) {
            paused = true;
        }
    }
    
    /**
     * releases the queuing flag.
     */
    public void releaseTasks() {
        synchronized (lock) {
            paused = false;
        }
    }
    
    /**
     * Executes all queued invocation events.
     */
    public void runTasks() {
        Collection<AWTEvent> events = new ArrayList<AWTEvent>();
        
        for (AWTEvent event; (event = queue.poll()) != null; ) {
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
    
    /**
     * Simple signal with no implementation.
     */
    private final Signal pass = new Signal() {
        public void done() {
            /* no implementation */
        }
        
    };
    
    /**
     * Posts AWTEvents.  If the event is an InvocationEvent, it's wrapped
     * in a synchronizingInvocationEvent.  If queuing is on, these invocation
     * events are queued.
     * 
     * @param event The AWTEvent to post.
     */
    public void postEvent(final AWTEvent event) {
        LOGGER.trace("event posted: " + event);
        
        if (event instanceof InvocationEvent) {
            synchronized (lock) {
                if (paused) {
                    LOGGER.trace("event queued: " + event);
                    
                    queue.add(new SynchronizingInvocationEvent(
                        (InvocationEvent) event, workspace, latch));
                } else {
                    LOGGER.trace("event passed: " + event);
                    
                    super.postEvent(new SynchronizingInvocationEvent(
                        (InvocationEvent) event, workspace, pass));
                }
            }
        } else {
            super.postEvent(event);
        }
    }
    
    /**
     * Signal implementation that wraps a count-down latch.
     * 
     * @author Matt Watson
     */
    private class Latch implements Signal {
        /** the underlying latch. */
        private volatile CountDownLatch counter;
        
        /**
         * Called when a task is done.
         */
        public void done() {
            CountDownLatch counter = this.counter;
            
            counter.countDown();
        }
        
        /**
         * Calls await on the latch.
         */
        public void await() {
            try {
                counter.await();
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}