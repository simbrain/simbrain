package org.simbrain.custom_sims.simulations.neuroscience

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.addNeuronGroup
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.spikeresponders.ShortTermPlasticity
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.plot.rasterchart.RasterPlotDesktopComponent
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.place
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.LogNormalDistribution
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Model of canonical cortex (Douglas and Martin, 2004) using rat barrel cortex
 * as a reference (Lefort, Tomm, Sarria and Petersen, 2009). Users should be
 * able to inject current and see it propagate consistently with empirical
 * studies.
 *
 * Also see Haeusler and Mass, 2007.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
val corticalLayers = newSim {

    // Location and scale params for lognormal dist of all synapse groups
    var exlocation = 1.8
    var exscale = 0.5
    var inlocation = 3.6
    var inscale = 0.5
    var neuronsPerLayer = 300

    // TODO: Membrane properties
    // TODO: Build using z coordinates

    workspace.clearWorkspace()

    // Build network
    val nc = addNetworkComponent(
        "Cortical Simulation",
    )

    val net = nc.network

    val rasterPlot = addRasterPlot("Raster Plot")
    rasterPlot.model.removeDataSource()
    rasterPlot.model.addDataSources(3, listOf("L2/3", "L4", "L5/6"))

    withGui {
        place(nc, 10, 10, 550, 800)
        place(rasterPlot, 560, 12, 524, 568)
        rasterPlot.model.windowSize = 1000
        (getDesktopComponent(rasterPlot) as RasterPlotDesktopComponent).rasterPanel.updateChartSettings()
    }

    suspend fun buildLayer(
        numNeurons: Int,
        restingPotential: Double,
        timeConstant: Double,
        threshold: Double,
        resistance: Double
    ): NeuronGroup {
        return net.addNeuronGroup(numNeurons) {
            updateRule = IntegrateAndFireRule().also {
                it.restingPotential = restingPotential
                it.timeConstant = timeConstant
                it.threshold = threshold
                it.resistance = resistance
                it.backgroundCurrent = 0.0
                it.resetPotential = restingPotential
            }
            activation = restingPotential
        }
    }

    suspend fun connectLayers(
        src: NeuronGroup, tar: NeuronGroup,
        sparsity: Double,
    ): SynapseGroup {
        val exRand: ProbabilityDistribution = LogNormalDistribution(exlocation, exscale, false)
        val inRand: ProbabilityDistribution = LogNormalDistribution(inlocation, inscale, true)
        val con = Sparse(sparsity, false, false)
        val sg = SynapseGroup(src, tar, con)
        sg.connectionStrategy.exRandomizer = exRand
        sg.connectionStrategy.inRandomizer = inRand
        sg.randomizeExcitatory()
        sg.randomizeInhibitory()
        sg.label = "Synapses"

        sg.synapses.filter { it.source.polarity == Polarity.EXCITATORY }.forEach {
            it.upperBound = 200.0
            it.lowerBound = 0.0
        }
        sg.synapses.filter { it.source.polarity == Polarity.INHIBITORY }.forEach {
            it.upperBound = 0.0
            it.lowerBound = -200.0
        }

        sg.synapses.forEach {
            val stp = ShortTermPlasticity()
            stp.init(it)
            (stp.spikeResponderLocal as? JumpAndDecay)?.timeConstant = 2.0
            it.spikeResponder = stp
        }
        net.addNetworkModel(sg)
        return sg
    }

    fun random3Position(data: DoubleArray, xlim: DoubleArray, ylim: DoubleArray, zlim: DoubleArray) {
        data[0] = Random.nextDouble(xlim[0], xlim[1])
        data[1] = Random.nextDouble(ylim[0], ylim[1])
        data[2] = Random.nextDouble(zlim[0], zlim[1])
    }

    fun getDelay(xyz1: DoubleArray?, xyz2: DoubleArray?, maxDist: Double, maxDly: Double): Int {
        val dist = SimbrainMath.distance(xyz1, xyz2)
        return (dist / maxDist * maxDly / net.timeStep).toInt()
    }

    suspend fun buildNetwork() {
        net.timeStep = 0.2

        // Make the layers.  Params from Petersen, 2009.
        val btwnLayerSpacing = 150
        val layer_23 = buildLayer(
            neuronsPerLayer,
            restingPotential = -71.5,
            timeConstant = 29.0,
            threshold = -38.4,
            resistance = 190.0
        )
        layer_23.label = "Layer 2/3"
        val layer_4 = buildLayer(
            neuronsPerLayer,
            restingPotential = -66.0,
            timeConstant = 34.8,
            threshold = -39.7,
            resistance = 302.0
        )
        layer_4.label = "Layer 4"
        val layer_56 = buildLayer(
            neuronsPerLayer,
            restingPotential = -62.8,
            timeConstant = 31.7,
            threshold = -40.0,
            resistance = 187.0
        )
        layer_56.label = "Layer 5/6"
        val tmp = DoubleArray(3)
        val defMax = (layer_4.size * 2).toDouble()
        val xlim = doubleArrayOf(0.0, defMax)
        val zlim = doubleArrayOf(0.0, defMax)
        for (ii in 0 until layer_4.size) {
            val pol = if (Math.random() < 0.2) Polarity.INHIBITORY else Polarity.EXCITATORY
            var ylim = doubleArrayOf(0.0, defMax)
            random3Position(tmp, xlim, ylim, zlim)
            layer_56.neuronList[ii].position3D = tmp
            layer_56.neuronList[ii].polarity = pol
            ylim = doubleArrayOf(defMax + 100, 2 * defMax + 100)
            random3Position(tmp, xlim, ylim, zlim)
            layer_4.neuronList[ii].position3D = tmp
            layer_4.neuronList[ii].polarity = pol
            ylim = doubleArrayOf(2 * defMax + 200, 3 * defMax + 200)
            random3Position(tmp, xlim, ylim, zlim)
            layer_23.neuronList[ii].position3D = tmp
            layer_23.neuronList[ii].polarity = pol
        }
        layer_23.setLocation(500.0, 300.0)
        layer_4.setLocation(-150.0, 1120.0)
        layer_56.setLocation(500.0, 1850.0)

        // Connect layers
        val synGroups: MutableMap<String, SynapseGroup> = HashMap()
        // Recurrent connections - moderate strength to maintain activity but prevent runaway
        synGroups["L2/3 Rec."] = connectLayers(layer_23, layer_23, .12)
        synGroups["L4 Rec."] = connectLayers(layer_4, layer_4, .24)
        synGroups["L5/6 Rec."] = connectLayers(layer_56, layer_56, .24)

        // Strong forward connections - Layer 4 is the main input that drives other layers
        synGroups["L4 \u2192 L2/3"] = connectLayers(layer_4, layer_23, .14)  // Strong
        synGroups["L4 \u2192 L5/6"] = connectLayers(layer_4, layer_56, .08)  // Strong
        synGroups["L2/3 \u2192 L5/6"] = connectLayers(layer_23, layer_56, .08)  // Strong
        
        // Weak feedback connections - minimal influence
        synGroups["L2/3 \u2192 L4"] = connectLayers(layer_23, layer_4, .01)  // Weak
        synGroups["L5/6 \u2192 L4"] = connectLayers(layer_56, layer_4, .007)  // Weak
        synGroups["L5/6 \u2192 L2/3"] = connectLayers(layer_56, layer_23, .03)  // Weak

        for (sgn in synGroups.keys) {
            val sg = synGroups[sgn]
            for (s in sg!!.synapses) {
                s.delay = getDelay(
                    s.source.position3D, s.target.position3D,
                    sqrt((2 * (600 * 600) + 2000 * 2000).toDouble()), 20.0
                )
            }
            sg.label = sgn
        }

        val (layer23Plot, layer4Plot, layer56Plot) = rasterPlot.model.rasterConsumerList

        with(couplingManager) {
            layer_23.getProducer(layer_23::spikes) couple layer23Plot.getConsumer(layer23Plot::setValues)
            layer_4.getProducer(layer_4::spikes) couple layer4Plot.getConsumer(layer4Plot::setValues)
            layer_56.getProducer(layer_56::spikes) couple layer56Plot.getConsumer(layer56Plot::setValues)
        }
    }

    buildNetwork()
    addSidebarInfo(
        """
        # Cortical Layers Simulation

        This simulation models three major layers of the mammalian cerebral cortex (Layers 2/3, 4, and 5/6), each containing populations of spiking integrate-and-fire neurons connected with biologically-inspired synaptic dynamics. You can observe how activity propagates through these layers via [feedforward](https://docs.simbrain.net/docs/network/subnetworks/feedForward.html), [recurrent](https://docs.simbrain.net/docs/network/subnetworks/simpleRecurrentNetwork.html), and [feedback connections](https://pmc.ncbi.nlm.nih.gov/articles/PMC9990137/).

        # Background

        The [cerebral cortex](https://en.wikipedia.org/wiki/Cerebral_cortex) is organized into distinct layers, each with specialized types of neurons and connection patterns. Layers 2/3 are involved in processing and integrating information, Layer 4 primarily receives sensory input, and Layers 5/6 project to other brain areas. This layered structure supports complex computations like perception, motor control, and cognition.

        This model is inspired by canonical microcircuitry described in [Douglas and Martin (2004)](https://www.cns.nyu.edu/~tony/vns/readings/douglas-martin-2004.pdf) and empirical data from [rodent barrel cortex studies (Lefort et al., 2009)](https://doi.org/10.1016/j.neuron.2008.12.020). Neurons are modeled with leaky [integrate-and-fire dynamics](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html#integrate-and-fire), and [synapses](https://docs.simbrain.net/docs/network/synapses/) include [short-term plasticity](https://docs.simbrain.net/docs/network/spikeresponders/shortTermPlasticity.html) to capture dynamic changes in [synaptic strength](https://docs.simbrain.net/docs/network/synapses/hebbian.html#hebbian-rule) during activity.

        The simulation uses [sparse connectivity](https://docs.simbrain.net/docs/network/connections/sparse.html) with varying synaptic strengths and delays, reflecting realistic cortical connectivity. [Excitatory and inhibitory](https://docs.simbrain.net/docs/network/networkDialogs.html#excitatory--inhibitory-ratio) neuron populations are assigned according to experimentally observed ratios (~20% inhibitory neurons).

        # Visualization

        The [raster plot](https://docs.simbrain.net/docs/plots/rasterPlot.html#raster) shows [spiking](https://docs.simbrain.net/docs/network/spikingneurons.htm) activity in each cortical layer over time. Each dot represents a spike from an individual neuron. Patterns of synchronous or asynchronous firing reveal how information may flow and be processed within and between layers.

        # Things you can do

        ## Explore Layer Dynamics

        1. Click `Run` to start the simulation.
        2. Use the node activation tool to inject activation into the  “output” layer 5/6. Few spikes in the other layers should be observed.
        3.  Use the node activation tool to inject activation into layer 4. A burst of activity in 2/3 should then be observed, followed by a burst of activity in 5/6, consistent with known connectivity.
        4.  Use the node activation tool to inject activation into layer 2/3. This should lead, after some delay, to activity in 5/6, and then to a burst of activity in layer 4.

        # References

        [Douglas, R. J., & Martin, K. A. (2004). Neuronal circuits of the neocortex. *Annual Review of Neuroscience*, 27, 419–451.](https://www.cns.nyu.edu/~tony/vns/readings/douglas-martin-2004.pdf)

        [Lefort, S., Tomm, C., Sarria, J. C. F., & Petersen, C. C. H. (2009). The excitatory neuronal network of the C2 barrel column in mouse primary somatosensory cortex. *Neuron*, 61(2), 301–316.](https://doi.org/10.1016/j.neuron.2008.12.020)

        # Credits

        Zoë Tosi  
        Jeff Yoshimi  
        Elijah Olson
        """.trimIndent()
    )
}