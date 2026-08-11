package org.simbrain.workspace

import org.pmw.tinylog.Logger
import org.simbrain.workspace.couplings.Coupling
import org.simbrain.workspace.couplings.CouplingManager
import org.simbrain.workspace.events.WorkspaceComponentEvents
import java.io.File
import java.io.OutputStream

/**
 * Represents a component in a Simbrain [Workspace]. Extend this class to create your own component type.
 *
 * For deserialization sub-classes must have a static "open" method, that is called using reflection by
 * [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]. See
 * [org.simbrain.network.NetworkComponent.open] for an example.
 *
 */
abstract class WorkspaceComponent(name: String) {

    open lateinit var workspace: Workspace


    var isRunning: Boolean = false

    /**
     * The name of this component. Used in the title, in saving, etc.
     */
    var name: String = ""

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
     * Used when "saving" a component. Subclasses can provide a
     * default value using User Preferences.
     */
    var currentFile: File? = null

    /**
     * If set to true, serialize this component before others. Possibly replace
     * with priority system later.
     */
    var serializePriority: Int = 0
        protected set

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
    open val formats: List<String>
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
        events.close()
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
    fun fireAttributeContainerAdded(addedContainer: AttributeContainer) = events.attributeContainerAdded.fireAsync(addedContainer)

    /**
     * Notify listeners that an [AttributeContainer]  has been removed from the
     * component.
     */
    fun fireAttributeContainerRemoved(removedContainer: AttributeContainer) = events.attributeContainerRemoved.fire(removedContainer)

    /**
     * Notify listeners that an existing [AttributeContainer]'s couplable state has changed in a way consumers may
     * want to re-read, e.g. its labels. Relayed workspace-wide via
     * [org.simbrain.workspace.couplings.CouplingEvents.attributeContainerChanged].
     */
    fun fireAttributeContainerChanged(changedContainer: AttributeContainer) = events.attributeContainerChanged.fire(changedContainer)

    /**
     * Invoke [handler] whenever what a producer coupled into one of this component's [attributeContainers]
     * should be called may have changed: when such a coupling is created, and when the producing container
     * reports a change such as a relabeled neuron. Plots use this to name series, bars, slices, or axis ticks
     * from the producing side rather than fixing names when the plot is created.
     *
     * The handler receives the consuming container, so a component with several couplable containers, such as
     * a time series plot whose individual series can each be coupled to, can tell which one is affected, and
     * the producer, so it can take names alone ([Producer.getDisplayNames]), keyed components
     * ([Producer.getDisplayComponents]) when it holds per-component state to preserve, or the attribute's own
     * name ([Producer.getSimpleDescription]) when it displays the attribute as a whole. Components and names
     * are empty for a producer that declares none, which handlers must cope with.
     */
    fun onCoupledProducer(handler: (consumer: AttributeContainer, producer: Producer) -> Unit) {
        fun handle(coupling: Coupling) {
            val consumingContainer = coupling.consumer.baseObject
            if (attributeContainers.any { it === consumingContainer }) {
                handler(consumingContainer, coupling.producer)
            }
        }
        workspace.couplingManager.events.couplingAdded.on { handle(it) }
        workspace.couplingManager.events.attributeContainerChanged.on { changed ->
            workspace.couplingManager.couplings
                .filter { it.producer.baseObject === changed }
                .forEach { handle(it) }
        }
    }

    /**
     * Called after a global update ends.
     */
    fun doStopped() {
        stopped()
    }

    override fun toString(): String {
        return name
    }

    /**
     * Retrieves a simple version of a component name from its class, e.g.
     * "Network" from "org.simbrain.network.NetworkComponent".
     *
     * @return the simple name.
     */
    val simpleName: String
        get() {
            var simpleName = javaClass.simpleName
            if (simpleName.endsWith("Component")) {
                simpleName = simpleName.replaceFirst("Component".toRegex(), "")
            }
            return simpleName
        }

    open val xml: String?
        get() = null

    /**
     * The file extension for a component type, e.g. By default, "xml".
     *
     * @return the file extension
     */
    val defaultFormat: String
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
