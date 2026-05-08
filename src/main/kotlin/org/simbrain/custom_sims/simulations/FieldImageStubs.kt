package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim

private val fieldImagePlaceholder = newSim {
    workspace.clearWorkspace()
    addSidebarInfo("# Coming soon\n\nThis simulation has not been implemented yet.")
}

val fieldImageDemo = fieldImagePlaceholder
val fieldImagePlaceCellsDemo = fieldImagePlaceholder
