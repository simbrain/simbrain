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

import org.pmw.tinylog.Logger;
import org.simbrain.workspace.events.WorkspaceComponentEvents;
import org.simbrain.workspace.gui.ComponentPanel;
import org.simbrain.workspace.gui.GuiComponent;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a component in a Simbrain {@link Workspace}. Extend this class to
 * create your own component type.
 * <p>
 * Note that for deserialization sublclasses must have a static "open" method,
 * that is called using reflection by {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}.
 * See {@link org.simbrain.network.NetworkComponent#open(InputStream, String,
 * String)} for an example.
 */
public abstract class WorkspaceComponent {

    /**
     * The workspace that 'owns' this component.
     */
    private Workspace workspace;

    transient private WorkspaceComponentEvents events = new WorkspaceComponentEvents(this);

    /**
     * Whether this component has changed since last save.
     */
    private boolean changedSinceLastSave = false;

    /**
     * Whether to display the GUI for this component (obviously only relevant
     * when Simbrain is run as a GUI). TODO: This should really be a property of
     * the GUI only, since we can imagine the gui is on or off for different
     * views of the component. This design is kind of hack, based on the fact
     * that {@link ComponentPanel} has no easy access to {@link GuiComponent}.
     */
    private boolean guiOn = true;

    /**
     * Whether to update this component.
     */
    private boolean updateOn = true;

    /**
     * Whether or not this component is being iterated more than just one time.
     */
    private boolean isRunning = false;

    /**
     * The name of this component. Used in the title, in saving, etc.
     */
    private String name = "";

    /**
     * Current file. Used when "saving" a component. Subclasses can provide a
     * default value using User Preferences.
     */
    private File currentFile;

    /**
     * If set to true, serialize this component before others. Possibly replace
     * with priority system later.
     */
    private int serializePriority = 0;

    /**
     * Construct a workspace component.
     *
     * @param name The name of the component.
     */
    public WorkspaceComponent(final String name) {
        this.name = name;
        Logger.trace(getClass().getCanonicalName() + ": " + name + " created");
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
     * Returns a list of the formats that this component supports. The default
     * behavior is to return a list containing the default format.
     *
     * @return a list of the formats that this component supports.
     */
    public List<? extends String> getFormats() {
        return Collections.singletonList(getDefaultFormat());
    }

    /**
     * Fires an event which leads any linked gui components to close, which
     * calls the haschanged dialog.
     */
    public void tryClosing() {
        events.fireComponentClosing();
        //TODO: If there is no Gui then close must be called directly
    }

    /**
     * Closes the WorkspaceComponent.
     */
    public void close() {
        closing();
        getAttributeContainers().forEach(this::fireAttributeContainerRemoved);
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
    }

    /**
     * Override to return objects based on a key. Used in deserializing {@link Attribute}s.
     * Any class that produces attributes should override this for
     * serialization.  Each attribute container in a component must be given a
     * unique id (relative to that component) for deserializing to work.
     *
     * @param objectKey String key
     * @return the corresponding AttributeContainer
     */
    public AttributeContainer getAttributeContainer(String objectKey) {
        return null;
    }

    /**
     * Override to return a collection of all {@link AttributeContainer}'s currently managed by this
     * component.
     */
    public List<AttributeContainer> getAttributeContainers() {
        return new ArrayList<>();
    }

    public CouplingManager getCouplingManager() {
        return workspace.getCouplingManager();
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
    }

    /**
     * Notify listeners that an {@link AttributeContainer} has been added to the component.
     */
    public void fireAttributeContainerAdded(AttributeContainer addedContainer) {
        events.fireAttributeContainerAdded(addedContainer);
    }

    /**
     * Notify listeners that an {@link AttributeContainer}  has been removed from the
     * component.
     */
    public void fireAttributeContainerRemoved(AttributeContainer removedContainer) {
        events.fireAttributeContainerRemoved(removedContainer);
    }

    /**
     * Called after a global update ends.
     */
    void doStopped() {
        stopped();
    }

    /**
     * Returns the name of this component.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
        // TODO: Think about this
        // for (WorkspaceComponentListener listener : this.getListeners()) {
        // listener.setTitle(name);
        // }
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Retrieves a simple version of a component name from its class, e.g.
     * "Network" from "org.simbrain.network.NetworkComponent"/
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
     * Returns the workspace associated with this component.
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * Sets the workspace for this component. Called by the workspace right
     * after this component is created.
     *
     * @param workspace The workspace for this component.
     */
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
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
     * Set to true when a component changes, set to false after a component is
     * saved.
     *
     * @param changedSinceLastSave whether this component has changed since the
     *                             last save.
     */
    public void setChangedSinceLastSave(boolean changedSinceLastSave) {
        Logger.debug("component changed");
        this.changedSinceLastSave = changedSinceLastSave;
    }

    /**
     * Returns true if it's changed since the last save.
     */
    public boolean hasChangedSinceLastSave() {
        return changedSinceLastSave;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }

    public boolean isGuiOn() {
        return guiOn;
    }

    public void setGuiOn(boolean guiOn) {
        this.guiOn = guiOn;
        events.fireGUIToggled();
    }

    public boolean getUpdateOn() {
        return updateOn;
    }

    /**
     * @param updateOn the updateOn to set
     */
    public void setUpdateOn(boolean updateOn) {
        this.updateOn = updateOn;
        events.fireComponentOnOffToggled();
    }

    /**
     * Sets whether or not this component is marked as currently running...
     * meant to be false if only doing a one-off update
     *
     * @param running
     */
    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    /**
     * @return if this component is marked as running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Return the serializePriority
     */
    public int getSerializePriority() {
        return serializePriority;
    }

    /**
     * @param serializePriority the serializePriority to set
     */
    protected void setSerializePriority(int serializePriority) {
        this.serializePriority = serializePriority;
    }

    public WorkspaceComponentEvents getEvents() {
        return events;
    }

    /**
     * Called when a simulation begins, e.g. when the "run" button is pressed.
     * Subclasses should override this if special events need to occur at the
     * start of a simulation.
     */
    public void start() {
    }

    /**
     * Called when a simulation stops, e.g. when the "stop" button is pressed.
     * Subclasses should override this if special events need to occur at the
     * start of a simulation.
     */
    public void stop() {
    }
}
