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

import org.apache.log4j.Logger;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * This class manages workspace updates. "Running" and "Stepping" the simulation
 * happen here, in a way that allows for concurrent update (in some cases) and
 * also interacts properly with single threaded guis using a "task
 * synchronization manager". Notification events about workspace events are
 * fired from here. Every time the workspace is updated, a list of actions in
 * the UpdateActionManager is invoked. By default one single action, a "buffered
 * update", occurs, in which components are updated in parallel, and when they
 * have all finished updating, couplings are updated. The update action manager
 * can also be used to customize update. Three executor services are here, one
 * for workspace updates (a single thread), one for event notification updates
 * (a single thread), and one for component updates (a thread pool with multiple
 * threads that can be configured), for cases when component updating happens
 * concurrently.
 *
 * @author Matt Watson
 * @author Jeff Yoshimi
 */
public class WorkspaceUpdater {

    /**
     * The static logger for the class.
     */
    static final Logger LOGGER = Logger.getLogger(WorkspaceUpdater.class);

    /**
     * The parent workspace.
     */
    private final Workspace workspace;

    /**
     * The executor service for managing workspace updates.
     */
    private final ExecutorService workspaceUpdateExecutor;

    /**
     * The executor service for notifying listeners.
     */
    private final ExecutorService notificationEvents;

    /**
     * Component listeners.
     */
    private final List<UpdateEventListener> componentListeners = new CopyOnWriteArrayList<UpdateEventListener>();

    /**
     * Updater listeners.
     */
    private final List<WorkspaceUpdaterListener> updaterListeners = new CopyOnWriteArrayList<WorkspaceUpdaterListener>();

    /**
     * Creates a default synch-manager that does nothing.
     */
    private volatile TaskSynchronizationManager syncManager = NO_ACTION_SYNC_MANAGER;

    /**
     * Executes queued InvocationEvents (i.e. AWT-driven events).
     */
    private SynchronizedTaskUpdateAction syncUpdateAction = new SynchronizedTaskUpdateAction(syncManager);

    /**
     * Whether updates should continue to run.
     */
    private volatile boolean run = false;

    /**
     * The number of times the update has run.
     */
    private volatile int time;

    /**
     * Number of threads used in the update service.
     */
    private int numThreads;

    /**
     * The update Manager.
     */
    private UpdateActionManager updateActionManager;

    /**
     * Constructor for the updater that uses the provided controller and
     * threads.
     *
     * @param workspace The parent workspace.
     * @param threads   The number of threads for component updates.
     */
    public WorkspaceUpdater(Workspace workspace, int threads) {
        this.workspace = workspace;
        this.numThreads = threads;
        // A single thread updates the workspace
        workspaceUpdateExecutor = Executors.newSingleThreadExecutor();
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
    public WorkspaceUpdater(Workspace workspace) {
        this(workspace, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Sets the manager. Setting the manager to null clears the manager.
     *
     * @param manager the new manager.
     */
    public void setTaskSynchronizationManager(TaskSynchronizationManager manager) {
        if (manager == null) {
            syncManager = NO_ACTION_SYNC_MANAGER;
        } else {
            syncManager = manager;
        }
        syncUpdateAction.setSyncManager(syncManager);
    }

    /**
     * Returns the update action used to execute synchronized invocation events. This should be added to the update
     * action manager if the update sequence is cleared or deserialized.
     */
    public UpdateAction getSyncUpdateAction() {
        return syncUpdateAction;
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
     * Sets the time.
     *
     * @param time time to set
     */
    public void setTime(final int time) {
        this.time = time;
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
     */
    public boolean isRunning() {
        return run;
    }

    /**
     * Starts the update thread. Used when "running" the workspace by pressing
     * the play button in the gui.
     */
    public void run() {
        run = true;
        for (WorkspaceComponent wc : workspace.getComponentList()) {
            wc.setRunning(true);
        }
        workspaceUpdateExecutor.submit(() -> {
            notifyWorkspaceUpdateStarted();
            syncManager.queueTasks();
            try {
                while (run) {
                    doUpdate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            run = false;
            for (WorkspaceComponent component : workspace.getComponentList()) {
                component.setRunning(false);
            }
            syncManager.releaseTasks();
            syncManager.runTasks();
            notifyWorkspaceUpdateCompleted();
        });
    }

    /**
     * Submits a single task to the queue.
     */
    public void runOnce() {
        workspaceUpdateExecutor.submit(() -> {
            notifyWorkspaceUpdateStarted();
            syncManager.queueTasks();
            try {
                doUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
            syncManager.releaseTasks();
            syncManager.runTasks();
            notifyWorkspaceUpdateCompleted();
        });
    }

    /**
     * Iterate a set number of iterations.
     * <p>
     * See {@link Workspace#iterate(int)}
     *
     * @param numIterations the number of iterations to update
     */
    public void iterate(int numIterations) {
        Future<?> wait = workspaceUpdateExecutor.submit(() -> {
            notifyWorkspaceUpdateStarted();
            try {
                syncManager.queueTasks();
                for (int i = 0; i < numIterations; i++) {
                    doUpdate();
                }
                syncManager.releaseTasks();
                syncManager.runTasks();
            } catch (Exception e) {
                e.printStackTrace();
            }
            notifyWorkspaceUpdateCompleted();
        });
        try {
            wait.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes the main workspace update.
     */
    private void doUpdate() {
        time++;
        LOGGER.trace("starting: " + time);
        for (UpdateAction action : updateActionManager.getActionList()) {
            notifyBeforeUpdateAction(action);
            action.invoke();
            notifyAfterUpdateAction(action);
        }
        notifyWorkspaceUpdated();
        LOGGER.trace("done: " + time);
    }

    /**
     * Adds a component listener to this instance.
     *
     * @param listener The component listener to add.
     */
    public void addComponentListener(UpdateEventListener listener) {
        componentListeners.add(listener);
    }

    /**
     * Removes a component listener from this instance.
     *
     * @param listener The listener to add.
     */
    public void removeComponentListener(UpdateEventListener listener) {
        componentListeners.remove(listener);
    }

    /**
     * Adds an updater listener to this instance.
     *
     * @param listener updater component listener to add.
     */
    public void addUpdaterListener(WorkspaceUpdaterListener listener) {
        updaterListeners.add(listener);
    }

    /**
     * Removes an updater listener from this instance.
     *
     * @param listener The updater listener to add.
     */
    public void removeUpdaterListener(WorkspaceUpdaterListener listener) {
        updaterListeners.remove(listener);
    }

    /**
     * Called when an update action is about to be invoked.
     *
     * @param action The action to be invoked.
     */
    void notifyBeforeUpdateAction(UpdateAction action) {
        final long nanoTime = System.nanoTime();
        notificationEvents.submit(() -> {
            componentListeners.forEach(l -> l.beforeUpdateAction(action, nanoTime));
        });
    }

    /**
     * Called after an update action has been invoked.
     *
     * @param action The action that was invoked.
     */
    void notifyAfterUpdateAction(UpdateAction action) {
        final long nanoTime = System.nanoTime();
        notificationEvents.submit(() -> {
            componentListeners.forEach(l -> l.afterUpdateAction(action, nanoTime));
        });
    }

    /**
     * Called when a new component is starting to update.
     *
     * @param component The component to update.
     * @param thread    The number of the thread doing the update.
     */
    void notifyComponentUpdateStarted(WorkspaceComponent component, int thread) {
        final int simTime = this.time;
        final long nanoTime = System.nanoTime();
        notificationEvents.submit(() -> {
            componentListeners.forEach(l -> l.beforeComponentUpdate(component, simTime, thread, nanoTime));
        });
    }

    /**
     * Called when a new component is finished updating.
     *
     * @param component The component to update.
     * @param thread    The number of the thread doing the update.
     */
    void notifyComponentUpdateFinished(WorkspaceComponent component, int thread) {
        final int simTime = this.time;
        final long nanoTime = System.nanoTime();
        notificationEvents.submit(() -> {
            componentListeners.forEach(l -> l.afterComponentUpdate(component, simTime, thread, nanoTime));
        });
    }

    /**
     * Called when the couplings are updated.
     */
    protected void notifyCouplingsUpdated() {
        final int time = this.time;
        Consumer<WorkspaceUpdaterListener> notifier = l -> l.updatedCouplings(time);
        notificationEvents.submit(() -> updaterListeners.forEach(notifier));
    }

    /**
     * Called when the workspace update begins.
     */
    private void notifyWorkspaceUpdateStarted() {
        notificationEvents.submit(() -> updaterListeners.forEach(WorkspaceUpdaterListener::updatingStarted));
    }

    /**
     * Called when workspace update finishes.
     */
    private void notifyWorkspaceUpdateCompleted() {
        notificationEvents.submit(() -> updaterListeners.forEach(WorkspaceUpdaterListener::updatingFinished));
    }

    /**
     * Called after every workspace update .
     */
    private void notifyWorkspaceUpdated() {
        notificationEvents.submit(() -> updaterListeners.forEach(WorkspaceUpdaterListener::workspaceUpdated));
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
        // this.componentUpdates = Executors.newFixedThreadPool(numThreads,
        // new UpdaterThreadFactory());
        for (WorkspaceUpdaterListener listener : updaterListeners) {
            listener.changeNumThreads();
        }

    }

    /**
     * A synch-manager where the methods do nothing.
     */
    private static final TaskSynchronizationManager NO_ACTION_SYNC_MANAGER = new TaskSynchronizationManager() {
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
        List<? extends WorkspaceComponent> components = workspace.getComponentList();
        synchronized (components) {
            components = new ArrayList<WorkspaceComponent>(components);
        }
        return components;
    }

    /**
     * @return the workspace
     */
    public Workspace getWorkspace() {
        return workspace;
    }

}