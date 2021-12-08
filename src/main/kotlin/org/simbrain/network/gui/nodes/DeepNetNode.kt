package org.simbrain.network.gui.nodes

import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.actions.edit.CopyAction
import org.simbrain.network.gui.actions.edit.CutAction
import org.simbrain.network.gui.actions.edit.DeleteAction
import org.simbrain.network.gui.actions.edit.PasteAction
import org.simbrain.network.gui.dialogs.getDeepNetEditDialog
import org.simbrain.network.gui.dialogs.showDeepNetTrainingDialog
import org.simbrain.network.kotlindl.DeepNet
import org.simbrain.util.*
import java.awt.Font
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JDialog
import javax.swing.JPopupMenu

/**
 * GUI representation of KotlinDL deep network.
 */
class DeepNetNode(
    networkPanel: NetworkPanel?,
    /**
     * The deep network being represented.
     */
    private val deepNet: DeepNet
) : ScreenElement(networkPanel) {
    /**
     * Width in pixels of the main display box.
     */
    private val initialWidth = 80f

    /**
     * Height in pixels of the main display box.
     */
    private val initialHeight = 120f

    /**
     * Text showing info about the array.
     */
    private val infoText: PText
    private val box = createRectangle(0f, 0f, initialWidth, initialHeight)

    private var activationImages = listOf<PImage>()

    /**
     * Update status text.
     */
    private fun updateInfoText() {
        infoText.text = """
             Output: (${Utils.doubleArrayToString(deepNet.outputs!!.col(0), 2)})
             
             Input: (${Utils.doubleArrayToString(deepNet.doubleInputs, 2)})
             """.trimIndent()
        val newBounds = infoText.bounds.bounds
        newBounds.grow(10, 10) // add a margin
        box.setBounds(newBounds)
        setBounds(box.bounds)
        deepNet.width = box.width
        deepNet.height = box.height
    }

    override fun isSelectable(): Boolean {
        return true
    }

    override fun isDraggable(): Boolean {
        return true
    }

    override fun getContextMenu(): JPopupMenu? {
        val contextMenu = JPopupMenu()
        contextMenu.add(CutAction(networkPanel))
        contextMenu.add(CopyAction(networkPanel))
        contextMenu.add(PasteAction(networkPanel))
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
        contextMenu.add(DeleteAction(networkPanel))
        contextMenu.addSeparator()

        // Coupling menu
        //contextMenu.addSeparator();
        //JMenu couplingMenu = networkPanel.getCouplingMenu(neuronArray);
        //if (couplingMenu != null) {
        //    contextMenu.add(couplingMenu);
        //}

        // Train Submenu
        val trainDeepNet: Action = object : AbstractAction("Train...") {
            override fun actionPerformed(event: ActionEvent) {
                showDeepNetTrainingDialog(deepNet)
            }
        }
        contextMenu.add(trainDeepNet)
        return contextMenu
    }

    override fun getModel(): DeepNet {
        return deepNet
    }

    fun pullViewPositionFromModel() {
        val point: Point2D = deepNet.location.minus(Point2D.Double(width / 2, height / 2))
        this.globalTranslation = point
    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    fun pushViewPositionToModel() {
        val p = this.globalTranslation
        deepNet.location = point(p.x + width / 2, p.y + height / 2)
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        pushViewPositionToModel()
        super.offset(dx, dy)
    }

    override fun acceptsSourceHandle(): Boolean {
        return true
    }

    override fun getPropertyDialog(): JDialog? {
        return getDeepNetEditDialog(deepNet)
    }

    companion object {
        /**
         * Font for info text.
         */
        val INFO_FONT = Font("Arial", Font.PLAIN, 8)
    }

    override fun paint(paintContext: PPaintContext) {
        paintContext.graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
        super.paint(paintContext)
    }

    init {
        box.pickable = true
        addChild(box)
        val events = deepNet.events
        events.onDeleted { n: NetworkModel? -> removeFromParent() }
        events.onUpdated { updateInfoText() }

        val layerImageHeight = 5.0
        val layerImageWidth = initialWidth - 10.0
        val layerImagePadding = 2.0

        events.onUpdated {
            val output = deepNet.outputs!!.col(0).map { it.toFloat() }.toFloatArray()
            val input = deepNet.floatInputs
            val allActivations = listOf(input) + deepNet.activations + listOf(output)
            activationImages.forEach { removeChild(it) }
            val totalHeight = activationImages.size * 7.0
            activationImages = allActivations.mapIndexed { index, layer ->
                PImage(layer.toSimbrainColorImage(layer.size, 1)).also { image ->
                    image.setBounds(
                        0.0,
                        totalHeight - index * (layerImageHeight + layerImagePadding),
                        layerImageWidth,
                        layerImageHeight
                    )
                }
            }
            activationImages.forEach {
                addChild(it)
            }
            val allActivationsBound = activationImages.map { it.bounds.bounds2D }.reduce { acc, bound ->
                acc.createUnion(bound)
            }
            val newBounds = allActivationsBound.addPadding(10.0)
            box.setBounds(newBounds)
            setBounds(newBounds)
        }

        // Info text
        infoText = PText()
        infoText.font = INFO_FONT
        // addChild(infoText)
        updateInfoText()
        deepNet.events.onLocationChange { pullViewPositionFromModel() }
        pullViewPositionFromModel()
    }
}