package org.simbrain.custom_sims.simulations.imageworld

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.OneToOne
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.toGrayScaleImage

val attentionAsGain = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Attention Network")
    val network = networkComponent.network

    // Create image world with drawable canvas
    val imageWorldComponent = addImageWorld("Visual Input")
    val w = 24
    val h = 24
    val halfSize = (w * h) / 2  
    val imageWorld = imageWorldComponent.world.apply {
        resetImageAlbum(w, h)
        setCurrentPipeline("Threshold 30x15")
        
        // Pre-load some example images
        
        // Image 1: Left side pattern (vertical bars on left)
        imageAlbum.addImage(
            DoubleArray(w * h) {
                val col = it % w
                if (col < w / 2 && col % 2 == 0) 1.0 else 0.0
            }.toGrayScaleImage(w, h)
        )
        
        // Image 2: Right side pattern (vertical bars on right)
        imageAlbum.addImage(
            DoubleArray(w * h) {
                val col = it % w
                if (col >= w / 2 && col % 2 == 0) 1.0 else 0.0
            }.toGrayScaleImage(w, h)
        )
        
        // Image 3: Both sides pattern (vertical bars on both sides)
        imageAlbum.addImage(
            DoubleArray(w * h) {
                val col = it % w
                if (col % 2 == 0) 1.0 else 0.0
            }.toGrayScaleImage(w, h)
        )
        
        // Image 4: Horizontal line across both sides
        imageAlbum.addImage(
            DoubleArray(w * h) {
                val row = it / w
                if (row == h / 2) 1.0 else 0.0
            }.toGrayScaleImage(w, h)
        )
        
        // Image 5: Blank canvas for drawing
        imageAlbum.addImage(
            DoubleArray(w * h) { 0.0 }.toGrayScaleImage(w, h)
        )

        // Remove the default image when the album was first created
        imageAlbum.setFrame(0)
        imageAlbum.deleteCurrentImage()
    }

    val imageInput = network.addNeuronCollection(w*h).apply {
        label = "Visual Input"
        isClamped = true
        setUpperBound(1.0)
        layout(GridLayout(40.0, 40.0, w))
    }

    // Left region neurons - process left half of image
    val leftRegion = network.addNeuronCollection(halfSize) {
        updateRule = LinearRule().apply {
            clippingType = LinearRule.ClippingType.Relu
            slope = 1.0
        }
    }.apply {
        label = "Left Region (Unattended)"
        layout(GridLayout(40.0, 40.0, w / 2))
    }

    // Right region neurons - process right half of image
    val rightRegion = network.addNeuronCollection(halfSize) {
        updateRule = LinearRule().apply {
            clippingType = LinearRule.ClippingType.Relu
            slope = 1.0
        }
    }.apply {
        label = "Right Region (Unattended)"
        layout(GridLayout(40.0, 40.0, w / 2))
    }

    // Connect left half of input to left region (one-to-one)
    val leftConnection = OneToOne().apply {
        percentExcitatory = 100.0
    }.connectNeurons(
        imageInput.neuronList.filterIndexed { index, _ -> index % w < w / 2 },
        leftRegion.neuronList
    )
    network.addNetworkModels(leftConnection)

    // Connect right half of input to right region (one-to-one)
    val rightConnection = OneToOne().apply {
        percentExcitatory = 100.0
    }.connectNeurons(
        imageInput.neuronList.filterIndexed { index, _ -> index % w >= w / 2 },
        rightRegion.neuronList
    )
    network.addNetworkModels(rightConnection)

    // Layout the network with specific positions
    imageInput.location = point(0.0, -500.0)
    leftRegion.location = point(-500.0, 500.0)
    rightRegion.location = point(500.0, 500.0)

    // Coupling
    with(couplingManager) {
        imageWorld.imagePipelineCollection.currentPipeline.let { pipeline ->
            createCoupling(
                pipeline.getProducer(pipeline::brightness),
                imageInput.getConsumer(imageInput::activationArray)
            )
        }
    }

    // Time series for monitoring activations (commented out for now)
    // val (leftPlotComponent, leftSeries) = addTimeSeries("Left Region Activity", seriesNames = listOf("Average Activation"))
    // leftPlotComponent.model.apply {
    //     isAutoRange = true
    //     fixedWidth = false
    // }

    // val (rightPlotComponent, rightSeries) = addTimeSeries("Right Region Activity", seriesNames = listOf("Average Activation"))
    // rightPlotComponent.model.apply {
    //     isAutoRange = true
    //     fixedWidth = false
    // }

    // Custom coupling to plot average activations
    // workspace.addUpdateAction("Plot Average Activations") {
    //     val leftAvg = leftRegion.neuronList.map { it.activation }.average()
    //     val rightAvg = rightRegion.neuronList.map { it.activation }.average()
    //     leftSeries.setValue(leftAvg)
    //     rightSeries.setValue(rightAvg)
    // }

    var leftGain = 1.0
    var rightGain = 1.0

    fun updateGains() {
        leftRegion.neuronList.forEach { neuron ->
            (neuron.updateRule as? LinearRule)?.slope = leftGain
        }
        rightRegion.neuronList.forEach { neuron ->
            (neuron.updateRule as? LinearRule)?.slope = rightGain
        }
        
        // Update labels to reflect attention state
        when {
            leftGain > rightGain + 0.1 -> {
                leftRegion.label = "Left Region (ATTENDED)"
                rightRegion.label = "Right Region (Unattended)"
            }
            rightGain > leftGain + 0.1 -> {
                leftRegion.label = "Left Region (Unattended)"
                rightRegion.label = "Right Region (ATTENDED)"
            }
            else -> {
                leftRegion.label = "Left Region (Equal Attention)"
                rightRegion.label = "Right Region (Equal Attention)"
            }
        }
    }

    withGui {
        // Control panel on the left
        createControlPanel("Attention Control", 10, 10) {
            addSlider("Left Gain", 0.1, 5.0, leftGain, 0.1) { value ->
                leftGain = value
                updateGains()
            }
            
            addSlider("Right Gain", 0.1, 5.0, rightGain, 0.1) { value ->
                rightGain = value
                updateGains()
            }
            
            addSeparator()
            
            addButton("Image 1: Left bars") {
                imageWorld.imageAlbum.setFrame(0)
            }
            
            addButton("Image 2: Right bars") {
                imageWorld.imageAlbum.setFrame(1)
            }
            
            addButton("Image 3: Both sides") {
                imageWorld.imageAlbum.setFrame(2)
            }
            
            addButton("Image 4: Horizontal line") {
                imageWorld.imageAlbum.setFrame(3)
            }
            
            addButton("Image 5: Blank canvas") {
                imageWorld.imageAlbum.setFrame(4)
            }
            
            addSeparator()
            
            addButton("Reset Network") {
                network.clearActivations()
            }
        }
        
        // Place components after control panel is created
        place(networkComponent, 220, 10, 650, 600)
        place(imageWorldComponent, 880, 10, 500, 600)
        // place(leftPlotComponent, 10, 620, 550, 250)
        // place(rightPlotComponent, 570, 620, 550, 250)
    }

    addSidebarInfo(
        """
        # Attention as Gain
        
        This simulation demonstrates a fundamental principle in visual neuroscience: **attention acts as a gain modulation mechanism**. 
        By adjusting the gain (slope) of neurons processing different spatial regions, we can simulate how attention 
        amplifies neural responses to attended stimuli.
        
        # Simulation Details
        
        In neuroscience, "gain" refers to the multiplicative factor applied to neural responses. When you attend to a 
        particular location or feature in the visual field, neurons responding to that location show increased gain - 
        they respond more strongly to the same stimulus. This is not an additive effect (adding a constant) but a 
        multiplicative one (scaling the response).
        
        The equation is simple: `output = gain × input`
        
        Higher gain → stronger response to the same input → better signal for attended stimuli.
        
        The image is split into left and right regions whose gains can be controlled independently.
        
        The gain/slope parameter directly controls how strongly each region responds to its input.
        
        # What to Do
        
        ### Quick Start with Pre-loaded Images
        
        1. Click `Image 1: Left bars` to load vertical bars on the left side
        2. Move the `Left Gain` slider to `4.0` or `5.0`
        3. Iterate the workspace (play button or spacebar) and observe the left region neurons activate strongly
        4. Move the `Left Gain` slider back to `1.0` and see how the response drops
        5. Try `Image 2: Right bars` and experiment with the `Right Gain` slider
        
        ### Compare Attended vs Unattended
        
        1. Click `Image 3: Both sides` to load a pattern spanning both regions
        2. Set `Left Gain` to `4.0` and `Right Gain` to `1.0` - observe the left region shows strong activation
        3. Reverse it: `Left Gain` to `1.0` and `Right Gain` to `4.0` - the relationship flips
        4. Set both sliders to `4.0` - both regions show strong responses
        5. Set both sliders to `1.0` - both regions show baseline responses
        
        ### Manual Gain Control
        
        1. Use the `Left Gain` and `Right Gain` sliders to set arbitrary gain values
        2. Try gain = `0.5` vs gain = `3.0` to see a `6`-fold difference in response
        3. Observe how neuron group labels update to show attention state
        4. Watch the network visualization - attended regions have higher activations (brighter neuron colors)
        
        ### Draw Your Own Patterns
        
        1. Click `Image 5: Blank canvas` to get a clean slate
        2. Use the drawing tools in the Image World window to create your own patterns
        3. Try drawing on just the left or right side, or create patterns that span both
        4. Experiment with gain settings to see how attention modulates your custom patterns
        
        ## Things to Notice
        
        - **Multiplicative effect**: A neuron receiving input of `0.5` produces output of `0.5` with gain=`1`, but `2.0` with gain=`4`
        - **Spatial selectivity**: Attention can be directed to specific spatial locations independently
        - **Response amplification**: Same stimulus → different neural response based on attention state
        - **Zero inputs stay zero**: Gain modulation only affects neurons receiving input (ReLU ensures no negative activations)
        
        ## Pre-loaded Images
        
        - **Image 1**: Vertical bars on the left side (tests left region)
        - **Image 2**: Vertical bars on the right side (tests right region)
        - **Image 3**: Vertical bars on both sides (tests both regions simultaneously)
        - **Image 4**: Horizontal line across both sides (equal input to both regions)
        - **Image 5**: Blank canvas for custom drawings
        
        ## Extensions
        
        This simulation provides a foundation for exploring other attention mechanisms:
        
        - **Feature-based attention**: Multiple channels tuned to different features (color, orientation, motion)
        - **Normalization models**: Add divisive normalization to implement response vs contrast gain
        - **Attentional blink**: Time-varying gain to show temporal attention limits
        - **Cueing effects**: Pre-cues that set gain before stimulus appears
        - **Attention with noise**: Add noise to inputs to demonstrate signal-to-noise improvements
        - **Competitive attention**: Limited gain resources that must be allocated between regions
        
        # References
        
        Reynolds, J. H., & Heeger, D. J. (2009). [*The normalization model of attention*](https://doi.org/10.1016/j.neuron.2009.01.002). _Neuron_, _61_(2), 168-185.
        
        Maunsell, J. H., & Treue, S. (2006). [*Feature-based attention in visual cortex*](https://doi.org/10.1016/j.tins.2006.04.001). _Trends in Neurosciences_, _29_(6), 317-322.
        
        Carandini, M., & Heeger, D. J. (2012). [*Normalization as a canonical neural computation*](https://doi.org/10.1038/nrn3136). _Nature Reviews Neuroscience_, _13_(1), 51-62.
        
        Martinez-Trujillo, J. C., & Treue, S. (2002). [*Attentional modulation strength in cortical area MT depends on stimulus contrast*](https://doi.org/10.1016/S0896-6273(02)00778-X). _Neuron_, _35_(2), 365-370.
        
        # Credits
        
        [Jeff Yoshimi](https://jeffyoshimi.net)
        
        """.trimIndent()
    )
}
