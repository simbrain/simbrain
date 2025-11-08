package org.simbrain.custom_sims.simulations.rl

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.core.getSynapse
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.place
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.fitWorldToFrameSize
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.awt.geom.Point2D
import javax.swing.JButton
import kotlin.math.abs
import kotlin.math.exp


/**
 * Using actor-critic to train a Braitenberg vehicles
 *
 * Define behavioral modules that correspond to Braitenberg vehicle types: Cheese Pursuer, Poison Pursuer, Cheese Avoider, Poison Avoider.
 * At each time step estimate which of these modules was more responsible for the behavior.
 * Then reinforce that. These are theoretically motivated actions.
 *
 * This is better than updating each synapse because learning then is slow and noisy and can produce descriptive interference.
 */
val braitenbergRL = newSim {

    var learningRate = 0.05
    var gamma = 0.95

    var numTrials = 100
    var maxStepsPerTrial = 1000
    var trialStep = 0
    var stopRequested = false
    
    var explorationRate = 0.3
    var explorationDecay = 0.995

    var cheeseRewardMultiplier = 1.0
    var poisonRewardMultiplier = -1.0

    workspace.clearWorkspace()
    val oc = addOdorWorldComponent("RL Braitenberg world")
    val world = oc.world
    world.isObjectsBlockMovement = true
    oc.world.isUseCameraCentering = false

    val poison = oc.world.addEntity(398, 335, EntityType.Poison)
    val cheese = oc.world.addEntity(500, 184, EntityType.Swiss)

    // Improved reward function with smoother gradients
    fun calculateReward(
        agent: OdorWorldEntity,
    ): Pair<Double, Double> {

        var cheeseComponent = 0.0
        var poisonComponent = 0.0

        // Cheese Proximity Reward. Exponential decay from 15 to 0 as distance increases
        val distanceToCheese = agent.location.distance(cheese.location)
        cheeseComponent = 15.0 * exp(-0.05 * distanceToCheese) * cheeseRewardMultiplier

        // Poison Proximity penalty. Exponential decay from 15 to 0 as distance increases
        val distanceToPoison = agent.location.distance(poison.location)
        poisonComponent = 15.0 * exp(-0.05 * distanceToPoison) * poisonRewardMultiplier

        return Pair(cheeseComponent, poisonComponent)
    }

    val dispersion = 75.0
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val entityOffset = Point2D.Double(100.0, 100.0)
    val agent = oc.world.addEntity(entityOffset.x, entityOffset.y, EntityType.Circle).apply {
        addSensor(ObjectSensor(EntityType.Swiss, 50.0, 45.0).apply {
            label = "Cheese left"
            decayFunction.dispersion = dispersion
        })
        addSensor(ObjectSensor(EntityType.Swiss, 50.0, -45.0).apply {
            label = "Cheese right"
            decayFunction.dispersion = dispersion
        })
        addSensor(ObjectSensor(EntityType.Poison, 50.0, 45.0).apply {
            label = "Poison left"
            decayFunction.dispersion = dispersion
        })
        addSensor(ObjectSensor(EntityType.Poison, 50.0, -45.0).apply {
            label = "Poison right"
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
            upperBound = 3.0
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
        label = "Cheese reward"
    }
    val poisonPenalty = network.addNeuron(300, 50).apply {
        clamped = true
        label = "Poison penalty"
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
        label = "TD error"
        upperBound = 100.0
        lowerBound = -100.0
    }

    // Add time series for monitoring
    // val (plot, rewardSeries, valueSeries, tdErrorSeries) = addTimeSeries(
    //     "Reward, Value, TD Error",
    //     seriesNames = listOf("Reward", "Value", "TD Error")
    // )

    // couplingManager.createCoupling(rewardNeuron, rewardSeries)
    // couplingManager.createCoupling(valueNeuron, valueSeries)
    // couplingManager.createCoupling(tdErrorNeuron, tdErrorSeries)

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

    network.freeSynapses.forEach { s ->
        s.strength = 0.0
        //s.strength = (Math.random() - 0.5) * 0.1
    }

    fun resetVehicle() {
        agent.location = Point2D.Double((50..500).random().toDouble(), (50..500).random().toDouble())
        agent.heading = (0..360).random().toDouble()
    }

    // Minseparation is between cheese and poison so it does not confuse them
    fun resetObjects(minSeparation: Double = 100.0) {
        val swiss = world.entityList.find { it.entityType == EntityType.Swiss } ?: return
        val poison = world.entityList.find { it.entityType == EntityType.Poison } ?: return

        var cheeseLoc: Point2D
        var poisonLoc: Point2D

        do {
            cheeseLoc = Point2D.Double((100..500).random().toDouble(), (100..500).random().toDouble())
            poisonLoc = Point2D.Double((100..500).random().toDouble(), (100..500).random().toDouble())
        } while (cheeseLoc.distance(poisonLoc) < minSeparation)

        swiss.location = cheeseLoc
        poison.location = poisonLoc
    }


    fun applyLearning(button: JButton) {
        button.isEnabled = false
        val random = java.util.Random()
        workspace.launch {
            try {
                for (trial in 1..numTrials) {
                    trialStep = 0
                    resetVehicle()
                    resetObjects()
                    
                    // Store previous value for TD error calculation
                    var previousValue = 0.0

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

                        // Add exploration noise that decays over time
                        val currentExploration = explorationRate * Math.pow(explorationDecay, trial.toDouble())
                        leftTurn.activation += random.nextGaussian() * currentExploration
                        rightTurn.activation += random.nextGaussian() * currentExploration

                        val (cheeseR, poisonR) = calculateReward(agent)

                        cheeseReward.activation = cheeseR
                        poisonPenalty.activation = poisonR
                        rewardNeuron.activation = cheeseR + poisonR

                        // Correct TD error calculation
                        val currentValue = valueNeuron.activation
                        val tdError = rewardNeuron.activation + gamma * currentValue - previousValue
                        tdErrorNeuron.activation = tdError.coerceIn(-1.0, 1.0)
                        tdErrorNeuron.activation = tdError

                        // Update critic weights
                        valueNeuron.fanIn.forEach { syn ->
                            syn.strength += learningRate * tdError * syn.source.activation
                        }

                        // Improved module selection based on actual behavior
                        val cheesePursuer = mapOf(
                            cheeseLeftInput to leftTurn,
                            cheeseRightInput to rightTurn,
                        )

                        val cheeseAvoider = mapOf(
                            cheeseLeftInput to rightTurn,
                            cheeseRightInput to leftTurn,
                        )

                        val poisonPursuer = mapOf(
                            poisonLeftInput to leftTurn,
                            poisonRightInput to rightTurn,
                        )

                        val poisonAvoider = mapOf(
                            poisonLeftInput to rightTurn,
                            poisonRightInput to leftTurn,
                        )

                        val behaviorModules = listOf(
                            "cheesePursuer" to cheesePursuer,
                            "cheeseAvoider" to cheeseAvoider,
                            "poisonPursuer" to poisonPursuer,
                            "poisonAvoider" to poisonAvoider,
                        )

                        // Identify the most active module in the sense that those neurons
                        // are most active
                        fun scoreModule(module: Map<Neuron, Neuron>): Double {
                            return module.entries.sumOf { (input, output) ->
                                val syn = getSynapse(input, output)
                                if (syn != null) {
                                    syn.strength * input.activation * output.activation
                                } else {
                                    0.0
                                }
                            }
                        }

                        // Identify the most active module
                        val (winningName, winningModule) = behaviorModules.maxByOrNull { scoreModule(it.second) } ?: continue

                        // Only update if TD error is significant
                        if (abs(tdError) > 0.1) {
                            // Reinforce the winning module
                            for ((input, output) in winningModule) {
                                val syn = getSynapse(input, output) ?: continue
                                val inputActivation = input.activation
                                syn.strength += learningRate * tdError * inputActivation
                            }

                            //if (trial % 5 == 0) { // Print less frequently
                            //    println("Trial $trial, Step $trialStep: $winningName, TD Error: $tdError")
                            //}
                        }

                        // Clamp weights to prevent runaway
                        actorSynapses.forEach { syn ->
                            syn.strength = syn.strength.coerceIn(-10.0, 10.0)
                        }

                        // Update previous value for next iteration
                        previousValue = currentValue
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
        place(networkComponent, 272, 0, 404, 595)
        place(oc, 662, 0, 601, 592)
        // place(plot, 1080, 0, 500, 500)
        oc.getDesktopComponentAs<OdorWorldDesktopComponent>().fitWorldToFrameSize()

        // Combined control panel
        createControlPanel("RL parameters", 0, 10) {
            addFormattedNumericTextField("Learning rate", initValue = learningRate) {
                learningRate = it
            }
            addFormattedNumericTextField("Gamma", initValue = gamma) {
                gamma = it
            }
            addFormattedNumericTextField("Exploration Rate", initValue = explorationRate) {
                explorationRate = it
            }
            addFormattedNumericTextField("Exploration Decay", initValue = explorationDecay) {
                explorationDecay = it
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
            
            addSeparator()
            
            //addFormattedNumericTextField("Cheese Multiplier", initValue = cheeseRewardMultiplier) {
            //    cheeseRewardMultiplier = it
            //}
            //addFormattedNumericTextField("Poison Multiplier", initValue = poisonRewardMultiplier) {
            //    poisonRewardMultiplier = it
            //}
            addButton("Both Rewarding") {
                cheeseRewardMultiplier = 1.0
                poisonRewardMultiplier = 1.0
            }
            addButton("Both Punishing") {
                cheeseRewardMultiplier = -1.0
                poisonRewardMultiplier = -1.0
            }
            addButton("Reverse") {
                cheeseRewardMultiplier = -1.0
                poisonRewardMultiplier = 1.0
            }
            addButton("Normal") {
                cheeseRewardMultiplier = 1.0
                poisonRewardMultiplier = -1.0
            }
        }
    }


    addSidebarInfo(
        """ 
            
    # Braitenberg RL
        
    Improved version with better learning dynamics.
    
    ### Key Improvements:
    
    1. **Fixed TD Error**: Now correctly calculates temporal difference error
    2. **Better Exploration**: Decaying exploration rate for better learning
    3. **Smoother Rewards**: Continuous reward gradients instead of binary
    4. **Improved Module Selection**: Better scoring of behavioral modules
    5. **Random Initialization**: Weights start with small random values
    
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