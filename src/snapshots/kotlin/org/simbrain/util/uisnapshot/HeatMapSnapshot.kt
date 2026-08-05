package org.simbrain.util.uisnapshot

import org.simbrain.plot.ChartColorMap
import org.simbrain.plot.heatmap.HeatMapModel
import org.simbrain.plot.heatmap.HeatMapPanel
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.exp
import kotlin.math.sin

/**
 * The heat map plot under each of its color maps, fed a synthetic population of travelling bumps so
 * the block grid, the inverted row axis, and the colorbar can all be inspected against light and dark.
 */
class HeatMapSnapshot : UiSnapshotDef {
    override val name = "heat_map"

    private fun populate(model: HeatMapModel, map: ChartColorMap) {
        model.colorMap = map
        model.fixedWidth = false
        var step = 0
        model.timeSupplier = { step }
        repeat(120) {
            step = it
            model.setValues(DoubleArray(16) { row ->
                val center = 8.0 + 6.0 * sin(it / 18.0)
                exp(-((row - center) * (row - center)) / 6.0) + 0.15 * sin(row + it / 4.0)
            })
        }
    }

    override fun build(): Component {
        lateinit var panel: JPanel
        SwingUtilities.invokeAndWait {
            panel = JPanel(GridLayout(2, 2, 8, 8)).apply {
                preferredSize = Dimension(940, 700)
                ChartColorMap.entries.forEach { map ->
                    val model = HeatMapModel()
                    populate(model, map)
                    add(HeatMapPanel(model))
                }
            }
        }
        return panel
    }
}
