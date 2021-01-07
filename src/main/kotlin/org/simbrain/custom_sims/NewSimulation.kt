package org.simbrain.custom_sims

import org.simbrain.custom_sims.helper_classes.ControlPanel
import org.simbrain.docviewer.DocViewerComponent
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.util.ResourceManager
import org.simbrain.util.Utils
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.updater.UpdateAction
import org.simbrain.world.odorworld.OdorWorldComponent

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
    fun withGui(block: SimbrainDesktop.() -> Unit) {
        desktop?.run(block)
    }

}

// TODO: Rename to Simulation after Simulation.java is removed
class NewSimulation(val task: SimulationScope.() -> Unit) {
    fun run(desktop: SimbrainDesktop? = null) {
        SimulationScope(desktop).apply(task)
    }
}

fun newSim(block: SimulationScope.() -> Unit) = NewSimulation(block)

fun SimulationScope.addNetworkComponent(name: String, config: NetworkComponent.() -> Unit = { }): NetworkComponent {
    return NetworkComponent(name)
        .apply(config)
        .also(workspace::addWorkspaceComponent)
}

fun SimulationScope.addNetworkComponent(name: String, network : Network): NetworkComponent {
    return NetworkComponent(name, network)
        .also(workspace::addWorkspaceComponent)
}

fun SimulationScope.addOdorWorldComponent(
    name: String? = null,
    map: String = "empty.tmx",
    config: OdorWorldComponent.() -> Unit = { }
): OdorWorldComponent {
    return OdorWorldComponent(name ?: map)
        .apply(config)
        .also { if (map != "empty.tmx") it.world.tileMap = loadTileMap(map) }
        .also(workspace::addWorkspaceComponent)
}

/**
 * Add a projection plot and return a plot builder.
 *
 * @param name title to display at top of panel
 * @return the component the plot builder
 */
fun SimulationScope.addProjectionPlot(name: String?): ProjectionComponent {
    val projectionComponent = ProjectionComponent(name)
    workspace.addWorkspaceComponent(projectionComponent)
    return projectionComponent
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
    workspace.addWorkspaceComponent(docViewer)
    return docViewer
}

val SimulationScope.couplingManager get() = workspace.couplingManager

fun SimbrainDesktop.createControlPanel(name: String, x: Int, y: Int, config: ControlPanel.() -> Unit): ControlPanel {
    return ControlPanel.makePanel(this, name, x, y).apply(config)
}

fun updateAction(description: String, longDescription: String = description, action: () -> Unit)
        = object : UpdateAction {
    override fun invoke() = action()
    override fun getDescription(): String = description
    override fun getLongDescription(): String = longDescription
}

