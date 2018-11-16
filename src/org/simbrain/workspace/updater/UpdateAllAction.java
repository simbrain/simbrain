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
import org.simbrain.workspace.WorkspaceComponent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * This is the default action for all workspace updates.
 * First update couplings then update all the components.
 *
 * @author jyoshimi
 */
public class UpdateAllAction implements UpdateAction {

    /**
     * Provides access to workspace updater.
     */
    private transient WorkspaceUpdater updater;

    /**
     * The static logger for the class.
     */
    static Logger LOGGER = Logger.getLogger(UpdateAllAction.class);

    /**
     * The executor service for doing the component updates.
     */
    private transient ExecutorService componentUpdateExecutor;

    /**
     * Construct the action.
     *
     * @param updater reference to parent updater
     */
    public UpdateAllAction(WorkspaceUpdater updater) {
        this.updater = updater;

        // In some cases components can be updated in parallel. So
        // a thread pool with a configurable number of threads is used
        componentUpdateExecutor = Executors.newFixedThreadPool(updater.getNumThreads(), new UpdaterThreadFactory());

        // TODO: the executor needs to be reinitialized when
        // updater.setNumThreads is called
        //this.componentUpdates = Executors.newFixedThreadPool(numThreads,
        //        new UpdaterThreadFactory());
    }

    @Override
    public void invoke() {
        List<? extends WorkspaceComponent> components = updater.getComponents();

        int componentCount = components.size();

        if (componentCount < 1) {
            return;
        }

        LOGGER.trace("updating couplings");
        updateCouplings();

        LOGGER.trace("creating latch");
        LatchCompletionSignal latch = new LatchCompletionSignal(componentCount);

        LOGGER.trace("updating components");
        for (WorkspaceComponent component : components) {
            updateComponent(component, latch);
        }
        LOGGER.trace("waiting");
        latch.await();
        LOGGER.trace("update complete");
    }

    /**
     * Update the provided workspace component.
     *
     * @param component the component to update.
     * @param signal    completion signal
     */
    public void updateComponent(final WorkspaceComponent component, final CompletionSignal signal) {

        // If update is turned off on this component, return
        if (!component.getUpdateOn()) {
            signal.done();
            return;
        }

        componentUpdateExecutor.submit(() -> {
            UpdateThread thread = (UpdateThread) Thread.currentThread();
            thread.setCurrentTask(component);
            component.update();
            thread.clearCurrentTask(component);
            signal.done();
        });

    }

    /**
     * Update couplings.
     */
    public void updateCouplings() {
        updater.getWorkspace().getCouplingManager().updateCouplings();
        LOGGER.trace("couplings updated");
        updater.notifyCouplingsUpdated();
    }

    @Override
    public String getDescription() {
        return "Update All Components and Couplings";
    }

    @Override
    public String getLongDescription() {
        return getDescription();
    }

    /**
     * Creates the threads used in the ExecutorService. Used to create a custom
     * thread class that will be generated inside the executor. This allows for
     * a clean way to capture the events using the thread instances themselves
     * which 'know' their thread number.
     */
    private class UpdaterThreadFactory implements ThreadFactory {

        /**
         * Numbers the threads sequentially.
         */
        private int nextThread = 1;

        /**
         * Creates a new UpdateThread with the current thread number.
         *
         * @param runnable The runnable this thread will execute.
         * @return current thread number
         */
        public Thread newThread(final Runnable runnable) {
            synchronized (this) {
                return new UpdateThread(updater, runnable, nextThread++);
            }
        }
    }
}