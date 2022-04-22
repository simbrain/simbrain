package org.simbrain.workspace.updater

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.pmw.tinylog.Logger
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.function.Consumer

/**
 * This class manages workspace updates. "Running" and "Stepping" the simulation
 * happen here, in a way that allows for concurrent update (in some cases) and
 * also interacts properly with single threaded guis using a "task
 * synchronization manager". Notification events about workspace events are
 * fired from here. Every time the workspace is updated, a list of actions in
 * the UpdateActionManager is invoked. By default one single action, a "buffered
 * update", occurs, in which components are updated in parallel, and when they
 * have all finished updating, couplings are updated. The update action manager
 * can also be used to customize update. Three executor services are here, one
 * for workspace updates (a single thread), one for event notification updates
 * (a single thread), and one for component updates (a thread pool with multiple
 * threads that can be configured), for cases when component updating happens
 * concurrently.
 *
 * @author Matt Watson
 * @author Jeff Yoshimi
 */
class WorkspaceUpdater(val workspace: Workspace) {

    /**
     * The executor service for notifying listeners.
     */
    private val notificationEvents = Executors.newSingleThreadExecutor()

    /**
     * Updater listeners.
     */
    private val updaterListeners: MutableList<WorkspaceUpdaterListener> = CopyOnWriteArrayList()

    /**
     * Returns whether the updater is set to run.
     */
    /**
     * Whether updates should continue to run.
     */
    var isRunning = false
        private set

    /**
     * The number of times the update has run.
     */
    var time = 0

    /**
     * The update Manager.
     */
    val updateManager: UpdateActionManager = UpdateActionManager(this)

    /**
     * Reset time to 0.
     */
    fun resetTime() {
        time = 0
    }

    /**
     * Stops the update thread.
     */
    fun stop() {
        isRunning = false
    }

    /**
     * Starts the update thread. Used when "running" the workspace by pressing
     * the play button in the gui.
     */
    suspend fun run() = coroutineScope {
        isRunning = true
        for (wc in workspace.componentList) {
            wc.isRunning = true
        }
        notifyWorkspaceUpdateStarted()
        withContext(Dispatchers.Swing) {
            while (isRunning) {
                doUpdate()
            }
        }
        isRunning = false
        for (component in workspace.componentList) {
            component.isRunning = false
        }
        notifyWorkspaceUpdateCompleted()
    }

    /**
     * Submits a single task to the queue.
     */
    suspend fun runOnce() {
        isRunning = true
        for (wc in workspace.componentList) {
            wc.isRunning = true
        }
        notifyWorkspaceUpdateStarted()
        withContext(Dispatchers.Swing) {
            doUpdate()
        }
        notifyWorkspaceUpdateCompleted()
        isRunning = false
        for (component in workspace.componentList) {
            component.isRunning = false
        }
    }

    fun runBlocking() {
        isRunning = true
        for (wc in workspace.componentList) {
            wc.isRunning = true
        }
        notifyWorkspaceUpdateStarted()
        runBlocking {
            doUpdate()
        }
        notifyWorkspaceUpdateCompleted()
        isRunning = false
        for (component in workspace.componentList) {
            component.isRunning = false
        }
    }

    /**
     * Iterate a set number of iterations.
     *
     *
     * See [Workspace.iterate]
     *
     * @param numIterations the number of iterations to update
     */
    suspend fun iterate(numIterations: Int) {
        isRunning = true
        for (wc in workspace.componentList) {
            wc.isRunning = true
        }
        notifyWorkspaceUpdateStarted()
        withContext(Dispatchers.Swing) {
            repeat(numIterations) {
                doUpdate()
            }
        }
        isRunning = false
        for (component in workspace.componentList) {
            component.isRunning = false
        }
        notifyWorkspaceUpdateCompleted()
    }

    /**
     * Executes the main workspace update.
     */
    private suspend fun doUpdate() {
        time++
        Logger.trace("starting: $time")
        for (action in updateManager.actionList + updateManager.nonRemovableActions) {
            withContext(Dispatchers.Default) {
                with(PerformanceMonitor) {
                    action()
                }
            }
        }
        notifyWorkspaceUpdated()
        Logger.trace("done: $time")
    }

    /**
     * Adds an updater listener to this instance.
     *
     * @param listener updater component listener to add.
     */
    fun addUpdaterListener(listener: WorkspaceUpdaterListener) {
        updaterListeners.add(listener)
    }

    /**
     * Removes an updater listener from this instance.
     *
     * @param listener The updater listener to add.
     */
    fun removeUpdaterListener(listener: WorkspaceUpdaterListener) {
        updaterListeners.remove(listener)
    }

    /**
     * Called when the couplings are updated.
     */
    fun notifyCouplingsUpdated() {
        val time = time
        val notifier = Consumer { l: WorkspaceUpdaterListener -> l.updatedCouplings(time) }
        notificationEvents.submit { updaterListeners.forEach(notifier) }
    }

    /**
     * Called when the workspace update begins.
     */
    private fun notifyWorkspaceUpdateStarted() {
        notificationEvents.submit { updaterListeners.forEach(Consumer { obj: WorkspaceUpdaterListener -> obj.updatingStarted() }) }
    }

    /**
     * Called when workspace update finishes.
     */
    private fun notifyWorkspaceUpdateCompleted() {
        notificationEvents.submit { updaterListeners.forEach(Consumer { obj: WorkspaceUpdaterListener -> obj.updatingFinished() }) }
    }

    /**
     * Called after every workspace update .
     */
    private fun notifyWorkspaceUpdated() {
        updaterListeners.forEach { it.workspaceUpdated() }
        // notificationEvents.submit { updaterListeners.forEach(Consumer { obj: WorkspaceUpdaterListener -> obj.workspaceUpdated() }) }
    }

    /**
     * Constructor for the updater that uses the provided controller and
     * threads.
     *
     * @param workspace The parent workspace.
     * @param numThreads   The number of threads for component updates.
     */
    /**
     * Constructor for the updater that uses the default controller and default
     * number of threads.
     *
     * @param workspace The parent workspace.
     */
    init {
        // A single thread updates the workspace
        // A single thread to fire notification events
        // Instantiate the update action manager
    }

    /**
     * Get a synchronized list of component.
     *
     * @return the synchronized list of components
     */
    val components: List<WorkspaceComponent>
        get() {
            var components = workspace.componentList
            synchronized(components) { components = ArrayList(components) }
            return components
        }

}
