package org.simbrain.custom_sims.simulations.demos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import org.simbrain.custom_sims.*
import org.simbrain.network.connections.radialGaussianStyle
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.addToNetwork
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.toGrayScaleImage
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JPanel

/**
 * A retina-like simulation / first effort to link 3d sensor to free nodes with recurrent connections
 */
val view3dNavigationLateralInhibitionDemo = newSim {

    workspace.clearWorkspace()

    val scene = createView3dNavigationScene(24, 24)
    scene.agent.apply {
        location = point(54, 116)
        heading = 344.0
    }
    scene.view3dSensor.update(scene.agent)
    val odorWorldComponent = scene.odorWorldComponent
    val view3dSensor = scene.view3dSensor
    val imageSize = view3dSensor.outputWidth * view3dSensor.outputHeight

    val networkComponent = addNetworkComponent("Lateral Inhibition Network")
    val network = networkComponent.network

    val retina = network.addNeuronCollection(imageSize) {
        updateRule = LinearRule().apply {
            clippingType = LinearRule.ClippingType.Relu
            slope = 1.0
        }
    }.apply {
        label = "Lateral Inhibition Retina"
        setUpperBound(1.0)
        layout(GridLayout(26.0, 26.0, view3dSensor.outputWidth))
        location = point(0.0, 0.0)
    }

    val excitatoryLateralSynapses = radialGaussianStyle(
        lambda = 22.0,
        distConst = 0.12
    ).apply {
        percentExcitatory = 100.0
    }.connectNeurons(retina.neuronList, retina.neuronList)
    excitatoryLateralSynapses.addToNetwork(network)

    val inhibitoryLateralSynapses = radialGaussianStyle(
        lambda = 70.0,
        distConst = 0.05
    ).apply {
        percentExcitatory = 0.0
    }.connectNeurons(retina.neuronList, retina.neuronList)
    inhibitoryLateralSynapses.addToNetwork(network)

    var excitStrength = 0.20
    var inhibStrength = 0.12
    var inputGain = 1.0
    var reverseInput = false
    var rawInputMode = false
    var savedExcitStrength = excitStrength
    var savedInhibStrength = inhibStrength

    fun applyStrengths() {
        excitatoryLateralSynapses.forEach { it.strength = excitStrength }
        inhibitoryLateralSynapses.forEach { it.strength = -inhibStrength }
    }
    applyStrengths()

    val sensorInputAdapter = object : AttributeContainer {
        override val id: String = "Sensor input adapter"

        @Consumable
        fun setInput(raw: DoubleArray) {
            if (raw.size != imageSize) return
            val processed = DoubleArray(imageSize) { i ->
                val base = if (reverseInput) 1.0 - raw[i] else raw[i]
                (base * inputGain).coerceIn(0.0, 1.0)
            }
            retina.addInputs(processed)
        }
    }

    with(couplingManager) {
        createCoupling(
            view3dSensor.getProducer(view3dSensor::brightness),
            sensorInputAdapter.getConsumer("setInput")
        )
    }

    withGui {
        val brightnessPanel = object : JPanel() {
            override fun getPreferredSize() = Dimension(280, 180)
            override fun getMinimumSize() = Dimension(280, 180)
            override fun getMaximumSize() = Dimension(280, 180)

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val brightness = view3dSensor.brightness
                if (brightness.size == imageSize) {
                    val image = brightness.toGrayScaleImage(
                        view3dSensor.outputWidth,
                        view3dSensor.outputHeight
                    )
                    g.drawImage(image, 0, 0, width, height, null)
                } else {
                    g.drawString("Waiting for brightness data...", 10, height / 2)
                }
            }
        }

        val controlPanel = createControlPanel("Lateral Inhibition Controls", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addLabel("Brightness Array")
            addComponent(brightnessPanel)
            addSeparator()
            addSlider("Input Gain", 0.1, 2.5, inputGain, 0.05) {
                inputGain = it
            }
            addCheckBox("Reverse Input (1-x)", reverseInput) { enabled ->
                reverseInput = enabled
            }
            addSlider("Excitatory Lateral", 0.0, 0.6, excitStrength, 0.01) {
                excitStrength = it
                applyStrengths()
            }
            addSlider("Inhibitory Lateral", 0.0, 1.2, inhibStrength, 0.01) {
                inhibStrength = it
                applyStrengths()
            }
            addCheckBox("Raw Input Mode", rawInputMode) { enabled ->
                rawInputMode = enabled
                if (enabled) {
                    savedExcitStrength = excitStrength
                    savedInhibStrength = inhibStrength
                    excitStrength = 0.0
                    inhibStrength = 0.0
                } else {
                    excitStrength = savedExcitStrength
                    inhibStrength = savedInhibStrength
                }
                applyStrengths()
            }
        }.awaitLayout()
        controlPanel.setBounds(SIM_WINDOW_GAP, SIM_WINDOW_GAP, 353, 597)
        place(odorWorldComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 400, 620)
        place(networkComponent, controlPanel.rightEdgeWithGap() + 400 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 565, 618)

        controlPanel.launch(Dispatchers.Swing) {
            while (true) {
                brightnessPanel.repaint()
                delay(50)
            }
        }
    }

    workspace.updater.iterate(2)

    addSidebarInfo(
        """
        # 3D Navigation + Local Inhibition Edge Map

        A retina-like model to illustrate coupling from `Odor World` with a `3D sensor` to a local circuit.
        The circuit is recurrently connected by two [distance-based](https://docs.simbrain.net/docs/network/connections/distanceBased.html) strategies:
          one short-range excitatory and one broader inhibitory.

        # Simulation Details

        This captures core neurocomputational ideas from early vision:
        - center-surround style interactions
        - lateral inhibition as contrast enhancement
        - recurrent dynamics shaping the final response

        It is a simplified model, not a realistic retinal circuit, and it's also fairly provisional (as of Spring 2026). 
        We have only recently implemented 3d and this is a first example of what's possible. More is coming!

        # What to Do

        1. Click `Run` and watch Amy wander through `Odor World`, or move her manually.
        2. Watch `Brightness Array` in the control panel to see the raw data sent to the network
        3. Watch the network window which shows how a simple retina-like circuit processes this information
        4. Adjust controls and compare:
           - More `Inhibitory Lateral`: sharper contrast, stronger surround suppression.
           - More `Excitatory Lateral`: broader local spread / smoothing.
           - `Reverse Input (1-x)`: inverts polarity so that darker regions map to less rather than more neural activity
           - `Raw Input Mode`: disables recurrent effects so you can compare what the network does with direct input.

        """.trimIndent()
    )
}
