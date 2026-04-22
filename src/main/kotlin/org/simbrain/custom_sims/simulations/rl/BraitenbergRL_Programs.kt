package org.simbrain.custom_sims.simulations.rl

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.swingInvokeLater
import org.simbrain.util.toDoubleArray
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.awt.geom.Point2D

/**
 * Analysis of RL-trained Braitenberg vehicles (for comparison with [braitenbergRL])
 * where they are trained using "programs", that is, single nodes which, when active, produce
 * the weights appropriate to a task
 *
 * Programs set the turn weights directly, while a learned sensor-to-program preference matrix
 * determines which program is selected by softmax on each step.
 */
val braitenbergRLPrograms = newSim { optionString ->

    val tasks = rlTasks

    var learningRate = 0.05
    var actorLearningRate = 0.5
    var gamma = 0.95
    var programWeightStrength = 20.0
    var cheeseRewardMultiplier = 1.0
    var poisonRewardMultiplier = -1.0
    var learningEnabled = true
    var sparseReward = false
    var temperature = 0.5

    // Sparse reward flags set by collision events
    var justHitCheese = false
    var justHitPoison = false

    workspace.clearWorkspace()
    val oc = addOdorWorldComponent("TD Experiments World")
    oc.world.tileMap.updateMapSize(20, 20)
    val world = oc.world
    world.isObjectsBlockMovement = true

    val poison = oc.world.addEntity(398, 335, EntityType.Poison)
    val cheese = oc.world.addEntity(500, 184, EntityType.Swiss)

    val sharedDecayFunction = GaussianDecayFunction(75.0)
    val cheeseRewardConfig = RewardConfig("Cheese Reward", sharedDecayFunction.copy() as DecayFunction).apply { maxReward = 15.0 }
    val poisonRewardConfig = RewardConfig("Poison Reward", sharedDecayFunction.copy() as DecayFunction).apply { maxReward = 15.0 }

    fun calculateReward(agent: OdorWorldEntity): Double {
        if (sparseReward) {
            var reward = 0.0
            if (justHitCheese) {
                reward += cheeseRewardMultiplier
                justHitCheese = false
            }
            if (justHitPoison) {
                reward += poisonRewardMultiplier
                justHitPoison = false
            }
            return reward
        }
        val distanceToCheese = agent.location.distance(cheese.location)
        val distanceToPoison = agent.location.distance(poison.location)
        return cheeseRewardConfig.calculateReward(distanceToCheese, cheeseRewardMultiplier) +
                poisonRewardConfig.calculateReward(distanceToPoison, poisonRewardMultiplier)
    }

    fun sample(probabilities: DoubleArray): Int {
        val r = kotlin.random.Random.nextDouble()
        var cumulative = 0.0
        for (i in probabilities.indices) {
            cumulative += probabilities[i]
            if (r < cumulative) return i
        }
        return probabilities.size - 1
    }

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val activeTextLabel = NetworkTextObject("Active: None").apply { fontSize = 16 }

    val entityOffset = Point2D.Double(100.0, 100.0)
    val agent = oc.world.addEntity(entityOffset.x, entityOffset.y, EntityType.Circle).apply {
        addSensor(ObjectSensor(EntityType.Swiss, 50.0, 45.0).apply {
            label = "Cheese left"
            decayFunction = sharedDecayFunction.copy() as GaussianDecayFunction
        })
        addSensor(ObjectSensor(EntityType.Swiss, 50.0, -45.0).apply {
            label = "Cheese right"
            decayFunction = sharedDecayFunction.copy() as GaussianDecayFunction
        })
        addSensor(ObjectSensor(EntityType.Poison, 50.0, 45.0).apply {
            label = "Poison left"
            decayFunction = sharedDecayFunction.copy() as GaussianDecayFunction
        })
        addSensor(ObjectSensor(EntityType.Poison, 50.0, -45.0).apply {
            label = "Poison right"
            decayFunction = sharedDecayFunction.copy() as GaussianDecayFunction
        })
        addDefaultEffectors()
    }

    // Sensor input neurons wrapped in a NeuronCollection
    val cheeseLeftInput = runBlocking {
        network.addNeuron(-71, 193).apply { label = "Cheese (L)"; clamped = true }
    }
    val cheeseRightInput = runBlocking {
        network.addNeuron(4, 193).apply { label = "Cheese (R)"; clamped = true }
    }
    val poisonLeftInput = runBlocking {
        network.addNeuron(75, 193).apply { label = "Poison (L)"; clamped = true }
    }
    val poisonRightInput = runBlocking {
        network.addNeuron(151, 193).apply { label = "Poison (R)"; clamped = true }
    }

    val sensorNeurons = listOf(cheeseLeftInput, cheeseRightInput, poisonLeftInput, poisonRightInput)
    val sensorCollection = NeuronCollection(sensorNeurons).apply { label = "Sensors" }
    network.addNetworkModelAsync(sensorCollection)

    // Motor neurons
    val straight = runBlocking {
        network.addNeuron(39, 5).apply {
            label = "Speed"; activation = 0.0; clamped = false; bias = 2.0; upperBound = 10.0
        }
    }
    val leftTurn = runBlocking {
        network.addNeuron(-10, 5).apply { label = "Left"; lowerBound = -200.0; upperBound = 200.0 }
    }
    val rightTurn = runBlocking {
        network.addNeuron(89, 5).apply { label = "Right"; lowerBound = -200.0; upperBound = 200.0 }
    }

    // Actor synapses (set by the active program)
    val cheeseLeftToLeftTurn = network.addSynapseAsync(cheeseLeftInput, leftTurn)
    val cheeseRightToRightTurn = network.addSynapseAsync(cheeseRightInput, rightTurn)
    val poisonLeftToLeftTurn = network.addSynapseAsync(poisonLeftInput, leftTurn)
    val poisonRightToRightTurn = network.addSynapseAsync(poisonRightInput, rightTurn)

    // Program NeuronArray with SoftmaxRule (4 programs, circle mode, vertical)
    val programNames = listOf(
        "Seek Cheese, Avoid Poison",
        "Seek Both Objects",
        "Avoid Both Objects",
        "Seek Poison, Avoid Cheese"
    )
    val numPrograms = programNames.size

    val softmaxRule = SoftmaxRule().apply { this.temperature = temperature }

    val programArray = NeuronArray(numPrograms).apply {
        label = "Programs"
        updateRule = softmaxRule
        circleMode = true
        verticalLayout = true
        labelArray = programNames.toTypedArray()
    }
    network.addNetworkModelAsync(programArray)
    programArray.setLocation(452.0, 135.0)

    // Weight matrix: sensors → programs (the learned program preferences)
    val actorWeightMatrix = WeightMatrix(sensorCollection, programArray).apply {
        hardClear() // start with zero matrix
    }
    network.addNetworkModelAsync(actorWeightMatrix)

    // Critic and TD error neurons
    val rewardNeuron = network.addNeuron(205, 3).apply { clamped = true; label = "Reward" }
    val valueNeuron = network.addNeuron(255, 3).apply { label = "Value"; upperBound = 100.0; lowerBound = -100.0 }
    val tdErrorNeuron = network.addNeuron(305, 3).apply { label = "TD Error"; upperBound = 100.0; lowerBound = -100.0 }

    // Critic weights: sensors → value
    val criticWeights = sensorNeurons.map { sensorNeuron ->
        network.addSynapseAsync(sensorNeuron, valueNeuron).apply { strength = 0.0 }
    }

    network.addNetworkModels(activeTextLabel)

    // Couplings
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

    network.freeSynapses.forEach { s -> s.strength = 0.0 }

    var previousValue = 0.0
    var previousWinningProgram = -1
    var previousSensorValues = DoubleArray(sensorNeurons.size) { 0.0 }
    var stuckCounter = 0

    fun applyProgram(programIndex: Int) {
        when (programIndex) {
            0 -> {
                cheeseLeftToLeftTurn.strength = programWeightStrength
                cheeseRightToRightTurn.strength = programWeightStrength
                poisonLeftToLeftTurn.strength = -programWeightStrength
                poisonRightToRightTurn.strength = -programWeightStrength
            }
            1 -> {
                cheeseLeftToLeftTurn.strength = programWeightStrength
                cheeseRightToRightTurn.strength = programWeightStrength
                poisonLeftToLeftTurn.strength = programWeightStrength
                poisonRightToRightTurn.strength = programWeightStrength
            }
            2 -> {
                cheeseLeftToLeftTurn.strength = -programWeightStrength
                cheeseRightToRightTurn.strength = -programWeightStrength
                poisonLeftToLeftTurn.strength = -programWeightStrength
                poisonRightToRightTurn.strength = -programWeightStrength
            }
            3 -> {
                cheeseLeftToLeftTurn.strength = -programWeightStrength
                cheeseRightToRightTurn.strength = -programWeightStrength
                poisonLeftToLeftTurn.strength = programWeightStrength
                poisonRightToRightTurn.strength = programWeightStrength
            }
        }
    }

    workspace.addUpdateAction("TD Experiments") {

        val sensorValues = sensorNeurons.map { it.activation }.toDoubleArray()

        // Respawn agent if it has been stuck with near-zero sensor input for too long
        if (sensorValues.sum() < 0.01) {
            stuckCounter++
            if (stuckCounter > 200) {
                agent.location = Point2D.Double((100..500).random().toDouble(), (100..500).random().toDouble())
                agent.heading = (0..360).random().toDouble()
                stuckCounter = 0
                previousValue = 0.0
                previousWinningProgram = -1
            }
        } else {
            stuckCounter = 0
        }

        // Sync softmax temperature from the control panel
        softmaxRule.temperature = temperature

        // SoftmaxRule computes program probabilities from the learned sensor-to-program preferences.
        var probabilities = programArray.activations.toDoubleArray()
        // First iteration before network update, use uniform
        if (probabilities.sum() < 1e-6) {
            probabilities = DoubleArray(numPrograms) { 1.0 / numPrograms }
        }

        val winningProgram = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0

        // Sample the program to run next step
        val activeProgram = sample(probabilities)
        activeTextLabel.text = "Active: ${programNames[activeProgram]}"

        applyProgram(activeProgram)

        // Reward reflects the outcome of the previous program's action
        rewardNeuron.activation = calculateReward(agent)

        // Critic: V(s') is the current value; V(s) was stored as previousValue
        val currentValue = valueNeuron.activation
        val tdError = rewardNeuron.activation + gamma * currentValue - previousValue
        tdErrorNeuron.activation = tdError

        // Only update if a previous winning program was active (skip first step)
        if (learningEnabled && previousWinningProgram >= 0) {
            // Update critic weights using previous sensor state: w_c += α * δ * s(t-1)
            criticWeights.forEachIndexed { j, syn ->
                syn.strength += learningRate * tdError * previousSensorValues[j]
            }

            // Simple TD actor update: strengthen or weaken the chosen program's preference weights
            // using the previous state's sensor activations.
            // Update only the preference weights projecting to the winning program node
            // from the previous step.
            for (j in previousSensorValues.indices) {
                actorWeightMatrix.weights[previousWinningProgram, j] +=
                    actorLearningRate * tdError * previousSensorValues[j]
            }
            actorWeightMatrix.events.updated.fire()
        }

        previousValue = currentValue
        previousWinningProgram = winningProgram
        previousSensorValues = sensorValues
    }

    agent.events.collided.on { collidedWith ->
        if (collidedWith === cheese) {
            justHitCheese = true
            respawnObject(world, collidedWith)
        }
        if (collidedWith === poison) {
            justHitPoison = true
            respawnObject(world, collidedWith)
        }
    }

    fun resetLearning() {
        actorWeightMatrix.hardClear()
        programArray.clear()
        criticWeights.forEach { it.strength = 0.0 }
        previousValue = 0.0
        previousWinningProgram = -1
        previousSensorValues = DoubleArray(sensorNeurons.size) { 0.0 }
        stuckCounter = 0
        valueNeuron.activation = 0.0
        tdErrorNeuron.activation = 0.0
    }

    withGui {
        place(networkComponent, 320, 10, 650, 450)
        place(oc, 970, 10, 500, 500)

        activeTextLabel.location = point(50.0, -70.0)

        createControlPanel("Control Panel", 0, 10) {

            addLabel("Task:")
            addComboBox("", tasks, tasks[0]) { selectedTask ->
                cheeseRewardMultiplier = selectedTask.cheeseReward
                poisonRewardMultiplier = selectedTask.poisonReward
            }

            addSeparator()

            addCheckBox("Learning Enabled", learningEnabled) { learningEnabled = it }

            addCheckBox("Sparse Reward (collision only)", sparseReward) { sparseReward = it }

            addSeparator()

            addFormattedNumericTextField("Critic Learning Rate", initValue = learningRate) {
                learningRate = it
            }

            addFormattedNumericTextField("Actor Learning Rate", initValue = actorLearningRate) {
                actorLearningRate = it
            }

            addFormattedNumericTextField("Gamma", initValue = gamma) {
                gamma = it
            }

            addFormattedNumericTextField("Temperature", initValue = temperature) {
                temperature = it.coerceAtLeast(0.01)
            }

            addFormattedNumericTextField("Speed Bias", initValue = straight.bias) {
                straight.bias = it
            }

            addFormattedNumericTextField("Weight Strength", initValue = programWeightStrength) {
                programWeightStrength = it
            }

            addSeparator()

            addButton("Reset Learning") { resetLearning() }

            swingInvokeLater { pack() }
        }
    }

    // Headless mode
    var maxIterations: Int? = null
    var currentIteration = 0

    if (optionString?.isNotEmpty() == true) {
        val options = JSONObject(optionString)

        if (options.has("taskIndex")) {
            val taskIndex = options.getInt("taskIndex")
            if (taskIndex in tasks.indices) {
                val task = tasks[taskIndex]
                cheeseRewardMultiplier = task.cheeseReward
                poisonRewardMultiplier = task.poisonReward
                println("Task set to: ${task.name}")
            }
        }
        if (options.has("learningRate")) learningRate = options.getDouble("learningRate")
        if (options.has("actorLearningRate")) actorLearningRate = options.getDouble("actorLearningRate")
        if (options.has("gamma")) gamma = options.getDouble("gamma")
        if (options.has("temperature")) temperature = options.getDouble("temperature").coerceAtLeast(0.01)
        if (options.has("maxIterations")) {
            maxIterations = options.getInt("maxIterations")
            println("Max iterations: $maxIterations, temperature=$temperature, lr=$learningRate, actorLR=$actorLearningRate")
        }

        maxIterations?.let { max ->
            println("=== Starting Headless Simulation ===")
            workspace.addUpdateAction("Headless iteration counter") {
                currentIteration++
                if (currentIteration % 1000 == 0) {
                    val activations = programArray.activations.toDoubleArray()
                    println("Iter $currentIteration | probs=[${activations.map { "%.3f".format(it) }.joinToString(",")}] | reward=${"%.3f".format(rewardNeuron.activation)} | tdErr=${"%.3f".format(tdErrorNeuron.activation)}")
                }
                if (currentIteration >= max) {
                    val activations = programArray.activations.toDoubleArray()
                    println("\n=== Final Results (iter $currentIteration) ===")
                    programNames.forEachIndexed { i, name -> println("  [$i] $name: ${"%.4f".format(activations[i])}") }
                    val winner = activations.indices.maxByOrNull { activations[it] }!!
                    println("Winner: ${programNames[winner]} (index $winner)")
                    workspace.stop()
                }
            }
            runBlocking { workspace.iterateSuspend(max) }
            println("=== Simulation Complete ===")
        }
    }

    addSidebarInfo(
        """
    # Braitenberg Program Learning

    A program-based actor-critic experiment using discrete Braitenberg controllers.

    ## The Setup

    Four programs (discrete actions) control a Braitenberg vehicle:
    - **Seek Cheese, Avoid Poison**: approach cheese, flee poison
    - **Seek Both Objects**: approach everything
    - **Avoid Both Objects**: flee everything
    - **Seek Poison, Avoid Cheese**: approach poison, flee cheese

    The agent selects programs using a **softmax policy**. The program activations show the current probability of selecting each program.

    The actor computes preferences from sensor inputs via a **weight matrix**:
    ```
    preference[i] = Σ w[i][j] × sensor[j]
    ```
    The NeuronArray applies softmax to get probabilities. The selected program then sets the turn weights for that step.

    The actor uses a simple TD-style update on the chosen program:
    ```
    Δw[chosen,j] = α × δ × sensor[j]
    ```

    ## Critic

    The critic maps sensors to value: `V(s) = Σ w_c[j] × sensor[j]`. This is a proper state-based value estimate — "how good is it to be HERE?" TD error: `δ = r + γV(s') - V(s)`.

    ## Temperature

    Controls the softmax sharpness:
    - **Low (e.g. 0.1)**: nearly greedy — exploits the best program
    - **0.5**: good starting point for this simulation
    - **1.0**: softer exploration
    - **High (e.g. 5.0)**: nearly uniform — explores all programs equally

    ## Reward Modes

    - **Continuous** (default): proximity-based Gaussian reward, always provides a gradient signal
    - **Sparse**: reward ONLY on collision with cheese (+1) or poison (-1). This is the classic Sutton & Barto scenario where TD must propagate value backward from distant rewards.

    ## What The Investigation Found

    Headless testing found several real implementation issues, and those were fixed:
    - reward scale mismatch
    - softmax temperature initialization bug
    - unnecessary policy-gradient / bandit bookkeeping for the current design

    After those fixes, a simpler TD-style actor was implemented. That version is cleaner and somewhat better, but repeated runs still do not reliably learn all four tasks.

    The current conclusion is that the remaining difficulty appears to be **structural**:
    - each action is a whole bundled Braitenberg program
    - states often contain mixed cheese and poison evidence
    - one TD error must reinforce or punish the entire bundled behavior

    This makes credit assignment much harder than in `BraitenbergRL.kt`, where TD updates direct sensor-to-motor weights.

    ## Reading the Display

    The program nodes display probabilities, not binary activations. In successful runs one program becomes dominant. In many runs the probabilities stay ambiguous, spike briefly, or drift back toward uniform.

    """.trimIndent()
    )

}
