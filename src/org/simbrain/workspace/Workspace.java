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
package org.simbrain.workspace;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.SimbrainPreferences.PropertyNotFoundException;
import org.simbrain.workspace.updater.TaskSynchronizationManager;
import org.simbrain.workspace.updater.UpdateAction;
import org.simbrain.workspace.updater.WorkspaceUpdater;

/**
 * A collection of components which interact via couplings. Neural networks,
 * data-tables, gauges, and scripts are examples of components in a Simbrain
 * workspace. Essentially, an instance of a workspace corresponds to a single
 * simulation (though at some point it will be possible to link multiple
 * workspaces on different machines together). A workspace can be visualized via
 * a {@link org.simbrain.workspace.gui.SimbrainDesktop}.
 *
 * @see org.simbrain.workspace.Coupling
 *
 *      TODO: Check everything that has been commented out
 */
public class Workspace {

    
    //TODO
    
    
    /** The static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Workspace.class);

    /** The coupling manager for this workspace. */
    // private final CouplingManager manager;

    /** List of workspace components. */
    private List<WorkspaceComponent> componentList = Collections
            .synchronizedList(new ArrayList<WorkspaceComponent>());

    /** Flag to indicate workspace has been changed since last save. */
    private boolean workspaceChanged = false;

    /** Current workspace file. */
    private File currentFile = null;

    /**
     * A persistence representation of the time (the updater's state is not
     * persisted).
     */
    private int savedTime;

    /**
     * Listeners on this workspace. The CopyOnWriteArrayList is not a problem
     * because writes to this list are uncommon.
     */
    private CopyOnWriteArrayList<WorkspaceListener> listeners = new CopyOnWriteArrayList<WorkspaceListener>();

    /**
     * Mapping from workspace component types to integers which show how many
     * have been added. For naming new workspace components.
     */
    private Hashtable<Class<?>, Integer> componentNameIndices = new Hashtable<Class<?>, Integer>();

    /**
     * The updater used to manage component updates.
     */
    private Object updaterLock = new Object();

    /**
     * Delay in milliseconds between update cycles. Used to artificially slow
     * down simulation (sometimes useful in teaching).
     */
    private int updateDelay = 0;

    /**
     * The updater used to manage component updates.
     */
    private final WorkspaceUpdater updater;

    /**
     * Construct a workspace.
     */
    public Workspace() {
        // manager = new CouplingManager(this);
        updater = new WorkspaceUpdater(this);
    }

    /**
     * Adds a listener to the workspace.
     *
     * @param listener the Listener to add.
     */
    public void addListener(final WorkspaceListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the listener from the workspace.
     *
     * @param listener The listener to remove.
     */
    public void removeListener(final WorkspaceListener listener) {
        listeners.remove(listener);
    }

    /**
     * Fire a new workspace opened event.
     */
    private void fireNewWorkspaceOpened() {
        for (WorkspaceListener listener : listeners) {
            listener.newWorkspaceOpened();
        }
    }

    /**
     * Fire a workspace cleared event
     */
    private void fireWorkspaceCleared() {
        for (WorkspaceListener listener : listeners) {
            listener.workspaceCleared();
        }
    }

    /**
     * Fire a component added event.
     *
     * @param component the component added
     */
    private void fireWorkspaceComponentAdded(WorkspaceComponent component) {
        for (WorkspaceListener listener : listeners) {
            listener.componentAdded(component);
        }
    }

    /**
     * Fire a component removed event.
     *
     * @param component the component added
     */
    private void fireWorkspaceComponentRemoved(WorkspaceComponent component) {
        for (WorkspaceListener listener : listeners) {
            listener.componentRemoved(component);
        }
    }

    // TODO: Move these methods to other coupling stuff
    /**
     * Couple each source attribute to all target attributes.
     *
     * @param sourceAttributes source producing attributes
     * @param targetAttributes target consuming attributes
     */
    // public void coupleOneToMany(final List<PotentialProducer>
    // sourceAttributes,
    // final List<PotentialConsumer> targetAttributes) {
    // for (PotentialProducer producingAttribute : sourceAttributes) {
    // for (PotentialConsumer consumingAttribute : targetAttributes) {
    // Coupling<?> coupling = new Coupling(
    // producingAttribute.createProducer(),
    // consumingAttribute.createConsumer());
    // try {
    // getCouplingManager().addCoupling(coupling);
    // } catch (MismatchedAttributesException e) {
    // e.printStackTrace();
    // }
    // }
    // }
    // }

    /**
     * Couple each producer to each consumer.
     *
     * @param producers source producers
     * @param consumers target consumers
     * @exception MismatchedAttributesException
     */
    @SuppressWarnings("unchecked")
    public void coupleOneToOne(final List<Producer2<?>> producers,
            final List<Consumer2<?>> consumers)
            throws MismatchedAttributesException {

        Iterator<Consumer2<?>> consumerIterator = consumers.iterator();
        for (Producer2<?> producer : producers) {
            if (consumerIterator.hasNext()) {
                Coupling2 coupling = new Coupling2(producer,
                        consumerIterator.next());
                couplings.add(coupling);
                continue;
            }
        }

        this.fireCouplingsAdded();

        // Iterator<PotentialConsumer> consumerIterator =
        // consumerKeys.iterator();
        //
        // for (PotentialProducer producerID : producerKeys) {
        // if (consumerIterator.hasNext()) {
        // Producer<?> producer = producerID.createProducer();
        // Consumer<?> consumer = consumerIterator.next().createConsumer();
        // Coupling<?> coupling = new Coupling(producer, consumer);
        // try {
        // getCouplingManager().addCoupling(coupling);
        // } catch (UmatchedAttributesException e) {
        // throw e;
        // }

    }

    /**
     * Adds a workspace component to the workspace.
     *
     * @param component The component to add.
     */
    public void addWorkspaceComponent(final WorkspaceComponent component) {
        LOGGER.debug("adding component: " + component);
        componentList.add(component);
        component.setWorkspace(this);
        component.setChangedSinceLastSave(false);
        this.setWorkspaceChanged(true);

        /*
         * Handle component naming.
         *
         * If the component has not yet been named, name as follows: (ClassName
         * - "Component") + index where index iterates as new components are
         * added. e.g. Network 1, Network 2, etc.
         */
        if (component.getName().equalsIgnoreCase("")) {
            if (componentNameIndices.get(component.getClass()) == null) {
                componentNameIndices.put(component.getClass(), 1);
            } else {
                int index = componentNameIndices.get(component.getClass());
                componentNameIndices.put(component.getClass(), index + 1);
            }
            component.setName(component.getSimpleName()
                    + componentNameIndices.get(component.getClass()));
        }

        fireWorkspaceComponentAdded(component);


    }

    /**
     * Remove the specified component.
     *
     * @param component The component to remove.
     */
    public void removeWorkspaceComponent(final WorkspaceComponent component) {
        LOGGER.debug("removing component: " + component);

        // Remove all couplings associated with this component
        // this.getCouplingManager().removeCouplings(component);
        componentList.remove(component);
        this.setWorkspaceChanged(true);
        fireWorkspaceComponentRemoved(component);
    }

    /**
     * Should be called when updating is stopped.
     */
    void updateStopped() {
        synchronized (componentList) {
            for (WorkspaceComponent component : componentList) {
                component.doStopped();
            }
        }
    }

    /**
     * Iterates all couplings on all components until halted by user.
     */
    public void run() {        
        for (WorkspaceComponent wc : getComponentList()) {
            wc.start();
        }
        synchronized (updaterLock) {
            updater.run();
        }
    }

    /**
     * Stops iteration of all couplings on all components.
     */
    public void stop() {
        for (WorkspaceComponent wc : getComponentList()) {
            wc.stop();
        }
        synchronized (updaterLock) {
            updater.stop();
        }
        updateStopped();
    }

    /**
     * Update the workspace a single time.
     */
    public void iterate() {
        synchronized (updaterLock) {
            updater.runOnce();
        }
        updateStopped();
    }

    /**
     * Iterated for a specified number of iterations using a latch. Used in
     * scripts when making a series of events occur, e.g. set some neurons, run
     * for 50 iterations, set some other neurons, run 20 iterations, etc.
     *
     * The latch must be initialized with a count of 1 and must be run from a
     * separate thread. For example:
     *
     * <pre>
     * {@code}
     *  Executors.newSingleThreadExecutor().execute(new Runnable() {
     *               public void run() {
     *                   // Do one thing
     *                    CountDownLatch latch = new CountDownLatch(1);
     *                    workspace.iterate(latch, 100);
     *                    try {
     *                        latch.await();
     *                    } catch (InterruptedException e) {
     *                        e.printStackTrace();
     *                    }
     *                    // Do another
     *                    CountDownLatch latch = new CountDownLatch(1);
     *                    workspace.iterate(latch, 100);
     *                    try {
     *                        latch.await();
     *                    } catch (InterruptedException e) {
     *                        e.printStackTrace();
     *                    }
     *               }
     *            });
     * }
     * </pre>
     *
     * @param latch the latch to wait on
     * @param numIterations the number of iteration to run while waiting on the
     *            latch
     */
    public void iterate(CountDownLatch latch, final int numIterations) {
        synchronized (updaterLock) {
            updater.iterate(latch, numIterations);
        }
        updateStopped();
    }

    /**
     * Remove all components (networks, worlds, etc.) from this workspace.
     */
    public void clearWorkspace() {
        stop();
        removeAllComponents();
        resetTime();
        this.setWorkspaceChanged(false);
        currentFile = null;
        fireWorkspaceCleared();
        // manager.clearCouplings();
        this.getUpdater().getUpdateManager().setDefaultUpdateActions();
    }

    /**
     * Disposes all Simbrain Windows.
     */
    public void removeAllComponents() {
        List<WorkspaceComponent> toRemove = new ArrayList<WorkspaceComponent>();
        synchronized (componentList) {
            for (WorkspaceComponent component : componentList) {
                toRemove.add(component);
            }
            for (WorkspaceComponent component : toRemove) {
                removeWorkspaceComponent(component);
            }
        }
    }

    /**
     * Check whether there have been changes in the workspace or its components.
     *
     * @return true if changes exist, false otherwise
     */
    public boolean changesExist() {
        if (workspaceChanged) {
            return true;
        } else {
            boolean hasChanged = false;
            synchronized (componentList) {
                for (WorkspaceComponent component : componentList) {
                    // System.out.println(component.getName() + ":" +
                    // component.hasChangedSinceLastSave());
                    if (component.hasChangedSinceLastSave()) {
                        hasChanged = true;
                    }
                }
            }
            return hasChanged;
        }
    }

    /**
     * Sets whether the workspace has been changed.
     *
     * @param workspaceChanged Has workspace been changed value
     */
    public void setWorkspaceChanged(final boolean workspaceChanged) {
        this.workspaceChanged = workspaceChanged;
    }

    /**
     * @return the currentDirectory
     */
    public String getCurrentDirectory() {
        try {
            return SimbrainPreferences
                    .getString("workspaceSimulationDirectory");
        } catch (PropertyNotFoundException e) {
            e.printStackTrace();
            return ".";
        }
    }

    /**
     * @param currentDirectory the currentDirectory to set
     */
    public void setCurrentDirectory(final String currentDirectory) {
        SimbrainPreferences.putString("workspaceSimulationDirectory",
                currentDirectory);
    }

    /**
     * @return Returns the currentFile.
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * @param currentFile The current_file to set.
     */
    public void setCurrentFile(final File currentFile) {
        this.currentFile = currentFile;
        // WorkspacePreferences.setDefaultFile(currentFile.getAbsolutePath());
    }

    /**
     * @return the componentList
     */
    public List<? extends WorkspaceComponent> getComponentList() {
        return Collections.unmodifiableList(componentList);
    }

    /**
     * Get a component using its name id. Used in terminal mode.
     *
     * @param id name of component
     * @return Workspace Component
     */
    public WorkspaceComponent getComponent(final String id) {
        synchronized (componentList) {
            for (WorkspaceComponent component : componentList) {
                if (component.getName().equalsIgnoreCase(id)) {
                    return component;
                }
            }
        }
        return null;
    }

    // /**
    // * Returns the coupling associated with a string id.
    // *
    // * @param id the string id
    // * @return the associated coupling
    // */
    // public Coupling<?> getCoupling(String id) {
    // for (Coupling<?> coupling : this.getCouplingManager().getCouplings()) {
    // if (coupling.getId().equalsIgnoreCase(id)) {
    // return coupling;
    // }
    // }
    // return null;
    // }

    /**
     * Set the task synchronization manager.
     *
     * @param manager
     */
    public void setTaskSynchronizationManager(
            final TaskSynchronizationManager manager) {
        updater.setTaskSynchronizationManager(manager);
    }

    // /**
    // * Returns the coupling manager for this workspace.
    // *
    // * @return The coupling manager for this workspace.
    // */
    // public CouplingManager getCouplingManager() {
    // return manager;
    // }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(
                "Number of components: " + componentList.size() + "\n");
        int i = 0;
        synchronized (componentList) {
            for (WorkspaceComponent component : componentList) {
                builder.append(
                        "Component " + ++i + ":" + component.getName() + "\n");
            }
        }
        return builder.toString();
    }

    // /**
    // * Adds a coupling to the CouplingManager.
    // *
    // * @param coupling The coupling to add.
    // */
    // public void addCoupling(final Coupling<?> coupling) {
    // try {
    // manager.addCoupling(coupling);
    // } catch (MismatchedAttributesException e) {
    // e.printStackTrace();
    // }
    // }
    //
    // /**
    // * Removes a coupling from the CouplingManager.
    // *
    // * @param coupling The coupling to remove.
    // */
    // public void removeCoupling(final Coupling<?> coupling) {
    // manager.removeCoupling(coupling);
    // }

    /**
     * Returns all components of the specified type, e.g. all
     * WorkspaceComponents of type NetworkComponent.class.
     *
     * @param componentType the type of the component, in the sense of its class
     * @return list of components
     */
    public Collection<? extends WorkspaceComponent> getComponentList(
            Class<?> componentType) {
        List<WorkspaceComponent> returnList = new ArrayList<WorkspaceComponent>();
        for (WorkspaceComponent component : componentList) {
            if (component.getClass() == componentType) {
                returnList.add(component);
            }
        }
        return returnList;
    }

    /**
     * Returns global time.
     *
     * @return the time
     */
    public int getTime() {
        if (updater == null) {
            return 0;
        } else {
            return updater.getTime();
        }
    }

    /**
     * @return the savedTime
     */
    protected int getSavedTime() {
        return savedTime;
    }

    /**
     * Reset time.
     */
    public void resetTime() {
        updater.resetTime();
    }

    /**
     * Returns a reference to the workspace updater.
     *
     * @return reference to workspace updater.
     */
    public WorkspaceUpdater getUpdater() {
        return updater;
    }

    /**
     * @return the updateDelay
     */
    public int getUpdateDelay() {
        return updateDelay;
    }

    /**
     * @param updateDelay the updateDelay to set
     */
    public void setUpdateDelay(int updateDelay) {
        this.updateDelay = updateDelay;
    }

    /**
     * Actions required prior to proper serialization.
     */
    void preSerializationInit() {
        /**
         * TODO: A bit of a hack. Currently just moves trainer components to the
         * back of the list, so they are serialized last, and hence deserialized
         * last.
         */
        Collections.sort(componentList, new Comparator<WorkspaceComponent>() {
            public int compare(WorkspaceComponent c1, WorkspaceComponent c2) {
                return Integer.valueOf(c1.getSerializePriority())
                        .compareTo(Integer.valueOf(c2.getSerializePriority()));
            }
        });
        savedTime = getTime();
    }

    /**
     * Open a workspace from a file.
     *
     * @param theFile the file to try to open
     */
    public void openWorkspace(final File theFile) {
        WorkspaceSerializer serializer = new WorkspaceSerializer(this);
        try {
            if (theFile != null) {
                clearWorkspace();
                serializer.deserialize(new FileInputStream(theFile));
                setCurrentFile(theFile);
                setWorkspaceChanged(false);
                fireNewWorkspaceOpened();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convenience method for adding an update action to the workspace's action
     * list (the sequence of actions invoked on each iteration of the
     * workspace).
     *
     * @param action new action
     */
    public void addUpdateAction(UpdateAction action) {
        updater.getUpdateManager().addAction(action);
    }

    //
    // TODO: New / temp coupling refactor stuff
    //

    /** All couplings for the workspace. */
    private final List<Coupling2<?>> couplings = new ArrayList<Coupling2<?>>();

    public void addCoupling(Coupling2<?> coupling) {
        couplings.add(coupling);
        fireCouplingAdded(coupling);
    }

    // TODO: Check couplingmanager versions of all this
    public void updateCouplings() {
        for (Coupling2<?> coupling : couplings) {
            coupling.update();
        }
    }

    /** List of listeners to fire updates when couplings are changed. */
    private List<CouplingListener> couplingListeners = new ArrayList<CouplingListener>();

    /**
     * Adds a new listener to be updated when changes are made.
     *
     * @param listener to be updated of changes
     */
    public void addCouplingListener(final CouplingListener listener) {
        couplingListeners.add(listener);
    }

    /**
     * Removes the listener from the list.
     *
     * @param listener to be removed
     */
    public void removeCouplingListener(final CouplingListener listener) {
        couplingListeners.remove(listener);
    }

    /**
     * Coupling added.
     *
     * @param coupling coupling that was added
     */
    private void fireCouplingAdded(Coupling2<?> coupling) {
        for (CouplingListener listeners : couplingListeners) {
            listeners.couplingAdded(coupling);
        }
    }

    private void fireCouplingsAdded() {
        for (CouplingListener listeners : couplingListeners) {
            listeners.couplingsAdded();
        }
    }

    private void fireCouplingsRemoved() {
        for (CouplingListener listeners : couplingListeners) {
            listeners.couplingsRemoved();
        }
    }

    /**
     * Coupling removed.
     *
     * @param coupling coupling that was removed
     */
    private void fireCouplingRemoved(Coupling2<?> coupling) {
        for (CouplingListener listeners : couplingListeners) {
            listeners.couplingRemoved(coupling);
        }
    }

    /**
     * @return the couplings
     */
    public List<Coupling2<?>> getCouplings() {
        return couplings;
    }

    public void removeCouplings(List<Coupling2<?>> couplings) {
        this.couplings.removeAll(couplings);
        this.fireCouplingsRemoved();
    }

    //TODO: If not used by the time update actions are re-implemented, remove
    public Coupling2<?> getCoupling(String id) {
        return couplings.stream().filter(c -> c.getId().equalsIgnoreCase(id))
                .findFirst().get();
    }

    /**
     * Convenience method for updating a set of couplings.
     *
     * @param couplingList the list of couplings to be updated
     */
    public void updateCouplings(List<Coupling2<?>> couplingList) {
        // for (Coupling2<?> coupling : couplingList) {
        // coupling.setBuffer();
        // }
        for (Coupling2<?> coupling : couplingList) {
            coupling.update();
        }
    }

}