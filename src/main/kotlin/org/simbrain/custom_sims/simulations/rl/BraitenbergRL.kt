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
        lastPos: Point2D,
        stationarySteps: Int
    ): Pair<Double, Int> {
        var reward = 0.0
        val cheese = world.entityList.find { it.entityType == EntityType.Swiss } ?: return Pair(0.0, stationarySteps)
        val distanceToCheese = agent.location.distance(cheese.location)

        reward += when {
            distanceToCheese < 50 -> 10.0
            distanceToCheese < 100 -> 5.0
            distanceToCheese < 200 -> 1.0
            else -> 0.0
        }

        if (distanceToCheese > lastDistance) {
            reward -= 10
        }

        if (agent.location.distance(lastPos) < 0.1) {
            reward -= 0.5  // penalize being stuck
        }

        if (agent.location.distance(lastPos) < 1.0) {
            val newStationarySteps = stationarySteps + 1
            if (newStationarySteps >= 15) {
                reward -= 10.0
                return Pair(reward, 0)
            }
            return Pair(reward, newStationarySteps)
        } else {
            return Pair(reward, 0)
        }
    }

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val entityOffset = Point2D.Double(100.0, 100.0)

    val agent = oc.world.addEntity(entityOffset.x, entityOffset.y, EntityType.Circle).apply {
        addLeftRightSensors(EntityType.Swiss, 270.0)
        addDefaultEffectors()
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
    val leftSynapse = network.addSynapse(leftInput, leftTurn)
    val rightSynapse = network.addSynapse(rightInput, rightTurn)
    val straightSynapseLeft = network.addSynapse(leftInput, straight)
    val straightSynapseRight = network.addSynapse(rightInput, straight)

    // val neuronCollection = network.addNetworkModelAsync(
    //     NeuronCollection(network, listOf(leftInput, rightInput, straight, leftTurn, rightTurn))
    // )

    val (leftSensor, rightSensor) = agent.sensors
    val (eStraight, eLeft, eRight) = agent.effectors
    with(couplingManager) {
        leftSensor couple leftInput
        rightSensor couple rightInput
        leftTurn couple eLeft
        rightTurn couple eRight
        straight couple eStraight
    }

    // Start off with synapse strength of 0
    network.freeSynapses.forEach {  s -> s.strength = 0.0 }

    fun resetVehicle() {
        agent.location = Point2D.Double(60.0, 400.0)
        agent.heading = 45.0
    }

    withGui {
        place(networkComponent, 53, 282, 359, 327)
        place(oc, 462, 19, 600, 600)
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
            addButton("Run Trials") {
                workspace.launch {
                    for (trial in 1..numTrials) {
                        trialStep = 0
                        resetVehicle()
                        var goalAchieved = false
                        var lastDistanceToCheese = Double.POSITIVE_INFINITY
                        var lastPosition = agent.location
                        var stationarySteps = 0

                        while (trialStep++ < maxStepsPerTrial && !goalAchieved) {
                            workspace.iterateSuspend(1)

                            val cheese = world.entityList.find { it.entityType == EntityType.Swiss } ?: continue
                            val distanceToCheese = agent.location.distance(cheese.location)
                            if (distanceToCheese < 30.0) {
                                goalAchieved = true
                                break
                            }
                            //for (poison in world.entityList.filter { it.entityType == EntityType.Poison }) {
                            //    if (agent.location.distance(poison.location) < 30.0) {
                            //        goalAchieved = true
                            //        break
                            //    }
                            //}

                            val (reward, newStationarySteps) = calculateReward(
                                agent,
                                world,
                                lastDistanceToCheese,
                                lastPosition,
                                stationarySteps
                            )
                            val tdError = -reward
                            leftSynapse.strength += learningRate * tdError * leftInput.activation
                            rightSynapse.strength += learningRate * tdError * rightInput.activation
                            straightSynapseLeft.strength += learningRate * tdError * leftInput.activation
                            straightSynapseRight.strength += learningRate * tdError * rightInput.activation
                            leftSynapse.strength = leftSynapse.strength.coerceIn(-10.0, 10.0)
                            rightSynapse.strength = rightSynapse.strength.coerceIn(-10.0, 10.0)
                            straightSynapseLeft.strength = straightSynapseLeft.strength.coerceIn(-3.0, 3.0)
                            straightSynapseRight.strength =
                                straightSynapseRight.strength.coerceIn(-3.0, 3.0)

                            lastDistanceToCheese = distanceToCheese
                            lastPosition = agent.location
                            stationarySteps = newStationarySteps
                        }
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
    
    Dave Noelle suggested the origina idea
            
    """.trimIndent()
    )


}







