package org.simbrain.util

import java.awt.Component
import java.awt.event.*
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JDialog

fun StandardDialog.present() = apply { isVisible = true }

inline fun StandardDialog.onClosed(crossinline block: (WindowEvent?) -> Unit) = apply {
    addWindowListener(object : WindowAdapter() {
        override fun windowClosed(e: WindowEvent?) {
            block(e)
        }
    })
}

fun JDialog.display() {
    pack()
    setLocationRelativeTo(null)
    isVisible = true
}

inline fun Component.onDoubleClick(crossinline block: MouseEvent.() -> Unit) {
    addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) {
            if (e?.clickCount == 2 && e.button == MouseEvent.BUTTON1) e.block()
        }
    })
}

/**
 * Similar to Utils.createAction but using Kotlin context. Can be used with any JComponent.
 */
fun <T: JComponent> T.createAction(
    iconPath: String? = null,
    name: String,
    description: String = name,
    keyCombo: KeyCombination? = null,
    block: T.() -> Unit
): AbstractAction {
    return object : AbstractAction() {
        init {
            if (iconPath != null) {
                putValue(SMALL_ICON, ResourceManager.getImageIcon(iconPath))
            }

            putValue(NAME, name)
            putValue(SHORT_DESCRIPTION, description)
            if (keyCombo != null) {
                keyCombo.withKeyStroke { putValue(ACCELERATOR_KEY,it)}
                this@createAction.bindTo(keyCombo, this)
            }
        }
        override fun actionPerformed(e: ActionEvent) {
            block()
        }
    }
}

/**
 * Create an action with a char rather than a key combinaation
 */
fun <T: JComponent> T.createAction(
    iconPath: String = "",
    name: String = "",
    description: String = name,
    keyPress: Char,
    block: T.() -> Unit
): AbstractAction {
    return createAction(iconPath, name, description, KeyCombination(keyPress), block)
}