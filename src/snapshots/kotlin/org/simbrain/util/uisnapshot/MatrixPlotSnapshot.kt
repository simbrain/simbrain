/**
 * Corr plots at several viewport sizes: the built-in Hopfield patterns in a small and a large viewport, to check
 * that cells scale to fill and labels are not clipped, a matrix with many long labels that must scroll, and
 * trained-pattern labels with pattern thumbnails.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.simulations.hebb.TrainedPatterns
import org.simbrain.custom_sims.simulations.hebb.builtInPatterns
import org.simbrain.util.computeCorrelationMatrix
import org.simbrain.util.toSimbrainColorImage
import org.simbrain.util.widgets.MatrixPlot
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import kotlin.random.Random

class MatrixPlotSnapshot : UiSnapshotDef {
    override val name = "matrix_plot"

    private fun plotPane(labels: List<String>, data: Array<DoubleArray>, size: Int, withThumbnails: Boolean = false) =
        JScrollPane(MatrixPlot(
            labels,
            runBlocking { computeCorrelationMatrix(data) },
            if (withThumbnails) data.map { it.toSimbrainColorImage(10, 10) } else null
        )).apply {
            border = null
            preferredSize = Dimension(size, size)
        }

    override fun build(): Component {
        lateinit var panel: JPanel
        SwingUtilities.invokeAndWait {
            val patterns = builtInPatterns(100)
            val random = Random(1)
            val manyLabels = List(20) { "feature-number-$it" }
            val manyRows = Array(manyLabels.size) { DoubleArray(30) { random.nextDouble() } }
            panel = JPanel(FlowLayout(FlowLayout.LEFT, 12, 12)).apply {
                add(plotPane(patterns.keys.toList(), patterns.values.toTypedArray(), 260))
                add(plotPane(patterns.keys.toList(), patterns.values.toTypedArray(), 520))
                add(plotPane(manyLabels, manyRows, 420))
                val trained = TrainedPatterns(patterns, 0.5)
                val noisyCircle = patterns.getValue("Circle").copyOf().apply { this[0] = 1.0; this[1] = 1.0 }
                listOf(patterns.getValue("Circle"), patterns.getValue("Circle"), noisyCircle, patterns.getValue("Cross"),
                    DoubleArray(100) { if (it % 2 == 0) 1.0 else 0.0 }).forEach { trained.record(it) }
                add(plotPane(
                    trained.labels,
                    arrayOf(patterns.getValue("Circle"), noisyCircle, patterns.getValue("Cross"),
                        DoubleArray(100) { if (it % 2 == 0) 1.0 else 0.0 }),
                    420,
                    withThumbnails = true
                ))
                add(plotPane(
                    patterns.keys.toList(),
                    patterns.values.toTypedArray(),
                    260,
                    withThumbnails = true
                ))
                preferredSize = Dimension(1300, 1020)
            }
        }
        return panel
    }
}
