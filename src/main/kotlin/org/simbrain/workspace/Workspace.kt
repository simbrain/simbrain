package org.simbrain.workspace

import kotlinx.coroutines.*
import org.pmw.tinylog.Logger
import org.simbrain.custom_sims.NewSimulation
import org.simbrain.custom_sims.simulations
import org.simbrain.docviewer.DocViewer
import org.simbrain.util.SimpleIdManager
import org.simbrain.util.Utils
import org.simbrain.workspace.couplings.Coupling
import org.simbrain.workspace.couplings.CouplingManager
import org.simbrain.workspace.events.WorkspaceEvents
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.serialization.WorkspaceSerializer
import org.simbrain.workspace.updater.UpdateAction
import org.simbrain.workspace.updater.WorkspaceUpdater
import org.simbrain.workspace.updater.updateAction
import java.io.*
import java.util.*

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

    @Transient
    val infoDoc = DocViewer()

    init {
        initIdManager()
    }

    fun addWorkspaceComponent(component: WorkspaceComponent) {
        Logger.debug("adding component: $component")
        _componentList.add(component)
        component.workspace = this
        component.setChangedSinceLastSave(false)
        setWorkspaceChanged(true)

        // If there is no custom name, use the id manager to produce a default name
        if (component.name.isEmpty()) {
            component.name = idManager.getAndIncrementId(component.javaClass)
        }
        events.componentAdded.fireAndBlock(component)
        component.events.attributeContainerRemoved.on { attributeContainer: AttributeContainer? ->
            couplingManager.removeAttributeContainer(
                attributeContainer!!
            )
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
     * Iterates all couplings on all components until halted by user.
     */
    fun run() {
        for (wc in componentList) {
            wc.start()
        }
        launch {
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
        for (wc in componentList) {
            wc.start()
        }
        launch {
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

    var currentDirectory = Utils.USER_DIR + Utils.FS + "simulations" + Utils.FS + "workspaces"


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

    suspend fun openWorkspace(theFile: File?, useDesktop: Boolean = false) {
        stop()
        val serializer = WorkspaceSerializer(this)
        try {
            if (theFile != null) {
                clearWorkspace()
                withContext(Dispatchers.IO) {
                    serializer.deserialize(FileInputStream(theFile))
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
}