package org.simbrain.custom_sims.simulations.behaviorism

import org.simbrain.custom_sims.Simulation
import org.simbrain.custom_sims.helper_classes.ControlPanel
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.neurongroups.getWinner
import org.simbrain.util.math.SimbrainMath
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.updater.create
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import kotlin.math.max

/**
 * Simulation to demonstrate classical and operant conditioning.
 * Discriminative case
 *
 * @author Tim Meyer
 * @author Jeff Yoshimi
 */
class OperantWithEnvironment : Simulation {
    // Network
    lateinit var nc: NetworkComponent
    lateinit var net: Network
    lateinit var panel: ControlPanel
    lateinit var behaviorNet: NeuronGroup
    lateinit var stimulusNet: NeuronGroup
    lateinit var rewardNeuron: Neuron
    lateinit var punishNeuron: Neuron
    var nodeToLabel: MutableMap<Neuron?, String?> = HashMap()
    val numNeurons: Int = 3
    var firingProbabilities: DoubleArray = DoubleArray(numNeurons)
    var winningNode: Int = 0
    private set(nodeIndex) {
        field = nodeIndex
        for (i in 0 until behaviorNet.size()) {
            if (i == nodeIndex) {
                behaviorNet.getNeuron(i)!!.addInputValue(1.0)
                behaviorNet.getNeuron(i)!!.activation = 1.0
            } else {
                behaviorNet.getNeuron(i)!!.addInputValue(0.0)
                behaviorNet.getNeuron(i)!!.activation = 0.0
            }
        }
    }

    // World
    lateinit var oc: OdorWorldComponent
    lateinit var mouse: OdorWorldEntity
    lateinit var cheese: OdorWorldEntity
    lateinit var flower: OdorWorldEntity
    lateinit var fish: OdorWorldEntity

    constructor() : super()

    constructor(desktop: SimbrainDesktop?) : super(desktop)

    override fun run() {
        // Clear workspace

        sim.workspace.clearWorkspace()
        nc = sim.addNetwork(155, 9, 575, 500, "Brain")
        net = nc.getNetwork()

        // Behavioral nodes
        behaviorNet = net.addNeuronGroup(-9.25, 95.93, numNeurons)
        behaviorNet.layout = LineLayout(100.0, LineLayout.LineOrientation.HORIZONTAL)
        behaviorNet.applyLayout()
        behaviorNet.label = "Behaviors"

        // Stimulus nodes
        stimulusNet = net.addNeuronGroup(-9.25, 295.93, numNeurons)
        stimulusNet.layout = LineLayout(100.0, LineLayout.LineOrientation.HORIZONTAL)
        stimulusNet.applyLayout()
        stimulusNet.setClamped(true)
        stimulusNet.label = "Stimuli"
        stimulusNet.setIncrement(1.0)

        // Reward and punish nodes
        rewardNeuron = net.addNeuron(
            stimulusNet.maxX.toInt() + 100,
            stimulusNet.centerY.toInt()
        )
        rewardNeuron.label = "Food Pellet"
        punishNeuron = net.addNeuron(
            rewardNeuron.x.toInt() + 100,
            stimulusNet.centerY.toInt()
        )
        punishNeuron.label = "Shock"

        // Set base text for behavior labels
        nodeToLabel[behaviorNet.getNeuron(0)] = "Wiggle"
        nodeToLabel[behaviorNet.getNeuron(1)] = "Explore"
        nodeToLabel[behaviorNet.getNeuron(2)] = "Spin"

        // Set stimulus labels
        stimulusNet.getNeuron(0)!!.label = "Candle"
        stimulusNet.getNeuron(1)!!.label = "Flower"
        stimulusNet.getNeuron(2)!!.label = "Bell"

        // Use aux values to store "intrinsic" firing probabilities for behaviors
        behaviorNet.getNeuron(0)!!.auxValue = .33 // Node 0
        behaviorNet.getNeuron(1)!!.auxValue = .33 // Node 1
        behaviorNet.getNeuron(2)!!.auxValue = .33 // Node 2

        // Initialize behaviorism labels
        updateNodeLabels()

        // Clear selection
        sim.getNetworkPanel(nc).selectionManager.clear()

        // Connect the layers together
        val syns: List<Synapse> = with(net) { connectAllToAll(stimulusNet, behaviorNet) }
        for (s in syns) {
            s.strength = 0.0
        }

        // network.fireSynapsesUpdated(); // TODO: [event]

        // Create the odor world
        oc = sim.addOdorWorld(730, 7, 315, 383, "Three Objects")
        oc.getWorld().isObjectsBlockMovement = false
        oc.getWorld().isUseCameraCentering = false
        mouse = oc.getWorld().addEntity(120, 245, EntityType.MOUSE)
        mouse.heading = 90.0

        // Set up world
        cheese = oc.getWorld().addEntity(27, 20, EntityType.CANDLE)
        flower = oc.getWorld().addEntity(79, 20, EntityType.PANSY)
        fish = oc.getWorld().addEntity(125, 20, EntityType.FISH)


        // Set up object sensors
        val cheeseSensor = mouse.addObjectSensor(EntityType.SWISS, 50.0, 0.0, 65.0)
        val flowerSensor = mouse.addObjectSensor(EntityType.PANSY, 50.0, 0.0, 65.0)
        val fishSensor = mouse.addObjectSensor(EntityType.FISH, 50.0, 0.0, 65.0)

        // Couple agent to network
        sim.couple(cheeseSensor, stimulusNet.getNeuron(0))
        sim.couple(flowerSensor, stimulusNet.getNeuron(1))
        sim.couple(fishSensor, stimulusNet.getNeuron(2))

        // Add custom network update action
        net.updateManager.clear()
        net.updateManager.addAction(create("Custom behaviorism update") {
            updateNetwork()
            updateBehaviors()
        })

        setUpControlPanel()
    }

    /**
     * Update behavior of odor world agent based on which node is active.
     * Assumes behaviors partitioned into increments of (currently) 100 time steps
     */
    private fun updateBehaviors() {
        val loopTime = sim.workspace.time % 10

        // Node 0: Wiggle
        if (winningNode == 0) {
            if (loopTime < 5) {
                mouse.heading = mouse.heading + 5
            } else {
                mouse.heading = mouse.heading - 5
            }
        } else if (winningNode == 1) {
            if (Math.random() < .2) {
                mouse.heading = mouse.heading + Math.random() * 20 - 10
            }
            mouse.speed = 2.5
        } else {
            mouse.heading = mouse.heading + 20
        }
    }

    private fun updateNetwork() {
        // Update actual firing probabilities, which combine
        // intrinsic probabilities with weighted inputs

        for (i in 0 until behaviorNet.size()) {
            val n = behaviorNet.getNeuron(i)
            firingProbabilities[i] = n!!.weightedInputs + n.auxValue
        }
        if (SimbrainMath.getMinimum(firingProbabilities) < 0) {
            firingProbabilities = SimbrainMath.minMaxNormalize(firingProbabilities)
        }
        firingProbabilities = SimbrainMath.normalizeVec(firingProbabilities)

        // System.out.println(Arrays.toString(firingProbabilities));

        // Select "winning" neuron based on its probability
        val random = Math.random()
        if (random < firingProbabilities[0]) {
            winningNode = 0
        } else if (random < firingProbabilities[0] + firingProbabilities[1]) {
            winningNode = 1
        } else {
            winningNode = 2
        }

        net.bufferedUpdate()
    }

    private fun setUpControlPanel() {
        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10, 150, 189)

        panel.addButton("Reward", Runnable {
            learn(1.0)
            rewardNeuron.forceSetActivation(1.0)
            punishNeuron.forceSetActivation(0.0)
            sim.iterate()
        })

        panel.addButton("Punish", Runnable {
            learn(-1.0)
            rewardNeuron.forceSetActivation(0.0)
            punishNeuron.forceSetActivation(1.0)
            sim.iterate()
        })

        panel.addButton("Do nothing", Runnable {
            rewardNeuron.forceSetActivation(0.0)
            punishNeuron.forceSetActivation(0.0)
            sim.iterate()
        })
    }

    private fun learn(valence: Double) {
        var valence = valence
        val rewardLearningRate = .1
        val punishLearningRate = .1

        valence *= if (valence > 0) {
            rewardLearningRate
        } else {
            punishLearningRate
        }

        val totalActivation = stimulusNet.neuronList.stream().mapToDouble(Neuron::activation).sum()
        val winner = getWinner(behaviorNet.neuronList, true)

        // If there are inputs, update weights
        if (totalActivation > .1) {
            val src = getWinner(stimulusNet.neuronList, true)
            val s_r = getSynapse(src!!, winner!!)
            // Strengthen or weaken active S-R Pair
            s_r!!.strength = s_r.strength + valence
        } else {
            // Else update intrinsic probability
            val p = winner!!.auxValue
            winner.auxValue = max(p + valence * p, 0.0)
            normIntrinsicProbabilities()
            updateNodeLabels()
        }
    }

    private fun normIntrinsicProbabilities() {
        var totalMass = 0.0
        for (n in behaviorNet.neuronList) {
            totalMass += n.auxValue
        }
        for (n in behaviorNet.neuronList) {
            n.auxValue = n.auxValue / totalMass
        }
    }

    private fun updateNodeLabels() {
        for (n in behaviorNet.neuronList) {
            n.label = (nodeToLabel[n] + ": "
                    + SimbrainMath.roundDouble(n.auxValue, 2))
        }
    }

    private val submenuName: String
        get() = "Behaviorism"

    override fun getName(): String {
        return "Operant Conditioning (Environment)"
    }

    override fun instantiate(desktop: SimbrainDesktop): OperantWithEnvironment {
        return OperantWithEnvironment(desktop)
    }
}
