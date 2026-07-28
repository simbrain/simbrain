/**
 * A visual, steering-only implementation of the fitted thermotaxis circuit in
 * Ikeda, Matsumoto, and Izquierdo (2021).
 */
package org.simbrain.custom_sims.simulations.neuroscience

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.piccolo2d.PLayer
import org.piccolo2d.PNode
import org.piccolo2d.util.PPaintContext
import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapseAsync
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.showAPEOptionDialog
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.fitWorldToFrameSize
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.geom.Line2D
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import kotlin.math.*
import kotlin.random.Random

val nematodeThermotaxis = newSim { optionString ->
    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Thermotaxis Circuit")
    val network = networkComponent.network.apply { timeStep = 0.1 }

    suspend fun neuron(label: String, x: Double, y: Double) = network.addNeuron {
        this.label = label
        location = point(x, y)
        updateRule = LinearRule()
    }

    val afd = neuron("AFD (Temperature)", 190.0, 400.0)
    val aib = neuron("AIB", 290.0, 320.0)
    val aiy = neuron("AIY", 90.0, 320.0)
    val aiz = neuron("AIZ", 190.0, 230.0)
    val dmn = neuron("DMN (Output)", 280.0, 135.0)
    val vmn = neuron("VMN (Output)", 100.0, 135.0)
    val cpg = neuron("CPG", 190.0, 45.0)

    fun addConnection(source: org.simbrain.network.core.Neuron, target: org.simbrain.network.core.Neuron, weight: Double) =
        network.addSynapseAsync(source, target) {
            strength = weight
            lowerBound = -15.0
            upperBound = 15.0
        }

    val afdToAibGap = addConnection(afd, aib, 2.98878639079434)
    val aibToAfdGap = addConnection(aib, afd, 2.98878639079434)
    val afdToAiy = addConnection(afd, aiy, -6.86431476903374)
    val aibToAiy = addConnection(aib, aiy, -3.22231085979830)
    val aibToDmn = addConnection(aib, dmn, -6.54395366094551)
    val aiyToAiz = addConnection(aiy, aiz, 4.60507293122372)
    val aizToAib = addConnection(aiz, aib, -6.18485677383669)
    val aizToDmn = addConnection(aiz, dmn, 12.5176985752213)
    val aizToVmn = addConnection(aiz, vmn, -14.1246289219806)
    val dmnToDmn = addConnection(dmn, dmn, 4.87761445986278)
    val dmnToVmn = addConnection(dmn, vmn, -1.70971858601307)
    val vmnToDmn = addConnection(vmn, dmn, 7.23275421563747)
    val vmnToVmn = addConnection(vmn, vmn, -5.06198070709775)
    val cpgToDmn = addConnection(cpg, dmn, 12.5352695337539)
    val cpgToVmn = addConnection(cpg, vmn, -12.5352695337539)

    val worldComponent = addOdorWorldComponent("Thermal Plate")
    val world = worldComponent.world.apply {
        wrapAround = false
        isObjectsBlockMovement = false
        isUseCameraCentering = false
        tileMap.updateMapSize(34, 24)
        tileMap.fill("water_1")
    }
    val worm = world.addEntity(world.width / 2.0, world.height / 2.0, EntityType.Nematode).apply {
        name = "Model worm"
        heading = 0.0
        distancePerAnimationFrame = 0.35
        isShowSensorsAndEffectors = false
        isShowTrail = true
    }

    val model = ThermotaxisModel(
        states = DoubleArray(5),
        biases = doubleArrayOf(
            0.261331049344628,
            -9.94979936474547,
            -11.8836526406511,
            -0.243075226129511,
            4.21550001866696
        )
    )
    if (optionString == "validate") {
        println(ThermotaxisAfdValidation.run().summary())
    }
    if (optionString == "turn-assay") {
        val result = requireNotNull(ThermotaxisPopulationSimulation.run(worms = 12, seconds = 120, seed = 2021))
        println(
            "Population simulation: ${result.trajectories} worms, ${result.durationSeconds.toInt()} s each; " +
                "mean final x = ${"%.2f".format(result.meanEndpointX)}, warm half = ${"%.1f".format(result.warmSideFraction * 100)}%"
        )
    }
    var gradientDirection = 1.0
    var temperatureOffset = 0.0
    var useEmpiricalTurns = true
    var turnTime = 0.0
    var remainingTurnSteps = 0
    var turnStepX = 0.0
    var turnStepY = 0.0
    var turnRandom = Random(Random.nextInt())
    val activeTurnLabel = AtomicReference<String?>(null)
    val circuitNeurons = listOf(afd, aib, aiy, aiz, dmn, vmn, cpg)

    fun connectionWeight(synapse: org.simbrain.network.core.Synapse) =
        if (synapse in network.flatSynapseList) synapse.strength else 0.0

    fun currentWeights(): ThermotaxisWeights {
        val gapConductance = minOf(connectionWeight(afdToAibGap), connectionWeight(aibToAfdGap))
        return ThermotaxisWeights(
            afdToAibGap = gapConductance,
            afdToAiy = connectionWeight(afdToAiy),
            aibToAiy = connectionWeight(aibToAiy),
            aibToDmn = connectionWeight(aibToDmn),
            aiyToAiz = connectionWeight(aiyToAiz),
            aizToAib = connectionWeight(aizToAib),
            aizToDmn = connectionWeight(aizToDmn),
            aizToVmn = connectionWeight(aizToVmn),
            dmnToDmn = connectionWeight(dmnToDmn),
            dmnToVmn = connectionWeight(dmnToVmn),
            vmnToDmn = connectionWeight(vmnToDmn),
            vmnToVmn = connectionWeight(vmnToVmn),
            cpgToDmn = connectionWeight(cpgToDmn),
            cpgToVmn = connectionWeight(cpgToVmn)
        )
    }

    fun resetModel(heading: Double = 90.0) {
        model.reset()
        worm.location = point(world.width / 2.0, world.height / 2.0)
        worm.heading = heading
        worm.resetAnimation()
        listOf(afd, aib, aiy, aiz, dmn, vmn, cpg).forEach { it.activation = 0.0 }
        turnTime = 0.0
        remainingTurnSteps = 0
        turnRandom = Random(Random.nextInt())
        activeTurnLabel.set(null)
    }

    resetModel()

    network.updateManager.clear()
    workspace.addUpdateAction("Thermotaxis steering", position = 0) {
        repeat(5) {
            val plateX = 136.0 * (worm.x / world.width - 0.5)
            val temperature = 17.0 + temperatureOffset + gradientDirection * 3.0 * plateX / 68.0
            val result = model.step(
                temperature = temperature,
                weights = currentWeights(),
                activityOverrides = circuitNeurons.map { neuron ->
                    when {
                        neuron !in network.flatNeuronList -> 0.0
                        neuron.clamped -> neuron.activation
                        else -> null
                    }
                }
            )

            afd.activation = result.afdState
            aib.activation = result.outputs[0]
            aiy.activation = result.outputs[1]
            aiz.activation = result.outputs[2]
            dmn.activation = result.outputs[3]
            vmn.activation = result.outputs[4]
            cpg.activation = result.cpgOutput

            var heading = worm.heading * PI / 180.0
            val plateXPosition = 136.0 * worm.x / world.width
            val turn = if (useEmpiricalTurns && remainingTurnSteps == 0) {
                ThermotaxisTurnPolicy.select(temperature, turnTime, heading, turnRandom, gradientDirection)
            } else {
                null
            }
            if (turn != null) {
                heading = turn.heading
                val duration = turn.durationSeconds.coerceAtLeast(network.timeStep)
                turnStepX = network.timeStep * turn.displacement * cos(heading) / duration * world.width / 136.0
                turnStepY = -network.timeStep * turn.displacement * sin(heading) / duration * world.height / 96.0
                remainingTurnSteps = (duration / network.timeStep).roundToInt()
                activeTurnLabel.set(turn.label)
            }
            val isTurning = useEmpiricalTurns && remainingTurnSteps > 0
            val stepDistance: Double
            var nextX: Double
            var nextY: Double
            if (isTurning) {
                nextX = worm.x + turnStepX
                nextY = worm.y + turnStepY
                stepDistance = hypot(turnStepX, turnStepY)
                remainingTurnSteps--
            } else {
                activeTurnLabel.set(null)
                heading += result.curvature * network.timeStep
                stepDistance = 0.2 * network.timeStep * world.width / 136.0
                nextX = worm.x + stepDistance * cos(heading)
                nextY = worm.y - stepDistance * sin(heading)
            }
            val edgeMargin = 12.0
            if (nextX < edgeMargin || nextX > world.width - edgeMargin) {
                heading = PI - heading
                nextX = nextX.coerceIn(edgeMargin, world.width - edgeMargin)
            }
            if (nextY < edgeMargin || nextY > world.height - edgeMargin) {
                heading = -heading
                nextY = nextY.coerceIn(edgeMargin, world.height - edgeMargin)
            }
            worm.heading = heading * 180.0 / PI
            worm.location = point(nextX, nextY)
            worm.recordTravelDistance(stepDistance)
            turnTime += network.timeStep
        }
    }

    val gradientOverlay = object : PNode() {
        override fun paint(paintContext: PPaintContext) {
            val graphics = paintContext.graphics as Graphics2D
            val bandWidth = world.width / 20.0
            repeat(20) { index ->
                val directionFraction = if (gradientDirection > 0.0) index / 19.0f else 1.0f - index / 19.0f
                val fraction = (directionFraction + (temperatureOffset / 6.0).toFloat()).coerceIn(0.0f, 1.0f)
                graphics.color = Color(0.05f + 0.9f * fraction, 0.2f, 0.95f - 0.9f * fraction, 0.48f)
                graphics.fillRect((index * bandWidth).toInt(), 0, bandWidth.toInt() + 1, world.height.toInt())
            }
            graphics.color = Color.WHITE
            val leftTemperature = if (gradientDirection > 0.0) 14.0 + temperatureOffset else 20.0 + temperatureOffset
            val rightTemperature = if (gradientDirection > 0.0) 20.0 + temperatureOffset else 14.0 + temperatureOffset
            val leftLabel = "${"%.1f".format(leftTemperature)}°C"
            val rightLabel = "${"%.1f".format(rightTemperature)}°C"
            graphics.drawString(leftLabel, 8, 20)
            graphics.drawString(rightLabel, (world.width - 40).toInt(), 20)
            activeTurnLabel.get()?.let { turnLabel ->
                val label = "TURN: $turnLabel"
                val labelWidth = graphics.fontMetrics.stringWidth(label) + 16
                val labelX = ((world.width - labelWidth) / 2).toInt()
                val labelY = (world.height - 30).toInt()
                graphics.color = Color(0, 0, 0, 170)
                graphics.fillRoundRect(labelX, labelY, labelWidth, 22, 8, 8)
                graphics.color = Color.WHITE
                graphics.drawString(label, labelX + 8, labelY + 16)
            }
        }
    }.apply {
        pickable = false
        setBounds(0.0, 0.0, world.width, world.height)
    }

    withGui {
        val networkWidth = 385
        val networkHeight = 447
        val odorWorldDesktopComponent = worldComponent.getDesktopComponentAs<OdorWorldDesktopComponent>()

        fun repaintThermalPlate() {
            gradientOverlay.invalidatePaint()
            odorWorldDesktopComponent.worldPanel.canvas.repaint()
        }

        var ensembleFrame: GenericJInternalFrame? = null
        var ensembleFrameX = 100

        fun showEnsembleTrajectories(result: ThermotaxisEnsembleResult) {
            ensembleFrame?.dispose()
            ensembleFrame = GenericJInternalFrame("Thermotaxis ensemble trajectories", true, true, true, true).apply {
                layout = BorderLayout()
                add(ThermotaxisEnsemblePanel(result), BorderLayout.CENTER)
                setBounds(ensembleFrameX, 60, 680, 520)
                defaultCloseOperation = JInternalFrame.DISPOSE_ON_CLOSE
                isVisible = true
            }
            addInternalFrame(ensembleFrame)
            ensembleFrame?.toFront()
            ensembleFrame?.isSelected = true
        }

        val controlPanel = createControlPanel("Thermotaxis Controls", 10, 10) {
            addButton("Reset up") { resetModel() }
            addButton("Reset left") { resetModel(180.0) }
            addButton("Reset right") { resetModel(0.0) }
            addButton("Reverse gradient", context = Dispatchers.Swing) {
                gradientDirection *= -1.0
                repaintThermalPlate()
            }
            suspend fun updateTemperatureOffset(delta: Double) = withContext(Dispatchers.Swing) {
                temperatureOffset += delta
                repaintThermalPlate()
            }
            addButton("Warm plate") { updateTemperatureOffset(0.5) }
            addButton("Cool plate") { updateTemperatureOffset(-0.5) }
            addCheckBox("Show trail", true) { showTrail -> worm.isShowTrail = showTrail }
            addCheckBox("Use empirical turns", true) { enabled ->
                useEmpiricalTurns = enabled
                if (!enabled) remainingTurnSteps = 0
            }
            addButton("Run population simulation") {
                val validationButton = this
                val options = ThermotaxisPopulationSimulationOptions().showAPEOptionDialog("Population Simulation Options") ?: return@addButton
                val trajectories = options.worms
                val seconds = options.seconds
                val cancelRequested = AtomicBoolean(false)
                val progressWindow = withContext(Dispatchers.Swing) {
                    validationButton.isEnabled = false
                    ProgressWindow(trajectories, "Population simulation: preparing $trajectories worms").apply progressWindow@{
                        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
                        val cancelButton = JButton("Cancel").apply {
                            addActionListener {
                                cancelRequested.set(true)
                                isEnabled = false
                                text = "Cancelling..."
                                this@progressWindow.text = "Cancelling after current trajectory..."
                            }
                        }
                        add(JPanel(BorderLayout()).apply {
                            border = BorderFactory.createEmptyBorder(0, 10, 10, 10)
                            add(cancelButton, BorderLayout.CENTER)
                        }, BorderLayout.SOUTH)
                        addWindowListener(object : WindowAdapter() {
                            override fun windowClosing(event: WindowEvent) {
                                cancelRequested.set(true)
                            }
                        })
                        pack()
                        setLocationRelativeTo(null)
                    }
                }
                try {
                val assayWeights = currentWeights()
                val assayGradientDirection = gradientDirection
                val assayTemperatureOffset = temperatureOffset
                val result = ThermotaxisPopulationSimulation.run(
                    worms = trajectories,
                    seconds = seconds,
                    weights = assayWeights,
                    gradientDirection = assayGradientDirection,
                    temperatureOffset = assayTemperatureOffset,
                    onProgress = { completed, total ->
                        SwingUtilities.invokeLater {
                            progressWindow.value = completed
                            progressWindow.text = "Population simulation: $completed of $total worms complete"
                        }
                    },
                    shouldCancel = cancelRequested::get
                )
                    withContext(Dispatchers.Swing) {
                        progressWindow.close()
                        if (result != null) {
                            showEnsembleTrajectories(result)
                        }
                    }
                } finally {
                    withContext(Dispatchers.Swing) {
                        validationButton.isEnabled = true
                        if (progressWindow.isDisplayable) {
                            progressWindow.close()
                        }
                    }
                }
            }
        }.awaitLayout()
        controlPanel.setLocation(10, 10)
        val networkX = controlPanel.rightEdgeWithGap()
        val worldX = networkX + networkWidth + SIM_WINDOW_GAP
        ensembleFrameX = worldX - 30
        place(networkComponent, networkX, 10, networkWidth, networkHeight)
        place(worldComponent, worldX, 10, 621, networkHeight)
        odorWorldDesktopComponent.apply {
            val overlayLayer = PLayer().apply { addChild(gradientOverlay) }
            worldPanel.canvas.camera.addLayer(1, overlayLayer)
            fitWorldToFrameSize()
            gradientOverlay.invalidatePaint()
            worldPanel.canvas.repaint()
        }
        resetModel()
    }

    addSidebarInfo(
        """
        # Thermotaxis in Nematodes

        This simulation illustrates thermotaxis—movement guided by temperature—in *Caenorhabditis elegans*. Here, “worm,” “nematode,” and *C. elegans* all refer to this small roundworm. Across a population and enough simulated time, worms should tend to migrate toward the warmer side of the 14°C–20°C plate. Their paths and turns are stochastic, however, so this tendency is not immediate and will not be obvious in every individual run. However using the population simulation it is easier to confirm this behavior, though migration is still statistical and may not always be observed.

        # What to Do

        1. Click `Run` and watch the worm move. With `Use empirical turns` enabled, it changes direction stochastically as well as steering continuously; an individual trail may wander even though a population should tend toward warmth over time.
        2. Click `Run population simulation` to view many trajectories together. The defaults are 12 worms for 1500 seconds; use more worms or longer runs for a clearer aggregate tendency. Reverse the gradient before starting a population simulation to see the warm-directed tendency reverse sides.
        3. Turn off `Use empirical turns` to isolate the steering circuit. The clearest signature is straighter movement in hotter regions and tighter, more circling movement in colder regions, rather than reliable migration in one short run.
        4. Use `Reverse gradient`, `Warm plate`, and `Cool plate` to change the visible thermal environment. You can also clamp or edit circuit neurons and synapses to explore their effects.

        # Details of This Simulation

        ## Evolutionary Search

        The original study used an evolutionary algorithm to search for circuit parameters that reproduced observed thermotaxis and steering behavior. Each search evolved a population of 96 parameter sets for 300 generations and retained its best-performing individual. This simulation is one such evolved parameter set.

        ## Circuit Guide

        - **AFD (Temperature)** is the thermosensory neuron. Its filtered response represents recent temperature history; the AFD–AIY link is chemical.
        - **AIB, AIY, and AIZ** are interneurons that relay and transform the AFD signal within the steering circuit.
        - **CPG** is a central pattern generator: an oscillatory input that supplies opposite rhythmic drive to the motor neurons, producing small dorsal–ventral wiggles that sample the temperature gradient.
        - **DMN (Output)** and **VMN (Output)** are dorsal and ventral neck motor neurons. Their activity difference determines instantaneous path curvature.

        The AFD–AIB electrical gap junction is implemented as reciprocal synapses so it can be shown and edited in Simbrain. Its current is `conductance × (AFD activity − AIB state)`.

        ## Empirical Turns

        In addition to ordinary circuit-generated steering, the simulation can use measured abrupt turn events: **omega turns** are tight, loop-like reorientations; **reversals** move the worm backward; **reversal turns** combine a reversal with reorientation; and **shallow turns** are gentler heading changes. Their frequency, exit direction, duration, and displacement are fixed behavioral measurements from the paper, not evolved circuit behaviors.

        # References

        Ikeda, M., Matsumoto, H., & Izquierdo, E. J. (2021). [Persistent thermal input controls steering behavior in Caenorhabditis elegans](https://doi.org/10.1371/journal.pcbi.1007916). _PLoS Computational Biology, 17_(1), e1007916.
        """.trimIndent()
    )
}

internal class ThermotaxisModel(
    private val states: DoubleArray,
    private val biases: DoubleArray
) {
    private val dt = 0.1
    private val temperatureHistory = DoubleArray(1000) { 17.0 }
    private var time = 0.0

    fun reset() {
        states.fill(0.0)
        temperatureHistory.fill(17.0)
        time = 0.0
    }

    fun step(
        temperature: Double,
        weights: ThermotaxisWeights = ThermotaxisWeights(),
        activityOverrides: List<Double?> = List(7) { null }
    ): ThermotaxisStep {
        require(activityOverrides.size == 7) { "Expected one override for each circuit component" }
        temperatureHistory.copyInto(temperatureHistory, 0, 1)
        temperatureHistory[temperatureHistory.lastIndex] = temperature
        val sensedAfdState = temperatureHistory.indices.sumOf { index ->
            val lag = (temperatureHistory.lastIndex - index) * dt
            responseKernel(lag) * thresholdResponse(temperatureHistory[index]) * dt
        }
        val afdState = activityOverrides[0] ?: sensedAfdState
        fun output(index: Int): Double {
            return activityOverrides[index + 1] ?: sigmoid(states[index] + biases[index])
        }
        val outputs = states.indices.map(::output).toDoubleArray()
        val inputs = doubleArrayOf(
            weights.afdToAibGap * (afdState - states[0]) + weights.aizToAib * outputs[2],
            weights.afdToAiy * sigmoid(afdState - 12.6471320830562) + weights.aibToAiy * outputs[0],
            weights.aiyToAiz * outputs[1],
            weights.aibToDmn * outputs[0] + weights.aizToDmn * outputs[2] + weights.dmnToDmn * outputs[3] + weights.vmnToDmn * outputs[4],
            weights.aizToVmn * outputs[2] + weights.dmnToVmn * outputs[3] + weights.vmnToVmn * outputs[4]
        )
        val oscillator = activityOverrides[6] ?: sin(2.0 * PI * time / 4.2)
        inputs[3] += weights.cpgToDmn * oscillator
        inputs[4] += weights.cpgToVmn * oscillator
        states.indices.forEach { index ->
            val override = activityOverrides[index + 1]
            states[index] = if (override == null) {
                states[index] + dt * (inputs[index] - states[index])
            } else {
                logit(override)
            }
        }
        val updatedOutputs = states.indices.map(::output).toDoubleArray()
        time += dt
        val neuromuscularWeight = 13.1309355602490 * PI / 180.0
        return ThermotaxisStep(afdState, updatedOutputs, oscillator, neuromuscularWeight * (updatedOutputs[3] - updatedOutputs[4]))
    }

    private fun thresholdResponse(temperature: Double): Double = if (temperature < 14.4248659160223) {
        0.0
    } else {
        val difference = temperature - 14.4248659160223
        difference.pow(9.56568480432967) / (56.1383589609848 + difference.pow(9.56568480432967))
    }

    private fun responseKernel(lag: Double): Double = exp(-0.434236245297562 * lag) *
        (1.437310682312189 - 0.434236245297562 * 0.342912724081394 * lag)

    private fun sigmoid(value: Double): Double = 1.0 / (1.0 + exp(-value))

    private fun logit(value: Double): Double {
        val boundedValue = value.coerceIn(1e-9, 1.0 - 1e-9)
        return ln(boundedValue / (1.0 - boundedValue))
    }
}

internal data class ThermotaxisWeights(
    val afdToAibGap: Double = 2.98878639079434,
    val afdToAiy: Double = -6.86431476903374,
    val aibToAiy: Double = -3.22231085979830,
    val aibToDmn: Double = -6.54395366094551,
    val aiyToAiz: Double = 4.60507293122372,
    val aizToAib: Double = -6.18485677383669,
    val aizToDmn: Double = 12.5176985752213,
    val aizToVmn: Double = -14.1246289219806,
    val dmnToDmn: Double = 4.87761445986278,
    val dmnToVmn: Double = -1.70971858601307,
    val vmnToDmn: Double = 7.23275421563747,
    val vmnToVmn: Double = -5.06198070709775,
    val cpgToDmn: Double = 12.5352695337539,
    val cpgToVmn: Double = -12.5352695337539
)

internal data class ThermotaxisStep(val afdState: Double, val outputs: DoubleArray, val cpgOutput: Double, val curvature: Double)

internal class ThermotaxisPopulationSimulationOptions : EditableObject {

    var worms by GuiEditable(
        initValue = 12,
        min = 1,
        description = "Number of independently simulated worms. The paper used 100.",
        order = 10
    )

    var seconds by GuiEditable(
        initValue = 1500,
        min = 1,
        description = "Simulated duration per worm in seconds. The paper used 1800 seconds (30 minutes).",
        order = 20
    )
}

internal object ThermotaxisEnsemble {

    private const val plateWidth = 136.0
    private const val plateHeight = 96.0
    private const val edgeMargin = 12.0
    private const val thermalGradient = 3.0
    private const val timeStep = 0.1
    private const val substepsPerTrajectory = 6_000

    fun run(
        trajectories: Int = 96,
        substeps: Int = substepsPerTrajectory,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
        shouldCancel: () -> Boolean = { false }
    ): ThermotaxisEnsembleResult? {
        require(trajectories > 0) { "At least one trajectory is required" }
        require(substeps > 0) { "At least one time step is required" }
        val paths = mutableListOf<ThermotaxisPath>()
        repeat(trajectories) { index ->
            if (shouldCancel()) return null
            paths += runTrajectory(2.0 * PI * index / trajectories, substeps)
            onProgress(index + 1, trajectories)
        }
        val endpoints = paths.map { it.points.last().x }
        val meanEndpointX = endpoints.average()
        val warmSideFraction = endpoints.count { it > plateWidth / 2.0 }.toDouble() / trajectories
        return ThermotaxisEnsembleResult(trajectories, substeps * timeStep, meanEndpointX, warmSideFraction, paths)
    }

    private fun runTrajectory(initialHeading: Double, substeps: Int): ThermotaxisPath {
        val model = ThermotaxisModel(
            states = DoubleArray(5),
            biases = doubleArrayOf(0.261331049344628, -9.94979936474547, -11.8836526406511, -0.243075226129511, 4.21550001866696)
        )
        var x = plateWidth / 2.0
        var y = plateHeight / 2.0
        var heading = initialHeading
        val points = mutableListOf(ThermotaxisPosition(x, y))
        repeat(substeps) { step ->
            val temperature = 17.0 + thermalGradient * (x / plateWidth - 0.5)
            val curvature = model.step(temperature).curvature
            heading += curvature * timeStep
            val stepDistance = 0.2 * timeStep * plateWidth / 136.0
            var nextX = x + stepDistance * cos(heading)
            var nextY = y - stepDistance * sin(heading)
            if (nextX < edgeMargin || nextX > plateWidth - edgeMargin) {
                heading = PI - heading
                nextX = nextX.coerceIn(edgeMargin, plateWidth - edgeMargin)
            }
            if (nextY < edgeMargin || nextY > plateHeight - edgeMargin) {
                heading = -heading
                nextY = nextY.coerceIn(edgeMargin, plateHeight - edgeMargin)
            }
            x = nextX
            y = nextY
            if ((step + 1) % 10 == 0 || step == substeps - 1) {
                points += ThermotaxisPosition(x, y)
            }
        }
        return ThermotaxisPath(points)
    }
}

internal data class ThermotaxisEnsembleResult(
    val trajectories: Int,
    val durationSeconds: Double,
    val meanEndpointX: Double,
    val warmSideFraction: Double,
    val paths: List<ThermotaxisPath>,
    val gradientDirection: Double = 1.0,
    val temperatureOffset: Double = 0.0
)

internal data class ThermotaxisPosition(val x: Double, val y: Double)

internal data class ThermotaxisPath(val points: List<ThermotaxisPosition>)

internal object ThermotaxisAfdValidation {

    fun run(): ThermotaxisAfdValidationResult {
        val lowAfdBias = meanSteeringBias(0.0)
        val highAfdBias = meanSteeringBias(2.0)
        return ThermotaxisAfdValidationResult(lowAfdBias, highAfdBias)
    }

    private fun meanSteeringBias(afdValue: Double): Double {
        val model = ThermotaxisModel(
            states = DoubleArray(5),
            biases = doubleArrayOf(0.261331049344628, -9.94979936474547, -11.8836526406511, -0.243075226129511, 4.21550001866696)
        )
        return (1..1_000)
            .map { model.step(temperature = 17.0, activityOverrides = afdOverride(afdValue)).curvature }
            .drop(200)
            .average()
            .let(::abs)
    }

    private fun afdOverride(value: Double) = MutableList<Double?>(7) { null }.apply { this[0] = value }
}

internal data class ThermotaxisAfdValidationResult(
    val lowAfdSteeringBias: Double,
    val highAfdSteeringBias: Double
) {
    val passes: Boolean get() = highAfdSteeringBias < lowAfdSteeringBias

    fun summary(): String = """
        AFD steering-response validation

        Steering bias at low fixed AFD: ${"%.4f".format(lowAfdSteeringBias)} rad/s
        Steering bias at high fixed AFD: ${"%.4f".format(highAfdSteeringBias)} rad/s
        Result: ${if (passes) "PASS — higher AFD activity produces a straighter path." else "FAIL — higher AFD activity did not reduce steering bias."}

        This reproduces Figure 6's circuit-level result in the fitted model. It does not reproduce the paper's full population migration assay, which also used empirical turning behavior.
    """.trimIndent()
}

internal class ThermotaxisEnsemblePanel(private val result: ThermotaxisEnsembleResult) : JPanel() {

    init {
        preferredSize = java.awt.Dimension(660, 480)
        background = Color.WHITE
    }

    override fun paintComponent(graphics: java.awt.Graphics) {
        super.paintComponent(graphics)
        val g = graphics.create() as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val plateX = 45.0
        val plateY = 55.0
        val plateWidth = width - 90.0
        val plateHeight = height - 145.0
        val toScreenX = { x: Double -> plateX + x / 136.0 * plateWidth }
        val toScreenY = { y: Double -> plateY + y / 96.0 * plateHeight }
        try {
            repeat(20) { index ->
                val fraction = if (result.gradientDirection > 0.0) index / 19.0f else 1.0f - index / 19.0f
                g.color = Color(0.05f + 0.9f * fraction, 0.2f, 0.95f - 0.9f * fraction)
                val left = plateX + index * plateWidth / 20.0
                g.fillRect(left.toInt(), plateY.toInt(), (plateWidth / 20.0).toInt() + 1, plateHeight.toInt())
            }
            g.color = Color(255, 255, 255, 150)
            g.fillRect((plateX + plateWidth / 2.0 - 1).toInt(), plateY.toInt(), 2, plateHeight.toInt())
            g.color = Color.BLACK
            g.stroke = BasicStroke(1.1f)
            result.paths.forEach { path ->
                path.points.zipWithNext().forEachIndexed { index, (from, to) ->
                    val progress = (index + 1).toFloat() / (path.points.size - 1).coerceAtLeast(1)
                    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.06f + 0.52f * progress)
                    g.draw(Line2D.Double(toScreenX(from.x), toScreenY(from.y), toScreenX(to.x), toScreenY(to.y)))
                }
                val endpoint = path.points.last()
                g.composite = AlphaComposite.SrcOver
                g.color = Color.WHITE
                g.fillOval((toScreenX(endpoint.x) - 3).toInt(), (toScreenY(endpoint.y) - 3).toInt(), 6, 6)
                g.color = Color.BLACK
                g.fillOval((toScreenX(endpoint.x) - 2).toInt(), (toScreenY(endpoint.y) - 2).toInt(), 4, 4)
            }
            g.composite = AlphaComposite.SrcOver
            g.color = Color.WHITE
            g.fillOval((toScreenX(68.0) - 4).toInt(), (toScreenY(48.0) - 4).toInt(), 8, 8)
            g.color = Color.DARK_GRAY
            g.drawOval((toScreenX(68.0) - 4).toInt(), (toScreenY(48.0) - 4).toInt(), 8, 8)
            val leftTemperature = if (result.gradientDirection > 0.0) 14.0 + result.temperatureOffset else 20.0 + result.temperatureOffset
            val rightTemperature = if (result.gradientDirection > 0.0) 20.0 + result.temperatureOffset else 14.0 + result.temperatureOffset
            g.drawString("${"%.1f".format(leftTemperature)}°C", plateX.toInt(), (plateY - 10).toInt())
            g.drawString("${"%.1f".format(rightTemperature)}°C", (plateX + plateWidth - 38).toInt(), (plateY - 10).toInt())
            g.color = Color.BLACK
            g.drawString("${result.trajectories} population trajectories, ${"%.0f".format(result.durationSeconds)} s each", 45, height - 76)
            g.drawString("Mean final x: ${"%.2f".format(result.meanEndpointX)}; warm half: ${"%.1f".format(result.warmSideFraction * 100)}%", 45, height - 52)
        } finally {
            g.dispose()
        }
    }
}
