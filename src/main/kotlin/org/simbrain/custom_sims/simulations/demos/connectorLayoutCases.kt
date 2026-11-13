/**
 * A network of common arrangements of layers and connectors, laid out as a grid of labeled cases, for checking by
 * eye how arrows are routed and where weight matrix images, kernels and label boxes are placed. Nothing is trained
 * or updated; the cases are there to be looked at, dragged about, zoomed and edited.
 */
package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.FlattenConnector
import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Padding
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.network.core.WeightMatrix
import org.simbrain.util.place
import org.simbrain.util.point

val connectorLayoutCases = newSim("connector_layout_cases") {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Connector layout cases")
    networkComponent.network.addConnectorLayoutCases()

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 1200, 820)
    }

    addSidebarInfo(
        """
            # Connector layout cases

            Common arrangements of layers and the connectors between them, for checking how arrows are routed and
            where weight matrix images, kernels and label boxes end up. Nothing here is trained or run.

            # The cases

            - **Feed-forward**: three layers of different sizes in a stack.
            - **Reciprocal pair**: two layers connected in both directions, once with layers of the same size and
            once with a wide layer and a narrow one.
            - **Skip connection**: a stack with a connection from the bottom layer straight to the top one, and a
            deeper stack with two skips that overlap.
            - **Fan-in** and **fan-out**: several layers feeding one, and one feeding several.
            - **Crossing**: two connections whose arrows cross.
            - **Loops**: a layer connected to itself, with a context layer beside it.
            - **Side by side** and **staggered**: chains that run left to right and diagonally.
            - **Mixed connectors**: a convolution, a flatten connector, a weight matrix and a synapse group together.

            # What to try

            - Drag a layer and watch its arrows and the images on them follow.
            - Drag a layer across another connection's image. The image should slide along its arrow to get out of
            the way and go back when the layer has gone.
            - Zoom out until the label boxes start to rescale, then back in.
            - In the symmetric cases, compare the two sides: the arrows should mirror each other and the images
            should be the same size.
            - Select a connector and delete it, then undo.
        """.trimIndent()
    )
}

/** Add every case to this network, each in its own cell of a grid, under a title. */
suspend fun Network.addConnectorLayoutCases() {
    val cases: List<Pair<String, suspend LayoutCase.() -> Unit>> = listOf(
        "Feed-forward" to {
            chain(array(0.0, 0.0, 8), array(0.0, -270.0, 5), array(0.0, -540.0, 3))
        },
        "Reciprocal pair, same size" to {
            reciprocal(array(0.0, 0.0), array(0.0, -460.0))
        },
        "Reciprocal pair, different sizes" to {
            reciprocal(collection(0.0, 0.0, 9), array(0.0, -460.0, 4))
        },
        "Skip connection" to {
            val (bottom, _, top) = chain(array(0.0, 0.0), array(0.0, -270.0), array(0.0, -540.0))
            matrix(bottom, top)
        },
        "Overlapping skips" to {
            val layers = chain(array(0.0, 0.0), array(0.0, -230.0), array(0.0, -460.0), array(0.0, -690.0))
            matrix(layers[0], layers[2])
            matrix(layers[1], layers[3])
        },
        "Fan-in" to {
            val target = array(0.0, -460.0)
            listOf(-220.0, 0.0, 220.0).forEach { x -> matrix(array(x, 0.0), target) }
        },
        "Fan-out" to {
            val source = array(0.0, 0.0)
            listOf(-220.0, 0.0, 220.0).forEach { x -> matrix(source, array(x, -460.0)) }
        },
        "Crossing" to {
            val lowerLeft = array(-120.0, 0.0)
            val lowerRight = array(120.0, 0.0)
            val upperLeft = array(-120.0, -520.0)
            val upperRight = array(120.0, -520.0)
            matrix(lowerLeft, upperRight)
            matrix(lowerRight, upperLeft)
        },
        "Loops" to {
            val (_, hidden, _) = chain(array(-90.0, 0.0), array(-90.0, -270.0), array(-90.0, -540.0))
            matrix(hidden, hidden)
            reciprocal(hidden, array(260.0, -270.0, 4))
        },
        "Side by side" to {
            chain(array(-300.0, -270.0), array(0.0, -270.0), array(300.0, -270.0))
        },
        "Staggered" to {
            chain(array(-200.0, 0.0), array(0.0, -270.0), array(200.0, -540.0))
        },
        "Mixed connectors" to {
            val shape = TensorShape(8, 8, 2)
            val image = tensorLayer(-120.0, 0.0, shape)
            val features = tensorLayer(-120.0, -300.0, shape.convOutputShape(3, 2, Padding.SAME, 4))
            add(ConvolutionConnector(image, features, 3, 4, 2, Padding.SAME))
            val flat = array(-120.0, -560.0, features.shape.size)
            add(FlattenConnector(features, flat))
            val readout = collection(250.0, -560.0, 4)
            val units = collection(250.0, -260.0, 4)
            matrix(flat, readout)
            add(SynapseGroup(units, readout).apply {
                autoVisibility = false
                displaySynapses = false
            })
        },
    )
    cases.forEachIndexed { index, (title, build) ->
        val column = index % CASES_PER_ROW
        val row = index / CASES_PER_ROW
        LayoutCase(this, column * CELL_WIDTH, row * CELL_HEIGHT).apply {
            title(title)
            build()
        }
    }
}

private const val CASES_PER_ROW = 4
private const val CELL_WIDTH = 860.0
private const val CELL_HEIGHT = 940.0

/**
 * Builds one case. Positions are given relative to the middle of the bottom of the case's cell, with layers stacked
 * upward from there, and are the centers of what is placed.
 */
private class LayoutCase(val network: Network, val baseX: Double, val baseY: Double) {

    suspend fun <T : org.simbrain.network.core.NetworkModel> add(model: T): T {
        network.addNetworkModel(model, usePlacementManager = false)
        return model
    }

    suspend fun title(text: String) {
        add(NetworkTextObject(text).apply {
            fontSize = 22
            isBold = true
            location = point(baseX - 330.0, baseY - 790.0)
        })
    }

    suspend fun array(x: Double, y: Double, size: Int = 6) = add(NeuronArray(size).apply {
        location = point(baseX + x, baseY + y)
    })

    suspend fun tensorLayer(x: Double, y: Double, shape: TensorShape) = add(TensorLayer(shape).apply {
        // A pattern, so that the layer is not drawn blank
        activations = DoubleArray(shape.size) { (it % 7) / 7.0 }
        location = point(baseX + x, baseY + y)
    })

    /** A row of loose neurons collected into a layer, which is as wide as it has neurons. */
    suspend fun collection(x: Double, y: Double, count: Int): NeuronCollection {
        val spacing = 45.0
        val neurons = List(count) { index ->
            add(Neuron().apply { location = point(baseX + x + (index - (count - 1) / 2.0) * spacing, baseY + y) })
        }
        return add(NeuronCollection(neurons))
    }

    suspend fun matrix(source: Layer, target: Layer) = add(WeightMatrix(source, target).apply {
        // A fixed pattern, so that an image is easy to tell from what is drawn across it
        setWeights(Array(target.size) { row ->
            DoubleArray(source.size) { column -> ((row * 3 + column * 5) % 7 - 3) / 3.0 }
        })
    })

    /** Connect each layer to the next, and return the layers. */
    suspend fun chain(vararg layers: Layer): List<Layer> {
        layers.toList().zipWithNext().forEach { (source, target) -> matrix(source, target) }
        return layers.toList()
    }

    suspend fun reciprocal(first: Layer, second: Layer) {
        matrix(first, second)
        matrix(second, first)
    }
}
