package org.simbrain.custom_sims.simulations.behaviorism

import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neurongroups.getWinner
import org.simbrain.util.*
import org.simbrain.workspace.updater.updateAction
import org.simbrain.world.odorworld.entities.EntityType
import kotlin.math.max
import kotlin.random.Random


val operantWithEnvironment = newSim {
    workspace.clearWorkspace()

    val random = Random(Random.nextLong())

    val networkComponent = addNetworkComponent("Brain")
    val network = networkComponent.network

    val numNeurons = 3

    val behaviorNet = network.addNeuronGroup(numNeurons, location = point(-9.25, 95.93)).apply {
        layout = LineLayout(100.0, LineLayout.LineOrientation.HORIZONTAL)
        applyLayout()
        label = "Behaviors"
        neuronList.labels = listOf("Wiggle: ", "Explore: ", "Spin: ")
        neuronList.forEach { it.auxValue = .33 }
    }

    val stimulusNet = network.addNeuronGroup(numNeurons, location = point(-9.25, 295.93)).apply {
        layout = LineLayout(100.0, LineLayout.LineOrientation.HORIZONTAL)
        applyLayout()
        setClamped(true)
        label = "Stimuli"
        setIncrement(1.0)
        neuronList.labels = listOf("Candle", "Flower", "Bell")
    }

    val rewardNeuron = network.addNeuron {
        location = point(stimulusNet.maxX + 100, stimulusNet.locationY)
        label = "Food Pellet"
    }

    val punishNeuron = network.addNeuron {
        location = point(rewardNeuron.x + 100, rewardNeuron.locationY)
        label = "Shock"
    }

    val syns = with(network) {
        connectAllToAll(stimulusNet, behaviorNet)
    }.onEach { it.strength = 0.0 }

    val odorWorldComponent = addOdorWorldComponent("Three Objects")
    val odorWorld = odorWorldComponent.world.apply {
        isObjectsBlockMovement = false
        isUseCameraCentering = false
    }

    val mouse = odorWorld.addEntity(120, 245, EntityType.MOUSE).apply {
        heading = 90.0
    }

    val cheese = odorWorld.addEntity(27, 50, EntityType.CANDLE)
    val flower = odorWorld.addEntity(79, 50, EntityType.PANSY)
    val fish = odorWorld.addEntity(125, 50, EntityType.FISH)

    val cheeseSensor = mouse.addObjectSensor(EntityType.SWISS, 50.0, 0.0, 65.0)
    val flowerSensor = mouse.addObjectSensor(EntityType.PANSY, 50.0, 0.0, 65.0)
    val fishSensor = mouse.addObjectSensor(EntityType.FISH, 50.0, 0.0, 65.0)

    fun updateBehaviorNetNeuronLabels() {
        behaviorNet.neuronList.forEach {
            it.label = it.label?.replace(Regex(":.+"), ": ${it.auxValue.format(2)}")
        }
    }

    with(couplingManager) {
        val (n1, n2, n3) = stimulusNet.neuronList
        cheeseSensor couple n1
        flowerSensor couple n2
        fishSensor couple n3
    }

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

        updateBehaviorNetNeuronLabels()
        updateNetwork()
        updateBehaviors()
    })

    updateBehaviorNetNeuronLabels()

    withGui {
        (getDesktopComponent(networkComponent) as NetworkDesktopComponent)
            .networkPanel.selectionManager.clear()

        place(networkComponent, 155, 9, 575, 500)
        place(odorWorldComponent, 730, 7, 315, 383)

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
                    updateBehaviorNetNeuronLabels()
                }
            }

            addButton("Reward") {
                learn(1.0)
                rewardNeuron.activation = 1.0
                punishNeuron.activation = 0.0
                workspace.iterateSuspend()
            }

            addButton("Punish") {
                learn(-1.0)
                rewardNeuron.activation = 0.0
                punishNeuron.activation = 1.0
                workspace.iterateSuspend()
            }

            addButton("Do nothing") {
                rewardNeuron.activation = 0.0
                punishNeuron.activation = 0.0
                workspace.iterateSuspend()
            }

        }
    }

}