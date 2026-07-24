/**
 * A visual, steering-only implementation of the fitted thermotaxis circuit in
 * Ikeda, Matsumoto, and Izquierdo (2021).
 */
package org.simbrain.custom_sims.simulations.neuroscience

import org.piccolo2d.PLayer
import org.piccolo2d.PNode
import org.piccolo2d.util.PPaintContext
import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapseAsync
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.fitWorldToFrameSize
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.*

val nematodeThermotaxis = newSim {
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

    val activityPlot = addTimeSeriesComponent("Motor and sensory activity", listOf("AFD", "DMN", "VMN"))

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
    var gradientDirection = 1.0
    var temperatureOffset = 0.0
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

            var heading = worm.heading * PI / 180.0 + result.curvature * network.timeStep
            val stepDistance = 0.2 * network.timeStep * world.width / 136.0
            var nextX = worm.x + stepDistance * cos(heading)
            var nextY = worm.y - stepDistance * sin(heading)
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
        }
    }

    val activityPlotCouplings = mutableListOf<org.simbrain.workspace.couplings.Coupling>()

    fun setActivityPlotVisible(visible: Boolean) {
        activityPlot.isGuiOn = visible
        if (visible) {
            with(couplingManager) {
                activityPlotCouplings += afd couple activityPlot.model.timeSeriesList[0]
                activityPlotCouplings += dmn couple activityPlot.model.timeSeriesList[1]
                activityPlotCouplings += vmn couple activityPlot.model.timeSeriesList[2]
            }
        } else {
            couplingManager.removeCouplings(activityPlotCouplings)
            activityPlotCouplings.clear()
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
        }
    }.apply {
        pickable = false
        setBounds(0.0, 0.0, world.width, world.height)
    }

    withGui {
        val networkWidth = 385
        val networkHeight = 447
        val controlPanel = createControlPanel("Thermotaxis Controls", 10, 10) {
            addButton("Reset up") { resetModel() }
            addButton("Reset left") { resetModel(180.0) }
            addButton("Reset right") { resetModel(0.0) }
            addButton("Reverse gradient") {
                gradientDirection *= -1.0
                gradientOverlay.invalidatePaint()
            }
            fun updateTemperatureOffset(delta: Double) {
                temperatureOffset += delta
                gradientOverlay.invalidatePaint()
            }
            addButton("Warm plate") { updateTemperatureOffset(0.5) }
            addButton("Cool plate") { updateTemperatureOffset(-0.5) }
            addCheckBox("Show trail", true) { showTrail -> worm.isShowTrail = showTrail }
            addCheckBox("Show activity plot", false) { visible -> setActivityPlotVisible(visible) }
        }.awaitLayout()
        controlPanel.setLocation(10, 10)
        val networkX = controlPanel.rightEdgeWithGap()
        val worldX = networkX + networkWidth + SIM_WINDOW_GAP
        place(networkComponent, networkX, 10, networkWidth, networkHeight)
        place(worldComponent, worldX, 10, 621, networkHeight)
        place(activityPlot, worldX, 10 + networkHeight + SIM_WINDOW_GAP, 621, 210)
        setActivityPlotVisible(false)
        worldComponent.getDesktopComponentAs<OdorWorldDesktopComponent>().apply {
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
        # C. elegans Thermotaxis: Steering Circuit

        This simulation presents one fitted neural circuit for thermotactic steering from Ikeda, Matsumoto, and Izquierdo (2021). The model worm moves on a 14°C–20°C thermal gradient, using its neural activity to continuously adjust its path.

        # Evolutionary Search

        The original study used an evolutionary algorithm to search for circuit parameters that reproduced observed thermotaxis and steering behavior. Each search evolved a population of 96 parameter sets for 300 generations and retained its best-performing individual. This simulation is one such evolved parameter set: a concrete member of the family of circuits that matched the behavioral data.

        # Circuit Guide

        - **AFD (Temperature)** is the thermosensory neuron. Its filtered response represents recent temperature history; the AFD–AIY link is chemical.
        - **AIB, AIY, and AIZ** are interneurons that relay and transform the AFD signal within the steering circuit.
        - **CPG** is a central pattern generator: an oscillatory input that supplies opposite rhythmic drive to the two motor neurons, producing the dorsal–ventral locomotor rhythm. Starting straight up, these small side-to-side wiggles sample the left–right temperature gradient and provide the temperature changes that AFD integrates. The wiggles are easiest to see by zooming in on the trail.
        - **DMN (Output)** and **VMN (Output)** are dorsal and ventral neck motor neurons. Their difference in activity determines the instantaneous curvature of the path.

        The AFD–AIB electrical gap junction is implemented as a pair of reciprocal synapses so it can be shown and edited in Simbrain. Its current is `conductance × (AFD activity − AIB state)`; the conductance is fixed while the activity difference changes over time.

        # What to Do

        1. Click `Run` and follow the trail across the thermal plate. In hotter regions, the worm should move more straight; in colder regions, it should follow tighter, more circling paths.
        2. Use `Reverse gradient` while the model is running to observe how the thermal signal and steering pattern adapt.
        3. Select **AFD (Temperature)** in the network, press `Shift+F` to clamp it, then use the arrow keys to set its activation. Comparing low and high AFD values tests how the temperature signal changes the longer-scale steering bias.
        4. Clamp **AIB**, **AIY**, or **AIZ** to zero and compare its trail with the intact circuit. The original study found that ablating each of these interneurons impaired the characteristic thermotactic curving bias.
        5. Clamp **CPG** to zero, then zoom in on the trail. The local path should become straighter rather than following its small wiggly curves, reducing its initial sampling of the left–right gradient. Clamp **DMN (Output)** or **VMN (Output)** to test how a fixed dorsal or ventral motor output changes the path's turning bias.
        6. Use `Warm plate` or `Cool plate` to shift the temperature of the whole plate in 0.5°C steps. The colors and temperature labels update with the shift, and AFD receives the changed temperature signal.

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
