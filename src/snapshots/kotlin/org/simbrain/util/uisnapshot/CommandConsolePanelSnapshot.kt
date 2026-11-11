package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.console.CommandConsolePanel
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.workspace.Workspace
import java.awt.Component
import java.awt.Dimension
import javax.swing.SwingUtilities

class CommandConsolePanelSnapshot : UiSnapshotDef {
    override val name = "command_console_panel"

    override fun build(): Component {
        val workspace = Workspace()
        val component = NetworkComponent("net1")
        val source = Neuron().apply { activation = 0.75 }
        val target = Neuron()
        runBlocking { component.network.addNetworkModels(source, target, Synapse(source, target)) }
        workspace.addWorkspaceComponent(component)

        lateinit var panel: CommandConsolePanel
        SwingUtilities.invokeAndWait {
            panel = CommandConsolePanel(workspace).apply { preferredSize = Dimension(640, 420) }
        }
        runBlocking {
            listOf("ls net1", "cd net1", "add neurons 3 rule=Sigmoidal", "set Neuron_3 activation=0.5", "ls where activation>0.4", "get Neuron_* activation", "show Neuron_3 | grep rule", "help connect", "run 10").forEach { panel.submit(it).join() }
        }
        return panel
    }
}
