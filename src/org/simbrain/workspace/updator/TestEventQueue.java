package org.simbrain.workspace.updator;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.InvocationEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

class TestEventQueue extends EventQueue {
    private static final Logger LOGGER = Logger.getLogger(TestEventQueue.class);
    
//    ExecutorService events = Executors.newSingleThreadExecutor();
    final WorkspaceUpdator updator;
//    Queue<AWTEvent> queue = new ConcurrentLinkedQueue<AWTEvent>();
    boolean paused = false;
    Object lock = new Object();
    
    TestEventQueue(final WorkspaceUpdator updator) {
        this.updator = updator;
    }
    
    public void pauseInvocationEvents() {
        synchronized(lock) {
            paused = true;
        }
    }
    
    public void runInvocationEvents() {
//        synchronized(lock) {
//        
//            for (AWTEvent event; (event = queue.poll()) != null;) {
//                LOGGER.debug("event unqueued: " + event);
//                super.postEvent(event);
//            }
//        
////            holdForInput("");
//        
//            paused = false;
//        }
    }
    
    static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    
    static void holdForInput(String out) {
//        System.out.print(out);
        
        try {
            reader.readLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void postEvent(AWTEvent event) {
        LOGGER.debug("event posted: " + event);
        
        if (event instanceof InvocationEvent) {
            event = new TestInvocationEvent((InvocationEvent) event, updator);
//        }
//            synchronized (lock) {
//                if (paused) {
//                    LOGGER.debug("event queued: " + event);
//                    queue.add(event);
//                    return;
//                }
//            }

        }

        super.postEvent(event);
    }
}