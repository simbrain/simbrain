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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * This is the default action for all workspace updates.
 *
 * First update couplings using a buffering system whereby the order in which
 * they are updated does not matter (read all producer values, write them to a
 * buffer, then read all buffer values and write them to the consumers). Then
 * update all the components.
 *
 * In workspace updater, the following happens. Each component update call is
 * fed to an executor service which uses as many threads as it's configured to
 * use (it defaults to the number of available processors which can be changed.)
 * Then the executing thread waits on a countdown latch. Each component update
 * decrements the latch so that after the last update is complete, the thread
 * waiting on the latch wakes up and updates all the couplings.
 *
 * @author jyoshimi
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class UpdateAllBuffered implements UpdateAction {

    /** Provides access to workspace updater. */
    @XmlTransient
    private WorkspaceUpdater updater;

    /** The static logger for the class. */
    static Logger LOGGER = Logger.getLogger(UpdateAllBuffered.class);

    /** The executor service for doing the component updates. */
    @XmlTransient
    private ExecutorService componentUpdateExecutor;

    //TODO
    /**
     * No-argument constructor for jaxb.
     */
    public UpdateAllBuffered() {
    }

    /**
     * Construct the action.
     *
     * @param updater reference to parent updater
     */
    public UpdateAllBuffered(WorkspaceUpdater updater) {
        this.updater = updater;

        // In some cases components can be updated in parallel. So
        // a thread pool with a configurable number of threads is used
        componentUpdateExecutor = Executors.newFixedThreadPool(updater.getNumThreads(),
                new UpdaterThreadFactory());

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
     * @param signal completion signal
     */
    public void updateComponent(final WorkspaceComponent component,
            final CompletionSignal signal) {

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
        updater.getWorkspace().getCouplingManager().updateAllCouplings();
        LOGGER.trace("couplings updated");
        updater.notifyCouplingsUpdated();
    }

    @Override
    public String getDescription() {
        return "Buffered update of all components and couplings.";
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

        /** Numbers the threads sequentially. */
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