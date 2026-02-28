package org.simbrain.custom_sims.simulations.rl

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.simbrain.custom_sims.*
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapseAsync
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.swingInvokeLater
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.awt.geom.Point2D
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val braitenbergProgramLearning = newSim { optionString ->

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
    var learningEnabled = true
    var programWeightStrength = 20.0
    var cheeseRewardMultiplier = 1.0
    var poisonRewardMultiplier = -1.0

    workspace.clearWorkspace()
    val oc = addOdorWorldComponent("Program Learning World")
    oc.world.tileMap.updateMapSize(20, 20)
    val world = oc.world
    world.isObjectsBlockMovement = true

    val poison = oc.world.addEntity(398, 335, EntityType.Poison)
    val cheese = oc.world.addEntity(500, 184, EntityType.Swiss)

    val sharedDecayFunction = GaussianDecayFunction(75.0)

    fun calculateReward(agent: OdorWorldEntity): Double {
        val distanceToCheese = agent.location.distance(cheese.location)
        val cheeseReward = 15.0 * sharedDecayFunction.getScalingFactor(distanceToCheese) * cheeseRewardMultiplier

        val distanceToPoison = agent.location.distance(poison.location)
        val poisonReward = 15.0 * sharedDecayFunction.getScalingFactor(distanceToPoison) * poisonRewardMultiplier

        return cheeseReward + poisonReward
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
    val rightTurn = runBlocking {
        network.addNeuron(100, 0).apply {
            label = "Right"
            lowerBound = -200.0
            upperBound = 200.0
        }
    }

    val cheeseLeftToLeftTurn = network.addSynapseAsync(cheeseLeftInput, leftTurn)
    val cheeseRightToRightTurn = network.addSynapseAsync(cheeseRightInput, rightTurn)
    val poisonLeftToLeftTurn = network.addSynapseAsync(poisonLeftInput, leftTurn)
    val poisonRightToRightTurn = network.addSynapseAsync(poisonRightInput, rightTurn)

    val programNames = listOf(
        "Seek Cheese, Avoid Poison",
        "Seek Both Objects",
        "Avoid Both Objects",
        "Seek Poison, Avoid Cheese"
    )
    
    val programNodes = listOf(
        runBlocking {
            network.addNeuron(250, 50).apply {
                label = programNames[0]
                bias = 0.0
                upperBound = 100.0
                lowerBound = -100.0
            }
        },
        runBlocking {
            network.addNeuron(250, 100).apply {
                label = programNames[1]
                bias = 0.0
                upperBound = 100.0
                lowerBound = -100.0
            }
        },
        runBlocking {
            network.addNeuron(250, 150).apply {
                label = programNames[2]
                bias = 0.0
                upperBound = 100.0
                lowerBound = -100.0
            }
        },
        runBlocking {
            network.addNeuron(250, 200).apply {
                label = programNames[3]
                bias = 0.0
                upperBound = 100.0
                lowerBound = -100.0
            }
        }
    )

    val rewardNeuron = network.addNeuron(400, 100).apply {
        clamped = true
        label = "Reward"
    }
    val valueNeuron = network.addNeuron(450, 100).apply {
        label = "Value"
        upperBound = 100.0
    }
    val tdErrorNeuron = network.addNeuron(500, 100).apply {
        label = "TD error"
        upperBound = 100.0
        lowerBound = -100.0
    }

    val criticWeights = programNodes.map { programNode ->
        network.addSynapseAsync(programNode, valueNeuron).apply {
            strength = 0.0
        }
    }

    network.addNetworkModels(activeTextLabel)

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

    var previousValue = 0.0
    var activeProgram = 0

    var currentIteration = 0
    var currentTaskIndex = 0
    var csvData: StringBuilder? = null
    var csvFilePath: String? = null
    var maxIterations: Int? = null
    var parameterSchedule: List<Pair<Int, JSONObject>> = emptyList()

    fun applyParameterChanges(params: JSONObject) {
        if (params.has("taskIndex")) {
            val newTaskIndex = params.getInt("taskIndex")
            if (newTaskIndex in tasks.indices) {
                currentTaskIndex = newTaskIndex
                val task = tasks[currentTaskIndex]
                cheeseRewardMultiplier = task.cheeseReward
                poisonRewardMultiplier = task.poisonReward
                println("[Iteration $currentIteration] Task changed to: ${task.name}")
            }
        }
        if (params.has("learningRate")) {
            learningRate = params.getDouble("learningRate")
            println("[Iteration $currentIteration] Learning rate changed to: $learningRate")
        }
        if (params.has("gamma")) {
            gamma = params.getDouble("gamma")
            println("[Iteration $currentIteration] Gamma changed to: $gamma")
        }
        if (params.has("programWeightStrength")) {
            programWeightStrength = params.getDouble("programWeightStrength")
            println("[Iteration $currentIteration] Program weight strength changed to: $programWeightStrength")
        }
        if (params.has("learningEnabled")) {
            learningEnabled = params.getBoolean("learningEnabled")
            println("[Iteration $currentIteration] Learning enabled changed to: $learningEnabled")
        }
    }

    fun appendCsvRow() {
        csvData?.append(
            listOf(
                currentIteration,
                currentTaskIndex,
                learningRate,
                gamma,
                programWeightStrength,
                activeProgram,
                cheeseLeftInput.activation,
                cheeseRightInput.activation,
                poisonLeftInput.activation,
                poisonRightInput.activation,
                straight.activation,
                leftTurn.activation,
                rightTurn.activation,
                rewardNeuron.activation,
                valueNeuron.activation,
                tdErrorNeuron.activation,
                programNodes[0].bias,
                programNodes[1].bias,
                programNodes[2].bias,
                programNodes[3].bias,
                programNodes[0].activation,
                programNodes[1].activation,
                programNodes[2].activation,
                programNodes[3].activation,
                criticWeights[0].strength,
                criticWeights[1].strength,
                criticWeights[2].strength,
                criticWeights[3].strength,
                cheeseLeftToLeftTurn.strength,
                cheeseRightToRightTurn.strength,
                poisonLeftToLeftTurn.strength,
                poisonRightToRightTurn.strength,
                agent.x,
                agent.y,
                agent.heading,
                cheese.x,
                cheese.y,
                poison.x,
                poison.y
            ).joinToString(",") + "\n"
        )
    }

    fun writeCsvFile() {
        csvData?.let { data ->
            val filePath = csvFilePath ?: run {
                val outputDir = File("simulation_outputs")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
                "simulation_outputs/braitenberg_program_learning_$timestamp.csv"
            }
            
            val header = listOf(
                "iteration", "taskIndex", "learningRate", "gamma", "programWeightStrength", "activeProgram",
                "cheeseLeftInput", "cheeseRightInput", "poisonLeftInput", "poisonRightInput",
                "straight", "leftTurn", "rightTurn", "reward", "value", "tdError",
                "program0_bias", "program1_bias", "program2_bias", "program3_bias",
                "program0_activation", "program1_activation", "program2_activation", "program3_activation",
                "criticW_program0", "criticW_program1", "criticW_program2", "criticW_program3",
                "actorW_cheeseL_leftTurn", "actorW_cheeseR_rightTurn", "actorW_poisonL_leftTurn", "actorW_poisonR_rightTurn",
                "agentX", "agentY", "agentHeading",
                "cheeseX", "cheeseY", "poisonX", "poisonY"
            ).joinToString(",")
            
            val fullContent = StringBuilder()
            fullContent.append("# BraitenbergProgramLearning Simulation Results\n")
            fullContent.append("# Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}\n")
            fullContent.append("# MaxIterations: $maxIterations\n")
            fullContent.append("#\n")
            fullContent.append(header + "\n")
            fullContent.append(data)
            
            File(filePath).writeText(fullContent.toString())
            println("\nCSV data exported to: $filePath")
        }
    }

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

    workspace.addUpdateAction("Program Learning") {
        currentIteration++

        parameterSchedule.filter { it.first == currentIteration }.forEach { (_, params) ->
            applyParameterChanges(params)
        }

        activeProgram = programNodes.indices.maxByOrNull { programNodes[it].bias } ?: 0
        
        programNodes.forEachIndexed { index, node ->
            node.activation = if (index == activeProgram) 1.0 else 0.0
        }

        activeTextLabel.text = "Active: ${programNames[activeProgram]}"

        applyProgram(activeProgram)

        rewardNeuron.activation = calculateReward(agent)

        val currentValue = valueNeuron.activation
        val tdError = rewardNeuron.activation + gamma * currentValue - previousValue
        tdErrorNeuron.activation = tdError

        if (learningEnabled) {
            criticWeights.forEach { syn ->
                syn.strength += learningRate * tdError * syn.source.activation
            }

            programNodes[activeProgram].bias += learningRate * tdError
        }

        previousValue = currentValue

        appendCsvRow()

        maxIterations?.let { max ->
            if (currentIteration >= max) {
                writeCsvFile()
                workspace.stop()
            }
        }
    }

    fun respawnObject(obj: OdorWorldEntity, minSeparation: Double = 100.0) {
        var newLoc: Point2D
        do {
            newLoc = Point2D.Double((100..600).random().toDouble(), (100..600).random().toDouble())
        } while (world.entityList.any { it !== obj && newLoc.distance(it.location) < minSeparation })
        obj.location = newLoc
    }

    agent.events.collided.on { collidedWith ->
        if (collidedWith === cheese || collidedWith === poison) {
            respawnObject(collidedWith)
        }
    }

    withGui {
        place(networkComponent, 320, 10, 550, 350)
        place(oc, 870, 10, 500, 500)

        activeTextLabel.location = point(50.0, -50.0)

        createControlPanel("Control Panel", 0, 10) {

            addLabel("Task:")

            val taskComboBox = addComboBox("", tasks, tasks[0]) { selectedTask ->
                cheeseRewardMultiplier = selectedTask.cheeseReward
                poisonRewardMultiplier = selectedTask.poisonReward
            }
            taskComboBox.toolTipText = "Select the learning task"

            addSeparator()

            val learningCheckbox = addCheckBox("Learning Enabled", learningEnabled) { enabled ->
                learningEnabled = enabled
            }
            learningCheckbox.toolTipText = "Enable/disable learning on program biases"

            addSeparator()

            val lrField = addFormattedNumericTextField("Learning Rate", initValue = learningRate) {
                learningRate = it
            }
            lrField.toolTipText = "Step size for bias updates"

            val gammaField = addFormattedNumericTextField("Gamma", initValue = gamma) {
                gamma = it
            }
            gammaField.toolTipText = "Discount factor for future rewards"

            val strengthField = addFormattedNumericTextField("Weight Strength", initValue = programWeightStrength) {
                programWeightStrength = it
            }
            strengthField.toolTipText = "Magnitude of weights when program is active"

            addSeparator()

            val resetButton = addButton("Reset Biases") {
                programNodes.forEach { it.bias = 0.0 }
                criticWeights.forEach { it.strength = 0.0 }
            }
            resetButton.toolTipText = "Reset all program biases to 0"

            swingInvokeLater { pack() }
        }
    }

    addSidebarInfo(
        """
    # Braitenberg Program Learning

    A Braitenberg vehicle that learns by selecting between four pre-programmed behavioral policies. Instead of learning connection weights directly, the agent learns biases on four "program" neurons. The program with the highest bias is selected via winner-take-all, and its corresponding weight pattern is applied.

    ## The Four Programs

    Each program represents a complete policy (how to respond to both cheese AND poison):

    - **Seek Cheese, Avoid Poison**: Cheese weights +`weightStrength`, poison weights -`weightStrength`
    - **Seek Both Objects**: All weights set to +`weightStrength` (approach both)
    - **Avoid Both Objects**: All weights set to -`weightStrength` (avoid both)
    - **Seek Poison, Avoid Cheese**: Poison weights +`weightStrength`, cheese weights -`weightStrength`

    ## How It Works

    At each timestep:
    1. The program with the highest bias is selected (winner-take-all)
    2. That program's weight pattern is applied to the four actor connections
    3. The agent acts according to those weights
    4. Reward is calculated based on proximity and the selected task
    5. TD error is computed: `r + γ×V(current) - V(previous)`
    6. The selected program's bias is updated: `bias += learningRate × tdError`
    7. Critic weights are also updated to predict value

    The program nodes show which program is active (activation = 1.0 for winner, 0.0 for others). Their biases are learned values representing how good each program is. The text label at the top shows which program is currently winning.

    # What to Do

    1. Select a `Task` from the dropdown menu in the control panel
    2. Make sure `Learning Enabled` is checked
    3. Click the workspace run button
    4. Watch which program gets selected (shown by activation and the text label)
    5. Observe the biases change over time
    6. The agent should learn to prefer the program that matches the task

    For example, if you select "Seek Cheese, Avoid Poison" as the task, the agent should learn to prefer the "Seek Cheese, Avoid Poison" program node, since that program's weight pattern maximizes reward for that task.

    The program nodes' biases represent learned preferences for each complete behavioral policy.
    
    Tip: The program runs faster if you minimize both main windows while learning. It should learn pretty quickly, maybe 200 iterations. Minimizing just the network window makes the behavior faster as well.

    """.trimIndent()
    )

    /*
     * ============================================================================
     * HEADLESS MODE DOCUMENTATION
     * ============================================================================
     *
     * SAMPLE TERMINAL COMMANDS:
     *
     * Basic run with 10000 iterations:
     *   ./gradlew runSim -PsimName="Braitenberg Program Learning" -PoptionString='{"maxIterations": 10000, "csvOutput": {}}'
     *
     * Task switching mid-training:
     *   ./gradlew runSim -PsimName="Braitenberg Program Learning" -PoptionString='{"taskIndex": 0, "maxIterations": 20000, "csvOutput": {}, "parameterSchedule": [{"atIteration": 5000, "set": {"taskIndex": 1}}]}'
     *
     * PARAMETERS:
     * - taskIndex: 0=Seek Cheese/Avoid Poison, 1=Seek Both, 2=Avoid Both, 3=Seek Poison/Avoid Cheese
     * - learningRate: Learning rate (default: 0.05)
     * - gamma: Discount factor (default: 0.95)
     * - programWeightStrength: Magnitude of weights applied by programs (default: 20.0)
     * - maxIterations: Number of iterations to run before stopping (required for headless mode)
     * - csvOutput: Enable CSV output of simulation data. Use {} for auto-generated filename,
     *              or {"filePath": "path/to/file.csv"} for custom path
     * - parameterSchedule: List of parameter changes triggered at specific iterations
     *     - atIteration: Iteration number to trigger the change
     *     - set: Object with parameters to change (taskIndex, learningRate, gamma, programWeightStrength, learningEnabled)
     *
     * CSV OUTPUT COLUMNS:
     *   iteration, taskIndex, learningRate, gamma, programWeightStrength, activeProgram,
     *   cheeseLeftInput, cheeseRightInput, poisonLeftInput, poisonRightInput,
     *   straight, leftTurn, rightTurn, reward, value, tdError,
     *   program0_bias, program1_bias, program2_bias, program3_bias,
     *   program0_activation, program1_activation, program2_activation, program3_activation,
     *   criticW_program0, criticW_program1, criticW_program2, criticW_program3,
     *   actorW_cheeseL_leftTurn, actorW_cheeseR_rightTurn, actorW_poisonL_leftTurn, actorW_poisonR_rightTurn,
     *   agentX, agentY, agentHeading, cheeseX, cheeseY, poisonX, poisonY
     */

    if (optionString?.isNotEmpty() == true) {
        val options = JSONObject(optionString)

        learningRate = options.optDouble("learningRate", learningRate)
        gamma = options.optDouble("gamma", gamma)
        programWeightStrength = options.optDouble("programWeightStrength", programWeightStrength)

        if (options.has("taskIndex")) {
            val taskIndex = options.getInt("taskIndex")
            if (taskIndex in tasks.indices) {
                currentTaskIndex = taskIndex
                val task = tasks[currentTaskIndex]
                cheeseRewardMultiplier = task.cheeseReward
                poisonRewardMultiplier = task.poisonReward
            }
        }

        if (options.has("maxIterations")) {
            maxIterations = options.getInt("maxIterations")
            println("Max iterations set to: $maxIterations")
        }

        if (options.has("csvOutput")) {
            csvData = StringBuilder()
            options.optJSONObject("csvOutput")?.let { csvConfig ->
                if (csvConfig.has("filePath")) {
                    csvFilePath = csvConfig.getString("filePath")
                }
            }
            println("CSV output enabled" + (csvFilePath?.let { ": $it" } ?: " (auto-generated filename)"))
        }

        options.optJSONArray("parameterSchedule")?.let { scheduleArray ->
            parameterSchedule = (0 until scheduleArray.length()).mapNotNull { i ->
                val item = scheduleArray.getJSONObject(i)
                if (item.has("atIteration") && item.has("set")) {
                    val iteration = item.getInt("atIteration")
                    val params = item.getJSONObject("set")
                    Pair(iteration, params)
                } else {
                    null
                }
            }
            if (parameterSchedule.isNotEmpty()) {
                println("Parameter schedule configured with ${parameterSchedule.size} triggers:")
                parameterSchedule.forEach { (iter, params) ->
                    println("  - At iteration $iter: ${params.keySet().joinToString(", ")}")
                }
            }
        }

        maxIterations?.let { max ->
            println("\n=== Starting Headless Simulation ===")
            println("Running for $max iterations...")
            
            runBlocking {
                workspace.iterateSuspend(max)
            }
            
            println("=== Simulation Complete ===")
        }
    }

}
