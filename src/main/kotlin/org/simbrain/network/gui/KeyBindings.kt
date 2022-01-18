package org.simbrain.network.gui

import org.simbrain.network.LocatableModel
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.matrix.ZoeLayer
import org.simbrain.util.*
import java.awt.event.KeyEvent.*

/**
 * Add key bindings to network panel. Controls many keyboard shortcuts. Bindings not found here are in the action
 * classes.
 */
fun NetworkPanel.addKeyBindings() {

    bind(VK_UP) { incrementSelectedObjects() }
    bind(VK_DOWN) { decrementSelectedObjects() }
    bind(Shift + VK_UP) { nudge(0, -1) }
    bind(Shift + VK_DOWN) { nudge(0, 1) }
    bind(Shift + VK_LEFT) { nudge(-1, 0) }
    bind(Shift + VK_RIGHT) { nudge(1, 0) }
    bind("delete", "back_space") { deleteSelectedObjects() }
    bind(VK_ESCAPE) { selectionManager.clear(); selectionManager.clearAllSource() }

    bind(Shift + 'C') { hardClearSelectedObjects() }
    bind(Alt + 'D') { println(network) } // Print debug information
    bind(Shift + 'F') { toggleClamping() }
    bind(CmdOrCtrl + 'D') { duplicate() }
    bind(CmdOrCtrl + 'E') {
        selectionManager.selection.firstNotNullOfOrNull { it.propertyDialog }?.display()
    }
    bindTo("G", networkActions.neuronGroupAction)
    bindTo("I", networkActions.wandEditModeAction)
    bind("K") { selectionManager.set(filterScreenElements<NeuronNode>()); clearSelectedObjects() }
    bind(Shift + 'I') {
        selectionManager.filterSelectedModels<WeightMatrix>().forEach {
            it.diagonalize()
        }
    }
    bind(Ctrl + 'P') {showPiccoloDebugger()}
    bind("S") { selectNeuronsInNeuronGroups() }
    bindTo("T", networkActions.textEditModeAction)
    //TODO Make default settable (same with neuron?)
    bind("Y") { network.addNetworkModel(NeuronArray(network,50)) }
    bind(Shift +'Y') { showNeuronArrayCreationDialog() }
    bind(CmdOrCtrl + 'Y') { redo() }
    bind(Shift + 'Z') { network.addNetworkModel(ZoeLayer(network, 10)) } // TODO: Temp testing key command
    bind(CmdOrCtrl + 'Z') { undo() }
    bind(CmdOrCtrl + Shift + 'Z') { redo() }
    bind("1") { selectionManager.convertSelectedNodesToSourceNodes() }
    bind("2") { connectSelectedModels() }
    // TODO: Add Shift+2 to add models with a creation dialog
    bind("5") { looseWeightsVisible = !looseWeightsVisible }
    bindTo("6", networkActions.selectIncomingWeightsAction)
    bindTo("7", networkActions.selectOutgoingWeightsAction)
    bind("8") {
        network.events.fireDebug()
        selectionManager.selectedModels.filterIsInstance<LocatableModel>().forEach { println(it.location) }
    }
}
