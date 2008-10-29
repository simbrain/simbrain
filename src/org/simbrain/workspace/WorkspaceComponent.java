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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Represents a component in a Simbrain {@link org.simbrain.workspace.Workspace}. Extend this class to create
 * your own component type.  Gui representations of a workspace component should extend {@link org.simbrain.workspace.gui.GuiComponent}.
 * 
 * @param <E> The type of the workspace listener associated with this
 * component.
 */
public abstract class WorkspaceComponent<E extends WorkspaceComponentListener> implements UpdatePriority {

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
    
    /** Default priority. */
    private static final int DEFAULT_PRIORITY = 0;
    
    /** Priority of this component; used in priorty based workspace update. */
    private int priority = DEFAULT_PRIORITY;
    
    /** How to order a list of attributes. */
    public enum Strategy {TOTAL, DEFAULT_EACH, CUSTOM };
    
    /** Current Strategy for this component. */
    private Strategy strategy;

    /**
     * Current directory. So when re-opening this type of component the app remembers where to look. 
     * Subclasses can provide a default value using User Preferences.
     */
    private String currentDirectory;

    /**
     * Current file.  Used when "saving" a component.
     * Subclasses can provide a default value using User Preferences.
     */
    private File currentFile;

    
    /** The set of all listeners on this component. */
    private Collection<E> listeners = new HashSet<E>();
    
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
     * Returns a list of the formats that this component supports.
     * 
     * <p>The default behavior is to return an empty list.  This means
     * that there is one format.
     * 
     * @return a list of the formats that this component supports.
     */
    public List<? extends String> getFormats() {
        return Collections.singletonList(getDefaultFormat());
    }

    public final void close() {
        closing();
        workspace.removeWorkspaceComponent(this);
    }
    
    /**
     * Perform cleanup after closing.
    */
    protected abstract void closing();

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
    public final void doUpdate() {
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
//        return this.getClass().getSimpleName() + ": " + name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
        for (WorkspaceComponentListener listener : this.getListeners()) {
            listener.setTitle(name);
        }
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
     * Open a workspace component from a file.  Currently assumes xml.
     *
     * @param openFile file representing saved component.
     */
    public final void open(final File openFile) {
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
     * Get a SingleConsumingAttribute by name
     *
     * @param consumerId id of single consuming attribute
     * @return the attribute
     */
    public ConsumingAttribute getSingleConsumingAttribute(String consumerId) {
        for (Consumer consumer : getConsumers()) {
            if (consumer instanceof SingleAttributeConsumer) {
                if (consumer.getDescription().equalsIgnoreCase(consumerId)) {
                    return consumer.getDefaultConsumingAttribute();
                }
            }
        }
        return null;
    }


    /**
     * Get a SingleProducingAttribute by id
     *
     * @param producerId id of single producing attribute
     * @return the attribute
     */
    public ProducingAttribute getSingleProducingAttribute(String producerId) {
        for (Producer producer : getProducers()) {
            if (producer instanceof SingleAttributeProducer) {
                if (producer.getDescription().equalsIgnoreCase(producerId)) {
                    return producer.getDefaultProducingAttribute();
                }
            }
        }
        return null;
    }

    /**
     * Get a consuming attribute, using the id of the attribute holder and the attribute itself.
     *
     * @param consumerId the name of the consumer (attribute holder)
     * @param attributeId the name of the attribute
     * @return the consuming attribute
     */
    public ConsumingAttribute getConsumingAttribute(String consumerId, String attributeId) {
        for (Consumer consumer : getConsumers()) {
            if (consumer.getDescription().equalsIgnoreCase(consumerId)) {
                   for(ConsumingAttribute attribute : consumer.getConsumingAttributes()) {
                       //System.out.println(attribute.getAttributeDescription());
                          if (attribute.getAttributeDescription().equalsIgnoreCase(attributeId)) {
                              return attribute;
                          }
                   }
            }
        }
        return null;
    }

    /**
     * Get a producing attribute, using the id of the attribute holder and the attribute itself.
     *
     * @param producerId the name of the producing (attribute holder)
     * @param attributeId the name of the attribute
     * @return the producing attribute
     */
    public ProducingAttribute getProducingAttribute(String producerId, String attributeId) {
        for (Producer producer : getProducers()) {
            //System.out.println(producer.getDescription());            
            if (producer.getDescription().equalsIgnoreCase(producerId)) {
                   for(ProducingAttribute attribute : producer.getProducingAttributes()) {
                       //System.out.println(attribute.getAttributeDescription());
                          if (attribute.getAttributeDescription().equalsIgnoreCase(attributeId)) {
                              return attribute;
                          }
                   }
            }
        }
        return null;
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
     * The overall name for the set of supported formats
     *
     * @return the description
     */
    public String getDescription() {
        return null;
    }
    
    /**
     * The file extension for a component type, e.g. By default, "xml".
     *
     * @return the file extension
     */
    public String getDefaultFormat() {
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
    
    /**
     * {@inheritDoc}
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setPriority(int value) {
        priority = value;
    }
    
    

}
