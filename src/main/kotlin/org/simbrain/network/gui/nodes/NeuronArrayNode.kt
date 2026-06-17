package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.Layer
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.randomizeBiases
import org.simbrain.network.gui.*
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.*
import org.simbrain.util.piccolo.SimbrainImage
import org.simbrain.util.piccolo.addBorder
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import org.simbrain.workspace.couplings.getConsumer
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.event.ActionEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JMenu
import javax.swing.JPopupMenu
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * The current pnode representation for all [Layer] objects. May be broken out into subtypes for different
 * subclasses of Layer.
 */
class NeuronArrayNode(networkPanel: NetworkPanel, val neuronArray: NeuronArray) :
    ArrayLayerNode(networkPanel, neuronArray) {

    val imageNodeGroup = PNode().apply {
        if (!neuronArray.circleMode) {
            mainNode.addChild(this)
        }
    }

    val neuronCircleGroup = PNode().apply {
        if (neuronArray.circleMode) {
            mainNode.addChild(this)
        }
    }

    val neuronCircles by lazy {
        neuronArray.activationArray.mapIndexed { i, activation ->
            NeuronCircleNode(networkPanel).also { circle ->
                circle.drawActivation(activation, neuronArray.updateRule.graphicalBounds)
                circle.addInputEventListener(object : PBasicInputEventHandler() {
                    override fun mouseEntered(event: PInputEvent) {
                        networkPanel.updateNeuronArrayTrace(neuronArray, i)
                    }
                    override fun mouseExited(event: PInputEvent) {
                        networkPanel.clearNeuronArrayTrace()
                    }
                    override fun mousePressed(event: PInputEvent) {
                        if (!event.isAltDown) return
                        selectPixel(i, addToSelection = event.isShiftDown)
                        if (this@NeuronArrayNode !in networkPanel.selectionManager) {
                            networkPanel.selectionManager.add(this@NeuronArrayNode)
                        }
                        event.isHandled = true
                    }
                })
            }
        }.onEach {
            neuronCircleGroup.addChild(it)
        }
    }

    private fun layoutNeuronCircles() {
        if (!neuronArray.circleMode) return
        val ncol = if (neuronArray.gridMode) {
            ceil(sqrt(neuronArray.size.toDouble())).toInt()
        } else if (neuronArray.verticalLayout) {
            1
        } else {
            neuronArray.size
        }
        val offset = 50.0
        neuronCircles.forEachIndexed { i, circle ->
            val row = i / ncol
            val col = i % ncol
            circle.setOffset(
                col * offset,
                row * offset
            )
        }
    }

    /**
     * Main pixel image for activations.
     */
    protected val activationImage = SimbrainImage().apply {
        imageNodeGroup.addChild(this)
        pickable = true
        addInputEventListener(object : PBasicInputEventHandler() {
            override fun mouseMoved(event: PInputEvent) {
                val idx = pixelToNeuronIndex(event.getPositionRelativeTo(this@apply)) ?: return
                networkPanel.updateNeuronArrayTrace(neuronArray, idx)
            }
            override fun mouseExited(event: PInputEvent) {
                networkPanel.clearNeuronArrayTrace()
            }
            override fun mousePressed(event: PInputEvent) {
                if (!event.isAltDown) return
                val idx = pixelToNeuronIndex(event.getPositionRelativeTo(this@apply)) ?: return
                selectPixel(idx, addToSelection = event.isShiftDown)
                if (this@NeuronArrayNode !in networkPanel.selectionManager) {
                    networkPanel.selectionManager.add(this@NeuronArrayNode)
                }
                event.isHandled = true
            }
        })
    }

    /** Map a pixel within [activationImage] to a neuron index, respecting layout mode. */
    private fun pixelToNeuronIndex(localPt: Point2D): Int? {
        val size = neuronArray.size
        if (size <= 0) return null
        return if (gridMode) {
            val len = ceil(sqrt(size.toDouble())).toInt()
            val row = ((localPt.y / imageSize) * len).toInt().coerceIn(0, len - 1)
            val col = ((localPt.x / imageSize) * len).toInt().coerceIn(0, len - 1)
            (row * len + col).takeIf { it < size }
        } else if (neuronArray.verticalLayout) {
            ((localPt.y / imageSize) * size).toInt().coerceIn(0, size - 1)
        } else {
            ((localPt.x / imageSize) * size).toInt().coerceIn(0, size - 1)
        }
    }

    /**
     * Collect cell indices whose visual area intersects the given global ellipse.
     * Used by the wand handler to operate on multiple pixels under the brush.
     */
    fun collectCellsInGlobalEllipse(ellipse: Ellipse2D): List<Int> {
        val size = neuronArray.size
        if (size <= 0) return emptyList()
        return when {
            neuronArray.circleMode ->
                neuronCircles.indices.filter { i -> ellipse.intersects(neuronCircles[i].globalBounds) }
            gridMode -> {
                val len = ceil(sqrt(size.toDouble())).toInt()
                val cell = imageSize / len
                activationImage.cellsIntersectingGlobalEllipse(ellipse, len, len, cell, cell) { row, col ->
                    (row * len + col).takeIf { it < size }
                }
            }
            neuronArray.verticalLayout ->
                activationImage.cellsIntersectingGlobalEllipse(
                    ellipse, size, 1, flatPixelArrayHeight.toDouble(), imageSize / size
                ) { row, _ -> row }
            else ->
                activationImage.cellsIntersectingGlobalEllipse(
                    ellipse, 1, size, imageSize / size, flatPixelArrayHeight.toDouble()
                ) { _, col -> col }
        }
    }

    /** Trace highlight set by the neuron array tracer. */
    var traceHighlight: NeuronArrayTraceHighlight? = null

    /**
     * Indices of pixels (neurons) the user has selected for quick-edit. Empty means no pixel selection
     * is active; in that case increment/decrement/clear/randomize fall through to whole-component behavior.
     */
    var pixelSelection: Set<Int> = emptySet()
        set(value) {
            field = value
            repaint()
        }

    /** Pick or toggle a pixel as part of the per-pixel selection. */
    private fun selectPixel(idx: Int, addToSelection: Boolean) {
        if (idx !in 0 until neuronArray.size) return
        pixelSelection = if (addToSelection) {
            if (idx in pixelSelection) pixelSelection - idx else pixelSelection + idx
        } else {
            setOf(idx)
        }
    }

    /**
     * Image with spikes and transparent background overlaid on the activation image for spiking neuron arrays.
     */
    private val spikeImage = SimbrainImage().apply {
        imageNodeGroup.addChild(this)
    }

    protected val biasImage = SimbrainImage()

    /**
     * Background for label text, so that background objects don't show up.
     */
    private val labelBackground = PNode()

    private val imageSize = 100.0

    /**
     * If true, show the image array as a grid; if false show it as a horizontal line.
     */
    private var gridMode = false
        set(value) {
            field = value
            updateActivationImage()
            updateBorder()
            layoutNeuronCircles()
        }

    private var showBias = false
        set(value) {
            if (value != field) {
                if (value) {
                    imageNodeGroup.addChild(biasImage)
                } else {
                    imageNodeGroup.removeChild(biasImage)
                }
            }
            field = value
            updateActivationImage()
            updateBorder()
        }

    override val margin = 10.0

    /**
     * Height of array when in "flat" mode.
     */
    private val flatPixelArrayHeight = 10

    /**
     * Create a new neuron array node.
     *
     * @param np Reference to NetworkPanel
     * @param na reference to model neuron array
     */
    init {

        // TODO: fixed events type on override
        val events = neuronArray.events

        events.visualPropertiesChanged.on(Dispatchers.Swing) {
            gridMode = neuronArray.gridMode
            showBias = neuronArray.isShowBias
            layoutNeuronCircles()
            networkPanel.network.events.zoomToFitPage.fire()
        }
        gridMode = neuronArray.gridMode
        showBias = neuronArray.isShowBias

        addChild(labelBackground)

        events.updated.on(Dispatchers.Default) {
            events.updateGraphics.fire()
        }

        events.updateGraphics.on(Dispatchers.Swing) {
            updateActivationImage()
        }
        events.updateRuleChanged.on(Dispatchers.Swing) {
            if (!neuronArray.updateRule.isSpikingRule) {
                mainNode.removeChild(spikeImage)
            }
        }
        events.clampChanged.on(Dispatchers.Swing) {
            updateActivationImage()
        }

        updateActivationImage()
        neuronCircleGroup.setOffset(NEURON_DIAMETER / 2.0, NEURON_DIAMETER / 2.0 + 20.0)
        activationImage.offset(0.0, 5.0)
        spikeImage.offset(0.0, 5.0)
        activationImage.addBorder()
        updateBorder()

        // call once to make sure all the actions are registered
        contextMenu

    }

    private fun updateActivationImage() {
        activationImage.removeAllChildren()
        spikeImage.removeAllChildren()
        biasImage.removeAllChildren()
        val activations = neuronArray.activations.toDoubleArray()

        fun renderGridImages() {
            val len = ceil(sqrt(activations.size.toDouble())).toInt()
            val img = activations.toSimbrainColorImage(len, len)
            activationImage.image = img
            activationImage.setBounds(
                0.0, 0.0,
                imageSize, imageSize
            )
            activationImage.addBorder()
            if (neuronArray.updateRule.isSpikingRule) {
                val spikes = (neuronArray.dataHolder as SpikingMatrixData).spikes
                spikeImage.image = spikes.toOverlay(len, len, NetworkPreferences.spikingColor)
                spikeImage.setBounds(
                    0.0, 0.0,
                    imageSize, imageSize
                )
                spikeImage.addBorder()
            }
            if (showBias) {
                biasImage.image = neuronArray.biases.toDoubleArray().toSimbrainColorImage(len, len)
                biasImage.setBounds(
                    0.0, imageSize + margin,
                    imageSize, imageSize
                )
                biasImage.addBorder()
            }
        }

        fun renderFlatImages() {
            val vertical = neuronArray.verticalLayout
            val imgWidth = if (vertical) 1 else activations.size
            val imgHeight = if (vertical) activations.size else 1
            val boundsW = if (vertical) flatPixelArrayHeight.toDouble() else imageSize
            val boundsH = if (vertical) imageSize else flatPixelArrayHeight.toDouble()

            val img = activations.toSimbrainColorImage(imgWidth, imgHeight)
            activationImage.image = img
            activationImage.setBounds(0.0, 0.0, boundsW, boundsH)
            activationImage.addBorder()
            if (neuronArray.updateRule.isSpikingRule) {
                val spikes = (neuronArray.dataHolder as SpikingMatrixData).spikes
                spikeImage.image = spikes.toOverlay(imgWidth, imgHeight, NetworkPreferences.spikingColor)
                spikeImage.setBounds(0.0, 0.0, boundsW, boundsH)
                spikeImage.addBorder()
            }
            if (showBias) {
                biasImage.image = neuronArray.biases.toDoubleArray().toSimbrainColorImage(imgWidth, imgHeight)
                if (vertical) {
                    biasImage.setBounds(boundsW + margin, 0.0, boundsW, boundsH)
                } else {
                    biasImage.setBounds(0.0, boundsH + margin, boundsW, boundsH)
                }
                biasImage.addBorder()
            }
        }

        fun renderNeuronCircles() {
            neuronCircles.forEachIndexed { i, circle ->
                circle.drawActivation(activations[i], neuronArray.updateRule.graphicalBounds)
                circle.setClamped(neuronArray.isClamped)
                circle.setLabel(neuronArray.labelArray.getOrNull(i))
            }
        }

        if (neuronArray.circleMode) {
            if (mainNode.indexOfChild(neuronCircleGroup) == -1) {
                mainNode.addChild(neuronCircleGroup)
            }
            mainNode.removeChild(imageNodeGroup)
            renderNeuronCircles()
        } else {
            if (mainNode.indexOfChild(imageNodeGroup) == -1) {
                mainNode.addChild(imageNodeGroup)
            }
            mainNode.removeChild(neuronCircleGroup)
            if (gridMode) {
                renderGridImages()
            } else {
                renderFlatImages()
            }
        }
    }

    override fun paintAfterChildren(paintContext: PPaintContext) {
        super.paintAfterChildren(paintContext)
        val g2 = paintContext.graphics

        drawTraceHighlight(g2)
        drawPixelSelection(g2)

        if (neuronArray.circleMode) return
        if (!NetworkPreferences.showNumericOverlays) return
        val activations = neuronArray.activations.toDoubleArray()
        val decimalPlaces = NetworkPreferences.neuronActivationDecimalPlaces

        if (gridMode) {
            val len = ceil(sqrt(activations.size.toDouble())).toInt()
            g2.drawNumericOverlay(
                data = activations,
                rows = len, cols = len,
                imageWidth = imageSize, imageHeight = imageSize,
                scalingFactor = networkPanel.scalingFactor,
                decimalPlaces = decimalPlaces,
                offsetX = activationImage.xOffset,
                offsetY = activationImage.yOffset
            )
        } else {
            val vertical = neuronArray.verticalLayout
            g2.drawNumericOverlay(
                data = activations,
                rows = if (vertical) activations.size else 1,
                cols = if (vertical) 1 else activations.size,
                imageWidth = if (vertical) flatPixelArrayHeight.toDouble() else imageSize,
                imageHeight = if (vertical) imageSize else flatPixelArrayHeight.toDouble(),
                scalingFactor = networkPanel.scalingFactor,
                decimalPlaces = decimalPlaces,
                offsetX = activationImage.xOffset,
                offsetY = activationImage.yOffset
            )
        }
    }

    private fun drawTraceHighlight(g2: Graphics2D) {
        val highlight = traceHighlight ?: return
        drawIndexHighlight(g2, highlight.indices, highlight.color, strokeWidth = 2f)
    }

    private fun drawPixelSelection(g2: Graphics2D) {
        if (pixelSelection.isEmpty()) return
        drawIndexHighlight(g2, pixelSelection, PIXEL_SELECTION_COLOR, strokeWidth = 3f)
    }

    /** Draw a rectangular outline around each of the given neuron indices. Shared by trace + pixel selection. */
    private fun drawIndexHighlight(g2: Graphics2D, indices: Set<Int>, color: Color, strokeWidth: kotlin.Float) {
        val size = neuronArray.size
        if (size <= 0) return
        g2.color = color
        g2.stroke = BasicStroke(strokeWidth)

        if (neuronArray.circleMode) {
            val pad = 4
            val d = (NEURON_DIAMETER + 2 * pad).toDouble()
            for (i in indices) {
                val circle = neuronCircles.getOrNull(i) ?: continue
                val cx = circle.xOffset + neuronCircleGroup.xOffset
                val cy = circle.yOffset + neuronCircleGroup.yOffset
                g2.drawRect((cx - d / 2).toInt(), (cy - d / 2).toInt(), d.toInt(), d.toInt())
            }
            return
        }

        val xOff = activationImage.xOffset
        val yOff = activationImage.yOffset
        if (gridMode) {
            val len = ceil(sqrt(size.toDouble())).toInt()
            val cellW = imageSize / len
            val cellH = imageSize / len
            for (i in indices) {
                if (i !in 0 until size) continue
                val row = i / len
                val col = i % len
                g2.drawRect(
                    (xOff + col * cellW).toInt(),
                    (yOff + row * cellH).toInt(),
                    cellW.toInt().coerceAtLeast(1),
                    cellH.toInt().coerceAtLeast(1)
                )
            }
        } else {
            val vertical = neuronArray.verticalLayout
            val cellW = if (vertical) flatPixelArrayHeight.toDouble() else imageSize / size
            val cellH = if (vertical) imageSize / size else flatPixelArrayHeight.toDouble()
            for (i in indices) {
                if (i !in 0 until size) continue
                val x = if (vertical) 0.0 else i * cellW
                val y = if (vertical) i * cellH else 0.0
                g2.drawRect(
                    (xOff + x).toInt(),
                    (yOff + y).toInt(),
                    cellW.toInt().coerceAtLeast(1),
                    cellH.toInt().coerceAtLeast(1)
                )
            }
        }
    }

    companion object {
        /** Outline color used to mark pixels in the per-pixel selection. */
        val PIXEL_SELECTION_COLOR: Color = Color(255, 200, 0)
    }

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()

            // Edit Menu
            contextMenu.add(networkPanel.networkActions.cutAction)
            contextMenu.add(networkPanel.networkActions.copyAction)
            contextMenu.add(networkPanel.networkActions.pasteAction)
            contextMenu.add(networkPanel.networkActions.duplicateAction)
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()
            val selectedArrays = networkPanel.selectionManager.filterSelectedModels<NeuronArray>()
            val count = selectedArrays.sizeIncluding(neuronArray)
            val editArray = networkPanel.createAction(
                name = "Edit $count neuron ${if (count == 1) "array" else "arrays"}..."
            ) {
                propertyDialog?.display()
            }
            contextMenu.add(editArray)
            contextMenu.addSeparator()
            contextMenu.add(networkPanel.networkActions.connectSelectedModels)
            contextMenu.addSeparator()

            // Choose style
            val switchStyle: Action = networkPanel.createAction(
                name = "Toggle line / grid",
                iconPath = "menu_icons/grid.png",
                keyboardShortcut = 'G',
                description = "Toggle line / grid style"
            ) {
                networkPanel.selectionManager
                    .filterSelectedModels<NeuronArray>()
                    .forEach { it.gridMode = !it.gridMode }
            }
            contextMenu.add(switchStyle)

            val switchOrientation: Action = networkPanel.createAction(
                name = "Toggle horizontal / vertical layout",
                keyboardShortcut = 'L',
                description = "Toggle horizontal / vertical layout"
            ) {
                networkPanel.selectionManager
                    .filterSelectedModels<NeuronArray>()
                    .forEach { it.verticalLayout = !it.verticalLayout }
            }
            contextMenu.add(switchOrientation)

            val toggleCircleMode: Action = networkPanel.createAction(
                name = "Toggle circle mode",
                keyboardShortcut = CmdOrCtrl + Shift + 'C',
                description = "Toggle activation rendering mode between circle and image",
            ) {
                networkPanel.selectionManager
                    .filterSelectedModels<NeuronArray>()
                    .forEach { it.circleMode = !it.circleMode }
            }
            contextMenu.add(toggleCircleMode)

            val toggleShowBias: Action = networkPanel.createAction(
                name = "Toggle bias visibility",
                keyboardShortcut = 'B',
                description = "Toggle whether biases are visible"
            ) {
                networkPanel.selectionManager
                    .filterSelectedModels<NeuronArray>()
                    .forEach { it.isShowBias = !it.isShowBias }
            }
            contextMenu.add(toggleShowBias)
            contextMenu.addSeparator()

            val createSupervisedModelAction = networkPanel.networkActions.createSupervisedModelAction
            contextMenu.add(createSupervisedModelAction)
            contextMenu.add(networkPanel.networkActions.createConvolutionalNeuralNetworkAction)
            contextMenu.addSeparator()
            contextMenu.add(networkPanel.createAction(name = "Add outgoing neuron array...") {
                networkPanel.showAddNeuronArrayDialog(neuronArray)
            })
            contextMenu.addSeparator()

            val applyInputs: Action = networkPanel.networkActions.createTestInputPanelAction(neuronArray)
            contextMenu.add(applyInputs)
            val addActivationToInput = networkPanel.networkActions.createAddActivationToInputAction(neuronArray)
            contextMenu.add(addActivationToInput)
            contextMenu.addSeparator()

            // Randomize Action
            val randomizeAction = networkPanel.networkActions.randomizeObjectsAction

            contextMenu.add(randomizeAction)
            val randomizeBiasesAction = networkPanel.createAction(
                name = "Randomize Biases",
                description = "Randomize the biases of this neuron array",
                iconPath = "menu_icons/Rand.png"
            ) {
                with(networkPanel.network) {
                    networkPanel.selectionManager
                        .filterSelectedModels<NeuronArray>()
                        .forEach { it.randomizeBiases() }
                }
            }
            contextMenu.add(randomizeBiasesAction)
            contextMenu.addSeparator()
            val editComponents: Action = object : AbstractAction("Edit components...") {
                override fun actionPerformed(event: ActionEvent) {
                    val dialog = StandardDialog()
                    val arrayData = MatrixDataFrame(neuronArray.activations)
                    dialog.contentPane = SimbrainTablePanel(arrayData)
                    dialog.addCommitTask {
                        with(networkPanel.network) {
                            neuronArray.update()
                        }
                    }
                    dialog.pack()
                    dialog.setLocationRelativeTo(null)
                    dialog.isVisible = true
                }
            }
            contextMenu.add(editComponents)

            // Projection Plot Action
            contextMenu.addSeparator()
            contextMenu.add(
                actionManager.createCoupledPlotMenu(
                    neuronArray.getProducer(NeuronArray::activationArray),
                    objectName = "${neuronArray.id ?: "Neuron Array"} Activations",
                    menuTitle = "Plot Activations${if (neuronArray.updateRule.isSpikingRule) "/Spikes" else ""}"
                ).also {
                    if (neuronArray.updateRule.isSpikingRule) {
                        it.addSeparator()
                        it.add(
                            actionManager.createCoupledRasterPlotAction(
                                neuronArray.getProducer(NeuronArray::spikes),
                                "${neuronArray.displayName} Spikes"
                            )
                        )
                    }
                }
            )
            contextMenu.add(
                actionManager.createCoupledPlotMenu(
                    neuronArray.getProducer(NeuronArray::biasArray),
                    objectName = "${neuronArray.id ?: "Neuron Array"} Biases",
                    menuTitle = "Plot Biases"
                )
            )
            contextMenu.add(actionManager.createImageInput(
                neuronArray.getConsumer(NeuronArray::activationArray),
                neuronArray.size,
                menuTitle = "Add coupled image world",
                postActionBlock = {
                    neuronArray.gridMode = true
                    neuronArray.isClamped = true
                }
            ))
            contextMenu.add(
                actionManager.createCoupledDataWorldAction(
                    name = "Record Activations",
                    neuronArray.getProducer(NeuronArray::activationArray),
                    sourceName = "${neuronArray.id ?: "Neuron Array"} Activations",
                    neuronArray.size
                )
            )
            contextMenu.add(
                actionManager.createCoupledDataWorldAction(
                    name = "Record Biases",
                    neuronArray.getProducer(NeuronArray::biasArray),
                    sourceName = "${neuronArray.id ?: "Neuron Array"} Biases",
                    neuronArray.size
                )
            )

            contextMenu.addSeparator()
            contextMenu.add(networkPanel.alignMenu)
            contextMenu.add(networkPanel.spaceMenu)

            // Coupling menu
            contextMenu.addSeparator()
            val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(neuronArray)
            contextMenu.add(couplingMenu)
            return contextMenu
        }

    override fun createEditDialog(): StandardDialog? = networkPanel.filterSelectedModelByClass<NeuronArray>().let { nanList ->

        if (nanList.isEmpty()) return null

        nanList.createEditorDialog(
            titleName = if (nanList.size == 1) "Edit ${nanList.first().displayName}" else "Edit ${nanList.size} Neuron Arrays"
        )
    }

    override val propertyDialog: StandardDialog?
        get() = if (pixelSelection.isNotEmpty()) networkPanel.createPixelEditDialog() else createEditDialog()

    override val model: NeuronArray
        get() = neuronArray



}
