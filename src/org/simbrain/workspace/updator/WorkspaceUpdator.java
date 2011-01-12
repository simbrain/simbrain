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
package org.simbrain.workspace.updator;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.simbrain.workspace.CouplingManager;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * This class manages the workspace updates, possibly using multiple threads.
 * The main part of the default update can be found in the DEFAULT_CONTROLLER
 * instance. Each component update call is fed to an executor service which uses
 * as many threads as it's configured to use (it defaults to the number of
 * available processors which can be changed.) Then the executing thread waits
 * on a countdown latch. Each component update decrements the latch so that
 * after the last update is complete, the thread waiting on the latch wakes up
 * and updates all the couplings. There's a second executor service that's only
 * there to execute event updates. This is to support the need for a view on
 * threads. The thread factory is used to create a custom thread class that will
 * be generated inside the executor. This allows for a clean way to capture the
 * events using the thread instances themselves which 'know' their thread
 * number.
 *
 * @author Matt Watson
 */
public class WorkspaceUpdator {

    /** The static logger for the class. */
    static final Logger LOGGER = Logger.getLogger(WorkspaceUpdator.class);

    /** The parent workspace. */
    private final Workspace workspace;

    /** The executor service for managing updates. */
    private final ExecutorService updates;

    /** The executor service for doing updates. */
    private ExecutorService service;

    /** The executor service for notifying listeners. */
    private final ExecutorService events;

    /** Component listeners. */
    private final List<ComponentUpdateListener> componentListeners = new CopyOnWriteArrayList<ComponentUpdateListener>();

    /** Updator listeners. */
    private final List<WorkspaceUpdatorListener> updatorListeners = new CopyOnWriteArrayList<WorkspaceUpdatorListener>();

    /** creates a default synch-manager that does nothing. */
    private volatile TaskSynchronizationManager snychManager = NO_ACTION_SYNCH_MANAGER;

    /** Whether updates should continue to run. */
    private volatile boolean run = false;

    /** The number of times the update has run. */
    private volatile int time = 0;

    /** Number of threads used in the update service. */
    private int numThreads;

    /** The default update controls. */
    private final DefaultUpdateControls controls;

    /** The default controller. */
    public static final UpdateController DEFAULT_CONTROLLER = new BufferedUpdator();

    /** The Update Controller. */
    private UpdateController updateController;

    /**
     * Types of update controller.
     */
    public enum TYPE {
        BUFFERED {
            @Override
            public String toString() {
                return "Buffered";
            }
        },
        PRIORITY {
            @Override
            public String toString() {
                return "Priority-based";
            }
        },
        CUSTOM {
            @Override
            public String toString() {
                return "Custom";
            }
        }
    }

    /**
     * Constructor for the updator that uses the provided controller and
     * threads.
     *
     * @param workspace The parent workspace.
     * @param controller The update controller.
     * @param threads The number of threads for component updates.
     */
    public WorkspaceUpdator(final Workspace workspace,
            final UpdateController controller, final int threads) {
        this.workspace = workspace;
        this.updateController = controller;
        this.updates = Executors.newSingleThreadExecutor();
        this.service = Executors.newFixedThreadPool(threads,
                new UpdatorThreadFactory());
        this.events = Executors.newSingleThreadExecutor();
        this.numThreads = threads;
        controls = new DefaultUpdateControls(workspace, service);

    }

    /**
     * Constructor for the updator that uses the default controller and default
     * number of threads.
     *
     * @param workspace The parent workspace.
     * @param controller The update controller.
     */
    public WorkspaceUpdator(final Workspace workspace, final UpdateController controller) {
        this(workspace, controller, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Constructor for the updator that uses the default controller and default
     * number of threads.
     *
     * @param workspace The parent workspace.
     */
    public WorkspaceUpdator(final Workspace workspace) {
        this(workspace, DEFAULT_CONTROLLER, Runtime.getRuntime()
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
            snychManager = NO_ACTION_SYNCH_MANAGER;
        } else {
            snychManager = manager;
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

        updates.submit(new Runnable() {
            public void run() {
                notifyWorkspaceUpdateStarted();

                snychManager.queueTasks();

                while (run) {
                    try {
                        doUpdate();
                    } catch (Exception e) {
                        // TODO exception handler
                        e.printStackTrace();
                    }
                }

                snychManager.releaseTasks();
                snychManager.runTasks();

                notifyWorkspaceUpdateCompleted();
            }
        });

    }

    /**
     * Submits a single task to the queue.
     */
    public void runOnce() {
        updates.submit(new Runnable() {
            public void run() {
                notifyWorkspaceUpdateStarted();
                snychManager.queueTasks();

                try {
                    doUpdate();
                } catch (Exception e) {
                    // TODO exception handler
                    e.printStackTrace();
                }

                snychManager.releaseTasks();
                snychManager.runTasks();
                
                notifyWorkspaceUpdateCompleted();

            }
        });
    }

    /**
     * Executes the updates using the set controller.
     */
    private void doUpdate() {
        time++;

        LOGGER.trace("starting: " + time);

        try {
            Thread.sleep(workspace.getUpdateDelay());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        updateController.doUpdate(controls);

        snychManager.runTasks();

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
     * Adds an updator listener to this instance.
     *
     * @param listener updator component listener to add.
     */
    public void addUpdatorListener(final WorkspaceUpdatorListener listener) {
        updatorListeners.add(listener);
    }

    /**
     * Return list of updator listeners.
     *
     * @return list of updator listeners;
     */
    public List<WorkspaceUpdatorListener> getUpdatorListeners() {
        return updatorListeners;
    }

    /**
     * Removes an updator listener from this instance.
     *
     * @param listener The updator listener to add.
     */
    public void removeUpdatorListener(final WorkspaceUpdatorListener listener) {
        updatorListeners.remove(listener);
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

        events.submit(new Runnable() {
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

        events.submit(new Runnable() {
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

        events.submit(new Runnable() {
            public void run() {
                for (WorkspaceUpdatorListener listener : updatorListeners) {
                    listener.updatedCouplings(time);
                }
            }
        });
    }

    /**
     * Called when the workspace update begins.
     */
    private void notifyWorkspaceUpdateStarted() {
        events.submit(new Runnable() {
            public void run() {
                for (WorkspaceUpdatorListener listener : updatorListeners) {
                    listener.updatingStarted();
                }
            }
        });
    }

    /**
     * Called when workspace update finishes.
     */
    private void notifyWorkspaceUpdateCompleted() {
        events.submit(new Runnable() {
            public void run() {
                for (WorkspaceUpdatorListener listener : updatorListeners) {
                    listener.updatingFinished();
                }
            }
        });
    }

    /**
     * Called after every workspace update .
     */
    private void notifyWorkspaceUpdated() {

        events.submit(new Runnable() {
            public void run() {
                for (WorkspaceUpdatorListener listener : updatorListeners) {
                    listener.workspaceUpdated();
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
     * Set number of threads in updator.
     *
     * @param numThreads number of threads.
     */
    public void setNumThreads(final int numThreads) {
        if (isRunning()) {
            stop();
        }
        this.numThreads = numThreads;
        this.service = Executors.newFixedThreadPool(numThreads,
                new UpdatorThreadFactory());
        for (WorkspaceUpdatorListener listener : updatorListeners) {
            listener.changeNumThreads();
        }

    }

    /**
     * Returns the name of the current UpdateController.
     *
     * @return the name of the current update method.
     */
    public String getCurrentUpdatorName() {
        return updateController.getName();
    }

    /**
     * Iterate the updator for a specified number of iterations.
     *
     * @param numIterations number of times to iterate updator.
     */
    public void iterate(final int numIterations) {
        updates.submit(new Runnable() {
            public void run() {
                notifyWorkspaceUpdateStarted();
                for (int i = 0; i < numIterations; i++) {
                    snychManager.queueTasks();

                    try {
                        doUpdate();
                    } catch (Exception e) {
                        // TODO exception handler
                        e.printStackTrace();
                    }
                    snychManager.releaseTasks();
                    snychManager.runTasks();
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

    /** Creates the threads used in the ExecutorService. */
    private class UpdatorThreadFactory implements ThreadFactory {
        /** Numbers the threads sequentially. */
        private int nextThread = 1;

        /**
         * Creates a new UpdateThread with the current thread number.
         *
         * @param runnable The runnable this thread will execute.
         */
        public Thread newThread(final Runnable runnable) {
            synchronized (this) {
                return new UpdateThread(WorkspaceUpdator.this, runnable,
                        nextThread++);
            }
        }
    }


    /**
     * @return the updateType
     */
    public TYPE getType() {
        // Set the type of this controller
        if (updateController instanceof BufferedUpdator) {
            return TYPE.BUFFERED;
        } else if (updateController instanceof PriorityUpdator) {
            return TYPE.PRIORITY;
        } else {
            return TYPE.CUSTOM;
        }
    }

    /**
     * @return the updateController
     */
    public UpdateController getUpdateController() {
        return updateController;
    }

    /**
     * @param updateController the updateController to set
     */
    public void setUpdateController(final UpdateController updateController) {
        this.updateController = updateController;
    }

}
