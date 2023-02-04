package org.simbrain.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.actions.ConditionallyEnabledAction
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.Component
import java.awt.event.*
import java.io.File
import javax.swing.*


inline fun StandardDialog.onClosed(crossinline block: (WindowEvent?) -> Unit) = apply {
    addWindowListener(object : WindowAdapter() {
        override fun windowClosed(e: WindowEvent?) {
            block(e)
        }
    })
}

fun showSaveDialog(
    initialDirectory: String = "",
    initialFileName: String? = null,
    block: File.() -> Unit
) {
    val chooser = SFileChooser(initialDirectory, "")
    val theFile = if (initialFileName != null) {
        chooser.showSaveDialog(initialFileName)
    } else {
        chooser.showSaveDialog()
    }
    if (theFile != null) {
        theFile.block()
    }
}

fun showOpenDialog(
    initialDirectory: String = "",
    extension: String? = null,
    block: File.() -> Unit
) {
    val chooser = SFileChooser(initialDirectory, "")
    if (extension != null) {
        chooser.addExtension(extension)
    }
    val theFile = chooser.showOpenDialog()
    if (theFile != null) {
        theFile.block()
    }
}

fun main() {
    // showOpenDialog(extension = "txt") {
    //     println(this.readText())
    // }
    // showSaveDialog("", "test.txt") {
    //     writeText("testing...")
    // }
    print(showDirectorySelectionDialog())
}

/**
 * Shows a dialog that lets you select a directory, then returns that directory path as a string.
 */
fun showDirectorySelectionDialog(): String? {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    return if (chooser.showDialog(null, "Select Folder") == JFileChooser.APPROVE_OPTION) {
        return if (chooser.selectedFile.isDirectory) {
            chooser.selectedFile.path
        } else {
            chooser.currentDirectory.path
        }
    } else {
        null
    }
}

/**
 * Place the panel in a [StandardDialog] and show the dialog.
 */
@JvmOverloads
fun <T : JPanel> T.displayInDialog(block: T.() -> Unit = {}): StandardDialog {
    val dialog = StandardDialog()
    dialog.contentPane = this
    dialog.makeVisible()
    dialog.addClosingTask { block() }
    return dialog
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
fun <T : JComponent> T.createAction(
    iconPath: String? = null,
    name: String? = null,
    description: String? = null,
    keyCombo: KeyCombination? = null,
    initBlock: AbstractAction.() -> Unit = {},
    block: T.(e: ActionEvent) -> Unit
): AbstractAction {
    return object : AbstractAction() {
        init {
            if (iconPath != null) {
                putValue(SMALL_ICON, ResourceManager.getImageIcon(iconPath))
            }

            putValue(NAME, name)
            putValue(SHORT_DESCRIPTION, description)
            if (keyCombo != null) {
                keyCombo.withKeyStroke { putValue(ACCELERATOR_KEY, it) }
                this@createAction.bindTo(keyCombo, this)
            }

            initBlock()
        }

        override fun actionPerformed(e: ActionEvent) {
            block(e)
        }
    }
}

/**
 * Create an action when no JComponent available. Key combo's not possible.
 */
fun createAction(
    iconPath: String? = null,
    name: String? = null,
    description: String? = null,
    block: (e: ActionEvent) -> Unit
): AbstractAction {
    return object : AbstractAction() {
        init {
            if (iconPath != null) {
                putValue(SMALL_ICON, ResourceManager.getImageIcon(iconPath))
            }

            putValue(NAME, name)
            putValue(SHORT_DESCRIPTION, description)
        }

        override fun actionPerformed(e: ActionEvent) {
            block(e)
        }
    }
}

fun <T> T.createSuspendAction(
    iconPath: String? = null,
    name: String? = null,
    description: String? = null,
    keyCombo: KeyCombination? = null,
    block: suspend T.(e: ActionEvent) -> Unit
): AbstractAction where T : JComponent, T : CoroutineScope {
    return object : AbstractAction() {
        init {
            if (iconPath != null) {
                putValue(SMALL_ICON, ResourceManager.getImageIcon(iconPath))
            }

            putValue(NAME, name)
            putValue(SHORT_DESCRIPTION, description)
            if (keyCombo != null) {
                keyCombo.withKeyStroke { putValue(ACCELERATOR_KEY, it) }
                this@createSuspendAction.bindTo(keyCombo, this)
            }
        }

        override fun actionPerformed(e: ActionEvent) {
            launch { block(e) }
        }
    }
}

fun NetworkPanel.createConditionallyEnabledAction(
    iconPath: String? = null,
    name: String,
    enablingCondition: ConditionallyEnabledAction.EnablingCondition,
    description: String = name,
    keyCombo: KeyCombination? = null,
    block: NetworkPanel.() -> Unit
): AbstractAction {
    return object : ConditionallyEnabledAction(this, name, enablingCondition) {
        init {
            if (iconPath != null) {
                putValue(SMALL_ICON, ResourceManager.getImageIcon(iconPath))
            }

            putValue(NAME, name)
            putValue(SHORT_DESCRIPTION, description)
            if (keyCombo != null) {
                keyCombo.withKeyStroke { putValue(ACCELERATOR_KEY, it) }
                this@createConditionallyEnabledAction.bindTo(keyCombo, this)
            }
        }

        override fun actionPerformed(e: ActionEvent) {
            block()
        }
    }
}

/**
 * Create an action with a char rather than a key combination
 */
fun <T : JComponent> T.createAction(
    iconPath: String = "",
    name: String = "",
    description: String = "",
    keyCombo: Char,
    initBlock: AbstractAction.() -> Unit = {},
    block: T.(e: ActionEvent) -> Unit
): AbstractAction {
    return createAction(iconPath, name, description, KeyCombination(keyCombo), initBlock, block)
}

/**
 * Shows a dialog for setting an editable object in an [AnnotatedPropertyEditor]. The provided block is executed when
 * closing the dialog.
 */
fun <E : EditableObject> E.createDialog(block: (E) -> Unit): StandardDialog {
    val editor = AnnotatedPropertyEditor(this)
    return StandardDialog(editor).apply {
        addClosingTask {
            editor.commitChanges()
            block(editor.editedObject as E)
        }
    }
}

fun showWarningDialog(message: String) {
    val dialog = JDialog()
    dialog.isAlwaysOnTop = true
    JOptionPane.showMessageDialog(dialog, message, "Warning!", JOptionPane.WARNING_MESSAGE)
}
