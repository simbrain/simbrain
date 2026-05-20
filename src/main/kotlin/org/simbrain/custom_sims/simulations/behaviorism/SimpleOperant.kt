package org.simbrain.custom_sims.simulations.behaviorism

import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.layouts.LineLayout
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.place
import org.simbrain.util.updateAction
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import kotlin.math.max

/**
 * Simulation to demonstrate simple operant conditioning.
 *
 * @author Tim Meyer
 * @author Jeff Yoshimi
 */
val simpleOperant = newSim("simple operant conditioning") {

    val numNeurons = 3
    workspace.clearWorkspace()

    val nc = addNetworkComponent("Behaviors")
    withGui {
        place(nc, 239, 10, 575, 500)
    }
    val behaviorNet = nc.network.addNeuronCollection(numNeurons).apply {
        label = "Behaviors"
        layout = LineLayout(100.0, LineLayout.LineOrientation.HORIZONTAL)
        setLocation(-9.25, 95.93)
        applyLayout(-5, -85)
        isClamped = true
    }

    // Use aux values to store firing probabilities
    behaviorNet.getNeuron(0).auxValue = .34
    behaviorNet.getNeuron(1).auxValue = .33
    behaviorNet.getNeuron(2).auxValue = .33

    setUpSimpleOperantWorkpace(workspace)

}.registerReopenFunction { workspace -> setUpSimpleOperantWorkpace(workspace) }


suspend fun SimulationScope.setUpSimpleOperantWorkpace(workspace: Workspace) {

    val network = workspace.componentList.filterIsInstance<NetworkComponent>().first().network
    val behaviorNet = network.getModelByLabel<NeuronCollection>("Behaviors")
    val nodeToLabel = HashMap<Neuron, String>()
    nodeToLabel[behaviorNet.getNeuron(0)] = "Wiggle"
    nodeToLabel[behaviorNet.getNeuron(1)] = "Explore"
    nodeToLabel[behaviorNet.getNeuron(2)] = "Spin"

    fun setWinningNode(nodeIndex: Int) {
        for (i in 0 until behaviorNet.size) {
            if (i == nodeIndex) {
                behaviorNet.getNeuron(i).activation = 1.0
            } else {
                behaviorNet.getNeuron(i).activation = 0.0
            }
        }
    }

    // Add custom network update action
    network.updateManager.addAction(updateAction("Custom behaviorism update") {
        // Select "winning" neuron based on its probability
        // TODO: There must be a better, generalizable way to do this
        val random = Math.random()
        if (random < behaviorNet.getNeuron(0).auxValue) {
            setWinningNode(0)
        } else if (random < (behaviorNet.getNeuron(0).auxValue
                    + behaviorNet.getNeuron(1).auxValue)
        ) {
            setWinningNode(1)
        } else {
            setWinningNode(2)
        }
    })

    withGui {

        fun updateNodeLabels() {
            for (n in behaviorNet.neuronList) {
                n.label = "" + nodeToLabel[n] + ": " + SimbrainMath.roundDouble(n.auxValue, 2)
            }
        }
        updateNodeLabels()

        // Initialize labels
        fun normalizeProbabilities() {
            var totalMass = 0.0
            for (n in behaviorNet.neuronList) {
                totalMass += n.auxValue
            }
            for (n in behaviorNet.neuronList) {
                n.auxValue /= totalMass
            }
        }

        createControlPanel("Control Panel", 5, 10) {

            addButton("Reward Agent") {
                for (n in behaviorNet.neuronList) {
                    if (n.activation > 0) {
                        val p = n.auxValue
                        n.auxValue = max(p + .1 * p, 0.0)
                    }
                }
                normalizeProbabilities()
                updateNodeLabels()
                SimbrainDesktop.workspace.simpleIterate()
            }

            addButton("Punish Agent") {
                for (n in behaviorNet.neuronList) {
                    if (n.activation > 0) {
                        val p = n.auxValue
                        n.auxValue = max(p - .1 * p, 0.0)
                    }
                }
                normalizeProbabilities()
                updateNodeLabels()
                SimbrainDesktop.workspace.simpleIterate()
            }

            addButton("Don't reinforce or punish") {
                SimbrainDesktop.workspace.simpleIterate()
            }
        }

        addSidebarInfo(
            """
            # Simple Operant
            
            This simulation demonstrates simple or "free" operant conditioning using a probabilistic model of behavior. A simple operant is a behavior that occurs spontaneously without 
            requiring any external stimulus to trigger it. The organism simply emits the behavior as part of its natural behavioral repertoire, and the frequency of that behavior can be 
            shaped through reinforcement and punishment.
            
            For example, imagine a rat in an operant chamber that spontaneously engages in various behaviors like standing, grooming, sniffing, and lever pressing. If the experimenter 
            selectively rewards only lever pressing (by delivering food whenever it occurs), that behavior will increase in frequency even though no signal tells the rat when to press. 
            Similarly, an infant spontaneously babbles different sounds, and when parents get excited about certain sounds (like "mama"), those vocalizations become more frequent.
            
            The three nodes represent different behaviors (`Wiggle`, `Explore`, `Spin`) that are initially performed with equal probability. Through selective reinforcement and punishment, 
            you can shape the agent's spontaneous behavior to favor desired actions. This differs from more complex operant conditioning scenarios where behaviors come under the control of 
            discriminative stimuli. For an example of that, see the `Operant with Environment` simulation, which builds on this one by adding controlling stimuli that signal when certain behaviors will be reinforced.

            # Simulation Details
            
            In this model:
            - Three behavior neurons represent possible actions the agent can take
            - Initial probabilities (visible in labels beneath the neurons) are equal (`0.33` each) so all behaviors are equally likely
            - Winning behavior is selected based on sampling from current probabilities each time step
            - Learning occurs when you reward or punish the currently active behavior
            
            When you reward a behavior, its probability increases. When you punish it, the probability decreases. After each learning event, probabilities are normalized so they always
            sum to `1.0`.

            # What to Do
            
            1. Run the simulation to see random behavior selection based on current probabilities
            
            2. Choose a target behavior you want the agent to learn (e.g., make it always `Wiggle`)
            
            3. Apply reinforcement:
               - When your target behavior is active (activation = `1.0`), click `Reward Agent`
               - When other behaviors are active, click `Punish Agent`
               - You can also click `Don't reinforce or punish` to just observe without learning
            
            4. Monitor the probabilities shown in the neuron labels. They should shift toward your target behavior over time.
            
            5. Continue training until the desired behavior becomes highly probable
            
            6. Experiment with different reinforcement schedules or try training different behaviors

            # References
            
            Skinner, B. F. (1938). [_The behavior of organisms: An experimental analysis_](https://pure.mpg.de/rest/items/item_2398357/component/file_2398356/content). Appleton-Century-Crofts.

            # Credits
            
            Tim Meyer
            
            [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
            
            """.trimIndent()
        )
    }
}




