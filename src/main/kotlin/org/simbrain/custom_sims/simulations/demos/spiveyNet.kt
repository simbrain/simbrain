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
import org.simbrain.world.odorworld.entities.OdorWorldEntity
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
    val targetObject = OdorWorldEntity(world, EntityType.Candy).apply {
        location = point(287, 44)
    }
    world.addEntity(targetObject)
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
                targetObject.entityType = EntityType.Candy
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
                targetObject.entityType = EntityType.Fork
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
                targetObject.entityType = EntityType.Handle
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
         
        The visual world localist attractor is a computational model that serves as a linking hypothesis between internal cognitive processes 
        (specifically in spoken word recognition) and observable motor outputs like eye movements and 
        mouse trajectories in the Visual World Paradigm. The core idea is that cognition and action are 
        dynamically interconnected: what we look at or move toward not only reflects but also 
        influences what we're thinking. Spivey's model integrates parallel lexical activations, 
        visual input, and motor output with feedback loops especially from eye position to 
        simulate how these processes unfold over time. 

        This simulation demonstrates how smooth, continuous cognitive activations can produce 
        both abrupt saccadic eye movements and gradually curving mouse paths, even replicating 
        nuanced behavioral patterns seen in experiments (like greater mouse curvature after 
        competitor fixations). By doing so, the model offers a powerful tool to test and refine 
        theories of real-time language processing and perception-action coupling.

        There is no learning in this type of network. It was hand-crafted, something like an IAC network.

        Overall, this model illustrates how a relatively simple, structured network can simulate the intricate, r
        eal-time interplay between language comprehension, visual attention, and motor behavior. 
        By incorporating dynamic feedback loops, probabilistic saccade generation, and continuous 
        motor output, the system not only mirrors behavioral data observed in human participants 
        but also sheds light on the underlying cognitive mechanisms. 
        The visual world localist attractor thus stands as a compelling demonstration of how cognition 
        is not a series of isolated stages, but a fluid, embodied process deeply shaped by perception 
        and action unfolding over time.
         
        # Background
        
        Relevant papers are 
        - [Continuous attraction toward phonological competitors](https://pmc.ncbi.nlm.nih.gov/articles/PMC1177386/)
        - [A Linking Hypothesis for Eyetracking and Mousetracking in the Visual World Paradigm](https://www.sciencedirect.com/science/article/pii/S0006899325000356/pdfft?md5=593289ceb624d37b85229782945c7b40&pid=1-s2.0-S0006899325000356-main.pdf)


        This simulates a study of eye and mouse tracking.  Participants are shown two objects and told which to point to,
        and the scientist tracks their mouse and eyes as they point to the requested object.
        
         - Control condition: candle target and fork competitor. Eyes should go straight to target.
         - Cohort condition: candle target and candy competitor. The first phonemes are the same in "candle" and "candy"
                and so the mouse motion is more complex.
         - Rhyming condition: candle target and handle competitor. 
         
        Here is how Spivey explains it

        > Participants were presented with color images of two objects on a screen (one target and one distractor), 
        and a prerecorded speech file instructed them to click one of them with the mouse. 
        Objects were presented in the upper left and upper right corners of the computer screen 
        (e.g., a candle and a candy, in the cohort condition, or a candle and a jacket, in the control condition). 
        Eight target objects were used to make 32 trials in which the distractor object was either a cohort for 
        the target object or a phonologically dissimilar control and in which the target object was either on the left
        or right side of the display. Participants were instructed to mouse-click a box in the bottom center of 
        the screen when they were ready to begin a trial. At this time, the two object images would appear in 
        the upper left and right, and 500 ms after the onset of the images, a single spoken word 
        (from a speech file on the computer; mean duration, 532 ms) would name the target object. 
        [Imposing this asynchrony between image onset and speech onset grew out of observations from 
        pilot studies in which simultaneous onset caused participants to occasionally wait until 
        the entire word was spoken before beginning their mouse movement. 
        With the spoken word beginning 500 ms after onset of the images, participants usually begin their 
        mouse movement (straight upward) before the onset of the spoken word, which gives distinguishing 
        properties in the acoustic–phonetic input a chance to influence the continuous motor output midflight.


        # How it works, step by step
                 
        1. Network Architecture
        Nodes: Represent words (Lexical layer) and visual objects (Visual layer).
        Layers:
        Lexical (word units)
        Visual (object units)
        Integration (sum of Lexical + Visual)
        Eyes (fixated object)
        Mouse (motor plan)
        All layers have bidirectional connections, allowing feedback.
         
        2. Initialization
        Two objects are present in the display
        Their nodes in the Visual layer are initialized to 1.0, others 0.0.
        The Visual vector is normalized so activations sum to 1.0. (e.g., [0.5, 0.5, 0, 0] for 2 objects.)
         
        3. Phoneme Input (Over Time Steps)
        At each timestep, a phoneme of the target word is input to the Lexical layer.
        Each word node receives 1.0 if the phoneme matches, 0.0 otherwise.
        E.g., “candle” input at time 2 might activate "candle" and "candy" nodes for /k/.
         
        4. Normalize Lexical and Visual Vectors
        After each input, normalize Lexical and Visual vectors so their activations sum to 1.0.
         
        5. Compute the Integration Layer
        Integration = Lexical + Visual (pointwise sum).
        Normalize the Integration vector so it also sums to 1.0.
         
        6. Generate Motor Outputs
        Two motor systems: Eye movements and Mouse movements.
         
        A. Eye Movements:
        Stochastic: Each fixation is sampled from the Visual vector (probability distribution).
        Saccades have a 180 ms refractory period (~3 timesteps).
        Fixation boosts activation in the Eyes vector: fixated object gets 0.55, others 0.45.
         
        B. Mouse Movements:
        Smooth and continuous.
        X-position changes based on activation difference between Target and Competitor in the Mouse vector (a copy of the Visual vector).
        Y-position increases by 50 pixels every timestep (straight up).
        Curvature emerges if activation leans toward the competitor.
         
        7. Feedback to Lexical and Visual Layers
        Feedback is multiplicative:
        This biases the system toward currently active items, especially the fixated object (via Eyes vector).
        After feedback, normalize Lexical and Visual vectors again.
        This keeps competition active (inhibits less relevant nodes).
        
        9. Iterate for Each Timestep
        Repeat steps 3–8 for each timestep in the trial (usually 6 phoneme inputs).
        Track Lexical activations, eye fixations, and mouse path at each step.
        
        10. Output
        Mouse trajectories: 2D path of the mouse over time, showing curvature based on perception and production.
        Overall, this model illustrates how a relatively simple, structured network can simulate the intricate, real-time interplay between language comprehension, 
        visual attention, and motor behavior. By incorporating dynamic feedback loops, probabilistic 
        saccade generation, and continuous motor output, the system not only mirrors behavioral 
        data observed in human participants but also sheds light on the underlying cognitive mechanisms. 
        The visual world localist attractor thus stands as a compelling demonstration of how cognition 
        is not a series of isolated stages, but a fluid, embodied process deeply shaped by perception 
        and action unfolding over time.      
       
        """.trimIndent()
    )


}