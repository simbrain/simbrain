package org.simbrain.network.gui

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.workspace.gui.SimbrainDesktop

/**
 * Shared harness for GUI delete -> undo -> redo tests: a fresh network/component registered with the
 * (shared) [SimbrainDesktop], its [NetworkPanel], and selection/await helpers that cope with the
 * asynchronous, debounced node lifecycle.
 */
abstract class NetworkPanelDeleteUndoTestBase {

    protected val network = Network()
    private val networkComponent = NetworkComponent("Test", network)

    init {
        SimbrainDesktop.workspace.clearWorkspace()
        Clipboard.clear()
        SimbrainDesktop.workspace.addWorkspaceComponent(networkComponent)
    }

    protected val panel: NetworkPanel by lazy {
        runBlocking { (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel }
    }

    protected suspend fun selectOnly(model: NetworkModel) = selectOnly(listOf(model))

    protected suspend fun selectOnly(models: List<NetworkModel>) {
        // Node creation is async; wait for each model's node, then read it with the non-blocking peek().
        // (get() caps at 1000ms and times out under shared-desktop load.)
        models.forEach { m -> awaitUntil { panel.modelNodeMap.peek(m) != null } }
        // Also settle the containing subnetwork's node: undo's restore does a blocking
        // getImmediately<SubnetworkNode>(parent) (UndoManager.kt) that would otherwise time out at 1000ms
        // if the subnetwork node is still pending under heavy test-load EDT.
        models.forEach { m ->
            network.getModels<Subnetwork>().firstOrNull { m in it.modelList.all }?.let { subnet ->
                awaitUntil { panel.modelNodeMap.peek(subnet) != null }
            }
        }
        panel.selectionManager.set(models.mapNotNull { panel.modelNodeMap.peek(it) })
    }

    /** Poll until [cond] holds or the timeout elapses (node creation/removal is async + debounced). */
    protected suspend fun awaitUntil(timeoutMs: Int = 5000, cond: () -> Boolean) {
        repeat(timeoutMs / 50) {
            if (cond()) return
            delay(50)
        }
    }
}
