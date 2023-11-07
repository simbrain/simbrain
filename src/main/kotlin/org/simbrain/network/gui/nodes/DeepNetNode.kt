package org.simbrain.network.gui.nodes

import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getDeepNetEditDialog
import org.simbrain.network.gui.dialogs.showDeepNetTrainingDialog
import org.simbrain.network.kotlindl.DeepNet
import org.simbrain.network.kotlindl.TFInputLayer
import org.simbrain.util.*
import org.simbrain.util.piccolo.addBorder
import org.simbrain.workspace.gui.CouplingMenu
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JDialog
import javax.swing.JPopupMenu

/**
 * GUI representation of KotlinDL deep network.
 */
class DeepNetNode(networkPanel: NetworkPanel, private val deepNet: DeepNet):
    ArrayLayerNode(networkPanel, deepNet) {

    private val infoText = PText().apply {
        font = INFO_FONT
        text = computeInfoText()
        mainNode.addChild(this)
    }

    /**
     * List of pixel grid images.
     */
    private var activationImages = listOf<PImage>()

    /**
     * Boxes drawn around the [activationImages].
     */
    private var activationImagesBoxes = listOf<PPath>()

    init {

        val events = deepNet.events
        events.updated.on(wait = true) {
            infoText.text = computeInfoText()
            renderActivations()
        }

        renderActivations()
        updateBorder()
    }

    /**
     * Update status text.
     */
    private fun computeInfoText() = "${deepNet.id} : ${deepNet.prediction}"
    // infoText.text = """
    //      Output: (${Utils.doubleArrayToString(deepNet.outputs!!.toDoubleArray(), 2)})
    //
    //      Input: (${Utils.doubleArrayToString(deepNet.doubleInputs, 2)})
    //      """.trimIndent()
    // val (x,y,width,height) = infoText.bounds

    override fun getModel(): NetworkModel {
        return deepNet
    }

    override fun getToolTipText(): String {
        return deepNet.toString()
    }

    override fun getContextMenu(): JPopupMenu {
        val contextMenu = JPopupMenu()
        contextMenu.add(networkPanel.networkActions.cutAction)
        contextMenu.add(networkPanel.networkActions.copyAction)
        contextMenu.add(networkPanel.networkActions.pasteAction)
        contextMenu.addSeparator()

        // Edit Submenu
        val editNet: Action = object : AbstractAction("Edit...") {
            override fun actionPerformed(event: ActionEvent) {
                val dialog = propertyDialog as StandardDialog?
                dialog!!.setLocationRelativeTo(null)
                dialog.pack()
                dialog.isVisible = true
            }
        }
        contextMenu.add(editNet)
        contextMenu.add(networkPanel.networkActions.deleteAction)
        contextMenu.addSeparator()

        // Train Submenu
        val trainDeepNet = networkPanel.createAction(
            name = "Train...",
            keyboardShortcut = CmdOrCtrl + 'T'
        ) {
            // TODO: Commented out code prevents the dialog being opened for multiple
            //  deep net nodes, but prevents it being called from right click node.
            // if (selectionManager.isSelected(this@DeepNetNode)) {
                showDeepNetTrainingDialog(deepNet)
            // }
        }
        contextMenu.add(trainDeepNet)

        // Coupling menu
        contextMenu.addSeparator()
        contextMenu.add(CouplingMenu(networkPanel.networkComponent, deepNet))

        return contextMenu
    }

    override fun getPropertyDialog(): JDialog {
        return getDeepNetEditDialog(deepNet)
    }

    /**
     * Render all activations as pixel grids. Rank 1: Activations are lines. Rank 3. A series of matrices. Rank 2 is
     * the case of just one matrix.
     */
    private fun renderActivations() {

        // Adjustible parameters
        val denseLayerImageHeight = 5.0
        val convLayerImageHeight = 10.0
        val layerImageWidth = 100.0
        val layerImagePadding = 2.0
        var totalHeight = infoText.height

        // Data from the deepNet being represented.
        val output = deepNet.outputs.toDoubleArray().map { it.toFloat() }.toFloatArray()
        val input = deepNet.floatInputs
        val inputLayer = (deepNet.tfLayers[0] as TFInputLayer)
        val inputActivations = if (inputLayer.layer?.outputShape?.rank() == 4) {
            listOf(input.reshape(inputLayer.rows, inputLayer.cols, inputLayer.channels).toList())
        } else {
            listOf(input)
        }
        val allActivations = inputActivations + deepNet.activations + listOf(output)

        // Set up the images.
        // Images are added from the bottom-up, so that the y value is negative with an increasing absolute value.
        activationImages.forEach { removeChild(it) }
        activationImages = sequence {
            allActivations.forEachIndexed { index, layer ->
                // Rank 1 case
                if (layer is FloatArray) {
                    val height = denseLayerImageHeight + layerImagePadding
                    totalHeight += height
                    yield(PImage(layer.toSimbrainColorImage(layer.size, 1)).also { image ->
                        image.setBounds(
                            0.0,
                            -totalHeight,
                            layerImageWidth,
                            denseLayerImageHeight
                        )
                    })
                } else if (layer is List<*>) {
                    // Rank 3 case (Rank 2 is also handled here)
                    val width = layerImageWidth / layer.size - layerImagePadding * ((layer.size - 1.0) / layer.size)
                    totalHeight += width + layerImagePadding
                    layer.filterIsInstance<Array<FloatArray>>().forEachIndexed { x, array ->
                        yield(PImage(array.toSimbrainColorImage()).also { image ->
                            image.setBounds(
                                x * (width + layerImagePadding),
                                -totalHeight,
                                width,
                                width
                            )
                        })
                    }
                } else {
                    // Debug code: If this is called, something went wrong. Produce a blue rectangle in this case.
                    val height = denseLayerImageHeight + layerImagePadding
                    totalHeight += height
                    yield(PImage(floatArrayOf(-1.0f).toSimbrainColorImage(1, 1)).also { image ->
                        image.setBounds(
                            0.0,
                            -totalHeight,
                            layerImageWidth,
                            denseLayerImageHeight
                        )
                    })
                }
            }
        }.toList()

        // Place info text at the top
        infoText.setBounds(0.0, activationImages.last().y - infoText.height - 7,
            infoText.width, infoText.height)

        // Add these as children to the main node
        activationImages.forEach { mainNode.addChild(it) }
        activationImages.forEach { it.addBorder() }

    }

}