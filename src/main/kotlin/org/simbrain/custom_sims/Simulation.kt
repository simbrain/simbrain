package org.simbrain.custom_sims

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.simbrain.docviewer.DocViewerComponent
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.*
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.WorkspacePreferences
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.dataworld.DataWorld
import org.simbrain.world.dataworld.DataWorldComponent
import org.simbrain.world.imageworld.ImageWorldComponent
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.textworld.TextWorldComponent
import java.io.File

class SimulationScope private constructor(
    val desktop: SimbrainDesktop?,
    val workspace: Workspace
) {

    constructor(desktop: SimbrainDesktop? = null): this(desktop, desktop?.workspace ?: Workspace())

    private constructor(workspace: Workspace): this(null, workspace)

    operator fun <T> Workspace.invoke(block: SimulationScope.() -> T): T {
        return SimulationScope(this).block()
    }

    /**
     * If Desktop exists, provide a context for convenient access.
     */
    suspend fun <T> withGui(block: suspend SimbrainDesktop.() -> T): T? {
        if (desktop != null) {
            return desktop.block()
        }
        return null
    }
}

class NewSimulation(val id: String?, val task: suspend SimulationScope.(optionString: String?) -> Unit): CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Default + job

    private lateinit var reopenFunction: suspend SimulationScope.(workspace: Workspace) -> Unit

    suspend fun run(desktop: SimbrainDesktop? = null, optionString: String? = null) {
        with(SimulationScope(desktop)) {
            task(optionString)
            workspace.simulationId = id
        }
    }

    fun registerReopenFunction(block: suspend SimulationScope.(workspace: Workspace) -> Unit): NewSimulation = apply {
        if (id == null) {
            println("reopen function can only be called when the simulation id is set")
        }
        reopenFunction = block
    }

    suspend fun reopen(workspace: Workspace, desktop: SimbrainDesktop? = null) {
        with(SimulationScope(desktop)) {
            reopenFunction(workspace)
        }
    }
}

/**
 * When running simulation headless, the option string can be used to pass option to the simulation.
 * @see CowGrazing
 * @param id a unique id for workspace to identify which simulation to run upon deserialization
 */
fun newSim(id: String? = null, block: suspend SimulationScope.(optionString: String?) -> Unit) = NewSimulation(id, block)

fun SimulationScope.addNetworkComponent(name: String, config: NetworkComponent.() -> Unit = { }): NetworkComponent {
    return NetworkComponent(name)
        .apply(config)
        .also { workspace.addWorkspaceComponent(it) }
}

fun SimulationScope.addNetworkComponent(name: String, network : Network): NetworkComponent {
    return NetworkComponent(name, network)
        .also { workspace.addWorkspaceComponent(it) }
}

fun SimulationScope.addOdorWorldComponent(
    name: String,
    config: OdorWorldComponent.() -> Unit = { }
): OdorWorldComponent {
    return OdorWorldComponent(name)
        .apply(config)
        .also { workspace.addWorkspaceComponent(it) }
}

/**
 * Add a projection plot and return a plot builder.
 *
 * @param name title to display at top of panel
 * @return the component the plot builder
 */
suspend fun SimulationScope.addProjectionPlot(name: String): ProjectionComponent {
    val projectionComponent = ProjectionComponent(name)
    workspace.addWorkspaceComponent(projectionComponent)
    return projectionComponent
}

suspend fun SimulationScope.addRasterPlot(name: String): RasterPlotComponent {
    val rasterComponent = RasterPlotComponent(name)
    workspace.addWorkspaceComponent(rasterComponent)
    return rasterComponent
}

fun SimulationScope.addImageWorld(name: String): ImageWorldComponent {
    val imageWorldComponent = ImageWorldComponent(name)
    workspace.addWorkspaceComponent(imageWorldComponent)
    return imageWorldComponent
}

fun SimulationScope.addTextWorld(name: String): TextWorldComponent {
    val textWorldComponent = TextWorldComponent(name)
    workspace.addWorkspaceComponent(textWorldComponent)
    return textWorldComponent
}

fun SimulationScope.addTimeSeriesComponent(name: String, seriesName: String) = addTimeSeriesComponent(name, listOf(seriesName))

fun SimulationScope.addTimeSeriesComponent(name: String, seriesNames: List<String>): TimeSeriesPlotComponent {
    val timeSeriesPlotComponent = TimeSeriesPlotComponent(name)
    seriesNames.forEach { timeSeriesPlotComponent.addTimeSeries(it) }
    workspace.addWorkspaceComponent(timeSeriesPlotComponent)
    return timeSeriesPlotComponent
}

class TimeSeriesPlotComponentSeriesData(val plotComponent: TimeSeriesPlotComponent, val series: List<TimeSeriesModel.TimeSeries>) {
    operator fun component1() = plotComponent
    operator fun component2() = series.component1()
    operator fun component3() = series.component2()
    operator fun component4() = series.component3()
    operator fun component5() = series.component4()
    operator fun component6() = series.component5()
}

fun SimulationScope.addTimeSeries(name: String, seriesNames: List<String>): TimeSeriesPlotComponentSeriesData {
    val timeSeriesPlotComponent = TimeSeriesPlotComponent(name)
    val series = seriesNames.map { timeSeriesPlotComponent.addTimeSeries(it) }
    workspace.addWorkspaceComponent(timeSeriesPlotComponent)
    return TimeSeriesPlotComponentSeriesData(timeSeriesPlotComponent, series)
}

/**
 * Add a DataWorld (numeric table) component.
 *
 * @param name title to display at top of panel
 * @param numColumns the number of columns in the table
 */
fun SimulationScope.addDataWorldComponent(name: String, numRows: Int, numColumns: Int): DataWorldComponent {
    val table = DataWorld(numRows, numColumns)
    val component = DataWorldComponent(name, table)
    workspace.addWorkspaceComponent(component)
    return component
}

suspend fun SimulationScope.placeComponent(component: WorkspaceComponent, x: Int, y: Int, width: Int, height: Int) {
    withGui {
        place(component) {
            location = point(x,y)
            this.width = width
            this.height = height
        }
    }
}

/**
 * Grabs a resource from the src/main/resources/custom_sims directory and returns it as a string.
 */
fun SimulationScope.readSimulationFileContents(fileName: String): String {
    return File(Utils.USER_DIR / "simulations" / fileName).readText()
}

val simulationsPath = File(Utils.USER_DIR / "simulations").absolutePath

/**
 * Add a doc viewer component.
 *
 * @param title    title to display at top of panel
 * @param fileName name of the markdown or html file, e.g. "ActorCritic.html"
 */
fun SimulationScope.addDocViewerFromFile(title: String, fileName: String): DocViewerComponent {

    val docViewerComponent = DocViewerComponent(name = title)
    val html = ResourceManager.readFileContents(
        "custom_sims" + Utils.FS + fileName
    )
    docViewerComponent.docViewer.text = html
    docViewerComponent.docViewer.render()
    workspace.addWorkspaceComponent(docViewerComponent)
    return docViewerComponent
}

suspend fun SimulationScope.addSidebarInfoFromFile(fileName: String, initiallyOpened: Boolean = WorkspacePreferences.showSimulationInfoByDefault) {
    workspace.infoDoc.text =  ResourceManager.readFileContents(
        "custom_sims" + Utils.FS + fileName
    )
    workspace.infoDoc.render()
    withGui {
        if (initiallyOpened) {
            sideDockSplitter.showDock()
        } else {
            sideDockSplitter.hideDock()
        }
    }
}

/**
 * Add a doc viewer component with inlined markdown text
 */
fun SimulationScope.addDocViewer(title: String, markdownText: String): DocViewerComponent {
    val docViewerComponent = DocViewerComponent(name = title)
    docViewerComponent.docViewer.text = markdownText
    docViewerComponent.docViewer.render()
    workspace.addWorkspaceComponent(docViewerComponent)
    return docViewerComponent
}

suspend fun SimulationScope.addSidebarInfo(markdownText: String, width: Int? = null, initiallyOpened: Boolean = WorkspacePreferences.showSimulationInfoByDefault) {
    workspace.infoDoc.text = markdownText
    workspace.infoDoc.render()
    withGui {
        if (initiallyOpened) {
            sideDockSplitter.showDock(width)
        } else {
            sideDockSplitter.hideDock()
        }
    }
}


val SimulationScope.couplingManager get() = workspace.couplingManager

fun SimbrainDesktop.createControlPanel(name: String, x: Int, y: Int, config: ControlPanelKt.() -> Unit): ControlPanelKt {
    return ControlPanelKt(name)
        .apply { setLocation(x, y) }
        .apply(config)
        .also {
            addInternalFrame(it)
            it.pack()
            it.isVisible = true
        }
}

context(SimbrainDesktop)
suspend fun OdorWorldComponent.scale(scale: Double) {
    (getDesktopComponent(this) as? OdorWorldDesktopComponent)?.worldPanel?.scalingFactor = scale
}

context(SimbrainDesktop)
suspend fun getNetworkPanel(component: NetworkComponent): NetworkPanel {
    val desktopComponent = getDesktopComponent(component) as NetworkDesktopComponent
    return desktopComponent.networkPanel
}

context(SimbrainDesktop)
suspend fun getOdorWorldPanel(component: OdorWorldComponent): OdorWorldPanel {
    val desktopComponent = getDesktopComponent(component) as OdorWorldDesktopComponent
    return desktopComponent.worldPanel
}

/**
 * Utility class for tracking which neurons "win" for each label in competitive/SOM networks.
 * Automatically updates neuron labels to show which patterns they respond to.
 */
class WinnerLabeler {
    private val labelToNodeMap = mutableMapOf<String, org.simbrain.network.core.Neuron>()
    
    /**
     * Update the tracker with a new winning neuron for the given label.
     * This will:
     * - Remove the label from the previous winner (if different)
     * - Add the label to the new winner
     * - Update the internal map
     */
    fun updateWinner(label: String, winner: org.simbrain.network.core.Neuron) {
        val previousNode = labelToNodeMap[label]
        if (previousNode != null && previousNode != winner) {
            val oldLabel = previousNode.label ?: ""
            previousNode.label = oldLabel.replace(label, "").replace(", ,", ",").trim(',', ' ').ifEmpty { null }
        }
        
        val currentLabel = winner.label
        if (currentLabel.isNullOrEmpty()) {
            winner.label = label
        } else if (!currentLabel.contains(label)) {
            winner.label = currentLabel + ", " + label
        }
        
        labelToNodeMap[label] = winner
    }
    
    /**
     * Clear all tracked labels and remove labels from all neurons.
     */
    fun clear(neurons: List<org.simbrain.network.core.Neuron>) {
        neurons.forEach { it.label = null }
        labelToNodeMap.clear()
    }
}

