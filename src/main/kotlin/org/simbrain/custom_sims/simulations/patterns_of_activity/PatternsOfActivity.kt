package org.simbrain.custom_sims.simulations.patterns_of_activity

import org.simbrain.custom_sims.Simulation
import org.simbrain.network.connections.*
import org.simbrain.network.core.*
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.learningrules.STDPRule
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.spikeresponders.SpikeResponder
import org.simbrain.network.spikeresponders.UDF
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.projection.MarkovColoringManager
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.*

/**
 * Four outputs are connected to two continuous valued neurons that control the
 * x and y velocity of the mouse.
 *
 * The right two of the four spiking neurons each send one connection to the
 * rightmost continuous valued one, one with a strength of +1 and another with a
 * strength of -1. The same is the case for the two spiking neurons and one
 * continuous valued one on the left. Basically this results in each of the four
 * spiking neuron being responsible for up, down, left, and right movement
 * independently.
 *
 * Synapses at every stage from the sensory net to the recurrent portion, within
 * the recurrent portion, and from the recurrent portion to the four outputs are
 * subjected to both STDP and synaptic normalization.
 *
 * Each spiking neurons receives connections from only one spatial quadrant of
 * the reservoir. So for example the UP spiking output only receives inputs from
 * the (I think...) bottom left 256 recurrent neurons, while the DOWN spiking
 * output receives inputs from only the bottom right 256 recurrent neurons.
 * Connections within the recurrent neurons are based on distance in a gaussian
 * manner but with the parameters tuned to down regulate tails.
 */
class PatternsOfActivity : Simulation {
    // References
    lateinit var net: Network

    private val netSize = 1024
    private val spacing = 40
    private val maxDly = 12
    private var recurrentNetwork: NeuronGroup? = null
    private val dispersion = 140
    var maxDist: Double = sqrt((2 * netSize * spacing).toDouble())
    private val ruleEx = STDPRule(
        5.0, 1.0, 20.0, 100.0, 0.001,
        true
    )
    private val ruleExRec = STDPRule(
        5.2, .8, 25.0, 100.0, 0.001,
        true
    )
    private val ruleIn = STDPRule(
        2.5, -2.5, 40.0, 40.0, 0.001,
        true
    )
    private val spkR: SpikeResponder = UDF()
    private val quadrantDensity = 0.5

    private val rtQuad: MutableList<Neuron> = ArrayList()
    private val lfQuad: MutableList<Neuron> = ArrayList()
    private val dwQuad: MutableList<Neuron> = ArrayList()
    private val upQuad: MutableList<Neuron> = ArrayList()

    private var rtNeuron: Neuron? = null
    private var lfNeuron: Neuron? = null
    private var dwNeuron: Neuron? = null
    private var upNeuron: Neuron? = null

    private val frEsts = DoubleArray(netSize + 4)
    private val graphicVal = DoubleArray(netSize + 4)


    private var outputNeurons: NeuronGroup? = null
    var mouse: OdorWorldEntity? = null

    override fun run() {
        // Clear workspace
        sim.workspace.clearWorkspace()

        // Set up network
        val nc = sim.addNetwork(
            10, 10, 543, 545,
            "Patterns of Activity"
        )
        net = nc.network
        net.timeStep = 0.5

        // Set up sensory group and odor world
        val sensoryNetL = net.addNeuronGroup(-9.25, 95.93, 5)
        // TODO: Removed random spike chance for now
        sensoryNetL.setUpdateRule(IntegrateAndFireRule())
        sensoryNetL.setPolarity(Polarity.EXCITATORY)
        sensoryNetL.label = "Sensory Left"

        // Set up sensory group and odor world
        val sensoryNetR = net.addNeuronGroup(-9.25, 155.93, 5)
        sensoryNetR.setUpdateRule(IntegrateAndFireRule())
        sensoryNetR.setPolarity(Polarity.EXCITATORY)
        sensoryNetR.label = "Sensory Right"

        // Build odor world
        mouse = buildWorld()

        // Create network
        buildNetwork(sensoryNetL, sensoryNetR, mouse)

        // Set up plots
        addProjection(sensoryNetL, 8, 562, 1.0, "getActivationArray")
        addProjection(recurrentNetwork, 368, 562, 24.0, "getSubsampledActivations")
        addProjection(outputNeurons, 723, 562, .1, "getActivationArray")
    }

    private fun buildWorld(): OdorWorldEntity {
        // Set up odor world
        val oc = sim.addOdorWorld(547, 5, 504, 548, "World")
        oc.world.isObjectsBlockMovement = false
        oc.world.isUseCameraCentering = false
        oc.world.tileMap = loadTileMap("empty.tmx")
        val mouse = oc.world.addEntity(120, 245, EntityType.MOUSE)
        mouse.addSensor(SmellSensor("Smell-Right", 36.0, 40.0))
        mouse.addSensor(SmellSensor("Smell-Left", -36.0, 40.0))
        mouse.heading = 90.0
        val cheese = oc.world.addEntity(
            72, 220, EntityType.SWISS,
            doubleArrayOf(18.0, 0.0, 5.0, 10.0, 5.0)
        )
        cheese.smellSource.dispersion = dispersion.toDouble()
        val flower = oc.world.addEntity(
            190, 221, EntityType.FLOWER,
            doubleArrayOf(3.0, 18.0, 2.0, 5.0, 10.0)
        )
        flower.smellSource.dispersion = dispersion.toDouble()
        val cow = oc.world.addEntity(
            90, 50, EntityType.COW,
            doubleArrayOf(3.0, 7.0, 16.0, 19.0, 0.0)
        )
        cow.smellSource.dispersion = dispersion.toDouble()
        val lion = oc.world.addEntity(
            300, 54, EntityType.LION,
            doubleArrayOf(5.0, 2.0, 13.0, 16.0, 0.0)
        )
        lion.smellSource.dispersion = dispersion.toDouble()
        val susi = oc.world.addEntity(
            97, 331, EntityType.SUSI,
            doubleArrayOf(0.0, 12.0, 15.0, 20.0)
        )
        susi.smellSource.dispersion = dispersion.toDouble()
        val steve = oc.world.addEntity(
            315, 305, EntityType.STEVE,
            doubleArrayOf(12.0, 0.0, 20.0, 15.0)
        )
        steve.smellSource.dispersion = dispersion.toDouble()
        return mouse
    }

    private fun buildNetwork(sensoryNetL: NeuronGroup, sensoryNetR: NeuronGroup, mouse: OdorWorldEntity?) {
        // Set up Recurrent portion
        val neuronList: MutableList<Neuron> = ArrayList()
        for (ii in 0 until netSize) {
            val n = Neuron()
            n.updateRule = NormIFRule(ii)
            if (Math.random() < 0.8) {
                n.polarity = Polarity.EXCITATORY
                (n.updateRule as IntegrateAndFireRule).timeConstant = 30.0
                (n.updateRule as IntegrateAndFireRule).refractoryPeriod = 3.0
            } else {
                n.polarity = Polarity.INHIBITORY
                (n.updateRule as IntegrateAndFireRule).timeConstant = 20.0
                (n.updateRule as IntegrateAndFireRule).refractoryPeriod = 2.0
            }
            (n.updateRule as IntegrateAndFireRule).addNoise = true
            (n.updateRule as IntegrateAndFireRule).backgroundCurrent = 18.0
            (n.updateRule as IntegrateAndFireRule).noiseGenerator = NormalDistribution(0.0, 0.2)
            neuronList.add(n)
        }
        recurrentNetwork = NeuronGroup(neuronList)
        recurrentNetwork!!.label = "Recurrent network"
        HexagonalGridLayout(spacing.toDouble(), spacing.toDouble(), sqrt(netSize.toDouble()).toInt())
            .layoutNeurons(recurrentNetwork!!.neuronList)
        sensoryNetL.setLocation(recurrentNetwork!!.maxX + 300, recurrentNetwork!!.minY + 100)
        sensoryNetR.setLocation(recurrentNetwork!!.maxX + 300, recurrentNetwork!!.minY + 100)

        // Set up recurrent synapses
        val recSyns = SynapseGroup(
            recurrentNetwork!!, recurrentNetwork!!,
            RadialGaussian(
                DEFAULT_EE_CONST * 3, DEFAULT_EI_CONST * 3,
                DEFAULT_IE_CONST * 3, DEFAULT_II_CONST * 3, .25, 200.0
            )
        )
        // new Sparse(0.10, false, false)
        //        .connectNeurons(recSyns);
        // initializeSynParameters(recSyns);
        // TODO
        // recSyns.setLearningRule(ruleExRec, Polarity.EXCITATORY);
        for (n in neuronList) {
            for (m in neuronList) {
                if (Math.random() < 0.002 && n != m) {
                    val s = Synapse(n, m)
                    s.strength = n.polarity.value(20.0)
                    recSyns.addSynapse(s)
                    // Delays based on distance
                }
            }
        }
        for (s in recSyns.synapses) {
            val d = SimbrainMath.distance(s.source.position3D, s.target.position3D)
            s.delay = (maxDly * d / maxDist).toInt()
        }

        // Set up input synapses (connections from sensory group to the recurrent group)
        val inpSynGL = SynapseGroup(
            sensoryNetL, recurrentNetwork!!,
            Sparse(0.25, true, false)
        )
        // initializeSynParameters(inpSynGL);
        // TODO
        // inpSynGL.setStrength(50, Polarity.EXCITATORY);
        // inpSynGL.setStrength(-10, Polarity.INHIBITORY);
        for (s in inpSynGL.synapses) {
            s.delay = ThreadLocalRandom.current().nextInt(2, maxDly / 2)
            if (s.target.polarity === Polarity.INHIBITORY) {
                inpSynGL.removeSynapse(s)
            }
        }
        val inpSynGR = SynapseGroup(
            sensoryNetR, recurrentNetwork!!,
            Sparse(0.25, true, false)
        )
        // initializeSynParameters(inpSynGR);
        // TODO
        // inpSynGR.setStrength(50, Polarity.EXCITATORY);
        // inpSynGL.setStrength(-10, Polarity.INHIBITORY);
        for (s in inpSynGR.synapses) {
            s.delay = ThreadLocalRandom.current().nextInt(2, maxDly / 2)
            if (s.target.polarity === Polarity.INHIBITORY) {
                inpSynGR.removeSynapse(s)
            }
        }

        // Set up the first out group (comprised of LIF neurons to allow for STDP)
        val outGroup = NeuronGroup(4)
        var tmp = 0
        for (n in outGroup.neuronList) {
            if (tmp % 2 == 1) {
                n.polarity = Polarity.INHIBITORY
            } else {
                n.polarity = Polarity.EXCITATORY
            }
            n.updateRule = NormIFRule(netSize + tmp)
            //((IntegrateAndFireRule) n.getUpdateRule()).setNoiseGenerator(NormalDistribution.builder()
            //        .ofMean(0).ofStandardDeviation(0.2).build());
            (n.updateRule as IntegrateAndFireRule).backgroundCurrent = 19.9
            tmp++
        }
        rtNeuron = outGroup.getNeuron(0)
        lfNeuron = outGroup.getNeuron(1)
        dwNeuron = outGroup.getNeuron(2)
        upNeuron = outGroup.getNeuron(3)
        outGroup.setLocation(recurrentNetwork!!.maxX + 300, recurrentNetwork!!.minY + 800)

        // Set up the synapses between the recurrent network and the output
        // Each neuron recieves from one quadrant of the recurrent neurons in terms of location
        val rec2out = SynapseGroup(recurrentNetwork!!, outGroup)
        // initializeSynParameters(rec2out);
        val xEdge = recurrentNetwork!!.centerX
        val yEdge = recurrentNetwork!!.centerY
        for (ii in 0 until netSize) {
            val n = neuronList[ii]
            if (n.polarity === Polarity.INHIBITORY) {
                continue
            }
            val x = n.x
            val y = n.y
            if (y > yEdge) {
                if (x < xEdge && Math.random() < quadrantDensity) {
                    val s = Synapse(n, outGroup.getNeuron(2))
                    s.delay = ThreadLocalRandom.current().nextInt(5, 10)
                    rec2out.addSynapse(s)
                    dwQuad.add(n)
                    continue
                }
                if (x > xEdge && Math.random() < quadrantDensity) {
                    val s = Synapse(n, outGroup.getNeuron(3))
                    s.delay = ThreadLocalRandom.current().nextInt(5, 10)
                    upQuad.add(n)
                    rec2out.addSynapse(s)
                    continue
                }
            } else {
                if (x < xEdge && Math.random() < quadrantDensity) {
                    rtQuad.add(n)
                    val s = Synapse(n, outGroup.getNeuron(0))
                    s.delay = ThreadLocalRandom.current().nextInt(5, 10)
                    rec2out.addSynapse(s)
                    continue
                }
                if (x > xEdge && Math.random() < quadrantDensity) {
                    lfQuad.add(n)
                    val s = Synapse(n, outGroup.getNeuron(1))
                    s.delay = ThreadLocalRandom.current().nextInt(5, 10)
                    rec2out.addSynapse(s)
                    continue
                }
            }
        }

        // rec2out.setConnectionManager(new AllToAll());

        // Set up the neurons that read from the spiking outputs (converting it to a continuous value) which
        // are coupled to the X and Y velocities of the mouse
        outputNeurons = NeuronGroup(2)
        outputNeurons!!.label = "Motor outputs"
        outputNeurons!!.setLocation(recurrentNetwork!!.maxX + 350, recurrentNetwork!!.minY + 380)
        for (n in outputNeurons!!.neuronList) {
            n.updateRule = SigmoidalRule()
            (n.updateRule as SigmoidalRule).lowerBound = -4.0
            (n.updateRule as SigmoidalRule).upperBound = 4.0
            (n.updateRule as SigmoidalRule).slope = 4.0
            n.upperBound = 4.0
            n.lowerBound = -4.0
        }

        // Set up the connections to the read out neurons
        val out2read = SynapseGroup(outGroup, outputNeurons!!)

        // TODO
        // out2read.setSpikeResponder(new ConvolvedJumpAndDecay(20), Polarity.BOTH);
        // out2read.addSynapse(new Synapse(outGroup.getNeuron(0), outputNeurons.getNeuron(0)));
        // out2read.addSynapse(new Synapse(outGroup.getNeuron(1), outputNeurons.getNeuron(0)));
        // out2read.addSynapse(new Synapse(outGroup.getNeuron(2), outputNeurons.getNeuron(1)));
        // out2read.addSynapse(new Synapse(outGroup.getNeuron(3), outputNeurons.getNeuron(1)));
        // out2read.setDisplaySynapses(false);
        // out2read.setConnectionManager(new AllToAll());
        // out2read.setUpperBound(1000000000, Polarity.BOTH);
        // out2read.setLowerBound(-1000000000, Polarity.BOTH);

        // Make couplings
        sim.couple(
            sim.getProducer(outputNeurons!!.getNeuron(0), "getActivation"),
            sim.getConsumer(mouse, "setVelocityX")
        )
        sim.couple(
            sim.getProducer(outputNeurons!!.getNeuron(1), "getActivation"),
            sim.getConsumer(mouse, "setVelocityY")
        )
        sim.couple(mouse!!.getSensor("Smell-Left") as SmellSensor, sensoryNetL)
        sim.couple(mouse.getSensor("Smell-Right") as SmellSensor, sensoryNetR)

        // Add everything to the network
        net!!.addNetworkModel(recurrentNetwork!!)
        net!!.addNetworkModel(inpSynGL)
        net!!.addNetworkModel(inpSynGR)
        net!!.addNetworkModel(recSyns)
        net!!.addNetworkModel(outGroup)
        net!!.addNetworkModel(rec2out)
        net!!.addNetworkModel(outputNeurons!!)
        net!!.addNetworkModel(out2read)
        net!!.addNetworkModel(sensoryNetL)
        net!!.addNetworkModel(sensoryNetR)

        // Set up concurrent buffered update
        // net.getUpdateManager().clear();
        // net.getUpdateManager().addAction(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(net));
    }


    private fun initializeSynParameters(synG: SynapseGroup) {
        // TODO
        // synG.setLearningRule(ruleEx, Polarity.EXCITATORY);
        // synG.setLearningRule(ruleIn, Polarity.INHIBITORY);
        // synG.setSpikeResponder(spkR, Polarity.BOTH);
        // synG.setUpperBound(200, Polarity.EXCITATORY);
        // synG.setLowerBound(0, Polarity.EXCITATORY);
        // synG.setLowerBound(-200, Polarity.INHIBITORY);
        // synG.setUpperBound(0, Polarity.INHIBITORY);
        synG.connectionStrategy.exRandomizer = NormalDistribution(10.0, 2.5)
        synG.connectionStrategy.inRandomizer = NormalDistribution(-10.0, 2.5)
        synG.randomize()
    }

    constructor(desktop: SimbrainDesktop?) : super(desktop)

    constructor() : super()

    override fun getName(): String {
        return "Patterns of activity"
    }

    override fun instantiate(desktop: SimbrainDesktop): PatternsOfActivity {
        return PatternsOfActivity(desktop)
    }

    /**
     * Extends the normal integrate and fire rule but also normalizes synapses
     */
    private inner class NormIFRule(val index: Int) : IntegrateAndFireRule() {
        private var totalTimeS = 0.0

        private val nv = 1 / ln(4.98)

        private val saturation = 3000.0

        override fun copy(): NormIFRule {
            val ifn = NormIFRule(index)
            ifn.restingPotential = restingPotential
            ifn.resetPotential = resetPotential
            ifn.threshold = threshold
            ifn.backgroundCurrent = backgroundCurrent
            ifn.timeConstant = timeConstant
            ifn.resistance = resistance
            ifn.addNoise = addNoise
            ifn.noiseGenerator = noiseGenerator.copy()
            return ifn
        }

        //
        init {
            frEsts[index] = 0.01
        }

        context(Network)
        override fun apply(n: Neuron, data: SpikingScalarData) {
            val dt = timeStep
            totalTimeS += dt
            val tau_base = dt / 1E5
            // frEsts[index] = (1 - (tau_base * Math.sqrt((spkCounts[index] + 1) / (totalTimeS + 50))) * frEsts[index])
            //        + (n.isSpike() ? 1 : 0) * (tau_base * Math.sqrt((spkCounts[index] + 1) / (totalTimeS + 50)));
            val spk = (if (n.isSpike) 1 else 0).toDouble()
            frEsts[index] = (((1 - tau_base) * frEsts[index])
                    + spk * ((spk) + frEsts[index]) * tau_base) // * tau_base;
            graphicVal[index] = (((1 - 0.1 * dt) * graphicVal[index])
                    + spk * ((spk) + graphicVal[index]) * 0.1 * dt)
            val nrmVal = saturation / (1 + exp(-frEsts[index] * 100)) - saturation / 3
            var totStrEx = 0.0
            var totStrIn = 0.0
            val toR: MutableList<Synapse> = ArrayList()
            for (jj in n.fanIn.indices) {
                if (abs(n.fanIn[jj].strength) < 0.1) {
                    toR.add(n.fanIn[jj])
                    continue
                }
                if (n.fanIn[jj].source.polarity === Polarity.EXCITATORY) {
                    totStrEx += abs(n.fanIn[jj].strength)
                } else {
                    totStrIn += abs(n.fanIn[jj].strength)
                }
            }
            if (java.lang.Double.isInfinite(totStrEx) || java.lang.Double.isNaN(totStrEx) || java.lang.Double.isInfinite(
                    totStrIn
                ) || java.lang.Double.isNaN(totStrIn)
            ) {
                println()
            }
            for (s in toR) {
                s.delete()
            }
            for (jj in n.fanIn.indices) {
                val s = n.fanIn[jj]
                if (s.source.polarity === Polarity.EXCITATORY) {
                    if (totStrEx != 0.0) {
                        s.strength = s.strength * (nrmVal / (totStrEx))
                    }
                } else {
                    if (totStrIn != 0.0) {
                        s.strength = s.strength * (nrmVal / (totStrIn))
                    }
                }
                val dampFac = nv * ln(5 - (abs(s.strength) / 50))
                (s.learningRule as STDPRule).delta_w = ((s.learningRule as STDPRule).delta_w
                        * dampFac)
                if (java.lang.Double.isNaN((s.learningRule as STDPRule).delta_w) ||
                    java.lang.Double.isNaN(s.strength)
                ) {
                    println()
                }
                if (abs(s.strength) > 200) {
                    val sgn = -sign(s.strength)
                    (s.learningRule as STDPRule).delta_w = sgn * abs((s.learningRule as STDPRule).delta_w)
                }
            }

            // TODO: This was here but is it needed?
            // super.apply(n, data);
        }

        override val name: String
            get() = "Norm. Integrate and Fire"

        override fun toString(): String {
            return name
        }

        override fun getGraphicalValue(n: Neuron): Double {
            return 1000 * graphicVal[index]
        }

        override val graphicalLowerBound: Double = 0.0

        override val graphicalUpperBound = 30.0
    }

    private fun addProjection(toPlot: NeuronGroup?, x: Int, y: Int, tolerance: Double, methodName: String) {
        // Create projection component

        val pc = sim.addProjectionPlot(x, y, 362, 320, toPlot!!.label)
        pc.projector.init()
        pc.projector.tolerance = tolerance
        pc.projector.coloringManager = MarkovColoringManager()

        // Coupling
        val inputProducer = sim.getProducer(toPlot, methodName)
        val plotConsumer = sim.getConsumer(pc, "addPoint")
        sim.couple(inputProducer, plotConsumer)

        // Text of nearest world object to projection plot current dot
        val currentObject = sim.getProducer(mouse, "getNearbyObjects")
        val plotText = sim.getConsumer(pc, "setLabel")
        sim.couple(currentObject, plotText)
    }

    private val submenuName: String
        get() = "Cognitive Maps"
}