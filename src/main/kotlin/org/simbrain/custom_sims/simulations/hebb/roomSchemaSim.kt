package org.simbrain.custom_sims.simulations
import org.simbrain.custom_sims.*
import org.simbrain.custom_sims.simulations.hebb.TrainedPatterns
import org.simbrain.custom_sims.simulations.hebb.showPatternCorrelations
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.util.place
import org.simbrain.util.runWithProgressWindow
import kotlin.random.Random

/**
 * Demo for studying Room Schema From PDP Chapter 14.
 */

val roomSchemaSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val labels = arrayOf(
        "ceiling", "large", "telephone", "books", "sofa", "drapes",
        "cupboard", "toilet", "walls", "medium", "bed", "desk-chair",
        "easy-chair", "stove", "sink", "scale", "door", "small",
        "typewriter", "clock", "coffee-cup", "coffeepot", "dresser", "oven",
        "windows", "very-small", "bookshelf", "picture", "ashtray", "refrigerator",
        "television", "computer", "very-large", "desk", "carpet", "floor-lamp",
        "fireplace", "toaster", "bathtub", "clothes-hanger"
    )

    // Competitive network
    val rbm = RestrictedBoltzmannMachine(labels.size, 64)
    network.addNetworkModel(rbm)
    rbm.visibleLayer.circleMode = true
    rbm.visibleLayer.offset(-100.0, 0.0)

    rbm.visibleLayer.labelArray = labels

    fun activateNode(label: String) = rbm.visibleLayer.labelArray
        .indexOf(label)
        .let { index ->
            if (index < 0) null else index
        }?.let { index ->
            rbm.visibleLayer.activations[index, 0] = 1.0
        }

    fun activateNodes(labels: List<String>) {
        rbm.visibleLayer.clear()
        labels.forEach { activateNode(it) }
        rbm.visibleLayer.events.updated.fire()
    }

    val rooms = mapOf(
        "Kitchen" to listOf(
            "oven", "coffeepot", "cupboard", "toaster", "refrigerator", "sink", "stove", "drapes",
            "coffee-cup", "clock", "telephone", "small", "windows", "walls", "ceiling"
        ),
        "Office" to listOf(
            "computer", "ashtray", "coffee-cup", "picture", "desk-chair", "books", "carpet",
            "bookshelf", "typewriter", "telephone", "desk", "large", "door", "walls", "ceiling"
        ),
        "Bathroom" to listOf("scale", "toilet", "bathtub", "cupboard", "sink", "very-small", "door", "walls", "ceiling"),
        "Living Room" to listOf(
            "television", "drapes", "fireplace", "easy-chair", "sofa", "floor-lamp", "picture",
            "clock", "books", "carpet", "bookshelf", "telephone", "very-large", "windows", "door",
            "walls", "ceiling"
        ),
        "Bedroom" to listOf(
            "clothes-hanger", "television", "dresser", "drapes", "picture", "clock", "books",
            "carpet", "bookshelf", "bed", "medium", "windows", "door", "walls", "ceiling"
        ),
    )
    // activateNode silently skips unknown labels, so catch misspelled room features here
    rooms.values.flatten().forEach { require(it in labels) { "Unknown room feature: $it" } }
    val roomPatterns = rooms.mapValues { (_, features) ->
        DoubleArray(labels.size) { if (labels[it] in features) 1.0 else 0.0 }
    }
    val trainedPatterns = TrainedPatterns(roomPatterns, 0.5)

    fun flipBitWithChance(bit: Int, chance: Double): Int {
        return if (Random.nextDouble() < chance) 1 - bit else bit
    }

    withGui {
        val controlPanel = createControlPanel("Control Panel", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            for ((room, features) in rooms) {
                addButton(room) {
                    activateNodes(features)
                }.apply {
                    toolTipText = "Activate the features of a typical ${room.lowercase()} on the visible layer."
                }
            }
            addSeparator()
            addButton("Show correlations") {
                showPatternCorrelations(roomPatterns)
            }.apply {
                toolTipText = "Show how strongly each pair of room patterns overlaps."
            }
            addButton("Show trained correlations") {
                trainedPatterns.showCorrelations()
            }.apply {
                toolTipText = "Show how strongly each pair of patterns trained so far overlaps."
            }
            addSeparator()
            addButton("Train on Current Pattern") {
                trainedPatterns.record(rbm.visibleLayer.activationArray)
                with(network) {
                    runWithProgressWindow(20, batchSize = 10) {
                        rbm.trainOnCurrentPattern()
                    }
                }
            }.apply {
                toolTipText = "Run 20 RBM training steps on the room features currently active on the visible layer."
            }
            addButton("Recall Current Pattern") {
                workspace.iterateSuspend(10)
            }.apply {
                toolTipText = "Run the network for 10 steps so it settles toward a stored room schema."
            }
            addButton("Permute Current Pattern") {
                rbm.visibleLayer.activationArray = rbm.visibleLayer
                    .activationArray
                    .map { flipBitWithChance(it.toInt(), 0.1).toDouble() }
                    .toDoubleArray()
            }.apply {
                toolTipText = "Flip each visible feature with 10% probability, to test whether recall restores the room."
            }
        }.awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 800, 600)
    }

    addSidebarInfo(
        """
        # Room Schema
        
        This simulation demonstrates how room schemas emerge from constraint satisfaction in a Restricted Boltzmann Machine (RBM). It is based on the classic room schema example 
        from Chapter `14` of _Parallel Distributed Processing_ by Rumelhart, McClelland, and Hinton.
        
        The simulation shows how our knowledge of different types of rooms (kitchen, office, bathroom, living room, bedroom) can be represented as patterns of activation over 
        simple descriptive features. Rather than storing explicit schemas in memory, the system learns statistical relationships between room features and can reconstruct complete 
        room descriptions from partial information.
        
        ## Background
        
        This simulation illustrates an alternative to traditional symbolic approaches to knowledge representation. Instead of storing room schemas as explicit frames or scripts, 
        knowledge emerges from the collective behavior of simple processing units. This approach naturally handles the flexibility and context-sensitivity that make schemas so 
        useful for human cognition.
        
        The original room schema example helped establish parallel distributed processing as a viable alternative to symbolic AI, showing how high-level cognitive phenomena could 
        emerge from low-level statistical patterns.

        # Simulation Details
        
        The network contains `40` units representing descriptors like `ceiling`, `telephone`, `sofa`, `stove`, `sink`, etc. The connections between units encode the statistical 
        relationships found in typical rooms. For example, `stove` and `refrigerator` have strong positive connections because they tend to appear together in kitchens.
        
        The system uses a Restricted Boltzmann Machine with:
        - `40` visible units (room descriptors)  
        - `64` hidden units that learn to detect room-type patterns
        - Symmetric connections that create constraint satisfaction dynamics
        
        When you activate a few descriptive features (like "oven"), the network settles into a stable state that represents the most typical room containing those features. 
        This demonstrates how schemas emerge from the interaction of many simple constraints rather than being stored as explicit data structures.

        # What to Do
        
        1. Explore room types: Click the room buttons (`Kitchen`, `Office`, `Bathroom`, `Living Room`, `Bedroom`) to see the typical features of each room type. Click `Show correlations` to see how much the room patterns overlap. `Living Room` and `Bedroom` overlap the most, while the other rooms are nearly uncorrelated. `Show trained correlations` does the same for the patterns you have trained, labelling a permuted room like `Kitchen ~3`
        
        2. Train the network: After setting a room pattern, click `Train on Current Pattern` to strengthen the associations. The network learns by adjusting connection weights 
        based on which features tend to co-occur
        
        3. Test pattern completion: Click `Permute Current Pattern` to randomly flip some features, then click `Recall Current Pattern` to see if the network can restore the 
        original room pattern
        
        4. Experiment with partial cues: Manually activate individual units by clicking on them, then use `Recall Current Pattern` to see what complete room the network constructs 
        from those cues

        The network demonstrates several key properties of schemas:
        - Default values: Missing features are filled in with typical values for that room type
        - Context sensitivity: The same feature (like `telephone`) takes on different meanings in different room contexts  
        - Graceful degradation: Partial or noisy input still leads to reasonable room interpretations
        - Flexible structure: The same underlying network can represent multiple different room types


        # References
        
        Rumelhart, D. E., McClelland, J. L., & the PDP Research Group (1986). [_Chapter 14: Schemata and Sequential Thought Processes in PDP Models_](https://web.stanford.edu/group/pdplab/pdphandbook/). In _Parallel Distributed Processing: Volume 1_.

        # Credits
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        Makenzy Gilbert
        
        """.trimIndent()
    )

}
