package org.simbrain.custom_sims.simulations.rl

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.simbrain.custom_sims.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.place
import org.simbrain.util.swingInvokeLater
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.awt.geom.Point2D
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JButton
import kotlin.math.abs
import kotlin.math.exp


/**
 * Using actor-critic to train a Braitenberg vehicle.
 *
 * The vehicle learns to approach or avoid cheese and poison objects based on reward feedback.
 * All actor weights are updated uniformly using TD error at each time step.
 *
 * Run headless using:
 * gradle runSim -PsimName="Braitenberg RL" -PoptionString='{"numTrials": 100, "taskIndex": 0, "learningRate": 0.05}'
 *
 * Parameters:
 * - taskIndex: 0=Seek Cheese/Avoid Poison, 1=Seek Both, 2=Avoid Both, 3=Seek Poison/Avoid Cheese
 * - learningRate: Learning rate (default: 0.05)
 * - gamma: Discount factor (default: 0.95)
 * - explorationRate: Initial exploration rate (default: 0.3)
 * - explorationDecay: Exploration decay per trial (default: 0.995)
 * - dispersion: Sensor decay dispersion parameter (default: 75.0)
 * - numTrials: Number of training trials (default: 100)
 * - maxStepsPerTrial: Max steps per trial (default: 1000)
 * - printInterval: Print stats every N trials (default: 10)
 */
val braitenbergRL = newSim { optionString ->

    // Task configuration data class
    data class Task(
        val name: String,
        val cheeseReward: Double,
        val poisonReward: Double
    ) {
        override fun toString() = name
    }

    // Define available tasks
    val tasks = listOf(
        Task("Seek Cheese, Avoid Poison", 1.0, -1.0),
        Task("Seek Both Objects", 1.0, 1.0),
        Task("Avoid Both Objects", -1.0, -1.0),
        Task("Seek Poison, Avoid Cheese", -1.0, 1.0)
    )

    // Data class to track training metrics
    data class TrainingMetrics(
        val trialNumber: Int,
        val totalReward: Double,
        val steps: Int,
        val avgTDError: Double,
        val cheeseReward: Double,
        val poisonReward: Double
    )

    val trainingMetricsList = mutableListOf<TrainingMetrics>()

    var learningRate = 0.05
    var gamma = 0.95

    var numTrials = 10
    var maxStepsPerTrial = 1000
    var trialStep = 0
    var currentTrial = 0
    var stopRequested = false

    var printInterval = 10

    var trialLabel: javax.swing.JLabel? = null
    var learningEnabled = true
    var inBatchTraining = false  // Flag to prevent double-learning during batch training

    var cheeseRewardMultiplier = 1.0
    var poisonRewardMultiplier = -1.0

    var dispersion = 75.0

    workspace.clearWorkspace()
    val oc = addOdorWorldComponent("RL Braitenberg world")
    oc.world.tileMap.updateMapSize(16, 13)
    val world = oc.world
    world.isObjectsBlockMovement = true
    oc.world.isUseCameraCentering = false

    val poison = oc.world.addEntity(398, 335, EntityType.Poison)
    val cheese = oc.world.addEntity(500, 184, EntityType.Swiss)

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

    val (plot, rewardSeries, valueSeries, tdErrorSeries) = addTimeSeries(
        "Reward, Value, TD Error",
        seriesNames = listOf("Reward", "Value", "TD Error")
    )

    couplingManager.createCoupling(rewardNeuron, rewardSeries)
    couplingManager.createCoupling(valueNeuron, valueSeries)
    couplingManager.createCoupling(tdErrorNeuron, tdErrorSeries)

    // Actor synapses for learning
    val actorSynapses = mutableListOf<org.simbrain.network.core.Synapse>()

    // Actor synapses: only 4 trainable connections based on Braitenberg vehicle design
    val cheeseLeftToLeftTurn = network.addSynapse(cheeseLeftInput, leftTurn)
    val cheeseRightToRightTurn = network.addSynapse(cheeseRightInput, rightTurn)
    val poisonLeftToLeftTurn = network.addSynapse(poisonLeftInput, leftTurn)
    val poisonRightToRightTurn = network.addSynapse(poisonRightInput, rightTurn)

    // Add actor synapses to list for learning
    actorSynapses.addAll(listOf(
        cheeseLeftToLeftTurn,
        cheeseRightToRightTurn,
        poisonLeftToLeftTurn,
        poisonRightToRightTurn
    ))

    val valueInputs = listOf(cheeseLeftInput, cheeseRightInput, poisonLeftInput, poisonRightInput)
    val criticWeights = valueInputs.map { input ->
        network.addSynapse(input, valueNeuron).apply {
            strength = 0.0
        }
    }

    val cheeseLeftSensor = agent.getSensor("Cheese left")
    val cheeseRightSensor = agent.getSensor("Cheese right")
    val poisonLeftSensor = agent.getSensor("Poison left")
    val poisonRightSensor = agent.getSensor("Poison right")
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
    }

    // Track previous value for TD error calculation
    var previousValue = 0.0

    workspace.addUpdateAction("Update RL metrics and learning") {
        // Calculate current reward
        val (cheeseR, poisonR) = calculateReward(agent)
        rewardNeuron.activation = cheeseR + poisonR

        // Calculate current value (critic network output)
        // This is already computed by the network update, we just read it
        val currentValue = valueNeuron.activation

        // Calculate TD error: r + gamma * V(s') - V(s)
        val tdError = rewardNeuron.activation + gamma * currentValue - previousValue
        tdErrorNeuron.activation = tdError

        if (learningEnabled && !inBatchTraining) {
            // Update critic weights
            valueNeuron.fanIn.forEach { syn ->
                syn.strength += learningRate * tdError * syn.source.activation
            }

            // Update actor weights
            actorSynapses.forEach { syn ->
                syn.strength += learningRate * tdError * syn.source.activation
            }
        }

        // Update previous value for next iteration
        previousValue = currentValue
    }

    var respawnCountPerTrial = 0

    fun getObjectName(entity: OdorWorldEntity): String {
        return when (entity.entityType) {
            EntityType.Swiss -> "Cheese"
            EntityType.Poison -> "Poison"
            else -> entity.entityType.name
        }
    }

    fun resetVehicle() {
        agent.location = Point2D.Double((50..500).random().toDouble(), (50..500).random().toDouble())
        agent.heading = (0..360).random().toDouble()
    }

    // Reset all objects to random locations far from each other
    fun resetObjects(minSeparation: Double = 100.0) {
        val objectsToReset = listOf(cheese, poison)
        val newLocations = mutableListOf<Point2D>()

        for (obj in objectsToReset) {
            var newLoc: Point2D
            do {
                newLoc = Point2D.Double((100..500).random().toDouble(), (100..500).random().toDouble())
            } while (newLocations.any { it.distance(newLoc) < minSeparation })

            newLocations.add(newLoc)
            obj.location = newLoc
        }
    }

    // Respawn an object at a new location far from all other objects
    fun respawnObject(obj: OdorWorldEntity, minSeparation: Double = 100.0) {
        val objName = getObjectName(obj)
        val oldLoc = obj.location

        // Generate new location far from all other entities
        var newLoc: Point2D
        do {
            newLoc = Point2D.Double((100..500).random().toDouble(), (100..500).random().toDouble())
        } while (world.entityList.any { it !== obj && newLoc.distance(it.location) < minSeparation })

        obj.location = newLoc
        respawnCountPerTrial++

        // Log respawning event
        //println("[Respawn] $objName: Collision detected | Old=(${oldLoc.x.toInt()},${oldLoc.y.toInt()}) → New=(${newLoc.x.toInt()},${newLoc.y.toInt()}) | AgentDist=${"%.1f".format(newLoc.distance(agent.location))}px")
    }

    agent.events.collided.on { collidedWith ->
        if (collidedWith === cheese || collidedWith === poison) {
            //println("[Collision] Agent collided with ${getObjectName(collidedWith).uppercase()}")
            respawnObject(collidedWith)
        }
    }

    fun updateCriticWeights(tdError: Double) {
        valueNeuron.fanIn.forEach { syn ->
            syn.strength += learningRate * tdError * syn.source.activation
        }
    }

    fun exportMetricsToCSV() {
        try {
            // Create output directory if it doesn't exist
            val outputDir = File("simulation_outputs")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
            val filename = "simulation_outputs/braitenberg_rl_$timestamp.csv"
            val file = File(filename)

            file.bufferedWriter().use { writer ->
                // Write metadata header
                writer.write("# BraitenbergRL Training Results\n")
                writer.write("# Task: ${tasks.find { it.cheeseReward == cheeseRewardMultiplier && it.poisonReward == poisonRewardMultiplier }?.name ?: "Custom"}\n")
                writer.write("# Learning Rate: $learningRate, Gamma: $gamma\n")
                writer.write("# Trials: $numTrials, Max Steps: $maxStepsPerTrial\n")
                writer.write("#\n")

                // Write CSV header
                writer.write("trial,totalReward,steps,avgTDError,cheeseReward,poisonReward\n")

                // Write data
                trainingMetricsList.forEach { metrics ->
                    writer.write("${metrics.trialNumber},${metrics.totalReward},${metrics.steps},${metrics.avgTDError},${metrics.cheeseReward},${metrics.poisonReward}\n")
                }
            }

            println("\nTraining data exported to: $filename")
        } catch (e: Exception) {
            println("Error exporting CSV: ${e.message}")
        }
    }

    fun printTrialProgress(
        trial: Int,
        totalReward: Double,
        steps: Int,
        avgTDError: Double,
        respawnCount: Int
    ) {
        if (desktop != null) return  // Only print in headless mode

        val shouldPrint = trial % printInterval == 0 || trial == 1
        val significantImprovement = if (trial > 1 && trainingMetricsList.size >= 2) {
            val prevReward = trainingMetricsList[trial - 2].totalReward
            prevReward != 0.0 && (totalReward - prevReward) / abs(prevReward) > 0.1
        } else false

        if (shouldPrint || significantImprovement) {
            val improvement = if (trial > 1 && trainingMetricsList.size >= 2) {
                val prevReward = trainingMetricsList[trial - 2].totalReward
                if (prevReward != 0.0) {
                    val pctChange = ((totalReward - prevReward) / abs(prevReward) * 100)
                    if (pctChange > 0) " (+%.1f%%)".format(pctChange) else " (%.1f%%)".format(pctChange)
                } else ""
            } else ""
            println("Trial $trial: Reward=%.2f%s, Steps=%d, AvgTDError=%.3f, Respawns=%d".format(
                totalReward, improvement, steps, avgTDError, respawnCount
            ))
        }
    }

    fun printFinalSummary() {
        if (desktop != null || trainingMetricsList.isEmpty()) return  // Only print in headless mode

        println("\n" + "=".repeat(40))
        println("=== Training Complete ===")
        val avgReward = trainingMetricsList.map { it.totalReward }.average()
        val bestTrial = trainingMetricsList.maxByOrNull { it.totalReward }
        println("Average Reward: %.2f".format(avgReward))
        println("Best Trial: ${bestTrial?.trialNumber} (Reward: %.2f)".format(bestTrial?.totalReward ?: 0.0))

        // Check convergence (last 10 trials within 5%)
        if (trainingMetricsList.size >= 10) {
            val last10 = trainingMetricsList.takeLast(10).map { it.totalReward }
            val last10Avg = last10.average()
            val maxDeviation = last10.maxOfOrNull { abs(it - last10Avg) / (abs(last10Avg) + 0.001) } ?: 1.0
            val convergence = if (maxDeviation < 0.05) "Stable" else "Variable"
            println("Convergence: $convergence (last 10 trials deviation: %.1f%%)".format(maxDeviation * 100))
        }

        // Print the 4 learned synaptic weights
        println("\nLearned Synapse Weights:")
        println("  Cheese Left -> Left Turn: %.3f (Pursuer=Pos, Avoider=Neg)".format(cheeseLeftToLeftTurn.strength))
        println("  Cheese Right -> Right Turn: %.3f (Pursuer=Pos, Avoider=Neg)".format(cheeseRightToRightTurn.strength))
        println("  Poison Left -> Left Turn: %.3f (Pursuer=Pos, Avoider=Neg)".format(poisonLeftToLeftTurn.strength))
        println("  Poison Right -> Right Turn: %.3f (Pursuer=Pos, Avoider=Neg)".format(poisonRightToRightTurn.strength))
        println("=".repeat(40))

        // Export to CSV
        exportMetricsToCSV()
    }

    fun applyLearning(button: JButton?) {
        button?.isEnabled = false
        // Enable learning for training mode
        learningEnabled = true
        inBatchTraining = true
        workspace.launch {
            try {
                // Print header for headless mode
                if (desktop == null) {
                    println("\n=== BraitenbergRL Training Started ===")
                    println("Task: ${tasks.find { it.cheeseReward == cheeseRewardMultiplier && it.poisonReward == poisonRewardMultiplier }?.name ?: "Custom"}")
                    println("Learning Rate: $learningRate, Gamma: $gamma")
                    println("Trials: $numTrials, Max Steps: $maxStepsPerTrial")
                    println("=".repeat(40))
                }

                val startTrial = currentTrial + 1
                val endTrial = numTrials
                for (trial in startTrial..endTrial) {
                    currentTrial = trial
                    desktop?.let { swingInvokeLater { trialLabel?.text = "Trial: $currentTrial" } }
                    trialStep = 0
                    resetVehicle()
                    resetObjects()
                    respawnCountPerTrial = 0

                    // Store previous value for TD error calculation
                    var previousValue = 0.0
                    var totalReward = 0.0
                    var totalTDError = 0.0
                    var tdErrorCount = 0
                    var totalCheeseReward = 0.0
                    var totalPoisonReward = 0.0

                    val actorSynapses = listOf(
                        cheeseLeftToLeftTurn,
                        cheeseRightToRightTurn,
                        poisonLeftToLeftTurn,
                        poisonRightToRightTurn
                    )

                    while (trialStep++ < maxStepsPerTrial && !stopRequested) {
                        workspace.iterateSuspend(1)

                        val (cheeseR, poisonR) = calculateReward(agent)

                        rewardNeuron.activation = cheeseR + poisonR

                        // Track rewards
                        totalReward += cheeseR + poisonR
                        totalCheeseReward += cheeseR
                        totalPoisonReward += poisonR

                        // Correct TD error calculation
                        val currentValue = valueNeuron.activation
                        val tdError = rewardNeuron.activation + gamma * currentValue - previousValue
                        tdErrorNeuron.activation = tdError

                        // Track TD error
                        totalTDError += abs(tdError)
                        tdErrorCount++

                        // Update critic weights
                        updateCriticWeights(tdError)

                        // Update all actor weights
                        actorSynapses.forEach { syn ->
                            syn.strength += learningRate * tdError * syn.source.activation
                        }

                        // Update previous value for next iteration
                        previousValue = currentValue
                    }

                    // Record metrics for this trial
                    val avgTDError = if (tdErrorCount > 0) totalTDError / tdErrorCount else 0.0
                    val metrics = TrainingMetrics(
                        trialNumber = trial,
                        totalReward = totalReward,
                        steps = trialStep - 1,
                        avgTDError = avgTDError,
                        cheeseReward = totalCheeseReward,
                        poisonReward = totalPoisonReward
                    )
                    trainingMetricsList.add(metrics)
                    printTrialProgress(trial, totalReward, trialStep - 1, avgTDError, respawnCountPerTrial)

                    if (stopRequested) break
                }

                printFinalSummary()
            } finally {
                inBatchTraining = false
                button?.isEnabled = true
                stopRequested = false
            }
        }
    }

    withGui {
        place(networkComponent, 320, 10, 360, 400)
        place(oc, 670, 10, 415, 415)
        place(plot, 320, 410, 500, 300)
        swingInvokeLater { oc.getDesktopComponentAs<OdorWorldDesktopComponent>().worldPanel.scalingFactor = 0.1 }

        // Combined control panel
        createControlPanel("Control Panel", 0, 10) {

            addLabel("Task:")

            val taskComboBox = addComboBox("", tasks, tasks[0]) { selectedTask ->
                cheeseRewardMultiplier = selectedTask.cheeseReward
                poisonRewardMultiplier = selectedTask.poisonReward
                learningRate = 0.05
                gamma = 0.95
                numTrials = 10
                maxStepsPerTrial = 1000
            }
            taskComboBox.toolTipText = "Select the learning task: which objects to seek or avoid"

            addSeparator()

            trialLabel = addLabel("Trial: 0")
            trialLabel?.toolTipText = "Current trial number (continues across training runs)"

            addSeparator()

            val learningCheckbox = addCheckBox("Learning Enabled", learningEnabled) { enabled ->
                learningEnabled = enabled
                // Clamp/unclamp trainable weights
                val allTrainableWeights = actorSynapses + valueNeuron.fanIn
                allTrainableWeights.forEach { syn ->
                    syn.clamped = !enabled
                }
            }
            learningCheckbox.toolTipText = "Enable/disable weight updates (unchecking freezes all weights)"

            addSeparator()

            val runButton = addButton("Run Training") {
                applyLearning(this@addButton)
            }
            runButton.toolTipText = "Start or resume training for the specified number of trials"

            val stopButton = addButton("Stop Training") {
                stopRequested = true
            }
            stopButton.toolTipText = "Pause training (can be resumed later)"

            val resetButton = addButton("Reset") {
                network.freeSynapses.forEach { s ->
                    s.strength = 0.0
                }
                currentTrial = 0
                trainingMetricsList.clear()
                swingInvokeLater { trialLabel?.text = "Trial: 0" }
            }
            resetButton.toolTipText = "Reset all weights to zero and restart trial counter"

            addSeparator()

            val lrField = addFormattedNumericTextField("Learning Rate", initValue = learningRate) {
                learningRate = it
            }
            lrField.toolTipText = "Step size for weight updates (higher = faster but less stable)"

            val gammaField = addFormattedNumericTextField("Gamma (Discount Factor)", initValue = gamma) {
                gamma = it
            }
            gammaField.toolTipText = "Importance of future rewards (0-1, higher = more farsighted)"

            val dispersionField = addFormattedNumericTextField("Dispersion", initValue = dispersion) {
                dispersion = it
            }
            dispersionField.toolTipText = "Sensor range parameter (higher = wider detection range)"

            val trialsField = addFormattedNumericTextField("Number of Trials", initValue = numTrials) {
                numTrials = it
            }
            trialsField.toolTipText = "How many trials to run when 'Run Training' is clicked"

            val stepsField = addFormattedNumericTextField("Max Steps per Trial", initValue = maxStepsPerTrial.toDouble()) {
                maxStepsPerTrial = it.toInt()
            }
            stepsField.toolTipText = "Maximum time steps per trial before reset"

            swingInvokeLater { pack() }
        }
    }


    addSidebarInfo(
        """ 
    # Braitenberg RL
        
    A Braitenberg vehicle that learns different approach and avoidance behaviors using actor-critic reinforcement learning. The vehicle uses sensory inputs to detect cheese and poison objects, and learns to approach or avoid them based on reward feedback.
    
    ## Background

    Braitenberg vehicles are simple agent models that exhibit complex behaviors through sensory-motor connections. In this simulation, instead of hand-coding the connection weights, the vehicle learns them through reinforcement learning. The actor-critic architecture combines policy learning (actor) with value estimation (critic) to efficiently learn behaviors.

    # Simulation Details
    
    ## Network Architecture
    
    The network consists of:
    - **Input Neurons**: Four sensors detecting cheese and poison objects (left and right for each)
    - **Output Neurons**: Three motor outputs controlling left turn, right turn, and forward speed
    - **Actor Weights**: Connect inputs to motor outputs, determining behavior
    - **Critic Network**: Learns to predict value of current state
    - **Reward Neuron**: Displays the total reward signal
    
    ## Learning Algorithm
    
    The simulation uses temporal difference (TD) learning:
    
    1. The agent observes its environment through sensors
    2. The actor selects actions based on current weights plus exploration noise
    3. The agent receives rewards based on proximity to objects
    4. The critic computes the TD error (difference between predicted and actual value)
    5. Both actor and critic weights are updated based on TD error
    
    Rewards are distance-based with exponential decay, providing smooth gradients that guide learning.

    ## Actor Weights

    The vehicle has four trainable actor connections:
    - Cheese Left sensor → Left Turn
    - Cheese Right sensor → Right Turn
    - Poison Left sensor → Left Turn
    - Poison Right sensor → Right Turn

    All weights are updated uniformly at each time step based on the TD error. Positive weights create pursuit behavior (turn toward the object), while negative weights create avoidance behavior (turn away from the object).

    ## Dynamic Object Respawning

    Objects automatically respawn at new locations when the agent collides with them. This prevents the vehicle from getting stuck on objects and encourages continuous exploration. Objects always respect a minimum separation distance of 100 pixels from each other to avoid confusion during learning. 

    # What to Do


    1. Select a task from the `Task` dropdown menu in the control panel
    2. Choose your learning mode:
       - **Continuous Learning**: Check `Learning Enabled` and use the workspace run button to watch continuous learning in real-time
       - **Batch Training**: Click `Run Training` to execute a specific number of trials and collect metrics
    3. Watch the vehicle move around the environment as it learns
    4. Observe the `Reward, Value, TD Error` plot to monitor learning progress
    5. The `Trial` counter shows current progress and persists across training runs

    **Learning Modes:**
    - `Learning Enabled` checkbox: When checked, learning happens continuously whenever the workspace runs. Uncheck to freeze weights and observe behavior without learning.
    - `Run Training` button: Executes batch training for a specified number of trials, collecting statistics. Good for systematic experiments.
    - `Stop Training`: Pause batch training (resume by clicking `Run Training` again)
    - `Reset`: Clear all weights to zero and restart trial counter

    Performance tip: Training runs much faster if you minimize or iconify component windows (time series, network, odor world). The simulation still runs in the background but doesn't spend time rendering visualizations.
    Another option is to leave the odor world open since it does not impact performance much and it’s fun to watch it learn.
    
    Try each task to see how the vehicle learns different behaviors:

    - Seek Cheese, Avoid Poison: Standard Braitenberg behavior where cheese is rewarding and poison is penalizing
    - Seek Both Objects: Both objects provide positive reward
    - Avoid Both Objects: Both objects provide negative reward
    - Seek Poison, Avoid Cheese: Opposite behavior where poison is rewarding and cheese is penalizing

    During training, watch:
    - How the vehicle's movement patterns change over trials
    - The `Reward` trace showing how total reward changes
    - The `Value` trace showing the critic's learned predictions
    - The `TD Error` showing the learning signal
    - The network weights updating in real-time
    
    ## Experiment

    Try different learning approaches:
    - **Interactive Learning**: Use the workspace run button with `Learning Enabled` checked. Stop and examine weights at any time, then continue.
    - **Run Trials**: Use `Run Training` for systematic experiments with multiple trials and automatic metric collection.
    - **Observing Learned Behavior**: Uncheck `Learning Enabled` to freeze weights and watch the vehicle execute its learned policy without further updates.

    Adjust parameters to see their effects:

    - **Learning Rate**: Controls how quickly weights change (higher = faster but less stable learning)
    - **Gamma**: Discount factor for future rewards (higher = more farsighted)
    - **Dispersion**: Sensor range (higher = wider detection)
    - **Number of Trials**: How many trials to run in batch training mode
    - **Max Steps per Trial**: Maximum time steps per episode

    After training on one task, try clicking `Reset` and training on a different task to see how the vehicle adapts.

    ## Headless Mode

    This simulation can be run in headless mode for batch experiments:
    
    ```
    gradle runSim -PsimName="BraitenbergRL" -PoptionString='{"numTrials": 100, "taskIndex": 0, "learningRate": 0.05}'
    ```
    
    Parameters:
    - `taskIndex`: 0=Seek Cheese/Avoid Poison, 1=Seek Both, 2=Avoid Both, 3=Seek Poison/Avoid Cheese
    - `learningRate`: Learning rate (default: 0.05)
    - `gamma`: Discount factor (default: 0.95)
    - `explorationRate`: Initial exploration rate (default: 0.3)
    - `explorationDecay`: Exploration decay per trial (default: 0.995)
    - `dispersion`: Sensor decay dispersion parameter (default: 75.0)
    - `numTrials`: Number of training trials (default: 100)
    - `maxStepsPerTrial`: Max steps per trial (default: 1000)
    - `printInterval`: Print stats every N trials (default: 10)
    
    Results are printed to stdout and saved to `simulation_outputs/braitenberg_rl_TIMESTAMP.csv`
    
    # References

    Braitenberg, V. (1986). [_Vehicles: Experiments in synthetic psychology_](https://mitpress.mit.edu/9780262521123/vehicles/). MIT Press.

    Hotton, S., & Yoshimi, J. (2024). [_The Open Dynamics of Braitenberg Vehicles_](https://mitpress.mit.edu/9780262548199/the-open-dynamics-of-braitenberg-vehicles/). MIT Press.

    Sutton, R. S., & Barto, A. G. (2018). [_Reinforcement Learning: An Introduction_](http://incompleteideas.net/book/the-book.html) (2nd ed.). MIT Press.

    # Credits

    Dave Noelle
    
    Yulin Li

    Veer Sahai

    Jeff Yoshimi
            
    """.trimIndent()
    )

    // Parse optionString and run in headless mode if provided
    if (optionString?.isNotEmpty() == true) {
        val options = JSONObject(optionString)

        // Parse parameters
        learningRate = options.optDouble("learningRate", learningRate)
        gamma = options.optDouble("gamma", gamma)
        dispersion = options.optDouble("dispersion", dispersion)
        numTrials = options.optInt("numTrials", numTrials)
        maxStepsPerTrial = options.optInt("maxStepsPerTrial", maxStepsPerTrial)
        printInterval = options.optInt("printInterval", printInterval)
        
        // Set task based on taskIndex
        if (options.has("taskIndex")) {
            val taskIndex = options.getInt("taskIndex")
            if (taskIndex in tasks.indices) {
                val task = tasks[taskIndex]
                cheeseRewardMultiplier = task.cheeseReward
                poisonRewardMultiplier = task.poisonReward
            }
        }
        
        // Run training automatically in headless mode
        applyLearning(null)
    }

}