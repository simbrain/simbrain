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

import org.apache.log4j.Logger;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.serialization.WorkspaceSerializer;
import org.simbrain.workspace.updater.TaskSynchronizationManager;
import org.simbrain.workspace.updater.UpdateAction;
import org.simbrain.workspace.updater.WorkspaceUpdater;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A collection of components which interact via couplings. Neural networks,
 * simulated environments, data-tables, plots and gauges are examples of
 * components in a Simbrain workspace. Essentially, an instance of a workspace
 * corresponds to a single simulation, that can be run with or without a
 * graphical view of it. The main visualization of a workspace is {@link
 * org.simbrain.workspace.gui.SimbrainDesktop}.
 * <p>
 * To create a new type of workspace component, extend {@link
 * WorkspaceComponent}, and {@link org.simbrain.workspace.gui.GuiComponent}. The
 * latter is a gui representation of the former. Follow the pattern in {@link
 * AbstractComponentFactory} to register this mapping.  The workspace component
 * holds all the model objects, and manages couplings. Usually there is some
 * central model object, like {@link org.simbrain.world.odorworld.OdorWorld} or
 * {@link org.simbrain.network.core.Network} that the workspace component
 * creates and wraps.  The gui component is a {@link javax.swing.JPanel}  and
 * can either manage the graphics or (more typically) hold custom panels etc.
 * that do. De-serialization has a lot of steps, but the main things to be aware
 * of are to handle custom model deserializing in a readresolve method in the
 * main model object (e.g. Network or OdorWorld) and that if any special
 * graphical syncing is needed that it can be done the guicomponent constructor
 * by overriding {@link GuiComponent#postAddInit()}. Other init can happen in
 * overrides of {@link WorkspaceComponent#save(OutputStream, String)} and in a
 * static open method that must also be created. An example is {@link
 * org.simbrain.world.odorworld.OdorWorldComponent#open(InputStream, String,
 * String)}
 *
 * @author Jeff Yoshimi
 * @author Matt Watson
 * @author Tim Shea
 */
public class Workspace {

    /**
     * The static logger for this class.
     */
    private static transient final Logger LOGGER = Logger.getLogger(Workspace.class);

    /**
     * List of workspace components.
     */
    private transient List<WorkspaceComponent> componentList = Collections.synchronizedList(new ArrayList<WorkspaceComponent>());

    /**
     * Component factory should be used to create new workspace and gui
     * components.
     */
    private transient AbstractComponentFactory componentFactory = new AbstractComponentFactory(this);

    /**
     * Flag to indicate workspace has been changed since last save.
     */
    private transient boolean workspaceChanged = false;

    /**
     * Current workspace file.
     */
    private transient File currentFile = null;

    /**
     * A persistence representation of the time (the updater's state is not
     * persisted).
     */
    private int savedTime;

    /**
     * Listeners on this workspace. The CopyOnWriteArrayList is not a problem
     * because writes to this list are uncommon.
     */
    private transient CopyOnWriteArrayList<WorkspaceListener> listeners = new CopyOnWriteArrayList<WorkspaceListener>();

    /**
     * Mapping from workspace component types to integers which show how many
     * have been added. For naming new workspace components.
     */
    private transient Hashtable<Class<?>, Integer> componentNameIndices = new Hashtable<Class<?>, Integer>();

    /**
     * The updater used to manage component updates.
     */
    private transient Object updaterLock = new Object();

    /**
     * Delay in milliseconds between update cycles. Used to artificially slow
     * down simulation (sometimes useful in teaching).
     */
    private int updateDelay = 0;

    /**
     * The updater used to manage component updates.
     */
    private transient WorkspaceUpdater updater;

    /**
     * The CouplingFactory for this workspace.
     */
    private transient CouplingManager couplingManager = new CouplingManager(this);

    /**
     * Construct a workspace.
     */
    public Workspace() {
        updater = new WorkspaceUpdater(this);
    }

    /**
     * Adds a listener to the workspace.
     *
     * @param listener the Listener to add.
     */
    public void addListener(WorkspaceListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the listener from the workspace.
     *
     * @param listener The listener to remove.
     */
    public void removeListener(WorkspaceListener listener) {
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
        component.getAttributeTypeVisibilityMap().clear();
        for (WorkspaceListener listener : listeners) {
            listener.componentRemoved(component);
        }
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
            component.setName(component.getSimpleName() + componentNameIndices.get(component.getClass()));
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
        for (WorkspaceComponent wc : getComponentList()) {
            wc.start();
        }
        synchronized (updaterLock) {
            updater.runOnce();
        }
        stop();
    }

    /**
     * Iterated for a specified number of iterations using a latch. Used in
     * scripts when making a series of events occur, e.g. set some neurons, run
     * for 50 iterations, set some other neurons, run 20 iterations, etc.
     *
     * @param numIterations the number of iteration to run while waiting on the
     *                      latch.
     */
    public void iterate(int numIterations) {
        for (WorkspaceComponent wc : getComponentList()) {
            wc.start();
        }
        synchronized (updaterLock) {
            updater.iterate(numIterations);
        }
        stop();
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
        getCouplings().clear();
        fireWorkspaceCleared();
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
     * Check whether there have been changes in the workspace or its
     * components.
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

    public AbstractComponentFactory getComponentFactory() {
        return componentFactory;
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
        return SimbrainPreferences.getString("workspaceSimulationDirectory");
    }

    /**
     * @param currentDirectory the currentDirectory to set
     */
    public void setCurrentDirectory(final String currentDirectory) {
        SimbrainPreferences.putString("workspaceSimulationDirectory", currentDirectory);
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

    /**
     * Set the task synchronization manager.
     *
     * @param manager
     */
    public void setTaskSynchronizationManager(final TaskSynchronizationManager manager) {
        updater.setTaskSynchronizationManager(manager);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Number of components: " + componentList.size() + "\n");
        int i = 0;
        synchronized (componentList) {
            for (WorkspaceComponent component : componentList) {
                builder.append("Component " + ++i + ":" + component.getName() + "\n");
            }
        }
        return builder.toString();
    }

    /**
     * Returns all components of the specified type, e.g. all
     * WorkspaceComponents of type NetworkComponent.class.
     *
     * @param componentType the type of the component, in the sense of its
     *                      class
     * @return list of components
     */
    public Collection<? extends WorkspaceComponent> getComponentList(Class<?> componentType) {
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
    public int getSavedTime() {
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
    public void preSerializationInit() {
        /*
         * TODO: A bit of a hack. Currently just moves trainer components to the
         * back of the list, so they are serialized last, and hence deserialized
         * last.
         */
        Collections.sort(componentList, new Comparator<WorkspaceComponent>() {
            public int compare(WorkspaceComponent c1, WorkspaceComponent c2) {
                return Integer.compare(c1.getSerializePriority(), c2.getSerializePriority());
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

    /* Get the CouplingFactory for this workspace. */
    public CouplingManager getCouplingManager() {
        return couplingManager;
    }


    /**
     * Convenience method which gets the couplings the coupling manager stores.
     */
    public List<Coupling<?>> getCouplings() {
        return couplingManager.getCouplings();
    }
}
