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

import java.awt.Container;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.simbrain.network.NetworkComponent;
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
    private final Collection<PotentialAttributeListener> potentialAttributeListeners;

    /** Whether this component has changed since last save. */
    private boolean changedSinceLastSave = false;

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
//        consumers = new CopyOnWriteArrayList<Consumer>();
//        producers = new CopyOnWriteArrayList<Producer>();
//        attributeTypes = new ArrayList<String>();
        workspaceComponentListeners = new HashSet<WorkspaceComponentListener>();
        potentialAttributeListeners = new HashSet<PotentialAttributeListener>();
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

    // TODO: Change to abstract once I get this working
    public List<PotentialConsumer> getPotentialConsumers() {
        return null;
    }

    public List<PotentialProducer> getPotentialProducers() {
        return null;
    }
    
    public List<AttributeType> getAttributeTypes() {
        return null;
    }

    /**
     * Adds a AttributeListener to this component.
     *
     * @param listener the AttributeListener to add.
     */
    public void addAttributeListener(final PotentialAttributeListener listener) {
        potentialAttributeListeners.add(listener);
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

    public void firePotentialAttributeUpdateEvent(WorkspaceComponent component) {
        for (PotentialAttributeListener listener: potentialAttributeListeners) {
            listener.update(component);
        }
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

}
