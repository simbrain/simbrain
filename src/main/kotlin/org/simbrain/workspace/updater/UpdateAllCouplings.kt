package org.simbrain.workspace.updater

import kotlinx.coroutines.coroutineScope
import org.pmw.tinylog.Logger
import org.simbrain.util.UpdateAction

/**
 * This is the default action for all workspace updates.
 * First update couplings then update all the components.
 *
 * @author jyoshimi
 */
class UpdateAllCouplings(@Transient val updater: WorkspaceUpdater) : UpdateAction(description = "Update All Couplings") {

    override suspend fun run(): Unit = coroutineScope {
        updater.workspace.couplingManager.updateCouplings()
        Logger.trace("couplings updated")
        updater.events.couplingsUpdates.fire()
    }

}