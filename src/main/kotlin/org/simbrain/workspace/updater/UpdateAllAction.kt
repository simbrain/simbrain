package org.simbrain.workspace.updater

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.pmw.tinylog.Logger

/**
 * This is the default action for all workspace updates.
 * First update couplings then update all the components.
 *
 * @author jyoshimi
 */
class UpdateAllAction(val updater: WorkspaceUpdater) : UpdateAction(description = "Update All Components and Couplings") {

    override suspend operator fun invoke(): Unit = coroutineScope {
        val components = updater.components
        updateCouplings()
        components.map { async { it.update() } }.awaitAll()
    }

    /**
     * Update couplings.
     */
    suspend fun updateCouplings() {
        updater.workspace.couplingManager.updateCouplings()
        Logger.trace("couplings updated")
        updater.notifyCouplingsUpdated()
    }

}