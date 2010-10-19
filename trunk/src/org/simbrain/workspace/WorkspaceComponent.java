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
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.simbrain.workspace.updator.ComponentUpdatePart;

/**
 * Represents a component in a Simbrain {@link org.simbrain.workspace.Workspace}
 * . Extend this class to create your own component type. Gui representations of
 * a workspace component should extend
 * {@link org.simbrain.workspace.gui.GuiComponent}.
 *
 */
public abstract class WorkspaceComponent {

    /** The workspace that 'owns' this component. */
    private Workspace workspace;

    /** Log4j logger. */
    private Logger logger = Logger.getLogger(WorkspaceComponent.class);

    /** The set of all WorkspaceComponentListeners on this component. */
    private final Collection<WorkspaceComponentListener> workspaceComponentListeners;

    /** List of attribute listeners. */
    private final Collection<AttributeListener> attributeListeners;

    /** Whether this component has changed since last save. */
    private boolean changedSinceLastSave = false;

    /** List of producer types. */
    private final List<AttributeType> producerTypes = new ArrayList<AttributeType>();

    /** List of consumer types. */
    private final List<AttributeType> consumerTypes = new ArrayList<AttributeType>();

    /**
     * Whether to display the GUI for this component (obviously only relevant
     * when Simbrain is run as a GUI).
     */
    private Boolean guiOn = true;

    /** Whether to update this component. */
    private Boolean updateOn = true;

    /** The name of this component.  Used in the title, in saving, etc. */
    private String name  = "";

    /** Default current directory if it is not set elsewhere. */
    private static final String DEFAULT_CURRENT_DIRECTORY = "."
            + System.getProperty("file.separator");

    /**
     * Current directory. So when re-opening this type of component the app
     * remembers where to look.
     *
     * <p> Subclasses can provide a default value using User Preferences.
     */
    private String currentDirectory = DEFAULT_CURRENT_DIRECTORY;

    /**
     * Current file.  Used when "saving" a component.
     * Subclasses can provide a default value using User Preferences.
     */
    private File currentFile;

    /**
     * Initializer
     */
    {
        workspaceComponentListeners = new HashSet<WorkspaceComponentListener>();
        attributeListeners = new HashSet<AttributeListener>();
    }

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
     * Used when saving a workspace. All changed workspace components are saved
     * using this method.
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
     * Return the potential consumers associated with this component. Subclasses
     * should override this to make their consumers available.
     *
     * @return the consumer list.
     */
    public List<PotentialConsumer> getPotentialConsumers() {
        return Collections.EMPTY_LIST;
    }

    /**
     * Return the potential producers associated with this component. Subclasses
     * should override this to make their producers available.
     *
     * @return the producer list.
     */
    public List<PotentialProducer> getPotentialProducers() {
        return Collections.EMPTY_LIST;
    }

    /**
     * Fire attribute object removed event (when the base object of an attribute
     * is removed).
     *
     * @param object the object which was removed
     */
    public void fireAttributeObjectRemoved(Object object) {
        for (AttributeListener listener : attributeListeners) {
            listener.attributeObjectRemoved(object);
        }
    }

    /**
     * Fire potential attributes changed event.
     */
    public void firePotentialAttributesChanged() {
        for (AttributeListener listener : attributeListeners) {
            listener.potentialAttributesChanged();
        }
    }

    /**
     * Fire attribute type visibility changed event.
     *
     * @param type the type whose visibility changed.
     */
    public void fireAttributeTypeVisibilityChanged(AttributeType type) {
        for (AttributeListener listener : attributeListeners) {
            listener.attributeTypeVisibilityChanged();
        }

    }

    /**
     * Adds a AttributeListener to this component.
     *
     * @param listener the AttributeListener to add.
     */
    public void addAttributeListener(final AttributeListener listener) {
        attributeListeners.add(listener);
    }

    /**
     * Removes an AttributeListener from this component.
     *
     * @param listener the AttributeListener to remove.
     */
    public void removeAttributeListener(AttributeListener listener) {
        attributeListeners.remove(listener);
    }

    /**
     * Add a new type of producer.
     *
     * @param type type to add
     */
    public void addProducerType(AttributeType type) {
        producerTypes.add(type);
    }

    /**
     * Add a new type of consumer.
     *
     * @param type type to add
     */
    public void addConsumerType(AttributeType type) {
        consumerTypes.add(type);
    }

    /**
     * Create a producer. This version of the method does the real work; others
     * forward to it.
     *
     * @param parentObject parent object
     * @param methodBaseName name of method
     * @param dataType type of data
     * @param description description
     * @return the resulting producer
     */
    public Producer<?> createProducer(
            final Object parentObject,
            final String methodBaseName,
            final Class<?> dataType,
            final String description) {

        Producer<?> producer = new Producer() {

            private Method theMethod;

            // Static initializer
            {
                try {
                    theMethod = parentObject.getClass().getMethod(
                            "get" + methodBaseName, null);
                } catch (SecurityException e1) {
                    e1.printStackTrace();
                } catch (NoSuchMethodException e1) {
                    System.err.println("Could not find method "
                            + methodBaseName + " with return type of "
                            + dataType.getCanonicalName());
                    e1.printStackTrace();
                }

            }

            /**
             * {@inheritDoc}
             */
            public Object getValue() {
                try {
                    return theMethod.invoke(parentObject, null);
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return null;
            }

            /**
             * {@inheritDoc}
             */
            public WorkspaceComponent getParentComponent() {
                return WorkspaceComponent.this;
            }

            /**
             * {@inheritDoc}
             */
            public Object getBaseObject() {
                return parentObject;
            }

            /**
             * {@inheritDoc}
             */
            public String getMethodBaseName() {
                return methodBaseName;
            }

            /**
             * {@inheritDoc}
             */
            public Class<?> getDataType() {
                return dataType;
            }

            /**
             * {@inheritDoc}
             */
            public String getDescription() {
                return description;
            }

        };
        return producer;

    }


    /**
     * Create a producer without specifying a custom description (the
     * description is created automatically).
     *
     * @param baseObject
     *            base object
     * @param methodBaseName
     *            method name
     * @param dataType
     *            data type
     * @return created producer
     */
    public Producer<?> createProducer(final Object baseObject,
            final String methodBaseName, final Class<?> dataType) {
        String description = getDescriptionString(baseObject, methodBaseName, dataType);
        return createProducer(baseObject, methodBaseName, dataType, description);
    }

    /**
     * Create an actual producer from a potential producer.
     *
     * @param potentialAttribute the potential attribute to actualize
     * @return the resulting producer
     */
    public Producer<?> createProducer(final PotentialAttribute potentialAttribute) {
        return createProducer(potentialAttribute.getBaseObject(), potentialAttribute
                .getMethodBaseName(), potentialAttribute.getDataType(),
                potentialAttribute.getDescription());
    }

    /**
     * Create a consumer. This version of the method does the real work; others
     * forward to it.
     *
     * @param parentObject parent object
     * @param methodBaseName name of method
     * @param dataType type of data
     * @param description description
     * @return the resulting consumer
     */
    public Consumer<?> createConsumer(final Object parentObject,
            final String methodBaseName, final Class<?> dataType,
            final String description) {

        Consumer<?> consumer = new Consumer() {

            Method theMethod;

            // Static initializer
            {
                try {
                    theMethod = parentObject.getClass().getMethod(
                            "set" + methodBaseName, new Class[] { dataType });
                } catch (SecurityException e1) {
                    e1.printStackTrace();
                } catch (NoSuchMethodException e1) {
                    System.err.println("Could not find method "
                            + methodBaseName + " with argument of type of "
                            + dataType.getCanonicalName());
                    e1.printStackTrace();
                }
            }

            /**
             * {@inheritDoc}
             */
            public void setValue(Object value) {
                try {
                    theMethod.invoke(parentObject, new Object[] { value });
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            /**
             * {@inheritDoc}
             */
            public WorkspaceComponent getParentComponent() {
                return WorkspaceComponent.this;
            }

            /**
             * {@inheritDoc}
             */
            public Object getBaseObject() {
                return parentObject;
            }

            /**
             * {@inheritDoc}
             */
            public String getMethodBaseName() {
                return methodBaseName;
            }

            /**
             * {@inheritDoc}
             */
            public Class<?> getDataType() {
                return dataType;
            }

            /**
             * {@inheritDoc}
             */
            public String getDescription() {
                return description;
            }

        };
        return consumer;

    }

    /**
     * Create a consumer using
     *  1) Parent Object
     *  2) Method name
     *  3) Data type
     *  Description is automatically created.
     */
    public Consumer<?> createConsumer(final Object baseObject,
            final String methodBaseName, final Class<?> dataType) {
        String description = getDescriptionString(baseObject, methodBaseName, dataType);
        return createConsumer(baseObject, methodBaseName, dataType, description);
    }

    /**
     * Create an actual consumer from a potential consumer.
     *
     * @param potentialAttribute the potential attribute to actualize
     * @return the resulting consumer
     */
    public Consumer<?> createConsumer(final PotentialAttribute potentialAttribute) {
        return createConsumer(potentialAttribute.getBaseObject(), potentialAttribute
                .getMethodBaseName(), potentialAttribute.getDataType(),
                potentialAttribute.getDescription());
    }

    /**
     * Returns a formatted description string
     *
     * @param baseObject base object
     * @param methodBaseName base name of method
     * @param dataType class of data
     * @return formatted string
     */
    private String getDescriptionString(Object baseObject,
            String methodBaseName, Class<?> dataType) {
        return baseObject.getClass().getSimpleName() + ":" + methodBaseName
                + "<" + dataType.getSimpleName() + ">";
    }

    /**
     * Finds objects based on a key. Used in deserializing attributes. Any class
     * that produces attributes should override this for serialization.
     *
     * @param objectKey String key
     * @return the corresponding object
     */
    public Object getObjectFromKey(final String objectKey) {
        return null;
    }

    /**
     * Returns a unique key associated with an object. Used in serializing
     * attributes. Any class that produces attributes should override this for
     * serialization.
     *
     * @param object object which should be associated with a key
     * @return the key
     */
    public String getKeyFromObject(Object object) {
        return null;
    }


    /**
     * Find the potential producer whose base object matches the supplied
     * object.
     *
     * @param baseObject object to match
     * @param methodName method name to match
     * @return matching producer, or null if there is none.
     */
    public PotentialAttribute getPotentialProducer(Object baseObject,
            String methodName) {
        for (PotentialAttribute producer : this.getPotentialProducers()) {
            boolean baseObjectMatches = (producer.getBaseObject() == baseObject);
            boolean methodNameMatches = (producer.getMethodBaseName()
                    .equalsIgnoreCase(methodName));
            if (baseObjectMatches && methodNameMatches) {
                return producer;
            }
        }
        return null;
    }

    /**
     * Find the potential consumer whose base object matches the supplied
     * object.
     *
     * @param baseObject object to match
     * @param methodName method name to match
     * @return matching producer, or null if there is none.
     */
    public PotentialAttribute getPotentialConsumer(Object baseObject,
            String methodName) {
        for (PotentialAttribute consumer : this.getPotentialConsumers()) {
            boolean baseObjectMatches = (consumer.getBaseObject() == baseObject);
            boolean methodNameMatches = (consumer.getMethodBaseName()
                    .equalsIgnoreCase(methodName));
            if (baseObjectMatches && methodNameMatches) {
                return consumer;
            }
        }
        return null;
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

        return Collections.singleton(new ComponentUpdatePart(this, callable,
                toString(), this));
    }

    /**
     * Returns the locks for the update parts. There should be one lock per
     * part. These locks need to be the same ones used to lock the update of
     * each part.
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
     * Override for use with open service.
     *
     * @return xml string representing stored file.
     */
    public String getXML() {
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
        logger.debug("component changed");
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

    /**
     * @return the producerTypes
     */
    public List<AttributeType> getProducerTypes() {
        return producerTypes;
    }

    /**
     * @return the consumerTypes
     */
    public List<AttributeType> getConsumerTypes() {
        return consumerTypes;
    }

}
