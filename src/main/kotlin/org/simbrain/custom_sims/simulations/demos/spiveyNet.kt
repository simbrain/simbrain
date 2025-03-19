package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.*
import org.simbrain.network.connections.OneToOne
import org.simbrain.network.core.labels
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neurongroups.NormalizationGroup
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import java.awt.Dimension

/**
 * Based on Spivey's 2024 paper, "A Linking Hypothesis for Eyetracking and Mousetracking
 * in the Visual World Paradigm"
 */
val spiveyNet = newSim {

    workspace.clearWorkspace()

    // Network
    val networkComponent = addNetworkComponent("Spivey Net")
    val net = networkComponent.network

    val lexicalNodes = NormalizationGroup(4).apply {
        layout = LineLayout()
        applyLayout()
        label = "Lexical"
        neuronList.labels = listOf("candle", "candy", "handle", "fork")
    }
    val visualNodes = NormalizationGroup(4).apply {
        layout = LineLayout()
        applyLayout()
        label = "Visual"
        neuronList.labels = listOf("candle", "candy", "handle", "fork")
    }
    val mouseNodes = NormalizationGroup(4).apply {
        layout = LineLayout()
        applyLayout()
        label = "Mouse"
        neuronList.labels = listOf("candle", "candy", "handle", "fork")
    }
    // TODO: Determine what "foveal prominence" means and how the activaions are determined
    // stochastically from the visual vector
    val eyesNodes = NormalizationGroup(4).apply {
        layout = LineLayout()
        applyLayout()
        label = "Eyes"
        neuronList.labels = listOf("candle", "candy", "handle", "fork")
    }
    val integrationNodes = NormalizationGroup(4).apply {
        layout = LineLayout()
        applyLayout()
        label = "Integration"
    }

    net.addNetworkModels(lexicalNodes, visualNodes, mouseNodes, eyesNodes, integrationNodes).awaitAll()
    lexicalNodes.location = point(-3.70,12.64)
    visualNodes.location = point(285.98,5.92)
    mouseNodes.location = point(438.46,160.33)
    eyesNodes.location = point(144.87,158.44)
    integrationNodes.location = point(131.67, -134.59)

    val connector = OneToOne().apply {
        percentExcitatory = 100.0
        useBidirectionalConnections = true
    }
    net.addNetworkModels(connector.connectNeurons(visualNodes.neuronList, mouseNodes.neuronList))
    net.addNetworkModels(connector.connectNeurons(eyesNodes.neuronList, visualNodes.neuronList))
    net.addNetworkModels(connector.connectNeurons(lexicalNodes.neuronList, integrationNodes.neuronList))
    net.addNetworkModels(connector.connectNeurons(integrationNodes.neuronList, visualNodes.neuronList))
    //bidirectional so order doesn't matter

    // Target vs competitor index
    var targetIndex = 0
    var competitorIndex = 0

    // World
    val oc = addOdorWorldComponent()
    val world = oc.world
    world.isUseCameraCentering = false
    desktop?.getDesktopComponent(oc)?.title = "Mouse Trace"
    val mouse = world.addEntity(157, 271, EntityType.Mouse).apply {
        heading = 90.0
    }
    world.addEntity(38, 49, EntityType.Candle)
    world.addEntity(287, 44, EntityType.Bell)
    mouse.isShowTrail = true

    workspace.addUpdateAction("Move mouse") {
        if (mouse.y > 15) {
            mouse.y -= 50
        }
        mouse.x += 500 * (mouseNodes.neuronList[targetIndex].activation - mouseNodes.neuronList[competitorIndex].activation)
    }

    fun resetMouse() {
        mouse.location = point(157,271)
        mouse.clearTrail()
    }

    // TODO: Make odor world objects match conditions

    withGui {
        //place(docViewer, 0, 0, 464, 619)
        place(networkComponent, 222, 15, 400, 400)
        place(oc, 613, 15, 391, 455)
        (desktop?.getDesktopComponent(oc) as? OdorWorldDesktopComponent)?.worldPanel?.scalingFactor = 0.1
        createControlPanel("Control Panel", 15, 15) {
            addButton("Cohort Condition") {
                // Candle / Candy
                resetMouse()
                targetIndex = 0
                competitorIndex = 1
                // Time 1: Visual input only
                visualNodes.addInputs(doubleArrayOf(1.0,1.0,0.0,0.0))
                workspace.simpleIterate()
                // Times 2-6: Lexical + Visual
                visualNodes.addInputs(doubleArrayOf(1.0,1.0,0.0,0.0))
                lexicalNodes.addInputs(doubleArrayOf(1.0,1.0,0.0,0.0))
                workspace.simpleIterate()
                visualNodes.addInputs(doubleArrayOf(1.0,1.0,0.0,0.0))
                lexicalNodes.addInputs(doubleArrayOf(1.0,1.0,1.0,0.0))
                workspace.simpleIterate()
                visualNodes.addInputs(doubleArrayOf(1.0,1.0,0.0,0.0))
                lexicalNodes.addInputs(doubleArrayOf(1.0,1.0,1.0,0.0))
                workspace.simpleIterate()
                visualNodes.addInputs(doubleArrayOf(1.0,1.0,0.0,0.0))
                lexicalNodes.addInputs(doubleArrayOf(1.0,1.0,1.0,0.0))
                workspace.simpleIterate()
                visualNodes.addInputs(doubleArrayOf(1.0,1.0,0.0,0.0))
                lexicalNodes.addInputs(doubleArrayOf(1.0,0.0,1.0,0.0))
                workspace.simpleIterate()
            }.apply {
                // Hack to make the panel wider
                preferredSize = Dimension(170, 30)
            }
            addButton("Target Condition") {
                // Candle / Fork
                targetIndex = 0
                competitorIndex = 3
                // Time 1: Visual input only
                visualNodes.addInputs(doubleArrayOf(1.0,0.0,0.0,1.0))
                workspace.simpleIterate()
                // Times 2-6: Lexical + Visual
                visualNodes.addInputs(doubleArrayOf(1.0,0.0,0.0,1.0))
                lexicalNodes.addInputs(doubleArrayOf(0.0,0.0,0.0,0.0))
                workspace.simpleIterate()
                visualNodes.addInputs(doubleArrayOf(1.0,0.0,0.0,1.0))
                lexicalNodes.addInputs(doubleArrayOf(0.0,0.0,0.0,0.0))
                workspace.simpleIterate()
                visualNodes.addInputs(doubleArrayOf(1.0,0.0,0.0,1.0))
                lexicalNodes.addInputs(doubleArrayOf(0.0,0.0,0.0,0.0))
                workspace.simpleIterate()
                visualNodes.addInputs(doubleArrayOf(1.0,0.0,0.0,1.0))
                lexicalNodes.addInputs(doubleArrayOf(0.0,0.0,0.0,0.0))
                workspace.simpleIterate()
                visualNodes.addInputs(doubleArrayOf(1.0,0.0,0.0,1.0))
                lexicalNodes.addInputs(doubleArrayOf(0.0,0.0,0.0,0.0))
                workspace.simpleIterate()
            }.apply {
                preferredSize = Dimension(170, 30)
            }
            addButton("Rhyme Condition") {
                resetMouse()
                lexicalNodes.setActivations(doubleArrayOf(-1.0,1.0,-1.0,1.0))
                workspace.simpleIterate(3)
                visualNodes.setActivations(doubleArrayOf(1.0,-1.0,1.0,-1.0))

            }.apply {
                preferredSize = Dimension(170, 30)
            }
            // Reset
            addButton("Reset") {
                resetMouse()
            }
        }
    }
    // TODO: Make + label button for each trial -- NOT COMPLETE


    // TODO: Add basic info about this
    addSidebarInfo(
        """ 
        # Introduction
        
        This is a simulation of a localist attractor simulation of mouse trajectories relative to visual and auditory inputs
         due to Michael Spivey and others.
         
       The simulation shows...
         
        # Background
        
        Relevant papers are 

        [Continuous attraction toward phonological competitors](https://pmc.ncbi.nlm.nih.gov/articles/PMC1177386/)

        [A Linking Hypothesis for Eyetracking and Mousetracking in the Visual World Paradigm](https://www.sciencedirect.com/science/article/pii/S0006899325000356/pdfft?md5=593289ceb624d37b85229782945c7b40&pid=1-s2.0-S0006899325000356-main.pdf)

        > Participants were presented with color images of two objects on a screen (one target and one distractor), and a prerecorded speech file instructed them to click one of them with the mouse. Objects were presented in the upper left and upper right corners of the computer screen (e.g., a candle and a candy, in the cohort condition, or a candle and a jacket, in the control condition). Eight target objects were used to make 32 trials in which the distractor object was either a cohort for the target object or a phonologically dissimilar control and in which the target object was either on the left or right side of the display. Participants were instructed to mouse-click a box in the bottom center of the screen when they were ready to begin a trial. At this time, the two object images would appear in the upper left and right, and 500 ms after the onset of the images, a single spoken word (from a speech file on the computer; mean duration, 532 ms) would name the target object. [Imposing this asynchrony between image onset and speech onset grew out of observations from pilot studies in which simultaneous onset caused participants to occasionally wait until the entire word was spoken before beginning their mouse movement. With the spoken word beginning 500 ms after onset of the images, participants usually begin their mouse movement (straight upward) before the onset of the spoken word, which gives distinguishing properties in the acoustic–phonetic input a chance to influence the continuous motor output midflight."

        # What to do
        
        1. For the lexical task, press the `lexical` button.. observe that...
      
       
        """.trimIndent()
    )


}