package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.ActivationSequence
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.randomizeBiases
import org.simbrain.network.gui.*
import org.simbrain.util.*
import org.simbrain.util.piccolo.SimbrainImage
import org.simbrain.util.piccolo.addBorder
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.SimbrainTablePanel
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

/**
 * PNode representation for [ActivationSequence]
 */
class ActivationSequenceNode(networkPanel: NetworkPanel, val activationSequence: ActivationSequence) :
    ArrayLayerNode(networkPanel, activationSequence) {

    /**
     * Main pixel image for activations.
     */
    protected val activationImage = SimbrainImage().apply {
        mainNode.addChild(this)
        pickable = true
        addInputEventListener(object : PBasicInputEventHandler() {
            override fun mouseMoved(event: PInputEvent) {
                val (row, col) = pixelToCell(event.getPositionRelativeTo(this@apply)) ?: return
                networkPanel.updateActivationSequenceTrace(activationSequence, row, col)
            }
            override fun mouseExited(event: PInputEvent) {
                networkPanel.clearNeuronArrayTrace()
            }
            override fun mousePressed(event: PInputEvent) {
                if (!event.isAltDown) return
                val (row, col) = pixelToCell(event.getPositionRelativeTo(this@apply)) ?: return
                selectCell(row, col, addToSelection = event.isShiftDown)
                if (this@ActivationSequenceNode !in networkPanel.selectionManager) {
                    networkPanel.selectionManager.add(this@ActivationSequenceNode)
                }
                event.isHandled = true
            }
        })
    }

    /** Trace highlight set by the activation sequence tracer. */
    var traceHighlight: SequenceTraceHighlight? = null

    /**
     * Cells (row = sequence position, col = feature) the user has selected for quick-edit. Empty means no
     * pixel selection is active; in that case increment/decrement/clear/randomize fall through to
     * whole-component behavior.
     */
    var pixelSelection: Set<Pair<Int, Int>> = emptySet()
        set(value) {
            field = value
            repaint()
        }

    /** Map a pixel within [activationImage] to a `(position row, feature col)` cell. */
    private fun pixelToCell(localPt: Point2D): Pair<Int, Int>? {
        val rows = activationSequence.sequenceSize
        val cols = activationSequence.size
        if (rows <= 0 || cols <= 0) return null
        val row = ((localPt.y / imageSize) * rows).toInt().coerceIn(0, rows - 1)
        val col = ((localPt.x / imageSize) * cols).toInt().coerceIn(0, cols - 1)
        return row to col
    }

    private fun selectCell(row: Int, col: Int, addToSelection: Boolean) {
        if (row !in 0 until activationSequence.sequenceSize || col !in 0 until activationSequence.size) return
        val cell = row to col
        pixelSelection = if (addToSelection) {
            if (cell in pixelSelection) pixelSelection - cell else pixelSelection + cell
        } else {
            setOf(cell)
        }
    }

    /**
     * Collect (row, col) cells whose visual area intersects the given global ellipse.
     * Used by the wand handler to operate on multiple pixels under the brush.
     */
    fun collectCellsInGlobalEllipse(ellipse: Ellipse2D): List<Pair<Int, Int>> {
        val rows = activationSequence.sequenceSize
        val cols = activationSequence.size
        if (rows <= 0 || cols <= 0) return emptyList()
        val cellW = imageSize / cols
        val cellH = imageSize / rows
        return activationImage.cellsIntersectingGlobalEllipse(ellipse, rows, cols, cellW, cellH) { row, col ->
            row to col
        }
    }

    /**
     * Image with spikes and transparent background overlaid on the activation image for spiking neuron arrays.
     */
    private val spikeImage = SimbrainImage().apply {
        mainNode.addChild(this)
    }

    protected val biasImage = SimbrainImage()

    val imageSize = 100.0

    override val margin = 10.0

    /**
     * Create a new neuron array node.
     *
     * @param np Reference to NetworkPanel
     * @param na reference to model neuron array
     */
    init {

        val events = activationSequence.events



        events.updated.on(Dispatchers.Default) {
            events.updateGraphics.fire()
        }

        events.updateGraphics.on(Dispatchers.Swing) {
            updateActivationImage()
        }

        updateActivationImage()
        activationImage.offset(0.0, 5.0)
        spikeImage.offset(0.0, 5.0)
        activationImage.addBorder()
        updateBorder()

        // call once to make sure all the actions are registered
        contextMenu

    }

    override fun refreshTheme() {
        super.refreshTheme()
        updateActivationImage()
    }

    private fun updateActivationImage() {
        activationImage.removeAllChildren()
        spikeImage.removeAllChildren()
        biasImage.removeAllChildren()
        val activations = activationSequence.activations

        val img = activations.flatten().toSimbrainColorImage(activations.ncol(), activations.nrow())
        activationImage.image = img
        activationImage.setBounds(
            0.0, 0.0,
            imageSize, imageSize
        )
        activationImage.addBorder()
    }

    override fun paintAfterChildren(paintContext: PPaintContext) {
        super.paintAfterChildren(paintContext)
        val g2 = paintContext.graphics
        traceHighlight?.let { drawSequenceHighlight(g2, it.rows, it.cols, it.color, strokeWidth = 2f) }
        if (pixelSelection.isNotEmpty()) {
            drawCellHighlight(g2, pixelSelection, NeuronArrayNode.PIXEL_SELECTION_COLOR, strokeWidth = 3f)
        }
        val highlightedRows = activationSequence.highlightedRows
        if (highlightedRows.isNotEmpty()) {
            drawSequenceHighlight(g2, highlightedRows, emptySet(), NetworkTheme.current.rowHighlight, strokeWidth = 1.5f)
        }
    }

    /** Draw full-width row outlines and full-height column outlines for trace highlighting. */
    private fun drawSequenceHighlight(g2: Graphics2D, rows: Set<Int>, cols: Set<Int>, color: Color, strokeWidth: kotlin.Float) {
        val nRows = activationSequence.sequenceSize
        val nCols = activationSequence.size
        if (nRows <= 0 || nCols <= 0) return
        val cellW = imageSize / nCols
        val cellH = imageSize / nRows
        val xOff = activationImage.xOffset
        val yOff = activationImage.yOffset
        g2.color = color
        g2.stroke = BasicStroke(strokeWidth)
        for (row in rows) {
            if (row !in 0 until nRows) continue
            g2.drawRect(xOff.toInt(), (yOff + row * cellH).toInt(), imageSize.toInt(), cellH.toInt().coerceAtLeast(1))
        }
        for (col in cols) {
            if (col !in 0 until nCols) continue
            g2.drawRect((xOff + col * cellW).toInt(), yOff.toInt(), cellW.toInt().coerceAtLeast(1), imageSize.toInt())
        }
    }

    /** Draw a rectangular outline around each selected `(row, col)` cell. */
    private fun drawCellHighlight(g2: Graphics2D, cells: Set<Pair<Int, Int>>, color: Color, strokeWidth: kotlin.Float) {
        val nRows = activationSequence.sequenceSize
        val nCols = activationSequence.size
        if (nRows <= 0 || nCols <= 0) return
        val cellW = imageSize / nCols
        val cellH = imageSize / nRows
        val xOff = activationImage.xOffset
        val yOff = activationImage.yOffset
        g2.color = color
        g2.stroke = BasicStroke(strokeWidth)
        for ((row, col) in cells) {
            if (row !in 0 until nRows || col !in 0 until nCols) continue
            g2.drawRect(
                (xOff + col * cellW).toInt(),
                (yOff + row * cellH).toInt(),
                cellW.toInt().coerceAtLeast(1),
                cellH.toInt().coerceAtLeast(1)
            )
        }
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
            val selectedSequences = networkPanel.selectionManager.filterSelectedModels<ActivationSequence>()
            val count = selectedSequences.sizeIncluding(activationSequence)
            val editArray = networkPanel.createAction(
                name = "Edit $count activation ${if (count == 1) "sequence" else "sequences"}..."
            ) {
                propertyDialog?.display()
            }
            contextMenu.add(editArray)
            contextMenu.addSeparator()
            contextMenu.add(networkPanel.networkActions.connectSelectedModels)
            contextMenu.addSeparator()

            val applyInputs: Action = networkPanel.networkActions.createTestInputPanelAction(activationSequence)
            contextMenu.add(applyInputs)
            val addActivationToInput = networkPanel.networkActions.createAddActivationToInputAction(activationSequence)
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
                with(network) {
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
                    val arrayData = MatrixDataFrame(activationSequence.activations)
                    dialog.contentPane = SimbrainTablePanel(arrayData)
                    dialog.addCommitTask {
                        with(networkPanel.network) {
                            activationSequence.update()
                        }
                    }
                    dialog.pack()
                    dialog.setLocationRelativeTo(null)
                    dialog.isVisible = true
                }
            }
            contextMenu.add(editComponents)

            contextMenu.addSeparator()
            contextMenu.add(networkPanel.alignMenu)
            contextMenu.add(networkPanel.spaceMenu)

            // Coupling menu
            contextMenu.addSeparator()
            val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(activationSequence)
            contextMenu.add(couplingMenu)
            return contextMenu
        }

    private fun createEditDialog(editingObjects: List<ActivationSequenceNode>): StandardDialog? = editingObjects.let { aqnList ->

        if (aqnList.isEmpty()) return null

        aqnList.map { it.model }.createEditorDialog()
    }


    override fun createEditDialog(): StandardDialog? = createEditDialog(networkPanel.filterSelectedNodeByClass<ActivationSequenceNode>())

    override val propertyDialog: StandardDialog?
        get() = if (pixelSelection.isNotEmpty()) networkPanel.createPixelEditDialog() else createEditDialog(listOf(this))

    override val model: ActivationSequence
        get() = activationSequence


}