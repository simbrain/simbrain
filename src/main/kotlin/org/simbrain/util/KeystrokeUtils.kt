package org.simbrain.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.MagicConstant
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.ActionMap
import javax.swing.JComponent
import javax.swing.KeyStroke

sealed class KeyMask {
    operator fun plus(key: Char) = KeyCombination(key, keyCode)

    operator fun plus(@MagicConstant(valuesFromClass = KeyStroke::class) key: Int) =
        KeyCombination(key, keyCode)

    operator fun plus(keyMask: KeyMask) = KeyCombination(modifiers = this.keyCode or keyMask.keyCode)

    abstract val keyCode: Int
}

object Alt : KeyMask() {
    override val keyCode = KeyEvent.ALT_DOWN_MASK
}

object Shift : KeyMask() {
    override val keyCode = KeyEvent.SHIFT_DOWN_MASK
}

object Ctrl : KeyMask() {
    override val keyCode = KeyEvent.CTRL_DOWN_MASK
}

// Command on Mac, Control on other systems
object CmdOrCtrl : KeyMask() {
    override val keyCode = if (Utils.isMacOSX()) KeyEvent.META_DOWN_MASK else KeyEvent.CTRL_DOWN_MASK
}

/**
 * A convenient way to represent a key plus a modifier, using integer representations from [KeyEvent].
 */
class KeyCombination(
    @MagicConstant(valuesFromClass = KeyStroke::class) val key: Int? = null,
    val modifiers: Int = 0
) {
    constructor(char: Char, modifiers: Int = 0) : this(char.toInt(), modifiers)

    operator fun plus(keyMask: KeyMask) = KeyCombination(key, modifiers or keyMask.keyCode)
    operator fun plus(key: Char) = KeyCombination(key, modifiers)
    // Not using getKeyStroke because this.key is nullable (because e.g. the cmd keystroke by itself does not have a
    // key)
    fun withKeyStroke(block: (KeyStroke) -> Unit) = key?.let { block(KeyStroke.getKeyStroke(it, modifiers)) }
    override fun toString() = "$modifiers + $key"
}

fun JComponent.bindTo(key: String, action: AbstractAction) {
    val keyName = "Key $key"
    putInputMap(KeyStroke.getKeyStroke(key), keyName)
    actionMap.put(keyName, action)
}

fun JComponent.bindTo(keystroke: KeyStroke, action: AbstractAction) {
    val keyName = keystroke.toString()
    putInputMap(keystroke, keystroke.toString())
    actionMap.put(keyName, action)
}

fun JComponent.bindTo(keyCombo: KeyCombination, action: AbstractAction) {
    val keyName = keyCombo.toString()
    keyCombo.withKeyStroke { getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(it, keyName) }
    actionMap.put(keyName, action)
}

inline fun <C: JComponent> C.bind(vararg keys: String, crossinline action: C.() -> Unit) {
    val keyName = "Key ${keys.joinToString("")}"
    keys.forEach { key -> putInputMap(KeyStroke.getKeyStroke(key.toUpperCase()), keyName) }
    putActionMap(keyName, action)
}

inline fun <C: JComponent> C.bind(keyStroke: KeyStroke, crossinline action: C.() -> Unit) {
    val keyName = "Key $keyStroke"
    putInputMap(keyStroke, keyName)
    putActionMap(keyName, action)
}

inline fun <C: JComponent> C.bind(
    @MagicConstant(valuesFromClass = KeyStroke::class) key: Int,
    crossinline action: C.() -> Unit
) {
    val keyName = "Key vki_$key"
    putInputMap(KeyStroke.getKeyStroke(key, 0), keyName)
    putActionMap(keyName, action)
}

inline fun <C: JComponent> C.bind(vararg keys: KeyCombination, crossinline action: C.() -> Unit) {
    val keyName = "Key ${keys.joinToString("")}"
    keys.forEach { key ->
        key.withKeyStroke { getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(it, keyName) }
        putActionMap(keyName, action)
    }
}

fun JComponent.putInputMap(keyStroke: KeyStroke, mapKey: String) {
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, mapKey)
}

inline fun <C: JComponent> C.putActionMap(key: String, crossinline action: C.() -> Unit) {
    if (this is CoroutineScope) {
        actionMap.put(key) { launch { action() } }
    } else {
        actionMap.put(key) { action() }
    }
}

inline fun ActionMap.put(key: String, crossinline action: () -> Unit) {
    put(key, object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            action()
        }
    })
}
