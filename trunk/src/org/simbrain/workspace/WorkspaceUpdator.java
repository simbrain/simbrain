package org.simbrain.workspace;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.InvocationEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;

/**
 * This class manages the workspace updates possibly using
 * multiple threads.
 * 
 * @author Matt Watson
 */
public class WorkspaceUpdator {
    /** The static logger for the class. */
    private static final Logger LOGGER = Logger.getLogger(WorkspaceUpdator.class);
    
    /** The parent workspace. */
    private final Workspace workspace;
    /** The coupling manager for the workspace. */
    private final CouplingManager manager;
    /** */
    private final UpdateController controller;
    /** The executor service for doing updates. */
    private final ExecutorService service;
    /** The executor service for notifying listeners. */
    private final ExecutorService events;
    /** The listeners on this object. */
    private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    /** */
    private final TestEventQueue eventQueue;
    
    /** Whether updates should continue to run. */
    private volatile boolean run = false;
    /** The number of times the update has run. */
    private volatile int time = 0;
    /** The lock used to lock calls on syncAllComponents. */
    private final Object componentLock = new Object();
    
    /** Creates the threads used in the ExecutorService. */
    private final ThreadFactory factory = new ThreadFactory() {
        /** Numbers the threads sequentially. */
        private int nextThread = 1;
        
        /**
         * Creates a new UpdateThread with the current thread number.
         * 
         * @param runnable The runnable this thread will execute.
         */
        public Thread newThread(final Runnable runnable) {
            synchronized (this) {
                return new UpdateThread(runnable, nextThread++);
            }
        }
    };
    
    /**
     * Constructor for the updator that uses the provided controller and
     * threads.
     * 
     * @param workspace The parent workspace.
     * @param manager The coupling manager for the workspace.
     * @param controller The update controller.
     * @param threads The number of threads for component updates.
     */
    public WorkspaceUpdator(final Workspace workspace, final CouplingManager manager,
            final UpdateController controller, final int threads) {
        this.workspace = workspace;
        this.manager = manager;
        this.controller = controller;
        this.service = Executors.newFixedThreadPool(threads, factory);
        this.events = Executors.newSingleThreadExecutor();
        this.eventQueue = new TestEventQueue(this);
        
        addListener(new Listener() {
            public void finishedComponentUpdate(
                    final WorkspaceComponent<?> component, final int update, final int thread) {
                System.out.println("Update: " + update + " thread: "
                    + thread + " finished: " + component);
            }

            public void startingComponentUpdate(
                    final WorkspaceComponent<?> component, final int update, final int thread) {
                System.out.println("Update: " + update + " thread: "
                    + thread + " updating: " + component);
            }

            public void updatedCouplings(final int update) {
                System.out.println("Update: " + update + " Updated couplings");
            }
        });
        
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(eventQueue);
    }
    
    /**
     * Constructor for the updator that uses the default controller and
     * default number of threads.
     * 
     * @param workspace The parent workspace.
     * @param manager The coupling manager for the workspace.
     * @param controller The update controller.
     */
    public WorkspaceUpdator(final Workspace workspace,
            final CouplingManager manager, final UpdateController controller) {
        this(workspace, manager, controller, Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Constructor for the updator that uses the default controller and
     * default number of threads.
     * 
     * @param workspace The parent workspace.
     * @param manager The coupling manager for the workspace.
     */
    public WorkspaceUpdator(final Workspace workspace, final CouplingManager manager) {
        this(workspace, manager, DEFAULT_CONTROLLER, Runtime.getRuntime().availableProcessors());
    }
    
    /** Controls used by Controllers to manage updates. */
    private final UpdateControls controls = new UpdateControls() {
        public List<? extends WorkspaceComponent<?>> getComponents() {
            List<? extends WorkspaceComponent<?>> components = workspace.getComponentList();
            synchronized (components) {
                components = new ArrayList<WorkspaceComponent<?>>(components);
            }
            
            return components;
        }

        public void updateComponent(
                final WorkspaceComponent<?> component, final CountDownLatch latch) {
            synchronized (component) {
                service.submit(new ComponentUpdate(component, latch));
            }
        }

        public void updateCouplings() {
            manager.updateAllCouplings();
            
            LOGGER.trace("couplings updated?");
            
            notifyCouplingsUpdated();
        }
        
    };
    
    /** The default controller. */
    private static final UpdateController DEFAULT_CONTROLLER = new UpdateController() {
        public void doUpdate(final UpdateControls controls) {
            List<? extends WorkspaceComponent<?>> components = controls.getComponents();
            
            int componentCount = components.size();
            
            if (componentCount < 1) return;
            
            CountDownLatch latch = new CountDownLatch(components.size());
            
            for (WorkspaceComponent<?> component : components) {
                controls.updateComponent(component, latch);
            }
            
            try {
                latch.await();
            } catch (InterruptedException e) {
                return;
            }
            
            LOGGER.trace("couplings");
            
            controls.updateCouplings();
        }
    };
    
    /**
     * Returns the 'time' or number of update iterations that have
     * passed.
     * 
     * @return The time.
     */
    public int getTime() {
        return time;
    }
    
    /**
     * Stops the update thread.
     */
    public void stop() {
        run = false;
    }
    
    /**
     * Returns whether the updator is set to run.
     * 
     * @return whether the updator is set to run.
     */
    public boolean isRunning() {
        return run;
    }
    
    /**
     * Starts the update thread.
     */
    public void run() {
        run = true;
        
        new Thread(new Runnable() {
            public void run() {
                while (run) {
                    doUpdate();
                }
            }
        }).start();
    }
    
    /**
     * Executes the updates using the set controller.
     */
    void doUpdate() {
        time++;
        
        LOGGER.trace("starting: " + time);
        
        eventQueue.pauseInvocationEvents();
        
        controller.doUpdate(controls);

        eventQueue.runInvocationEvents();
        
        LOGGER.trace("done: " + time);
    }
    
    /**
     * Adds a listener to this instance.
     * 
     * @param listener The listener to add.
     */
    public void addListener(final Listener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a listener from this instance.
     * 
     * @param listener The listener to add.
     */
    public void removeListener(final Listener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Called when a new component is starting to update.
     * 
     * @param component The component to update.
     * @param thread The number of the thread doing the update.
     */
    private void notifyUpdateStarted(final WorkspaceComponent<?> component, final int thread) {
        final int time = this.time;
        
        events.submit(new Runnable() {
            public void run() {
                for (Listener listener : listeners) {
                    listener.startingComponentUpdate(component, time, thread);
                }
            }
        });
    }
    
    /**
     * Called when a new component is finished updating.
     * 
     * @param component The component to update.
     * @param thread The number of the thread doing the update.
     */
    private void notifyUpdateFinished(final WorkspaceComponent<?> component, final int thread) {
        final int time = this.time;
        
        events.submit(new Runnable() {
            public void run() {
                for (Listener listener : listeners) {
                    listener.finishedComponentUpdate(component, time, thread);
                }
            }
        });
    }
    
    /**
     * Called when the couplings are updated.
     */
    private void notifyCouplingsUpdated() {
        final int time = this.time;
        
        events.submit(new Runnable() {
            public void run() {
                for (Listener listener : listeners) {
                    listener.updatedCouplings(time);
                }
            }
        });
    }
    
    public <E> E syncOnAllComponents(Callable<E> task) throws Exception {
        synchronized (componentLock) {
            return syncRest(workspace.getComponentList().iterator(), task);
        }
    }
    
    public static <E> E syncRest(Iterator<? extends Object> iterator, Callable<E> task) throws Exception {
        if (iterator.hasNext()) {
            synchronized (iterator.next()) {
                return syncRest(iterator, task);
            }
        } else {
            return task.call();
        }
    }
    
    /**
     * This thread class adds some special methods specific to this
     * class.
     * 
     * @author Matt Watson
     */
    private class UpdateThread extends Thread {
        /** The thread number. */
        final int thread;
        
        /**
         * Creates a new instance with the given runnable and thread.
         * 
         * @param runnable The runnable instance.
         * @param thread The thread number.
         */
        UpdateThread(final Runnable runnable, final int thread) {
            super(runnable);
            
            this.thread = thread;
        }
        
        /**
         * Sets the current component update that is being executed.
         * 
         * @param update the component update that is executing.
         */
        void setCurrentTask(final ComponentUpdate update) {
            notifyUpdateStarted(update.component, thread);
        }
        
        /**
         * Clears the current component update.
         * 
         * @param update The component update to be cleared.
         */
        void clearCurrentTask(final ComponentUpdate update) {
            notifyUpdateFinished(update.component, thread);
        }
    }
    
    /**
     * Runnable for updating a specific component.
     * 
     * @author Matt Watson
     */
    private class ComponentUpdate implements Runnable {
        /** The component that this instance updates. */
        private final WorkspaceComponent<?> component;
        /** count down latch used to update couplings after all components are updated. */
        private final CountDownLatch latch;
        
        /**
         * Creates a new instance with the given component and latch.
         * 
         * @param component The component instance.
         * @param latch The latch.
         */
        ComponentUpdate(final WorkspaceComponent<?> component, final CountDownLatch latch) {
            this.component = component;
            this.latch = latch;
        }

        /**
         * Updates the component.
         */
        public void run() {
            UpdateThread thread = (UpdateThread) Thread.currentThread();
            
            thread.setCurrentTask(this);
            
            LOGGER.trace("updating component: " + component);
            
            component.update();
            
            thread.clearCurrentTask(this);
            
            latch.countDown();
        }
    }
    
    /**
     * Interface for controllers to receive the controls required to
     * run custom updates.
     * 
     * @author Matt Watson
     */
    public interface UpdateController {
        /**
         * Starts an update the provided controls should be used to manage the update.
         * 
         * @param controls controls required to run custom updates.
         */
        void doUpdate(UpdateControls controls);
    }
    
    /**
     * Interface provided to custom update methods.
     * 
     * @author Matt Watson
     */
    public interface UpdateControls {
        /**
         * Updates all the couplings.
         */
        void updateCouplings();
        
        /**
         * Returns the components in the workspace.
         * 
         * @return the components in the workspace.
         */
        List<? extends WorkspaceComponent<?>> getComponents();
        
        /**
         * Submits a component for update, calling countDown on the given latch
         * when it is updated.
         * 
         * @param component The component to update.
         * @param latch The latch to countDown when the update is completed.
         */
        void updateComponent(WorkspaceComponent<?> component, CountDownLatch latch);
    }
    
    /**
     * The listener interface for observers interested in events related to
     * component update activities.
     * 
     * @author Matt Watson
     */
    public interface Listener {
        /**
         * Called when a component update begins.
         *  
         * @param component The component being updated.
         * @param thread The thread doing the update.
         */
        void startingComponentUpdate(WorkspaceComponent<?> component, int update, int thread);
        
        /**
         * Called when a component update ends.
         *  
         * @param component The component that was updated.
         * @param thread The thread doing the update.
         */
        void finishedComponentUpdate(WorkspaceComponent<?> component, int update, int thread);
        
        /**
         * Called when the couplings are updated.
         */
        void updatedCouplings(int update);
    }
}

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
    
    class TestInvocationEvent extends InvocationEvent {
        private final InvocationEvent event;
//        private final WorkspaceUpdator updator;
        
        public TestInvocationEvent(final InvocationEvent event, final WorkspaceUpdator updator) {
            super(event.getSource(), new Runnable() {
                public void run() {
                    try {
                        updator.syncOnAllComponents(new Callable<Object>() {
                            public Object call() throws Exception {
                                event.dispatch();
                                return null;
                            }
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            
            this.event = event;
        }
        
        public Exception getException() {
            return event.getException();
        }
        
        public Throwable getThrowable() {
            return event.getThrowable();
        }
        
        public long getWhen() {
            return event.getWhen();
        }
        
        public String paramString() {
            return event.paramString();
        }
    }
}