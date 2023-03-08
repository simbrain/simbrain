package org.simbrain.custom_sims.simulations.cortex

import kotlinx.coroutines.launch
import org.simbrain.custom_sims.Simulation
import org.simbrain.custom_sims.helper_classes.ControlPanel
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.SynapseGroup2
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.LogNormalDistribution
import org.simbrain.workspace.gui.SimbrainDesktop
import java.util.concurrent.ThreadLocalRandom

/**
 * Model of canonical cortex (Douglas and Martin, 2004) using rat barrel cortex
 * as a reference (Lefort, Tomm, Sarria and Petersen, 2009). Users should be
 * able to inject current and see it propagate consistently with empirical
 * studies.
 *
 *
 * Also see Haeusler and Mass, 2007.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class CortexSimple : Simulation {
    // Simulation Parameters
    var NUM_NEURONS = 120
    var GRID_SPACE = 25

    // Location and scale params for lognormal dist of all synapse groups
    var location = -1.0
    var scale = .35
    var exlocation = 0.0
    var exscale = .5
    var inlocation = 1.0
    var inscale = .5
    var numNeuPerLay = 300

    // TODO: Membrane properties
    // TODO: Build using z coordinates
    // References
    var net: Network? = null
    override fun run() {

        // Clear workspace
        sim.workspace.clearWorkspace()

        // Build network
        val nc = sim.addNetwork(
            10, 10, 550, 800,
            "Cortical Simulation"
        )
        net = nc.network
        sim.workspace.launch {
            buildNetwork()
        }

        // Set up control panel
        // controlPanel();
    }

    private fun controlPanel() {
        val panel = ControlPanel.makePanel(sim, "Controller", 5, 10)
        panel.addButton("Inject current") {}
    }

    //    Group locations (503.25,-521.61). (-174.62,328.62). (481.16,1268.68).
    suspend fun buildNetwork() {
        net!!.timeStep = 0.2

        // Make the layers.  Params from Petersen, 2009.
        val btwnLayerSpacing = 150
        // resting potential, time constant, threshold, resistance
        val layer_23 = buildLayer(
            numNeuPerLay,
            doubleArrayOf(-71.5, .35),
            doubleArrayOf(29.0, 0.45),
            doubleArrayOf(-38.4, 0.2),
            doubleArrayOf(190.0, 4.0)
        )
        layer_23.label = "Layer 2/3"
        val layer_4 = buildLayer(
            numNeuPerLay,
            doubleArrayOf(-66.0, 0.3),
            doubleArrayOf(34.8, 0.5),
            doubleArrayOf(-39.7, 0.2),
            doubleArrayOf(302.0, 4.0)
        )
        layer_4.label = "Layer 4"
        val layer_56 = buildLayer(
            numNeuPerLay,
            doubleArrayOf(-62.8, 0.2),
            doubleArrayOf(31.7, 0.65),
            doubleArrayOf(-40.0, 0.25),
            doubleArrayOf(187.0, 4.0)
        )
        layer_56.label = "Layer 5/6"
        val tmp = DoubleArray(3)
        val defMax = (layer_4.size() * 2).toDouble()
        val xlim = doubleArrayOf(0.0, defMax)
        val zlim = doubleArrayOf(0.0, defMax)
        for (ii in 0 until layer_4.size()) {
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
        val synGroups: MutableMap<String, SynapseGroup2> = HashMap()
        synGroups["L2/3 Rec."] = connectLayers(layer_23, layer_23, .12)
        synGroups["L4 Rec."] = connectLayers(layer_4, layer_4, .24)
        synGroups["L5/6 Rec."] = connectLayers(layer_56, layer_56, .24)
        synGroups["L4 \u2192 L2/3"] = connectLayers(layer_4, layer_23, .14)
        synGroups["L2/3 \u2192 L4"] = connectLayers(layer_23, layer_4, .01)
        synGroups["L4 \u2192 L5/6"] = connectLayers(layer_4, layer_56, .08)
        synGroups["L5/6 \u2192 L4"] = connectLayers(layer_56, layer_4, .007)
        synGroups["L2/3 \u2192 L5/6"] = connectLayers(layer_23, layer_56, .08)
        synGroups["L5/6 \u2192 L2/3"] = connectLayers(layer_56, layer_23, .03)
        for (sgn in synGroups.keys) {
            val sg = synGroups[sgn]
            for (s in sg!!.synapses) {
                s.delay = getDelay(
                    s.source.position3D, s.target.position3D,
                    Math.sqrt((2 * (600 * 600) + 2000 * 2000).toDouble()), 20.0
                )
            }
            sg.label = sgn
        }

        // TODO
        // layer_4.fireGroupUpdated();
        // Todo; Add labels

        // Use concurrent buffered update
        // net.getUpdateManager().clear();
        // net.getUpdateManager().addAction(ConcurrentBufferedUpdate
        //     .createConcurrentBufferedUpdate(net));
    }

    private suspend fun buildLayer(
        numNeurons: Int,
        restingPotential: DoubleArray, timeConstant: DoubleArray, threshold: DoubleArray,
        resistance: DoubleArray
    ): NeuronGroup {

        // GridLayout layout = new GridLayout(GRID_SPACE, GRID_SPACE,
        //    (int) Math.sqrt(numNeurons));
        val neurons: MutableList<Neuron> = ArrayList(numNeurons)
        val locR = ThreadLocalRandom.current()
        for (i in 0 until numNeurons) {
            val neuron = Neuron(net)
            val rule = IntegrateAndFireRule()
            rule.restingPotential = restingPotential[0] + locR.nextDouble() * restingPotential[1]
            rule.timeConstant = timeConstant[0] + locR.nextDouble() * timeConstant[1]
            rule.threshold = threshold[0] + locR.nextDouble() * threshold[1]
            rule.resistance = resistance[0] + locR.nextDouble() * resistance[1]
            rule.backgroundCurrent = 0.0
            rule.resetPotential = restingPotential[0] + locR.nextDouble() * restingPotential[1]
            neuron.updateRule = rule
            neuron.lowerBound = rule.restingPotential - 10
            neuron.upperBound = rule.threshold
            neurons.add(neuron)
        }
        val ng = NeuronGroup(net, neurons)
        net!!.addNetworkModel(ng)?.join()
        // ng.setLayout(layout);
        // ng.applyLayout();
        return ng
    }

    private fun connectLayers(
        src: NeuronGroup, tar: NeuronGroup,
        sparsity: Double
    ): SynapseGroup2 {
        val exRand: ProbabilityDistribution = LogNormalDistribution(exlocation, exscale, false)
        val inRand: ProbabilityDistribution = LogNormalDistribution(exlocation, exscale, true)
        val con = Sparse(sparsity, false, false)
        con.connectionDensity = 0.65
        val sg = SynapseGroup2(src, tar, con)
        // sg.setRandomizers(exRand, inRand);
        sg.label = "Synapses"
        sg.displaySynapses = false

        // TODO
        // sg.setUpperBound(200, Polarity.EXCITATORY);
        // sg.setLowerBound(0, Polarity.EXCITATORY);
        // sg.setLowerBound(-200, Polarity.INHIBITORY);
        // sg.setUpperBound(0, Polarity.INHIBITORY);
        //
        // sg.setSpikeResponder(new UDF(), Polarity.BOTH);
        net!!.addNetworkModel(sg)
        return sg
    }

    private fun random3Position(data: DoubleArray, xlim: DoubleArray, ylim: DoubleArray, zlim: DoubleArray) {
        data[0] = ThreadLocalRandom.current().nextDouble(xlim[0], xlim[1])
        data[1] = ThreadLocalRandom.current().nextDouble(ylim[0], ylim[1])
        data[2] = ThreadLocalRandom.current().nextDouble(zlim[0], zlim[1])
    }

    fun getDelay(xyz1: DoubleArray?, xyz2: DoubleArray?, maxDist: Double, maxDly: Double): Int {
        val dist = SimbrainMath.distance(xyz1, xyz2)
        return (dist / maxDist * maxDly / net!!.timeStep).toInt()
    }

    constructor(desktop: SimbrainDesktop?) : super(desktop)
    constructor() : super()

    private val submenuName: String
        private get() = "Brain"

    override fun getName(): String {
        return "Cortical circuit"
    }

    override fun instantiate(desktop: SimbrainDesktop): CortexSimple {
        return CortexSimple(desktop)
    }
}
