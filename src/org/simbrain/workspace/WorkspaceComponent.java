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
import org.simbrain.workspace.gui.ComponentPanel;
import org.simbrain.workspace.gui.GuiComponent;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.simbrain.workspace.CouplingUtils.getConsumersFromContainer;
import static org.simbrain.workspace.CouplingUtils.getProducersFromContainers;

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

    /**
     * Log4j logger.
     */
    private Logger logger = Logger.getLogger(WorkspaceComponent.class);

    /**
     * The set of all WorkspaceComponentListeners on this component.
     */
    private Collection<WorkspaceComponentListener> listeners;

    /**
     * The attribute type method to visibility map. If a given method
     * should be invisible in the {@link org.simbrain.workspace.gui.couplingmanager.AttributePanel}
     * (for example if synapse couplings are visible this can crowd that panel).
     */
    private final Map<Method, Boolean> attributeTypeVisibilityMap = new HashMap<>();

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

    /** Initializer */ {
        listeners = new HashSet<WorkspaceComponentListener>();
    }

    /**
     * Construct a workspace component.
     *
     * @param name The name of the component.
     */
    public WorkspaceComponent(final String name) {
        this.name = name;
        logger.trace(getClass().getCanonicalName() + ": " + name + " created");
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
        fireComponentClosing();
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
     * Finds objects based on a key. Used in deserializing {@link Attribute}'s.
     * Any class that produces attributes should override this for
     * serialization.  Each attribute object in a component must be given a
     * unique id (relative to that component) for deserializing to work.
     *
     * @param objectKey String key
     * @return the corresponding object
     */
    public AttributeContainer getObjectFromKey(String objectKey) {
        return null;
    }

    /**
     * Return a collection of all {@link AttributeContainer}'s currently managed by this
     * component.
     */
    public List<AttributeContainer> getAttributeContainers() {
        return new ArrayList<>();
    }

    /**
     * Get all {@link Producible} or {@link Consumable} methods in this workspace component.
     *
     * @param annotation Annotation of the methods. Expect {@link Producible} or {@link Consumable}.
     * @return a list of all attribute methods in this component.
     */
    public List<Method> getAttributeMethods(Class<? extends Annotation> annotation) {
        if (annotation != Producible.class && annotation != Consumable.class) {
            return null;
        }
        return getAttributeContainers().stream()
                .map(Object::getClass)
                .distinct()
                .flatMap(c -> Arrays.stream(c.getMethods()))
                .filter(m -> m.isAnnotationPresent(annotation))
                .collect(Collectors.toList());
    }

    /**
     * Get all visible producers on a specified component.
     *
     * @return the visible producers
     */
    public List<Producer<?>> getVisibleProducers() {
        getProducers().stream()
                .map(Attribute::getMethod)
                .filter(m -> !attributeTypeVisibilityMap.containsKey(m))
                .forEach(m -> attributeTypeVisibilityMap.put(m, m.getAnnotation(Producible.class).defaultVisibility()));
        return getProducers().stream()
                .filter(a -> attributeTypeVisibilityMap.get(a.getMethod()))
                .collect(Collectors.toList());
    }

    /**
     * Get all visible consumers on a specified component.
     *
     * @return the visible consumers
     */
    public List<Consumer<?>> getVisibleConsumers() {
        getConsumers().stream()
                .map(Attribute::getMethod)
                .filter(m -> !attributeTypeVisibilityMap.containsKey(m))
                .forEach(m -> attributeTypeVisibilityMap.put(m, m.getAnnotation(Consumable.class).defaultVisibility()));
        return getConsumers().stream()
                .filter(a -> attributeTypeVisibilityMap.get(a.getMethod()))
                .collect(Collectors.toList());
    }

    /**
     * Get all the potential producers for a given WorkspaceComponent.
     *
     * @return A list of potential producers.
     */
    public List<Producer<?>> getProducers() {
        return getAttributeContainers().stream()
                .flatMap(ac -> getProducersFromContainers(ac).stream())
                .collect(Collectors.toList());
    }

    /**
     * Get all the potential consumers for a given WorkspaceComponent.
     *
     * @return A list of potential consumers.
     */
    public List<Consumer<?>> getConsumers() {
        return getAttributeContainers().stream()
                .flatMap(ac -> getConsumersFromContainer(ac).stream())
                .collect(Collectors.toList());
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
     * Notify listeners that the component has been updated.
     */
    public void fireUpdateEvent() {
        for (WorkspaceComponentListener listener : listeners) {
            listener.componentUpdated();
        }
    }

    /**
     * Notify listeners that the gui has been turned on or off.
     */
    public void fireGuiToggleEvent() {
        for (WorkspaceComponentListener listener : listeners) {
            listener.guiToggled();
        }
    }

    /**
     * Notify listeners that the component has been turned on or off.
     */
    public void fireComponentToggleEvent() {
        for (WorkspaceComponentListener listener : listeners) {
            listener.componentOnOffToggled();
        }
    }

    /**
     * Notify listeners that the component is closing.
     */
    public void fireComponentClosing() {
        for (WorkspaceComponentListener listener : listeners) {
            listener.componentClosing();
        }
    }

    /**
     * Update the visibility map when new attribute type is added.
     *
     * @param updatedContainer the new attribute container added to this workspace
     */
    public void updateVisibilityMap(AttributeContainer updatedContainer) {
        CouplingUtils.getConsumableMethodsFromContainer(updatedContainer)
                .forEach(m -> {
                    if (!attributeTypeVisibilityMap.containsKey(m)) {
                        attributeTypeVisibilityMap.put(m, m.getAnnotation(Consumable.class).defaultVisibility());
                    }
                });
        CouplingUtils.getProducibleMethodsFromContainer(updatedContainer)
                .forEach(m -> {
                    if (!attributeTypeVisibilityMap.containsKey(m)) {
                        attributeTypeVisibilityMap.put(m, m.getAnnotation(Producible.class).defaultVisibility());
                    }
                });
    }

    /**
     * Notify listeners that an {@link AttributeContainer} has been added to the component.
     */
    public void fireAttributeContainerAdded(AttributeContainer addedContainer) {
        for (WorkspaceComponentListener listener : listeners) {
            listener.attributeContainerAdded(addedContainer);
        }
        updateVisibilityMap(addedContainer);
    }

    /**
     * Notify listeners that an {@link AttributeContainer}  has been removed from the
     * component.
     */
    public void fireAttributeContainerRemoved(AttributeContainer removedContainer) {
        for (WorkspaceComponentListener listener : listeners) {
            listener.attributeContainerRemoved(removedContainer);
        }
    }

    /**
     * Notify listeners that an {@link AttributeContainer} has been changed in the component.
     */
    public void fireAttributeContainerChanged(AttributeContainer updatedContainer) {
        for (WorkspaceComponentListener listener : listeners) {
            listener.attributeContainerChanged(updatedContainer);
        }
    }

    /**
     * Called after a global update ends.
     */
    void doStopped() {
        stopped();
    }

    /**
     * Returns the WorkspaceComponentListeners on this component.
     *
     * @return The WorkspaceComponentListeners on this component.
     */
    public Collection<WorkspaceComponentListener> getListeners() {
        return Collections.unmodifiableCollection(listeners);
    }

    /**
     * Adds a listener to this component.
     *
     * @param listener the WorkspaceComponentListener to add.
     */
    public void addListener(WorkspaceComponentListener listener) {
        listeners.add(listener);
    }

    /**
     * Adds a listener to this component.
     *
     * @param listener the WorkspaceComponentListener to add.
     */
    public void removeListener(WorkspaceComponentListener listener) {
        listeners.remove(listener);
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

    public Map<Method, Boolean> getAttributeTypeVisibilityMap() {
        return attributeTypeVisibilityMap;
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
     * @return the currentFile
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * @param currentFile the currentFile to set
     */
    public void setCurrentFile(File currentFile) {
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
    public boolean isGuiOn() {
        return guiOn;
    }

    /**
     * @param guiOn the guiOn to set
     */
    public void setGuiOn(boolean guiOn) {
        this.guiOn = guiOn;
        this.fireGuiToggleEvent();
    }

    /**
     * @return the updateOn
     */
    public boolean getUpdateOn() {
        return updateOn;
    }

    /**
     * @param updateOn the updateOn to set
     */
    public void setUpdateOn(boolean updateOn) {
        this.updateOn = updateOn;
        this.fireComponentToggleEvent();
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
