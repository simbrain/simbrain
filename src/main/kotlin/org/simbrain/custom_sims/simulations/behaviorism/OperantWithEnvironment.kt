package org.simbrain.custom_sims.simulations.behaviorism

import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.neurongroups.getWinner
import org.simbrain.util.*
import org.simbrain.util.piccolo.TileMap
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import kotlin.math.max
import kotlin.random.Random


val operantWithEnvironment = newSim("operant_with_environment") {
    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Brain")
    val network = networkComponent.network

    val numNeurons = 3

    val behaviorNet = network.addNeuronGroup(numNeurons, location = point(-9.25, 95.93)).apply {
        layout = LineLayout(100.0, LineLayout.LineOrientation.HORIZONTAL)
        label = "Behaviors"
        neuronList.labels = listOf("Wiggle: ", "Explore: ", "Spin: ")
        neuronList.forEach { it.auxValue = .33 }
        applyLayout()
    }

    val stimulusNet = network.addNeuronGroup(numNeurons, location = point(-9.25, 295.93)).apply {
        layout = LineLayout(100.0, LineLayout.LineOrientation.HORIZONTAL)
        isClamped = true
        label = "Stimuli"
        setIncrement(1.0)
        neuronList.labels = listOf("Candle", "Flower", "Bell")
        applyLayout()
    }

    val rewardNeuron = network.addNeuron {
        location = point(stimulusNet.maxX + 100, stimulusNet.locationY)
        label = "Food Pellet"
        clamped = true
    }

    val punishNeuron = network.addNeuron {
        location = point(rewardNeuron.x + 100, rewardNeuron.locationY)
        label = "Shock"
        clamped = true
    }

    val syns = with(network) {
        connectAllToAll(stimulusNet, behaviorNet)
    }.onEach { it.strength = 0.0 }

    val odorWorldComponent = addOdorWorldComponent("Three Objects")
    val odorWorld = odorWorldComponent.world.apply {
        tileMap = TileMap(12, 12)
        isObjectsBlockMovement = false
        isUseCameraCentering = false
    }

    val mouse = odorWorld.addEntity(120, 245, EntityType.Mouse).apply {
        heading = 90.0
    }

    val candle = odorWorld.addEntity(27, 50, EntityType.Candle)
    val flower = odorWorld.addEntity(79, 50, EntityType.Pansy)
    val bell = odorWorld.addEntity(125, 50, EntityType.Bell)
    odorWorld.addEntity(candle)
    odorWorld.addEntity(flower)
    odorWorld.addEntity(bell)

    val cndleSensor = mouse.addObjectSensor(EntityType.Candle, 50.0, 0.0, 65.0)
    val flowerSensor = mouse.addObjectSensor(EntityType.Pansy, 50.0, 0.0, 65.0)
    val bellSensor = mouse.addObjectSensor(EntityType.Bell, 50.0, 0.0, 65.0)

    with(couplingManager) {
        val (n1, n2, n3) = stimulusNet.neuronList
        cndleSensor couple n1
        flowerSensor couple n2
        bellSensor couple n3
    }

    updateBehaviorNetNeuronLabels(behaviorNet)

    withGui {
        place(networkComponent, 155, 9, 575, 500)
        (getDesktopComponent(networkComponent) as NetworkDesktopComponent)
            .networkPanel.selectionManager.clear()

        place(odorWorldComponent, 730, 7, 315, 383)
        (getDesktopComponent(odorWorldComponent) as OdorWorldDesktopComponent).worldPanel.scalingFactor = .5
    }

    setupOperantWithEnvironmentWorkspace(workspace)

    addSidebarInfo(
        """
        
        # Operant with Environment
        
        This simulation demonstrates operant conditioning in an environment where an agent learns to associate stimuli with behaviors through reward and punishment. The agent has three
        basic behaviors (`Wiggle`, `Explore`, `Spin`) that it performs with certain probabilities. These behaviors can be reinforced or discouraged based on environmental feedback.
        
        # Simulation Details
        
        In this simulation, an agent (mouse) can sense three different objects: a `Candle`, `Flower`, and `Bell`.  
        The agent can perform three behaviors:
        
        - Wiggle: oscillates left and right  
        - Explore: moves around randomly  
        - Spin: rotates in place  
        
        The agent's behavior is controlled by a neural network with the following elements:
        
        - Stimulus neurons detect objects in the environment  
        - Behavior neurons control which action the agent performs  
        - Intrinsic probabilities (shown as labels on behavior neurons) determine baseline or spontaneous behavior tendencies  
        - Connections between stimuli and behaviors can be strengthened or weakened through learning. These connections determine behaviors that are conditional on the presence of 
        stimuli.  
        
        When the agent is near an object and performs a behavior, you can reward or punish that stimulus–response pairing. This increases or decreases the likelihood of that behavior
        occurring in that context.
        
        # What to Do
        
        1. Run the simulation and observe the agent's initial random behavior.  
        
        2. Train the agent to perform a behavior spontaneously. This is like the simple operant conditioning simulation.  
        
        3. Add a conditional behavior. Wait for the agent to approach an object (candle, flower, or bell). You will see the corresponding stimulus neuron activate.  
        
        4. Observe which behavior the agent performs near the object.  
        
        5. Provide feedback:  
           - Click `Reward` to encourage the stimulus–behavior pairing  
           - Click `Punish` to discourage it  
           - Click `Do nothing` to let the agent continue without feedback  
        
        6. Repeat the process. Over time, the agent will learn to associate certain stimuli with rewarded behaviors.  
        
        7. Monitor the changes:  
           - Watch how the connection weights between stimuli and behaviors change  
           - Notice how the agent's behavior becomes more predictable near certain objects  
           - Observe how intrinsic behavior probabilities (shown on the behavior neurons) adjust when no stimuli are present  
        
        At this point you can demonstrate the idea of a discriminative stimulus (also called a controlling stimulus). This is a stimulus that, after training, increases the probability
        of an operant behavior. It signals the relationship between a behavior and a reinforcer. The behavior can then be said to be under the control of that stimulus. For example, 
        pressing a lever may only produce food when a light is on.  
        
        In this simulation, you might first train the agent to wiggle spontaneously. Then you can transfer control of this behavior to the candle, so that wiggling only occurs when the
        candle is present. You could punish wiggling when the candle is absent, and train the agent to do something else spontaneously instead. In that case, wiggling is said to be under 
        the control of the candle, which is now the discriminative stimulus.
        
        # References
        
        Skinner, B. F. (1953). [_Science and human behavior_](https://www.bfskinner.org/newtestsite/wp-content/uploads/2014/02/ScienceHumanBehavior.pdf). Macmillan.

        # Credits
        
        Tim Meyer
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
                
        """.trimIndent()
    )

}.registerReopenFunction { workspace -> setupOperantWithEnvironmentWorkspace(workspace) }

fun updateBehaviorNetNeuronLabels(behaviorNet: NeuronGroup) {
    behaviorNet.neuronList.forEach {
        it.label = it.label?.replace(Regex(":.+"), ": ${it.auxValue.format(2)}")
    }
}

suspend fun SimulationScope.setupOperantWithEnvironmentWorkspace(workspace: Workspace) {

    val random = Random(Random.nextLong())

    val network = workspace.componentList.filterIsInstance<NetworkComponent>().first().network
    val behaviorNet = network.getModelByLabel<NeuronGroup>("Behaviors")
    val stimulusNet = network.getModelByLabel<NeuronGroup>("Stimuli")
    val rewardNeuron = network.getModelByLabel<Neuron>("Food Pellet")
    val punishNeuron = network.getModelByLabel<Neuron>("Shock")

    val odorWorldComponent = workspace.componentList.filterIsInstance<OdorWorldComponent>().first()
    val odorWorld = odorWorldComponent.world
    val mouse = odorWorld.entityList.first { it.entityType == EntityType.Mouse }

    network.updateManager.clear()
    network.updateManager.addAction(updateAction("Custom behaviorism update") {

        var winningNode = 0

        /**
         * Update actual firing probabilities, which combine intrinsic probabilities with weighted inputs
         */
        suspend fun updateNetwork() {
            val firingProbabilityCDF = behaviorNet.neuronList
                .map { it.weightedInputs + it.auxValue }
                .let { if (it.any { v -> v < 0 }) it.minMaxNormalize() else it }
                .normalize()
                .runningReduce(Double::plus)

            val selection = random.nextDouble()

            // Select "winning" neuron based on its probability
            winningNode = firingProbabilityCDF.indexOfFirst {
                selection < it
            }

            // Set neuron activation based on winning node
            behaviorNet.neuronList.forEachIndexed { index, neuron -> neuron.addInputValue(if (index == winningNode) 1.0 else 0.0) }

            network.bufferedUpdate()
        }

        /**
         * Update behavior of odor world agent based on which node is active.
         * Assumes behaviors partitioned into increments of (currently) 100 time steps
         */
        fun updateBehaviors() {
            val loopTime = workspace.time % 10

            when (winningNode) {
                0 -> {
                    mouse.heading += if (loopTime < 5) 5 else -5
                }
                1 -> {
                    if (random.nextDouble() < 0.2) {
                        mouse.heading += random.nextDouble(-10.0, 10.0)
                    }
                    mouse.speed = 2.5
                }
                2 -> {
                    mouse.heading += 20
                }
            }
        }

        updateBehaviorNetNeuronLabels(stimulusNet)
        updateNetwork()
        updateBehaviors()
    })

    withGui {
        createControlPanel("Control Panel", 5, 10) {

            fun normIntrinsicProbabilities() {
                val totalMass = behaviorNet.neuronList.sumOf { it.auxValue }
                behaviorNet.neuronList.forEach { it.auxValue /= totalMass }
            }

            fun learn(initValence: Double) {
                val rewardLearningRate = .1
                val punishLearningRate = .1

                val valence = initValence * if (initValence > 0) {
                    rewardLearningRate
                } else {
                    punishLearningRate
                }

                val totalActivation = stimulusNet.neuronList.sumOf { it.activation }
                val winner = getWinner(behaviorNet.neuronList, true)

                // If there are inputs, update weights
                if (totalActivation > .1) {
                    val src = getWinner(stimulusNet.neuronList, true)
                    val s_r = getSynapse(src, winner) ?: throw IllegalStateException("Synapse not found")
                    // Strengthen or weaken active S-R Pair
                    s_r.strength += valence
                } else {
                    // Else update intrinsic probability
                    val p = winner.auxValue
                    winner.auxValue = max(p + valence * p, 0.0)
                    normIntrinsicProbabilities()
                    updateBehaviorNetNeuronLabels(behaviorNet)
                }
            }

            addButton("Reward") {
                learn(1.0)
                rewardNeuron.activation = 1.0
                punishNeuron.activation = 0.0
                SimbrainDesktop.workspace.iterateSuspend()
            }

            addButton("Punish") {
                learn(-1.0)
                rewardNeuron.activation = 0.0
                punishNeuron.activation = 1.0
                SimbrainDesktop.workspace.iterateSuspend()
            }

            addButton("Do nothing") {
                rewardNeuron.activation = 0.0
                punishNeuron.activation = 0.0
                SimbrainDesktop.workspace.iterateSuspend()
            }

        }
    }

}
