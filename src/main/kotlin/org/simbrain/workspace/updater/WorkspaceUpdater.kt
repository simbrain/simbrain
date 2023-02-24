package org.simbrain.workspace.updater

import kotlinx.coroutines.*
import org.pmw.tinylog.Logger
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.events.WorkspaceUpdaterEvents

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

    val events = WorkspaceUpdaterEvents()

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
        events.runStarted.fireAndForget()
        withContext(workspace.coroutineContext) {
            while (isRunning) {
                doUpdate()
            }
        }
        isRunning = false
        for (component in workspace.componentList) {
            component.isRunning = false
        }
        events.runFinished.fireAndForget()
    }

    /**
     * Submits a single task to the queue.
     */
    suspend fun runOnce() {
        isRunning = true
        for (wc in workspace.componentList) {
            wc.isRunning = true
        }
        events.runStarted.fireAndForget()
        withContext(workspace.coroutineContext) {
            doUpdate()
        }
        events.runFinished.fireAndForget()
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
        runBlocking {
            events.runStarted.fireAndForget()
            doUpdate()
            events.runFinished.fireAndForget()
        }
        isRunning = false
        for (component in workspace.componentList) {
            component.isRunning = false
        }
    }

    /**
     * Iterate a set number of iterations.
     *
     * Optional finishing task is run after main iteration finishes.
     *
     * See [Workspace.iterate]
     *
     * @param numIterations the number of iterations to update
     */
    suspend fun iterate(numIterations: Int, finishingTask: () -> Unit = {}) {
        isRunning = true
        for (wc in workspace.componentList) {
            wc.isRunning = true
        }
        events.runStarted.fireAndForget()
        repeat(numIterations) {
            doUpdate()
        }
        isRunning = false
        finishingTask()
        for (component in workspace.componentList) {
            component.isRunning = false
        }
        events.runFinished.fireAndForget()
    }

    suspend fun iterateWhile(predicate: () -> Boolean) {
        isRunning = true
        for (wc in workspace.componentList) {
            wc.isRunning = true
        }
        events.runStarted.fireAndSuspend()
        do {
            doUpdate()
        } while (predicate())
        isRunning = false
        for (component in workspace.componentList) {
            component.isRunning = false
        }
        events.runFinished.fireAndSuspend()
    }

    /**
     * Executes the main workspace update.
     */
    private suspend fun doUpdate() {
        time++
        Logger.trace("starting: $time")
        withContext(workspace.coroutineContext) {
            for (action in updateManager.actionList + updateManager.nonRemovableActions) {
                with(PerformanceMonitor) {
                    action()
                }
            }
        }
        events.workspaceUpdated.fireAndForget()
        Logger.trace("done: $time")
    }

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
