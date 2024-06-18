package org.simbrain.network.gui

import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.util.*
import java.awt.event.KeyEvent.*

/**
 * Add key bindings to network panel. Controls many keyboard shortcuts. Bindings not found here are in the action
 * classes. See [NetworkActions.kt]
 */
fun NetworkPanel.addKeyBindings() {

    bind(VK_UP) { incrementSelectedObjects() }
    bind(VK_DOWN) { decrementSelectedObjects() }
    bind(Shift + VK_UP) { nudge(0, -1) }
    bind(Shift + VK_DOWN) { nudge(0, 1) }
    bind(Shift + VK_LEFT) { nudge(-1, 0) }
    bind(Shift + VK_RIGHT) { nudge(1, 0) }
    bind(VK_ESCAPE) { selectionManager.clear(); selectionManager.clearAllSource() }

    bind(Shift + 'C') { hardClearSelectedObjects() }
    bind(Alt + 'D') { println(network) } // Print debug information
    bind(Shift + 'F') { toggleClamping() }
    bind(CmdOrCtrl + 'D') { duplicate() }
    bind(CmdOrCtrl + 'E') {
        selectionManager.selection.firstNotNullOfOrNull { it.propertyDialog }?.display()
    }
    bindTo("G", networkActions.addGroupAction)
    bindTo("I", networkActions.wandEditModeAction)
    bind("K") { selectionManager.set(filterScreenElements<NeuronNode>()); clearSelectedObjects() }
    bind("K") { selectionManager.set(filterScreenElements<NeuronNode>()); clearSelectedObjects() }
    // TODO: Is this the right place for this?
    bind(Shift + 'I') {
        selectionManager.filterSelectedModels<WeightMatrix>().forEach {
            it.diagonalize()
        }
    }
    bind(Ctrl + 'P') {showPiccoloDebugger()}
    bind("S") { selectNeuronsInNeuronGroups() }
    bind(CmdOrCtrl + 'Y') { redo() }
    bind(CmdOrCtrl + 'Z') { undo() }
    bind(CmdOrCtrl + Shift + 'Z') { redo() }

    bind("1") { selectionManager.convertSelectedNodesToSourceNodes() }
    bind("2") { connectSelectedModelsDefault() }
    bind("3") { connectSelectedModelsCustom()}
    bindTo("6", networkActions.selectIncomingWeightsAction)
    bindTo("7", networkActions.selectOutgoingWeightsAction)
    bind("8") {
        selectionManager.selectedModels.filterIsInstance<LocatableModel>().forEach { println(it.location) }
    }
}
