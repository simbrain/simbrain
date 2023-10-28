package org.simbrain.plot.projection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.labels.CustomXYToolTipGenerator
import org.jfree.chart.labels.StandardXYItemLabelGenerator
import org.jfree.chart.labels.XYItemLabelGenerator
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.xy.XYDataset
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.simbrain.util.*
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.projection.*
import org.simbrain.util.widgets.ToggleButton
import org.simbrain.workspace.gui.DesktopComponent
import java.awt.*
import java.awt.geom.Ellipse2D
import javax.swing.*
import kotlin.reflect.full.primaryConstructor

class ProjectionDesktopComponent(frame: GenericFrame, component: ProjectionComponent)
    : DesktopComponent<ProjectionComponent>(frame, component), CoroutineScope {

    val projector = component.projector
    override var coroutineContext = projector.coroutineContext

    var running = false

    /**
     * Ordered list of [DataPoint] points so that the renderer can access points by index.
     */
    val pointList = ArrayList<DataPoint>()

    // Actions
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
        description = "Run",
        coroutineScope = projector
    ) {
        if (!running) {
            running = true
            projector.events.startIterating.fire()
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
        projector.events.stopIterating.fire()
    }

    val prefsAction = createAction(
        iconPath = "menu_icons/Prefs.png",
        name = "Preferences...",
        description = "Set projection preferences"
    ) {
        showPrefDialog()
    }

    val randomizeAction = createAction(
        name = "Randomize",
        description = "Randomize points",
        iconPath = "menu_icons/Rand.png"
    ) {
        projector.dataset.randomizeDownstairs()
        projector.events.datasetChanged.fireAndForget()
    }

    val clearDataAction = createAction(
        name = "Clear",
        description = "Clear all points",
        iconPath = "menu_icons/Eraser.png"
    ) {
        synchronized(projector.dataset) {
            projector.dataset.kdTree.clear()
            projector.events.datasetChanged.fireAndForget()
            projector.events.datasetCleared.fireAndForget()
        }
    }

    // Top stuff
    val projectionMethods = projectionTypes
        .associateWith { it.kotlin.primaryConstructor!!.call() }
    val projectionSelector = JComboBox<ProjectionMethod>().apply {
        maximumSize = Dimension(200, 100)
        projectionMethods.values.forEach {
            addItem(it)
        }.also {
            addActionListener {
                projector.projectionMethod = (selectedItem as ProjectionMethod)
            }
        }
        selectedItem = projectionMethods[projector.projectionMethod.javaClass]
    }
    val mainToolbar = JToolBar().apply {
        add(projectionSelector)
        addSeparator()
        add(prefsAction)
        add(randomizeAction)
        add(clearDataAction)
    }
    private val runToolbar = JToolBar().apply {
        add(ToggleButton(listOf(stopAction, runAction)).apply {
            setAction("Run")
            projector.events.startIterating.on {
                setAction("Stop")
            }
            projector.events.stopIterating.on {
                setAction("Run")
            }
        })
        add(iterateAction)
    }

    val topPanel = JPanel(FlowLayout(FlowLayout.LEFT)).also {
        it.add(mainToolbar)
    }

    // Central Chart Panel
    private suspend fun iterate() {
        projector.projectionMethod.let { projection ->
            if (projection is IterableProjectionMethod) {
                projection.iterate(projector.dataset)
                projector.events.iterated.fire(projection.error)
            }
        }
        projector.events.datasetChanged.fire()
    }

    /**
     * JChart representation of the data.
     */
    private val xyCollection: XYSeriesCollection = XYSeriesCollection().apply {
        addSeries(XYSeries("Data", false, true))
    }

    private val renderer = CustomRenderer(this).apply {
        setSeriesLinesVisible(0, projector.connectPoints)
        setSeriesShape(0, Ellipse2D.Double(-7.0, -7.0, 7.0, 7.0))
        val generator = CustomToolTipGenerator(this@ProjectionDesktopComponent)
        setSeriesToolTipGenerator(0, generator)
        defaultItemLabelsVisible = true
        defaultItemLabelGenerator = LegendXYItemLabelGenerator(this@ProjectionDesktopComponent)
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
        xyPlot.renderer = renderer
    }
    val chartPanel = ChartPanel(chart).also {
        add(it)
    }

    // Bottom stuff
    val pointsLabel = JLabel()
    val dimensionsLabel = JLabel()
    val errorLabel = JLabel("Error: ---")
    val bottomPanel = JPanel().apply {
        layout = FlowLayout(FlowLayout.LEFT)
        add(pointsLabel)
        add(Box.createHorizontalStrut(25));
        add(dimensionsLabel)
        add(Box.createHorizontalStrut(25));
    }

    fun showPrefDialog() {
        projector.createEditorDialog {
            it.init()
            launch {
                it.events.settingsChanged.fire()
                redrawAllPoints()
            }
        }.display()
    }

    private suspend fun redrawAllPoints() {
        withContext(Dispatchers.Swing) {
            xyCollection.getSeries(0).clear()
            pointList.clear()
            projector.dataset.kdTree.forEach {
                pointList.add(it)
                val (x, y) = it.downstairsPoint
                xyCollection.getSeries(0).add(x, y)
            }
            pointsLabel.text = "Datapoints: ${projector.dataset.kdTree.size}"
            dimensionsLabel.text = "Dimensions: ${projector.dimension}"
        }
    }

    init {
        layout = BorderLayout()

        add("North", topPanel)
        add("Center", chartPanel)
        add("South", bottomPanel)

        frame.jMenuBar = JMenuBar().apply {
            add(JMenu("File").apply {
                add(JMenuItem(importAction))
                add(JMenuItem(exportAction))
                addSeparator()
                add(JMenuItem(closeAction))
            })
            add(JMenu("Edit").apply {
                val prefsAction: Action = prefsAction
                add(JMenuItem(prefsAction))
            })
        }

        projector.events.datasetChanged.on {
            redrawAllPoints()
        }
        projector.events.pointUpdated.on {
            chart.fireChartChanged()
        }
        projector.events.datasetCleared.on {
            projector.coloringManager.reset()
        }
        projector.events.settingsChanged.on {
            renderer.setSeriesLinesVisible(0, projector.connectPoints)
        }
        projector.events.methodChanged.on { o, n ->
            projectionSelector.selectedItem = projectionMethods[n.javaClass]
            running = false
            projector.events.stopIterating.fireAndBlock()
            if (n is IterableProjectionMethod) {
                topPanel.add(runToolbar)
                bottomPanel.add(errorLabel)
            } else {
                bottomPanel.remove(errorLabel)
                topPanel.remove(runToolbar)
            }
            topPanel.revalidate()
            topPanel.repaint()
            bottomPanel.revalidate()
            bottomPanel.repaint()
            launch { redrawAllPoints() }
        }
        projector.events.iterated.on { error ->
            errorLabel.text = "Error: ${error.format(2)}"
        }
        launch {
            redrawAllPoints()
        }
    }

}

fun main() {
    val projector = Projector(5).apply {
        // val random = Random(1)
        // repeat(100) {
        //     projector.addDataPoint(DoubleArray(5) { random.nextDouble() })
        // }
        // projector.projectionMethod = SammonProjection2(projector.dimension).apply {
        //     epsilon = 100.0
        // }
        (0 until 40).forEach {p ->
            val point= DataPoint(DoubleArray(100) { p.toDouble() }, label = "$p")
            addDataPoint(point)
        }
        dataset.randomizeDownstairs()
        init()
    }
    StandardDialog().apply{
        val desktopComponent = ProjectionDesktopComponent(
            this, ProjectionComponent("test", projector))
        contentPane = desktopComponent
        makeVisible()
    }
}

private class CustomRenderer(val proj: ProjectionDesktopComponent) : XYLineAndShapeRenderer() {
    override fun getItemPaint(series: Int, index: Int): Paint {
        val projector = proj.projector
        val hotColor = if (projector.useHotColor) projector.hotColor else projector.baseColor
        if (proj.pointList[index] === proj.projector.dataset.currentPoint) {
            return hotColor
        }
        return with(projector) {
            projector.coloringManager.getColor(proj.pointList[index])?: projector.baseColor
        }
    }
}

private class CustomToolTipGenerator(val proj: ProjectionDesktopComponent) : CustomXYToolTipGenerator() {
    override fun generateToolTip(data: XYDataset, series: Int, index: Int): String {
        return proj.pointList[index].upstairsPoint.format(2)
    }
}

class LegendXYItemLabelGenerator(val proj: ProjectionDesktopComponent) : StandardXYItemLabelGenerator(), XYItemLabelGenerator {
    override fun generateLabel(dataset: XYDataset, series: Int, index: Int): String? {
        return if (proj.projector.showLabels) proj.pointList[index].label else null
    }
}

