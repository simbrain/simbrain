/** Network keyboard bindings; selection editing shares the menu action and its captured model targets. */
package org.simbrain.network.gui

import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.util.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent.*
import javax.swing.KeyStroke

/**
 * Add key bindings to network panel. Controls many keyboard shortcuts. Bindings not found here are in the action
 * classes. See [NetworkActions.kt]
 */
fun NetworkPanel.addKeyBindings() {

    bind(VK_UP) { incrementSelectedObjects() }
    bind(VK_RIGHT) {incrementSelectedObjects() }
    bind(VK_DOWN) { decrementSelectedObjects() }
    bind(VK_LEFT) { decrementSelectedObjects() }
    bind(Shift + VK_UP) { nudge(0, -1) }
    bind(Shift + VK_DOWN) { nudge(0, 1) }
    bind(Shift + VK_LEFT) { nudge(-1, 0) }
    bind(Shift + VK_RIGHT) { nudge(1, 0) }
    bind(VK_ESCAPE) {
        clearAllPixelSelections()
        selectionManager.clear()
        selectionManager.clearAllSource()
    }

    bind(Shift + 'C') { hardClearSelectedObjects() }
    bind(Alt + 'D') { println(network) } // Print debug information
    bind(Shift + 'F') { toggleClamping() }
    bindTo(CmdOrCtrl + 'E', networkActions.editSelectedModelsAction)
    bindTo("G", networkActions.addGroupAction)
    bindTo("S", networkActions.selectionEditModeAction)
    bind("D") {
        if (mouseCursor == MouseEventHandler.MouseCursor.Wand) {
            NetworkPreferences.wandPalette.cycleToNextAction()
        } else {
            wandEventHandler.refreshCursor()
            mouseCursor = MouseEventHandler.MouseCursor.Wand
        }
    }
    bind(Shift + 'D') {
        if (mouseCursor == MouseEventHandler.MouseCursor.Wand) {
            NetworkPreferences.wandPalette.cycleToPreviousAction()
        }
    }
    bind("X") {
        if (mouseCursor == MouseEventHandler.MouseCursor.Wand) {
            NetworkPreferences.wandPalette.swapSelectedAndSecondary()
        }
    }
    // Shift on its own, for the wand cursor to show the secondary action while it is held. Run directly rather than
    // launched, so that a press and the release after it are seen in that order.
    putInputMap(KeyStroke.getKeyStroke(VK_SHIFT, InputEvent.SHIFT_DOWN_MASK), "Wand shift pressed")
    actionMap.put("Wand shift pressed") { wandEventHandler.shiftChanged(true) }
    putInputMap(KeyStroke.getKeyStroke(VK_SHIFT, 0, true), "Wand shift released")
    actionMap.put("Wand shift released") { wandEventHandler.shiftChanged(false) }
    bind("K") { selectionManager.set(filterScreenElements<NeuronNode>()); clearSelectedObjects() }
    // TODO: Is this the right place for this?
    bind(Shift + 'I') {
        val matrices = selectionManager.filterSelectedModels<WeightMatrix>()
        if (matrices.isNotEmpty()) networkComponent.markChanged()
        matrices.forEach {
            it.diagonalize()
        }
    }
    bind(Ctrl + 'P') {showPiccoloDebugger()}

    bind(Alt + 'R') {
        if (selectionManager.selectedModels.any { it is NeuronArray }) networkComponent.markChanged()
        selectionManager.selectedModels.forEach {
            if (it is NeuronArray) {
                it.verticalLayout = !it.verticalLayout
            }
        }
    }

    bind("1") { selectionManager.convertSelectedNodesToSourceNodes() }
    bind(Shift + '2') { connectSelectedModelsDefault(allowSelfConnection = true) }
    bind("2") { connectSelectedModelsDefault() }
    bind("3") { connectSelectedModelsCustom() }
    bindTo("4", networkActions.connectWithGapJunction)
    bindTo("6", networkActions.selectIncomingWeightsAction)
    bindTo("7", networkActions.selectOutgoingWeightsAction)
    bind("8") {
        selectionManager.selectedModels.filterIsInstance<LocatableModel>().forEach { println(it.location) }
    }
}
