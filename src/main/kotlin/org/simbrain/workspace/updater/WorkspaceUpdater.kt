package org.simbrain.workspace.updater

import kotlinx.coroutines.*
import org.pmw.tinylog.Logger
import java.util.concurrent.ConcurrentHashMap
import javax.swing.SwingUtilities
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
 * To make a new custom UpdateAction in kotlin use the updateAction() function.
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
     * The jobs of every iteration currently inside [doUpdate] — entry points can overlap, e.g. a
     * running loop plus a GUI-triggered single iterate — so a stop can escalate to cancelling them
     * when a suspend attribute or update action hangs instead of coming back to the loop's
     * [isRunning] check.
     */
    private val activeIterationJobs: MutableSet<Job> = ConcurrentHashMap.newKeySet()

    /**
     * Whether some iteration is inside [doUpdate] right now. With [isRunning] already false, this is
     * the signal a stop request did not come back — the GUI stop action escalates to [stopNow] on a
     * repeated press in that state.
     */
    val hasActiveIteration: Boolean
        get() = activeIterationJobs.isNotEmpty()

    /**
     * Reset time to 0.
     */
    fun resetTime() {
        time = 0
    }

    /**
     * Requests a cooperative stop: the current iteration finishes and the loop exits, which keeps the
     * step deterministic. Safe to call redundantly — programmatic callers do — so escalation is not
     * automatic here; a caller that knows the user wants out, such as the stop button on a repeated
     * press, uses [stopNow].
     */
    fun stop() {
        isRunning = false
    }

    /**
     * Stops and also cancels the in-flight iterations cooperatively, abandoning whatever step was in
     * progress. Suspend attributes are cancelled at their suspension points; blocking work must watch
     * its own abort flag.
     */
    fun stopNow() {
        isRunning = false
        activeIterationJobs.toList().forEach { it.cancel() }
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
        events.runStarted.fire()
        try {
            withContext(workspace.coroutineContext) {
                while (isRunning) {
                    doUpdate()
                }
            }
        } catch (e: CancellationException) {
            // Rethrows when this caller itself was cancelled; a cancellation from [stopNow] hitting the
            // in-flight iteration instead ends the run normally
            currentCoroutineContext().ensureActive()
        } finally {
            isRunning = false
            for (component in workspace.componentList) {
                component.isRunning = false
            }
            events.runFinished.fire()
        }
    }

    /**
     * Submits a single task to the queue.
     */
    suspend fun runOnce() {
        isRunning = true
        for (wc in workspace.componentList) {
            wc.isRunning = true
        }
        events.runStarted.fire()
        try {
            withContext(workspace.coroutineContext) {
                doUpdate()
            }
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
        } finally {
            events.runFinished.fire()
            isRunning = false
            for (component in workspace.componentList) {
                component.isRunning = false
            }
        }
    }

    fun runBlocking() {
        assertNotOnEventThread()
        isRunning = true
        for (wc in workspace.componentList) {
            wc.isRunning = true
        }
        runBlocking {
            events.runStarted.fire()
            try {
                doUpdate()
            } catch (e: CancellationException) {
                coroutineContext.ensureActive()
            }
            events.runFinished.fire()
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
     * See [Workspace.iterateAsync]
     *
     * @param numIterations the number of iterations to update
     */
    suspend fun iterate(numIterations: Int, finishingTask: () -> Unit = {}) {
        isRunning = true
        for (wc in workspace.componentList) {
            wc.isRunning = true
        }
        events.runStarted.fire()
        try {
            repeat(numIterations) {
                doUpdate()
            }
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
        } finally {
            isRunning = false
            finishingTask()
            for (component in workspace.componentList) {
                component.isRunning = false
            }
            events.runFinished.fire()
        }
    }

    suspend fun iterateWhile(predicate: () -> Boolean) {
        isRunning = true
        for (wc in workspace.componentList) {
            wc.isRunning = true
        }
        events.runStarted.fireAsync()
        try {
            do {
                doUpdate()
            } while (predicate())
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
        } finally {
            isRunning = false
            for (component in workspace.componentList) {
                component.isRunning = false
            }
            events.runFinished.fireAsync()
        }
    }

    /**
     * Executes the main workspace update. The iteration's job is exposed to [stopNow] for the duration,
     * so a stuck iteration can be cancelled from outside.
     */
    private suspend fun doUpdate() {
        time++
        Logger.trace("starting: $time")
        withContext(workspace.coroutineContext) {
            val iterationJob = coroutineContext.job
            activeIterationJobs.add(iterationJob)
            try {
                for (action in updateManager.actionList + updateManager.nonRemovableActions) {
                    with(PerformanceMonitor) {
                        action()
                    }
                }
            } finally {
                activeIterationJobs.remove(iterationJob)
            }
        }
        events.workspaceUpdated.fire()
        Logger.trace("done: $time")
    }

    private fun assertNotOnEventThread() {
        check(!SwingUtilities.isEventDispatchThread()) {
            "Blocking workspace iteration must not run on the event dispatch thread: suspend " +
                    "consumables wait for event thread handlers, which deadlocks against a blocked " +
                    "event thread. Use a launched iteration (e.g. Workspace.iterateAsync) instead."
        }
    }

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
