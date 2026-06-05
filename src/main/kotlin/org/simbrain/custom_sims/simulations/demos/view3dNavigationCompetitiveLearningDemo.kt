package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.util.place
import org.simbrain.util.swingInvokeLater
import org.simbrain.util.updateAction

/**
 * 3D sensor stream -> competitive feature learning.
 * Lots of tuning needed. Consider deleting and starting over.
 */
val view3dNavigationCompetitiveLearningDemo = newSim {

    workspace.clearWorkspace()

    val scene = createView3dNavigationScene(16, 16)
    val odorWorldComponent = scene.odorWorldComponent
    val view3dSensor = scene.view3dSensor
    val inputSize = view3dSensor.outputWidth * view3dSensor.outputHeight

    val prototypeComponent = addImageWorld("Winner Prototype")
    val prototypeWorld = prototypeComponent.world.apply {
        resetImageAlbum(view3dSensor.outputWidth, view3dSensor.outputHeight)
    }

    val networkComponent = addNetworkComponent("Competitive Feature Network")
    val network = networkComponent.network
    val competitive = CompetitiveNetwork(inputSize, 8)
    network.addNetworkModelAsync(competitive)
    competitive.inputLayer.setUpperBound(1.0)
    competitive.learningRate = 0.03
    competitive.normalizeInputs = true
    competitive.useLeakyLearning = false
    competitive.leakyLearningRate = 0.005

    var inputMode = "Raw"
    var learningRate = 0.03
    var learningEnabled = true
    var winnerLabelText = "Winner: N/A"
    var winnerLabelRef: javax.swing.JLabel? = null

    workspace.updater.updateManager.addAction(updateAction("Load 3D input to competitive layer") {
        val rawInput = view3dSensor.brightness
        val currentInput = if (inputMode == "Edge") {
            simpleEdgeMap(rawInput, view3dSensor.outputWidth, view3dSensor.outputHeight)
        } else {
            rawInput
        }
        competitive.inputLayer.activationArray = currentInput
    }, 0)

    workspace.addUpdateAction(updateAction("Winner + prototype readout") {
        val outputs = competitive.competitive.activationArray
        if (outputs.isEmpty()) return@updateAction

        val winnerIndex = outputs.indices.maxByOrNull { outputs[it] } ?: return@updateAction
        val winnerActivation = outputs[winnerIndex]
        val winnerNeuron = competitive.competitive.neuronList[winnerIndex]
        val prototype = winnerNeuron.fanIn.map { it.strength }.toDoubleArray()
        prototypeWorld.imageAlbum.setBrightness(normalizeToUnitRange(prototype))

        winnerLabelText = "Winner: N${winnerIndex + 1}  (activity=${"%.3f".format(winnerActivation)})"
        swingInvokeLater {
            winnerLabelRef?.text = winnerLabelText
        }
    })

    withGui {
        place(odorWorldComponent, 0, 0, 420, 420)
        place(prototypeComponent, 425, 0, 320, 420)
        place(networkComponent, 0, 425, 745, 340)

        createControlPanel("Competitive Controls", 750, 0) {
            winnerLabelRef = addLabel(winnerLabelText)

            addComboBox("Input Mode", listOf("Raw", "Edge"), inputMode) { selected ->
                inputMode = selected
            }
            addFormattedNumericTextField("Learning Rate", initValue = learningRate) { value ->
                learningRate = value
                if (learningEnabled) {
                    competitive.learningRate = learningRate
                }
            }
            addCheckBox("Learning Enabled", learningEnabled) { enabled ->
                learningEnabled = enabled
                competitive.learningRate = if (enabled) learningRate else 0.0
            }
            addCheckBox("Normalize Inputs", competitive.normalizeInputs) { checked ->
                competitive.normalizeInputs = checked
            }
            addCheckBox("Use Leaky Learning", competitive.useLeakyLearning) { checked ->
                competitive.useLeakyLearning = checked
            }
            addFormattedNumericTextField("Leaky Rate", initValue = competitive.leakyLearningRate) { value ->
                competitive.leakyLearningRate = value
            }
            addButton("Randomize Weights") {
                competitive.randomize()
            }
        }
    }

    addSidebarInfo(
        """
        # 3D Navigation + Competitive Feature Learning

        This simulation links embodied 3D vision to unsupervised competitive learning.

        ## What to Do

        1. Move the agent to sample different viewpoints and landmarks.
        2. Watch `Winner` and the `Winner Prototype` image update over time.
        3. Switch `Input Mode` between `Raw` and `Edge`.
        4. Adjust learning settings (learning rate, normalization, leaky learning).

        ## Concept

        Competitive neurons self-organize so different units specialize to recurring visual patterns in active experience.
        """.trimIndent()
    )
}
