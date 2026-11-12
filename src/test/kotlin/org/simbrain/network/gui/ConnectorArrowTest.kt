/**
 * A connector's arrow is drawn among the connections while the rest of the connector is drawn above them. Checks
 * that the arrow still belongs to the connector: a click on it selects the connector, mouse events on it reach the
 * connector's node, it leaves the canvas with the connector, and a loop that reaches past the layers counts toward
 * the extent of a subnetwork's outline and of the view fitted to the network. Also checks that what a connector
 * draws on its arrow is in front of other connections.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.util.PBounds
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.FlattenConnector
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Padding
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.addNeurons
import org.simbrain.network.gui.nodes.ConnectionDrawing
import org.simbrain.network.gui.nodes.SubnetworkNode
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.util.onEdt
import org.simbrain.util.piccolo.firstScreenElement
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.RecurrentArrow
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.JDialog

class ConnectorArrowTest {

    /** A panel laid out at a fixed size and validated, showing [network] with [models] added to it. */
    private fun panelFor(network: Network, vararg models: NetworkModel, autoZoom: Boolean = false): NetworkPanel {
        val panel = NetworkPanel(NetworkComponent("test", network))
        runBlocking { models.forEach { network.addNetworkModel(it, usePlacementManager = false) } }
        onEdt {
            panel.preferredSize = Dimension(800, 800)
            JDialog().apply { contentPane = panel; pack() }
            panel.autoZoom = autoZoom
        }
        panel.validateCanvas()
        if (!autoZoom) {
            onEdt { panel.canvas.camera.animateViewToCenterBounds(panel.canvas.layer.fullBounds, true, 0) }
            panel.validateCanvas()
        }
        return panel
    }

    private fun arrayAt(x: Double, y: Double, size: Int = 4) =
        NeuronArray(size).apply { location = Point2D.Double(x, y) }

    private fun tensorLayerAt(shape: TensorShape, y: Double) =
        TensorLayer(shape).apply { location = Point2D.Double(0.0, y) }

    /**
     * A point near the tail of the only straight arrow on the canvas, which runs up from a source layer to a target
     * layer above it. It is far enough along to be clear of a label box on top of the source, and short of what the
     * connector draws on its arrow, which sits in the middle of the gap.
     */
    private fun NetworkPanel.pointOnArrowTail(): Point2D = onEdt {
        val arrow = canvas.layer.allNodes.filterIsInstance<BezierArrow>().single()
        val bounds = arrow.globalFullBounds
        val point = Point2D.Double(bounds.centerX, bounds.maxY - bounds.height * 0.15)
        val inView = canvas.camera.viewToLocal(Point2D.Double(point.x, point.y))
        val picked = canvas.camera.pick(inView.x, inView.y, 1.0).pickedNode
        check(generateSequence(picked) { it.parent }.any { it === arrow }) { "$point is not on the arrow" }
        point
    }

    private fun NetworkPanel.click(globalPoint: Point2D) = onEdt {
        val p = canvas.camera.viewToLocal(Point2D.Double(globalPoint.x, globalPoint.y))
        listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED).forEach { id ->
            canvas.dispatchEvent(
                MouseEvent(
                    canvas, id, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
                    p.x.toInt(), p.y.toInt(), 1, false, MouseEvent.BUTTON1
                )
            )
        }
    }

    private fun NetworkPanel.selectedModels() = onEdt { selectionManager.selection.map { it.model } }

    /** Counts the presses that reach listeners on the node of [model]. */
    private fun NetworkPanel.countPressesOn(model: NetworkModel): () -> Int {
        var heard = 0
        onEdt {
            getNode(model)!!.addInputEventListener(object : PBasicInputEventHandler() {
                override fun mousePressed(event: PInputEvent) {
                    heard++
                }
            })
        }
        return { onEdt { heard } }
    }

    @Test
    fun `clicking a weight matrix arrow selects the matrix`() {
        val source = arrayAt(0.0, 0.0)
        val target = arrayAt(0.0, -420.0)
        val matrix = WeightMatrix(source, target)
        val panel = panelFor(Network(), source, target, matrix)

        panel.click(panel.pointOnArrowTail())

        assertEquals(listOf<NetworkModel>(matrix), panel.selectedModels())
    }

    @Test
    fun `clicking a convolution arrow selects the convolution`() {
        val shape = TensorShape(4, 4, 2)
        val source = tensorLayerAt(shape, 0.0)
        val target = tensorLayerAt(shape.convOutputShape(3, 1, Padding.SAME, 2), -500.0)
        val conv = ConvolutionConnector(source, target, 3, 2, 1, Padding.SAME)
        val panel = panelFor(Network(), source, target, conv)

        panel.click(panel.pointOnArrowTail())

        assertEquals(listOf<NetworkModel>(conv), panel.selectedModels())
    }

    @Test
    fun `clicking a flatten connector arrow selects the connector`() {
        val shape = TensorShape(4, 4, 2)
        val source = tensorLayerAt(shape, 0.0)
        val target = arrayAt(0.0, -420.0, size = shape.size)
        val flatten = FlattenConnector(source, target)
        val panel = panelFor(Network(), source, target, flatten)

        panel.click(panel.pointOnArrowTail())

        assertEquals(listOf<NetworkModel>(flatten), panel.selectedModels())
    }

    @Test
    fun `clicking a synapse group arrow selects the group`() {
        val network = Network()
        val source = NeuronCollection(runBlocking { network.addNeurons(4) })
        val target = NeuronCollection(runBlocking { network.addNeurons(4) })
        // Directly above the source, so that the arrow between them is straight
        onEdt {
            source.neuronList.zip(target.neuronList).forEach { (below, above) ->
                above.location = Point2D.Double(below.location.x, below.location.y - 420.0)
            }
        }
        // Drawn as a single arrow rather than as its synapses
        val group = SynapseGroup(source, target).apply {
            autoVisibility = false
            displaySynapses = false
        }
        val panel = panelFor(network, source, target, group)

        panel.click(panel.pointOnArrowTail())

        assertEquals(listOf<NetworkModel>(group), panel.selectedModels())
    }

    // The context menu and the property dialog are opened by listeners on a connector's node, which hear a right
    // click or a double click on its arrow the same way the listeners below hear a press

    @Test
    fun `a press on a weight matrix arrow is heard by listeners on the matrix node`() {
        val source = arrayAt(0.0, 0.0)
        val target = arrayAt(0.0, -420.0)
        val matrix = WeightMatrix(source, target)
        val panel = panelFor(Network(), source, target, matrix)
        val presses = panel.countPressesOn(matrix)

        panel.click(panel.pointOnArrowTail())

        assertEquals(1, presses())
    }

    @Test
    fun `a press on a convolution arrow is heard by listeners on the connector node`() {
        val shape = TensorShape(4, 4, 2)
        val source = tensorLayerAt(shape, 0.0)
        val target = tensorLayerAt(shape.convOutputShape(3, 1, Padding.SAME, 2), -500.0)
        val conv = ConvolutionConnector(source, target, 3, 2, 1, Padding.SAME)
        val panel = panelFor(Network(), source, target, conv)
        val presses = panel.countPressesOn(conv)

        panel.click(panel.pointOnArrowTail())

        assertEquals(1, presses())
    }

    @Test
    fun `a recurrent network's outline encloses the loop of its recurrent weights and the image on it`() {
        val bptt = BPTTNetwork(4, 3, 4)
        val panel = panelFor(Network(), bptt)

        onEdt {
            val loop = panel.canvas.layer.allNodes.filterIsInstance<RecurrentArrow>().single().globalFullBounds
            val image = (panel.getNode(bptt.hiddenToHidden) as WeightMatrixNode).imageBox.globalFullBounds
            val layersLeft = listOf(bptt.inputLayer, bptt.hiddenLayer, bptt.outputLayer)
                .minOf { panel.getNode(it)!!.globalFullBounds.x }
            // Both reach past the layers, so the outline only encloses them by counting them
            assertTrue(loop.x < layersLeft - 20.0, "loop $loop layers start at $layersLeft")
            assertTrue(image.x < layersLeft - 20.0, "image $image layers start at $layersLeft")

            val outline = (panel.getNode(bptt) as SubnetworkNode).outline.globalFullBounds
            assertTrue(outline.contains(loop), "outline $outline loop $loop")
            assertTrue(outline.contains(image), "outline $outline image $image")
        }
    }

    @Test
    fun `fitting the view to the network shows a self connection's loop and the image on it`() {
        val layer = arrayAt(0.0, 0.0)
        val matrix = WeightMatrix(layer, layer)
        val network = Network()
        val panel = panelFor(network, layer, matrix, autoZoom = true)

        network.events.zoomToFitPage.fire()
        panel.awaitCanvasIdle()

        onEdt {
            val loop = panel.canvas.layer.allNodes.filterIsInstance<RecurrentArrow>().single().globalFullBounds
            val image = (panel.getNode(matrix) as WeightMatrixNode).imageBox.globalFullBounds
            val layerLeft = panel.getNode(layer)!!.globalFullBounds.x
            assertTrue(loop.x < layerLeft - 20.0, "loop $loop layer starts at $layerLeft")

            val view = PBounds(panel.canvas.camera.viewBounds)
            assertTrue(view.contains(loop), "view $view loop $loop")
            assertTrue(view.contains(image), "view $view image $image")
        }
    }

    @Test
    fun `a weight matrix image is picked ahead of a synapse that runs across it`() {
        val source = arrayAt(0.0, 0.0)
        val target = arrayAt(0.0, -420.0)
        val matrix = WeightMatrix(source, target)
        val left = Neuron()
        val right = Neuron()
        val synapse = Synapse(left, right)
        val panel = panelFor(Network(), source, target, matrix, left, right, synapse)
        val image = onEdt { (panel.getNode(matrix) as WeightMatrixNode).imageBox.globalFullBounds }
        onEdt {
            left.location = Point2D.Double(image.x - 100.0, image.centerY)
            right.location = Point2D.Double(image.maxX + 100.0, image.centerY)
        }
        panel.validateCanvas()

        onEdt {
            val point = Point2D.Double(image.centerX, image.centerY)
            assertTrue(panel.getNode(synapse)!!.globalFullBounds.contains(point), "The synapse does not cross the image")
            val inView = panel.canvas.camera.viewToLocal(Point2D.Double(point.x, point.y))
            val picked = panel.canvas.camera.pick(inView.x, inView.y, 1.0).pickedNode
            val pickedModel = picked.firstScreenElement?.model
            assertTrue(pickedModel === matrix, "Picked $pickedModel")
        }
    }

    @Test
    fun `deleting a connector takes its arrow off the canvas`() {
        val source = arrayAt(0.0, 0.0)
        val target = arrayAt(0.0, -420.0)
        val matrix = WeightMatrix(source, target)
        val network = Network()
        val panel = panelFor(network, source, target, matrix)
        assertEquals(1, onEdt { panel.canvas.layer.allNodes.filterIsInstance<BezierArrow>().size })

        runBlocking { network.deleteModels(listOf(matrix)) }
        panel.awaitCanvasIdle()

        onEdt {
            assertTrue(panel.canvas.layer.allNodes.none { it is BezierArrow }, "The arrow is still on the canvas")
            assertTrue(panel.edgeTier.childrenReference.none { it is ConnectionDrawing })
        }
    }
}
