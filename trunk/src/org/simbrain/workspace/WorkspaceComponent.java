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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.simbrain.workspace.updator.ComponentUpdatePart;

/**
 * Represents a component in a Simbrain {@link org.simbrain.workspace.Workspace}.
 * Extend this class to create your own component type.  Gui representations of
 * a workspace component should extend {@link org.simbrain.workspace.gui.GuiComponent}.
 * 
 */
public abstract class WorkspaceComponent {
    
    /** The workspace that 'owns' this component. */
    private Workspace workspace;
    
    /** The static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WorkspaceComponent.class);
    
    /** Log4j logger. */
    private Logger logger = Logger.getLogger(WorkspaceComponent.class);

    //TODO: Check these for thread safety...
    
    /** Consumer list. */
    private final List<Consumer> consumers = new CopyOnWriteArrayList<Consumer>();

    /** Producer list. */
    private final List<Producer> producers = new CopyOnWriteArrayList<Producer>();

    /** Whether this component has changed since last save. */
    private boolean changedSinceLastSave = true;

    /**
     * Whether to display the GUI for this component (obviously only relevant
     * when Simbrain is run as a GUI).
     */
    private Boolean guiOn = true;
    
    /** Whether to udpate this component. */
    private Boolean updateOn = true;
    
    /** The name of this component.  Used in the title, in saving, etc. */
    private String name  = "";
    
    /** How to order a list of attributes. */
    public enum AttributeListingStyle { TOTAL, DEFAULT_EACH };
    
    /** Current attribute listing style. */
    private AttributeListingStyle attributeListingStyle = AttributeListingStyle.DEFAULT_EACH;

    /** Default current directory if it is not set elsewhere. */
    private static final String DEFAULT_CURRENT_DIRECTORY = "."
            + System.getProperty("file.separator");

    /**
     * Current directory. So when re-opening this type of component the app remembers
     * where to look.
     * <p>Subclasses can provide a default value using User Preferences.
     */
    private String currentDirectory = DEFAULT_CURRENT_DIRECTORY;

    /**
     * Current file.  Used when "saving" a component.
     * Subclasses can provide a default value using User Preferences.
     */
    private File currentFile;

    /** The set of all WorkspaceComponentListeners on this component. */
    private final Collection<WorkspaceComponentListener> workspaceComponentListeners = 
        new HashSet<WorkspaceComponentListener>();
    
    /** List of attribute listeners. */
    private final Collection<AttributeHolderListener> attributeListeners = 
        new HashSet<AttributeHolderListener>();
    
    /**
     * Construct a workspace component.
     * 
     * @param name The name of the component.
     */
    public WorkspaceComponent(final String name) {
        this.name = name;
        logger.trace(this.getClass().getCanonicalName() + ": " + name + " created");
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

    /**
     * Closes the WorkspaceComponent.
     */
    public void close() {
        closing();
        workspace.removeWorkspaceComponent(this);
    }
    
    /**
     * Perform cleanup after closing.
    */
    protected abstract void closing();

    /**
     * Called by Workspace to update the state of the component.
     */
    public void update() {
        /* no default implementation */
    }
    
    /**
     * Returns the collection of update parts for this component.
     * 
     * @return The collection of update parts for this component.
     */
    public Collection<ComponentUpdatePart> getUpdateParts() {
        Runnable callable = new Runnable() {
            public void run() {
                update();
            }
        };
        
        return Collections.singleton(new ComponentUpdatePart(this, callable, toString(), this));
    }
    
    /**
     * Returns the locks for the update parts.  There should be one lock
     * per part.  These locks need to be the same ones used to lock the
     * update of each part.
     * 
     * @return The locks for the update parts.
     */
    public Collection<? extends Object> getLocks() {
        return Collections.singleton(this);
    }
    
    /**
     * Called by Workspace to notify that updates have stopped.
     */
    protected void stopped() {
        /* no default implementation */
    }
    
    /**
     * Notify all workspaceComponentListeners of a componentUpdated event.
     */
    public final void fireUpdateEvent() {
        for (WorkspaceComponentListener listener : workspaceComponentListeners) {
            listener.componentUpdated();
        }
    }
    
    /**
     * Notify all workspaceComponentListeners that the gui has been turned on or off.
     */
    public final void fireGuiToggleEvent() {
        for (WorkspaceComponentListener listener : workspaceComponentListeners) {
            listener.guiToggled();
        }
    }

    /**
     * Notify all workspaceComponentListeners of a component has been turned on or off.
     */
    public final void fireComponentToggleEvent() {
        for (WorkspaceComponentListener listener : workspaceComponentListeners) {
            listener.componentOnOffToggled();
        }
    }

    
    /**
     * Called after a global update ends.
     */
    final void doStopped() {
        stopped();
    }
    
    /**
     * Returns the WorkspaceComponentListeners on this component.
     * 
     * @return The WorkspaceComponentListeners on this component.
     */
    public Collection<WorkspaceComponentListener> getWorkspaceComponentListeners() {
        return Collections.unmodifiableCollection(workspaceComponentListeners);
    }
    
    /**
     * Adds a WorkspaceComponentListener to this component.
     * 
     * @param listener the WorkspaceComponentListener to add.
     */
    public void addWorkspaceComponentListener(final WorkspaceComponentListener listener) {
        workspaceComponentListeners.add(listener);
    }
    
    /**
     * Adds a WorkspaceComponentListener to this component.
     * 
     * @param listener the WorkspaceComponentListener to add.
     */
    public void removeWorkspaceComponentListener(final WorkspaceComponentListener listener) {
        workspaceComponentListeners.remove(listener);
    }
    
    /**
     * Returns the Attribute Listeners on this component.
     * 
     * @return The Attribute Listeners on this component.
     */
    public Collection<AttributeHolderListener> getAttributeListeners() {
        return Collections.unmodifiableCollection(attributeListeners);
    }
    
    /**
     * Adds a AttributeHolderListener to this component.
     * 
     * @param listener the AttributeHolderListener to add.
     */
    public void addAttributeListener(final AttributeHolderListener listener) {
        attributeListeners.add(listener);
    }
    
    /**
     * Adds a AttributeHolderListener to this component.
     * 
     * @param listener the AttributeHolderListener to add.
     */
    public void removeAttributeListener(final AttributeHolderListener listener) {
        attributeListeners.remove(listener);
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
        //TODO: Think about this
//        for (WorkspaceComponentListener listener : this.getListeners()) {
//            listener.setTitle(name);
//        }
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
     * Return producing attributes in a particular order, for use in creating couplings.
     * 
     * @return custom list of producing attributes.
     */
    public ArrayList<ProducingAttribute<?>> getProducingAttributes() {
        
        ArrayList<ProducingAttribute<?>> list = new ArrayList<ProducingAttribute<?>>();
    
        if (attributeListingStyle.equals(AttributeListingStyle.DEFAULT_EACH)) {
            for (Producer producer : this.getProducers()) {
                list.add(producer.getProducingAttributes().get(0));
            }
        } else if (attributeListingStyle.equals(AttributeListingStyle.TOTAL)) {
            for (Producer producer : this.getProducers()) {
                for (ProducingAttribute<?> attribute : producer.getProducingAttributes()) {
                    list.add(attribute);
                }
            }
        }
        return list;
    }

    /**
     * Return consuming attributes in a specified order.
     * 
     * @return custom list of producing attributes.
     */
    public ArrayList<ConsumingAttribute<?>> getConsumingAttributes() {
        
        ArrayList<ConsumingAttribute<?>> list = new ArrayList<ConsumingAttribute<?>>();
        if (attributeListingStyle.equals(AttributeListingStyle.DEFAULT_EACH)) {
            for (Consumer consumer : this.getConsumers()) {
                list.add(consumer.getConsumingAttributes().get(0));
            }
        } else if (attributeListingStyle.equals(AttributeListingStyle.TOTAL)) {
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
     * 
     * @param reader the reader to deserialize from.
     */
    public final void deserializeFromReader(final FileReader reader) {
        // no implementation
    }

    /**
     * Returns the producers associated with this component.
     * 
     * @return The producers associated with this component.
     */
    public List<Producer> getProducers() {
        return producers;
    }
    
    /**
     * Returns the consumers associated with this component.
     * 
     * @return The consumers associated with this component.
     */
    public List<Consumer> getConsumers() {
        return consumers;
    }
    
    /**
     * Adds the specified consumer.
     *
     * @param consumer consumer to add.
     */
    public void addConsumer(final Consumer consumer) {
        consumers.add(consumer);
        for (AttributeHolderListener listener : attributeListeners) {
            listener.consumerAdded(consumer);
        }
    }

    /**
     * Adds the specified producer.
     *
     * @param producer producer to add.
     */
    public void addProducer(final Producer producer) {
        producers.add(producer);
        for (AttributeHolderListener listener : attributeListeners) {
            listener.producerAdded(producer);
        }
    }
    
    /**
     * Removes specified consumer.
     *
     * @param consumer consumer to remove
     */
    public void removeConsumer(final Consumer consumer) {
        workspace.getCouplingManager().removeAttachedCouplings(consumer);
        consumers.remove(consumer);
        for (AttributeHolderListener listener : attributeListeners) {
            listener.consumerRemoved(consumer);
        }
    }
    
    /**
     * Removes specified producer.
     *
     * @param producer producer to remove
     */
    public void removeProducer(final Producer producer) {
        workspace.getCouplingManager().removeAttachedCouplings(producer);
        producers.remove(producer);
        for (AttributeHolderListener listener : attributeListeners) {
            listener.producerRemoved(producer);
        }
    }
    
    /**
     * Get a SingleConsumingAttribute by name.
     *
     * @param consumerId id of single consuming attribute
     * @return the attribute
     */
    public ConsumingAttribute<?> getSingleConsumingAttribute(final String consumerId) {
        for (Consumer consumer : getConsumers()) {
            if (consumer instanceof SingleAttributeConsumer) {
                if (consumer.getDescription().equalsIgnoreCase(consumerId)) {
                    return ((SingleAttributeConsumer<?>)consumer).getAttribute();
                }
            }
        }
        return null;
    }


    /**
     * Get a SingleProducingAttribute by id.
     *
     * @param producerId id of single producing attribute
     * @return the attribute
     */
    public ProducingAttribute<?> getSingleProducingAttribute(final String producerId) {
        for (Producer producer : getProducers()) {
            if (producer instanceof SingleAttributeProducer) {
                if (producer.getDescription().equalsIgnoreCase(producerId)) {
                    return ((SingleAttributeProducer<?>)producer).getAttribute();
                }
            }
        }
        return null;
    }
    
    /**
    * Finds a consumer by name and returns it, or none if it is not found.
    *
    * @param consumerId the name of the consumer (attribute holder)
    * @return the the consumer
    */
    public Consumer getConsumer(final String consumerId) {
        for (Consumer consumer : getConsumers()) {
            //System.out.println(consumerId + "==" + consumer.getDescription() + "?");
            if (consumer.getDescription().equalsIgnoreCase(consumerId)) {
                return consumer;
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
    public ConsumingAttribute<?> getConsumingAttribute(final String consumerId,
            final String attributeId) {
        Consumer consumer = getConsumer(consumerId);
        if (consumer != null) {
            for (ConsumingAttribute<?> attribute : consumer.getConsumingAttributes()) {
                if (attribute.getKey().equals(attributeId)) {
                    return attribute;
                }
            }
        }
        return null;
    }

    /**
     * Finds a producer by name and returns it, or none if it is not found.
     *
     * @param producerId the name of the producer (attribute holder)
     * @return the producer
     */
     public Producer getProducer(final String producerId) {
         for (Producer producer : getProducers()) {
             //System.out.println(producerId + "==" + producer.getDescription() + "?");
             if (producer.getDescription().equalsIgnoreCase(producerId)) {
                 return producer;
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
    public ProducingAttribute<?> getProducingAttribute(final String producerId,
            final String attributeId) {
        Producer producer = getProducer(producerId);
        if (producer != null) {
            for (ProducingAttribute<?> attribute : producer.getProducingAttributes()) {
                if (attribute.getKey().equals(attributeId)) {
                    return attribute;
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
    public void setWorkspace(final Workspace workspace) {
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

    /**
     * Returns the attribute listing style.
     * 
     * @return The attribute listing style.
     */
    public AttributeListingStyle getAttributeListingStyle() {
        return attributeListingStyle;
    }

    /**
     * Sets the attribute listing style.
     * 
     * @param style the listing style.
     */
    public void setAttributeListingStyle(final AttributeListingStyle style) {
        this.attributeListingStyle = style;
    }
    
    /**
     * The overall name for the set of supported formats.
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
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * @param logger the logger to set
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * @return the guiOn
     */
    public Boolean getGuiOn() {
        return guiOn;
    }

    /**
     * @param guiOn the guiOn to set
     */
    public void setGuiOn(Boolean guiOn) {
        this.guiOn = guiOn;
        this.fireGuiToggleEvent();
    }

    /**
     * @return the updateOn
     */
    public Boolean getUpdateOn() {
        return updateOn;
    }

    /**
     * @param updateOn the updateOn to set
     */
    public void setUpdateOn(Boolean updateOn) {
        this.updateOn = updateOn;
        this.fireComponentToggleEvent();
    }
}
