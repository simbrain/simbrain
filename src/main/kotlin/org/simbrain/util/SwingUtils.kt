
package org.simbrain.util
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.widgets.DropDownTriangle
import org.simbrain.util.widgets.ProgressWindow
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import java.beans.PropertyChangeEvent
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.event.MenuEvent
import javax.swing.event.MenuListener
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.min


/**
 * Run [block] on the event dispatch thread and return its result, directly if already on it, else blocking via
 * [SwingUtilities.invokeAndWait]. Use for model mutations that Swing views observe (e.g. JFreeChart datasets).
 */
fun <T> runOnEventThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    val task = FutureTask { block() }
    SwingUtilities.invokeAndWait(task)
    return try {
        task.get()
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    }
}

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

/**
 * Runs [block] each time this menu is opened, so items can be rebuilt or their state refreshed before they are seen.
 */
fun JMenu.onMenuSelected(block: JMenu.() -> Unit) {
    addMenuListener(object : MenuListener {
        override fun menuSelected(e: MenuEvent?) = block()
        override fun menuDeselected(e: MenuEvent?) {}
        override fun menuCanceled(e: MenuEvent?) {}
    })
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

enum class MessageKind(val title: String, private val iconKey: String) {
    INFO("Information", "OptionPane.informationIcon"),
    WARNING("Warning", "OptionPane.warningIcon"),
    ERROR("Error", "OptionPane.errorIcon"),
    QUESTION("Question", "OptionPane.questionIcon");

    val icon: Icon? get() = UIManager.getIcon(iconKey)
}

private const val MESSAGE_WIDTH = 400
private const val TEXT_WIDTH = 640

/**
 * Show a message with an OK button. [text] wraps to fit and can be selected and copied; separate paragraphs with a
 * blank line. [heading] is an optional bold summary above the text, and [details] are optional label and value rows
 * below it, such as a file path or checksum.
 *
 * On the event dispatch thread this waits until the user closes the message. From any other thread it returns
 * immediately and the message appears on the event dispatch thread.
 */
@JvmOverloads
fun showMessage(
    text: String,
    kind: MessageKind = MessageKind.INFO,
    heading: String? = null,
    title: String = kind.title,
    details: List<Pair<String, String>> = listOf()
) {
    val show = { createMessageDialog(text, listOf("OK"), kind, heading, title, details).isVisible = true }
    if (SwingUtilities.isEventDispatchThread()) show() else SwingUtilities.invokeLater(show)
}

/**
 * Ask the user to confirm an action and return true if they click [okLabel]. Prefer a label naming the action,
 * such as "Download" or "Delete", to "OK".
 */
@JvmOverloads
fun showConfirm(
    text: String,
    okLabel: String = "OK",
    kind: MessageKind = MessageKind.QUESTION,
    heading: String? = null,
    title: String = kind.title,
    details: List<Pair<String, String>> = listOf()
): Boolean = showChoice(text, listOf(okLabel, "Cancel"), kind, heading, title, details) == 0

/**
 * Show a message with one button per option and return the index of the button clicked, or null if the dialog is
 * closed without choosing. The first option is the default button. Waits for the user from any thread.
 */
@JvmOverloads
fun showChoice(
    text: String,
    options: List<String>,
    kind: MessageKind = MessageKind.QUESTION,
    heading: String? = null,
    title: String = kind.title,
    details: List<Pair<String, String>> = listOf()
): Int? = runOnEventThread {
    var choice: Int? = null
    createMessageDialog(text, options, kind, heading, title, details) { choice = it }.isVisible = true
    choice
}

/**
 * The modal dialog shown by [showMessage], [showConfirm] and [showChoice], built but not yet visible. Clicking a
 * button passes its index to [onClose] and closes the dialog; closing it any other way passes null.
 */
fun createMessageDialog(
    text: String,
    options: List<String> = listOf("OK"),
    kind: MessageKind = MessageKind.INFO,
    heading: String? = null,
    title: String = kind.title,
    details: List<Pair<String, String>> = listOf(),
    onClose: (Int?) -> Unit = {}
): JDialog {
    val body = JPanel(MigLayout("insets 0, wrap 2, gapy 0", "[]16[]")).apply {
        heading?.let {
            val headingArea = wrappedText(it, Theme.heading)
            add(headingArea, "span, width ${headingArea.width}!, gapbottom 8")
        }
        val textArea = wrappedText(text)
        add(textArea, "span, width ${textArea.width}!")
        details.forEachIndexed { i, (label, value) ->
            val gap = if (i == 0) "gaptop 12" else "gaptop 4"
            add(JLabel(label).apply {
                font = Theme.body
                // A plain Color, since a look and feel refresh resets a ColorUIResource to the default foreground
                foreground = Color(Theme.mutedText.rgb)
            }, gap)
            add(JTextField(value).apply {
                isEditable = false
                isOpaque = false
                border = BorderFactory.createEmptyBorder()
                font = Font(Font.MONOSPACED, Font.PLAIN, Theme.body.size)
            }, gap)
        }
    }
    return dialogFrame(title, kind.icon, body, options) { dialog, choice ->
        onClose(choice)
        dialog.dispose()
    }
}

/**
 * Ask the user for a line of text and return it, or null if they cancel. [initValue] starts out selected.
 * Waits for the user from any thread.
 */
@JvmOverloads
fun showInputDialog(message: String, initValue: String? = "", title: String = "Input"): String? = runOnEventThread {
    var result: String? = null
    createInputDialog(message, initValue, title, parse = { it }) { result = it }.isVisible = true
    result
}

/**
 * Ask the user for a number and return it, or null if they cancel. The type of [initValue], Int or Double, is the
 * type returned. Text that is not a number of that type shows an error under the field and keeps the dialog open.
 */
fun <T : Number> showNumericInputDialog(message: String, initValue: T, title: String = "Input"): T? {
    @Suppress("UNCHECKED_CAST")
    val parse: (String) -> T? = when (initValue) {
        is Int -> { text -> text.trim().toIntOrNull() as T? }
        is Double -> { text -> text.trim().toDoubleOrNull() as T? }
        else -> throw IllegalArgumentException("Unsupported number type ${initValue::class.simpleName}")
    }
    val error = if (initValue is Int) "Enter a whole number." else "Enter a number."
    return runOnEventThread {
        var result: T? = null
        createInputDialog(message, initValue.toString(), title, error, parse) { result = it }.isVisible = true
        result
    }
}

/**
 * The modal dialog shown by [showInputDialog] and [showNumericInputDialog], built but not yet visible. OK passes the
 * field's text to [parse]. If that returns null, [error] appears under the field and the dialog stays open;
 * otherwise the parsed value goes to [onClose]. Cancelling passes null.
 */
fun <T> createInputDialog(
    message: String,
    initValue: String? = "",
    title: String = "Input",
    error: String = "",
    parse: (String) -> T?,
    onClose: (T?) -> Unit = {}
): JDialog {
    val field = JTextField(initValue).apply {
        font = Theme.large
        selectAll()
    }
    val errorLabel = JLabel(error).apply {
        font = Theme.body
        // A plain Color, since a look and feel refresh resets a ColorUIResource to the default foreground
        foreground = Color(Theme.errorText.rgb)
        isVisible = false
    }
    val body = JPanel(MigLayout("insets 0, wrap 1, gapy 0, hidemode 3")).apply {
        val prompt = wrappedText(message)
        add(prompt, "width ${prompt.width}!")
        add(field, "growx, wmin 320, gaptop 8")
        add(errorLabel, "gaptop 4")
    }
    return dialogFrame(title, null, body, listOf("OK", "Cancel"), focus = field) { dialog, choice ->
        if (choice == 0) {
            val value = parse(field.text)
            if (value == null) {
                errorLabel.isVisible = true
                dialog.pack()
                field.selectAll()
                field.requestFocusInWindow()
                return@dialogFrame
            }
            onClose(value)
        } else {
            onClose(null)
        }
        dialog.dispose()
    }
}

/**
 * A modal dialog with [body] beside [icon] and one button per option below it, the first being the default.
 * Clicking a button passes its index to [onButton]; Escape or closing the window passes null. [onButton] disposes
 * the dialog when it should close. [focus] gets the keyboard focus when the dialog opens, or else the first button.
 */
private fun dialogFrame(
    title: String,
    icon: Icon?,
    body: JComponent,
    options: List<String>,
    focus: JComponent? = null,
    onButton: (JDialog, Int?) -> Unit
): JDialog {
    val owner = activeWindow()
    val dialog = JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL)
    val buttons = options.mapIndexed { i, option ->
        JButton(option).apply { addActionListener { onButton(dialog, i) } }
    }
    dialog.contentPane = JPanel(MigLayout("insets 20 20 16 20", if (icon == null) "[grow]" else "[]20[grow]")).apply {
        icon?.let { add(JLabel(it), "aligny top") }
        add(body, "wrap")
        buttons.forEachIndexed { i, button ->
            add(button, if (i == 0) "span, split ${buttons.size}, alignx right, gaptop 16" else "")
        }
    }
    dialog.rootPane.defaultButton = buttons.firstOrNull()
    dialog.rootPane.registerKeyboardAction(
        { onButton(dialog, null) },
        KeyStroke.getKeyStroke("ESCAPE"),
        JComponent.WHEN_IN_FOCUSED_WINDOW
    )
    dialog.defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
    dialog.addWindowListener(object : WindowAdapter() {
        override fun windowOpened(e: WindowEvent?) {
            (focus ?: buttons.firstOrNull())?.requestFocusInWindow()
        }

        override fun windowClosing(e: WindowEvent?) = onButton(dialog, null)
    })
    if (owner == null) {
        dialog.isAlwaysOnTop = true
    }
    dialog.pack()
    dialog.setLocationRelativeTo(owner)
    return dialog
}

/**
 * Read-only wrapping text, sized to its longest line up to [maxWidth]. Unlike a label it shows `<` and `&`
 * literally and lets the user select and copy the text. Tab moves focus on rather than staying in the text.
 */
private fun wrappedText(
    text: String,
    textFont: Font = Theme.large,
    maxWidth: Int = MESSAGE_WIDTH
) = JTextArea(text).apply {
    isEditable = false
    isOpaque = false
    lineWrap = true
    wrapStyleWord = true
    border = BorderFactory.createEmptyBorder()
    margin = Insets(0, 0, 0, 0)
    font = textFont
    setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null)
    setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null)
    val longestLine = text.lines().maxOf { getFontMetrics(font).stringWidth(it) }
    setSize(min(longestLine + 1, maxWidth), Short.MAX_VALUE.toInt())
}

/**
 * The window new dialogs should be owned by and centered on, so on a multi-monitor setup they open on the same
 * screen as Simbrain rather than the primary screen.
 */
private fun activeWindow(): Window? = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
    ?: Frame.getFrames().firstOrNull { it.isVisible }

/**
 * Show long text, such as a stack trace or debug dump, in a non-modal dialog. The text wraps, can be selected, and
 * scrolls when it is too tall for the screen; a Copy button copies all of it. Prefer [showMessage] for short prose and [showResults] for
 * labelled values. Safe to call from any thread.
 */
fun showText(text: String, title: String) {
    SwingUtilities.invokeLater {
        val textArea = wrappedText(text.replace("\t", "    "), Theme.body, TEXT_WIDTH)
        val panel = JPanel(MigLayout("insets 16 20 16 20")).apply { add(textArea, "width ${textArea.width}!") }
        createDoneDialog(title, scrollIfTall(panel), text).isVisible = true
    }
}

/**
 * The receiver of the block passed to [showResults]. Each call to [section], [row] or [note] adds a line to the
 * dialog, in order: section headings, short notes, and rows of a label followed by one or more right-aligned values.
 */
class ResultsBuilder {

    internal sealed class Item {
        class Heading(val text: String, val columns: List<String>) : Item()
        class Note(val text: String) : Item()
        class Row(val label: String, val values: List<String>) : Item()
    }

    internal val items = mutableListOf<Item>()

    /**
     * The results as plain text, one line per heading, note or row, with values separated by tabs so they paste into
     * a spreadsheet as columns.
     */
    internal fun toText(): String = buildString {
        items.forEachIndexed { i, item ->
            when (item) {
                is Item.Heading -> {
                    if (i > 0) appendLine()
                    appendLine((listOf(item.text) + item.columns).joinToString("\t"))
                }
                is Item.Note -> appendLine(item.text)
                is Item.Row -> appendLine((listOf(item.label) + item.values).joinToString("\t"))
            }
        }
    }.trimEnd()

    /**
     * Start a section. [columns], when given, label the value columns of the rows that follow.
     */
    fun section(heading: String, columns: List<String> = listOf(), block: ResultsBuilder.() -> Unit = {}) {
        items.add(Item.Heading(heading, columns))
        block()
    }

    fun note(text: String) {
        items.add(Item.Note(text))
    }

    fun row(label: String, vararg values: String) {
        items.add(Item.Row(label, values.toList()))
    }
}

private fun resultsPanel(items: List<ResultsBuilder.Item>): JPanel {
    val valueColumns = items.maxOfOrNull {
        when (it) {
            is ResultsBuilder.Item.Row -> it.values.size
            is ResultsBuilder.Item.Heading -> it.columns.size
            is ResultsBuilder.Item.Note -> 0
        }
    } ?: 0
    val columnConstraints = "[left]" + "40[right]" + "24[right]".repeat((valueColumns - 1).coerceAtLeast(0))
    fun label(text: String, font: Font) = JLabel(text).apply { this.font = font }
    return JPanel(MigLayout("insets 20 24 12 24, wrap ${valueColumns + 1}", columnConstraints, "[]4[]")).apply {
        items.forEachIndexed { index, item ->
            when (item) {
                is ResultsBuilder.Item.Heading -> {
                    // Space each section after the first from the one above it
                    val gap = if (index == 0) "" else "gaptop 16"
                    if (item.columns.isEmpty()) {
                        add(label(item.text, Theme.heading), "span, $gap")
                    } else {
                        add(label(item.text, Theme.heading), gap)
                        item.columns.forEach { add(label(it, Theme.body), "aligny bottom, $gap") }
                        repeat(valueColumns - item.columns.size) { add(JLabel()) }
                    }
                }
                is ResultsBuilder.Item.Note -> add(label(item.text, Theme.body).apply {
                    foreground = Color(Theme.mutedText.rgb)
                }, "span")
                is ResultsBuilder.Item.Row -> {
                    add(label(item.label, Theme.large))
                    item.values.forEach { add(label(it, Theme.large.deriveFont(Font.BOLD))) }
                    repeat(valueColumns - item.values.size) { add(JLabel()) }
                }
            }
        }
    }
}

/**
 * Show labelled results, such as accuracies or distances, in a non-modal dialog that sizes itself to fit. Safe to
 * call from any thread, but [build] runs later on the event dispatch thread, so compute results first and only
 * format them inside [build].
 *
 * ```
 * showResults("Generalization accuracy") {
 *     section("Training set") {
 *         row("10,000 training images", "99.4%")
 *     }
 *     section("Testing sets") {
 *         row("Original", "94.6%")
 *         row("Shifted 2 pixels right", "56.4%")
 *     }
 * }
 * ```
 */
fun showResults(title: String, build: ResultsBuilder.() -> Unit) {
    SwingUtilities.invokeLater {
        createResultsDialog(title, build).isVisible = true
    }
}

/**
 * The dialog shown by [showResults], built but not yet visible. Its Copy button copies the results via
 * [ResultsBuilder.toText].
 */
fun createResultsDialog(title: String, build: ResultsBuilder.() -> Unit): StandardDialog {
    val results = ResultsBuilder().apply(build)
    return createDoneDialog(title, scrollIfTall(resultsPanel(results.items)), results.toText())
}

/**
 * [content] as is, or in a scroll pane [maxHeight] tall if it is taller, so a long list keeps the dialog's buttons
 * on screen. By default [maxHeight] is three quarters of the height of the screen Simbrain is on.
 */
private fun scrollIfTall(
    content: JComponent,
    maxHeight: Int = (activeWindow()?.graphicsConfiguration?.bounds?.height ?: 800) * 3 / 4
): JComponent {
    val size = content.preferredSize
    if (size.height <= maxHeight) return content
    return JScrollPane(content).apply {
        border = BorderFactory.createEmptyBorder()
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBar.unitIncrement = 16
        preferredSize = Dimension(size.width + verticalScrollBar.preferredSize.width, maxHeight)
    }
}

/**
 * A non-modal dialog showing [content], centered on the [activeWindow], with a Done button and a Copy button that
 * puts [copyText] on the clipboard.
 */
private fun createDoneDialog(title: String, content: Container, copyText: String): StandardDialog {
    val owner = activeWindow()
    return StandardDialog(owner, title).apply {
        contentPane = content
        isModal = false
        setAsDoneDialog()
        addButton(JButton("Copy").apply {
            toolTipText = "Copy all of the text to the clipboard"
            addActionListener {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(copyText), null)
                text = "Copied"
            }
        })
        pack()
        setLocationRelativeTo(owner)
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
 * Default cadence for coalescing GUI refreshes caused by very fast model updates.
 *
 * This was introduced to prevent language-model token generation from being paced by the display.
 * It is kept generic because other high-rate simulations can use the same protection.
 */
const val HIGH_RATE_GUI_REFRESH_INTERVAL_MS = 33L

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
