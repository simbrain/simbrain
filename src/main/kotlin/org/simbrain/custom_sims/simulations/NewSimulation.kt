package org.simbrain.custom_sims.simulations

import org.simbrain.network.NetworkComponent
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.odorworld.OdorWorldComponent

class SimulationScope(val desktop: SimbrainDesktop? = null) {

    val workspace = desktop?.workspace ?: Workspace()

    fun withGui(block: SimbrainDesktop.() -> Unit) {
        desktop?.run(block)
    }

}

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

val SimulationScope.couplingManager get() = workspace.couplingManager
