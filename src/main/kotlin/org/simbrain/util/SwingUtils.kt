package org.simbrain.util

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.widgets.DropDownTriangle
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.util.widgets.SimbrainTextArea
import java.awt.*
import java.awt.event.*
import java.beans.PropertyChangeEvent
import java.io.File
import javax.swing.*
import javax.swing.border.CompoundBorder
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.min


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

    // print(showDirectorySelectionDialog())
    // val userInput = showNumericInputDialog("Enter a number:", 1)
    val userInput = showNumericInputDialog("Enter a number:", 1.0)
    println("User entered: $userInput")
}

/**
 * Shows a dialog that lets you select a directory, then returns that directory path as a string.
 */
fun showDirectorySelectionDialog(approveButtonText: String = "Select Folder"): String? {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    return if (chooser.showDialog(null, approveButtonText) == JFileChooser.APPROVE_OPTION) {
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
fun <T : JComponent> T.displayInDialog(
    isModal: Boolean = true,
    parent: Component? = null,
    title: String = "Dialog",
    block: T.() -> Unit = {}
): StandardDialog {
    val dialog = StandardDialog(getParentFrame(parent) as Frame?, title)
    
    dialog.contentPane = this
    dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
    dialog.isModal = isModal
    
    if (isModal) {
        dialog.modalityType = Dialog.ModalityType.APPLICATION_MODAL
    }

    // Add Escape key binding
    dialog.rootPane.registerKeyboardAction(
        { dialog.dispose() },
        KeyStroke.getKeyStroke("ESCAPE"),
        JComponent.WHEN_IN_FOCUSED_WINDOW
    )

    dialog.addCommitTask { block() }
    dialog.makeVisible()
    return dialog
}

@JvmOverloads
fun JDialog.display(
    parent: Component? = null,
    isModal: Boolean = true
) {
    // Auto-detect parent if not specified
    val effectiveParent = parent ?: run {
        // Try to find the currently focused window as potential parent
        val focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusedWindow
        when (focusedWindow) {
            is JDialog, is JFrame -> focusedWindow
            else -> null
        }
    }
    
    // Set modality
    this.isModal = isModal
    if (isModal) {
        modalityType = Dialog.ModalityType.APPLICATION_MODAL
    }
    
    pack()
    setLocationRelativeTo(effectiveParent)
    
    // Make visible and ensure it gets focus
    SwingUtilities.invokeLater {
        isVisible = true
        toFront()
        requestFocus()
    }

}

inline fun Component.onDoubleClick(crossinline block: MouseEvent.() -> Unit) {
    addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) {
            if (e?.clickCount == 2 && e.button == MouseEvent.BUTTON1) e.block()
        }
    })
}

/**
 * The base createAction function. Requires a list of key combos. See other versions if only one or no keyboard shortcut was used.
 */
fun <T : JComponent> T.createAction(
    name: String? = null,
    description: String? = null,
    iconPath: String? = null,
    keyboardShortcuts: List<KeyCombination>,
    initBlock: AbstractAction.() -> Unit = {},
    coroutineScope: CoroutineScope? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend T.(e: ActionEvent) -> Unit
): AbstractAction {
    return object : AbstractAction() {
        init {
            if (!iconPath.isNullOrEmpty()) {
                putValue(SMALL_ICON, ResourceManager.getSmallIcon(iconPath))
            }

            putValue(NAME, name)
            putValue(SHORT_DESCRIPTION, description)
            keyboardShortcuts.forEach { keyCombo ->
                keyCombo.withKeyStroke { putValue(ACCELERATOR_KEY, it) }
                this@createAction.bindTo(keyCombo, this)
            }

            initBlock()
        }

        override fun actionPerformed(e: ActionEvent) {
            if (coroutineScope != null) {
                coroutineScope.launch(coroutineContext) { block(e) }
            } else if (this@createAction is CoroutineScope) {
                this@createAction.launch(coroutineContext) { block(e) }
            } else {
                runBlocking { block(e) }
            }
        }
    }
}

/**
 * The [createAction] with one or no keyboard shortcut.
 * @param initBlock called when creating the action. useful for conditional enabling.
 */
fun <T : JComponent> T.createAction(
    name: String? = null,
    description: String? = null,
    iconPath: String? = null,
    keyboardShortcut: KeyCombination? = null,
    initBlock: AbstractAction.() -> Unit = {},
    coroutineScope: CoroutineScope? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend T.(e: ActionEvent?) -> Unit
) = createAction(name, description, iconPath, keyboardShortcut?.let { listOf(it) } ?: listOf(), initBlock, coroutineScope, coroutineContext, block)

/**
 * Create an action when no JComponent available. Keyboard shortcuts are not possible.
 */
fun createAction(
    name: String? = null,
    iconPath: String? = null,
    description: String? = null,
    block: (e: ActionEvent?) -> Unit
): AbstractAction {
    return object : AbstractAction() {
        init {
            if (iconPath != null) {
                putValue(SMALL_ICON, ResourceManager.getSmallIcon(iconPath))
            }

            putValue(NAME, name)
            putValue(SHORT_DESCRIPTION, description)
        }

        override fun actionPerformed(e: ActionEvent?) {
            block(e)
        }
    }
}

/**
 * [createAction] with a char rather than a keyboard shortcut
 */
fun <T : JComponent> T.createAction(
    iconPath: String = "",
    name: String = "",
    description: String = "",
    keyboardShortcut: Char,
    initBlock: AbstractAction.() -> Unit = {},
    coroutineScope: CoroutineScope? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend T.(e: ActionEvent?) -> Unit
): AbstractAction {
    return createAction(
        iconPath = iconPath,
        name = name,
        description = description,
        keyboardShortcuts = listOf(KeyCombination(keyboardShortcut)),
        initBlock = initBlock,
        coroutineScope = coroutineScope,
        coroutineContext = coroutineContext,
        block = block
    )
}

/**
 * Helper function to extract the appropriate parent frame from a component.
 */
private fun getParentFrame(parent: Component?): JFrame? {
    return when (parent) {
        is JFrame -> parent
        is JDialog -> parent.owner as? JFrame
        else -> null
    }
}

/**
 * Shows a dialog for setting an editable object in an [AnnotatedPropertyEditor].
 *
 * The provided block is executed when closing the dialog.
 *
 * Add [display] after this call to create and display the dialog.
 */
@JvmOverloads
fun <E : EditableObject> E.createEditorDialog(
    titleName: String = "Edit " + name.convertCamelCaseToSpaces(),
    parent: Window? = null,
    block: (E) -> Unit = {}
): StandardDialog {
    val editor = AnnotatedPropertyEditor(listOf(this))
    val dialog = if (parent != null) {
        StandardDialog(parent, titleName)
    } else {
        StandardDialog(editor)
    }
    return dialog.apply {
        if (parent == null) {
            title = titleName
        }
        if (parent != null) {
            contentPane = editor
        }
        addCommitTask {
            editor.commitChanges()
            block(this@createEditorDialog)
        }
    }
}

@JvmOverloads
@JvmName("createEditorDialogFromList")
fun <E : EditableObject> List<E>.createEditorDialog(
    titleName: String = "Edit $size ${first()::class.simpleName?.convertCamelCaseToSpaces()}${if (size > 1) "s" else ""}",
    block: (List<E>) -> Unit = {}
): StandardDialog {
    val editor = AnnotatedPropertyEditor(this)
    return StandardDialog(editor).apply {
        title = titleName
        addCommitTask {
            editor.commitChanges()
            block(this@createEditorDialog)
        }
    }
}

/**
 * Creates an editor dialog that blocks until the user commits or cancels the dialog.
 */
suspend fun <T: EditableObject> T.showAPEOptionDialog(title: String) = let {
    val deferred = CompletableDeferred<T?>()
    it.createEditorDialog(title) { obj -> deferred.complete(obj) }
        .apply { addCloseTask { deferred.complete(null) } }
        .display()
    deferred
}.await()

@JvmOverloads
fun showWarningDialog(message: String?, title: String = "Warning!") {
    val dialog = JDialog()
    dialog.isAlwaysOnTop = true
    JOptionPane.showMessageDialog(dialog, message, title, JOptionPane.WARNING_MESSAGE)
}

@JvmOverloads
fun showErrorDialog(message: String?, title: String = "Error") {
    val dialog = JDialog()
    dialog.isAlwaysOnTop = true
    JOptionPane.showMessageDialog(dialog, message, title, JOptionPane.ERROR_MESSAGE)
}

@JvmOverloads
fun showInfoDialog(message: String?, title: String = "Information") {
    val dialog = JDialog()
    dialog.isAlwaysOnTop = true
    JOptionPane.showMessageDialog(dialog, message, title, JOptionPane.INFORMATION_MESSAGE)
}

/**
 * Like [showWarningDialog], but renders the message in a non-editable [JTextArea] so the user
 * can select and copy the text (e.g. to copy a command embedded in the message).
 */
fun showCopyableWarningDialog(message: String) {
    val textArea = JTextArea(message).apply {
        isEditable = false
        isOpaque = false
        font = UIManager.getFont("Label.font")
        border = null
    }
    val dialog = JDialog()
    dialog.isAlwaysOnTop = true
    JOptionPane.showMessageDialog(dialog, textArea, "Warning!", JOptionPane.WARNING_MESSAGE)
}

fun showWarningConfirmDialog(message: String): Int {
    val dialog = JDialog()
    dialog.isAlwaysOnTop = true
    return JOptionPane.showConfirmDialog(dialog, message, "Warning!", JOptionPane.WARNING_MESSAGE)
}

/**
 * Create a dialog that takes an input in a text field and returns it as a string.
 */
fun showInputDialog(message: String, initValue: String? = ""): String? {
    val dialog = JDialog()
    dialog.isAlwaysOnTop = true
    return JOptionPane.showInputDialog(dialog, message, initValue)
}

fun showOptionDialog(
    message: String,
    title: String,
    options: Array<String>,
    defaultOption: Int? = null
): Int {
    val dialog = JDialog()
    dialog.isAlwaysOnTop = true
    return JOptionPane.showOptionDialog(
        dialog,
        message,
        title,
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        options,
        defaultOption?.let { options[it] }
    )
}

/**
 * Shows a dialog with a scrollable grid of checkboxes and returns the labels of those
 * that are checked when the user clicks OK, or `null` if the user cancels.
 *
 * @param title           Dialog window title.
 * @param message         Optional explanatory label shown above the checkboxes.
 * @param items           Full list of item labels to display.
 * @param defaultSelected Items that should be pre-checked (defaults to all items).
 * @param columns         Number of columns in the checkbox grid (default 1).
 */
fun showCheckboxDialog(
    title: String,
    message: String = "",
    items: List<String>,
    defaultSelected: List<String> = items,
    columns: Int = 1
): List<String>? {
    val checkboxes = items.map { JCheckBox(it, it in defaultSelected) }

    val numRows = (items.size + columns - 1) / columns
    val grid = JPanel(GridLayout(numRows, columns, 8, 2))
    checkboxes.forEach { grid.add(it) }
    // Pad any empty cells in the last row
    repeat(numRows * columns - items.size) { grid.add(JLabel()) }

    val content = JPanel(BorderLayout(0, 8))
    if (message.isNotEmpty()) {
        content.add(JLabel("<html>$message</html>"), BorderLayout.NORTH)
    }
    content.add(JScrollPane(grid).apply {
        preferredSize = Dimension(columns * 180, minOf(numRows * 26 + 20, 480))
    }, BorderLayout.CENTER)

    val result = JOptionPane.showConfirmDialog(
        null, content, title,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
    )
    return if (result == JOptionPane.OK_OPTION)
        checkboxes.filter { it.isSelected }.map { it.text }
    else
        null
}

fun showMessageDialog(message: String, title: String, rows: Int = 10, columns: Int = 50) {
    val textArea = SimbrainTextArea().apply {
        text = message
        isEditable = false
        this.rows = rows
        this.columns = columns
    }
    val scrollPane = JScrollPane(textArea)
    SwingUtilities.invokeLater {
        StandardDialog().apply {
            this.title = title
            contentPane = scrollPane
            isModal = false
            setAsDoneDialog()
            makeVisible()
        }
    }
}

/**
 * Create a dialog that takes an input in a text field and returns it as a number.
 * Returns null if user cancels the input.
 *
 * Init value determines the type. Int and Double supported.
 */
fun <T: Number> showNumericInputDialog(message: String, initValue: T): T? {
    val dialog = JDialog()
    dialog.isAlwaysOnTop = true
    while (true) {
        val result = JOptionPane.showInputDialog(dialog, message, initValue)
        if (result == null) {
            // User cancelled
            return null
        }

        when (initValue) {
            is Int -> {
                try {
                    return Integer.parseInt(result) as T
                } catch (e: NumberFormatException) { /* continue below */ }
            }
            is Double -> {
                try {
                    return java.lang.Double.parseDouble(result) as T
                } catch (e: NumberFormatException) { /* continue below */ }
            }
            else -> {
                JOptionPane.showMessageDialog(dialog, "Unsupported number type!", "Error", JOptionPane.ERROR_MESSAGE)
                return null
            }
        }
        JOptionPane.showMessageDialog(dialog, "Please enter a valid number!", "Warning", JOptionPane.WARNING_MESSAGE)
    }
}

/**
 * Collapsable panel that uses a [DetailTriangle]
 */
class DetailTrianglePanel @JvmOverloads constructor(
    val contentPanel: JPanel,
    defaultOpen: Boolean = true,
    upLabel: String = "Settings",
    downLabel: String = upLabel,
    val topPanelComponent: JComponent? = null,
): JPanel(BorderLayout()) {

    val topPanel = JPanel(BorderLayout()).apply {
        val padding = BorderFactory.createEmptyBorder(5, 5, 10, 5)
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        alignmentX = CENTER_ALIGNMENT
        border = padding

        if (topPanelComponent != null) {
            add(topPanelComponent)
        }

        add(Box.createHorizontalStrut(30))
        add(Box.createHorizontalGlue())
    }.also {
        add(it, BorderLayout.NORTH)
    }

    val detailTriangle = DropDownTriangle(
        DropDownTriangle.UpDirection.LEFT,
        defaultOpen,
        upLabel,
        downLabel
    ).also {
        it.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(arg0: MouseEvent) {
                contentPanel.isVisible = it.isDown
                repaint()
                SwingUtilities.getWindowAncestor(this@DetailTrianglePanel)?.pack()
            }
        })
        topPanel.add(it)
    }

    init {
        contentPanel.isVisible = detailTriangle.isDown
        add(contentPanel, BorderLayout.CENTER)
    }

    fun setOpen(open: Boolean) {
        if (detailTriangle.isDown == open) return
        detailTriangle.setState(open)
        contentPanel.isVisible = open
        repaint()
        SwingUtilities.getWindowAncestor(this)?.pack()
    }

}

fun <C: JComponent> C.createApplyPanel(commitAction: suspend C.() -> Unit): JPanel {
    val component = this
    return JPanel().apply {
        layout = BorderLayout()
        add(component)
        JButton(createAction("Apply") { commitAction() }).also { applyButton ->
            add(JPanel(FlowLayout(FlowLayout.RIGHT)).also { it.add(applyButton) }, BorderLayout.SOUTH)
        }
    }
}

fun <O: EditableObject> AnnotatedPropertyEditor<O>.createApplyPanel(): JPanel {
    return this.createApplyPanel { commitChanges() }
}

val swingDispatcher get() = Dispatchers.Swing

/**
 * Custom JTabbedPane that automatically resizes to fit components while switching between tabs.
 */
class ResizableTabbedPane : JTabbedPane() {

    init {
        addChangeListener { resizeTabbedPane() }
    }

    private fun resizeTabbedPane() {
        if (selectedComponent != null) {
            // Calculate the preferred size of the currently selected tab
            val preferredSize: Dimension = selectedComponent.preferredSize

            // Fudge factor (TODO). Not sure why the extra height is needed.
            preferredSize.height += 30

            // Resize the tabbed pane
            setPreferredSize(preferredSize)

            // Resize and revalidate the parent container
            SwingUtilities.getWindowAncestor(this)?.pack()
        }
    }
}


object Swing : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        SwingUtilities.invokeLater(block)
    }
}

// Utility function to run suspend functions on the Swing EDT
fun swingInvokeLater(block: suspend () -> Unit) {
    // Launch a coroutine on the Swing context
    CoroutineScope(Swing).launch {
        block()
    }
}

fun runWithProgressWindow(maxIterations: Int, label: String = "Progress", batchSize: Int = 1, block: suspend (Int) -> Unit) {
    val progressWindow = ProgressWindow(maxIterations, label).apply {
        minimumSize = Dimension(300, 100)
        setLocationRelativeTo(null)
    }

    CoroutineScope(Swing).launch {
        var i = 0
        while (i < maxIterations) {
            progressWindow.value = i
            progressWindow.text = "$label: $i / $maxIterations"
            withContext(Dispatchers.Default) {
                repeat(min(batchSize, maxIterations - i)) {
                    block(i)
                    i++
                }
            }
        }
        progressWindow.close()
    }

}

/**
 * Invoke the provided block when the parent frame is closed.
 */
fun Container.onWindowClose(block: (evt: PropertyChangeEvent) -> Unit) {
    // Add property change listener to detect when the panel is disposed
    swingInvokeLater {
        addPropertyChangeListener("ancestor") { evt ->
            if (evt.newValue == null) {
                block(evt)
            }
        }
    }
}

fun Container.onWindowClose(block: Runnable) = onWindowClose {
    block.run()
}

/**
 * Apply the standard outer padding ([Theme.dialogBorder]) to a panel.
 * If the panel already has a border, the dialog padding wraps around it.
 */
fun JComponent.applyDialogPadding() {
    border = border?.let { CompoundBorder(Theme.dialogBorder(), it) }
        ?: Theme.dialogBorder()
}

/**
 * Horizontal row of components with [Theme.componentGap] between them.
 * Defaults to left-aligned; pass `align = FlowLayout.RIGHT` for a trailing row (e.g. OK/Cancel).
 */
@JvmOverloads
fun buttonRow(vararg components: Component, align: Int = FlowLayout.LEFT, gap: Int = Theme.componentGap): JPanel {
    val panel = JPanel(FlowLayout(align, gap, 0))
    panel.isOpaque = false
    for (c in components) panel.add(c)
    return panel
}

/**
 * Runs [action] on the EDT at most once per [intervalMs]: immediately when the interval has
 * elapsed (manual stepping stays responsive), otherwise as one trailing run when the window
 * closes, so the final state always lands. Coalesces per-iteration GUI work under fast update
 * loops instead of queueing a frame per iteration.
 */
class RateLimitedEdtAction(private val intervalMs: Long, private val action: () -> Unit) {

    private var last = 0L

    private val trailing = javax.swing.Timer(0) {
        last = System.currentTimeMillis()
        action()
    }.apply { isRepeats = false }

    operator fun invoke() {
        val now = System.currentTimeMillis()
        val elapsed = now - last
        if (elapsed >= intervalMs) {
            last = now
            action()
        } else if (!trailing.isRunning) {
            trailing.initialDelay = (intervalMs - elapsed).toInt().coerceAtLeast(1)
            trailing.restart()
        }
    }
}
