package org.simbrain.custom_sims.simulations.cortex

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.addNeuronGroup
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.place
import org.simbrain.util.sample
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.LogNormalDistribution
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

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
val cortexSimple = newSim {
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

    // Clear workspace
    workspace.clearWorkspace()

    // Build network
    val nc = addNetworkComponent(
        "Cortical Simulation",
    )

    val net = nc.network

    withGui {
        place(nc, 10, 10, 550, 800)
    }

    fun buildLayer(
        numNeurons: Int,
        restingPotential: ClosedRange<Double>,
        timeConstant: ClosedRange<Double>,
        threshold: ClosedRange<Double>,
        resistance: ClosedRange<Double>
    ): NeuronGroup {
        return with(Random) {
            net.addNeuronGroup(numNeurons) {
                updateRule = IntegrateAndFireRule().also {
                    it.restingPotential = restingPotential.sample()
                    it.timeConstant = timeConstant.sample()
                    it.threshold = threshold.sample()
                    it.resistance = resistance.sample()
                    it.backgroundCurrent = 0.0
                    it.resetPotential = restingPotential.sample()
                }
            }
        }
    }

    fun connectLayers(
        src: NeuronGroup, tar: NeuronGroup,
        sparsity: Double
    ): SynapseGroup {
        val exRand: ProbabilityDistribution = LogNormalDistribution(exlocation, exscale, false)
        val inRand: ProbabilityDistribution = LogNormalDistribution(exlocation, exscale, true)
        val con = Sparse(sparsity, false, false)
        con.connectionDensity = 0.65
        val sg = SynapseGroup(src, tar, con)
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
        net.addNetworkModelAsync(sg)
        return sg
    }

    fun random3Position(data: DoubleArray, xlim: DoubleArray, ylim: DoubleArray, zlim: DoubleArray) {
        data[0] = ThreadLocalRandom.current().nextDouble(xlim[0], xlim[1])
        data[1] = ThreadLocalRandom.current().nextDouble(ylim[0], ylim[1])
        data[2] = ThreadLocalRandom.current().nextDouble(zlim[0], zlim[1])
    }

    //    Group locations (503.25,-521.61). (-174.62,328.62). (481.16,1268.68).
    suspend fun buildNetwork() {
        net.timeStep = 0.2

        fun range(start: Double, delta: Double) = start..(start + delta)

        // Make the layers.  Params from Petersen, 2009.
        val btwnLayerSpacing = 150
        // resting potential, time constant, threshold, resistance
        val layer_23 = buildLayer(
            numNeuPerLay,
            range(-71.5, .35),
            range(29.0, 0.45),
            range(-38.4, 0.2),
            range(190.0, 4.0)
        )
        layer_23.label = "Layer 2/3"
        val layer_4 = buildLayer(
            numNeuPerLay,
            range(-66.0, 0.3),
            range(34.8, 0.5),
            range(-39.7, 0.2),
            range(302.0, 4.0)
        )
        layer_4.label = "Layer 4"
        val layer_56 = buildLayer(
            numNeuPerLay,
            range(-62.8, 0.2),
            range(31.7, 0.65),
            range(-40.0, 0.25),
            range(187.0, 4.0)
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
        val synGroups: MutableMap<String, SynapseGroup> = HashMap()
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
            // for (s in sg!!.synapses) {
            //     s.delay = getDelay(
            //         s.source.position3D, s.target.position3D,
            //         Math.sqrt((2 * (600 * 600) + 2000 * 2000).toDouble()), 20.0
            //     )
            // }
            sg!!.label = sgn
        }
    }

    fun getDelay(xyz1: DoubleArray?, xyz2: DoubleArray?, maxDist: Double, maxDly: Double): Int {
        val dist = SimbrainMath.distance(xyz1, xyz2)
        return (dist / maxDist * maxDly / net.timeStep).toInt()
    }

    buildNetwork()
}