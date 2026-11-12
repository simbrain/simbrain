/**
 * Checks that the canvas finishes its bounds bookkeeping after the changes connectors lay themselves out for: a
 * layer moving, a layer changing size, and the view zooming, which rescales label boxes below full size.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.FlattenConnector
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Padding
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.addNeurons
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.util.onEdt
import java.awt.Dimension
import java.awt.geom.Point2D
import javax.swing.JDialog

class CanvasBoundsTest {

    /** A panel laid out at a fixed size and validated, showing [network] with [models] added to it. */
    private fun panelFor(network: Network, vararg models: NetworkModel): NetworkPanel {
        val panel = NetworkPanel(NetworkComponent("test", network))
        runBlocking { models.forEach { network.addNetworkModel(it, usePlacementManager = false) } }
        onEdt {
            panel.preferredSize = Dimension(800, 800)
            JDialog().apply { contentPane = panel; pack() }
            panel.autoZoom = false
        }
        panel.validateCanvas()
        return panel
    }

    /** Collects every step that leaves the canvas unsettled, so one run shows all of them. */
    private class Steps(val panel: NetworkPanel) {
        private val problems = mutableListOf<String>()

        private fun check(after: String) {
            panel.awaitCanvasIdle()
            val report = panel.canvasBoundsReport()
            if (!report.settled) problems += "after $after: $report"
        }

        fun step(name: String, change: () -> Unit) {
            onEdt(change)
            check(name)
        }

        /** Zooming in leaves label boxes at full size; zooming out below full size rescales them. */
        fun zoomInAndOut() {
            step("zooming in") { panel.canvas.camera.scaleViewAboutPoint(1.7, 0.0, 0.0) }
            step("zooming out") { panel.canvas.camera.scaleViewAboutPoint(0.3, 0.0, 0.0) }
        }

        fun assertSettledThroughout() = assertTrue(problems.isEmpty()) { problems.joinToString("\n") }
    }

    private fun arrayAt(x: Double, y: Double, size: Int = 4) =
        NeuronArray(size).apply { location = Point2D.Double(x, y) }

    @Test
    fun `bounds settle around a weight matrix when a layer moves, changes size, or the view zooms`() {
        val source = arrayAt(0.0, 0.0, size = 16)
        val target = arrayAt(0.0, -300.0, size = 16)
        val panel = panelFor(Network(), source, target, WeightMatrix(source, target), WeightMatrix(target, source))
        val steps = Steps(panel)

        steps.step("moving a layer") { target.location = Point2D.Double(250.0, -420.0) }
        steps.step("switching a layer to grid mode") { target.gridMode = true }
        steps.zoomInAndOut()
        steps.assertSettledThroughout()
    }

    @Test
    fun `bounds settle around a self connection when its layer moves or the view zooms`() {
        val layer = arrayAt(0.0, 0.0)
        val panel = panelFor(Network(), layer, WeightMatrix(layer, layer))
        val steps = Steps(panel)

        steps.step("moving the layer") { layer.location = Point2D.Double(120.0, -80.0) }
        steps.zoomInAndOut()
        steps.assertSettledThroughout()
    }

    @Test
    fun `bounds settle around a convolution when a layer moves or the view zooms`() {
        val shape = TensorShape(4, 4, 2)
        val source = TensorLayer(shape).apply { location = Point2D.Double(0.0, 0.0) }
        val target = TensorLayer(shape.convOutputShape(3, 1, Padding.SAME, 2)).apply {
            location = Point2D.Double(0.0, -300.0)
        }
        val conv = ConvolutionConnector(source, target, 3, 2, 1, Padding.SAME)
        val panel = panelFor(Network(), source, target, conv)
        val steps = Steps(panel)

        steps.step("moving a layer") { target.location = Point2D.Double(250.0, -520.0) }
        steps.zoomInAndOut()
        steps.step("switching to the kernel grid") { conv.kernelGridMode = true }
        steps.step("moving a layer with the kernel grid showing") { target.location = Point2D.Double(0.0, -600.0) }
        steps.zoomInAndOut()
        steps.assertSettledThroughout()
    }

    @Test
    fun `bounds settle around a flatten connector when a layer moves or the view zooms`() {
        val shape = TensorShape(4, 4, 2)
        val source = TensorLayer(shape).apply { location = Point2D.Double(0.0, 0.0) }
        val target = arrayAt(0.0, -300.0, size = shape.size)
        val panel = panelFor(Network(), source, target, FlattenConnector(source, target))
        val steps = Steps(panel)

        steps.step("moving a layer") { target.location = Point2D.Double(250.0, -420.0) }
        steps.zoomInAndOut()
        steps.assertSettledThroughout()
    }

    @Test
    fun `bounds settle around a synapse group when a collection moves, the view zooms, or its synapses are shown`() {
        val network = Network()
        val source = NeuronCollection(runBlocking { network.addNeurons(4) })
        val target = NeuronCollection(runBlocking { network.addNeurons(4) })
        onEdt { target.neuronList.forEach { it.location = Point2D.Double(it.location.x, it.location.y - 300.0) } }
        // Drawn as a single arrow rather than as its synapses
        val group = SynapseGroup(source, target).apply {
            autoVisibility = false
            displaySynapses = false
        }
        val panel = panelFor(network, source, target, group)
        val steps = Steps(panel)

        steps.step("moving a collection") {
            target.neuronList.forEach { it.location = Point2D.Double(it.location.x + 250.0, it.location.y - 150.0) }
        }
        steps.zoomInAndOut()
        steps.step("showing its synapses") { group.displaySynapses = true }
        steps.step("moving a collection with its synapses showing") {
            target.neuronList.forEach { it.location = Point2D.Double(it.location.x - 250.0, it.location.y) }
        }
        steps.assertSettledThroughout()
    }

    @Test
    fun `bounds settle around a backprop network when it moves or the view zooms`() {
        val backprop = BackpropNetwork(intArrayOf(4, 3, 2))
        val panel = panelFor(Network(), backprop)
        val steps = Steps(panel)

        steps.step("moving the network") { backprop.location = Point2D.Double(200.0, -150.0) }
        steps.zoomInAndOut()
        steps.assertSettledThroughout()
    }

    @Test
    fun `bounds settle around a recurrent network when it moves, the view zooms, or it is unrolled`() {
        val bptt = BPTTNetwork(4, 3, 4)
        val panel = panelFor(Network(), bptt)
        val steps = Steps(panel)

        steps.step("moving the network") { bptt.location = Point2D.Double(200.0, -150.0) }
        steps.zoomInAndOut()
        steps.step("unrolling") { bptt.unrolledView = true }
        steps.step("moving the unrolled network") { bptt.location = Point2D.Double(0.0, 0.0) }
        steps.step("rolling back up") { bptt.unrolledView = false }
        steps.assertSettledThroughout()
    }
}
