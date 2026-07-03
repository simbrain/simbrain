package org.simbrain.custom_sims.simulations.edge_of_chaos

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.updaterules.BinaryRule
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.place
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.toMatrix
import org.simbrain.util.updateAction

/**
 * Demonstration of representational capacities of recurrent networks based on
 * Bertschinger, Nils, and Thomas Natschläger. "Real-time computation at the
 * edge of chaos in recurrent neural networks." Neural computation 16.7 (2004):
 * 1413-1436.
 *
 * Video of this in action: https://x.com/JeffYoshimi/status/1529126714948743168
 */
val edgeOfChaosBitStream = newSim("edgeOfChaosBitStream") {
    // Simulation Parameters
    val NUM_NEURONS = 120
    val u_bar = 1.0
    var variance = 0.5
    var currentRow = 0
    val seed = 42L

    // Clear workspace
    workspace.clearWorkspace()

    // Build network
    val networkComponent = addNetworkComponent("Edge of Chaos")
    val net = networkComponent.network
    net.timeStep = 0.5

    // Make reservoirs
    val res1 = createReservoir(net, 10, 10, NUM_NEURONS).apply {
        label = "Reservoir 1"
    }
    val res2 = createReservoir(net, res1.maxX.toInt() + 400, 10, NUM_NEURONS).apply {
        label = "Reservoir 2"
    }

    // Connect reservoirs
    val sgRes1 = connectReservoir(net, res1, variance, 4, seed)
    val sgRes2 = connectReservoir(net, res2, variance, 4, seed).apply {
        label = "Recurrent synapses"
    }

    // Set up "bit-stream" inputs
    suspend fun buildBitStream(reservoir: NeuronCollection): NeuronCollection {
        // Offset in pixels of input nodes to right of reservoir
        val offset = 200
        val b = BinaryRule(0.0, u_bar, 0.49)
        val bitStreamInputs = net.addNeuronCollection(1) { updateRule = b.copy() }.apply {
            val bitStream = arrayOf(
                doubleArrayOf(u_bar), doubleArrayOf(0.0), doubleArrayOf(0.0), doubleArrayOf(0.0), doubleArrayOf(0.0),
                doubleArrayOf(u_bar), doubleArrayOf(0.0), doubleArrayOf(u_bar), doubleArrayOf(u_bar), doubleArrayOf(0.0),
                doubleArrayOf(u_bar), doubleArrayOf(u_bar), doubleArrayOf(0.0), doubleArrayOf(0.0), doubleArrayOf(u_bar)
            )
            inputData = bitStream.toMatrix()
            setLocation(reservoir.centerX, reservoir.maxY + offset)
        }
        return bitStreamInputs
    }

    val bitStream1 = buildBitStream(res1).apply { label = "Bit stream 1" }
    val bitStream2 = buildBitStream(res2).apply { label = "Bit stream 2" }

    val connector1 = AllToAll(allowSelfConnection = false, seed = seed)
    val connector2 = AllToAll(allowSelfConnection = false, seed = seed)
    val thing1 = connector1.connectNeurons(bitStream1.neuronList, res1.neuronList)
    val thing2 = connector2.connectNeurons(bitStream2.neuronList, res2.neuronList)
    net.addNetworkModels(thing1)
    net.addNetworkModels(thing2)

    // Set up the time series and a custom action
    val (tsPlot, tsSeries) = addTimeSeries("Time Series", seriesNames = listOf("Difference"))

    workspace.updater.updateManager.addAction(updateAction("Update inputs") {
        bitStream1.addInputs(bitStream1.inputData.row(currentRow))
        bitStream2.addInputs(bitStream2.inputData.row(currentRow))
        currentRow = (currentRow + 1) % bitStream1.inputData.nrow()
    }, 0)

    workspace.addUpdateAction(updateAction("Update time series") {
        val activationDiff = SimbrainMath.hamming(res1.activationArray, res2.activationArray)
        tsSeries.series.add(workspace.time, activationDiff.toDouble())
    })

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 588, 617)

        // Set up control panel
        val controlPanel = createControlPanel("Controller", SIM_WINDOW_GAP + 588 + SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            val tf_stdev = addTextField("Weight stdev", variance.toString())
            addButton("Update") {
                variance = tf_stdev.text.toDouble()
                val normalDist1 = NormalDistribution(0.0, variance).apply { randomSeed = 42L }
                sgRes1.randomize(normalDist1)
                val normalDist2 = NormalDistribution(0.0, variance).apply { randomSeed = 42L }
                sgRes2.randomize(normalDist2)
            }
        }.awaitLayout()
        place(tsPlot, SIM_WINDOW_GAP + 588 + SIM_WINDOW_GAP, controlPanel.bottomEdgeWithGap(), 460, 434)
    }

    addSidebarInfo(
        """
        # Edge of Chaos Bitstream

        This simulation is an experimental study of reservoir networks, studying the ideas that Nils Bertschinger and Thomas Natschläger proposed in their paper, _Real-time
        computation at the edge of chaos in recurrent neural networks_.

        ## Reservoir Computing Background

        Here's an overview of reservoir computing and why reservoir networks are studied. Reservoir computing is a general theory of the computational properties that
        exists in neural networks. It attempts to explain the types of computation that a neural network in the brain requires to function properly (i.e., the constant cycling
        of recurrent activity in response to varying stimuli). From this theory emerges two key concepts: the fading memory property and the separation property.

        1) The **fading memory property** states that recurrent networks can store different representations and recall past representations. However, it
        also states that past representations should fade over time with the influx of new inputs. However, we do not want an excessive amount, or a lack
        thereof, of this fading of memory; we want just the right amount. Too little, and the network cannot store new information; too much, and the network cannot retain
        new information.

        2) The **separation property** builds upon the concept of the fading memory property, stating that with the influx of new inputs, a network will produce
        new representations. However, we do not want an excessive amount of separation. For instance, if the representations of two types of flowers are provided to
        a reservoir network, the network should still know that the object is a flower with a clear distinction in representation; but, not such a large distinction that it
        differentiates the two flowers entirely (an example of this can be tested in the `Edge Of Chaos Embodied` simulation).

        Tying these properties to the three different states, an ordered state will have weakened fading and weakened separation of representations, where its activation
        patterns will be pulled into a cyclic cycle. In this state, its difference will quickly decay. A chaotic state will have both properties however, with an excessive
        amount of separation, where its difference will continuously fluctuate. The edge of chaos state is the state that our brains have been theorized to be within; where
        there is just the right balance of the separation and fading of memory. In this state, the differences fluctuate for a period of time and then decay.

        # Simulation Details

        In this simulation, there are two reservoirs running concurrently where the difference between the two reservoirs is recorded in a time series. From their difference, we can infer
        the three different states of computation: ordered, edge of chaos, and chaos. Although, the values do not align perfectly to the original paper by Nils Bertschinger and
        Thomas Natschläger, all three dynamical regimes can be observed. Currently, the regimes are split into the specified value regions: chaos occurs around `5.0` and higher,
        ordered occurs for values around `1.0` and lower, and the edge of chaos occurs around `2.0` till around `4.0`.

        # What to Do

        To visualize these concepts, this simulation will utilize two reservoir networks to illustrate the three types of computational states that the neural networks can
        be in. They will begin with the same representations, and through the addition of new activation inputs, they will differ in representation. This difference
        will tell us which computational state the reservoir networks are in.

        In this simulation, the only configuration to the simulation is the `weight stdev`. To find each state, follow the steps below.

        1. Change the `weight stdev` value and press the `update` button to change the reservoirs' responses to new activation inputs, which will be shown in the time series.

        2. Start the simulation by clicking on the `Run` button in the top-left corner.

        3. Then, click on the `Activate Nodes` button (the button next to the `cursor` icon), and increase the activation in one of the reservoirs by holding left-click
        and moving around in either reservoirs.

        4. Now, observe the changes in the time series and determine its current computational state.

        5. To `reset` the simulation, stop the simulation by clicking the `Run` button again and press `k`.

        6. Afterwards, click back on the `cursor` icon, and left-click outside of the reservoirs to unselect all neurons.

        ## Experimentation with The Ordered State

        An experiment to better understand the ordered state is to set `weight stdev` to a very low value, like `0.01`, and look at the two reservoirs' activation patterns.
        Afterwards, add in new activation with the `Activate Nodes` button and see how the differences between the two reservoirs are changing. This task uses
        the same steps as above with an added step.

        ## Find The Edge Of Chaos

        To find the edge of chaos state, find the state where there is just the right amount of orderliness and chaos, where the difference remains a short period of time,
        and then disappears. To start, try a `weight stdev` that is greater than `5.0`, and slowly move down, repeating the steps above until the edge of chaos.

        # Links

        Here's a quick [demo](https://x.com/JeffYoshimi/status/1529126714948743168) on how the different types of states are exhibited in the time series.

        # References

        Bertschinger, N., & Natschläger, T. (2004). [_Real-Time Computation at the Edge of Chaos in Recurrent Neural Networks_](https://doi.org/10.1162/089976604323057443). _Neural Computation_, _16_(7), 1413–1436.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        Kanly Thao

        """.trimIndent()
    )
}
