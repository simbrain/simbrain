package org.simbrain

import org.simbrain.workspace.Workspace
import org.simbrain.workspace.updater.WorkspaceUpdaterListener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun Workspace.iterateAndRun(block: () -> Unit): Unit =
        suspendCoroutine { cont ->
            this.updater.addUpdaterListener(object : WorkspaceUpdaterListener {

                override fun updatingFinished() {
                    try {
                        block()
                        cont.resume(Unit)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        cont.resumeWithException(e)
                    }
                }

                override fun updatedCouplings(update: Int) {}
                override fun workspaceUpdated() {}
                override fun changedUpdateController() {}
                override fun changeNumThreads() {}
                override fun updatingStarted() {}

            })
            iterate()
        }