package org.simbrain.workspace.updator;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.simbrain.workspace.CouplingManager;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * This class manages the workspace updates, possibly using multiple threads.
 * 
 * The main part of the default update can be found in the DEFAULT_CONTROLLER
 * instance. Each component update call is fed to an executor service which uses
 * as many threads as it's configured to use (it defaults to the number of
 * available processors which can be changed.) Then the executing thread waits
 * on a countdown latch. Each component update decrements the latch so that
 * after the last update is complete, the thread waiting on the latch wakes up
 * and updates all the couplings.
 * 
 * When a single update occurs, it runs off of the AWT event thread. When the
 * continuous update runs, a new thread is spawned, separate from executor
 * service threads. The allows the gui to continue refreshing.
 * 
 * There's a second executor service that's only there to execute event updates.
 * This is to support the need for a view on threads.
 * 
 * The thread factory is used to create a custom thread class that will be
 * generated inside the executor. This allows for a clean way to capture the
 * events using the thread instances themselves which 'know' their thread
 * number.
 * 
 * @author Matt Watson
 */
public class WorkspaceUpdator {

    /** The static logger for the class. */
    static final Logger LOGGER = Logger.getLogger(WorkspaceUpdator.class);
    
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
    
    /** Call-back interface for component updates. */
    private final ComponentUpdator componentUpdator;
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
                return new UpdateThread(WorkspaceUpdator.this, runnable, nextThread++);
            }
        }
    };
    
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
                service.submit(new ComponentUpdate(component, componentUpdator, latch));
            }
        }

        public void updateCouplings() {
            manager.updateAllCouplings();
            
            LOGGER.trace("couplings updated?");
            
            notifyCouplingsUpdated();
        }
    };
    
    /**
     * Constructor for the updator that uses the provided controller and
     * threads.
     * 
     * @param workspace The parent workspace.
     * @param componentUpdator call-back for updating components.
     * @param manager The coupling manager for the workspace.
     * @param controller The update controller.
     * @param threads The number of threads for component updates.
     */
    public WorkspaceUpdator(final Workspace workspace, final ComponentUpdator componentUpdator,
            final CouplingManager manager, final UpdateController controller, final int threads) {
        this.workspace = workspace;
        this.componentUpdator = componentUpdator;
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
     * @param componentUpdator call-back for updating components.
     * @param manager The coupling manager for the workspace.
     * @param controller The update controller.
     */
    public WorkspaceUpdator(final Workspace workspace, final ComponentUpdator componentUpdator,
            final CouplingManager manager, final UpdateController controller) {
        this(workspace, componentUpdator, manager, controller,
            Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Constructor for the updator that uses the default controller and
     * default number of threads.
     * 
     * @param workspace The parent workspace.
     * @param componentUpdator call-back for updating components.
     * @param manager The coupling manager for the workspace.
     */
    public WorkspaceUpdator(final Workspace workspace, final ComponentUpdator componentUpdator,
            final CouplingManager manager) {
        this(workspace, componentUpdator, manager, DEFAULT_CONTROLLER,
            Runtime.getRuntime().availableProcessors());
    }
    
    
    
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
    public void doUpdate() {
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
    void notifyUpdateStarted(final WorkspaceComponent<?> component, final int thread) {
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
    void notifyUpdateFinished(final WorkspaceComponent<?> component, final int thread) {
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
    
    /**
     * synchronizes on all components and executes task, returning the
     * result of that callable.
     * 
     * @param <E> The return type of task.
     * @param task The task to synchronize.
     * @return The result of task.
     * @throws Exception If an exception occurs.
     */
    public <E> E syncOnAllComponents(final Callable<E> task) throws Exception {
        synchronized (componentLock) {
            return syncRest(workspace.getComponentList().iterator(), task);
        }
    }
    
    /**
     * recursively synchronizes on the next component in the iterator and executes
     * task if there are no more components.
     * 
     * @param <E> The return type of task.
     * @param iterator The iterator of the remaining components to synchronize on.
     * @param task The task to synchronize.
     * @return The result of task.
     * @throws Exception If an exception occurs.
     */
    private static <E> E syncRest(final Iterator<? extends Object> iterator, final Callable<E> task)
            throws Exception {
        if (iterator.hasNext()) {
            synchronized (iterator.next()) {
                return syncRest(iterator, task);
            }
        } else {
            return task.call();
        }
    }
}