package org.simbrain.plot.projection

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.simbrain.util.StandardDialog
import org.simbrain.util.createDialog
import org.simbrain.util.display
import org.simbrain.util.projection.Projector2
import java.awt.Color
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JPanel
import kotlin.random.Random

class ProjectionPanel: JPanel() {

    val projector = Projector2(5)

    /**
     * JChart representation of the data.
     */
    private val xyCollection: XYSeriesCollection= XYSeriesCollection().apply {
        addSeries(XYSeries("Data", false, true))
    }

    /**
     * The JFreeChart chart.
     */
    private val chart: JFreeChart = ChartFactory.createScatterPlot(
        "", "Projection X", "Projection Y",
        xyCollection, PlotOrientation.VERTICAL, false, true, false
    ).apply {
        xyPlot.backgroundPaint = Color.white
        xyPlot.domainGridlinePaint = Color.gray
        xyPlot.rangeGridlinePaint = Color.gray
        xyPlot.domainAxis.isAutoRange = true
        xyPlot.rangeAxis.isAutoRange = true
        xyPlot.foregroundAlpha = .5f // TODO: Make this settable
    }

    private val chartPanel = ChartPanel(chart).also {
        add("Center", it)
    }

    fun update() {
        xyCollection.getSeries(0).clear()
        projector.dataset.kdTree.forEach {
            val (x, y) = it.downstairsPoint
            xyCollection.getSeries(0).add(x, y)
        }
    }

}


fun main() {
    val random = Random(1)
    StandardDialog().apply {
        val projectionPanel = ProjectionPanel().apply {
            repeat(100) {
                projector.addDataPoint(DoubleArray(5) { random.nextDouble() })
            }
            projector.project()
            update()
        }.also { contentPane = it }

        jMenuBar = JMenuBar().apply {
            add(JMenu("Edit").apply {
                add(JMenuItem("Preferences...").apply {
                    addActionListener {
                        projectionPanel.projector.createDialog {
                            it.project()
                            projectionPanel.update()
                        }.display()
                    }
                })
            })
        }

        makeVisible()
    }
}