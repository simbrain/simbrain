package org.simbrain.custom_sims

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.helper_classes.ControlPanel
import org.simbrain.docviewer.DocViewerComponent
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.ResourceManager
import org.simbrain.util.Utils
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.updater.UpdateAction
import org.simbrain.world.imageworld.ImageWorldComponent
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.textworld.TextWorldComponent

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
    suspend fun withGui(block: suspend SimbrainDesktop.() -> Unit) {
        if (desktop != null) {
            desktop.block()
        }
    }
}

class NewSimulation(val task: suspend SimulationScope.() -> Unit): CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Default + job

    fun run(desktop: SimbrainDesktop? = null) {
        launch {
            with(SimulationScope(desktop)) {
                task()
            }
        }
    }
}

fun newSim(block: suspend SimulationScope.() -> Unit) = NewSimulation(block)

fun SimulationScope.addNetworkComponent(name: String, config: NetworkComponent.() -> Unit = { }): NetworkComponent {
    return NetworkComponent(name)
        .apply(config)
        .also { workspace.addWorkspaceComponent(it, true) }
}

fun SimulationScope.addNetworkComponent(name: String, network : Network): NetworkComponent {
    return NetworkComponent(name, network)
        .also { workspace.addWorkspaceComponent(it, true) }
}

fun SimulationScope.addOdorWorldComponent(
    name: String? = null,
    map: String = "empty.tmx",
    config: OdorWorldComponent.() -> Unit = { }
): OdorWorldComponent {
    return OdorWorldComponent(name ?: map)
        .apply(config)
        .also { if (map != "empty.tmx") it.world.tileMap = loadTileMap(map) }
        .also { workspace.addWorkspaceComponent(it, true) }
}

/**
 * Add a projection plot and return a plot builder.
 *
 * @param name title to display at top of panel
 * @return the component the plot builder
 */
fun SimulationScope.addProjectionPlot(name: String?): ProjectionComponent {
    val projectionComponent = ProjectionComponent(name)
    workspace.addWorkspaceComponent(projectionComponent, true)
    return projectionComponent
}

fun SimulationScope.addImageWorld(name: String?): ImageWorldComponent {
    val imageWorldComponent = ImageWorldComponent(name)
    workspace.addWorkspaceComponent(imageWorldComponent, true)
    return imageWorldComponent
}

fun SimulationScope.addTextWorld(name: String?): TextWorldComponent {
    val textWorldComponent = TextWorldComponent(name)
    workspace.addWorkspaceComponent(textWorldComponent, true)
    return textWorldComponent
}

fun SimulationScope.addTimeSeries(name: String?): TimeSeriesPlotComponent {
    val timeSeriesPlotComponent = TimeSeriesPlotComponent(name)
    workspace.addWorkspaceComponent(timeSeriesPlotComponent, true)
    return timeSeriesPlotComponent
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

fun SimulationScope.getResource(fileName: String): String {
    return ResourceManager.getString(
        "custom_sims" + Utils.FS + fileName
    )
}

/**
 * Add a doc viewer component.
 *
 * @param title    title to display at top of panel
 * @param fileName name of the html file, e.g. "ActorCritic.html"
 * @return the component
 */
fun SimulationScope.addDocViewer(title: String?, fileName: String): DocViewerComponent {

    val docViewer = DocViewerComponent(title)
    val html = ResourceManager.getString(
        "custom_sims" + Utils.FS + fileName
    )
    docViewer.text = html
    workspace.addWorkspaceComponent(docViewer, true)
    return docViewer
}

val SimulationScope.couplingManager get() = workspace.couplingManager

fun SimbrainDesktop.createControlPanel(name: String, x: Int, y: Int, config: ControlPanel.() -> Unit): ControlPanel {
    return ControlPanel.makePanel(this, name, x, y).apply(config)
}

fun updateAction(description: String, longDescription: String = description, action: () -> Unit)
        = object : UpdateAction(description, longDescription) {
    override suspend fun run() {
        action()
    }
}

