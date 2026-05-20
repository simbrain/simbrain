package org.simbrain.custom_sims.simulations.rl

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.decayfunctions.LinearDecayFunction
import org.simbrain.util.decayfunctions.StepDecayFunction
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.awt.*
import java.awt.geom.Point2D
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JButton
import javax.swing.JInternalFrame
import javax.swing.JPanel
import javax.swing.event.InternalFrameAdapter
import javax.swing.event.InternalFrameEvent

class RewardConfig(
    private val label: String,
    initDecayFunction: DecayFunction = GaussianDecayFunction(150.0)
) : EditableObject {
    override val name = label

    @UserParameter(label = "Max Reward", description = "Maximum reward magnitude", order = 1)
    var maxReward: Double = 15.0

    @UserParameter(label = "Decay Function", showDetails = false, order = 2)
    var decayFunction: DecayFunction = initDecayFunction

    fun calculateReward(distance: Double, multiplier: Double): Double {
        return maxReward * decayFunction.getScalingFactor(distance) * multiplier
    }

    fun getSummary(): String {
        val typeName = decayFunction.name
        val disp = decayFunction.dispersion.toInt()
        return "$typeName (max=${"%.1f".format(maxReward)}, disp=$disp)"
    }
}

data class Task(
    val name: String,
    val cheeseReward: Double,
    val poisonReward: Double
) {
    override fun toString() = name
}

val rlTasks = listOf(
    Task("Seek Cheese, Avoid Poison", 1.0, -1.0),
    Task("Seek Both Objects", 1.0, 1.0),
    Task("Avoid Both Objects", -1.0, -1.0),
    Task("Seek Poison, Avoid Cheese", -1.0, 1.0)
)

fun respawnObject(world: org.simbrain.world.odorworld.OdorWorld, obj: OdorWorldEntity, minSeparation: Double = 100.0) {
    var newLoc: Point2D
    do {
        newLoc = Point2D.Double((100..500).random().toDouble(), (100..500).random().toDouble())
    } while (world.entityList.any { it !== obj && newLoc.distance(it.location) < minSeparation })
    obj.location = newLoc
}

private data class WeightSpace(
    val label: String,
    val leftWeight: Synapse,
    val rightWeight: Synapse
)

private class BraitenbergWeightSpacePanel(
    private val spaces: List<WeightSpace>
) : JPanel() {

    private val trajectories = spaces.map { mutableListOf<Point2D.Double>() }
    private val maxTrajectoryPoints = 2000

    init {
        preferredSize = Dimension(720, 360)
        minimumSize = Dimension(520, 280)
        background = Color.WHITE
        recordCurrentWeights()
    }

    fun recordCurrentWeights() {
        spaces.forEachIndexed { index, space ->
            trajectories[index].add(Point2D.Double(space.leftWeight.strength, space.rightWeight.strength))
            if (trajectories[index].size > maxTrajectoryPoints) {
                trajectories[index].removeAt(0)
            }
        }
    }

    fun resetTrajectories() {
        trajectories.forEach { it.clear() }
        recordCurrentWeights()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val top = 34
        val bottom = 58
        val sideGap = 40
        val plotGap = 34
        val plotWidth = ((width - sideGap * 2 - plotGap) / spaces.size).coerceAtLeast(160)
        val plotHeight = (height - top - bottom).coerceAtLeast(160)
        val squareSize = minOf(plotWidth, plotHeight)
        val startY = top + (plotHeight - squareSize) / 2

        spaces.forEachIndexed { index, space ->
            val startX = sideGap + index * (plotWidth + plotGap) + (plotWidth - squareSize) / 2
            drawWeightSpace(g2, space, trajectories[index], startX, startY, squareSize)
        }

        g2.dispose()
    }

    private fun drawWeightSpace(
        g2: Graphics2D,
        space: WeightSpace,
        trajectory: List<Point2D.Double>,
        x: Int,
        y: Int,
        size: Int
    ) {
        val leftMin = space.leftWeight.lowerBound
        val leftMax = space.leftWeight.upperBound
        val rightMin = space.rightWeight.lowerBound
        val rightMax = space.rightWeight.upperBound
        val leftValue = space.leftWeight.strength
        val rightValue = space.rightWeight.strength

        g2.color = Color(246, 248, 250)
        g2.fillRect(x, y, size, size)

        g2.color = Color(210, 218, 226)
        g2.stroke = BasicStroke(1f)
        g2.drawLine(x, y, x + size, y + size)

        g2.color = Color(226, 239, 228)
        val pursuingLabel = "mostly pursuing"
        drawCenteredString(g2, pursuingLabel, x + (size * 0.86).toInt(), y + (size * 0.18).toInt())
        g2.color = Color(241, 229, 229)
        val avoidingLabel = "mostly avoiding"
        drawCenteredString(g2, avoidingLabel, x + (size * 0.14).toInt(), y + (size * 0.82).toInt())

        g2.color = Color(55, 65, 81)
        g2.stroke = BasicStroke(1.5f)
        g2.drawRect(x, y, size, size)

        g2.font = font.deriveFont(Font.BOLD, 14f)
        drawCenteredString(g2, space.label, x + size / 2, y - 12)

        g2.font = font.deriveFont(Font.PLAIN, 11f)
        g2.color = Color(85, 85, 85)
        g2.drawString("left weight", x + size / 2 - 28, y + size + 28)
        g2.rotate(-Math.PI / 2, (x - 26).toDouble(), (y + size / 2).toDouble())
        drawCenteredString(g2, "right weight", x - 26, y + size / 2)
        g2.rotate(Math.PI / 2, (x - 26).toDouble(), (y + size / 2).toDouble())

        g2.drawString("%.1f".format(leftMin), x - 4, y + size + 14)
        drawRightAlignedString(g2, "%.1f".format(leftMax), x + size, y + size + 14)
        drawRightAlignedString(g2, "%.1f".format(rightMin), x - 8, y + size + 4)
        drawRightAlignedString(g2, "%.1f".format(rightMax), x - 8, y + 4)

        g2.color = Color(33, 102, 172, 105)
        g2.stroke = BasicStroke(1.4f)
        trajectory.zipWithNext().forEach { (from, to) ->
            val fromX = mapWeightToPixel(from.x, leftMin, leftMax, x, size)
            val fromY = mapWeightToPixel(from.y, rightMin, rightMax, y + size, -size)
            val toX = mapWeightToPixel(to.x, leftMin, leftMax, x, size)
            val toY = mapWeightToPixel(to.y, rightMin, rightMax, y + size, -size)
            g2.drawLine(fromX, fromY, toX, toY)
        }

        val px = mapWeightToPixel(leftValue, leftMin, leftMax, x, size)
        val py = mapWeightToPixel(rightValue, rightMin, rightMax, y + size, -size)
        g2.color = Color(33, 102, 172)
        g2.fillOval(px - 5, py - 5, 10, 10)
        g2.color = Color.WHITE
        g2.drawOval(px - 5, py - 5, 10, 10)

        g2.color = Color(45, 55, 72)
        val valueText = "(${ "%.2f".format(leftValue) }, ${ "%.2f".format(rightValue) })"
        drawCenteredString(g2, valueText, x + size / 2, y + size + 44)
    }

    private fun mapWeightToPixel(value: Double, min: Double, max: Double, origin: Int, extent: Int): Int {
        val normalized = if (max == min) 0.5 else ((value - min) / (max - min)).coerceIn(0.0, 1.0)
        return origin + (normalized * extent).toInt()
    }

    private fun drawCenteredString(g2: Graphics2D, text: String, centerX: Int, baselineY: Int) {
        val metrics: FontMetrics = g2.fontMetrics
        g2.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY)
    }

    private fun drawRightAlignedString(g2: Graphics2D, text: String, rightX: Int, baselineY: Int) {
        val metrics: FontMetrics = g2.fontMetrics
        g2.drawString(text, rightX - metrics.stringWidth(text), baselineY)
    }
}

/**
 * Using actor-critic to train a Braitenberg vehicle.
 *
 * The vehicle learns to approach or avoid cheese and poison objects based on reward feedback.
 * All actor weights are updated uniformly using TD error at each time step.
 */
val braitenbergRL = newSim { optionString ->

    val tasks = rlTasks

    var learningRate = 0.05
    var gamma = 0.95

    var learningEnabled = true
    var trainSpeedConnections = false

    var cheeseRewardMultiplier = 1.0
    var poisonRewardMultiplier = -1.0

    // Shared decay function for all sensors and rewards
    var sharedDecayFunction: DecayFunction = GaussianDecayFunction(75.0)

    // Configurable reward configs (share decay function via sharedDecayFunction)
    val cheeseRewardConfig = RewardConfig("Cheese Reward", sharedDecayFunction.copy() as DecayFunction)
    val poisonRewardConfig = RewardConfig("Poison Reward", sharedDecayFunction.copy() as DecayFunction)

    // Function to update the shared decay function and propagate to reward configs
    fun updateSharedDecayFunction(newDecayFunction: DecayFunction) {
        sharedDecayFunction = newDecayFunction
        // Update reward config decay functions
        cheeseRewardConfig.decayFunction = newDecayFunction.copy() as DecayFunction
        poisonRewardConfig.decayFunction = newDecayFunction.copy() as DecayFunction
    }

    workspace.clearWorkspace()
    val oc = addOdorWorldComponent("RL Braitenberg world")
    oc.world.tileMap.updateMapSize(16, 13)
    val world = oc.world
    world.isObjectsBlockMovement = true
    oc.world.isUseCameraCentering = false

    val poison = oc.world.addEntity(398, 335, EntityType.Poison)
    val cheese = oc.world.addEntity(500, 184, EntityType.Swiss)

    fun calculateReward(agent: OdorWorldEntity): Pair<Double, Double> {
        val distanceToCheese = agent.location.distance(cheese.location)
        val cheeseComponent = cheeseRewardConfig.calculateReward(distanceToCheese, cheeseRewardMultiplier)

        val distanceToPoison = agent.location.distance(poison.location)
        val poisonComponent = poisonRewardConfig.calculateReward(distanceToPoison, poisonRewardMultiplier)

        return Pair(cheeseComponent, poisonComponent)
    }

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val entityOffset = Point2D.Double(100.0, 100.0)
    val agent = oc.world.addEntity(entityOffset.x, entityOffset.y, EntityType.Circle).apply {
        addSensor(ObjectSensor(EntityType.Swiss, 50.0, 45.0).apply {
            label = "Cheese left"
            decayFunction = sharedDecayFunction.copy() as DecayFunction
        })
        addSensor(ObjectSensor(EntityType.Swiss, 50.0, -45.0).apply {
            label = "Cheese right"
            decayFunction = sharedDecayFunction.copy() as DecayFunction
        })
        addSensor(ObjectSensor(EntityType.Poison, 50.0, 45.0).apply {
            label = "Poison left"
            decayFunction = sharedDecayFunction.copy() as DecayFunction
        })
        addSensor(ObjectSensor(EntityType.Poison, 50.0, -45.0).apply {
            label = "Poison right"
            decayFunction = sharedDecayFunction.copy() as DecayFunction
        })
        addDefaultEffectors()
    }

    // Function to sync sensor decay functions with the shared decay function
    fun syncSensorDecayFunctions() {
        agent.sensors.filterIsInstance<ObjectSensor>().forEach { sensor ->
            sensor.decayFunction = sharedDecayFunction.copy() as DecayFunction
        }
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
    val cheeseLeftToLeftTurn = network.addSynapseAsync(cheeseLeftInput, leftTurn).apply {
        lowerBound = -50.0
        upperBound = 50.0
    }
    val cheeseRightToRightTurn = network.addSynapseAsync(cheeseRightInput, rightTurn).apply {
        lowerBound = -50.0
        upperBound = 50.0
    }
    val poisonLeftToLeftTurn = network.addSynapseAsync(poisonLeftInput, leftTurn).apply {
        lowerBound = -50.0
        upperBound = 50.0
    }
    val poisonRightToRightTurn = network.addSynapseAsync(poisonRightInput, rightTurn).apply {
        lowerBound = -50.0
        upperBound = 50.0
    }

    // Add actor synapses to list for learning
    actorSynapses.addAll(listOf(
        cheeseLeftToLeftTurn,
        cheeseRightToRightTurn,
        poisonLeftToLeftTurn,
        poisonRightToRightTurn
    ))

    // Speed synapses: trainable connections from sensors to speed
    val cheeseLeftToSpeed = network.addSynapse(cheeseLeftInput, straight).apply {
        strength = -1.0
        lowerBound = -50.0
        upperBound = 50.0
    }
    val cheeseRightToSpeed = network.addSynapse(cheeseRightInput, straight).apply {
        strength = -1.0
        lowerBound = -50.0
        upperBound = 50.0
    }
    val poisonLeftToSpeed = network.addSynapse(poisonLeftInput, straight).apply {
        strength = -1.0
        lowerBound = -50.0
        upperBound = 50.0
    }
    val poisonRightToSpeed = network.addSynapse(poisonRightInput, straight).apply {
        strength = -1.0
        lowerBound = -50.0
        upperBound = 50.0
    }
    val speedSynapses = listOf(cheeseLeftToSpeed, cheeseRightToSpeed, poisonLeftToSpeed, poisonRightToSpeed)
    fun setSpeedSynapsesEnabled(enabled: Boolean) {
        if (enabled) {
            speedSynapses.forEach { syn -> syn.isVisible = true}
            speedSynapses.forEach { syn -> syn.isEnabled = true}
        } else {
            speedSynapses.forEach { syn -> syn.isVisible = false}
            speedSynapses.forEach { syn -> syn.isEnabled = false}
        }
    }
    setSpeedSynapsesEnabled(trainSpeedConnections)


    val valueInputs = listOf(cheeseLeftInput, cheeseRightInput, poisonLeftInput, poisonRightInput)
    val criticWeights = valueInputs.map { input ->
        network.addSynapseAsync(input, valueNeuron).apply {
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

    var weightSpacePanel: BraitenbergWeightSpacePanel? = null
    var weightSpaceFrame: JInternalFrame? = null

    fun repaintWeightSpacePanel() {
        weightSpacePanel?.takeIf { it.isDisplayable }?.let { panel ->
            swingInvokeLater {
                panel.recordCurrentWeights()
                panel.repaint()
            }
        }
    }

    fun resetWeightSpaceTrajectories() {
        weightSpacePanel?.takeIf { it.isDisplayable }?.let { panel ->
            swingInvokeLater {
                panel.resetTrajectories()
                panel.repaint()
            }
        }
    }

    // Track previous value for TD error calculation
    var previousValue = 0.0
    var stepsSinceCollision = 0

    // Variables for headless mode features
    var currentIteration = 0
    var currentTaskIndex = 0
    var csvData: StringBuilder? = null
    var csvFilePath: String? = null
    var maxIterations: Int? = null
    var parameterSchedule: List<Pair<Int, JSONObject>> = emptyList()

    // Helper function to apply parameter changes from schedule
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
        if (params.has("learningEnabled")) {
            learningEnabled = params.getBoolean("learningEnabled")
            println("[Iteration $currentIteration] Learning enabled changed to: $learningEnabled")
        }
    }

    // Helper function to append CSV row
    fun appendCsvRow() {
        csvData?.append(
            listOf(
                currentIteration,
                currentTaskIndex,
                learningRate,
                gamma,
                // Neuron activations
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
                // Actor weights
                cheeseLeftToLeftTurn.strength,
                cheeseRightToRightTurn.strength,
                poisonLeftToLeftTurn.strength,
                poisonRightToRightTurn.strength,
                // Speed weights
                cheeseLeftToSpeed.strength,
                cheeseRightToSpeed.strength,
                poisonLeftToSpeed.strength,
                poisonRightToSpeed.strength,
                // Critic weights
                criticWeights[0].strength,
                criticWeights[1].strength,
                criticWeights[2].strength,
                criticWeights[3].strength,
                // Agent state
                agent.x,
                agent.y,
                agent.heading,
                // Object locations
                cheese.x,
                cheese.y,
                poison.x,
                poison.y
            ).joinToString(",") + "\n"
        )
    }

    // Helper function to write CSV file
    fun writeCsvFile() {
        csvData?.let { data ->
            val filePath = csvFilePath ?: run {
                val outputDir = File("simulation_outputs")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
                "simulation_outputs/braitenberg_rl_$timestamp.csv"
            }
            
            val header = listOf(
                "iteration", "taskIndex", "learningRate", "gamma",
                "cheeseLeftInput", "cheeseRightInput", "poisonLeftInput", "poisonRightInput",
                "straight", "leftTurn", "rightTurn", "reward", "value", "tdError",
                "actorW_cheeseL_leftTurn", "actorW_cheeseR_rightTurn", "actorW_poisonL_leftTurn", "actorW_poisonR_rightTurn",
                "speedW_cheeseL", "speedW_cheeseR", "speedW_poisonL", "speedW_poisonR",
                "criticW_cheeseL", "criticW_cheeseR", "criticW_poisonL", "criticW_poisonR",
                "agentX", "agentY", "agentHeading",
                "cheeseX", "cheeseY", "poisonX", "poisonY"
            ).joinToString(",")
            
            val fullContent = StringBuilder()
            fullContent.append("# BraitenbergRL Simulation Results\n")
            fullContent.append("# Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}\n")
            fullContent.append("# MaxIterations: $maxIterations\n")
            fullContent.append("#\n")
            fullContent.append(header + "\n")
            fullContent.append(data)
            
            File(filePath).writeText(fullContent.toString())
            println("\nCSV data exported to: $filePath")
        }
    }

    fun resetVehicle() {
        agent.location = Point2D.Double((50..500).random().toDouble(), (50..500).random().toDouble())
        agent.heading = (0..360).random().toDouble()
    }

    workspace.addUpdateAction("Update RL metrics and learning") {
        // Increment iteration counter
        currentIteration++

        // Check parameter schedule for changes
        parameterSchedule.filter { it.first == currentIteration }.forEach { (_, params) ->
            applyParameterChanges(params)
        }

        stepsSinceCollision++
        if (stepsSinceCollision > 1000) {
            resetVehicle()
            stepsSinceCollision = 0
            previousValue = 0.0
        }

        // Calculate current reward
        val (cheeseR, poisonR) = calculateReward(agent)
        rewardNeuron.activation = cheeseR + poisonR

        // Calculate current value (critic network output)
        // This is already computed by the network update, we just read it
        val currentValue = valueNeuron.activation

        // Calculate TD error: r + gamma * V(s') - V(s)
        val tdError = rewardNeuron.activation + gamma * currentValue - previousValue
        tdErrorNeuron.activation = tdError

        if (learningEnabled) {
            // Update critic weights
            valueNeuron.fanIn.forEach { syn ->
                syn.strength += learningRate * tdError * syn.source.activation
            }

            // Update actor weights (turn synapses)
            actorSynapses.forEach { syn ->
                syn.strength += learningRate * tdError * syn.source.activation
            }

            // Update speed synapses if enabled
            if (trainSpeedConnections) {
                speedSynapses.forEach { syn ->
                    syn.strength += learningRate * tdError * syn.source.activation
                }
            }
        }

        // Update previous value for next iteration
        previousValue = currentValue

        // Append CSV row if CSV output is enabled
        appendCsvRow()

        repaintWeightSpacePanel()

        // Check if max iterations reached
        maxIterations?.let { max ->
            if (currentIteration >= max) {
                writeCsvFile()
                workspace.stop()
            }
        }
    }

    var respawnCountPerTrial = 0

    fun getObjectName(entity: OdorWorldEntity): String {
        return when (entity.entityType) {
            EntityType.Swiss -> "Cheese"
            EntityType.Poison -> "Poison"
            else -> entity.entityType.name
        }
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

    agent.events.collided.on { collidedWith ->
        if (collidedWith === cheese || collidedWith === poison) {
            respawnCountPerTrial++
            respawnObject(world, collidedWith)
            stepsSinceCollision = 0
        }
    }

    fun updateCriticWeights(tdError: Double) {
        valueNeuron.fanIn.forEach { syn ->
            syn.strength += learningRate * tdError * syn.source.activation
        }
    }

    withGui {
        place(networkComponent, 320, 10, 360, 400)
        place(oc, 670, 10, 415, 415)
        place(plot, 320, 410, 500, 300)
        val plotFrame = getDesktopComponent(plot).parentFrame
        if (plotFrame is JInternalFrame) {
            plotFrame.isClosable = false
        }
        swingInvokeLater {
            oc.getDesktopComponentAs<OdorWorldDesktopComponent>().worldPanel.scalingFactor = 0.1
            plotFrame.setIcon(true)
        }

        // Combined control panel
        createControlPanel("Control Panel", 0, 10) {

            addLabel("Task:")

            val taskComboBox = addComboBox("", tasks, tasks[0]) { selectedTask ->
                cheeseRewardMultiplier = selectedTask.cheeseReward
                poisonRewardMultiplier = selectedTask.poisonReward
                learningRate = 0.05
                gamma = 0.95
            }
            taskComboBox.toolTipText = "Select the learning task: which objects to seek or avoid"

            addSeparator()

            val lrField = addFormattedNumericTextField("Learning Rate", initValue = learningRate) {
                learningRate = it
            }
            lrField.toolTipText = "Step size for weight updates (higher = faster but less stable)"

            val gammaField = addFormattedNumericTextField("Gamma (Discount Factor)", initValue = gamma) {
                gamma = it
            }
            gammaField.toolTipText = "Importance of future rewards (0-1, higher = more farsighted)"

            val speedBiasField = addFormattedNumericTextField("Speed Bias", initValue = straight.bias) {
                straight.bias = it
            }
            speedBiasField.toolTipText = "Base forward speed (higher = faster default movement)"

            val learningCheckbox = addCheckBox("Learning Enabled", learningEnabled) { enabled ->
                learningEnabled = enabled
                // Clamp/unclamp trainable weights
                val allTrainableWeights = actorSynapses + valueNeuron.fanIn
                allTrainableWeights.forEach { syn ->
                    syn.clamped = !enabled
                }
            }
            learningCheckbox.toolTipText = "Enable/disable weight updates (unchecking freezes all weights)"

            val trainSpeedCheckbox = addCheckBox("Train Speed Connections", trainSpeedConnections) { enabled ->
                trainSpeedConnections = enabled
                speedSynapses.forEach { syn ->
                    syn.clamped = !enabled
                }
                setSpeedSynapsesEnabled(enabled)
            }
            trainSpeedCheckbox.toolTipText = "Include sensor-to-speed connections in learning"

            addSeparator()

            // Helper to get decay function summary
            fun getDecaySummary(): String {
                val df = sharedDecayFunction
                return "${df.name} (disp=${df.dispersion.toInt()})"
            }

            // Shared decay function editor
            val decayLabel = addLabel("Decay: ${getDecaySummary()}")
            val decaySelector = DecayFunction.DecayFunctionSelector(sharedDecayFunction.copy() as DecayFunction)
            val decayEditButton = addButton("Edit Decay Function...") {
                decaySelector.decayFunction = sharedDecayFunction.copy() as DecayFunction
                decaySelector.createEditorDialog {
                    updateSharedDecayFunction(decaySelector.decayFunction)
                    syncSensorDecayFunctions()
                    swingInvokeLater { decayLabel.text = "Decay: ${getDecaySummary()}" }
                }.display()
            }
            decayEditButton.toolTipText = "Configure decay function for all sensors and rewards"

            lateinit var weightSpaceButton: JButton

            fun showWeightSpaceFrame() {
                val existingFrame = weightSpaceFrame?.takeIf { it.isDisplayable }
                val frame = existingFrame ?: run {
                    val panel = BraitenbergWeightSpacePanel(
                        listOf(
                            WeightSpace("Cheese", cheeseLeftToLeftTurn, cheeseRightToRightTurn),
                            WeightSpace("Poison", poisonLeftToLeftTurn, poisonRightToRightTurn)
                        )
                    )
                    weightSpacePanel = panel
                    JInternalFrame("Braitenberg Actor Weight Space", true, true, true, true).apply {
                        layout = BorderLayout()
                        add(panel, BorderLayout.CENTER)
                        setBounds(165, 155, 760, 420)
                        defaultCloseOperation = JInternalFrame.DISPOSE_ON_CLOSE
                        addInternalFrameListener(object : InternalFrameAdapter() {
                            override fun internalFrameClosed(e: InternalFrameEvent) {
                                weightSpacePanel = null
                                weightSpaceFrame = null
                                weightSpaceButton.text = "Show Weight Space"
                            }
                        })
                        isVisible = true
                    }.also {
                        weightSpaceFrame = it
                        addInternalFrame(it)
                    }
                }
                frame.isIcon = false
                frame.toFront()
                frame.isSelected = true
                repaintWeightSpacePanel()
                weightSpaceButton.text = "Hide Weight Space"
            }

            fun hideWeightSpaceFrame() {
                weightSpaceFrame?.takeIf { it.isDisplayable }?.let { it.isIcon = true }
                weightSpaceButton.text = "Show Weight Space"
            }

            weightSpaceButton = addButton("Show Weight Space") {
                val frame = weightSpaceFrame?.takeIf { it.isDisplayable }
                if (frame != null && !frame.isIcon) {
                    hideWeightSpaceFrame()
                } else {
                    showWeightSpaceFrame()
                }
            }
            weightSpaceButton.toolTipText = "Open a live display of cheese and poison actor weights"

            val timeSeriesButton = addButton("Show Time Series") {
                if (plotFrame is JInternalFrame && !plotFrame.isIcon) {
                    plotFrame.setIcon(true)
                    this@addButton.text = "Show Time Series"
                } else {
                    plotFrame.setIcon(false)
                    plotFrame.toFront()
                    this@addButton.text = "Hide Time Series"
                }
            }
            timeSeriesButton.toolTipText = "Show reward, value, and TD error time series"

            addSeparator()

            val randomResetButton = addButton("Reset (Random Weights)") {
                network.freeSynapses.forEach { s ->
                    s.strength = s.lowerBound + Math.random() * (s.upperBound - s.lowerBound)
                }
                resetWeightSpaceTrajectories()
            }
            randomResetButton.toolTipText = "Randomize all weights within their current bounds"

            val zeroResetButton = addButton("Reset (0 Weights)") {
                network.freeSynapses.forEach { s ->
                    s.strength = 0.0
                }
                resetWeightSpaceTrajectories()
            }
            zeroResetButton.toolTipText = "Set all weights to zero"

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

    The network consists of:
    - **Input Neurons**: Four sensors detecting cheese and poison objects (left and right for each)
    - **Output Neurons**: Three motor outputs controlling left turn, right turn, and forward speed
    - **Actor Weights**: Connect inputs to motor outputs, determining behavior
    - **Speed Weights**: Optional trainable connections from sensors to speed (for speed modulation)
    - **Critic Network**: Learns to predict value of current state
    - **Reward Neuron**: Displays the total reward signal

    The simulation uses temporal difference (TD) learning:

    1. The agent observes its environment through sensors
    2. The actor selects actions based on current weights
    3. The agent receives rewards based on proximity to objects
    4. The critic computes the TD error (difference between predicted and actual value)
    5. Both actor and critic weights are updated based on TD error

    Rewards are distance-based with configurable decay functions, providing smooth gradients that guide learning.

    The vehicle has four trainable actor connections for turning:
    - Cheese Left sensor → Left Turn
    - Cheese Right sensor → Right Turn
    - Poison Left sensor → Left Turn
    - Poison Right sensor → Right Turn

    Additionally, four trainable speed connections can be enabled:
    - Cheese Left sensor → Speed
    - Cheese Right sensor → Speed
    - Poison Left sensor → Speed
    - Poison Right sensor → Speed

    All weights are updated uniformly at each time step based on the TD error. Positive weights create pursuit behavior (turn toward the object), while negative weights create avoidance behavior (turn away from the object). The speed connections allow the vehicle to learn to slow down or speed up based on what it detects.

    Objects automatically respawn at new locations when the agent collides with them. This prevents the vehicle from getting stuck on objects and encourages continuous exploration. Objects always respect a minimum separation distance of `100` pixels from each other to avoid confusion during learning.

    # What to Do

    1. Select a task from the `Task` dropdown menu in the control panel
    2. Check `Learning Enabled` to allow weight updates
    3. Optionally check `Train Speed Connections` to include speed modulation learning
    4. Click the workspace run button to start learning
    5. Watch the vehicle move around the environment as it learns
    6. Observe the `Reward, Value, TD Error` plot to monitor learning progress
    7. Click `Show Weight Space` to watch the cheese and poison actor weights move through pursuit / avoidance space

    You can stop and examine weights at any time, then continue. Uncheck `Learning Enabled` to freeze weights and watch the vehicle execute its learned policy without further updates.

    **Performance tip**: Training runs much faster if you minimize or iconify component windows (time series, network, odor world). The simulation still runs in the background but doesn't spend time rendering visualizations. Another option is to leave the odor world open since it does not impact performance much and it's fun to watch it learn.

    During training, watch:
    - How the vehicle's movement patterns change over time
    - The `Reward` trace showing how total reward changes
    - The `Value` trace showing the critic's learned predictions
    - The `TD Error` showing the learning signal
    - The network weights updating in real-time
    - The `Show Weight Space` display, where each point shows the current left and right turn weights for cheese or poison. Points above the anti-diagonal indicate mostly pursuing behavior; points below it indicate mostly avoiding behavior.

    You can adjust parameters to see their effects:

    - `Learning Rate`: Controls how quickly weights change (higher = faster but less stable learning)
    - `Gamma`: Discount factor for future rewards (higher = more farsighted)
    - `Dispersion`: Sensor range (higher = wider detection)
    - `Speed Bias`: Base forward speed (higher = faster default movement)

    ## Reward Configuration

    The proximity rewards use configurable decay functions. Each object type (cheese and poison) has independent settings:

    - `Max Reward`: Maximum reward magnitude (default: `15.0`)
    - `Decay Function`: How reward diminishes with distance
      - `Gaussian`: Smooth bell-curve decay (recommended for natural behavior)
      - `Linear`: Linear interpolation to zero at dispersion radius
      - `Step`: Binary on/off at dispersion radius
    - `Dispersion`: Distance at which reward approaches zero
    - `Peak Distance`: Distance where reward is maximum (usually `0`)

    The actual reward is: `maxReward × decayFunction(distance) × taskMultiplier`

    Task multipliers determine whether objects are rewarding (`+1`) or punishing (`-1`).

    After training on one task, try clicking `Reset` and training on a different task to see how the vehicle adapts.

    ## Interpreting the Graphs

    The time series plot shows three key signals that reveal the learning process:

    `Reward`: Shows the immediate reward signal based on proximity to objects. Positive values indicate being near rewarding objects (cheese in most tasks), negative values indicate being near penalizing objects (poison in most tasks). This signal fluctuates continuously as the agent moves around.

    `Value`: The critic's prediction of expected future cumulative reward from the current state. Early in training this is inaccurate, but it improves over time. You should see value increase as the agent approaches rewarding objects and decrease near penalizing ones, reflecting learned predictions about what will happen next.

    `TD Error`: The temporal difference error, which can be understood as: `TD_error = reward + γ×V(current) - V(previous)`. This is the learning signal that drives all weight updates. It represents the difference between what the critic predicted at the previous time step and what actually happened (current reward plus discounted current value). When value estimates are low or changing slowly, TD error tracks closely with reward. As the critic learns better predictions, TD error reflects prediction errors rather than just reward magnitude.

    ## Reading the Learning Process

    What to expect during training:
    - **Early**: All three traces are noisy and uncorrelated as the agent explores randomly. Value stays near zero because the critic hasn't learned anything yet, so TD error closely tracks reward.
    - **Mid training**: Reward and TD error begin to track each other more closely as value predictions improve but still have room to grow.
    - **Late training**: Reward and TD error converge toward similar values as value becomes smoother and more accurate. The small difference between them reflects how well the critic is predicting future outcomes.

    ## Common Questions

    **Why don't reward and TD error go to zero?**
    Unlike discrete episodic tasks, this is a continuous environment where the agent is always moving. The reward signal varies based on distance to objects, so it naturally fluctuates between positive and negative values depending on proximity. TD error tracks these fluctuations.

    **Why do reward and TD error converge toward each other?**
    The TD error formula is: `TD_error = r + γ×V(current) - V(previous)`. When the critic is well-trained, the value function changes smoothly as the agent moves, making the difference between `V(current)` and `V(previous)` small. This means `TD_error ≈ r`, so the two traces track each other closely. Early in training when value is near zero, TD error also approximates reward, but for a different reason: both value terms are near zero.

    **What causes the sharp spikes in TD error when objects respawn?**
    - Downward spikes occur when a rewarding object disappears. This is an unexpected negative change: at the previous step the agent was close to something good, and at the current step it's far away.
    - Upward spikes occur when a penalizing object (poison) disappears. This is an unexpected positive change: at the previous step the agent was close to something bad, and at the current step it's far away.

    These spikes represent genuine prediction errors: the critic didn't predict the object would suddenly teleport! However, these spikes don't disrupt learning much because weight updates are proportional to both TD error AND input activation: `Δw = learningRate × tdError × activation`. When objects respawn far away, sensor activations are near zero, so even though TD error is large, the weight changes are minimal. The algorithm only learns strongly when sensory signals are present, which is exactly what we want.

    # References

    Braitenberg, V. (1986). [_Vehicles: Experiments in synthetic psychology_](https://mitpress.mit.edu/9780262521123/vehicles/). MIT Press.

    Hotton, S., & Yoshimi, J. (2024). [_The Open Dynamics of Braitenberg Vehicles_](https://mitpress.mit.edu/9780262548199/the-open-dynamics-of-braitenberg-vehicles/). MIT Press.

    Sutton, R. S., & Barto, A. G. (2018). [_Reinforcement Learning: An Introduction_](http://incompleteideas.net/book/the-book.html) (2nd ed.). MIT Press.

    # Credits

    Dave Noelle

    Yulin Li

    Veer Sahai

    [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

    """.trimIndent()
    )

    /*
     * Headless mode documentation
     *
     * SAMPLE TERMINAL COMMANDS:
     *
     * Basic run with 10000 iterations:
     *   ./gradlew runSim -PsimName="Braitenberg RL" -PoptionString='{"maxIterations": 10000, "csvOutput": {}}'
     *
     * Task switching mid-training:
     *   ./gradlew runSim -PsimName="Braitenberg RL" -PoptionString='{"taskIndex": 0, "maxIterations": 20000, "csvOutput": {}, "parameterSchedule": [{"atIteration": 5000, "set": {"taskIndex": 1}}]}'
     *
     * PARAMETERS:
     * - taskIndex: 0=Seek Cheese/Avoid Poison, 1=Seek Both, 2=Avoid Both, 3=Seek Poison/Avoid Cheese
     * - learningRate: Learning rate (default: 0.05)
     * - gamma: Discount factor (default: 0.95)
     * - trainSpeedConnections: Whether to train sensor-to-speed connections
     * - decayFunction: Shared decay function for all sensors and rewards {type, dispersion, peakDistance}
     * - cheeseReward: {maxReward} - maximum cheese reward magnitude
     * - poisonReward: {maxReward} - maximum poison reward magnitude
     * - decayFunction types: "Gaussian", "Linear", "Step"
     * - maxIterations: Number of iterations to run before stopping (required for headless mode)
     * - csvOutput: Enable CSV output of simulation data. Use {} for auto-generated filename,
     *              or {"filePath": "path/to/file.csv"} for custom path
     * - parameterSchedule: List of parameter changes triggered at specific iterations
     *     - atIteration: Iteration number to trigger the change
     *     - set: Object with parameters to change (taskIndex, learningRate, gamma, learningEnabled)
     *
     * CSV OUTPUT COLUMNS:
     *   iteration, taskIndex, learningRate, gamma,
     *   cheeseLeftInput, cheeseRightInput, poisonLeftInput, poisonRightInput,
     *   straight, leftTurn, rightTurn, reward, value, tdError,
     *   actorW_cheeseL_leftTurn, actorW_cheeseR_rightTurn, actorW_poisonL_leftTurn, actorW_poisonR_rightTurn,
     *   speedW_cheeseL, speedW_cheeseR, speedW_poisonL, speedW_poisonR,
     *   criticW_cheeseL, criticW_cheeseR, criticW_poisonL, criticW_poisonR,
     *   agentX, agentY, agentHeading, cheeseX, cheeseY, poisonX, poisonY
     */

    // Helper function to parse decay function from JSON
    fun parseDecayFunction(config: JSONObject): DecayFunction {
        val type = config.optString("type", "Gaussian")
        val dispersion = config.optDouble("dispersion", 150.0)
        val peakDistance = config.optDouble("peakDistance", 0.0)

        return when (type.lowercase()) {
            "linear" -> LinearDecayFunction(dispersion).apply { this.peakDistance = peakDistance }
            "step" -> StepDecayFunction(dispersion).apply { this.peakDistance = peakDistance }
            else -> GaussianDecayFunction(dispersion).apply { this.peakDistance = peakDistance }
        }
    }

    // Parse optionString for headless mode parameters
    if (optionString?.isNotEmpty() == true) {
        val options = JSONObject(optionString)

        // Parse parameters
        learningRate = options.optDouble("learningRate", learningRate)
        gamma = options.optDouble("gamma", gamma)
        trainSpeedConnections = options.optBoolean("trainSpeedConnections", trainSpeedConnections)

        // Shared decay function (applies to all sensors and rewards)
        options.optJSONObject("decayFunction")?.let { decayConfig ->
            updateSharedDecayFunction(parseDecayFunction(decayConfig))
            syncSensorDecayFunctions()
        }

        // Cheese reward config (only maxReward, decay function is shared)
        options.optJSONObject("cheeseReward")?.let { cheeseConfig ->
            cheeseRewardConfig.maxReward = cheeseConfig.optDouble("maxReward", cheeseRewardConfig.maxReward)
        }

        // Poison reward config (only maxReward, decay function is shared)
        options.optJSONObject("poisonReward")?.let { poisonConfig ->
            poisonRewardConfig.maxReward = poisonConfig.optDouble("maxReward", poisonRewardConfig.maxReward)
        }

        // Set task based on taskIndex
        if (options.has("taskIndex")) {
            val taskIndex = options.getInt("taskIndex")
            if (taskIndex in tasks.indices) {
                currentTaskIndex = taskIndex
                val task = tasks[currentTaskIndex]
                cheeseRewardMultiplier = task.cheeseReward
                poisonRewardMultiplier = task.poisonReward
            }
        }

        // Parse maxIterations
        if (options.has("maxIterations")) {
            maxIterations = options.getInt("maxIterations")
            println("Max iterations set to: $maxIterations")
        }

        // Parse CSV output configuration
        if (options.has("csvOutput")) {
            csvData = StringBuilder()
            options.optJSONObject("csvOutput")?.let { csvConfig ->
                if (csvConfig.has("filePath")) {
                    csvFilePath = csvConfig.getString("filePath")
                }
            }
            println("CSV output enabled" + (csvFilePath?.let { ": $it" } ?: " (auto-generated filename)"))
        }

        // Parse parameter schedule
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

        // Run the simulation in headless mode if maxIterations is set
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
