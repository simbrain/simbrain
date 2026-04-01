package org.simbrain.custom_sims.simulations.rl

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.network.updaterules.SoftmaxRule
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
 * Two TD learning experiments comparing actor architectures:
 *
 * 1. State-dependent actor: sensors feed into actor weights, softmax over programs.
 *    Learns WHICH program to use based on what the agent currently sees.
 *
 * 2. State-independent actor (bandit): no sensor input, just per-program preferences.
 *    Learns which program is globally best regardless of state.
 *
 * Both use a proper sensor-based critic (V = w·sensors) and softmax policy
 * following Sutton & Barto's actor-critic framework.
 */
val tdExperiments = newSim { optionString ->

    data class Task(
        val name: String,
        val cheeseReward: Double,
        val poisonReward: Double
    ) {
        override fun toString() = name
    }

    val tasks = listOf(
        Task("Seek Cheese, Avoid Poison", 1.0, -1.0),
        Task("Seek Both Objects", 1.0, 1.0),
        Task("Avoid Both Objects", -1.0, -1.0),
        Task("Seek Poison, Avoid Cheese", -1.0, 1.0)
    )

    var learningRate = 0.05
    var gamma = 0.95
    var programWeightStrength = 20.0
    var cheeseRewardMultiplier = 1.0
    var poisonRewardMultiplier = -1.0
    var learningEnabled = true
    var sparseReward = false
    var temperature = 1.0
    var stateDependent = true

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

    val sharedDecayFunction = GaussianDecayFunction(150.0)

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
        val cheeseReward = 15.0 * sharedDecayFunction.getScalingFactor(distanceToCheese) * cheeseRewardMultiplier
        val distanceToPoison = agent.location.distance(poison.location)
        val poisonReward = 15.0 * sharedDecayFunction.getScalingFactor(distanceToPoison) * poisonRewardMultiplier
        return cheeseReward + poisonReward
    }

    fun sample(probabilities: DoubleArray): Int {
        val r = Math.random()
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
    val modeTextLabel = NetworkTextObject("Mode: State-Dependent").apply { fontSize = 14 }

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
        network.addNeuron(0, 150).apply { label = "Cheese (L)"; clamped = true }
    }
    val cheeseRightInput = runBlocking {
        network.addNeuron(50, 150).apply { label = "Cheese (R)"; clamped = true }
    }
    val poisonLeftInput = runBlocking {
        network.addNeuron(100, 150).apply { label = "Poison (L)"; clamped = true }
    }
    val poisonRightInput = runBlocking {
        network.addNeuron(150, 150).apply { label = "Poison (R)"; clamped = true }
    }

    val sensorNeurons = listOf(cheeseLeftInput, cheeseRightInput, poisonLeftInput, poisonRightInput)
    val sensorCollection = NeuronCollection(sensorNeurons).apply { label = "Sensors" }
    network.addNetworkModelAsync(sensorCollection)

    // Motor neurons
    val straight = runBlocking {
        network.addNeuron(50, 0).apply {
            label = "Speed"; activation = 0.0; clamped = false; bias = 0.5; upperBound = 3.0
        }
    }
    val leftTurn = runBlocking {
        network.addNeuron(0, 0).apply { label = "Left"; lowerBound = -200.0; upperBound = 200.0 }
    }
    val rightTurn = runBlocking {
        network.addNeuron(100, 0).apply { label = "Right"; lowerBound = -200.0; upperBound = 200.0 }
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

    val softmaxRule = SoftmaxRule().apply { this.temperature = 1.0 }

    val programArray = NeuronArray(numPrograms).apply {
        label = "Programs"
        updateRule = softmaxRule
        circleMode = true
        verticalLayout = true
        labelArray = programNames.toTypedArray()
    }
    network.addNetworkModelAsync(programArray)
    programArray.setLocation(300.0, 100.0)

    // Weight matrix: sensors → programs (the actor weights for state-dependent mode)
    val actorWeightMatrix = WeightMatrix(sensorCollection, programArray)
    network.addNetworkModelAsync(actorWeightMatrix)

    // Critic and TD error neurons
    val rewardNeuron = network.addNeuron(450, 50).apply { clamped = true; label = "Reward" }
    val valueNeuron = network.addNeuron(500, 50).apply { label = "Value"; upperBound = 100.0; lowerBound = -100.0 }
    val tdErrorNeuron = network.addNeuron(550, 50).apply { label = "TD Error"; upperBound = 100.0; lowerBound = -100.0 }

    // Critic weights: sensors → value
    val criticWeights = sensorNeurons.map { sensorNeuron ->
        network.addSynapseAsync(sensorNeuron, valueNeuron).apply { strength = 0.0 }
    }

    network.addNetworkModels(activeTextLabel)
    network.addNetworkModels(modeTextLabel)

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

    // In bandit mode, preferences are stored as programArray biases
    // (SoftmaxRule uses inputs + biases, so with no weight matrix, biases alone drive softmax)

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

        // Sync softmax temperature from the control panel
        softmaxRule.temperature = temperature

        // SoftmaxRule already computed probabilities from weighted inputs + biases
        // State-dependent: weight matrix provides inputs, biases are 0
        // Bandit: no weight matrix, biases alone drive softmax
        var probabilities = programArray.activations.toDoubleArray()
        // First iteration before network update, use uniform
        if (probabilities.sum() < 1e-6) {
            probabilities = DoubleArray(numPrograms) { 1.0 / numPrograms }
        }

        val activeProgram = sample(probabilities)
        activeTextLabel.text = "Active: ${programNames[activeProgram]} (p=${"%.2f".format(probabilities[activeProgram])})"

        applyProgram(activeProgram)

        // Reward
        rewardNeuron.activation = calculateReward(agent)

        // Critic: V(s) from sensors
        val currentValue = valueNeuron.activation
        val tdError = rewardNeuron.activation + gamma * currentValue - previousValue
        tdErrorNeuron.activation = tdError

        if (learningEnabled) {
            // Update critic weights: w_c += α * δ * sensor
            criticWeights.forEachIndexed { j, syn ->
                syn.strength += learningRate * tdError * sensorValues[j]
            }

            // Update actor using policy gradient: ∇ln(π) * δ
            if (stateDependent) {
                // Update weight matrix: Δw[i][j] += α * δ * sensor[j] * (1{i=chosen} - π(i))
                // WeightMatrix is target-source format: weights[programIndex, sensorIndex]
                for (i in 0 until numPrograms) {
                    val indicator = if (i == activeProgram) 1.0 else 0.0
                    for (j in sensorValues.indices) {
                        actorWeightMatrix.weights[i, j] += learningRate * tdError * sensorValues[j] * (indicator - probabilities[i])
                    }
                }
                actorWeightMatrix.events.updated.fire()
            } else {
                // Bandit: update biases on programArray (no sensor input)
                val biases = programArray.biases
                for (i in 0 until numPrograms) {
                    val indicator = if (i == activeProgram) 1.0 else 0.0
                    biases[i, 0] = biases[i, 0] + learningRate * tdError * (indicator - probabilities[i])
                }
            }
        }

        previousValue = currentValue
    }

    fun respawnObject(obj: OdorWorldEntity, minSeparation: Double = 100.0) {
        var newLoc: Point2D
        do {
            newLoc = Point2D.Double((100..600).random().toDouble(), (100..600).random().toDouble())
        } while (world.entityList.any { it !== obj && newLoc.distance(it.location) < minSeparation })
        obj.location = newLoc
    }

    agent.events.collided.on { collidedWith ->
        if (collidedWith === cheese) {
            justHitCheese = true
            respawnObject(collidedWith)
        }
        if (collidedWith === poison) {
            justHitPoison = true
            respawnObject(collidedWith)
        }
    }

    fun resetLearning() {
        actorWeightMatrix.hardClear()
        programArray.clear()
        programArray.biases.fill(0.0)
        criticWeights.forEach { it.strength = 0.0 }
        previousValue = 0.0
        valueNeuron.activation = 0.0
        tdErrorNeuron.activation = 0.0
    }

    fun removeWeightMatrix() {
        sensorCollection.removeOutgoingConnector(actorWeightMatrix)
        programArray.removeIncomingConnector(actorWeightMatrix)
        actorWeightMatrix.events.deleted.fire(actorWeightMatrix)
    }

    fun addWeightMatrix() {
        sensorCollection.addOutgoingConnector(actorWeightMatrix)
        programArray.addIncomingConnector(actorWeightMatrix)
        network.addNetworkModelAsync(actorWeightMatrix)
    }

    withGui {
        place(networkComponent, 320, 10, 650, 450)
        place(oc, 970, 10, 500, 500)

        activeTextLabel.location = point(50.0, -50.0)
        modeTextLabel.location = point(50.0, -70.0)

        createControlPanel("Control Panel", 0, 10) {

            addLabel("Task:")
            addComboBox("", tasks, tasks[0]) { selectedTask ->
                cheeseRewardMultiplier = selectedTask.cheeseReward
                poisonRewardMultiplier = selectedTask.poisonReward
            }

            addSeparator()

            addCheckBox("Learning Enabled", learningEnabled) { learningEnabled = it }

            addCheckBox("Sparse Reward (collision only)", sparseReward) { sparseReward = it }

            addCheckBox("State-Dependent Actor", stateDependent) { enabled ->
                stateDependent = enabled
                modeTextLabel.text = if (enabled) "Mode: State-Dependent" else "Mode: Bandit"
                resetLearning()
                if (enabled) {
                    addWeightMatrix()
                } else {
                    launch { removeWeightMatrix() }
                }
            }

            addSeparator()

            addFormattedNumericTextField("Learning Rate", initValue = learningRate) {
                learningRate = it
            }

            addFormattedNumericTextField("Gamma", initValue = gamma) {
                gamma = it
            }

            addFormattedNumericTextField("Temperature", initValue = temperature) {
                temperature = it.coerceAtLeast(0.01)
            }

            addFormattedNumericTextField("Weight Strength", initValue = programWeightStrength) {
                programWeightStrength = it
            }

            addSeparator()

            addButton("Reset Learning") { resetLearning() }

            swingInvokeLater { pack() }
        }
    }

    addSidebarInfo(
        """
    # TD Experiments: Actor-Critic Architectures

    Two experiments comparing actor architectures for program selection, following Sutton & Barto's actor-critic framework (Chapters 6 & 13).

    ## The Setup

    Four programs (discrete actions) control a Braitenberg vehicle:
    - **Seek Cheese, Avoid Poison**: approach cheese, flee poison
    - **Seek Both Objects**: approach everything
    - **Avoid Both Objects**: flee everything
    - **Seek Poison, Avoid Cheese**: approach poison, flee cheese

    The agent selects programs using a **softmax policy** (programs show their selection probability). The program activations show π(program|state) — the probability of selecting each program.

    ## Experiment 1: State-Dependent Actor

    The actor computes preferences from sensor inputs via a **weight matrix** (visible in the network):
    ```
    preference[i] = Σ w[i][j] × sensor[j]
    ```
    The NeuronArray applies softmax to get probabilities. This allows state-dependent decisions: "when I see cheese nearby, prefer the seek-cheese program."

    The actor update follows the policy gradient theorem:
    ```
    Δw[i][j] = α × δ × sensor[j] × (1{i=chosen} - π(i))
    ```

    ## Experiment 2: State-Independent (Bandit)

    The actor has one preference value per program with no sensor input:
    ```
    Δpref[i] = α × δ × (1{i=chosen} - π(i))
    ```
    This learns which program is globally best, like a multi-armed bandit. It cannot adapt behavior based on what the agent sees.

    ## Critic (Both Experiments)

    The critic maps sensors to value: `V(s) = Σ w_c[j] × sensor[j]`. This is a proper state-based value estimate — "how good is it to be HERE?" TD error: `δ = r + γV(s') - V(s)`.

    ## Temperature

    Controls the softmax sharpness:
    - **Low (e.g. 0.1)**: nearly greedy — exploits the best program
    - **1.0**: standard softmax
    - **High (e.g. 5.0)**: nearly uniform — explores all programs equally

    ## Reward Modes

    - **Continuous** (default): proximity-based Gaussian reward, always provides a gradient signal
    - **Sparse**: reward ONLY on collision with cheese (+1) or poison (-1). This is the classic Sutton & Barto scenario where TD must propagate value backward from distant rewards.

    ## What to Try

    1. Run with continuous reward + state-dependent actor — should learn quickly
    2. Switch to sparse reward — much harder, TD error must build the value gradient over time
    3. Switch to bandit mode — can it still learn? Compare learning speed
    4. Try sparse + bandit — the hardest combination

    The program nodes display selection probabilities (not binary activations). Early on they should be near 0.25 each (uniform), then shift toward the correct program.

    """.trimIndent()
    )

}
