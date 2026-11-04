package org.simbrain.util

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.jfree.chart.LegendItem
import org.jfree.chart.LegendItemCollection
import org.jfree.chart.title.LegendTitle
import org.jfree.chart.ui.Layer
import org.jfree.chart.ui.RectangleEdge
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.network.smile.SimbrainClassifier
import org.simbrain.plot.ChartSeriesPalette
import org.simbrain.plot.applySimbrainChartTheme
import org.simbrain.plot.projection.DecisionRegionAnnotation
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.plot.projection.ProjectionDesktopComponent
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.util.projection.AuxDataColoringManager
import org.simbrain.util.projection.CoordinateProjection
import org.simbrain.util.projection.DataPoint
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.WorkspacePreferences
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.gui.constrainToAspectLock
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.io.FileInputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Utility to make it easy to set location of a [SimbrainDesktop] component.
 * Note: these are the width and height of the visible window. In an odor world, for example, the width of the
 * world itself in pixels might be much larger.
 */
data class Placement(
    var location: Point? = null,
    var width: Int? = null,
    var height: Int? = null
)

suspend fun SimbrainDesktop.place(workspaceComponent: WorkspaceComponent, placement: suspend Placement.() -> Unit) {
    val (location, width, height) = Placement().apply { placement() }
    withContext(Dispatchers.Main) {
        val desktopComponent = getDesktopComponent(workspaceComponent)
        val frame = desktopComponent.parentFrame
        val bounds = frame.bounds
        frame.bounds = frame.constrainToAspectLock(
            Rectangle(
                location?.x ?: bounds.x,
                location?.y ?: bounds.y,
                width ?: bounds.width,
                height ?: bounds.height
            )
        )
    }
}

suspend fun SimbrainDesktop.place(workspaceComponent: WorkspaceComponent, x: Int, y: Int, width: Int, height: Int) {
    withContext(Dispatchers.Main) {
        val frame = getDesktopComponent(workspaceComponent).parentFrame
        frame.bounds = frame.constrainToAspectLock(Rectangle(x, y, width, height))
    }
}

/**
 * Place [component] immediately to the right of [reference], falling back to the reference's left edge only when
 * the new component would otherwise be completely offscreen.
 */
suspend fun SimbrainDesktop.placeBeside(component: WorkspaceComponent, reference: WorkspaceComponent) {
    val referenceBounds = getDesktopComponent(reference).parentFrame.bounds
    val componentFrame = getDesktopComponent(component).parentFrame
    val rightX = referenceBounds.x + referenceBounds.width + 20
    val x = if (rightX < desktopPane.width) rightX else referenceBounds.x
    withContext(Dispatchers.Swing) {
        componentFrame.setLocation(x, referenceBounds.y)
    }
}

suspend fun SimulationScope.place(workspaceComponent: WorkspaceComponent, x: Int, y: Int, width: Int, height: Int) {
    withGui {
        val frame = getDesktopComponent(workspaceComponent).parentFrame
        frame.bounds = frame.constrainToAspectLock(Rectangle(x, y, width, height))
    }
}

class ControlPanelKt(title: String = "Control Panel"):
    GenericJInternalFrame(title, true, true, false, false), CoroutineScope {

    @Transient
    private var job = SupervisorJob()

    @Transient
    override var coroutineContext = Dispatchers.Default + job

    /**
     * Default context for button and checkbox tasks. Runs them one at a time in click order, so that
     * two rapid clicks cannot interleave their writes to shared model state (e.g. two pattern buttons
     * each looping over the same neurons). A task that suspends releases the thread, so a queued task
     * (e.g. a stop button) can still run while a long task awaits workspace iteration. Pass an explicit
     * context with its own dispatcher to opt a task out of this serialization.
     */
    @Transient
    private val buttonTaskContext = Dispatchers.Default.limitedParallelism(1)

    val centralPanel = JPanel(BorderLayout()).apply {
        val scrollPane = JScrollPane(this)
        scrollPane.border = null
        this@ControlPanelKt.add(BorderLayout.CENTER, this)
    }

    override fun removeNotify() {
        super.removeNotify()
        // Cancel pending button actions when the panel is removed (e.g. when the workspace is cleared) so they
        // do not outlive the simulation and pop up dialogs over a different one.
        job.cancel()
    }

    val tabbedPane by lazy {
        JTabbedPane()
    }

    fun addTab(tabName: String) {
        launch(Dispatchers.Swing) {
            // If tabbedPane is not yet the main component
            if (centralPanel.components.none { it == tabbedPane }) {
                centralPanel.remove(mainPanel)
                // TODO: Make it possible to NOT have a main panel
                tabbedPane.addTab("Main", mainPanel)
                centralPanel.add(tabbedPane)
                centralPanel.revalidate()
                centralPanel.repaint()
            }
            tabbedPane.addTab(tabName, LabelledItemPanel())
        }
    }

    val mainPanel = LabelledItemPanel().apply {
        centralPanel.add(this)
    }

    fun getTab(name: String?): LabelledItemPanel = (0 until tabbedPane.tabCount)
        .firstOrNull { tabbedPane.getTitleAt(it) == name }
        ?.let { tabbedPane.getComponentAt(it) as? LabelledItemPanel }
        ?: mainPanel
    
    

    fun addComponent(component: JComponent, tab: String? = null) {
        launch(Dispatchers.Swing) { getTab(tab).addItem(component) }
    }

    fun addSeparator(tab: String? = null) {
        launch(Dispatchers.Swing) { getTab(tab).addItem(JSeparator(SwingConstants.HORIZONTAL)) }
    }

    fun addLabel(text: String, tab: String? = null) = JLabel(text).also {
        launch(Dispatchers.Swing) {
            getTab(tab).addItem(it)
            pack()
        }
    }

    /**
     * @param toolTip shown on both the text and its label
     */
    fun addLabelledText(label: String, text: String, tab: String? = null, toolTip: String? = null) = JLabel(text).also {
        it.toolTipText = toolTip
        launch(Dispatchers.Swing) {
            getTab(tab).addItem(label, it).toolTipText = toolTip
        }
    }

    fun addButton(
        label: String,
        context: CoroutineContext = EmptyCoroutineContext,
        tab: String? = null,
        task: suspend JButton.(ActionEvent) -> Unit
    ) = JButton(label).also { button ->
        button.addActionListener {
            UserEdits.report()
            launch(buttonTaskContext + context) { button.task(it) }
        }
        launch(Dispatchers.Swing) {
            getTab(tab).addItem(button)
            pack()
        }
    }

    fun addTextField(
        label: String,
        initValue: String,
        tab: String? = null,
        toolTip: String? = null,
        labelToolTip: String? = null,
        onChange: (String) -> Unit = {},
    ) =
        JTextField(initValue).also { textField ->
            textField.toolTipText = toolTip
            textField.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) {
                    onChange(textField.text)
                }

                override fun removeUpdate(e: DocumentEvent?) {
                    onChange(textField.text)
                }

                override fun changedUpdate(e: DocumentEvent?) {
                    onChange(textField.text)
                }
            })
            launch(Dispatchers.Swing) {
                getTab(tab).addItem(label, textField).toolTipText = labelToolTip ?: toolTip
            }
        }

    inline fun <reified T : Any> addFormattedNumericTextField(
        label: String,
        initValue: T,
        columns: Int = 8,
        maximumFractionDigits: Int = 12,
        tab: String? = null,
        toolTip: String? = null,
        crossinline onChange: (T) -> Unit
    ) = JFormattedTextField(initValue).also { textField ->
        textField.toolTipText = toolTip
        textField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                onChange(textField.value as T)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                onChange(textField.value as T)
            }

            override fun changedUpdate(e: DocumentEvent?) {
                onChange(textField.value as T)
            }
        })
        launch(Dispatchers.Swing) {
            val format = if (T::class == Int::class || T::class == Long::class || T::class == Short::class) {
                NumberFormat.getIntegerInstance(textField.getLocale())
            } else {
                NumberFormat.getNumberInstance(textField.getLocale())
                    .apply { this.maximumFractionDigits = maximumFractionDigits }
            } as DecimalFormat
            val formatterEditor = NumberFormatter(format)
            formatterEditor.valueClass = T::class.java
            val factory = DefaultFormatterFactory(formatterEditor)
            textField.setFocusLostBehavior(JFormattedTextField.PERSIST)
            textField.columns = columns
            textField.isEditable = true
            textField.setFormatterFactory(factory)
            textField.addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent) {
                    if (textField.isValid) {
                        textField.commitEdit()
                    }
                }
            })
            getTab(tab).addItem(label, textField).toolTipText = toolTip
        }
    }

    /**
     * @param toolTip shown on both the combo box and its label
     * @param labelToolTip overrides [toolTip] on the label
     */
    inline fun <reified T> addComboBox(
        label: String,
        items: List<T>,
        initialSelection: T? = null,
        tab: String? = null,
        labelToolTip: String? = null,
        toolTip: String? = null,
        crossinline onChange: (T) -> Unit = {}
    ) = JComboBox(items.toTypedArray()).also { comboBox ->
        comboBox.toolTipText = toolTip
        if (initialSelection != null) {
            comboBox.selectedItem = initialSelection
        }
        comboBox.addActionListener {
            UserEdits.report()
            @Suppress("UNCHECKED_CAST")
            onChange(comboBox.selectedItem as T)
        }
        launch(Dispatchers.Swing) {
            if (label.isEmpty()) {
                getTab(tab).addItem(comboBox)
            } else {
                getTab(tab).addItem(label, comboBox).toolTipText = labelToolTip ?: toolTip
            }
            pack()
        }
    }

    fun addSlider(
        label: String,
        minValue: Double,
        maxValue: Double,
        initValue: Double,
        increment: Double = 0.1,
        tab: String? = null,
        toolTip: String? = null,
        onChange: suspend (Double) -> Unit = {}
    ) = JSlider(
        (minValue / increment).toInt(),
        (maxValue / increment).toInt(),
        (initValue / increment).toInt()
    ).also { slider ->
        slider.toolTipText = toolTip
        slider.paintTicks = true
        slider.paintLabels = true

        // Set specific labels for min, max, and mid values
        val labelTable = Hashtable<Int, JLabel>()
        val minLabelValue = (minValue / increment).toInt()
        val midLabelValue = ((minValue + maxValue) / (2 * increment)).toInt()
        val maxLabelValue = (maxValue / increment).toInt()

        labelTable[minLabelValue] = JLabel(String.format("%.1f", minValue))
        labelTable[midLabelValue] = JLabel(String.format("%.1f", (minValue + maxValue) / 2))
        labelTable[maxLabelValue] = JLabel(String.format("%.1f", maxValue))

        slider.labelTable = labelTable
        slider.snapToTicks = true

        slider.addChangeListener {
            launch {
                onChange(slider.value * increment)
            }
        }

        launch(Dispatchers.Swing) {
            getTab(tab).addItem(label, slider).toolTipText = toolTip
            pack()
        }
    }

    /**
     * Adds a slider with a synchronized text field showing the current value.
     * Both the slider and text field can be used to change the value.
     */
    fun addSliderWithTextField(
        label: String,
        minValue: Double,
        maxValue: Double,
        initValue: Double,
        increment: Double = 0.1,
        showValueField: Boolean = false,
        showTicks: Boolean = false,
        textFieldColumns: Int = 5,
        tab: String? = null,
        toolTip: String? = null,
        onChange: suspend (Double) -> Unit = {}
    ): JPanel {
        val slider = JSlider(
            (minValue / increment).toInt(),
            (maxValue / increment).toInt(),
            (initValue / increment).toInt()
        )
        slider.toolTipText = toolTip
        if (showTicks) {
            slider.paintTicks = true
            slider.paintLabels = true
            val minSliderValue = (minValue / increment).toInt()
            val maxSliderValue = (maxValue / increment).toInt()
            val majorTickSpacing = ((maxSliderValue - minSliderValue) / 4).coerceAtLeast(1)
            slider.majorTickSpacing = majorTickSpacing
            slider.minorTickSpacing = (majorTickSpacing / 2).coerceAtLeast(1)
            slider.labelTable = Hashtable<Int, JLabel>().apply {
                put(minSliderValue, JLabel(String.format("%.1f", minValue)))
                put((minSliderValue + maxSliderValue) / 2, JLabel(String.format("%.1f", (minValue + maxValue) / 2)))
                put(maxSliderValue, JLabel(String.format("%.1f", maxValue)))
            }
        }

        val valueSpinner = if (showValueField) {
            JFormattedTextField(NumberFormat.getNumberInstance().apply {
                minimumFractionDigits = 1
                maximumFractionDigits = 2
            }).apply {
                columns = textFieldColumns
                value = initValue
                toolTipText = toolTip
            }
        } else null
        
        val labelComponent = JLabel(label)

        // Create a panel to hold both slider and text field. Enabling or disabling it applies to the whole row.
        val panel = object : JPanel() {
            override fun setEnabled(enabled: Boolean) {
                super.setEnabled(enabled)
                slider.isEnabled = enabled
                valueSpinner?.isEnabled = enabled
                labelComponent.isEnabled = enabled
            }
        }
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.add(slider)
        valueSpinner?.let {
            it.maximumSize = Dimension(it.preferredSize.width, it.preferredSize.height)
            it.alignmentY = 0.5f
            panel.add(Box.createHorizontalStrut(10))
            panel.add(it)
        }
        
        // Set while the slider and the text field write their value into each other, so each change is reported once
        var syncingField = false
        var syncingSlider = false

        // Slider updates text field and calls onChange
        slider.addChangeListener {
            if (syncingSlider) return@addChangeListener
            val value = slider.value * increment
            syncingField = true
            valueSpinner?.value = value
            syncingField = false
            launch {
                onChange(value)
            }
        }
        
        valueSpinner?.addPropertyChangeListener("value") {
            if (syncingField) return@addPropertyChangeListener
            val value = (valueSpinner.value as? Number)?.toDouble() ?: initValue
            val clampedValue = value.coerceIn(minValue, maxValue)
            syncingSlider = true
            slider.value = (clampedValue / increment).toInt()
            syncingSlider = false
            launch {
                onChange(clampedValue)
            }
        }
        
        launch(Dispatchers.Swing) {
            labelComponent.toolTipText = toolTip
            getTab(tab).addItemLabel(labelComponent, panel)
            pack()
        }
        return panel
    }

    /**
     * @param toolTip shown on both the check box and its label
     */
    fun addCheckBox(
        label: String,
        checked: Boolean,
        tab: String? = null,
        toolTip: String? = null,
        onChange: suspend (Boolean) -> Unit = {}
    ) =
        JCheckBox().also { checkBox ->
            checkBox.toolTipText = toolTip
            checkBox.addActionListener {
                UserEdits.report()
                launch(buttonTaskContext) {
                    onChange(checkBox.isSelected)
                }
            }
            launch(Dispatchers.Swing) {
                checkBox.isSelected = checked
                getTab(tab).addItem(label, checkBox).toolTipText = toolTip
                pack()
            }
        }

    fun <O : EditableObject> addAnnotatedPropertyEditor(editor: AnnotatedPropertyEditor<O>, tab: String? = null) {
        launch(Dispatchers.Swing) { getTab(tab).addItem(editor) }
    }

    /**
     * Add a horizontal row of components using a DSL builder.
     * Can optionally have a label on the left side.
     * 
     * Example:
     * ```
     * addRow("Object") {
     *     addButton("Move") { ... }
     *     addButton("Train") { ... }
     * }
     * ```
     */
    fun addRow(label: String? = null, tab: String? = null, init: RowBuilder.() -> Unit) {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        val builder = RowBuilder(this, panel)
        builder.init()
        
        if (label != null) {
            launch(Dispatchers.Swing) {
                getTab(tab).addItem(label, panel)
                pack()
            }
        } else {
            addComponent(panel, tab)
        }
    }

    /**
     * Builder for adding components horizontally in a row.
     */
    inner class RowBuilder(
        private val parent: ControlPanelKt,
        private val panel: JPanel
    ) {
        fun addButton(
            label: String,
            context: CoroutineContext = EmptyCoroutineContext,
            task: suspend JButton.(ActionEvent) -> Unit
        ) = JButton(label).also { button ->
            button.addActionListener {
                UserEdits.report()
                parent.launch(parent.buttonTaskContext + context) { button.task(it) }
            }
            panel.add(button)
        }
        
        fun addComponent(component: JComponent) {
            panel.add(component)
        }
    }

}

suspend fun SimbrainDesktop.loadWorkspaceZipFromFileChooser(): Boolean {
    val simulationChooser = SFileChooser(WorkspacePreferences.simulationDirectory, "Zip Archive", "zip")
    val simFile = simulationChooser.showOpenDialog()
    val serializer = WorkspaceSerializer(workspace)

    // not calling clearWorkspace because control panels created by simulations should be left in place
    if (simFile != null) {
        workspace.removeAllComponents()
        workspace.updater.updateManager.reset()
        workspace.couplingManager.clear()
        withContext(Dispatchers.IO) {
            serializer.deserialize(FileInputStream(simFile))
        }
        return true
    }
    return false
}

/**
 * Colors for classes in classifier plots: the Okabe-Ito palette, which stays distinguishable under common color
 * vision deficiencies, starting with blue and orange so that two-class plots have the strongest contrast.
 */
val classifierColors = listOf(4, 0, 2, 6, 5, 1, 3, 7).map { ChartSeriesPalette.OKABE_ITO.colors[it] }

fun classifierColor(classIndex: Int) = classifierColors[Math.floorMod(classIndex, classifierColors.size)]

/**
 * Plot a classifier's data colored by true class, with training points filled and test points hollow, and, as the
 * network runs, each new input colored by the predicted class, so an input that lands among points of another color
 * is easy to spot.
 *
 * For classifiers with two inputs the plot uses the raw inputs as axes and shades the background by the class the
 * model predicts at each location, so the decision regions are visible and misclassified points stand out.
 */
suspend fun SimbrainDesktop.createClassifierProjectionPlot(
    smileClassifier: ClassifierNetwork,
    name: String = "${smileClassifier.displayName} Plot"
): ProjectionComponent {
    val showRegions = smileClassifier.inputSize == 2
    val projectionComponent = ProjectionComponent(name).apply {
        projector.coloringManager = AuxDataColoringManager()
        // Keep every training point, even ones that nearly coincide
        projector.tolerance = 1e-9
        if (showRegions) {
            projector.projectionMethod = CoordinateProjection()
        }
    }
    fun plotData(classifier: SimbrainClassifier) = with(projectionComponent.projector) {
        synchronized(dataset) {
            dataset.clear()
            events.datasetCleared.fire()
        }
        // From the whole dataset, so a class absent from one split still gets its own color
        val labelOrder = classifier.dataset.targets.distinct().sorted()
        listOf(classifier.trainingData to false, classifier.testingData to true).forEach { (data, hollow) ->
            data.inputs.zip(data.targets).forEach { (input, target) ->
                val point = DataPoint(input.toDoubleArray(), aux = classifierColor(labelOrder.indexOf(target)))
                point.hollow = hollow
                addDataPoint(point)
            }
        }
        dataset.currentPoint = null
        events.datasetChanged.fire()
    }
    plotData(smileClassifier.classifier)
    val regions = if (showRegions) {
        DecisionRegionAnnotation(
            scope = workspace,
            modelKey = { smileClassifier.classifier.model },
            isActive = {
                (projectionComponent.projector.projectionMethod as? CoordinateProjection)
                    ?.let { it.dim1 == 0 && it.dim2 == 1 } == true
            },
            colors = List(smileClassifier.outputSize) { classifierColor(it) }
        ) { point ->
            with(smileClassifier.classifier) { model?.let { classIndex(it.predict(point)) } }
        }
    } else null
    var lastClassifier = smileClassifier.classifier
    var lastModel = lastClassifier.model
    val deregisterDeleteEvent = smileClassifier.events.deleted.on {
        workspace.removeWorkspaceComponent(projectionComponent)
    }
    val deregisterUpdateEvent = smileClassifier.events.updated.on(Dispatchers.Default) {
        val classifier = smileClassifier.classifier
        if (classifier !== lastClassifier) {
            lastClassifier = classifier
            plotData(classifier)
        }
        val model = classifier.model
        if (model !== lastModel) {
            lastModel = model
            regions?.refresh()
        }
        if (model == null || smileClassifier.winner == Integer.MIN_VALUE) return@on
        val datapoint = DataPoint(
            smileClassifier.inputNeuronGroup.activationArray,
            aux = classifierColor(classifier.classIndex(smileClassifier.winner))
        )
        projectionComponent.projector.addDataPoint(datapoint)
    }
    workspace.addWorkspaceComponent(
        projectionComponent
    )
    (getDesktopComponent(projectionComponent) as ProjectionDesktopComponent).apply {
        projector.dataset.currentPoint = null
        chart.apply {
            regions?.let { xyPlot.renderer.addAnnotation(it, Layer.BACKGROUND) }
            if (showRegions) {
                val (xLabel, yLabel) = smileClassifier.inputNeuronGroup.neuronList.map { it.label?.takeIf { it.isNotBlank() } }
                xyPlot.domainAxis.label = xLabel ?: "Input 1"
                xyPlot.rangeAxis.label = yLabel ?: "Input 2"
            }
            xyPlot.fixedLegendItems = LegendItemCollection().apply {
                val marker = java.awt.geom.Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0)
                smileClassifier.outputNeuronGroup.labelArray.forEachIndexed { i, label ->
                    add(LegendItem(label, null, null, null, marker, classifierColor(i)))
                }
                add(LegendItem("Training", null, null, null, marker, Color.GRAY))
                add(LegendItem("Test", null, null, null, marker, BasicStroke(2f), Color.GRAY))
            }
            addLegend(LegendTitle(xyPlot).apply { position = RectangleEdge.BOTTOM })
            applySimbrainChartTheme()
        }
    }
    lateinit var removalListener: Job
    removalListener = workspace.events.componentRemoved.on {
        if (it == projectionComponent) {
            deregisterDeleteEvent()
            deregisterUpdateEvent.cancel()
            removalListener.cancel()
        }
    }
    return projectionComponent
}

fun createDropdownButton(icon: Icon, actions: List<Action>, toolTip: String? = null): JButton {
    val button = JButton().apply {
        this.icon = icon
        text = "▾"
        horizontalTextPosition = SwingConstants.RIGHT
        verticalTextPosition = SwingConstants.CENTER
        if (toolTip != null) {
            toolTipText = toolTip
        }
    }
    
    val popupMenu = JPopupMenu()
    for (action in actions) {
        popupMenu.add(action)
    }
    
    button.addActionListener { e ->
        val btn = e.source as JButton
        popupMenu.show(btn, 0, btn.height)
    }
    
    button.componentPopupMenu = popupMenu
    return button
}

context(SimbrainDesktop)
suspend inline fun <reified T : DesktopComponent<*>> WorkspaceComponent.getDesktopComponentAs(): T {
    return getDesktopComponent(this) as T
}
