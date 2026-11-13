/**
 * Displays weight matrices, as an image sized to the space between the two layers, and supplies property/data
 * dialogs for explicit editing targets.
 */
package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
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
import org.simbrain.util.onUi
import org.simbrain.util.piccolo.centerOn
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import org.simbrain.util.table.addSimpleDefaults
import org.simbrain.util.table.createShowEigenValuesAction
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.BasicStroke
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.geom.CubicCurve2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.util.*
import java.util.function.Consumer
import javax.swing.*
import kotlin.math.abs
import kotlin.math.ceil

/**
 * A visual representation of a weight matrix
 */
class WeightMatrixNode(networkPanel: NetworkPanel, val weightMatrix: Connector) : ScreenElement(networkPanel),
    PixelSelectionHost, LaidOutConnector {
    /**
     * Side of the square [imageBox]. Fitted to the space between the two layers by [placeOnShaft]; a
     * self-connection, which has no such space, keeps the default.
     */
    private var imageSide = IMAGE_SIDE_DEFAULT

    /** Pixel dimensions of the image as last rendered, which a change of [imageSide] does not always change. */
    private var renderedSize: Pair<Int, Int>? = null

    private val boxThickness = 2f

    private var networkPanelScalingFactor = networkPanel.scalingFactor

    /**
     * A box around the [imageBox]
     */
    val imageBox = ImageBox(imageSide, imageSide, boxThickness)

    private val arrow = WeightMatrixArrow(this)

    override val connectionDrawing = ConnectionDrawing(this).also { it.addChild(arrow) }

    override fun layoutArrow() = arrow.layout()

    /** Place the image, label box and caption on the arrow's shaft. */
    override fun placeOnArrow(obstacles: Obstacles) = arrow.placeMatrix(obstacles)

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

    /** Rows × columns of the matrix as displayed, which depends on [NetworkPreferences.weightMatrixTargetSource]. */
    private val shapeCaption = ShapeCaption()

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
        val cellW = imageSide / displayCols
        val cellH = imageSide / displayRows
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
            if (value.isNotEmpty()) networkPanel.hideCanvasTooltip()
            repaint()
        }

    override val hasPixelSelection get() = pixelSelection.isNotEmpty()

    override fun clearPixelSelection() {
        if (pixelSelection.isNotEmpty()) pixelSelection = emptySet()
    }

    override fun selectedPixels(): List<PixelTarget> {
        val wm = weightMatrix as? WeightMatrix ?: return emptyList()
        return pixelSelection.map { (row, col) -> PixelTarget.MatrixPixel(wm, row, col) }
    }

    override fun pixelsUnder(ellipse: Ellipse2D): List<PixelTarget> {
        val wm = weightMatrix as? WeightMatrix ?: return emptyList()
        return collectCellsInGlobalEllipse(ellipse).map { (row, col) -> PixelTarget.MatrixPixel(wm, row, col) }
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
        // Marked, not relayed through updateGraphics: the panel's sync pass redraws the matrix once per frame
        events.updated.onImmediate { markDirty(WEIGHTS) }.untilDisposed()
        events.updateGraphics.onImmediate { markDirty(WEIGHTS) }.untilDisposed()
        events.clampChanged.on(Dispatchers.Swing) { setClamped((weightMatrix as WeightMatrix).clamped) }.untilDisposed()
        events.labelChanged.on(Dispatchers.Swing) { _, newLabel ->
            interactionBox.setText(weightMatrix.displayName)
        }.untilDisposed()
        addChild(interactionBox)
        addChild(imageBox)
        addChild(shapeCaption)
        updateShapeCaption()
        imageBox.pickable = true
        imageBox.markAsPixelTarget()
        imageBox.addInputEventListener(object : PBasicInputEventHandler() {
            override fun mouseEntered(event: PInputEvent) {
                networkPanel.hideCanvasTooltip()
                event.isHandled = true
            }
            override fun mouseMoved(event: PInputEvent) {
                val wm = weightMatrix as? WeightMatrix ?: return
                val localPt = event.getPositionRelativeTo(imageBox)
                val ij = pixelToWeightCell(wm, localPt) ?: return
                networkPanel.updateWeightMatrixCellTrace(wm, ij.first, ij.second)
                networkPanel.showPixelTooltip(event, pixelSelection.isNotEmpty()) {
                    cellTooltipText(wm, ij.first, ij.second)
                }
            }
            override fun mouseExited(event: PInputEvent) {
                networkPanel.clearNeuronArrayTrace()
                networkPanel.hideCanvasTooltip()
            }
            override fun mousePressed(event: PInputEvent) {
                val wm = weightMatrix as? WeightMatrix ?: return
                val ij = pixelToWeightCell(wm, event.getPositionRelativeTo(imageBox)) ?: return
                networkPanel.pressPixel(event, this@WeightMatrixNode) { selectCell(ij.first, ij.second, addToSelection = it) }
            }
        })
        
        fun updateLocations() {
            requestLayout()
        }
        weightMatrix.source.events.locationChanged.on(Dispatchers.Swing) {
            updateLocations()
        }.untilDisposed()
        weightMatrix.target.events.locationChanged.on(Dispatchers.Swing) {
            updateLocations()
        }.untilDisposed()
        (weightMatrix.source as? NeuronArray)?.events?.visualPropertiesChanged?.on(Dispatchers.Swing) {
            updateLocations()
        }?.untilDisposed()
        (weightMatrix.target as? NeuronArray)?.events?.visualPropertiesChanged?.on(Dispatchers.Swing) {
            updateLocations()
        }?.untilDisposed()
        layoutWithView()
        setClamped((weightMatrix as WeightMatrix).clamped)
        interactionBox.setText(weightMatrix.displayName)
        interactionBox.raiseToTop()
        renderMatrixToImage()
    }

    override fun syncFromModel(bits: Int) {
        if (bits and WEIGHTS != 0) {
            renderMatrixToImage()
        }
    }

    fun updateArrowColorFromPreferences() {
        imageBox.box.strokePaint = NetworkPreferences.weightMatrixBoundaryColor
        arrow.updateColorFromPreferences()
    }

    override fun refreshTheme() {
        renderMatrixToImage()
        updateArrowColorFromPreferences()
        setClamped((weightMatrix as WeightMatrix).clamped)
        updateShapeCaption()
    }

    private fun updateShapeCaption() {
        val weights = (weightMatrix as WeightMatrix).weights
        val (rows, cols) = if (NetworkPreferences.weightMatrixTargetSource) {
            weights.nrow() to weights.ncol()
        } else {
            weights.ncol() to weights.nrow()
        }
        shapeCaption.text = "${rows}×${cols}"
        shapeCaption.refresh()
    }

    /** The image with what is drawn around it, as placement on the arrow sees it. */
    private fun drawing() = ShaftDrawing(
        above = interactionBox.fullBounds.height,
        below = if (shapeCaption.visible) shapeCaption.height + CAPTION_GAP else 0.0,
        minWidth = interactionBox.fullBounds.width,
        minSide = IMAGE_SIDE_MIN,
        maxSide = IMAGE_SIDE_MAX,
        comfortable = IMAGE_SIDE_DEFAULT,
        fill = IMAGE_FILL
    )

    /**
     * Center the image box on ([x], [y]) with the interaction box above it and the shape caption below.
     *
     * @return the space the three now take up
     */
    fun placeAt(x: kotlin.Double, y: kotlin.Double): Rectangle2D {
        imageBox.centerOn(x, y)
        interactionBox.centerOn(x, y - imageBox.height / 2.0 - interactionBox.fullBounds.height / 2.0)
        shapeCaption.centerOn(x, y + imageBox.height / 2.0 + shapeCaption.height / 2.0 + CAPTION_GAP)
        return drawing().footprintAt(x, y, imageSide)
    }

    /**
     * Size the image and place it on the arrow's [shaft], with the label box above it and the caption below, clear
     * of [obstacles] where there is room. It goes at [position] along the shaft, from 0 at the source layer to 1 at
     * the base of the arrowhead, unless something is in the way there, in which case it slides along the shaft. A
     * matrix that is [shared] with one running the other way keeps to its half of the gap.
     *
     * With the arrow to itself the image fills the room it has until it is [IMAGE_SIDE_DEFAULT] across. On a longer
     * arrow it grows only as far as leaves it, with its label box and caption, [IMAGE_FILL] of the arrow, so that
     * the arrow shows on either side of it. It stops growing at [IMAGE_SIDE_MAX]. It shrinks only where nowhere on
     * the arrow has room for it, and stops at [IMAGE_SIDE_MIN], below which it overlaps what is in the way.
     *
     * @param ownLayers the space taken up by the two layers the matrix joins
     * @return the space the image, label box and caption now take up
     */
    fun placeOnShaft(
        shaft: CubicCurve2D,
        position: kotlin.Double,
        shared: Boolean,
        obstacles: Obstacles,
        ownLayers: List<Rectangle2D>
    ): Rectangle2D {
        val placement = ShaftPlacer.place(
            shaft, drawing(), obstacles.layers, obstacles.drawings, ownLayers, position, shared
        )
        if (abs(placement.side - imageSide) > ShaftRoom.RESIZE_THRESHOLD) {
            imageSide = placement.side
            // Stretches the image already rendered, which is all a small matrix needs
            imageBox.resize(imageSide, imageSide)
            if (renderSize() != renderedSize) markDirty(WEIGHTS)
        }
        return placeAt(placement.centerX, placement.centerY)
    }

    /**
     * Pixel dimensions the matrix is rendered at for the current [imageSide]: the resolution it is displayed at,
     * but no more than one pixel per weight.
     */
    private fun renderSize(): Pair<Int, Int> {
        val matrix = (weightMatrix as WeightMatrix).weights
        val imagePixels = ceil(imageSide).toInt()
        return scaledImageSize(
            imagePixels, imagePixels, matrix.ncol(), matrix.nrow(),
            networkPanel.scalingFactor * getScreenScalingFactor()
        )
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
        val imagePixels = ceil(imageSide).toInt()
        val imageData = matrix.toScaledImageData(imagePixels, imagePixels, scale)

        val img = imageData.toSimbrainColorImage().let { if (NetworkPreferences.weightMatrixTargetSource) it else it.transposed() }
        imageBox.image = img
        renderedSize = renderSize()
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
            imageWidth = imageSide, imageHeight = imageSide,
            scalingFactor = networkPanel.scalingFactor,
            decimalPlaces = NetworkPreferences.neuronActivationDecimalPlaces,
            offsetX = boxOffset.x,
            offsetY = boxOffset.y
        )
    }

    /** Value readout for the weight from source neuron [sourceCol] to target neuron [targetRow]. */
    internal fun cellTooltipText(wm: WeightMatrix, targetRow: Int, sourceCol: Int): String =
        "Source $sourceCol → Target $targetRow: ${wm.weights[targetRow, sourceCol].formatSignificant(3)}"

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
        val displayRow = ((localPt.y / imageSide) * displayRows).toInt().coerceIn(0, displayRows - 1)
        val displayCol = ((localPt.x / imageSide) * displayCols).toInt().coerceIn(0, displayCols - 1)
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
        val cellW = imageSide / displayCols
        val cellH = imageSide / displayRows
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
        val cellW = imageSide / displayCols
        val cellH = imageSide / displayRows
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
                java.awt.geom.Rectangle2D.Double(boxOffset.x, boxOffset.y + r * cellH, imageSide, cellH)
            }
            displayCol != null -> {
                val c = displayCol.coerceIn(0, displayCols - 1)
                java.awt.geom.Rectangle2D.Double(boxOffset.x + c * cellW, boxOffset.y, cellW, imageSide)
            }
            else -> return
        }
        g2.drawRect(rect.x.toInt(), rect.y.toInt(), rect.width.toInt().coerceAtLeast(1), rect.height.toInt().coerceAtLeast(1))
    }

    override val isDraggable: Boolean = false

    /** The image with its label box and, when it is showing, its caption. */
    override fun measureFootprint() = footprintOf(
        imageBox.box.globalFullBounds,
        interactionBox.globalBounds,
        shapeCaption.globalBounds.takeIf { shapeCaption.visible }
    )

    override val toolTipText: String?
        get() = if (pixelSelection.isNotEmpty()) null else createTooltipText(weightMatrix)

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
                        showEigenvalues(weightMatrix.weights)
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
    internal fun createEditDialog(editingObjects: List<WeightMatrix>): StandardDialog? {

            if (editingObjects.isEmpty()) return null

            val dialog = StandardDialog()

            val ape = AnnotatedPropertyEditor(editingObjects)

            val contentPane: JComponent
            if (editingObjects.size == 1) {
                val editingObject = editingObjects.first()
                dialog.title = "Edit ${editingObject.displayName}"
                contentPane = JTabbedPane()
                contentPane.addTab("Properties", ape)

                val targetSourceFormat = NetworkPreferences.weightMatrixTargetSource
                val wm = MatrixDataFrame(
                    if (targetSourceFormat) editingObject.weights else editingObject.weights.transpose()
                )
                val wmViewer = SimbrainTablePanel(wm, false)
                wmViewer.addSimpleDefaults()
                wmViewer.addSeparator()
                wmViewer.addAction(wmViewer.table.createShowEigenValuesAction())
                contentPane.addTab("Weight Matrix", wmViewer)
                // A learning matrix reports an update every iteration, so the table refreshes at most once per frame
                editingObject.events.updated.onUi(dialog.viewScope) {
                    if (!targetSourceFormat) {
                        wm.data = editingObject.weights.transpose()
                    }
                    wmViewer.model.fireTableDataChanged()
                }
                dialog.addCommitTask {
                    editingObject.setWeights(
                        if (targetSourceFormat) wm.get2DDoubleArray() else wm.data.transpose().toArray()
                    )
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

    companion object {
        private const val WEIGHTS = 1

        /**
         * Room for a weight matrix image on the [shaft] of the arrow it is drawn on, at [position] along it, with
         * [above] and [below] taken up by what is drawn over and under the image. Anything that draws a matrix on
         * an arrow sizes it with this and [fittedImageSide], so that every drawing of a matrix follows the same rule.
         */
        internal fun imageRoom(
            shaft: CubicCurve2D,
            position: kotlin.Double = 0.5,
            shared: Boolean = false,
            above: kotlin.Double = 0.0,
            below: kotlin.Double = 0.0
        ) = shaftRoom(shaft, position, shared, above, below, fill = IMAGE_FILL, comfortable = IMAGE_SIDE_DEFAULT)

        /** Side of the weight matrix image that goes in [room]. */
        internal fun fittedImageSide(room: ShaftRoom) = room.squareSide(IMAGE_SIDE_MIN, IMAGE_SIDE_MAX)

        /**
         * Side of the image on a self-connection, and the side it reaches between two layers by filling the gap
         * between them.
         */
        internal const val IMAGE_SIDE_DEFAULT = 90.0

        private const val IMAGE_SIDE_MIN = 30.0

        private const val IMAGE_SIDE_MAX = 180.0

        /**
         * Fraction of the gap between the layers that an image larger than [IMAGE_SIDE_DEFAULT] takes up together
         * with its label box and caption.
         */
        internal const val IMAGE_FILL = 0.6

        /** Space between the image and the shape caption under it. */
        private const val CAPTION_GAP = 3.0
    }
}
