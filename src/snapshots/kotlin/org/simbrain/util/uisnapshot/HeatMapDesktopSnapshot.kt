package org.simbrain.util.uisnapshot

import org.simbrain.plot.heatmap.HeatMapComponent
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.workspace.Workspace
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDesktopPane
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.math.exp
import kotlin.math.sin

/**
 * The heat map as it is actually assembled at runtime — built through the component factory and wrapped
 * in a desktop frame — so the registration wiring and the menu bar are exercised, not just the panel.
 */
class HeatMapDesktopSnapshot : UiSnapshotDef {
    override val name = "heat_map_desktop"

    override fun build(): Component {
        val workspace = Workspace()
        workspace.componentFactory.createWorkspaceComponent("Heat map")
        val component = workspace.componentList.filterIsInstance<HeatMapComponent>().single()
        var step = 0
        component.model.timeSupplier = { step }
        component.model.fixedWidth = false
        repeat(90) {
            step = it
            component.model.setValues(DoubleArray(12) { row ->
                val center = 6.0 + 4.0 * sin(it / 14.0)
                exp(-((row - center) * (row - center)) / 5.0)
            })
        }
        lateinit var host: JFrame
        SwingUtilities.invokeAndWait {
            val frame = GenericJInternalFrame("Heat map", true, true, true, true)
            frame.contentPane.add(workspace.componentFactory.createGuiComponent(frame, component))
            frame.setBounds(16, 16, 600, 460)
            frame.isVisible = true
            val desktop = JDesktopPane().apply { preferredSize = Dimension(632, 492) }
            desktop.add(frame)
            host = JFrame().apply { contentPane = desktop }
            host.pack()
        }
        return host
    }
}
