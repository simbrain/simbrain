package org.simbrain.workspace.updater

import org.simbrain.workspace.couplings.Coupling

/**
 * Updates a coupling.
 *
 * @author jyoshimi
 */
class UpdateCoupling(@field:Transient val coupling: Coupling) : UpdateAction("Update coupling (${coupling.producer}>${coupling.consumer})") {
    override suspend operator fun invoke() {
        coupling.update()
    }
}