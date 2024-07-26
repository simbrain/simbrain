package org.simbrain.custom_sims

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.simbrain.docviewer.DocViewerComponent
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.*
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.imageworld.ImageWorldComponent
import org.simbrain.world.odorworld.OdorWorldComponent
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

class NewSimulation(val task: suspend SimulationScope.(optionString: String?) -> Unit): CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Default + job

    suspend fun run(desktop: SimbrainDesktop? = null, optionString: String? = null) {
        with(SimulationScope(desktop)) {
            task(optionString)
        }
    }
}

/**
 * When running simulation headless, the option string can be used to pass option to the simulation.
 * @see CowGrazing
 */
fun newSim(block: suspend SimulationScope.(optionString: String?) -> Unit) = NewSimulation(block)

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
    name: String? = null,
    map: String = "empty.tmx",
    config: OdorWorldComponent.() -> Unit = { }
): OdorWorldComponent {
    return OdorWorldComponent(name ?: map)
        .apply(config)
        .also { if (map != "empty.tmx") it.world.tileMap = loadTileMap(map) }
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

suspend fun SimulationScope.addProjectionPlot2(name: String): ProjectionComponent {
    val projectionComponent = ProjectionComponent(name)
    workspace.addWorkspaceComponent(projectionComponent)
    return projectionComponent
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

