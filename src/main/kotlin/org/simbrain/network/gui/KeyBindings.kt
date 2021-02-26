package org.simbrain.network.gui

import org.intellij.lang.annotations.MagicConstant
import org.simbrain.network.LocatableModel
import org.simbrain.network.smile.SmileSVM
import org.simbrain.util.Utils
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent.*
import javax.swing.AbstractAction
import javax.swing.ActionMap
import javax.swing.JComponent
import javax.swing.KeyStroke

/**
 * Add key bindings to network panel. Controls many keyboard shortcuts. Bindings not found here are in the action
 * classes.
 */
fun NetworkPanel.addKeyBindings() {
    bind(VK_UP) { contextualIncrementSelectedObjects() }
    bind(VK_DOWN) { contextualDecrementSelectedObjects() }
    bind(Shift + VK_UP) { nudge(0, -1) }
    bind(Shift + VK_DOWN) { nudge(0, 1) }
    bind(Shift + VK_LEFT) { nudge(-1, 0) }
    bind(Shift + VK_RIGHT) { nudge(1, 0) }
    bind("delete", "back_space") { deleteSelectedObjects() }
    bind(Shift + 'F') { toggleClamping() }
    bind(CmdOrCtrl + 'D') { duplicate() }
    bind(VK_ESCAPE) { selectionManager.clear(); selectionManager.clearAllSource() }
    bind("C") { clearSelectedObjects() }
    bind(Alt + 'D') { println(network) } // Print debug information
    bind(Alt + 'P') {showPiccoloDebugger()}
    bind("S") { selectNeuronsInNeuronGroups() }
    bindTo("T", networkActions.textEditModeAction)
    bind(Shift + 'T') { network.addSVM(SmileSVM()) } // TODO: Temp testing key command
    bindTo("I", networkActions.wandEditModeAction)
    bindTo("G", networkActions.neuronGroupAction)
    bind("Y") { showNeuronArrayCreationDialog() }
    bind(CmdOrCtrl + 'Z') { undo() }
    bind(CmdOrCtrl + Shift + 'Z') { redo() }
    bind(CmdOrCtrl + 'Y') { redo() }
    bind("1") { selectionManager.convertSelectedNodesToSourceNodes() }
    bind("2") { connectSelectedModels() }
    bindTo("3", networkActions.selectIncomingWeightsAction)
    bindTo("4", networkActions.selectOutgoingWeightsAction)
    bind("5") { looseWeightsVisible = !looseWeightsVisible }
    bind("6") { connectWithWeightMatrix() }
    bind("8") {
        network.events.fireDebug()
        selectionManager.selectedModels.filterIsInstance<LocatableModel>().forEach { println(it.location) }
    }
}

sealed class KeyMask {
    operator fun plus(key: Char) = KeyCombination(key, keyCode)

    operator fun plus(@MagicConstant(valuesFromClass = KeyStroke::class) key: Int) =
            KeyCombination(key, keyCode)

    operator fun plus(keyMask: KeyMask) = KeyCombination(modifiers = this.keyCode or keyMask.keyCode)

    abstract val keyCode: Int
}

object Alt : KeyMask() {
    override val keyCode = ALT_DOWN_MASK
}

object Shift : KeyMask() {
    override val keyCode = SHIFT_DOWN_MASK
}

object Ctrl : KeyMask() {
    override val keyCode = CTRL_DOWN_MASK
}

// Command on Mac, Control on other systems
object CmdOrCtrl : KeyMask() {
    override val keyCode = if (Utils.isMacOSX()) META_DOWN_MASK else CTRL_DOWN_MASK
}

class KeyCombination(
        @MagicConstant(valuesFromClass = KeyStroke::class) val key: Int? = null,
        val modifiers: Int = 0
) {
    constructor(char: Char, modifiers: Int = 0) : this(char.toInt(), modifiers)

    operator fun plus(keyMask: KeyMask) = KeyCombination(key, modifiers or keyMask.keyCode)
    operator fun plus(key: Char) = KeyCombination(key, modifiers)
    fun withKeyStroke(block: (KeyStroke) -> Unit) = key?.let { block(KeyStroke.getKeyStroke(it, modifiers)) }
    override fun toString() = "$modifiers + $key"
}

fun NetworkPanel.bindTo(key: String, action: AbstractAction) {
    val keyName = "Key $key"
    putInputMap(KeyStroke.getKeyStroke(key), keyName)
    actionMap.put(keyName, action)
}

inline fun NetworkPanel.bind(vararg keys: String, crossinline action: NetworkPanel.() -> Unit) {
    val keyName = "Key ${keys.joinToString("")}"
    keys.forEach { key -> putInputMap(KeyStroke.getKeyStroke(key.toUpperCase()), keyName) }
    actionMap.put(keyName) { action() }
}

inline fun NetworkPanel.bind(keyStroke: KeyStroke, crossinline action: NetworkPanel.() -> Unit) {
    val keyName = "Key $keyStroke"
    putInputMap(keyStroke, keyName)
    actionMap.put(keyName) { action() }
}

inline fun NetworkPanel.bind(
        @MagicConstant(valuesFromClass = KeyStroke::class) key: Int,
        crossinline action: NetworkPanel.() -> Unit
) {
    val keyName = "Key vki_$key"
    putInputMap(KeyStroke.getKeyStroke(key, 0), keyName)
    actionMap.put(keyName) { action() }
}

inline fun NetworkPanel.bind(vararg keys: KeyCombination, crossinline action: NetworkPanel.() -> Unit) {
    val keyName = "Key ${keys.joinToString("")}"
    keys.forEach { key ->
        key.withKeyStroke { getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(it, keyName) }
        actionMap.put(keyName) { action() }
    }
}

fun NetworkPanel.putInputMap(keyStroke: KeyStroke, mapKey: String) {
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, mapKey)
}

inline fun ActionMap.put(key: String, crossinline action: () -> Unit) {
    put(key, object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            action()
        }
    })
}