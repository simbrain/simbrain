package org.simbrain.custom_sims.simulations.rl

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.place
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.fitWorldToFrameSize
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.awt.geom.Point2D
import javax.swing.JButton


/**
 * Using actor-critic to train a braitenberg pursuer
 */
val braitenbergRL = newSim {

    // TODO:
    //   Experiment with use of noise

    var learningRate = 0.01
    var gamma = 0.9

    var numTrials = 10
    var maxStepsPerTrial = 500
    var trialStep = 0
    var stopRequested = false

    workspace.clearWorkspace()
    val oc = addOdorWorldComponent("RL Braitenberg World")
    val world = oc.world
    world.isObjectsBlockMovement = true
    oc.world.isUseCameraCentering = false

    oc.world.addEntity(398, 335, EntityType.Poison)
    oc.world.addEntity(500, 184, EntityType.Swiss)

    // Returns a (reward, penalty) pair
    fun calculateReward(
        agent: OdorWorldEntity,
        lastDistanceToPoison: Double,
    ): Pair<Double, Double> {
        val cheese = world.entityList.find { it.entityType == EntityType.Swiss } ?: return Pair(0.0, 0.0)
        val poison = world.entityList.find { it.entityType == EntityType.Poison }

        var cheeseComponent = 0.0
        var poisonComponent = 0.0

        // Cheese Proximity Reward
        if (cheese != null) {
            val distanceToCheese = agent.location.distance(cheese.location)
            cheeseComponent += when {
                distanceToCheese < 50 -> 10.0
                distanceToCheese < 100 -> 5.0
                distanceToCheese < 200 -> 1.0
                else -> 0.0
            }
        }

        // Poison Proximity penalty
        if (poison != null) {
            val distanceToPoison = agent.location.distance(poison.location)
            poisonComponent += when {
                distanceToPoison < 30 -> -10.0
                distanceToPoison < 60 -> -5.0
                distanceToPoison < 100 -> -2.0
                else -> 0.0
            }

            // reward for escaping
            //if (distanceToPoison > lastDistanceToPoison) {
            //    poisonComponent += 2.0
            //}
        }

        return Pair(cheeseComponent, poisonComponent)
    }

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val entityOffset = Point2D.Double(100.0, 100.0)
    val dispersion = 150.0
    val agent = oc.world.addEntity(entityOffset.x, entityOffset.y, EntityType.Circle).apply {
        addSensor(ObjectSensor(EntityType.Swiss, 50.0, 45.0).apply {
            label = "Cheese Left"
            decayFunction.dispersion = dispersion
        })
        addSensor(ObjectSensor(EntityType.Swiss, 50.0, -45.0).apply {
            label = "Cheese Right"
            decayFunction.dispersion = dispersion
        })
        addSensor(ObjectSensor(EntityType.Poison, 50.0, 45.0).apply {
            label = "Poison Left"
            decayFunction.dispersion = dispersion
        })
        addSensor(ObjectSensor(EntityType.Poison, 50.0, -45.0).apply {
            label = "Poison Right"
            decayFunction.dispersion = dispersion
        })
        addDefaultEffectors()
        //isShowTrail = true
    }
    val cheeseLeftInput = runBlocking {
        network.addNeuron(0, 100).apply {
            label = "Cheese (L)"
            clamped = true
        }
    }
    val cheeseRightInput = runBlocking {
        network.addNeuron(100, 100).apply {
            label = "Cheese (R)"
            clamped = true
        }
    }
    val poisonLeftInput = runBlocking {
        network.addNeuron(0, 150).apply {
            label = "Poison (L)"
            clamped = true
        }
    }
    val poisonRightInput = runBlocking {
        network.addNeuron(100, 150).apply {
            label = "Poison (R)"
            clamped = true
        }
    }
    val straight = runBlocking {
        network.addNeuron(50, 0).apply {
            label = "Speed"
            activation = 0.0
            clamped = false
            bias = 0.5
            upperBound = 3.0  // speed limit
            //lowerBound = 0.0
        }
    }

    val leftTurn = runBlocking {
        network.addNeuron(0, 0).apply {
            label = "Left"
            lowerBound = -200.0
            upperBound = 200.0
        }
    }
    val rightTurn: Neuron = runBlocking {
        network.addNeuron(100, 0).apply {
            label = "Right"
            lowerBound = -200.0
            upperBound = 200.0
        }
    }
    val cheeseReward = network.addNeuron(300, 0).apply {
        clamped = true
        label = "Cheese Reward"
    }
    val poisonPenalty = network.addNeuron(300, 50).apply {
        clamped = true
        label = "Poison Penalty"
    }
    val rewardNeuron = network.addNeuron(300, 100).apply {
        clamped = true
        label = "Reward"
    }
    val valueNeuron = network.addNeuron(350, 100).apply {
        label = "Value"
        upperBound = 100.0
    }
    val tdErrorNeuron = network.addNeuron(400, 100).apply {
        label = "TD Error"
        upperBound = 100.0
        lowerBound = -100.0
    }

    val (plot, rewardSeries, valueSeries, tdErrorSeries) = addTimeSeries(
        "Reward, Value, TD Error",
        seriesNames = listOf("Reward", "Value", "TD Error")
    )

    couplingManager.createCoupling(rewardNeuron, rewardSeries)
    couplingManager.createCoupling(valueNeuron, valueSeries)
    couplingManager.createCoupling(tdErrorNeuron, tdErrorSeries)

    // All weights
    val cheeseLeftToLeftTurn = network.addSynapse(cheeseLeftInput, leftTurn)
    val cheeseLeftToRightTurn = network.addSynapse(cheeseLeftInput, rightTurn)
    val cheeseLeftToStraight = network.addSynapse(cheeseLeftInput, straight)
    val cheeseRightToLeftTurn = network.addSynapse(cheeseRightInput, leftTurn)
    val cheeseRightToRightTurn = network.addSynapse(cheeseRightInput, rightTurn)
    val cheeseRightToStraight = network.addSynapse(cheeseRightInput, straight)
    val poisonLeftToLeftTurn = network.addSynapse(poisonLeftInput, leftTurn)
    val poisonLeftToRightTurn = network.addSynapse(poisonLeftInput, rightTurn)
    val poisonLeftToStraight = network.addSynapse(poisonLeftInput, straight)
    val poisonRightToLeftTurn = network.addSynapse(poisonRightInput, leftTurn)
    val poisonRightToRightTurn = network.addSynapse(poisonRightInput, rightTurn)
    val poisonRightToStraightRight = network.addSynapse(poisonRightInput, straight)

    // List of all input neurons
    val valueInputs = listOf(cheeseLeftInput, cheeseRightInput, poisonLeftInput, poisonRightInput)
    val criticWeights = valueInputs.map { input ->
        network.addSynapse(input, valueNeuron).apply {
            strength = 0.0
        }
    }

    val cheeseLeftSensor = agent.getSensor("Cheese Left")
    val cheeseRightSensor = agent.getSensor("Cheese Right")
    val poisonLeftSensor = agent.getSensor("Poison Left")
    val poisonRightSensor = agent.getSensor("Poison Right")
    val (eStraight, eLeft, eRight) = agent.effectors

    with(couplingManager) {
        cheeseLeftSensor couple cheeseLeftInput
        cheeseRightSensor couple cheeseRightInput
        poisonLeftSensor couple poisonLeftInput
        poisonRightSensor couple poisonRightInput

        leftTurn couple eLeft
        rightTurn couple eRight
        straight couple eStraight
    }

    // Start off with synapse strength of 0
    network.freeSynapses.forEach { s -> s.strength = 0.0 }

    // Start off with aversion to poison (otherwise it has no reason to turn away)
    poisonRightToLeftTurn.strength = 2.0
    poisonLeftToRightTurn.strength = 2.0
    poisonLeftToStraight.strength = 1.0
    poisonRightToStraightRight.strength = 1.0
    cheeseLeftToLeftTurn.strength = 2.0
    cheeseRightToRightTurn.strength = 2.0

    fun resetVehicle() {
        agent.location = Point2D.Double((50..150).random().toDouble(), (350..450).random().toDouble())
        agent.heading = 45.0
    }

    fun resetObjects() {
        world.entityList.find { it.entityType == EntityType.Swiss }?.setLocation(
            (100..500).random().toDouble(),
            (100..500).random().toDouble()
        )

        world.entityList.find { it.entityType == EntityType.Poison }?.setLocation(
            (100..500).random().toDouble(),
            (100..500).random().toDouble()
        )
    }

    fun applyLearning(button: JButton) {
        button.isEnabled = false
        workspace.launch {
            try {
                for (trial in 1..numTrials) {
                    trialStep = 0
                    resetVehicle()
                    resetObjects()
                    var lastDistanceToPoison = Double.POSITIVE_INFINITY

                    val actorSynapses = listOf(
                            cheeseLeftToLeftTurn,
                            cheeseLeftToRightTurn,
                            cheeseLeftToStraight,
                            cheeseRightToLeftTurn,
                            cheeseRightToRightTurn,
                            cheeseRightToStraight,
                            poisonLeftToLeftTurn,
                            poisonLeftToRightTurn,
                            poisonLeftToStraight,
                            poisonRightToLeftTurn,
                            poisonRightToRightTurn,
                            poisonRightToStraightRight
                        )

                    while (trialStep++ < maxStepsPerTrial && !stopRequested) {
                        workspace.iterateSuspend(1)

                        // Add noise to turn neurons
                        val random = java.util.Random()
                        leftTurn.activation += random.nextGaussian() * 0.2
                        rightTurn.activation += random.nextGaussian() * 0.2

                        val cheese = world.entityList.find { it.entityType == EntityType.Swiss } ?: continue
                        val distanceToCheese = agent.location.distance(cheese.location)

                        val (cheeseR, poisonR) = calculateReward(agent, lastDistanceToPoison)

                        cheeseReward.activation = cheeseR
                        poisonPenalty.activation = poisonR
                        rewardNeuron.activation = cheeseR + poisonR

                        val tdError = rewardNeuron.activation + gamma * valueNeuron.activation - valueNeuron.auxValue
                        tdErrorNeuron.activation = tdError

                        // Update critic weights
                        valueNeuron.fanIn.forEach { syn ->
                            syn.strength += learningRate * tdError * syn.source.auxValue
                        }
                        valueNeuron.auxValue = valueNeuron.activation

                        // Directional Correlation-Based Learning
                        val turnPairs = listOf(
                            // Each input is paired with two turn synapses (preferred, opposing)
                            cheeseLeftInput to Pair(cheeseLeftToLeftTurn, cheeseLeftToRightTurn),
                            cheeseRightInput to Pair(cheeseRightToRightTurn, cheeseRightToLeftTurn),
                            poisonLeftInput to Pair(poisonLeftToLeftTurn, poisonLeftToRightTurn),
                            poisonRightInput to Pair(poisonRightToRightTurn, poisonRightToLeftTurn),
                        )

                        for ((input, pair) in turnPairs) {
                            val (synToPreferred, synToOpposing) = pair
                            val inputActivation = input.auxValue

                            // Reinforce preferred turn
                            synToPreferred.strength += learningRate * tdError * inputActivation * synToPreferred.target.activation

                            // Inhibit opposing turn slightly
                            synToOpposing.strength -= learningRate * tdError * inputActivation * synToOpposing.target.activation * 0.5
                        }

                        // Synaptic Competition: Normalize incoming weights per turn neuron
                        val turnFanIns = listOf(leftTurn, rightTurn).map { turnNeuron ->
                            turnNeuron to turnNeuron.fanIn.filter { it in actorSynapses }
                        }

                        turnFanIns.forEach { (turnNeuron, fanIns) ->
                            val totalStrength = fanIns.sumOf { kotlin.math.abs(it.strength) }
                            if (totalStrength > 1e-6) {  // avoid divide-by-zero
                                fanIns.forEach { syn ->
                                    syn.strength /= totalStrength
                                    // Optional: rescale to keep total input strength around 1.0 or 2.0
                                    syn.strength *= 2.0
                                }
                            }
                        }

                        // Clamp weights to prevent runaway
                        actorSynapses.forEach { syn ->
                            syn.strength = syn.strength.coerceIn(-5.0, 5.0)
                        }

                        // Update Aux Values 
                        valueInputs.forEach { it.auxValue = it.activation }
                        valueNeuron.auxValue = valueNeuron.activation
                        lastDistanceToPoison = agent.location.distance(
                            world.entityList.find { it.entityType == EntityType.Poison }?.location ?: agent.location
                        )
                    }

                    if (stopRequested) break
                }
            } finally {
                button.isEnabled = true
                stopRequested = false
            }
        }
    }

    withGui {
        place(networkComponent, 53, 282, 359, 327)
        place(oc, 462, 19, 600, 600)
        place(plot, 1080, 0, 500, 500)
        oc.getDesktopComponentAs<OdorWorldDesktopComponent>().fitWorldToFrameSize()

        // Add control panel for RL parameters
        createControlPanel("RL Parameters", 10, 10) {
            addFormattedNumericTextField("Learning Rate", initValue = learningRate) {
                learningRate = it
            }
            addFormattedNumericTextField("Trials", initValue = numTrials) {
                numTrials = it.toInt()
            }
            addFormattedNumericTextField("Max Steps", initValue = maxStepsPerTrial.toDouble()) {
                maxStepsPerTrial = it.toInt()
            }
            addButton("Stop") {
                stopRequested = true
            }
            addButton("Run Trials") {
                applyLearning(this@addButton)
            }
        }
    }


    addSidebarInfo(
        """ 
            
    # Braitenberg RL
        
    Work in progress.        

    ### References
        
    1. Braitenberg, V. (1986). [_Vehicles: Experiments in synthetic psychology_](https://mitpress.mit.edu/9780262521123/vehicles/). MIT press.
            
    2. Hotton, S., & Yoshimi, J. (2024). [_The Open Dynamics of Braitenberg Vehicles_](https://mitpress.mit.edu/9780262548199/the-open-dynamics-of-braitenberg-vehicles/). MIT Press.
            
    ### Credits
            
    Veer Sahai
    
    Jeff Yoshimi
    
    Dave Noelle suggested the original idea
            
    """.trimIndent()
    )


}







