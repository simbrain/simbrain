/**
 * The fitted thermotaxis circuit of Ikeda, Matsumoto, and Izquierdo (2021), using parameter set #67 from
 * the paper's S1 Table, the set the paper plots in Figure 7. The live simulation runs the circuit on
 * native Simbrain components (see ThermotaxisNativeCircuit); the [ThermotaxisModel] in this file is the
 * headless reference twin, verified step for step against a recorded run of the authors' C++
 * implementation and against the native circuit — see ThermotaxisTraceRecorder, NematodeThermotaxisTest,
 * and ThermotaxisNetworkParityTest. The model also powers the fast population assay.
 */
package org.simbrain.custom_sims.simulations.neuroscience

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.piccolo2d.PLayer
import org.piccolo2d.PNode
import org.piccolo2d.util.PPaintContext
import org.simbrain.custom_sims.*
import org.simbrain.network.core.GapJunction
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Synapse
import org.simbrain.network.updaterules.AfdThermoreceptorRule
import org.simbrain.workspace.couplings.ScaleOperation
import org.simbrain.plot.heatmap.HeatMapModel
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.showAPEOptionDialog
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.fitWorldToFrameSize
import org.simbrain.world.odorworld.sensors.TemperatureSensor
import org.simbrain.world.odorworld.sensors.ThermalGradient
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.geom.Line2D
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import kotlin.math.*

val nematodeThermotaxis = newSim { optionString ->
    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Thermotaxis Circuit")
    val network = networkComponent.network
    val circuit = ThermotaxisNativeCircuit.build(network)

    circuit.afd.apply { label = "AFD (Temperature)"; location = point(190.0, 400.0) }
    circuit.aib.location = point(290.0, 320.0)
    circuit.aiy.location = point(90.0, 320.0)
    circuit.aiz.location = point(190.0, 230.0)
    circuit.dmn.apply { label = "DMN (Output)"; location = point(280.0, 135.0) }
    circuit.vmn.apply { label = "VMN (Output)"; location = point(100.0, 135.0) }
    circuit.cpg.location = point(190.0, 45.0)
    circuit.synapses.values.forEach {
        it.lowerBound = -15.0
        it.upperBound = 15.0
    }

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

    val gradient = ThermalGradient()
    val temperatureSensor = TemperatureSensor().apply { this.gradient = gradient }
    worm.addSensor(temperatureSensor)
    val turningLeft = Turning(Turning.LEFT)
    val turningRight = Turning(Turning.RIGHT)
    worm.addEffector(turningLeft)
    worm.addEffector(turningRight)
    val wormBehavior = ThermotaxisWormBehavior().apply { this.gradient = gradient }
    worm.behavior = wormBehavior

    val steeringDegreesPerStep = NEUROMUSCULAR_WEIGHT * network.timeStep * 180.0 / PI
    with(couplingManager) {
        temperatureSensor.getProducer(temperatureSensor::currentValue) couple
            circuit.afd.getConsumer(Neuron::setTemperatureInput)
        circuit.dmn.getProducer(circuit.dmn::activation) via ScaleOperation(steeringDegreesPerStep) couple
            turningLeft.getConsumer(Turning::setAmount)
        circuit.vmn.getProducer(circuit.vmn::activation) via ScaleOperation(steeringDegreesPerStep) couple
            turningRight.getConsumer(Turning::setAmount)
    }

    if (optionString == "record-trace") {
        println(ThermotaxisTraceRecorder.run().let { "Wrote ${it.rows} rows to ${it.file}" })
    }
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
    val circuitNeuronCollection = NeuronCollection(circuit.neurons).apply { label = "Circuit neurons" }
    network.addNetworkModelAsync(circuitNeuronCollection)

    fun connectionWeight(synapse: Synapse) =
        if (synapse in network.flatSynapseList) synapse.strength else 0.0

    fun currentWeights(): ThermotaxisWeights {
        val junction = circuit.gapJunction
        val gapConductance = if (junction in network.getModels<GapJunction>() && junction.isEnabled) {
            junction.conductance
        } else {
            0.0
        }
        fun weight(key: String) = connectionWeight(circuit.synapses.getValue(key))
        return ThermotaxisWeights(
            afdToAibGap = gapConductance,
            afdToAiy = weight("afdToAiy"),
            aibToAiy = weight("aibToAiy"),
            aibToDmn = weight("aibToDmn"),
            aiyToAiz = weight("aiyToAiz"),
            aizToAib = weight("aizToAib"),
            aizToDmn = weight("aizToDmn"),
            aizToVmn = weight("aizToVmn"),
            dmnToDmn = weight("dmnToDmn"),
            dmnToVmn = weight("dmnToVmn"),
            vmnToDmn = weight("vmnToDmn"),
            vmnToVmn = weight("vmnToVmn"),
            cpgToDmn = weight("cpgToDmn"),
            cpgToVmn = weight("cpgToVmn")
        )
    }

    fun resetSimulation(heading: Double = 90.0) {
        circuit.reset()
        wormBehavior.reset()
        worm.location = point(world.width / 2.0, world.height / 2.0)
        worm.heading = heading
        worm.resetAnimation()
        // Prime AFD's temperature history from the worm's actual starting temperature. Couplings run
        // before component updates, so without this the first delivered sample would be the sensor's
        // uninitialized 0 °C and the whole 100 s history would prime cold, producing a long false
        // AFD transient.
        temperatureSensor.update(worm)
        circuit.afd.setTemperatureInput(temperatureSensor.currentValue)
    }

    resetSimulation()

    val gradientOverlay = object : PNode() {
        override fun paint(paintContext: PPaintContext) {
            val graphics = paintContext.graphics as Graphics2D
            val bandWidth = world.width / 20.0
            repeat(20) { index ->
                val directionFraction = if (gradient.direction > 0.0) index / 19.0f else 1.0f - index / 19.0f
                val fraction = (directionFraction + (gradient.offset / 6.0).toFloat()).coerceIn(0.0f, 1.0f)
                graphics.color = Color(0.05f + 0.9f * fraction, 0.2f, 0.95f - 0.9f * fraction, 0.48f)
                graphics.fillRect((index * bandWidth).toInt(), 0, bandWidth.toInt() + 1, world.height.toInt())
            }
            graphics.color = Color.WHITE
            val leftTemperature = gradient.temperatureAt(point(0.0, 0.0), world)
            val rightTemperature = gradient.temperatureAt(point(world.width, 0.0), world)
            val leftLabel = "${"%.1f".format(leftTemperature)}°C"
            val rightLabel = "${"%.1f".format(rightTemperature)}°C"
            graphics.drawString(leftLabel, 8, 20)
            graphics.drawString(rightLabel, (world.width - 40).toInt(), 20)
            wormBehavior.activeTurnLabel?.let { turnLabel ->
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
        var heatMapX = 0

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
            addButton("Reset up") { resetSimulation() }
            addButton("Reset left") { resetSimulation(180.0) }
            addButton("Reset right") { resetSimulation(0.0) }
            addButton("Reverse gradient", context = Dispatchers.Swing) {
                gradient.direction *= -1.0
                repaintThermalPlate()
            }
            suspend fun updateTemperatureOffset(delta: Double) = withContext(Dispatchers.Swing) {
                gradient.offset += delta
                repaintThermalPlate()
            }
            addButton("Warm plate") { updateTemperatureOffset(0.5) }
            addButton("Cool plate") { updateTemperatureOffset(-0.5) }
            addCheckBox("Show trail", true) { showTrail -> worm.isShowTrail = showTrail }
            addCheckBox("Use empirical turns", true) { enabled ->
                wormBehavior.useEmpiricalTurns = enabled
                if (!enabled) wormBehavior.cancelTurn()
            }
            addButton("Add activation heat map") {
                val heatMapButton = this
                val heatMap = addHeatMap("Circuit activation heat map")
                heatMap.model.fixedWidth = false
                with(couplingManager) {
                    circuitNeuronCollection.getProducer(circuitNeuronCollection::activationArray) couple
                        heatMap.model.getConsumer(HeatMapModel::setValues)
                }
                withContext(Dispatchers.Swing) {
                    place(heatMap, heatMapX, networkHeight + 2 * SIM_WINDOW_GAP, 700, 300)
                    heatMapButton.isEnabled = false
                }
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
                val assayGradientDirection = gradient.direction
                val assayTemperatureOffset = gradient.offset
                val result = ThermotaxisPopulationSimulation.run(
                    worms = trajectories,
                    seconds = seconds,
                    weights = assayWeights,
                    gradientDirection = assayGradientDirection,
                    temperatureOffset = assayTemperatureOffset,
                    centerTemperature = gradient.centerTemperature,
                    halfSpan = gradient.spanDegrees / 2.0,
                    bufferedSemantics = true,
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
        heatMapX = networkX
        place(networkComponent, networkX, 10, networkWidth, networkHeight)
        place(worldComponent, worldX, 10, 621, networkHeight)
        odorWorldDesktopComponent.apply {
            val overlayLayer = PLayer().apply { addChild(gradientOverlay) }
            worldPanel.canvas.camera.addLayer(1, overlayLayer)
            fitWorldToFrameSize()
            gradientOverlay.invalidatePaint()
            worldPanel.canvas.repaint()
        }
        resetSimulation()
    }

    addSidebarInfo(
        """
        # Thermotaxis in Nematodes

       This simulation models directed migration, movement guided toward warmer or colder regions of a temperature gradient, in *Caenorhabditis elegans*. This differs from isothermal migration, in which worms seek and remain near a preferred temperature. (Here, “worm,” “nematode,” and *C. elegans* all refer to the same small roundworm.) The model is both behaviorally and neurally realistic: it shows what real nematodes do and the neural activations are similar to those observed in real nematodes.
       
       Although these nematodes have 302 neurons, the process described in the [PNAS study](https://www.pnas.org/doi/abs/10.1073/pnas.1918528117) identified these eight as essential to directed migration. Across a population and enough simulated time, worms  tend to migrate toward the warmer side of the 14°C–20°C plate. Their paths and turns are stochastic, however, so this tendency is not immediate and will not be obvious in every individual run. 

        Tip: For better performance, minimize the network window while the simulation is running. Each workspace iteration advances the model by one 0.1-second step, so long migrations take many iterations; use the workspace run controls to let it run.

        # What to Do

        1. Click `Run` and watch the worm move. With `Use empirical turns` enabled, it changes direction stochastically as well as steering continuously; an individual trail may wander even though a population should tend toward warmth over time.
        2. Click `Run population simulation` to view many trajectories together. The defaults are 12 worms for 1500 seconds; use more worms or longer runs for a clearer aggregate tendency. Reverse the gradient before starting a population simulation to see the warm-directed tendency reverse sides.
        3. Turn off `Use empirical turns` to isolate the steering circuit. The clearest signature is tight, circling movement when the worm heads toward the cold and much straighter movement when it heads toward the warm, rather than reliable migration in one short run.
        4. Use `Reverse gradient`, `Warm plate`, and `Cool plate` to change the visible thermal environment.
        5. Click `Add activation heat map` to graph the seven circuit neurons over time.

        # Details of This Simulation

        ## Evolutionary Search

        The original study used an evolutionary algorithm to search for circuit parameters that reproduced the behavior of real nematodes during directed migration and steering. Each search evolved a population of 96 parameter sets for 300 generations and retained its best-performing individual. This simulation uses set #67 from the paper's S1 Table, the same set the paper plots in Figure 7.

        ## Circuit Guide

        - **AFD (Temperature)** is the thermosensory neuron. It responds to *change* in temperature rather than its absolute value: its measured response function is convolved over the previous 100 seconds and is biphasic, so a steady temperature leaves AFD near rest while warming drives it positive and cooling drives it negative.
        - **AIB, AIY, and AIZ** are interneurons that relay and transform the AFD signal within the steering circuit. The AFD–AIY link is chemical, and all three track AFD closely: the paper reports correlations of 0.99, 0.98, and 0.99 between AFD and each of them.
        - **CPG** is a central pattern generator: an oscillatory input that supplies opposite rhythmic drive to the motor neurons, producing small dorsal–ventral wiggles that sample the temperature gradient.
        - **DMN (Output)** and **VMN (Output)** are abbreviations for larger, highly correlated dorsal and ventral neck motor-neuron groups, as discussed in the PNAS study. Their activity difference determines instantaneous path curvature.

        The AFD–AIB electrical gap junction is a real bidirectional Simbrain gap junction, drawn as a line with a paired-bars channel glyph at its midpoint. It has a single conductance and no direction: it passes `conductance × (V other − V this)` into both endpoints simultaneously, computed from the neurons' internal membrane states.

        Each neuron has two values worth distinguishing. Its *state* is a membrane potential relative to rest, which can be any size and is what the paper plots in Figure 7A; double-click a neuron and open its state variables to see it. Its *activation* is the sigmoid of that state plus a bias, squashed into the range 0 to 1, and is what travels along chemical synapses. A neuron whose bias pushes it far into either tail of the sigmoid can therefore look flat while its underlying state is still moving. AFD's node displays its raw state, since that is the meaningful sensory quantity. Run the simulation with the `record-trace` option to write both to a CSV.

        ## Empirical Turns

        In addition to ordinary circuit-generated steering, the simulation can use measured abrupt turn events: **omega turns** are tight, loop-like reorientations; **reversals** move the worm backward; **reversal turns** combine a reversal with reorientation; and **shallow turns** are gentler heading changes. Their frequency, exit direction, duration, and displacement are fixed behavioral measurements from the paper, not evolved circuit behaviors.


        ## Implementation Note

        The circuit runs entirely on native Simbrain components: a thermoreceptor update rule for AFD, continuous sigmoidal rules with output biases for the interneurons and motor neurons, a sinusoidal activity generator for the CPG, ordinary chemical synapses, and a bidirectional gap junction. Temperature reaches AFD through a coupling from a temperature sensor on the worm, and the motor neurons steer the worm through couplings to its turning effectors, so everything you edit by double-clicking a node or connection is the real model. The empirical stochastic turns are a behavioral policy attached to the worm rather than circuit dynamics. This implementation is verified step for step against the authors' C++ implementation; see NematodeThermotaxisTest and ThermotaxisNetworkParityTest.

        # References

        Ikeda, M., Matsumoto, H., & Izquierdo, E. J. (2021). [Persistent thermal input controls steering behavior in Caenorhabditis elegans](https://doi.org/10.1371/journal.pcbi.1007916). _PLoS Computational Biology, 17_(1), e1007916.

        Ikeda, M., Nakano, S., Giles, A. C., Xu, L., Costa, W. S., Gottschalk, A., & Mori, I. (2020). [Context-dependent operation of neural circuits underlies a navigation behavior in *Caenorhabditis elegans*](https://www.pnas.org/doi/abs/10.1073/pnas.1918528117). _Proceedings of the National Academy of Sciences, 117_(11), 6178–6188.

        """.trimIndent()
    )
}

/**
 * When [bufferedSemantics] is set, the AFD state and CPG oscillator feed the interneuron inputs with a
 * one-step delay, while the reported trace columns stay current-step. This reproduces the semantics of
 * Simbrain's buffered network update, where every input is computed from previous-step values, and serves
 * as the parity reference for the native-component implementation of this circuit. The default preserves
 * the original same-step semantics verified against the authors' C++ implementation.
 */
internal class ThermotaxisModel(
    private val states: DoubleArray,
    private val biases: DoubleArray,
    private val bufferedSemantics: Boolean = false
) {
    private val dt = 0.1
    private val temperatureHistory = DoubleArray(afdResponseKernel.size) { PLATE_CENTER_TEMPERATURE }
    private var time = 0.0
    private var previousAfdState = 0.0
    private var previousOscillator = 0.0

    fun reset() {
        states.fill(0.0)
        temperatureHistory.fill(PLATE_CENTER_TEMPERATURE)
        time = 0.0
        previousAfdState = 0.0
        previousOscillator = 0.0
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
            afdResponseKernel[index] * thresholdResponse(temperatureHistory[index])
        }
        val afdState = activityOverrides[0] ?: sensedAfdState
        val afdForInputs = if (bufferedSemantics) previousAfdState else afdState
        fun output(index: Int): Double {
            return activityOverrides[index + 1] ?: sigmoid(states[index] + biases[index])
        }
        val outputs = states.indices.map(::output).toDoubleArray()
        val inputs = doubleArrayOf(
            weights.afdToAibGap * (afdForInputs - states[0]) + weights.aizToAib * outputs[2],
            weights.afdToAiy * sigmoid(afdForInputs + AFD_BIAS) + weights.aibToAiy * outputs[0],
            weights.aiyToAiz * outputs[1],
            weights.aibToDmn * outputs[0] + weights.aizToDmn * outputs[2] + weights.dmnToDmn * outputs[3] + weights.vmnToDmn * outputs[4],
            weights.aizToVmn * outputs[2] + weights.dmnToVmn * outputs[3] + weights.vmnToVmn * outputs[4]
        )
        time += dt
        val oscillator = activityOverrides[6] ?: sin(2.0 * PI * time / OSCILLATOR_PERIOD)
        val oscillatorForInputs = if (bufferedSemantics) previousOscillator else oscillator
        inputs[3] += weights.cpgToDmn * oscillatorForInputs
        inputs[4] += weights.cpgToVmn * oscillatorForInputs
        previousAfdState = afdState
        previousOscillator = oscillator
        states.indices.forEach { index ->
            val override = activityOverrides[index + 1]
            states[index] = if (override == null) {
                states[index] + dt * (inputs[index] - states[index])
            } else {
                logit(override) - biases[index]
            }
        }
        val updatedOutputs = states.indices.map(::output).toDoubleArray()
        return ThermotaxisStep(
            afdState,
            states.copyOf(),
            updatedOutputs,
            oscillator,
            NEUROMUSCULAR_WEIGHT * (updatedOutputs[3] - updatedOutputs[4])
        )
    }

    private fun thresholdResponse(temperature: Double): Double = if (temperature < THRESHOLD_TEMPERATURE) {
        0.0
    } else {
        val difference = (temperature - THRESHOLD_TEMPERATURE).pow(HILL_COEFFICIENT)
        difference / (DISSOCIATION_CONSTANT + difference)
    }

    private fun sigmoid(value: Double): Double = 1.0 / (1.0 + exp(-value))

    private fun logit(value: Double): Double {
        val boundedValue = value.coerceIn(1e-9, 1.0 - 1e-9)
        return ln(boundedValue / (1.0 - boundedValue))
    }
}

internal const val PLATE_CENTER_TEMPERATURE = 17.0
internal const val PLATE_GRADIENT = 3.0
internal const val PLATE_WIDTH = 136.0
internal const val PLATE_HEIGHT = 96.0
internal const val CRAWLING_SPEED = 0.2
internal const val OSCILLATOR_PERIOD = 4.2

internal const val AFD_BIAS = 11.57
private const val THRESHOLD_TEMPERATURE = 15.54
private const val DISSOCIATION_CONSTANT = 69.22
private const val HILL_COEFFICIENT = 4.80
internal val NEUROMUSCULAR_WEIGHT = 34.68 * PI / 180.0

/**
 * Biases for AIB, AIY, AIZ, DMN and VMN, read from the S1 Table row for parameter set #67 along with the
 * weights in [ThermotaxisWeights] and the sensory constants above. Fresh array per call because the model
 * that receives it also owns a mutable state array of the same shape.
 */
internal val fittedBiases
    get() = doubleArrayOf(1.10, 2.02, -11.90, -4.49, 9.82)

/**
 * The measured AFD response function, shared with [AfdThermoreceptorRule]. The kernel is biphasic and
 * nearly zero-sum, so AFD differentiates its input; replacing it with a purely decaying approximation
 * turns the neuron into a low-pass filter and silences the circuit.
 */
private val afdResponseKernel: DoubleArray get() = AfdThermoreceptorRule.responseKernel

internal data class ThermotaxisWeights(
    val afdToAibGap: Double = 2.62,
    val afdToAiy: Double = -9.25,
    val aibToAiy: Double = 8.98,
    val aibToDmn: Double = 13.34,
    val aiyToAiz: Double = 14.81,
    val aizToAib: Double = -10.55,
    val aizToDmn: Double = -1.53,
    val aizToVmn: Double = -8.75,
    val dmnToDmn: Double = -9.82,
    val dmnToVmn: Double = -6.32,
    val vmnToDmn: Double = 4.19,
    val vmnToVmn: Double = -1.75,
    val cpgToDmn: Double = 9.92,
    val cpgToVmn: Double = -9.92
)

/**
 * One circuit update. [states] are membrane potentials relative to rest, which is what the paper plots;
 * [outputs] are the sigmoid activations those states produce, which is what propagates through synapses.
 */
internal data class ThermotaxisStep(
    val afdState: Double,
    val states: DoubleArray,
    val outputs: DoubleArray,
    val cpgOutput: Double,
    val curvature: Double
)

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

/**
 * Sweeps AFD activity while holding temperature fixed and measures the resulting steering bias, reproducing
 * the AFD-versus-curvature panel of Figure 7B. For the fitted parameter set the relationship is V-shaped
 * rather than monotonic: curvature is largest when AFD is low, falls to a minimum at an intermediate level,
 * and rises again as AFD grows.
 */
internal object ThermotaxisAfdValidation {

    private const val transientSteps = 200
    private const val measuredSteps = 840

    fun run(bufferedSemantics: Boolean = false): ThermotaxisAfdValidationResult {
        val profile = (-30..20).map { level ->
            val afdValue = level * 0.1
            afdValue to meanSteeringBias(afdValue, bufferedSemantics)
        }
        return ThermotaxisAfdValidationResult(profile)
    }

    private fun meanSteeringBias(afdValue: Double, bufferedSemantics: Boolean = false): Double {
        val model = ThermotaxisModel(states = DoubleArray(5), biases = fittedBiases, bufferedSemantics = bufferedSemantics)
        val override = MutableList<Double?>(7) { null }.apply { this[0] = afdValue }
        return (1..transientSteps + measuredSteps)
            .map { model.step(temperature = PLATE_CENTER_TEMPERATURE, activityOverrides = override).curvature }
            .drop(transientSteps)
            .average()
            .let(::abs)
    }
}

internal data class ThermotaxisAfdValidationResult(val profile: List<Pair<Double, Double>>) {

    private val minimum get() = profile.minBy { it.second }

    val minimizingAfd get() = minimum.first

    val minimumSteeringBias get() = minimum.second

    val coldSteeringBias get() = profile.first().second

    /** The minimum must be interior, and steering at low AFD must be clearly stronger than at that minimum. */
    val passes: Boolean
        get() = minimum != profile.first() && minimum != profile.last() &&
            coldSteeringBias > 2.0 * minimumSteeringBias

    private fun degrees(radians: Double) = "%.2f".format(radians * 180.0 / PI)

    fun summary(): String = """
        AFD steering-response validation

        Steering bias at lowest AFD (${"%.1f".format(profile.first().first)}): ${degrees(coldSteeringBias)} deg/s
        Minimum steering bias: ${degrees(minimumSteeringBias)} deg/s at AFD = ${"%.1f".format(minimizingAfd)}
        Steering bias at highest AFD (${"%.1f".format(profile.last().first)}): ${degrees(profile.last().second)} deg/s
        Result: ${if (passes) "PASS — steering is strongest at low AFD and reaches a minimum at an intermediate level." else "FAIL — no interior minimum in the AFD-to-curvature relationship."}

        This reproduces the AFD-versus-curvature panel of Figure 7B for parameter set #67. It does not reproduce the paper's full population migration assay, which also used empirical turning behavior.
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
