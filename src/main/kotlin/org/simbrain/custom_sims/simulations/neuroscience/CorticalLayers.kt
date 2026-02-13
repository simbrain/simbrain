package org.simbrain.custom_sims.simulations.neuroscience

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.RandomWeightInitializer
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.addNeuronGroup
import org.simbrain.network.gui.ForceSpikeAction
import org.simbrain.network.gui.dialogs.NetworkPreferences
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
    var exlocation = 0.0
    var exscale = 0.5
    var inlocation = 1.0
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
        con.weightInitializer = RandomWeightInitializer().apply {
            exRandomizer = exRand
            inRandomizer = inRand
        }
        val sg = SynapseGroup(src, tar, con)
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

    withGui {
        // Make sure the force spike wand tool is present and selected
        val palette = NetworkPreferences.wandPalette
        var forceSpikeIndex = palette.actions.indexOfFirst { it is ForceSpikeAction }
        if (forceSpikeIndex < 0) {
            palette.addAction(ForceSpikeAction())
            forceSpikeIndex = palette.actions.lastIndex
        }
        palette.selectAction(forceSpikeIndex)
        NetworkPreferences.wandPalette = palette
    }

    addSidebarInfo(
        """
        # Cortical Layers Simulation

        This simulation models three major layers of the mammalian cerebral cortex (Layers `2/3`, `4`, and `5/6`), each containing populations of spiking integrate-and-fire neurons connected with biologically-inspired 
        synaptic dynamics. You can observe how activity propagates through these layers via [feedforward](https://docs.simbrain.net/docs/network/subnetworks/feedForward.html), 
        [recurrent](https://docs.simbrain.net/docs/network/subnetworks/simpleRecurrentNetwork.html), and [feedback connections](https://pmc.ncbi.nlm.nih.gov/articles/PMC9990137/) in a [raster plot](https://docs.simbrain.net/docs/plots/rasterPlot.html#raster).

        ## Background

        ### Cortical Organization
        
        The [cerebral cortex](https://en.wikipedia.org/wiki/Cerebral_cortex) (Latin for 'bark' or 'rind') is a large sheet of grey matter on the outer part of the brain, also known as the *neo-cortex* or telencephalon. 
        It consists primarily of cell bodies of neurons and glial cells, and surrounds white matter that connects different cortical regions together and to subcortical structures like the thalamus. The cortex is 
        thought to be the seat of most cognitive functions, including sensation, perception, motor processing, action planning, abstract reasoning, and thought.
        
        ### Laminar Structure
        
        The cortex has a repeating laminar (layered) structure with six distinct layers in mammals. Layer 1 is the outermost layer (closest to the skull), and layer 6 is the deepest. Each layer has distinctive 
        anatomical and physiological properties:
        
        <img src="//localfiles/simulations/images/corticalLayers/laminar_structure.png" width="300" alt="Laminar structure" />
        
        - **Layer 1**: Very sparse, containing few cell bodies but dendritic tufts and axons that enable local connectivity. Receives modulatory inputs from thalamus.
        
        - **Layer 2/3**: Primarily involved in cortico-cortical connections, receiving and sending information to other cortical areas both within the same hemisphere (ipsilateral) and across the corpus callosum 
        (contralateral). Thickest in non-sensory-motor areas like the frontal cortex. Layer 2/3 serves as a "meeting place" where inputs from Layer 4 and from other cortical areas converge.
        
        - **Layer 4**: The primary input layer, receiving dense projections from thalamus which in turn receives sensory information. It functions as an "input module" and is thus thicker in primary sensory areas. 
        Layer 4 exhibits the most irregular firing patterns.
        
        - **Layer 5/6**: The primary output layers, projecting to thalamus, subcortical structures (basal ganglia, cerebellum), and directly to the spinal cord. Contains large pyramidal cells and tends to be 
        thicker in motor areas. Layer 5 produces more "bursty" activity with higher average firing rates. Layer 6 creates precise reciprocal connections with thalamus, forming thalamocortical loops important 
        for gain control and can effectively silence or amplify thalamic input to its home column.
        
        ### Inter-Layer Connectivity
        
        The layers are interconnected in specific patterns that support the flow of information through cortex:
        
        <img src="//localfiles/simulations/images/corticalLayers/connectivity_data.png" width="300" alt="Connectivity patterns" />
        
        This figure shows empirical data from Lefort et al. (2009) demonstrating connectivity patterns. Each panel shows which layers activate when 10 neurons in a specific layer are stimulated. Notice that:
        
        - **Layer 4** activates itself (recurrent connections) and strongly activates **Layer 2/3** and **Layer 5/6**
        - **Layer 2/3** projects strongly to **Layer 5**
        - **Layer 5/6** shows relatively little activation of other layers
        
        In this simplified model, the layers function as:
        
        - **Layer 2/3**: Internal processing and communication between cortical areas
        - **Layer 4**: Input from sensory areas
        - **Layer 5/6**: Output to motor areas
        
        ### Model Details

        This model is inspired by canonical microcircuitry described in Douglas and Martin (2004) and empirical data from rodent barrel cortex studies (Lefort et al., 2009). Neurons are modeled with leaky 
        [integrate-and-fire dynamics](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html#integrate-and-fire), and [synapses](https://docs.simbrain.net/docs/network/synapses/) using 
        [short-term plasticity](https://docs.simbrain.net/docs/network/spikeresponders/shortTermPlasticity.html) to capture dynamic changes in [synaptic strength](https://docs.simbrain.net/docs/network/synapses/hebbian.html#hebbian-rule) during activity.

        The simulation uses [sparse connectivity](https://docs.simbrain.net/docs/network/connections/sparse.html) with varying synaptic strengths and delays to reflect realistic cortical connectivity. 
        [Excitatory and inhibitory](https://docs.simbrain.net/docs/network/networkDialogs.html#excitatory--inhibitory-ratio) neuron populations are assigned according to experimentally observed ratios 
        (~`20%` inhibitory neurons). Connection densities between layers are based on the empirical data shown above.

        # What to Do

        Exploring layer dynamics:

        1. Click `Run` to start the simulation.
        
        2. Use the `wand` tool with the `Force Spike` action to force spikes in `Layer 5/6`. Few spikes in the other layers should be observed.
        
        3. Use the wand on `Layer 4`. A burst of activity in `Layer 2/3` should then be observed, followed by a burst of activity in `Layer 5/6`, consistent with known connectivity.
        
        4. Use the wand on `Layer 2/3`. This should lead, after some delay, to activity in `Layer 5/6`, and then to a burst of activity in `Layer 4`.

        # References

        Douglas, R. J., & Martin, K. A. (2004). [Neuronal circuits of the neocortex](https://www.cns.nyu.edu/~tony/vns/readings/douglas-martin-2004.pdf). *Annual Review of Neuroscience*, *27*, 419–451.

        Lefort, S., Tomm, C., Sarria, J. C. F., & Petersen, C. C. H. (2009). [The excitatory neuronal network of the C2 barrel column in mouse primary somatosensory cortex](https://doi.org/10.1016/j.neuron.2008.12.020). *Neuron*, *61*(2), 301–316.

        # Credits

        [John Beggs](http://www.beggslab.com/about-john.html)

        Elijah Olson

        Zoë Tosi

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
  
        """.trimIndent()
    )
}
