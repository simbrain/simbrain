package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.subnetworks.SOMNetwork
import org.simbrain.util.SmellSource
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.place
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.SmellSensor

/**
 * Demo of SOM network learning to classify smells from flowers and cheeses.
 * 
 * The mouse agent moves around an environment with different objects (cheeses and flowers),
 * each emitting a unique 9-dimensional smell vector. The SOM network learns to organize
 * these smells, with similar smells (e.g., different cheeses) activating nearby neurons.
 * 
 * As the simulation runs, move the mouse near objects (within radius 50) and watch the SOM
 * neurons automatically get labeled based on which object is nearby.
 */
val somNetSmells = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("SOM Network")
    val network = networkComponent.network

    val somNet = SOMNetwork(9, 16)
    network.addNetworkModel(somNet)
    somNet.applySimulationLayout(somNeuronInterval = SOM_SMELLS_SOM_INTERVAL)
    somNet.inputLayer.apply {
        setUpperBound(1.0)
        isClamped = false
    }
    
    somNet.initialLearningRate = 0.06
    somNet.initNeighborhoodSize = 100.0
    
    somNet.modelList.get<SynapseGroup>().first().displaySynapses = false
    
    val labelTracker = WinnerLabeler()

    val oc = addOdorWorldComponent("Flowers and Cheeses")
    val world = oc.world
    
    val mouse = world.addEntity(140, 205, EntityType.Mouse)
    
    val smellCenter = SmellSensor("Smell-Center", theta = 0.0, radius = 0.0).apply {
        smellVector = DoubleArray(9)
    }
    mouse.addSensor(smellCenter)

    mouse.addEffector(StraightMovement())
    mouse.addEffector(Turning(1.0)) // Left turn
    mouse.addEffector(Turning(-1.0)) // Right turn
    
    // Add cheeses with their smell sources (extracted from original XML)
    val swiss = world.addEntity(36, 107, EntityType.Swiss)
    swiss.smellSource = SmellSource(doubleArrayOf(
        0.2843023824819105, 0.7969005568449742, -0.1869959701309641,
        -0.03930965224307115, 0.053630097604160105, 0.2719959853127682,
        -0.11784800055216271, -0.017583042347626886, -0.18146609193569982
    )).apply {
        decayFunction = GaussianDecayFunction()
    }
    
    val gouda = world.addEntity(169, 32, EntityType.Gouda)
    gouda.smellSource = SmellSource(doubleArrayOf(
        0.15685293544672108, 1.221526823511768, -0.04346913373941728,
        0.003500259993022259, 0.22820144543044077, -0.05772506145969827,
        0.007412608834111722, 0.16480447914896104, 0.005611294738952099
    )).apply {
        decayFunction = GaussianDecayFunction()
    }
    
    val blueCheese = world.addEntity(284, 90, EntityType.BlueCheese)
    blueCheese.smellSource = SmellSource(doubleArrayOf(
        0.1843023824819105, 0.8969005568449742, -0.0869959701309641,
        -0.03930965224307115, 0.053630097604160105, 0.1719959853127682,
        -0.21784800055216272, -0.027583042347626888, -0.2814660919356998
    )).apply {
        decayFunction = GaussianDecayFunction()
    }
    
    val flax = world.addEntity(315, 349, EntityType.Flax)
    flax.smellSource = SmellSource(doubleArrayOf(
        -0.010560182705044729, 0.026571523931577266, -0.020953978263698426,
        0.11038351834856239, 0.2004498158179706, 0.2554258614582945,
        -0.035022510283423534, 1.0522166340913646, -0.21703143721480345
    )).apply {
        decayFunction = GaussianDecayFunction()
    }
    
    val tulip = world.addEntity(55, 349, EntityType.Tulip)
    tulip.smellSource = SmellSource(doubleArrayOf(
        -0.13098862375238882, 0.27927051862860713, 0.2580699935112152,
        -0.15164347434053907, -0.03638494874066445, -0.02723936667295006,
        0.0769829931118381, 1.2672660699668596, -0.16995842810080472
    )).apply {
        decayFunction = GaussianDecayFunction()
    }
    
    val pansy = world.addEntity(198, 385, EntityType.Pansy)
    pansy.smellSource = SmellSource(doubleArrayOf(
        -0.14163903550434465, -0.11730551337793613, 0.16291558437323667,
        0.041681576470888305, 0.025233570401022785, -0.14725712846433012,
        0.22536179032743064, 0.7027610174415445, 0.03595542268231242
    )).apply {
        decayFunction = GaussianDecayFunction()
    }

    couplingManager.createCoupling(smellCenter, somNet.inputLayer)

    val objectsWithLabels = listOf(
        swiss to "Swiss",
        gouda to "Gouda",
        blueCheese to "Blue",
        flax to "Flax",
        tulip to "Tulip",
        pansy to "Pansy"
    )
    
    val proximityRadius = 50.0
    
    workspace.addUpdateAction("Auto-label on proximity") {
        objectsWithLabels.forEach { (entity, label) ->
            val distance = mouse.location.distance(entity.location)
            if (distance < proximityRadius) {
                val winner = somNet.winner
                winner?.let { labelTracker.updateWinner(label, it) }
            }
        }
    }

    withGui {
        place(networkComponent, 182, 10, 450, 720)
        place(oc, 640, 10, 450, 520)

        val originalLearningRate = somNet.initialLearningRate
        val originalNeighborhoodSize = somNet.initNeighborhoodSize
        
        var savedLearningRate = originalLearningRate
        var savedNeighborhoodSize = originalNeighborhoodSize
        
        createControlPanel("Control Panel", 10, 10) {


            val freezeLearning = addCheckBox("Freeze Learning", false)
            freezeLearning.addActionListener {
                if (freezeLearning.isSelected) {
                    savedLearningRate = somNet.somLearningRate
                    savedNeighborhoodSize = somNet.neighborhoodSize
                    somNet.somLearningRate = 0.0
                    somNet.neighborhoodSize = 0.0
                } else {
                    somNet.somLearningRate = savedLearningRate
                    somNet.neighborhoodSize = savedNeighborhoodSize
                }
                // State info text is updated automatically in updateSOM()
                somNet.events.customInfoUpdated.fire()
            }

            addButton("Reset Learning") {
                somNet.reset()
                network.events.updated.fire()
            }

            addButton("Reset Network") {
                somNet.randomize()
                somNet.reset()
                labelTracker.clear(somNet.som.neuronList)
                network.events.updated.fire()
            }

        }
    }

    addSidebarInfo(
        """
        # SOM Network (Smell)
        
        Self-Organizing Map (SOM) networks learn to classify inputs in an unsupervised way, 
        by making nodes respond to similar inputs. Nodes near each other end up representing 
        similar inputs.
        
        # Simulation Details
        
        The mouse agent moves through an environment containing cheeses and flowers, each emitting 
        a unique 9-dimensional smell vector. As the mouse approaches objects (within radius `50`), 
        the SOM learns to organize these smells spatially.
        
        ## About the Labeling System
        
        The automatic labeling system is designed to be simple and easy to follow. When the mouse 
        is near an object, only the single most active neuron (the winner) gets labeled with that 
        object's name. This label shows which object most recently activated that neuron.
        
        In reality, multiple neurons may respond to a given object, especially at different distances, 
        since distance affects input strength. Additionally, when the mouse is positioned between 
        objects, it receives a mixture of smells that could activate neurons not labeled with any 
        single object. The labeling system only tracks the most recent winner for each object to 
        keep the visualization straightforward and interpretable.
        
        # What to Do
        
        ## Interactive Training
        
        1. Press `Run` to start the simulation
        2. Drag the mouse around the environment to move it near different objects
        3. When the mouse is within radius `50` of an object, the most active SOM neuron 
           automatically gets labeled with that object's name
        4. Watch as the SOM learns to organize the smells - similar smells will activate 
           nearby neurons
        
        The learning rate and neighborhood size gradually decrease as the network trains, 
        allowing the network to make finer distinctions between similar smells.
        
        ## Expected Behavior
        
        The cheese response nodes should cluster together, and the flower response nodes should 
        cluster together, since cheeses smell more similar to each other than to flowers, and 
        vice versa. Similar smells (e.g., Swiss and Gouda) should activate nearby neurons in 
        the SOM grid.
        
        ## Control Panel
        
        - `Reset Network`: Randomize SOM weights and clear labels to start training over
        - `Reset Learning`: Restore learning rate and neighborhood size to initial values 
          without changing the network weights
        - `Freeze Learning`: Stop learning and neighborhood decay while keeping the simulation 
          running (useful for testing the trained network)
        
        ## Try This
        
        - Training works best when you expose the mouse to all objects multiple times
        - The hexagonal layout helps visualize the topological organization
        - Try moving the mouse in circles around objects to see the encoding patterns form
        - Use `Freeze Learning` to test the network after training
        - Right-click the SOM layer to adjust learning parameters
        - Use the `Recall SOM Pattern` menu item (right-click on the SOM layer) to see what 
          smell pattern a specific output neuron is currently tuned to
        - Position the mouse between objects to observe how the network responds to smell mixtures
        
        # Credits
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        """.trimIndent()
    )
}
