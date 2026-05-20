package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.simulations.hebb.HopfieldTestConfig
import org.simbrain.custom_sims.simulations.hebb.createHopfieldTestPane
import org.simbrain.custom_sims.simulations.hebb.createPatternControlPanel
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.network.subnetworks.Hopfield.HopfieldUpdate
import org.simbrain.util.place
import org.simbrain.util.showNumericInputDialog

/**
 * Demo for studying discrete Hopfield networks,
 */

val discreteHopfieldSim = newSim {

    val numNeurons = showNumericInputDialog(message = "Number of neurons", initValue = 100) ?: return@newSim

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Hopfield network
    val hopfield = Hopfield(numNeurons).apply {
        updateFunc = HopfieldUpdate.SYNC
        customInfo.fontSize = 24
    }
    network.addNetworkModelAsync(hopfield)

    // Text to potentially integrate
    // Select an input pattern and click the train button on the Control panel to train the network on the selected pattern.
    // The model learns the pattern and “remembers” it. When randomizing the network (by clicking “N” [Neuron], “R” [Randomize], and “Space” [Iterate], or using “I” [Wand Mode] over the nodes), the network adjusts the nodes on each iteration to reconfigure the inputted pattern.
    // The Network remembers the pattern and the antipattern, and when iterating (“Space”), it iterates to recreate the pattern with the most similar nodes.
    // You can get the pattern to memorize all the different patterns and antipatterns by training each one, randomizing and iterating to see if it is remembered, and training that pattern again if it needs to be learned.
    addSidebarInfo(
        """ 
            # Discrete Hopfield
            
            [Hopfield networks](https://en.wikipedia.org/wiki/Hopfield_network) are recurrent networks often used for pattern recognition and to model memory 
            retrieval. In this simulation, you can test the network's ability to store and retrieve memories in the form of activation patterns.

            # Simulation Details

            The network stores patterns as attractor states. After training, partial or noisy versions of a stored pattern should settle back toward the learned memory.
            
            # What to Do         
            
            Select one of the input patterns from the button panel (`Circle`, `Square`, `Diagonal Line`, `Cross`, `Vertical Line`, or `Horizontal Line`) and press `Train on current pattern` to train the network on that pattern. Each time you press it, the pattern is reinforced into the network's memory. Note that the network learns both the pattern and its anti-pattern (the version with all activations flipped).
            
            To confirm the pattern is remembered, randomize the network by pressing `N -> R` and then iterate by pressing `Space` to see if the pattern is recreated. You can also manually create part of a pattern using the wand tool (press `D` to activate) and see if the network completes it. The `Random Pattern` button generates a new random activation pattern, and `-1 Canvas` sets all neurons to `-1`.
            
            The `Training iterations` field controls how many learning steps occur when you press `Train on current pattern`. Increasing this value strengthens the memory trace more quickly.
            
            ## Training on Multiple Patterns
            
            Hopfield networks have a memory capacity of about 14% of the number of nodes. In this case, about 8 memory states. However, those memories need to be sufficiently distinct. The network should be able to learn all 6 provided patterns, but you must carefully train it on them by selecting each pattern and pressing `Train on current pattern` multiple times. If patterns are too similar or you store too many, the network may converge to spurious states or fail to recall correctly.
            
            ## Other things to observe
            
            When you iterate the network it tends to go to lower energy states.
            
            ## Memory Capacity Testing
            
            The Capacity tab provides tools to systematically test how many patterns the network can reliably store and retrieve. You can run automated tests that measure recall success rates across different numbers of stored patterns, with or without forgetting dynamics. A slider lets you explore individual test patterns, and the Capacity Test button launches the full analysis.
            
            ### Background on Memory Capacity
            
            Hopfield networks have a finite storage capacity—the maximum number of distinct patterns they can reliably store and recall. 
            The classic result shows that a network with N neurons can stably store approximately 0.138N patterns before performance degrades 
            (Amit et al., 1985). For example, a 100-neuron network can store about 13-14 uncorrelated patterns. Beyond this limit, the attractor 
            landscape becomes crowded, leading to spurious states and retrieval errors.
            
            This simulation extends the classical capacity analysis by examining how forgetting mechanisms affect memory stability. While traditional 
            models focus on interference from new memories overwriting old ones (palimpsest networks), this approach isolates forgetting as a 
            standalone process using two biologically plausible mechanisms: weight decay (synaptic weakening over time) and synaptic plasticity 
            noise (random fluctuations in connection strengths). These mechanisms reflect the intrinsic instability of biological synapses in the 
            absence of reactivation (Mongillo et al., 2017; Susman et al., 2019).
            
            Recent work has shown that forgetting in attractor networks can produce diverse dynamics. Gilbert (2024) demonstrated that weight decay 
            alone is insufficient to disrupt recall—proportional scaling preserves the relative structure of stored memories. However, adding 
            synaptic noise destabilizes attractors and generates realistic forgetting curves that are often linear or piecewise-linear rather than 
            exponential, consistent with behavioral data on meaningful memory (Radvansky et al., 2022).
            
            ### How the Capacity Test Works
            
            The Capacity tab provides tools to systematically study how many patterns a Hopfield network can reliably store and retrieve, 
            both with and without forgetting dynamics.
            
            1. **Pattern Generation**: When the control panel is created, a fixed set of random patterns is generated (one pattern for each neuron in the network). 
            This ensures reproducible results across test runs.
            
            2. **Running the Test**: Click the `Capacity Test` button to open a configuration dialog where you can set:
               - `Distance threshold`: Maximum allowable distance between recalled and original pattern (as a percentage). For discrete Hopfield, this uses Hamming distance.
               - `Percent to test`: What percentage of the network size to use as the number of test patterns
               - `Cue distance`: How much to perturb the pattern when testing retrieval (Hamming distance for discrete Hopfield)
               - `Test iterations`: How many iterations to run when testing pattern recall
               - `Forgetting options`: Enable forgetting with decay rates, perturbation, and forgetting iterations
            
            3. **Test Process**: For each number of patterns from 1 to the specified maximum:
               - Reset the network and weights
               - Set learning rate to 1/n where n is the number of patterns being tested
               - Train the network on n patterns
               - Apply forgetting process if enabled (decay weights, add noise, repeat for specified iterations)
               - Test each pattern by presenting a perturbed version as a cue
               - Run the network for the specified number of iterations
               - Check if the network settles within the distance threshold of the original pattern
               - Plot the percentage of successfully recalled patterns
            
            4. **Results**: The test generates a time series plot showing:
               - Blue line: Percentage of patterns successfully recalled without forgetting
               - Orange line (if forgetting enabled): Percentage of patterns successfully recalled with forgetting
               
            The x-axis shows the number of patterns stored, and the y-axis shows the recall success rate (0-100%).
            
            ### Exploring Individual Patterns
            
            Use the slider in the Capacity tab to load specific patterns from the test set into the network. This lets you manually 
            examine individual patterns and see how the network responds to them.
            
            # References
            
            Amit, D. J., Gutfreund, H., & Sompolinsky, H. (1985). [_Storing infinite numbers of patterns in a spin-glass model of neural networks_](https://doi.org/10.1103/PhysRevLett.55.1530). _Physical Review Letters_, _55_(14), 1530–1533.
            
            Gilbert, M. (2024). [_Modeling Forgetting with Attractor Neural Networks_](https://escholarship.org/content/qt9fb9d61n/qt9fb9d61n.pdf). Cognitive and Information Sciences, University of California Merced.
            
            Hopfield, J. J. (1982). [_Neural networks and physical systems with emergent collective computational abilities_](https://doi.org/10.1073/pnas.79.8.2554). _Proceedings of the National Academy of Sciences_, _79_(8), 2554–2558.
            
            Mongillo, G., Rumpel, S., & Loewenstein, Y. (2017). [_Intrinsic volatility of synaptic connections—a challenge to the synaptic trace theory of memory_](https://doi.org/10.1016/j.conb.2017.06.006). _Current Opinion in Neurobiology_, _46_, 7–13.
            
            Radvansky, G. A., Doolen, A. C., Pettijohn, K. A., & Ritchey, M. (2022). [_A new look at memory retention and forgetting_](https://doi.org/10.1037/xlm0001110). _Journal of Experimental Psychology: Learning, Memory, and Cognition_, _48_(11), 1698–1723.
            
            Susman, L., Brenner, N., & Barak, O. (2019). [_Stable memory with unstable synapses_](https://doi.org/10.1038/s41467-019-12306-2). _Nature Communications_, _10_, 4441.
                
            # Credits
            
            [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
            
            Makenzy Gilbert
        
        """.trimIndent()
    )

    withGui {
        place(networkComponent, 249, 0, 509, 619)

        var numTrainIterations = 1

        createPatternControlPanel(hopfield.neuronGroup, false) {
            hopfield.randomize()
        }?.apply {
            addTextField("Training iterations", "" + numTrainIterations) {
                it.toIntOrNull()?.let { num ->
                    numTrainIterations = num
                }
            }
            addButton("Train on current pattern") {
                with(network) { hopfield.trainOnCurrentPattern() }
            }
            val config = HopfieldTestConfig(
                workspace = workspace,
                hopfield = hopfield.neuronGroup,
                weights = hopfield.weightMatrix,
                applyTraining = { with(network) { hopfield.trainOnCurrentPattern()} },
                applyLearningRate = { hopfield.learningRate = it },
                applyReset = {
                    hopfield.clear()
                    hopfield.weightMatrix.hardClear()
                }
            )
            createHopfieldTestPane(config, true)
        }
    }

}
