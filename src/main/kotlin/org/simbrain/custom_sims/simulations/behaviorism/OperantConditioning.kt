package org.simbrain.custom_sims.simulations.behaviorism

import org.simbrain.custom_sims.Simulation
import org.simbrain.custom_sims.helper_classes.ControlPanel
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.util.math.SimbrainMath
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.updater.create
import kotlin.math.max

/**
 * Simulation to demonstrate classical and operant conditioning.
 * Discriminative case
 *
 * @author Tim Meyer
 * @author Jeff Yoshimi
 */
class OperantConditioning : Simulation {
    // TODO: Test.
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

    constructor() : super()

    constructor(desktop: SimbrainDesktop?) : super(desktop)

    /**
     * Run the simulation!
     */
    override fun run() {
        // Clear workspace

        sim.workspace.clearWorkspace()
        nc = sim.addNetwork(226, 9, 624, 500, "Simulation")
        net = nc.getNetwork()

        // Behavioral nodes
        behaviorNet = net.addNeuronGroup(-14.0, 73.0, numNeurons)
        behaviorNet.layout = LineLayout(100.0, LineLayout.LineOrientation.HORIZONTAL)
        behaviorNet.applyLayout()
        behaviorNet.label = "Behaviors"

        // Stimulus nodes
        stimulusNet = net.addNeuronGroup(-9.8, 269.93, numNeurons)
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
        nodeToLabel[behaviorNet.getNeuron(0)] = "Bar Press"
        nodeToLabel[behaviorNet.getNeuron(1)] = "Jump"
        nodeToLabel[behaviorNet.getNeuron(2)] = "Scratch Nose"

        // Set stimulus labels
        stimulusNet.getNeuron(0).label = "Red Light"
        stimulusNet.getNeuron(1).label = "Green Light"
        stimulusNet.getNeuron(2).label = "Speaker"

        // Use aux values to store "intrinsict" firing probabilities for behaviors
        behaviorNet.getNeuron(0).auxValue = .33
        behaviorNet.getNeuron(1).auxValue = .33
        behaviorNet.getNeuron(2).auxValue = .34

        // Initialize behaviorism labels
        updateNodeLabels()

        // Clear selection
        sim.getNetworkPanel(nc).selectionManager.clear()

        // Connect the layers together
        val syns: List<Synapse> = with(net) { connectAllToAll(stimulusNet, behaviorNet) }
        for (s in syns) {
            s.strength = 0.0
        }

        // Add custom network update action
        net.updateManager.addAction(create("Custom behaviorism update") { this.updateNetwork() })

        setUpControlPanel()
    }

    private fun updateNetwork() {
        // Update firing probabilities

        for (i in 0 until behaviorNet.size()) {
            val n = behaviorNet.getNeuron(i)
            firingProbabilities[i] = n.weightedInputs + n.auxValue
        }

        firingProbabilities = SimbrainMath.normalizeVec(firingProbabilities)

        // System.out.println(Arrays.toString(firingProbabilities));

        // Select "winning" neuron based on its probability
        val random = Math.random()
        if (random < firingProbabilities[0]) {
            setWinningNode(0)
        } else if (random < firingProbabilities[0] + firingProbabilities[1]) {
            setWinningNode(1)
        } else {
            setWinningNode(2)
        }
    }

    private fun setWinningNode(nodeIndex: Int) {
        for (i in 0 until behaviorNet.size()) {
            if (i == nodeIndex) {
                behaviorNet.getNeuron(i).forceSetActivation(1.0)
            } else {
                behaviorNet.getNeuron(i).forceSetActivation(0.0)
            }
        }
    }

    private fun setUpControlPanel() {
        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10, 221, 173)

        panel.addButton("Reward", Runnable {
            learn(1.0)
            rewardNeuron.addInputValue(1.0)
            punishNeuron.forceSetActivation(0.0)
            sim.iterate()
        })

        panel.addButton("Punish", Runnable {
            learn(-1.0)
            rewardNeuron.forceSetActivation(0.0)
            punishNeuron.addInputValue(1.0)
            sim.iterate()
        })

        panel.addButton("Do nothing", Runnable {
            rewardNeuron.forceSetActivation(0.0)
            punishNeuron.addInputValue(0.0)
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

        for (tar in behaviorNet.neuronList) {
            // The "winning" node

            if (tar.activation > 0) {
                // Update intrinsic probability

                val p = tar.auxValue
                tar.auxValue = max(p + valence * p, 0.0)

                // Update weight on active node
                for (src in stimulusNet.neuronList) {
                    if (src.activation > 0) {
                        val s = getSynapse(src, tar)
                        s!!.strength = max(s.strength + valence, 0.0)
                    }
                }
            }
        }
        normIntrinsicProbabilities()
        updateNodeLabels()
        sim.iterate()
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
        return "Operant Conditioning"
    }

    override fun instantiate(desktop: SimbrainDesktop): OperantConditioning {
        return OperantConditioning(desktop)
    }
}
