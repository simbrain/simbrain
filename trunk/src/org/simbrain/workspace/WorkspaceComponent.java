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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Represents a component model in a Simbrain workspace.  Services relating to
 * couplings and relations between are handled.  Implementations of this class
 * should not be bound to a user interface.
 * 
 * @param <E> The type of the workspace listener associated with this
 * component.
 */
public abstract class WorkspaceComponent<E extends WorkspaceComponentListener> {
    /** The static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WorkspaceComponent.class);
    
    /** Log4j logger. */
    private Logger logger = Logger.getLogger(WorkspaceComponent.class);

    /** Whether this component has changed since last save. */
    private boolean changedSinceLastSave = false;

    /** The name of this component.  Used in the title, in saving, etc. */
    private String name  = "";

    /** The workspace that 'owns' this component. */
    private Workspace workspace;
    
    /**
     * Construct a workspace component.
     * 
     * @param name The name of the component.
     */
    public WorkspaceComponent(final String name) {
        this.name = name;
        logger.trace(this.getClass().getCanonicalName() + " created");
    }

    /**
     * Used when saving a workspace.  All changed workspace components are saved using
     * this method.
     *
     * @param output the stream of data to write the data to.
     */
    public abstract void save(OutputStream output, String format);

    /**
     * When workspaces are opened, a path to a file is passed in.
     * So, all components which can be saved should have this.
     *
     * @param input stream of data representing saved component.
     * 
     * @return The new instance.
     */
//    public static WorkspaceComponent<?> open(InputStream input, String name, String format) {
//        
//    }

    /**
     * Returns a list of the formats that this component supports.
     * 
     * <p>The default behavior is to return an empty list.  This means
     * that there is one format.
     * 
     * @return a list of the formats that this component supports.
     */
    public List<? extends String> getFormats() {
        return Collections.emptyList();
    }

    /**
     * Perform cleanup after closing.
    */
    public abstract void close();

    /**
     * called by Workspace to update the state of the component.
     */
    protected abstract void update();
    
    /**
     * called by Workspace to notify that updates have stopped.
     */
    protected void stopped() {
        /* no default implementation */
    }
    
    /**
     * Update that goes beyond updating couplings.
     * Called when global workspace update is called.
     */
    final void doUpdate() {
        update();
        
        for (E listener : listeners) {
            listener.componentUpdated();
        }
    }
    
    /**
     * Called after a global update ends.
     */
    final void doStopped() {
        stopped();
    }
    
    /** The set of all listeners on this component. */
    private Collection<E> listeners = new HashSet<E>();
    
    /**
     * Returns the listeners on this component.
     * 
     * @return The listeners on this component.
     */
    protected Collection<? extends E> getListeners() {
        return Collections.unmodifiableCollection(listeners);
    }
    
    /**
     * Adds a listener to this component.
     * 
     * @param listener the Listener to add.
     */
    public void addListener(final E listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a listener to this component.
     * 
     * @param listener the Listener to add.
     */
    public void removeListener(final E listener) {
        listeners.add(listener);
    }

    /**
     * Returns the name of this component.
     * 
     * @return The name of this component.
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @param changedSinceLastSave the changedSinceLastSave to set
     */
    public void setChangedSinceLastSave(final boolean changedSinceLastSave) {
        LOGGER.debug("component changed");
        this.changedSinceLastSave = changedSinceLastSave;
    }

    /**
     * @return the changedSinceLastSave
     */
    public boolean isChangedSinceLastSave() {
        return changedSinceLastSave;
    }

    /**
     * Retrieves a simple version of a component name from its class,
     * e.g. "Network" from "org.simbrain.network.NetworkComponent"/
     *
     * @return the simple name.
     */
    public String getSimpleName() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Component")) {
            simpleName = simpleName.replaceFirst("Component", "");
        }
        return simpleName;
    }

    /**
     * Returns the consumers associated with this component.
     * 
     * @return The consumers associated with this component.
     */
    public Collection<? extends Consumer> getConsumers() {
        return Collections.emptySet();
    }

    /**
     * Returns the producers associated with this component.
     * 
     * @return The producers associated with this component.
     */
    public Collection<? extends Producer> getProducers() {
        return Collections.emptySet();
    }

    /**
     * Sets the workspace for this component.  Called by the
     * workspace right after this component is created.
     * 
     * @param workspace The workspace for this component.
     */
    void setWorkspace(final Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * Returns the workspace associated with this component.
     * 
     * @return The workspace associated with this component.
     */
    public Workspace getWorkspace() {
        return workspace;
    }
    
    /**
     * Called when a coupling that this workspace owns the target
     * or source of is removed from the workspace.  This method
     * will only be called once if the workspace owns both the
     * source and the target.
     * 
     * @param coupling The coupling that has been removed.
     */
    protected void couplingRemoved(final Coupling<?> coupling) {
        /* no default implementation */
    }
}
