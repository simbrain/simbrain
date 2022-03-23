package org.simbrain.workspace

import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.workspace.gui.SimbrainDesktop

class UpdaterTest {

    @Test
    fun `ensure gui components are ready for placement after component creation`() {
        val hiSim = newSim {
            val component = NetworkComponent("net1")
            workspace.addWorkspaceComponent(component)
            withGui {
                println("bleh")
                place(component) {
                    location = point(20, 20)
                    width = 300
                    height = 300
                }
            }
        }
        val desktop = SimbrainDesktop(Workspace())
        desktop.frame.isVisible = true
        hiSim.run(desktop)
    }

}