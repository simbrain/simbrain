package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.AbstractNeuronCollection
import org.simbrain.network.core.Connector
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.ImageBox
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.WeightMatrixArrow
import org.simbrain.network.gui.createCouplingMenu
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

    private val interactionBox = WeightMatrixInteractionBox()

    init {
        updateShowWeights()
        pickable = true
        val events = weightMatrix.events
        events.updated.on { events.updateGraphics.fire() }
        events.clampChanged.on { setClamped((weightMatrix as WeightMatrix).clamped) }
        events.updateGraphics.on(Dispatchers.Swing) { renderMatrixToImage() }
        events.labelChanged.on(Dispatchers.Swing) { _, newLabel -> interactionBox.setText(newLabel) }
        fun updateLocations() {
            arrow.invalidateFullBounds()
            updateInteractionBoxLocation()
        }
        weightMatrix.source.events.locationChanged.on(Dispatchers.Swing) {
            updateLocations()
        }
        weightMatrix.target.events.locationChanged.on(Dispatchers.Swing) {
            updateLocations()
        }
        invalidateFullBounds()
        weightMatrix.events.showWeightsChanged.on { updateShowWeights() }
        weightMatrix.events.colorPreferencesChanged.on {
            imageBox.box.strokePaint = NetworkPreferences.weightMatrixBoundaryColor
        }
        interactionBox.setText(weightMatrix.displayName)
        setClamped((weightMatrix as WeightMatrix).clamped)
    }

    private fun updateInteractionBoxLocation() {
        val (x, y) = ((weightMatrix.target.location - weightMatrix.source.location) / 2) + weightMatrix.source.location
        interactionBox.centerFullBoundsOnPoint(x, y)
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

        val img = imageData.toSimbrainColorImage().let { if (weightMatrix.transposeGraphics) it.transposed() else it }
        imageBox.image = img
    }

    private fun updateShowWeights() {
        networkPanel.selectionManager.remove(this)
        if (weightMatrix.isShowWeights) {
            arrow.invalidateFullBounds()
            removeChild(interactionBox)
            addChild(arrow)
            addChild(imageBox)
            renderMatrixToImage()
        } else {
            updateInteractionBoxLocation()
            interactionBox.invalidateFullBounds()
            removeChild(arrow)
            removeChild(imageBox)
            addChild(interactionBox)
        }
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
        get() = weightMatrix.toString()

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()
            contextMenu.add(networkPanel.networkActions.cutAction)
            contextMenu.add(networkPanel.networkActions.copyAction)
            contextMenu.add(networkPanel.networkActions.pasteAction)
            contextMenu.addSeparator()

            // Edit Submenu
            val editArray: Action = object : AbstractAction("Edit...") {
                override fun actionPerformed(event: ActionEvent) {
                    val dialog: StandardDialog = matrixDialog
                    dialog.setVisible(true)
                }
            }
            contextMenu.add(editArray)
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()
            val randomizeAction: Action = networkPanel.networkActions.randomizeObjectsAction
            contextMenu.add(randomizeAction)
            val diagAction: Action = object : AbstractAction("Diagonalize") {
                init {
                    // putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/"));
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
                    networkPanel.createAction(
                        name = "Transpose weight matrix image (Currently ${if (weightMatrix.transposeGraphics) "Source -> Target" else "Target -> Source"})",
                        description = "Transpose the weight matrix image",
                    ) {
                        weightMatrix.transposeGraphics = !weightMatrix.transposeGraphics
                    }
                )
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
                        JOptionPane.showMessageDialog(
                            this,
                            "[${eigenValues.joinToString(", ")}]",
                            "Eigenvalues",
                            JOptionPane.INFORMATION_MESSAGE
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

            if (model.source is AbstractNeuronCollection) {
                contextMenu.addSeparator()
                contextMenu.add(networkPanel.createAction(name = "Toggle show weights") {
                    weightMatrix.isShowWeights = !weightMatrix.isShowWeights
                })
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
    private val matrixDialog: StandardDialog
        get() {
            val dialog = StandardDialog()
            dialog.setTitle("Edit Weight Matrix")
            val tabs = JTabbedPane()

            // Property Editor
            val ape: AnnotatedPropertyEditor<*> = AnnotatedPropertyEditor(weightMatrix)
            tabs.addTab("Properties", ape)
            dialog.addCommitTask { ape.commitChanges() }

            // Weight matrix
            if (weightMatrix is WeightMatrix) {
                val wm = MatrixDataFrame(weightMatrix.weights)
                val wmViewer = SimbrainTablePanel(wm, false)
                wmViewer.addSimpleDefaults()
                wmViewer.addSeparator()
                wmViewer.addAction(wmViewer.table.createShowEigenValuesAction())
                tabs.addTab("Weight Matrix", wmViewer)
                weightMatrix.events.updated.on { wmViewer.model.fireTableDataChanged() }
                dialog.addCommitTask {
                    weightMatrix.setWeights(wm.get2DDoubleArray())
                    weightMatrix.events.updated.fire()
                }
            }
            dialog.setContentPane(tabs)
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


    override val propertyDialog: StandardDialog
        get() = matrixDialog

    override val model: Connector
        get() = weightMatrix

    inner class WeightMatrixInteractionBox : InteractionBox(networkPanel) {

        override val propertyDialog: StandardDialog = this@WeightMatrixNode.propertyDialog

        override val model: Connector
            get() = weightMatrix

        override val isDraggable: Boolean = false

        override val contextMenu: JPopupMenu
            get() = this@WeightMatrixNode.contextMenu

        override val toolTipText: String
            get() = this@WeightMatrixNode.toolTipText

    }
}
