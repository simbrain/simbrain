package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PCamera
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
import java.awt.RenderingHints
import java.awt.event.ActionEvent
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

    val interactionBox: WeightMatrixInteractionBox = WeightMatrixInteractionBox(networkPanel)

    val sourceNode by lazy { networkPanel.getNode(weightMatrix.source) }
    val targetNode by lazy { networkPanel.getNode(weightMatrix.target) }

    init {
        pickable = true
        val events = weightMatrix.events
        events.updated.on { events.updateGraphics.fire() }
        events.clampChanged.on { setClamped((weightMatrix as WeightMatrix).clamped) }
        events.updateGraphics.on(Dispatchers.Swing) { renderMatrixToImage() }
        events.labelChanged.on(Dispatchers.Swing) { _, newLabel -> 
            interactionBox.setText(weightMatrix.displayName)
        }
        addChild(interactionBox)
        addChild(arrow)
        addChild(imageBox)
        
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
        weightMatrix.events.colorPreferencesChanged.on {
            imageBox.box.strokePaint = NetworkPreferences.weightMatrixBoundaryColor
        }
        setClamped((weightMatrix as WeightMatrix).clamped)
        interactionBox.setText(weightMatrix.displayName)
        interactionBox.raiseToTop()
        renderMatrixToImage()
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
            contextMenu.addSeparator()

            // Edit Submenu
            val selectedMatrices = networkPanel.selectionManager.filterSelectedModels<WeightMatrix>()
            val count = selectedMatrices.size
            val editArray = networkPanel.createAction(
                name = "Edit $count weight ${if (count == 1) "matrix" else "matrices"}..."
            ) {
                propertyDialog?.display()
            }
            contextMenu.add(editArray)
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()
            val randomizeAction: Action = networkPanel.networkActions.randomizeObjectsAction
            contextMenu.add(randomizeAction)
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
                        name = "Randomize symmetric",
                        description = "Use network weight randomizer to randomize the matrix symmetrically ",
                    ) {
                            weightMatrix.weights.randomizeSymmetric(NetworkPreferences.weightRandomizer)
                            weightMatrix.events.updated.fire()
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
                editingObject.events.updated.on { wmViewer.model.fireTableDataChanged() }
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
            imageBox.box.strokePaint = Color.BLACK
        } else {
            imageBox.box.stroke = BasicStroke(1.0f)
            imageBox.box.strokePaint = NetworkPreferences.weightMatrixBoundaryColor
        }
    }

    override fun createEditDialog(): StandardDialog? = createEditDialog(networkPanel.filterSelectedModelByClass<WeightMatrix>())

    override val propertyDialog: StandardDialog? get() = createEditDialog()

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
            get() = this@WeightMatrixNode.createEditDialog()

        override val isDraggable: Boolean
            get() = false

        override val model: Connector
            get() = this@WeightMatrixNode.model

        override val toolTipText: String?
            get() = this@WeightMatrixNode.toolTipText
    }
}
