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

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.simbrain.gauge.core.Projector;

/**
 * Represents a component model in a Simbrain workspace.  Services relating to
 * couplings and relations between are handled.  Implementations of this class
 * should not be bound to a user interface.
 * 
 * @param <E> The type of the workspace listener associated with this
 * component.
 */
public abstract class WorkspaceComponent<E extends WorkspaceComponentListener> {

    private static final Set<String> NAMES = new HashSet<String>();
    
    /** The workspace that 'owns' this component. */
    private Workspace workspace;
    
    /** The static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WorkspaceComponent.class);
    
    /** Log4j logger. */
    private Logger logger = Logger.getLogger(WorkspaceComponent.class);

    /** Whether this component has changed since last save. */
    private boolean changedSinceLastSave = true;

    /** The name of this component.  Used in the title, in saving, etc. */
    private String name  = "";
    
    /** How to order a list of attributes. */
    public enum Strategy {TOTAL, DEFAULT_EACH, CUSTOM };
    
    /** Current Strategy for this component. */
    private Strategy strategy;

    /**
     * Current directory. So when re-opening this type of component the appremembers where to look. 
     * Subclasses can provide a default value using User Preferences.
     */
    private String currentDirectory;

    /**
     * Current file.  Used when "saving" a component.
     * Subclasses can provide a default value using User Preferences.
     */
    private File currentFile;

    /**
     * Construct a workspace component.
     * 
     * @param name The name of the component.
     */
    public WorkspaceComponent(final String name) {
        this.name = name;
        logger.trace(this.getClass().getCanonicalName() + ": " + name + " created");
        strategy = Strategy.DEFAULT_EACH;
    }

    /**
     * Used when saving a workspace.  All changed workspace components are saved using
     * this method.
     *
     * @param output the stream of data to write the data to.
     * @param format a key used to define the requested format.
     */
    public abstract void save(OutputStream output, String format);
    
    /**
     * Subclasses should override this; used in coupling serialization.
     */
    public String getKeyForAttribute(Attribute attribute) {
        return null;
    }

    /**
     * Subclasses should override this; used in coupling serialization.
     */
    public Attribute getAttributeForKey(String key) {
        return null;
    }

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
     * Return producing attributes in a particular order, for use in creating couplings.
     * Can create customized orderings by overriding this method.
     * 
     * @return custom list of producing attributes.
     */
    public ArrayList<ProducingAttribute<?>> getCustomListOfProducingAttributes() {
        
        ArrayList<ProducingAttribute<?>> list = new ArrayList<ProducingAttribute<?>>();
    
        if (strategy.equals(Strategy.DEFAULT_EACH)) {
            for (Producer producer : this.getProducers()) {
                list.add(producer.getDefaultProducingAttribute());
            }
        } else if (strategy.equals(Strategy.TOTAL)) {
            for (Producer producer : this.getProducers()) {
                for (ProducingAttribute<?> attribute : producer.getProducingAttributes()) {
                    list.add(attribute);
                }
            }
        }
        return list;
    }

    /**
     * Return consuming attributes in a particular order, for use in creating couplings.
     * Can create customized orderings by overriding this method.
     * 
     * @return custom list of producing attributes.
     */
    public ArrayList<ConsumingAttribute<?>> getCustomListOfConsumingAttributes() {
        
        ArrayList<ConsumingAttribute<?>> list = new ArrayList<ConsumingAttribute<?>>();
        if (strategy.equals(Strategy.DEFAULT_EACH)) {
            for (Consumer consumer : this.getConsumers()) {
                list.add(consumer.getDefaultConsumingAttribute());
            }
        } else if (strategy.equals(Strategy.TOTAL)) {
            for (Consumer consumer : this.getConsumers()) {
                for (ConsumingAttribute<?> attribute : consumer.getConsumingAttributes()) {
                    list.add(attribute);
                }

            }
        }
        return list;
    }
    
    /**
     * Save a workspace component to a file. Currently assumes xml.
     *
     * @param saveFile the file to save.
     */
    public void save(final File saveFile) {
        setCurrentFile(saveFile);
        String xml = getXML();
        try {
            FileWriter writer  = new FileWriter(saveFile);
            writer.write(xml);
            writer.close();
            setChangedSinceLastSave(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        
    /**
     * Open a workspace component from a file.  Currently assumes xml.
     *
     * @param openFile file representing saved component.
     */
    public void open(final File openFile) {
        setCurrentFile(openFile);
        FileReader reader;
        try {
            reader = new FileReader(openFile);
            deserializeFromReader(reader);
            setChangedSinceLastSave(false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Override for use with open service.
     *
     * @return xml string representing stored file.
     */
    public String getXML() {
        return null;
    }

    /**
     * Override for use with save service.
     * @param reader
     */
    public void deserializeFromReader(final FileReader reader) {
        // no implementation
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

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }
    

    /**
     * The file extension for a component type, e.g. By default, "xml".
     *
     * @return the file extension
     */
    public String getFileExtension() {
        return "xml";
    }
    
    /**
     * Set to true when a component changes, set to false after a component is saved.
     *
     * @param changedSinceLastSave whether this component has changed since the last save.
     */
    public void setChangedSinceLastSave(final boolean changedSinceLastSave) {
        LOGGER.debug("component changed");
        this.changedSinceLastSave = changedSinceLastSave;
    }

    /**
     * Returns true if it's changed since the last save.
     *
     * @return the changedSinceLastSave
     */
    public boolean hasChangedSinceLastSave() {
        return changedSinceLastSave;
    }

    /**
     * This should be overridden if there are user preferences to get.
     *
     * @return the currentDirectory
     */
    public String getCurrentDirectory() {
        return currentDirectory;
    }

    /**
     *
     * This should be overridden if there are user preferences to set.
     *
     * @param currentDirectory the currentDirectory to set
     */
    public void setCurrentDirectory(final String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    /**
     * @return the currentFile
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * @param currentFile the currentFile to set
     */
    public void setCurrentFile(final File currentFile) {
        this.currentFile = currentFile;
    }

}
