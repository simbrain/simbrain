/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.workspace.updater;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * This class manages workspace updates. "Running" and "Stepping" the simulation
 * happen here, in a way that allows for concurrent update (in some cases) and
 * also interacts properly with single threaded guis using a
 * "task synchronization manager". Notification events about workspace events
 * are fired from here. Every time the workspace is updated, a list of actions
 * in the UpdateActionManager is invoked. By default one single action, a
 * "buffered update", occurs, in which components are updated in parallel, and
 * when they have all finished updating, couplings are updated. The update
 * action manager can also be used to customize update. Three executor services
 * are here, one for workspace updates (a single thread), one for event
 * notification updates (a single thread), and one for component updates (a
 * thread pool with multiple threads that can be configured), for cases when
 * component updating happens concurrently.
 * 
 * @author Matt Watson
 * @author Jeff Yoshimi
 */
public class WorkspaceUpdater {

    /** The static logger for the class. */
    static final Logger LOGGER = Logger.getLogger(WorkspaceUpdater.class);

    /** The parent workspace. */
    private final Workspace workspace;

    /** The executor service for managing workspace updates. */
    private final ExecutorService workspaceUpdates;

    /** The executor service for doing the component updates. */
    private ExecutorService componentUpdates;

    /** The executor service for notifying listeners. */
    private final ExecutorService notificationEvents;

    /** Component listeners. */
    private final List<ComponentUpdateListener> componentListeners = new CopyOnWriteArrayList<ComponentUpdateListener>();

    /** Updater listeners. */
    private final List<WorkspaceUpdaterListener> updaterListeners = new CopyOnWriteArrayList<WorkspaceUpdaterListener>();

    /** Creates a default synch-manager that does nothing. */
    private volatile TaskSynchronizationManager synchManager = NO_ACTION_SYNCH_MANAGER;

    /** Whether updates should continue to run. */
    private volatile boolean run = false;

    /** The number of times the update has run. */
    private volatile int time = 0;

    /** Number of threads used in the update service. */
    private int numThreads;

    /** The update Manager. */
    private UpdateActionManager updateActionManager;

    /**
     * Constructor for the updater that uses the provided controller and
     * threads.
     *
     * @param workspace The parent workspace.
     * @param controller The update controller.
     * @param threads The number of threads for component updates.
     */
    public WorkspaceUpdater(final Workspace workspace, final int threads) {

    	this.workspace = workspace;
        this.numThreads = threads;
        
        // A single thread updates the workspace
        workspaceUpdates = Executors.newSingleThreadExecutor();
        
		// In some cases components can be updated in parallel. So
		// a thread pool with a configurable number of threads is used
        componentUpdates = Executors.newFixedThreadPool(threads,
                new UpdaterThreadFactory());
        
        // A single thread to fire notification events
        notificationEvents = Executors.newSingleThreadExecutor();
        
        // Instantiate the update action manager
        updateActionManager = new UpdateActionManager(this);

    }

    /**
     * Constructor for the updater that uses the default controller and default
     * number of threads.
     *
     * @param workspace The parent workspace.
     */
    public WorkspaceUpdater(final Workspace workspace) {
        this(workspace, Runtime.getRuntime()
                .availableProcessors());
    }

    /**
     * Sets the manager. Setting the manager to null clears the manager.
     *
     * @param manager the new manager.
     */
    public void setTaskSynchronizationManager(
            final TaskSynchronizationManager manager) {
        if (manager == null) {
            synchManager = NO_ACTION_SYNCH_MANAGER;
        } else {
            synchManager = manager;
        }
    }

    /**
     * Returns the 'time' or number of update iterations that have passed.
     *
     * @return The time.
     */
    public int getTime() {
        return time;
    }

    /**
     * Reset time to 0.
     */
    public void resetTime() {
        time = 0;
    }

    /**
     * Stops the update thread.
     */
    public void stop() {
        run = false;
    }

    /**
     * Returns whether the updater is set to run.
     *
     * @return whether the updater is set to run.
     */
    public boolean isRunning() {
        return run;
    }

    /**
     * Starts the update thread.  Used when "running" the workspace
     * by pressing the play button in the gui.
     */
    public void run() {
        run = true;

        workspaceUpdates.submit(new Runnable() {
            public void run() {
                notifyWorkspaceUpdateStarted();

                synchManager.queueTasks();

                while (run) {
                    try {
                        doUpdate();
                    } catch (Exception e) {
                        // TODO exception handler
                        e.printStackTrace();
                    }
                }

                synchManager.releaseTasks();
                synchManager.runTasks();

                notifyWorkspaceUpdateCompleted();
            }
        });

    }

    /**
     * Submits a single task to the queue, and counts down a latch when done.
     * Used when iterating the workspace for a set number of times.
     *
     * @param latch the latch to count down.
     */
    public void runOnce(final CountDownLatch latch) {
        workspaceUpdates.submit(new Runnable() {
            public void run() {
                notifyWorkspaceUpdateStarted();
                synchManager.queueTasks();

                try {
                    doUpdate();
                } catch (Exception e) {
                    // TODO exception handler
                    e.printStackTrace();
                }

                synchManager.releaseTasks();
                synchManager.runTasks();
                notifyWorkspaceUpdateCompleted();
                latch.countDown();

            }
        });
    }

    /**
     * Submits a single task to the queue.
     */
    public void runOnce() {
        workspaceUpdates.submit(new Runnable() {
            public void run() {
                notifyWorkspaceUpdateStarted();
                synchManager.queueTasks();

                try {
                    doUpdate();
                } catch (Exception e) {
                    // TODO exception handler
                    e.printStackTrace();
                }

                synchManager.releaseTasks();
                synchManager.runTasks();

                notifyWorkspaceUpdateCompleted();

            }
        });
    }

    /**
     * Executes the main workspace update.
     */
    private void doUpdate() {
        time++;

        LOGGER.trace("starting: " + time);

        try {
            Thread.sleep(workspace.getUpdateDelay());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(UpdateAction action : updateActionManager.getActionList()) {
            action.invoke();
        }        

        synchManager.runTasks(); 

        notifyWorkspaceUpdated();

        LOGGER.trace("done: " + time);
    }


    /**
     * Adds a component listener to this instance.
     *
     * @param listener The component listener to add.
     */
    public void addComponentListener(final ComponentUpdateListener listener) {
        componentListeners.add(listener);
    }

    /**
     * Return list of component listeners.
     *
     * @return list of component listeners;
     */
    public List<ComponentUpdateListener> getComponentListeners() {
        return componentListeners;
    }

    /**
     * Removes a component listener from this instance.
     *
     * @param listener The listener to add.
     */
    public void removeComponentListener(final ComponentUpdateListener listener) {
        componentListeners.remove(listener);
    }

    /**
     * Adds an updater listener to this instance.
     *
     * @param listener updater component listener to add.
     */
    public void addUpdaterListener(final WorkspaceUpdaterListener listener) {
        updaterListeners.add(listener);
    }

    /**
     * Return list of updater listeners.
     *
     * @return list of updater listeners;
     */
    public List<WorkspaceUpdaterListener> getUpdaterListeners() {
        return updaterListeners;
    }

    /**
     * Removes an updater listener from this instance.
     *
     * @param listener The updater listener to add.
     */
    public void removeUpdaterListener(final WorkspaceUpdaterListener listener) {
        updaterListeners.remove(listener);
    }

    /**
     * Called when a new component is starting to update.
     *
     * @param component The component to update.
     * @param thread The number of the thread doing the update.
     */
    void notifyComponentUpdateStarted(final WorkspaceComponent component,
            final int thread) {
        final int time = this.time;

        notificationEvents.submit(new Runnable() {
            public void run() {
                for (ComponentUpdateListener listener : componentListeners) {
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
    void notifyComponentUpdateFinished(final WorkspaceComponent component,
            final int thread) {
        final int time = this.time;

        notificationEvents.submit(new Runnable() {
            public void run() {
                for (ComponentUpdateListener listener : componentListeners) {
                    listener.finishedComponentUpdate(component, time, thread);
                }
            }
        });
    }

    /**
     * Called when the couplings are updated.
     */
    protected void notifyCouplingsUpdated() {
        final int time = this.time;

        notificationEvents.submit(new Runnable() {
            public void run() {
                for (WorkspaceUpdaterListener listener : updaterListeners) {
                    listener.updatedCouplings(time);
                }
            }
        });
    }

    /**
     * Called when the workspace update begins.
     */
    private void notifyWorkspaceUpdateStarted() {
        notificationEvents.submit(new Runnable() {
            public void run() {
                for (WorkspaceUpdaterListener listener : updaterListeners) {
                    listener.updatingStarted();
                }
            }
        });
    }

    /**
     * Called when workspace update finishes.
     */
    private void notifyWorkspaceUpdateCompleted() {
        notificationEvents.submit(new Runnable() {
            public void run() {
                for (WorkspaceUpdaterListener listener : updaterListeners) {
                    listener.updatingFinished();
                }
            }
        });
    }

    /**
     * Called after every workspace update .
     */
    private void notifyWorkspaceUpdated() {

        notificationEvents.submit(new Runnable() {
            public void run() {
                for (WorkspaceUpdaterListener listener : updaterListeners) {
                    listener.workspaceUpdated();
                }
            }
        });
    }

    /**
     * Called when update controller is changed.
     */
    private void notifyUpdateControllerChanged() {

        notificationEvents.submit(new Runnable() {
            public void run() {
                for (WorkspaceUpdaterListener listener : updaterListeners) {
                    listener.changedUpdateController();
                }
            }
        });
    }

    /**
     * @return the numThreads
     */
    public int getNumThreads() {
        return numThreads;
    }

    /**
     * Set number of threads in updater.
     *
     * @param numThreads number of threads.
     */
    public void setNumThreads(final int numThreads) {
        if (isRunning()) {
            stop();
        }
        this.numThreads = numThreads;
        this.componentUpdates = Executors.newFixedThreadPool(numThreads,
                new UpdaterThreadFactory());
        for (WorkspaceUpdaterListener listener : updaterListeners) {
            listener.changeNumThreads();
        }

    }

    /**
     * Iterate the updater for a specified number of iterations.
     *
     * @param numIterations number of times to iterate updater.
     */
    public void iterate(final int numIterations) {
        workspaceUpdates.submit(new Runnable() {
            public void run() {
                notifyWorkspaceUpdateStarted();
                for (int i = 0; i < numIterations; i++) {
                    synchManager.queueTasks();

                    try {
                        doUpdate();
                    } catch (Exception e) {
                        // TODO exception handler
                        e.printStackTrace();
                    }
                    synchManager.releaseTasks();
                    synchManager.runTasks();
                }
                notifyWorkspaceUpdateCompleted();
            }
        });
    }

    /** A synch-manager where the methods do nothing. */
    private static final TaskSynchronizationManager NO_ACTION_SYNCH_MANAGER = new TaskSynchronizationManager() {
        public void queueTasks() {
            /* no implementation */
        }

        public void releaseTasks() {
            /* no implementation */
        }

        public void runTasks() {
            /* no implementation */
        }
    };

	/**
	 * Creates the threads used in the ExecutorService. Used to create a custom
	 * thread class that will be generated inside the executor. This allows for
	 * a clean way to capture the events using the thread instances themselves
	 * which 'know' their thread number.
	 */
    private class UpdaterThreadFactory implements ThreadFactory {
        /** Numbers the threads sequentially. */
        private int nextThread = 1;

        /**
         * Creates a new UpdateThread with the current thread number.
         *
         * @param runnable The runnable this thread will execute.
         */
        public Thread newThread(final Runnable runnable) {
            synchronized (this) {
                return new UpdateThread(WorkspaceUpdater.this, runnable,
                        nextThread++);
            }
        }
    }

    /**
     * Returns a reference to the update manager.
     *
     * @return the update manager
     */
	public UpdateActionManager getUpdateManager() {
		return updateActionManager;
	}
	

	/**
	 * Get a synchronized list of component.
	 *
	 * @return the synchronized list of components
	 */
    public List<? extends WorkspaceComponent> getComponents() {
        List<? extends WorkspaceComponent> components = workspace
                .getComponentList();
        synchronized (components) {
            components = new ArrayList<WorkspaceComponent>(components);
        }

        return components;
    }

    /**
     * Update the provided workspace component.
     *
     * @param component the component to update.
     * @param signal completion signal
     */
    public void updateComponent(final WorkspaceComponent component,
            final CompletionSignal signal) {

        // If update is turned off on this component, return
        if (component.getUpdateOn() == false) {
            signal.done();
            return;
        }

        Collection<ComponentUpdatePart> parts = component.getUpdateParts();

        final LatchCompletionSignal partsSignal = new LatchCompletionSignal(
                parts.size()) {
            public void done() {
                super.done();

                /*
                 * I'm not 100% sure this is safe. The JavaDocs don't say it
                 * isn't but they don't say it is either. If a deadlock occurs
                 * in the caller to updateComponent, this may be the issue.
                 */
                if (getLatch().getCount() <= 0) {
                    signal.done();
                }
            }
        };

        for (ComponentUpdatePart part : parts) {
            componentUpdates.submit(part.getUpdate(partsSignal));
        }
    }

    /**
     * Update couplings.
     */
    public void updateCouplings() {
        workspace.getCouplingManager().updateAllCouplings();
        LOGGER.trace("couplings updated");
        workspace.getUpdater().notifyCouplingsUpdated();
    }


    /**
     * {@inheritDoc}
     */
    public void updateOutgoingCouplings(WorkspaceComponent component) {
        workspace.getCouplingManager().updateOutgoingCouplings(component);
    }

    /**
     * {@inheritDoc}
     */
    public void updateIncomingCouplings(WorkspaceComponent component) {
        workspace.getCouplingManager().updateIncomingCouplings(component);
    }

	/**
	 * @return the workspace
	 */
	public Workspace getWorkspace() {
		return workspace;
	}

}
