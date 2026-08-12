package org.simbrain.custom_sims.simulations.psychology

import kotlinx.coroutines.delay
import org.simbrain.custom_sims.*
import org.simbrain.network.connections.OneToOne
import org.simbrain.network.core.*
import org.simbrain.network.layouts.LineLayout
import org.simbrain.util.place
import org.simbrain.util.plus
import org.simbrain.util.point
import org.simbrain.util.times
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity

/**
 * Based on Spivey's 2024 paper, "A Linking Hypothesis for Eyetracking and Mousetracking
 * in the Visual World Paradigm"
 */
val spiveyNet = newSim {

    workspace.clearWorkspace()

    // Target vs competitor index
    // 0: Candle, 1: Candy, 2: Handle, 3: Fork
    var targetIndex = 0
    var competitorIndex = 0
    var timeIndex = 0
    var conditionText = ""

    // Network
    val networkComponent = addNetworkComponent("Spivey Net")
    val net = networkComponent.network

    val currentStatus = NetworkTextObject("").apply {
        fontSize = 18
    }

    val lexicalNodes = net.addNeuronCollection(4).apply {
        layout = LineLayout()
        applyLayout()
        isClamped = true
        label = "Lexical"
        neuronList.labels = listOf("candle", "candy", "handle", "fork")
    }
    val visualNodes = net.addNeuronCollection(4).apply {
        layout = LineLayout()
        applyLayout()
        isClamped = true
        label = "Visual Attention"
        neuronList.labels = listOf("candle", "candy", "handle", "fork")
    }
    val mouseNodes = net.addNeuronCollection(4).apply {
        layout = LineLayout()
        applyLayout()
        label = "Mouse"
        neuronList.labels = listOf("candle", "candy", "handle", "fork")
    }

    val eyesNodes = net.addNeuronCollection(4).apply {
        layout = LineLayout()
        applyLayout()
        label = "Eyes"
        neuronList.labels = listOf("candle", "candy", "handle", "fork")
    }
    val integrationNodes = net.addNeuronCollection(4).apply {
        layout = LineLayout()
        applyLayout()
        label = "Integration"
        neuronList.labels = listOf("candle", "candy", "handle", "fork")
    }

    net.addNetworkModel(currentStatus)
    lexicalNodes.location = point(-3.70, 12.64)
    visualNodes.location = point(285.98, 5.92)
    mouseNodes.location = point(438.46, 160.33)
    eyesNodes.location = point(144.87, 158.44)
    integrationNodes.location = point(131.67, -134.59)

    val connector = OneToOne().apply {
        percentExcitatory = 100.0
    }
    net.addNetworkModelsAsync(connector.connectNeurons(visualNodes.neuronList, integrationNodes.neuronList))
    net.addNetworkModelsAsync(connector.connectNeurons(lexicalNodes.neuronList, integrationNodes.neuronList))
    net.addNetworkModelsAsync(connector.connectNeurons(visualNodes.neuronList, mouseNodes.neuronList))

    // World
    val oc = addOdorWorldComponent("Screen")
    val world = oc.world
    world.isUseCameraCentering = false
    desktop?.getDesktopComponent(oc)?.title = "Mouse Trace"
    val mouse = world.addEntity(157, 271, EntityType.Hand).apply {
        heading = 90.0
    }
    val targetObject = world.addEntity(38, 49, EntityType.Candle)

    val competitorObject = OdorWorldEntity(world, EntityType.Candy).apply {
        location = point(287, 44)
    }
    world.addEntity(competitorObject)
    mouse.isShowTrail = true

    val eye = OdorWorldEntity(world, EntityType.Eye).apply {
        location = point(145, 157)
    }
    world.addEntity(eye)
    eye.isShowTrail = true

    fun List<Neuron>.normalize() {
        val total = activations.sum()
        if (total != 0.0) {
            forEach { it.activation /= total }
        }
    }

    net.updateManager.clear()
    workspace.addUpdateAction("Spivey Net Update", position = 0) {

        // Assume lexical nodes have been updated by a control panel button
        with(net) {
            lexicalNodes.neuronList.forEach { it.accumulateFanInInputs() }
            lexicalNodes.neuronList.forEach { it.update() }
            lexicalNodes.neuronList.normalize()
            integrationNodes.neuronList.forEach { it.accumulateFanInInputs() }
            integrationNodes.neuronList.forEach { it.update() }
            visualNodes.neuronList.normalize()
            mouseNodes.neuronList.forEach { it.accumulateFanInInputs() }
            mouseNodes.neuronList.forEach { it.update() }
        }

        // Eye movements
        timeIndex++
        // Update eye movements every three iterations
        if (timeIndex % 3 == 0) {
            val candleActivation = visualNodes.neuronList[0].activation
            // Cohort condition. Spivey does it using visual activations.
            if (targetIndex == 0 && competitorIndex == 1) {
                if (Math.random() < candleActivation) {
                    eyesNodes.setActivations(doubleArrayOf(.55, .45, 0.0, 0.0))
                } else {
                    eyesNodes.setActivations(doubleArrayOf(.45, .55, 0.0, 0.0))
                }
            }
            // Control Condition
            if (targetIndex == 0 && competitorIndex == 3) {
                if (Math.random() < candleActivation) {
                    eyesNodes.setActivations(doubleArrayOf(.55, 0.0, 0.0, .45))
                } else {
                    eyesNodes.setActivations(doubleArrayOf(.45, 0.0, 0.0, 0.55))
                }
            }
            // Rhyme condition
            if (targetIndex == 0 && competitorIndex == 2) {
                if (Math.random() < candleActivation) {
                    eyesNodes.setActivations(doubleArrayOf(.55, 0.0, 0.45, 0.0))
                } else {
                    eyesNodes.setActivations(doubleArrayOf(.45, 0.0, 0.55, 0.0))
                }
            }
        }

        lexicalNodes.activations += (integrationNodes.activations * lexicalNodes.activations)
        visualNodes.activations += (integrationNodes.activations * visualNodes.activations) +
                (eyesNodes.activations * visualNodes.activations) +
                (mouseNodes.activations * visualNodes.activations)
        lexicalNodes.neuronList.normalize()
        visualNodes.neuronList.normalize()

        // Move mouse
        val pix = 20.0 // 50 in Spivey's matlab code. Set here by trial and error
        if (mouse.y > 15) {
            mouse.y -= pix
        }
        // mousepos(1)=mousepos(1)+(mouse(1)*-pix)+(sum(mouse(2:4))*pix);
        mouse.x += mouseNodes.neuronList[targetIndex].activation * -pix +
                mouseNodes.neuronList[competitorIndex].activation * pix

        val eyeTargetActivation = eyesNodes.neuronList[0].activation
        val eyeCompetitorActivation = eyesNodes.neuronList.drop(1).sumOf { it.activation }

        val delta = eyeTargetActivation - eyeCompetitorActivation

        eye.location = if (delta > 0.01) {
            targetObject.location
        } else if (delta < -0.01){
            competitorObject.location
        } else {
            eye.location
        }



    }

    suspend fun resetExperiment() {
        timeIndex = 0
        mouse.location = point(157, 271)
        eye.location = point(145, 157)
        networkComponent.network.clearActivations()
        withGui {
            (getDesktopComponent(oc) as OdorWorldDesktopComponent).worldPanel.apply {
                getEntityNode(mouse).startTrailAtCurrentLocation()
                getEntityNode(eye).startTrailAtCurrentLocation()
            }
        }
    }

    suspend fun applyControlCondition() {
        // Candle / Fork
        conditionText = "Control Condition"
        currentStatus.text = conditionText
        targetIndex = 0
        competitorObject.entityType = EntityType.Fork
        competitorIndex = 3
        resetExperiment()
        visualNodes.setActivations(doubleArrayOf(1.0, 0.0, 0.0, 1.0))
    }

    suspend fun applyCohortCondition() {
        // Target = Candle, Competitor = Candy
        conditionText = "Cohort Condition"
        currentStatus.text = conditionText
        resetExperiment()
        targetIndex = 0
        competitorObject.entityType = EntityType.Candy
        competitorIndex = 1
        visualNodes.setActivations(doubleArrayOf(1.0, 1.0, 0.0, 0.0))
    }

    suspend fun applyRhymeCondition() {
        // Candle / Handle
        conditionText = "Rhyme Condition"
        currentStatus.text = conditionText
        targetIndex = 0
        competitorObject.entityType = EntityType.Handle
        competitorIndex = 2
        resetExperiment()
        visualNodes.setActivations(doubleArrayOf(1.0, 0.0, 1.0, 0.0))
    }

    withGui {
        //place(docViewer, 0, 0, 464, 619)
        (getDesktopComponent(oc) as? OdorWorldDesktopComponent)?.worldPanel?.let {
            it.scalingFactor = 0.1
            delay(100L)
            it.clearSelection()
        }

        currentStatus.location = point(220, -240)

        applyControlCondition()

        val controlPanel = createControlPanel("Control Panel", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addButton("Control Condition") {
                applyControlCondition()
            }
            addButton("Cohort Condition") {
                applyCohortCondition()
            }
            addButton("Rhyme Condition") {
                applyRhymeCondition()
            }
            addSeparator()
            addButton("Iterate") {
                mouse.drawTrailWithoutRunningWorkspace = true
                eye.drawTrailWithoutRunningWorkspace = true
                when (timeIndex) {
                    0 -> {
                        lexicalNodes.setActivations(doubleArrayOf(1.0, 1.0, 0.0, 0.0))
                        currentStatus.text = "$conditionText\nLexical: now hearing /k/"
                    }

                    1 -> {
                        lexicalNodes.setActivations(doubleArrayOf(1.0, 1.0, 1.0, 0.0))
                        currentStatus.text = "$conditionText\nLexical: now hearing /a/"
                    }

                    2 -> {
                        lexicalNodes.setActivations(doubleArrayOf(1.0, 1.0, 1.0, 0.0))
                        currentStatus.text = "$conditionText\nLexical: now hearing /n/"
                    }

                    3 -> {
                        lexicalNodes.setActivations(doubleArrayOf(1.0, 1.0, 1.0, 0.0))
                        currentStatus.text = "$conditionText\nLexical: now hearing /d/"
                    }

                    4 -> {
                        lexicalNodes.setActivations(doubleArrayOf(1.0, 0.0, 1.0, 0.0))
                        currentStatus.text = "$conditionText\nLexical: now hearing /l/"
                    }

                    else -> {
                        currentStatus.text = conditionText
                    }
                }
                workspace.iterateSuspend()
                delay(100L)
                mouse.drawTrailWithoutRunningWorkspace = false
            }
            addButton("Reset") {
                currentStatus.text = ""
                resetExperiment()
                net.flatNeuronList.forEach { n -> n.clear() }
                mouse.clearTrail()
                eye.clearTrail()
            }
        }.awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 672, 716)
        place(oc, controlPanel.rightEdgeWithGap() + 672 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 434, 548)
    }

    addSidebarInfo(
        """ 
        # Mouse and Eye Tracking
        
        This is a localist attractor network based on (Spivey, 2025) that simulates mouse trajectories relative to visual and auditory inputs.
         It is a computational model of behavioral studies described in (Spivey et al., 2005). It is not a model of learning
         but rather an illustration of how a relatively simple, structured network can simulate the intricate
         real-time interplay between language comprehension, visual attention, and motor behavior. 
         It shows how cognition  is not a series of isolated stages, but a fluid, embodied process deeply shaped by perception 
         and action unfolding over time.
         
        The visual world localist attractor network is a computational model that serves as a linking hypothesis between 
        internal cognitive processes (specifically in spoken word recognition) and observable motor outputs like 
        eye movements and mouse trajectories in the [Visual World Paradigm](https://people.clas.ufl.edu/jvaldeskroff/files/SecondLang_Chapter.pdf). 
        The core idea is that cognition and action are dynamically interconnected: what we look at or move toward not 
        only reflects but also influences what we're thinking. Spivey's model integrates parallel lexical activations, 
        visual input, and motor output with feedback loops especially from eye position to 
        simulate how these processes unfold over time. 

        This simulation demonstrates how smooth, continuous cognitive activations can produce 
        both abrupt saccadic eye movements and gradually curving mouse paths, even replicating 
        nuanced behavioral patterns seen in experiments (like greater mouse curvature after 
        competitor fixations). By doing so, the model offers a powerful tool to test and refine 
        theories of real-time language processing and perception-action coupling.
        
        # Simulation Details
        
        In the studies in (Spivey et al., 2005) participants were shown two objects and told which to point to.
        The scientists tracked their mouse and eyes as they pointed to the requested objects. This is the kind of data
        that were gathered, shown below.
         
         <img src="//localfiles/simulations/images/visualWorld/spiveyMouseTrace.png" alt="Mouse trace icon" height="200">
        
        There were three main conditions:
        
         1. **Control condition**: candle target and fork competitor. Eyes should go straight to target without being influenced by the competitor,
            because candle and fork have very different phonemic representations. In this condition eye position should settle on the candle quickly,
            with little or no dwell time on the fork. The mouse trace should be close to a straight path to the candle, with little or no bulge
            toward the fork.
         2. **Cohort condition**: candle target and candy competitor. The first four phonemes are the same in "candle" and "candy"
                and so the mouse motion should be drawn slightly towards "candy", because of their shared initial phonemic representations. This
                should produce the strongest outward bulge in the mouse trace, bending it toward the candy before it returns to the candle.
                Eye movements should also show the strongest competition here: early in the trial the eyes may be pulled toward or briefly linger on
                the candy before settling on the candle.
         3. **Rhyming condition**: candle target and handle competitor. There is shared phonemic representation but it is in the final phonemes.
                Thus the mouse trace should be pulled towards the competitor but not as much as in the cohort condition. The trajectory should show
                a smaller, later bulge toward the handle rather than the stronger early bulge seen in the cohort condition. Eye movements should be
                less strongly drawn to the handle than in the cohort condition, and any competitor-directed fixation should tend to appear later.
        
        # What to Do
        
        ## Compare conditions
        
        **To compare mouse traces across conditions, switch directly between `Control`, `Cohort`, and `Rhyming` without pressing `Reset`.**
        Those condition buttons start a new trial but preserve the existing trails, so multiple trajectories can remain visible together.
        
        1. Control Condition
        - Click on the `Control condition` button.
        - Click the `iterate` button repeatedly.
        - Observe the mouse goes straight to the candle, with little or no bulge toward the fork.
        - Observe the eye marker quickly locks on to the candle and usually stays there.
        
        2. Cohort Condition
        - Click on the `Cohort condition` button.
        - Click the `iterate` button repeatedly.
        - Observe the mouse trace bulges most strongly in this condition, bowing toward the candy before returning to the candle.
        - Observe the eye marker shows the strongest competition in this condition, often hesitating between candle and candy before committing.

        3. Rhyming Condition
        - Click on the `Rhyming condition` button.
        - Click the `iterate` button repeatedly.
        - Observe the mouse trace shows a smaller, later bulge toward the handle than the cohort-condition bulge toward candy.
        - Observe the eye marker mostly favors the candle, with any pull toward handle tending to be weaker and later than in the cohort condition.
        
        You can reset at any time using the `reset button` to erase all the trajectories.
        Pressing `Control`, `Cohort`, or `Rhyming` starts over without erasing trajectories, which is the intended way to compare conditions on the same display.  
       
        ## To plot activations 
        
        In (Spivey, 2025), the central figure is Figure 5 which shows time series plots for activations in different parts of the network,
        which illustrates the dynamics governing the mouse traces. Shown below.
        
        <img src="//localfiles/simulations/images/visualWorld/spiveyActivationDynamics.png" alt="Activation dynamics icon" height="200">
        
        This can also be simulated in Simbrain (but note the colors and details will be slightly different)
        
        1. Right-click on the interaction box (the yellow box of the groups of nodes) of whatever network is of interest (eyes, integration, etc).  
        1. Select `Plot > Time Series Plot` from the dropdown.
        1. Repeat the steps above (selecting a condition then iterating) to see activation dynamics in the time series.
        1. There will generally be four lines corresponding to the four nodes in each neuron group.
        
        # References
        
        Spivey, M. J. (2025). [_A linking hypothesis for eyetracking and mousetracking in the visual world paradigm_](https://www.sciencedirect.com/science/article/pii/S0006899325000356). _Brain Research_, 149477.
        
        Spivey, M. J., Grosjean, M., & Knoblich, G. (2005). [_Continuous attraction toward phonological competitors_](https://pmc.ncbi.nlm.nih.gov/articles/PMC1177386/). _Proceedings of the National Academy of Sciences_, _102_(29), 10393-10398.

        # Credits
        
        [Suzanne Garcia](https://www.linkedin.com/in/suzanne-garcia5537bb293)
        
        Olivia Gawel
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
       
        """.trimIndent()
    )


}



// Ask Spivey for things we should look for, esp in the feedback
//1. Network Architecture
//Nodes: Represent words (Lexical layer) and visual objects (Visual layer).
//Layers:
//Lexical (word units)
//Visual (object units)
//Integration (sum of Lexical + Visual)
//Eyes (fixated object)
//Mouse (motor plan)
//All layers have bidirectional connections, allowing feedback.
//
//2. Initialization
//Two objects are present in the display
//Their nodes in the Visual layer are initialized to 1.0, others 0.0.
//The Visual vector is normalized so activations sum to 1.0. (e.g., [0.5, 0.5, 0, 0] for 2 objects.)
//
//3. Phoneme Input (Over Time Steps)
//At each timestep, a phoneme of the target word is input to the Lexical layer.
//Each word node receives 1.0 if the phoneme matches, 0.0 otherwise.
//E.g., “candle” input at time 2 might activate "candle" and "candy" nodes for /k/.
//
//4. Normalize Lexical and Visual Vectors
//After each input, normalize Lexical and Visual vectors so their activations sum to 1.0.
//
//5. Compute the Integration Layer
//Integration = Lexical + Visual (pointwise sum).
//Normalize the Integration vector so it also sums to 1.0.
//
//6. Generate Motor Outputs
//Two motor systems: Eye movements and Mouse movements.
//
//A. Eye Movements:
//Stochastic: Each fixation is sampled from the Visual vector (probability distribution).
//Saccades have a 180 ms refractory period (~3 timesteps).
//Fixation boosts activation in the Eyes vector: fixated object gets 0.55, others 0.45.
//
//B. Mouse Movements:
//Smooth and continuous.
//X-position changes based on activation difference between Target and Competitor in the Mouse vector (a copy of the Visual vector).
//Y-position increases by 50 pixels every timestep (straight up).
//Curvature emerges if activation leans toward the competitor.
//
//7. Feedback to Lexical and Visual Layers
//Feedback is multiplicative:
//This biases the system toward currently active items, especially the fixated object (via Eyes vector).
//After feedback, normalize Lexical and Visual vectors again.
//This keeps competition active (inhibits less relevant nodes).
//
//9. Iterate for Each Timestep
//Repeat steps 3–8 for each timestep in the trial (usually 6 phoneme inputs).
//Track Lexical activations, eye fixations, and mouse path at each step.
//
//10. Output
//Mouse trajectories: 2D path of the mouse over time, showing curvature based on perception and production.
//Overall, this model illustrates how a relatively simple, structured network can simulate the intricate, real-time interplay between language comprehension,
//visual attention, and motor behavior. By incorporating dynamic feedback loops, probabilistic
//saccade generation, and continuous motor output, the system not only mirrors behavioral
//data observed in human participants but also sheds light on the underlying cognitive mechanisms.
//The visual world localist attractor thus stands as a compelling demonstration of how cognition
//is not a series of isolated stages, but a fluid, embodied process deeply shaped by perception
//and action unfolding over time.
