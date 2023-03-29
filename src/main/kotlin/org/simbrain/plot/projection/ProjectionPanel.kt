package org.simbrain.plot.projection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.simbrain.util.*
import org.simbrain.util.projection.IterableProjectionMethod2
import org.simbrain.util.projection.ProjectionMethod2
import org.simbrain.util.projection.Projector2
import org.simbrain.util.widgets.ToggleButton
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import kotlin.random.Random
import kotlin.reflect.full.primaryConstructor

class ProjectionPanel: JPanel(), CoroutineScope {
    val projector = Projector2(5)

    init {
        layout = BorderLayout()
        projector.events.downstairsChanged.on {
            update()
        }
        projector.events.methodChanged.on { o, n ->

            if (n is IterableProjectionMethod2) {
                northPanel.add(runToolbar)
                bottomPanel.add(errorLabel)
            } else {
                bottomPanel.remove(errorLabel)
                northPanel.remove(runToolbar)
            }
            northPanel.revalidate()
            northPanel.repaint()
            bottomPanel.revalidate()
            bottomPanel.repaint()
            launch { update() }
        }
        projector.events.iterated.on { error ->
            errorLabel.text = "Error: ${error.format(2)}"
        }

    }

    override var coroutineContext = projector.coroutineContext

    val randomizeAction = createAction(
        name = "Randomize",
        iconPath = "menu_icons/Rand.png"
    ) {
        projector.dataset.randomizeDownstairs()
        projector.events.downstairsChanged.fireAndForget()
    }

    var running = false

    val iterateAction = createAction(
        iconPath = "menu_icons/Step.png",
        name = "Iterate",
        description = "Iterate once"
    ) {
        iterate()
    }

    val runAction = createAction(
        iconPath = "menu_icons/Play.png",
        name = "Run",
        description = "Run"
    ) {
        if (!running) {
            running = true
            projector.events.beginTraining.fireAndSuspend()
            launch {
                while (running) {
                    iterate()
                }
            }
        }
    }

    val stopAction = createAction(
        iconPath = "menu_icons/Stop.png",
        name = "Stop",
        description = "Stop"
    ) {
        running = false
        projector.events.endTraining.fireAndSuspend()
    }

    val prefsAction = createAction(
        iconPath = "menu_icons/Prefs.png",
        name = "Preferences...",
        description = "Set projection preferences"
    ) {
        showPrefDialog()
    }

    fun showPrefDialog() {
        projector.createDialog {
            it.project()
            launch { update() }
        }.display()
    }

    private suspend fun iterate() {
        projector.projectionMethod.let { projection ->
            if (projection is IterableProjectionMethod2) {
                projection.iterate(projector.dataset)
                projector.events.iterated.fireAndSuspend(projection.error)
            }
        }
        projector.events.downstairsChanged.fireAndSuspend()
    }

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

    val projectionMethods = ProjectionMethod2.getTypes()
        .associateWith { it.kotlin.primaryConstructor!!.call() }
    val projectionSelector = JComboBox<ProjectionMethod2>().apply {
        maximumSize = Dimension(200, 100)
        projectionMethods.values.forEach {
            addItem(it)
        }.also {
            addActionListener {
                projector.projectionMethod = (selectedItem as ProjectionMethod2)
            }
        }
    }

    private val chartPanel = ChartPanel(chart).also {
        add("Center", it)
    }

    val pointsLabel = JLabel()
    val dimensionsLabel = JLabel()
    val errorLabel = JLabel("Error: ---")
    val bottomPanel = JPanel().apply {
        layout = FlowLayout(FlowLayout.LEFT)
        add(pointsLabel)
        add(Box.createHorizontalStrut(25));
        add(dimensionsLabel)
        add(Box.createHorizontalStrut(25));
    }.also {
        add("South", it)
    }

    private val northPanel = JPanel(FlowLayout(FlowLayout.LEFT)).also{
        add("North", it)
    }

    private val toolbar = JToolBar().apply {
        add(projectionSelector)
        addSeparator()
        add(prefsAction)
        add(randomizeAction)
    }.also {
        northPanel.add(it)
    }

    private val runToolbar = JToolBar().apply {
        add(ToggleButton(listOf(stopAction, runAction)).apply{
            setAction("Run")
            projector.events.beginTraining.on {
                setAction("Stop")
            }
            projector.events.endTraining.on {
                setAction("Run")
            }
        })
        add(iterateAction)
    }


    suspend fun update() {
        withContext(Dispatchers.Swing) {
            xyCollection.getSeries(0).clear()
            projector.dataset.kdTree.forEach {
                val (x, y) = it.downstairsPoint
                xyCollection.getSeries(0).add(x, y)
            }
            pointsLabel.text = "Datapoints: ${projector.dataset.kdTree.size}"
            dimensionsLabel.text = "Dimensions: ${projector.dimension}"
        }
    }

}

suspend fun main() {
    val random = Random(1)
    StandardDialog().apply {
        val projectionPanel = ProjectionPanel().apply {
            // repeat(100) {
            //     projector.addDataPoint(DoubleArray(5) { random.nextDouble() })
            // }
            // projector.projectionMethod = SammonProjection2(projector.dimension).apply {
            //     epsilon = 100.0
            // }
            (0 until 40).forEach { p ->
                projector.addDataPoint(DoubleArray(100) { p.toDouble() })
            }
            projector.dataset.randomizeDownstairs()
            projector.project()
            update()
        }.also { contentPane = it }

        jMenuBar = JMenuBar().apply {
            add(JMenu("Edit").apply {
                add(JMenuItem("Preferences...").apply {
                    addActionListener {
                        projectionPanel.showPrefDialog()
                    }
                })
            })
        }

        makeVisible()
    }
}