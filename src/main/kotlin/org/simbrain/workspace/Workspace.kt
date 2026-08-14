package org.simbrain.workspace

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.pmw.tinylog.Logger
import org.simbrain.custom_sims.NewSimulation
import org.simbrain.custom_sims.simulations
import org.simbrain.docviewer.DocViewer
import org.simbrain.network.NetworkComponent
import org.simbrain.network.update_actions.BufferedUpdate
import org.simbrain.network.update_actions.PriorityUpdate
import org.simbrain.network.update_actions.UpdateNetworkModel
import org.simbrain.util.ControlPanelKt
import org.simbrain.util.SimpleIdManager
import org.simbrain.util.UpdateAction
import org.simbrain.util.updateAction
import org.simbrain.workspace.couplings.Coupling
import org.simbrain.workspace.couplings.CouplingManager
import org.simbrain.workspace.events.WorkspaceEvents
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.serialization.WorkspaceSerializer
import org.simbrain.workspace.updater.UpdateAllAction
import org.simbrain.workspace.updater.UpdateComponent
import org.simbrain.workspace.updater.UpdateCoupling
import org.simbrain.workspace.updater.WorkspaceUpdater
import java.io.*
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine context marker present while a simulation build block (the body of `newSim { }` or a
 * reopen function) is running. Iteration triggered from within the build carries this marker and is
 * allowed to proceed without waiting on [Workspace.simulationBuildLock], which makes the lock
 * reentrant with respect to the building coroutine. Iteration coming from anywhere else (e.g. the
 * desktop run button or auto-run) does not carry the marker and still waits for the build to finish.
 */
internal object SimulationBuildContext : CoroutineContext.Element {
    override val key get() = Key
    object Key : CoroutineContext.Key<SimulationBuildContext>
}

/**
 * A collection of components which interact via couplings. Neural networks,
 * simulated environments, data-tables, plots and gauges are examples of
 * components in a Simbrain workspace. Essentially, an instance of a workspace
 * corresponds to a single simulation, that can be run with or without a
 * graphical view of it.
 *
 * For instructions on setting up serialization see [WorkspaceSerializer].
 *
 * @author Jeff Yoshimi
 * @author Matt Watson
 * @author Tim Shea
 */
class Workspace: CoroutineScope {

    @Transient
    private var job = SupervisorJob()

    @Transient
    override var coroutineContext = Dispatchers.Default + job

    @Transient
    private val _componentList = ArrayList<WorkspaceComponent>()
    val componentList: List<WorkspaceComponent> get() = Collections.unmodifiableList(_componentList)

    /**
     * Component factory should be used to create new workspace and gui
     * components.
     */
    @Transient
    val componentFactory = AbstractComponentFactory(this)

    /**
     * Flag to indicate workspace has been changed since last save.
     */
    @Transient
    private var workspaceChanged = false

    /**
     * Current workspace file.
     */
    @Transient
    var currentFile: File? = null

    var simulationId: String? = null

    /**
     * A persistent representation of the time (the updater's state is not persisted).
     */
    var savedTime = 0
        private set

    @Transient
    val events = WorkspaceEvents()

    @Transient
    lateinit var idManager: SimpleIdManager

    /**
     * Delay in milliseconds between update cycles. Used to artificially slow
     * down simulation (sometimes useful in teaching).
     */
    var updateDelay = 0

    @Transient
    val couplingManager = CouplingManager(this)

    @Transient
    val updater = WorkspaceUpdater(this)

    /**
     * Held while a simulation is being constructed (see [org.simbrain.custom_sims.NewSimulation.run]).
     * Update entry points (run / iterate) wait on this so that iteration cannot begin while the
     * network and other components are still being assembled, which would otherwise cause
     * concurrent modification errors (e.g. iterating a neuron's fan-in while synapses are still
     * being added).
     */
    @Transient
    val simulationBuildLock = Mutex()

    @Transient
    val infoDoc = DocViewer()

    init {
        initIdManager()
    }

    fun addWorkspaceComponent(component: WorkspaceComponent) {
        Logger.debug("adding component: $component")
        _componentList.add(component)
        component.workspace = this
        component.onWorkspaceAttached()
        component.setChangedSinceLastSave(false)
        setWorkspaceChanged(true)

        // If there is no custom name, use the id manager to produce a default name
        if (component.name.isEmpty()) {
            component.name = idManager.getAndIncrementId(component.javaClass)
        }
        events.componentAdded.fireAndBlock(component)
        component.events.attributeContainerRemoved.on(Dispatchers.Default) { attributeContainer: AttributeContainer? ->
            couplingManager.removeAttributeContainer(
                attributeContainer!!
            )
        }
        component.events.attributeContainerChanged.on(Dispatchers.Default) { attributeContainer ->
            couplingManager.events.attributeContainerChanged.fire(attributeContainer)
        }
    }

    fun removeWorkspaceComponent(component: WorkspaceComponent) {
        Logger.debug("removing component: $component")

        // Remove all couplings associated with this component
        // this.getCouplingManager().removeCouplings(component);
        _componentList.remove(component)
        setWorkspaceChanged(true)
        events.componentRemoved.fire(component)
    }

    /**
     * Should be called when updating is stopped.
     */
    fun updateStopped() {
        synchronized(componentList) {
            for (component in componentList) {
                component.doStopped()
            }
        }
    }

    /**
     * Suspends until any in-progress simulation build has finished, so that iteration never begins
     * on a partially constructed workspace.
     */
    private suspend fun awaitSimulationBuild() {
        // Iteration triggered from within the simulation build is part of the scripted, sequential
        // setup and is safe to proceed; re-acquiring the lock here would deadlock the build coroutine.
        if (currentCoroutineContext()[SimulationBuildContext.Key] != null) return
        simulationBuildLock.withLock { }
    }

    /**
     * Iterates all couplings on all components until halted by user.
     */
    fun run() {
        launch {
            awaitSimulationBuild()
            for (wc in componentList) {
                wc.start()
            }
            updater.run()
        }
    }

    /**
     * Stops iteration of all couplings on all components.
     */
    fun stop() {
        for (wc in componentList) {
            wc.stop()
        }
        updater.stop()
        updateStopped()
    }

    /**
     * Initiates asynchronous iteration of the system from Java or non-coroutine environments.
     * Internally launches a coroutine to perform the work in the background.
     *
     * Note: This method is intended for Java compatibility and does not block the calling thread.
     */
    fun iterateAsync(numIterations: Int) {
        launch {
            awaitSimulationBuild()
            for (wc in componentList) {
                wc.start()
            }
            updater.iterate(numIterations)
            stop()
        }
    }

    /**
     * Suspends while iterating the system for the given number of iterations.
     * This function must be called from a coroutine or another suspending function.
     * This is the preferred iteration method to use when working in Kotlin coroutine-based environments.
     */
    suspend fun iterateSuspend(numIterations: Int = 1) {
        awaitSimulationBuild()
        for (wc in componentList) {
            wc.start()
        }
        updater.iterate(numIterations)
        stop()
    }

    /**
     * Iterate until the provided predicate is true.
     */
    suspend fun iterateWhile(predicate: () -> Boolean) {
        awaitSimulationBuild()
        for (wc in componentList) {
            wc.start()
        }
        updater.iterateWhile(predicate)
        stop()
    }

    /**
     * Updates synchronously. Iterates and block, ensuring sequential execution.
     */
    fun simpleIterate() {
        updater.runBlocking()
    }

    /**
     * Updates synchronously for specified number of iterations.
     */
    fun simpleIterate(iterations: Int) {
        runBlocking {
            iterateSuspend(iterations)
        }
    }

    /**
     * Remove all components (networks, worlds, etc.) from this workspace.
     */
    fun clearWorkspace() {
        stop()
        removeAllComponents()
        resetTime()
        setWorkspaceChanged(false)
        currentFile = null
        simulationId = ""
        couplingManager.clear()
        events.workspaceCleared.fire()
        updater.updateManager.setDefaultUpdateActions()
        infoDoc.text = ""
    }

    /**
     * Disposes all Simbrain Windows.
     */
    fun removeAllComponents() {
        val toRemove: MutableList<WorkspaceComponent> = ArrayList()
        synchronized(componentList) {
            for (component in componentList) {
                toRemove.add(component)
            }
            for (component in toRemove) {
                removeWorkspaceComponent(component)
            }
        }
    }

    /**
     * Check whether there have been changes in the workspace or its
     * components.
     *
     * @return true if changes exist, false otherwise
     */
    fun changesExist(): Boolean {
        return if (workspaceChanged) {
            true
        } else {
            return componentList.any { it.hasChangedSinceLastSave() }
        }
    }

    /**
     * Sets whether the workspace has been changed.
     *
     * @param workspaceChanged Has workspace been changed value
     */
    fun setWorkspaceChanged(workspaceChanged: Boolean) {
        this.workspaceChanged = workspaceChanged
    }

    fun getComponent(name: String?): WorkspaceComponent? {
        return componentList.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    override fun toString(): String {
        val builder = StringBuilder(
            """
    Number of components: ${componentList.size}
    
    """.trimIndent()
        )
        var i = 0
        synchronized(componentList) {
            for (component in componentList) {
                builder.append(
                    """
    Component ${++i}:${component.name}
    
    """.trimIndent()
                )
            }
        }
        return builder.toString()
    }

    /**
     * Returns global time.
     */
    val time: Int
        get() = updater.time

    fun resetTime() {
        updater.resetTime()
    }

    /**
     * Actions required prior to proper serialization.
     */
    fun preSerializationInit() {
        /*
         * TODO: A bit of a hack. Currently just moves trainer components to the
         * back of the list, so they are serialized last, and hence deserialized
         * last.
         */
        Collections.sort(_componentList) { c1, c2 -> Integer.compare(c1.serializePriority, c2.serializePriority) }
        savedTime = time
    }

    suspend fun openWorkspace(theFile: File?, useDesktop: Boolean = false, progressCallback: ((Int, Int) -> Unit)? = null) {
        stop()
        val serializer = WorkspaceSerializer(this)
        try {
            if (theFile != null) {
                clearWorkspace()
                withContext(Dispatchers.IO) {
                    serializer.deserialize(FileInputStream(theFile), progressCallback)
                }
                currentFile = theFile
                simulations.items.firstNotNullOfOrNull { (_, sim) ->
                    (sim as? NewSimulation)?.let { newSim ->
                        if (newSim.id != null && newSim.id == simulationId) {
                            newSim
                        } else {
                            null
                        }
                    }
                }?.reopen(this, if (useDesktop) SimbrainDesktop else null)
                setWorkspaceChanged(false)
                events.workspaceOpened.fire()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmOverloads
    fun save(file: File?, headless: Boolean = false) {
        if (file != null) {
            try {
                val ostream = FileOutputStream(file)
                try {
                    val serializer = WorkspaceSerializer(this)
                    serializer.serialize(ostream, headless)
                    setWorkspaceChanged(false)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    ostream.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Returns a "flat" representation of the workspace as a byte array from the zipped representation
     * [WorkspaceSerializer] produces.
     */
    val zipData: ByteArray?
        get() = generateZipData(false)

    val zipDataHeadless: ByteArray?
        get() = generateZipData(true)

    fun generateZipData(headless: Boolean): ByteArray? {
        try {
            val serializer = WorkspaceSerializer(this)
            val bas = ByteArrayOutputStream()
            serializer.serialize(bas, headless)
            bas.close()
            return bas.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Open a workspace from the flat representation provided by [.getZipData] }.
     */
    suspend fun openFromZipData(zipData: ByteArray?) {
        try {
            clearWorkspace()
            val serializer = WorkspaceSerializer(this)
            withContext(Dispatchers.IO) {
                val bis = ByteArrayInputStream(zipData)
                serializer.deserialize(bis)
                bis.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Convenience method for adding an update action to the workspace's action
     * list (the sequence of actions invoked on each iteration of the
     * workspace).
     *
     * @param action new action
     */
    fun addUpdateAction(action: UpdateAction?) {
        updater.updateManager.addAction(action)
    }

    fun addUpdateAction(description: String, longDescription: String = description, position: Int = updater.updateManager.actionList.size, action: suspend () -> Unit) {
        updater.updateManager.addAction(updateAction(description, longDescription, action), position)
    }

    fun addNonRemovableAction(description: String, longDescription: String = description, action: suspend () -> Unit) {
        updater.updateManager.addNonRemovableAction(updateAction(description, longDescription, action))
    }

    /**
     * Convenience method which gets the couplings the coupling manager stores.
     */
    val couplings: List<Coupling>
        get() = couplingManager.couplings

    fun initIdManager() {
        idManager = SimpleIdManager(
            initIdFunction = { cls -> _componentList.count { comp -> comp.javaClass == cls } },
            baseNameGenerator = { cls -> cls.simpleName.removeSuffix("Component") },
            delimeter = " "
        )
    }

    /**
     * Check if the workspace has custom update actions that won't be restored on reopen.
     * Returns true if there are custom workspace-level or network-level update actions.
     */
    fun hasCustomUpdateActions(): Boolean {
        // Check workspace-level custom actions
        val hasWorkspaceCustomActions = updater.updateManager.actionList.any { action ->
            action !is UpdateAllAction && action !is UpdateComponent && action !is UpdateCoupling
        }
        
        if (hasWorkspaceCustomActions) {
            return true
        }
        
        // Check network-level custom actions
        val networkComponents = componentList.filterIsInstance<NetworkComponent>()
        return networkComponents.any { networkComponent ->
            networkComponent.network.updateManager.actionList.any { action ->
                action !is BufferedUpdate && action !is PriorityUpdate && action !is UpdateNetworkModel
            }
        }
    }

    /**
     * Check if the workspace has control panels that won't be restored on reopen.
     * Returns true if there are any ControlPanelKt instances in the desktop.
     */
    fun hasControlPanels(desktop: SimbrainDesktop?): Boolean {
        if (desktop == null) {
            return false
        }
        return desktop.desktopPane.allFrames.any { it is ControlPanelKt }
    }

    /**
     * Check if saving this workspace would be problematic (has features that won't restore).
     * Returns true if the workspace has custom update actions or control panels but no simulationId.
     */
    fun isSaveProblematic(desktop: SimbrainDesktop?): Boolean {
        return (hasCustomUpdateActions() || hasControlPanels(desktop)) && simulationId == null
    }
}