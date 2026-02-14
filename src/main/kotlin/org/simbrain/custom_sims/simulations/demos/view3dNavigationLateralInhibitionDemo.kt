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
import org.simbrain.util.updateAction
import java.awt.Dimension
import java.awt.Graphics

/**
 * 3D sensor stream -> lateral inhibition retina.
 *
 * This version emphasizes standard Simbrain components so users can recreate it manually.
 */
val view3dNavigationLateralInhibitionDemo = newSim {

    workspace.clearWorkspace()

    val (odorWorldComponent, view3dSensor) = createView3dNavigationScene(24, 24)
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
    var rawInputMode = false
    var savedExcitStrength = excitStrength
    var savedInhibStrength = inhibStrength

    fun applyStrengths() {
        excitatoryLateralSynapses.forEach { it.strength = excitStrength }
        inhibitoryLateralSynapses.forEach { it.strength = -inhibStrength }
    }
    applyStrengths()

    workspace.updater.updateManager.addAction(updateAction("Scale input gain") {
        retina.activationArray = retina.activationArray.map { a ->
            (a * inputGain).coerceIn(0.0, 1.0)
        }.toDoubleArray()
    }, 0)

    with(couplingManager) {
        createCoupling(
            view3dSensor.getProducer(view3dSensor::brightness),
            retina.getConsumer("addInputs")
        )
    }

    withGui {
        place(odorWorldComponent, 345, 0, 400, 620)
        place(networkComponent, 734, 0, 565, 618)

        val brightnessPanel = object : javax.swing.JPanel() {
            init {
                preferredSize = Dimension(280, 180)
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val brightness = view3dSensor.brightness
                val expectedSize = view3dSensor.outputWidth * view3dSensor.outputHeight
                if (brightness.size == expectedSize && expectedSize > 0) {
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

        val controlPanel = createControlPanel("Lateral Inhibition Controls", 0, 0) {
            addComponent(brightnessPanel)
            addLabel("Brightness Array")
            addSeparator()
            addSlider("Input Gain", 0.1, 2.5, inputGain, 0.05) {
                inputGain = it
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
            //addButton("Clear Retina") {
            //    retina.activationArray = DoubleArray(imageSize)
            //}
            //addButton("Randomize Lateral Weights") {
            //    excitatoryLateralSynapses.forEach { it.randomize() }
            //    inhibitoryLateralSynapses.forEach { it.randomize() }
            //    applyStrengths()
            //}
        }
        controlPanel.setBounds(0, 0, 353, 597)

        controlPanel.launch(Dispatchers.Swing) {
            while (true) {
                brightnessPanel.repaint()
                delay(50)
            }
        }
    }

    addSidebarInfo(
        """
        # 3D Navigation + Local Inhibition Edge Map

        This version is built from standard Simbrain parts that can be recreated by hand:

        - `View3DSensor` -> **coupling** -> `Lateral Inhibition Retina`.
        - `Retina` recurrently connected by two **distance-based (radial Gaussian) strategies**:
          one short-range excitatory and one broader inhibitory.

        ## What to Do

        1. Run and move the agent in Odor World.
        2. Watch `Brightness Array` in the control panel to see what is being coupled.
        3. Watch `Lateral Inhibition Retina` in the network window.
        4. Adjust controls and compare:
           - More `Inhibitory Lateral`: sharper contrast, stronger surround suppression.
           - More `Excitatory Lateral`: broader local spread / smoothing.
           - `Raw Input Mode`: disables recurrent effects so you can compare with direct input.

        ## Scientific Backing

        This captures core neurocomputational ideas from early vision:
        - center-surround style interactions
        - lateral inhibition as contrast enhancement
        - recurrent dynamics shaping the final response

        It is still a simplified model, not a full retinal circuit:
        - no explicit bipolar / horizontal / amacrine cell types
        - no realistic photoreceptor adaptation or noise statistics
        - no full biophysical membrane dynamics

        So it is useful as a conceptual and algorithmic retina-like demo, not a detailed biological retina simulation.
        """.trimIndent()
    )
}
