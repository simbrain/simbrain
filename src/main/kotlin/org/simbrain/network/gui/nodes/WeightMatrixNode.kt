package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.Connector
import org.simbrain.network.groups.AbstractNeuronCollection
import org.simbrain.network.gui.ImageBox
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.WeightMatrixArrow
import org.simbrain.network.gui.actions.edit.CopyAction
import org.simbrain.network.gui.actions.edit.CutAction
import org.simbrain.network.gui.actions.edit.PasteAction
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.matrix.ZoeConnector
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.table.MatrixDataWrapper
import org.simbrain.util.table.SimbrainDataViewer
import org.simbrain.util.table.addSimpleDefaults
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.*
import java.util.function.Consumer
import javax.swing.*

/**
 * A visual representation of a weight matrix
 */
class WeightMatrixNode(networkPanel: NetworkPanel, val weightMatrix: Connector) : ScreenElement(networkPanel), PropertyChangeListener {
    /**
     * Width of the [imageBox]
     */
    private val imageWidth = 90

    /**
     * Height of the [imageBox]
     */
    private val imageHeight = 90

    /**
     * A box around the [imageBox]
     */
    val imageBox = ImageBox(imageWidth, imageHeight, 4f)

    private val arrow = WeightMatrixArrow(this)

    private val interactionBox = WeightMatrixInteractionBox()

    init {
        updateShowWeights()
        pickable = true
        val events = weightMatrix.events
        events.deleted.on { removeFromParent() }
        events.updated.on(Dispatchers.Swing) { renderMatrixToImage() }
        events.labelChanged.on(Dispatchers.Swing) { _, newLabel -> interactionBox.setText(newLabel) }
        weightMatrix.source.events.locationChanged.on(Dispatchers.Swing) {
            arrow.invalidateFullBounds()
            updateInteractionBoxLocation()
        }
        weightMatrix.target.events.locationChanged.on(Dispatchers.Swing) {
            arrow.invalidateFullBounds()
            updateInteractionBoxLocation()
        }
        invalidateFullBounds()
        weightMatrix.events.showWeightsChanged.on { updateShowWeights() }
        interactionBox.setText(weightMatrix.label)
        addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this)
    }

    private fun updateInteractionBoxLocation() {
        val (x, y) = ((weightMatrix.target.location - weightMatrix.source.location) / 2) + weightMatrix.source.location
        interactionBox.centerFullBoundsOnPoint(x, y)
    }

    /**
     * Render the weight matrix to the [.imageBox].
     */
    private fun renderMatrixToImage() {
        var img: BufferedImage? = null
        if (weightMatrix.isEnableRendering) {
            img = if (weightMatrix is ZoeConnector) {
                // TODO: Temp representation. If there is enough divergence can break into separate classes and update
                //  NetworkPanel.kt accordingly
                val tempArray = DoubleArray(100)
                Arrays.fill(tempArray, .1)
                tempArray.toSimbrainColorImage(10, 10)
            } else {
                val pixelArray = (weightMatrix as WeightMatrix).weights
                pixelArray.toSimbrainColorImage(
                    weightMatrix.weightMatrix.ncol(),
                    weightMatrix.weightMatrix.nrow()
                )
            }
        }
        imageBox.image = img
    }

    private fun updateShowWeights() {
        networkPanel.selectionManager.remove(this)
        if (weightMatrix.isShowWeights) {
            lowerToBottom()
            arrow.invalidateFullBounds()
            removeChild(interactionBox)
            addChild(arrow)
            addChild(imageBox)
            renderMatrixToImage()
            setBounds(imageBox.bounds)
        } else {
            raiseToTop()
            updateInteractionBoxLocation()
            interactionBox.invalidateFullBounds()
            removeChild(arrow)
            removeChild(imageBox)
            addChild(interactionBox)
            setBounds(interactionBox.bounds)
        }
    }

    override fun paint(paintContext: PPaintContext) {
        paintContext.graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
        super.paint(paintContext)
    }

    override fun isSelectable(): Boolean {
        return true
    }

    override fun isDraggable(): Boolean {
        return false
    }

    override fun getToolTipText(): String {
        return weightMatrix.toString()
    }

    override fun getContextMenu(): JPopupMenu {
        val contextMenu = JPopupMenu()
        contextMenu.add(CutAction(getNetworkPanel()))
        contextMenu.add(CopyAction(getNetworkPanel()))
        contextMenu.add(PasteAction(getNetworkPanel()))
        contextMenu.addSeparator()

        // Edit Submenu
        val editArray: Action = object : AbstractAction("Edit...") {
            override fun actionPerformed(event: ActionEvent) {
                val dialog: StandardDialog = matrixDialog
                dialog.setVisible(true)
            }
        }
        contextMenu.add(editArray)
        contextMenu.add(getNetworkPanel().networkActions.deleteAction)
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
                actionManager
                    .createCoupledPlotMenu(
                        (weightMatrix).getProducer(WeightMatrix::getWeights),
                        Objects.requireNonNull<String>(weightMatrix.id),
                        "Plot Weight Matrix"
                    )
            )
        }

        if (model.source is AbstractNeuronCollection) {
            contextMenu.addSeparator()

            contextMenu.add(createAction("Toggle Show Weights") {
                weightMatrix.isShowWeights = !weightMatrix.isShowWeights
            })
        }

        // Coupling menu
        contextMenu.addSeparator()
        val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(weightMatrix)
        contextMenu.add(couplingMenu)

        return contextMenu
    }

    private val matrixDialog: StandardDialog
        /**
         * Returns the dialog for editing this weight matrix
         */
        private get() {
            val dialog = StandardDialog()
            dialog.setTitle("Edit Weight Matrix")
            val tabs = JTabbedPane()

            // Property Editor
            val ape: AnnotatedPropertyEditor<*> = AnnotatedPropertyEditor(weightMatrix)
            tabs.addTab("Properties", ape)
            dialog.addClosingTask { ape.commitChanges() }

            // Weight matrix
            if (weightMatrix is WeightMatrix) {
                val wm = MatrixDataWrapper(weightMatrix.weightMatrix)
                val wmViewer = SimbrainDataViewer(wm, false)
                wmViewer.addSimpleDefaults()
                tabs.addTab("Weight Matrix", wmViewer)
                weightMatrix.events.updated.on { wmViewer.model.fireTableDataChanged() }
                dialog.addClosingTask {
                    weightMatrix.setWeights(wm.get2DDoubleArray())
                    weightMatrix.events.updated.fireAndForget()
                }
            }
            dialog.setContentPane(tabs)
            dialog.pack()
            dialog.setLocationRelativeTo(null)
            return dialog
        }

    /**
     * Without this the node can't be selected.
     */
    override fun propertyChange(arg0: PropertyChangeEvent) {
        if (model.isShowWeights) {
            setBounds(imageBox.fullBounds)
        } else {
            setBounds(interactionBox.fullBounds)
        }
        invalidateFullBounds()
    }

    override fun getPropertyDialog(): JDialog {
        return matrixDialog
    }

    override fun getModel(): Connector {
        return weightMatrix
    }

    inner class WeightMatrixInteractionBox : InteractionBox(networkPanel) {

        override fun getPropertyDialog(): JDialog {
            return this@WeightMatrixNode.propertyDialog
        }

        override fun getModel(): Connector {
            return weightMatrix
        }

        override fun isDraggable(): Boolean {
            return false
        }

        override fun getContextMenu(): JPopupMenu {
            return this@WeightMatrixNode.contextMenu
        }

        override fun getToolTipText(): String {
            return this@WeightMatrixNode.toolTipText
        }

    }
}
