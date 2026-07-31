package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PCamera
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.Connector
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.*
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import org.simbrain.util.table.addSimpleDefaults
import org.simbrain.util.table.createShowEigenValuesAction
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.geom.Ellipse2D
import java.util.*
import java.util.function.Consumer
import javax.swing.*

/**
 * A visual representation of a weight matrix
 */
class WeightMatrixNode(networkPanel: NetworkPanel, val weightMatrix: Connector) : ScreenElement(networkPanel) {
    /**
     * Width of the [imageBox]
     */
    private val imageWidth = 90

    /**
     * Height of the [imageBox]
     */
    private val imageHeight = 90

    private val boxThickness = 2f

    private var networkPanelScalingFactor = networkPanel.scalingFactor

    /**
     * A box around the [imageBox]
     */
    val imageBox = ImageBox(imageWidth, imageHeight, boxThickness)

    private val arrow = WeightMatrixArrow(this)

    /**
     * Whether to draw the connecting arrow. The matrix's image box and label are unaffected, so hiding
     * it leaves the matrix visible and editable while some other drawing accounts for the connection.
     * A self-connection unrolled over time is the case this exists for: the loop is the same thing as
     * the chain of arrows across the unrolled timesteps, so drawing both says it twice.
     */
    var arrowVisible: Boolean
        get() = arrow.visible
        set(value) {
            arrow.visible = value
        }

    val interactionBox: WeightMatrixInteractionBox = WeightMatrixInteractionBox(networkPanel)

    val sourceNode by lazy { networkPanel.getNode(weightMatrix.source) }
    val targetNode by lazy { networkPanel.getNode(weightMatrix.target) }

    /**
     * Collect (row, col) cells whose visual area intersects the given global ellipse.
     * Returned coordinates are in target-row / source-col space regardless of display transpose.
     */
    fun collectCellsInGlobalEllipse(ellipse: Ellipse2D): List<Pair<Int, Int>> {
        val wm = weightMatrix as? WeightMatrix ?: return emptyList()
        val matrix = wm.weights
        val nTargets = matrix.nrow()
        val nSources = matrix.ncol()
        if (nTargets <= 0 || nSources <= 0) return emptyList()
        val targetSource = NetworkPreferences.weightMatrixTargetSource
        val displayRows = if (targetSource) nTargets else nSources
        val displayCols = if (targetSource) nSources else nTargets
        val cellW = imageWidth.toDouble() / displayCols
        val cellH = imageHeight.toDouble() / displayRows
        return imageBox.cellsIntersectingGlobalEllipse(ellipse, displayRows, displayCols, cellW, cellH) { displayRow, displayCol ->
            if (targetSource) displayRow to displayCol else displayCol to displayRow
        }
    }

    /** Trace highlight set by the neuron array tracer. */
    var traceHighlight: WeightMatrixTraceHighlight? = null

    /**
     * Cells (row=target, col=source) the user has selected for quick-edit. Empty means no pixel selection
     * is active; in that case increment/decrement/clear/randomize fall through to whole-component behavior.
     */
    var pixelSelection: Set<Pair<Int, Int>> = emptySet()
        set(value) {
            field = value
            repaint()
        }

    private fun selectCell(row: Int, col: Int, addToSelection: Boolean) {
        val wm = weightMatrix as? WeightMatrix ?: return
        if (row !in 0 until wm.weights.nrow() || col !in 0 until wm.weights.ncol()) return
        val cell = row to col
        pixelSelection = if (addToSelection) {
            if (cell in pixelSelection) pixelSelection - cell else pixelSelection + cell
        } else {
            setOf(cell)
        }
    }

    init {
        pickable = true
        val events = weightMatrix.events
        events.updated.on(Dispatchers.Default) { events.updateGraphics.fire() }
        events.clampChanged.on(Dispatchers.Swing) { setClamped((weightMatrix as WeightMatrix).clamped) }
        events.updateGraphics.on(Dispatchers.Swing) { renderMatrixToImage() }
        events.labelChanged.on(Dispatchers.Swing) { _, newLabel ->
            interactionBox.setText(weightMatrix.displayName)
        }
        addChild(interactionBox)
        addChild(arrow)
        addChild(imageBox)
        imageBox.pickable = true
        imageBox.addInputEventListener(object : PBasicInputEventHandler() {
            override fun mouseMoved(event: PInputEvent) {
                val wm = weightMatrix as? WeightMatrix ?: return
                val localPt = event.getPositionRelativeTo(imageBox)
                val ij = pixelToWeightCell(wm, localPt) ?: return
                networkPanel.updateWeightMatrixCellTrace(wm, ij.first, ij.second)
            }
            override fun mouseExited(event: PInputEvent) {
                networkPanel.clearNeuronArrayTrace()
            }
            override fun mousePressed(event: PInputEvent) {
                if (!event.isAltDown) return
                val wm = weightMatrix as? WeightMatrix ?: return
                val ij = pixelToWeightCell(wm, event.getPositionRelativeTo(imageBox)) ?: return
                selectCell(ij.first, ij.second, addToSelection = event.isShiftDown)
                if (this@WeightMatrixNode !in networkPanel.selectionManager) {
                    networkPanel.selectionManager.add(this@WeightMatrixNode)
                }
                event.isHandled = true
            }
        })
        
        fun updateLocations() {
            arrow.invalidateFullBounds()
        }
        weightMatrix.source.events.locationChanged.on(Dispatchers.Swing) {
            updateLocations()
        }
        weightMatrix.target.events.locationChanged.on(Dispatchers.Swing) {
            updateLocations()
        }
        (weightMatrix.source as? NeuronArray)?.events?.visualPropertiesChanged?.on(Dispatchers.Swing) {
            updateLocations()
        }
        (weightMatrix.target as? NeuronArray)?.events?.visualPropertiesChanged?.on(Dispatchers.Swing) {
            updateLocations()
        }
        networkPanel.canvas.camera.addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM) {
            updateLocations()
        }
        networkPanel
        invalidateFullBounds()
        setClamped((weightMatrix as WeightMatrix).clamped)
        interactionBox.setText(weightMatrix.displayName)
        interactionBox.raiseToTop()
        renderMatrixToImage()
    }

    fun updateArrowColorFromPreferences() {
        imageBox.box.strokePaint = NetworkPreferences.weightMatrixBoundaryColor
        arrow.updateColorFromPreferences()
    }

    override fun refreshTheme() {
        renderMatrixToImage()
        updateArrowColorFromPreferences()
        setClamped((weightMatrix as WeightMatrix).clamped)
    }

    /**
     * Render the weight matrix to the [.imageBox].
     *
     * Render the weight matrix into an image using Simbrain Color Scheme. If the image is bigger than 1000x1000, it will
     * be scaled down to 1000x1000 using nearest neighbor interpolation.
     */
    private fun renderMatrixToImage() {
        val weightMatrix = weightMatrix as WeightMatrix
        val matrix = weightMatrix.weights
        val screenScalingFactor = getScreenScalingFactor()
        networkPanelScalingFactor = networkPanel.scalingFactor
        val scale = networkPanel.scalingFactor * screenScalingFactor

        // Create the image data from the weight matrix using nearest neighbor interpolation
        val imageData = matrix.toScaledImageData(imageWidth, imageHeight, scale)

        val img = imageData.toSimbrainColorImage().let { if (NetworkPreferences.weightMatrixTargetSource) it else it.transposed() }
        imageBox.image = img
    }

    override fun paint(paintContext: PPaintContext) {
        if (networkPanelScalingFactor != networkPanel.scalingFactor) {
            renderMatrixToImage()
        }
        paintContext.graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
        super.paint(paintContext)
    }

    override fun paintAfterChildren(paintContext: PPaintContext) {
        super.paintAfterChildren(paintContext)
        val wm = weightMatrix as? WeightMatrix ?: return
        val g2 = paintContext.graphics

        drawTraceHighlight(g2, wm)
        drawPixelSelection(g2, wm)

        if (!NetworkPreferences.showNumericOverlays) return
        val matrix = wm.weights
        val rows: Int
        val cols: Int
        val data: DoubleArray
        if (NetworkPreferences.weightMatrixTargetSource) {
            rows = matrix.nrow()
            cols = matrix.ncol()
            data = matrix.flatten()
        } else {
            rows = matrix.ncol()
            cols = matrix.nrow()
            // Transpose: read column-major from original matrix
            data = DoubleArray(rows * cols).also { arr ->
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        arr[r * cols + c] = matrix[c, r]
                    }
                }
            }
        }
        val boxOffset = imageBox.offset
        g2.drawNumericOverlay(
            data = data,
            rows = rows, cols = cols,
            imageWidth = imageWidth.toDouble(), imageHeight = imageHeight.toDouble(),
            scalingFactor = networkPanel.scalingFactor,
            decimalPlaces = NetworkPreferences.neuronActivationDecimalPlaces,
            offsetX = boxOffset.x,
            offsetY = boxOffset.y
        )
    }

    /**
     * Map a pixel inside [imageBox] to a `(targetRow, sourceCol)` cell of [wm], respecting the
     * `weightMatrixTargetSource` display preference (which may transpose the rendered image).
     */
    private fun pixelToWeightCell(wm: WeightMatrix, localPt: java.awt.geom.Point2D): Pair<Int, Int>? {
        val matrix = wm.weights
        val nTargets = matrix.nrow()
        val nSources = matrix.ncol()
        if (nTargets <= 0 || nSources <= 0) return null
        val displayRows: Int
        val displayCols: Int
        if (NetworkPreferences.weightMatrixTargetSource) {
            displayRows = nTargets
            displayCols = nSources
        } else {
            displayRows = nSources
            displayCols = nTargets
        }
        val displayRow = ((localPt.y / imageHeight) * displayRows).toInt().coerceIn(0, displayRows - 1)
        val displayCol = ((localPt.x / imageWidth) * displayCols).toInt().coerceIn(0, displayCols - 1)
        return if (NetworkPreferences.weightMatrixTargetSource) {
            displayRow to displayCol
        } else {
            displayCol to displayRow
        }
    }

    private fun drawPixelSelection(g2: Graphics2D, wm: WeightMatrix) {
        if (pixelSelection.isEmpty()) return
        val matrix = wm.weights
        val nTargets = matrix.nrow()
        val nSources = matrix.ncol()
        if (nTargets <= 0 || nSources <= 0) return
        val targetSource = NetworkPreferences.weightMatrixTargetSource
        val displayRows = if (targetSource) nTargets else nSources
        val displayCols = if (targetSource) nSources else nTargets
        val cellW = imageWidth.toDouble() / displayCols
        val cellH = imageHeight.toDouble() / displayRows
        val boxOffset = imageBox.offset

        g2.color = NeuronArrayNode.PIXEL_SELECTION_COLOR
        g2.stroke = BasicStroke(3f)

        for ((row, col) in pixelSelection) {
            if (row !in 0 until nTargets || col !in 0 until nSources) continue
            val displayRow = if (targetSource) row else col
            val displayCol = if (targetSource) col else row
            g2.drawRect(
                (boxOffset.x + displayCol * cellW).toInt(),
                (boxOffset.y + displayRow * cellH).toInt(),
                cellW.toInt().coerceAtLeast(1),
                cellH.toInt().coerceAtLeast(1)
            )
        }
    }

    private fun drawTraceHighlight(g2: Graphics2D, wm: WeightMatrix) {
        val h = traceHighlight ?: return
        val matrix = wm.weights
        val nTargets = matrix.nrow()
        val nSources = matrix.ncol()
        if (nTargets <= 0 || nSources <= 0) return
        val displayRows: Int
        val displayCols: Int
        val displayRow: Int?
        val displayCol: Int?
        if (NetworkPreferences.weightMatrixTargetSource) {
            displayRows = nTargets
            displayCols = nSources
            displayRow = h.row
            displayCol = h.col
        } else {
            displayRows = nSources
            displayCols = nTargets
            displayRow = h.col
            displayCol = h.row
        }
        val cellW = imageWidth.toDouble() / displayCols
        val cellH = imageHeight.toDouble() / displayRows
        val boxOffset = imageBox.offset

        g2.color = h.color
        g2.stroke = BasicStroke(2f)

        val rect = when {
            displayRow != null && displayCol != null -> {
                val r = displayRow.coerceIn(0, displayRows - 1)
                val c = displayCol.coerceIn(0, displayCols - 1)
                java.awt.geom.Rectangle2D.Double(boxOffset.x + c * cellW, boxOffset.y + r * cellH, cellW, cellH)
            }
            displayRow != null -> {
                val r = displayRow.coerceIn(0, displayRows - 1)
                java.awt.geom.Rectangle2D.Double(boxOffset.x, boxOffset.y + r * cellH, imageWidth.toDouble(), cellH)
            }
            displayCol != null -> {
                val c = displayCol.coerceIn(0, displayCols - 1)
                java.awt.geom.Rectangle2D.Double(boxOffset.x + c * cellW, boxOffset.y, cellW, imageHeight.toDouble())
            }
            else -> return
        }
        g2.drawRect(rect.x.toInt(), rect.y.toInt(), rect.width.toInt().coerceAtLeast(1), rect.height.toInt().coerceAtLeast(1))
    }

    override val isDraggable: Boolean = false

    override val toolTipText: String
        get() = createTooltipText(weightMatrix)

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()
            contextMenu.add(networkPanel.networkActions.cutAction)
            contextMenu.add(networkPanel.networkActions.copyAction)
            contextMenu.add(networkPanel.networkActions.pasteAction)
            contextMenu.add(networkPanel.networkActions.duplicateAction)
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()

            // Edit Submenu
            val selectedMatrices = networkPanel.selectionManager.filterSelectedModels<WeightMatrix>()
            val count = selectedMatrices.sizeIncluding(weightMatrix)
            val editArray = networkPanel.createAction(
                name = "Edit $count weight ${if (count == 1) "matrix" else "matrices"}..."
            ) {
                propertyDialog?.display()
            }
            contextMenu.add(editArray)
            contextMenu.addSeparator()
            val diagAction: Action = object : AbstractAction("Diagonalize") {
                init {
                    // putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/"));
                    putValue(SHORT_DESCRIPTION, "Diagonalize array")
                }

                override fun actionPerformed(event: ActionEvent) {
                    networkPanel.selectionManager
                        .filterSelectedModels(WeightMatrix::class.java)
                        .forEach(Consumer { obj: WeightMatrix -> obj.diagonalize() })
                }
            }
            contextMenu.add(diagAction)
            contextMenu.addSeparator()
            if (weightMatrix is WeightMatrix) {
                val randomizeAction: Action = networkPanel.networkActions.randomizeObjectsAction
                contextMenu.add(randomizeAction)
                contextMenu.add(
                    networkPanel.createAction(
                        name = "Randomize symmetric",
                        description = "Use network weight randomizer to randomize the matrix symmetrically.",
                    ) {
                            weightMatrix.weights.randomizeSymmetric(NetworkPreferences.weightRandomizer)
                            weightMatrix.events.updated.fire()
                    }
                )
                contextMenu.add(networkPanel.networkActions.showWeightMatrixAdjustmentPanel)
                contextMenu.addSeparator()
                contextMenu.add(
                    actionManager
                        .createCoupledPlotMenu(
                            (weightMatrix).getProducer(WeightMatrix::weightArray),
                            Objects.requireNonNull<String>(weightMatrix.id),
                            "Plot Weight Matrix"
                        )
                )
                contextMenu.addSeparator()
                contextMenu.add(
                    networkPanel.createAction(
                        name = "Show eigenvalues...",
                        description = "Show eigenvalues for this matrix if it is square",
                        iconPath = "menu_icons/lambda.png",
                        initBlock = {
                            val canShowEigenValues = try {
                                weightMatrix.weights.eigen()
                                true
                            } catch (e: Exception) {
                                // println("Error: ${e.message}")
                                false
                            }
                            isEnabled = canShowEigenValues
                        }) {
                        val eigenValues = weightMatrix.weights.eigenValuesString()
                        val formattedEigenvalues = eigenValues.chunked(10).joinToString("\n") { chunk ->
                            chunk.joinToString(", ")
                        }
                        showMessageDialog(
                            "[$formattedEigenvalues]",
                            "Eigenvalues"
                        )
                    }
                )
                contextMenu.add(
                    networkPanel.createAction(
                        name = "Set spectral radius...",
                        description = "Rescale matrix so that max eigenvalue is the specified value. < .9 decays; .9" +
                                " churns; > 1 explodes.",
                    ) {
                        val radius =
                            showNumericInputDialog("Set spectral Radius:", weightMatrix.weights.maxEigenvalue())
                        if (radius != null) {
                            weightMatrix.weights.setSpectralRadius(radius)
                            weightMatrix.events.updated.fire()
                        }
                    }
                )
                contextMenu.add(
                    networkPanel.createAction(
                        name = "Zero diagonal",
                        description = "Effectively removes self-connections (in the recurrent case)",
                    ) {
                        weightMatrix.weights.zeroDiagonalInPlace()
                        weightMatrix.events.updated.fire()
                    }
                )
            }

            // Coupling menu
            contextMenu.addSeparator()
            val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(weightMatrix)
            contextMenu.add(couplingMenu)

            return contextMenu
        }

    /**
     * Returns the dialog for editing this weight matrix
     */
    private fun createEditDialog(editingObjects: List<WeightMatrix>): StandardDialog? {

            if (editingObjects.isEmpty()) return null

            val dialog = StandardDialog()

            val ape = AnnotatedPropertyEditor(editingObjects)

            val contentPane: JComponent
            if (editingObjects.size == 1) {
                val editingObject = editingObjects.first()
                dialog.title = "Edit ${editingObject.displayName}"
                contentPane = JTabbedPane()
                contentPane.addTab("Properties", ape)

                val wm = MatrixDataFrame(editingObject.weights)
                val wmViewer = SimbrainTablePanel(wm, false)
                wmViewer.addSimpleDefaults()
                wmViewer.addSeparator()
                wmViewer.addAction(wmViewer.table.createShowEigenValuesAction())
                contentPane.addTab("Weight Matrix", wmViewer)
                editingObject.events.updated.on(Dispatchers.Swing) { wmViewer.model.fireTableDataChanged() }
                dialog.addCommitTask {
                    editingObject.setWeights(wm.get2DDoubleArray())
                    editingObject.events.updated.fire()
                }
            } else {
                dialog.title = "Edit ${editingObjects.size} Weight Matrices"
                contentPane = ape
            }

            dialog.addCommitTask { ape.commitChanges() }

            dialog.setContentPane(contentPane)
            dialog.pack()
            dialog.setLocationRelativeTo(null)
            return dialog
        }

    fun setClamped(clamped: Boolean) {
        if (clamped) {
            imageBox.box.stroke = BasicStroke(4.0f)
            imageBox.box.strokePaint = NetworkTheme.current.nodeOutline
        } else {
            imageBox.box.stroke = BasicStroke(1.0f)
            imageBox.box.strokePaint = NetworkPreferences.weightMatrixBoundaryColor
        }
    }

    override fun createEditDialog(): StandardDialog? = createEditDialog(networkPanel.filterSelectedModelByClass<WeightMatrix>())

    override val propertyDialog: StandardDialog?
        get() = if (pixelSelection.isNotEmpty()) networkPanel.createPixelEditDialog() else createEditDialog()

    override val model: Connector
        get() = weightMatrix

    override fun isIntersecting(bound: PBounds?): Boolean {
        // Check intersection with actual visual components rather than full bounds
        return imageBox.globalBounds.intersects(bound) ||
               arrow.globalBounds.intersects(bound) ||
               interactionBox.globalBounds.intersects(bound)
    }

    /**
     * Basic interaction box for weight matrix nodes. Ensures a property dialog
     * appears when the box is double-clicked.
     */
    inner class WeightMatrixInteractionBox(net: NetworkPanel) : InteractionBox(net) {

        override val contextMenu: JPopupMenu
            get() = this@WeightMatrixNode.contextMenu

        override fun createEditDialog(): StandardDialog? {
            return this@WeightMatrixNode.createEditDialog()
        }

        override val propertyDialog: StandardDialog?
            get() = this@WeightMatrixNode.propertyDialog

        override val isDraggable: Boolean
            get() = false

        override val model: Connector
            get() = this@WeightMatrixNode.model

        override val toolTipText: String?
            get() = this@WeightMatrixNode.toolTipText
    }
}
