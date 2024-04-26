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
package org.simbrain.workspace

import org.pmw.tinylog.Logger
import org.simbrain.workspace.couplings.CouplingManager
import org.simbrain.workspace.events.WorkspaceComponentEvents
import java.io.File
import java.io.OutputStream

/**
 * Represents a component in a Simbrain [Workspace]. Extend this class to create your own component type.
 *
 *
 * For deserialization sublclasses must have a static "open" method, that is called using reflection by
 * [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]. See
 * [org.simbrain.network.NetworkComponent.open] for an example.
 */
abstract class WorkspaceComponent(name: String) {
    /**
     * Returns the workspace associated with this component.
     */
    /**
     * Sets the workspace for this component. Called by the workspace right
     * after this component is created.
     *
     * @param workspace The workspace for this component.
     */
    /**
     * The workspace that 'owns' this component.
     */
    open lateinit var workspace: Workspace

    @JvmField
    @Transient
    val events: WorkspaceComponentEvents = WorkspaceComponentEvents()

    /**
     * Whether this component has changed since last save.
     */
    private var changedSinceLastSave = false

    /**
     * Whether to display the GUI for this component (obviously only relevant
     * when Simbrain is run as a GUI). TODO: This should really be a property of
     * the GUI only, since we can imagine the gui is on or off for different
     * views of the component. This design is kind of hack, based on the fact
     * that [ComponentPanel] has no easy access to [DesktopComponent].
     */
    var isGuiOn: Boolean = true
        set(guiOn) {
            field = guiOn
            events.guiToggled.fire()
        }

    /**
     * Whether to update this component when updated from the workspace.
     */
    var updateOn: Boolean = true
        /**
         * @param updateOn the updateOn to set
         */
        set(updateOn) {
            field = updateOn
            events.guiToggled.fire()
        }

    /**
     * @return if this component is marked as running
     */
    /**
     * Sets whether or not this component is marked as currently running...
     * meant to be false if only doing a one-off update
     *
     * @param running
     */
    /**
     * Whether or not this component is being iterated more than just one time.
     */
    var isRunning: Boolean = false

    /**
     * Returns the name of this component.
     */
    /**
     * The name of this component. Used in the title, in saving, etc.
     */
    @JvmField
    var name: String = ""

    /**
     * Current file. Used when "saving" a component. Subclasses can provide a
     * default value using User Preferences.
     */
    @JvmField
    var currentFile: File? = null

    /**
     * Return the serializePriority
     */
    /**
     * @param serializePriority the serializePriority to set
     */
    /**
     * If set to true, serialize this component before others. Possibly replace
     * with priority system later.
     */
    var serializePriority: Int = 0
        /**
         * @param serializePriority the serializePriority to set
         */
        protected set

    /**
     * Construct a workspace component.
     *
     * @param name The name of the component.
     */
    init {
        this.name = name
        Logger.trace(javaClass.canonicalName + ": " + name + " created")
    }

    /**
     * Used when saving a workspace. All changed workspace components are saved
     * using this method.
     *
     * @param output the stream of data to write the data to.
     * @param format a key used to define the requested format.
     */
    abstract fun save(output: OutputStream, format: String?)

    /**
     * Returns a list of the formats that this component supports. The default
     * behavior is to return a list containing the default format.
     *
     * @return a list of the formats that this component supports.
     */
    open val formats: List<String?>
        get() = listOf(defaultFormat)

    /**
     * Fires an event which leads any linked gui components to close, which
     * calls the haschanged dialog.
     */
    fun tryClosing() {
        events.componentClosing.fire()
        // TODO: If there is no Gui then close must be called directly
    }

    /**
     * Closes the WorkspaceComponent.
     */
    open fun close() {
        attributeContainers.forEach { removedContainer ->
            this.fireAttributeContainerRemoved(
                removedContainer
            )
        }
        workspace.removeWorkspaceComponent(this)
    }

    /**
     * Called by Workspace to update the state of the component.
     */
    open suspend fun update() {
    }

    /**
     * Override to return a collection of all [AttributeContainer]'s currently managed by this
     * component.
     */
    open val attributeContainers: List<AttributeContainer>
        get() = ArrayList()

    val couplingManager: CouplingManager
        get() = workspace.couplingManager

    /**
     * Called by Workspace to notify that updates have stopped.
     */
    protected fun stopped() {
    }

    /**
     * Notify listeners that an [AttributeContainer] has been added to the component.
     */
    fun fireAttributeContainerAdded(addedContainer: AttributeContainer) = events.attributeContainerAdded.fire(addedContainer)

    /**
     * Notify listeners that an [AttributeContainer]  has been removed from the
     * component.
     */
    fun fireAttributeContainerRemoved(removedContainer: AttributeContainer) = events.attributeContainerRemoved.fire(removedContainer)

    /**
     * Called after a global update ends.
     */
    fun doStopped() {
        stopped()
    }

    override fun toString(): String {
        return name
    }

    val simpleName: String
        /**
         * Retrieves a simple version of a component name from its class, e.g.
         * "Network" from "org.simbrain.network.NetworkComponent".
         *
         * @return the simple name.
         */
        get() {
            var simpleName = javaClass.simpleName
            if (simpleName.endsWith("Component")) {
                simpleName = simpleName.replaceFirst("Component".toRegex(), "")
            }
            return simpleName
        }

    open val xml: String?
        /**
         * Override for use with open service.
         *
         * @return xml string representing stored file.
         */
        get() = null

    val defaultFormat: String
        /**
         * The file extension for a component type, e.g. By default, "xml".
         *
         * @return the file extension
         */
        get() = "xml"

    /**
     * Set to true when a component changes, set to false after a component is
     * saved.
     *
     * @param changedSinceLastSave whether this component has changed since the
     * last save.
     */
    fun setChangedSinceLastSave(changedSinceLastSave: Boolean) {
        Logger.debug("component changed")
        this.changedSinceLastSave = changedSinceLastSave
    }

    /**
     * Returns true if it's changed since the last save.
     */
    open fun hasChangedSinceLastSave(): Boolean {
        return changedSinceLastSave
    }

    /**
     * Called when a simulation begins, e.g. when the "run" button is pressed.
     * Subclasses should override this if special events need to occur at the
     * start of a simulation.
     */
    open fun start() {
    }

    /**
     * Called when a simulation stops, e.g. when the "stop" button is pressed.
     * Subclasses should override this if special events need to occur at the
     * start of a simulation.
     */
    open fun stop() {
    }

    /**
     * Any “read resolve” type initialization of components or models that require workspace access post serialization should occur in an override of this function.
     */
    open fun postOpenInit(workspace: Workspace?) {
    }
}
