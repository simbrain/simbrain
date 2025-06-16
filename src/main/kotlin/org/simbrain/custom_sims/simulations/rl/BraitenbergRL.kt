package org.simbrain.custom_sims.simulations.rl

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.graphicalUpperBound
import org.simbrain.util.place
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.fitWorldToFrameSize
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.workspace.updater.updateAction
import java.awt.geom.Point2D
import kotlin.math.max


/**
 * Using actor-critic to train a braitenberg pursuer
 */
val braitenbergRL = newSim {

    // TODOS
    // Update trial number, disallow "run" See the other RL sim
    // Add reward, value, and td error nodes as in the other sim
    // Add poison to reward. As you get closer, reward gets lower, and very low if you touch it
    // Start with a really dumb reward function. Just the above. We can add back in the lastdistance, etc. stuff later.
    // Use custom update to properly implement the actor (probably best with Jeff)
    // Implement a critic, so we have a value function

    var learningRate = 0.01
    var gamma = 0.9

    var numTrials = 10
    var maxStepsPerTrial = 500
    var trialStep = 0
    var goalAchieved = false
    val goalRadius = 30.0
    var stopRequested = false

    workspace.clearWorkspace()
    val oc = addOdorWorldComponent("RL Braitenberg World")
    val world = oc.world
    world.isObjectsBlockMovement = true
    oc.world.isUseCameraCentering = false

    oc.world.addEntity(398, 335, EntityType.Poison)
    oc.world.addEntity(500, 184, EntityType.Swiss)

    fun calculateReward(
        agent: OdorWorldEntity,
        world: OdorWorld,
        lastDistance: Double,
        lastDistanceToPoison: Double,
        lastPos: Point2D,
        stationarySteps: Int
    ): Triple<Double, Double, Int> {
        var reward = 0.0
        val cheese = world.entityList.find { it.entityType == EntityType.Swiss } ?: return Triple(0.0, 0.0, stationarySteps)
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

            if (distanceToPoison > lastDistanceToPoison) {
                poisonComponent += 2.0  // reward for escaping
            }
        }

        // if (distanceToCheese > lastDistance) {
        //     reward -= 10
        // }

        // if (agent.location.distance(lastPos) < 0.1) {
        //     reward -= 0.5  // penalize being stuck
        // }

        // if (agent.location.distance(lastPos) < 1.0) {
        //     val newStationarySteps = stationarySteps + 1
        //     if (newStationarySteps >= 15) {
        //         reward -= 10.0
        //         return Pair(reward, 0)
        //     }
        //     return Pair(reward, newStationarySteps)
        // } else {
        //     return Pair(reward, 0)
        // }
        return Triple(cheeseComponent, poisonComponent, 0)
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
    val leftInput = runBlocking {
        network.addNeuron(0, 100).apply {
            label = "Cheese (L)"
            clamped = true
        }
    }
    val rightInput = runBlocking {
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

    val leftSynapse = network.addSynapse(leftInput, leftTurn)
    val rightSynapse = network.addSynapse(rightInput, rightTurn)
    val straightSynapseLeft = network.addSynapse(leftInput, straight)
    val straightSynapseRight = network.addSynapse(rightInput, straight)
    val poisonToRightTurn = network.addSynapse(poisonLeftInput, rightTurn)
    val poisonToLeftTurn = network.addSynapse(poisonRightInput, leftTurn)
    val poisonToStraightLeft = network.addSynapse(poisonLeftInput, straight)
    val poisonToStraightRight = network.addSynapse(poisonRightInput, straight)
    val valueInputs = listOf(leftInput, rightInput, poisonLeftInput, poisonRightInput)
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
        cheeseLeftSensor couple leftInput
        cheeseRightSensor couple rightInput
        poisonLeftSensor couple poisonLeftInput
        poisonRightSensor couple poisonRightInput

        leftTurn couple eLeft
        rightTurn couple eRight
        straight couple eStraight
    }

    // Start off with synapse strength of 0
    network.freeSynapses.forEach {  s -> s.strength = 0.0 }
    
    // Start off with aversion to poison (otherwise it has no reason to turn away)
    // Move and Turn quickly away from
    poisonToLeftTurn.strength = 2.0
    poisonToRightTurn.strength = 2.0
    poisonToStraightLeft.strength = 1.0
    poisonToStraightRight.strength = 1.0
    
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
                this@addButton.isEnabled = false  // disable the button
                workspace.launch {
                    try {
                        for (trial in 1..numTrials) {
                            trialStep = 0
                            resetVehicle()
                            resetObjects()
                            var goalAchieved = false
                            var lastDistanceToCheese = Double.POSITIVE_INFINITY
                            var lastDistanceToPoison = Double.POSITIVE_INFINITY
                            var lastPosition = agent.location
                            var stationarySteps = 0

                            while (trialStep++ < maxStepsPerTrial && !goalAchieved && !stopRequested) {
                                workspace.iterateSuspend(1)

                                val cheese = world.entityList.find { it.entityType == EntityType.Swiss } ?: continue
                                val distanceToCheese = agent.location.distance(cheese.location)
                                if (distanceToCheese < 30.0) {
                                    goalAchieved = true
                                    break
                                }

                                val (cheeseR, poisonR, newStationarySteps) = calculateReward(
                                    agent,
                                    world,
                                    lastDistanceToCheese,
                                    lastDistanceToPoison,
                                    lastPosition,
                                    stationarySteps
                                )

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

                                // Update Actro weights
                                if (tdError > 0) {
                                    leftSynapse.strength += learningRate * tdError * leftInput.auxValue
                                    rightSynapse.strength += learningRate * tdError * rightInput.auxValue
                                    straightSynapseLeft.strength += learningRate * tdError * leftInput.auxValue
                                    straightSynapseRight.strength += learningRate * tdError * rightInput.auxValue
                                    poisonToRightTurn.strength += learningRate * tdError * poisonLeftInput.auxValue
                                    poisonToLeftTurn.strength += learningRate * tdError * poisonRightInput.auxValue
                                    poisonToStraightLeft.strength += learningRate * tdError * poisonLeftInput.auxValue
                                    poisonToStraightRight.strength += learningRate * tdError * poisonRightInput.auxValue
                                }

                                leftSynapse.strength = leftSynapse.strength.coerceIn(-10.0, 10.0)
                                rightSynapse.strength = rightSynapse.strength.coerceIn(-10.0, 10.0)
                                poisonToRightTurn.strength = poisonToRightTurn.strength.coerceIn(-10.0, 10.0)
                                poisonToLeftTurn.strength = poisonToLeftTurn.strength.coerceIn(-10.0, 10.0)
                                straightSynapseLeft.strength = straightSynapseLeft.strength.coerceIn(-3.0, 3.0)
                                straightSynapseRight.strength = straightSynapseRight.strength.coerceIn(-3.0, 3.0)

                                lastDistanceToCheese = distanceToCheese
                                lastPosition = agent.location
                                stationarySteps = newStationarySteps
                                valueInputs.forEach { it.auxValue = it.activation }
                                valueNeuron.auxValue = valueNeuron.activation
                                lastDistanceToPoison = agent.location.distance(
                                    world.entityList.find { it.entityType == EntityType.Poison }?.location ?: agent.location
                                )
                            }

                            if (stopRequested) break
                        }
                    } finally {
                        this@addButton.isEnabled = true  // re-enable the button
                        stopRequested = false           // reset stop flag
                    }
                }
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







