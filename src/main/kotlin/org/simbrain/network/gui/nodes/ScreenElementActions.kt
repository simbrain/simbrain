@file:JvmName("ScreenElementActions")
package org.simbrain.network.gui.nodes

import kotlinx.coroutines.CoroutineScope
import org.simbrain.network.core.Network
import org.simbrain.util.KeyCombination
import org.simbrain.util.createAction
import java.awt.event.ActionEvent
import javax.swing.AbstractAction


suspend fun meow() {

}

fun <T: ScreenElement> T.createScreenElementAction(
    name: String? = null,
    description: String? = null,
    iconPath: String? = null,
    keyboardShortcut: KeyCombination? = null,
    initBlock: AbstractAction.() -> Unit = {},
    coroutineScope: CoroutineScope? = null,
    block: suspend context(Network) T.(e: ActionEvent) -> Unit
): AbstractAction {
    val screenElement = this
    return networkPanel.createAction(name, description, iconPath, keyboardShortcut, initBlock, coroutineScope) {
        block(network, screenElement, it)
    }
}

fun SubnetworkNode.createRandomNetAction() = createScreenElementAction(
    name = "Randomize synapses symmetrically",
) {
    subnetwork.randomize()
}
