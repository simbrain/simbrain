package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
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
 */
val somNetSmells = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("SOM Network")
    val network = networkComponent.network

    val somNet = SOMNetwork(9, 16)
    network.addNetworkModelAsync(somNet)
    somNet.inputLayer.apply {
        setUpperBound(1.0)
        isClamped = false
    }
    
    somNet.som.params.initialLearningRate = 0.06
    somNet.som.params.initNeighborhoodSize = 100.0
    
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

    withGui {
        place(networkComponent, 300, 10, 350, 720)
        place(oc, 640, 10, 450, 520)

        // store original learning rate for reset
        val originalLearningRate = somNet.som.learningRate
        
        createControlPanel("Control Panel", 10, 10) {
            addButton("Reset Network") {
                somNet.randomize()
                somNet.som.learningRate = originalLearningRate
                labelTracker.clear(somNet.som.neuronList)
            }
            
            addSeparator()
            
            addRow("Swiss") {
                addButton("Move") { mouse.setLocation(36, 107) }
                addButton("Train") { 
                    mouse.setLocation(36, 107)
                    workspace.iterateSuspend()
                    val winner = somNet.som.neuronList.maxByOrNull { it.activation }
                    winner?.let { labelTracker.updateWinner("Swiss", it) }
                }
            }
            addRow("Gouda") {
                addButton("Move") { mouse.setLocation(169, 32) }
                addButton("Train") { 
                    mouse.setLocation(169, 32)
                    workspace.iterateSuspend()
                    val winner = somNet.som.neuronList.maxByOrNull { it.activation }
                    winner?.let { labelTracker.updateWinner("Gouda", it) }
                }
            }
            addRow("Blue Cheese") {
                addButton("Move") { mouse.setLocation(284, 90) }
                addButton("Train") { 
                    mouse.setLocation(284, 90)
                    workspace.iterateSuspend()
                    val winner = somNet.som.neuronList.maxByOrNull { it.activation }
                    winner?.let { labelTracker.updateWinner("Blue", it) }
                }
            }
            
            addSeparator()
            
            addRow("Flax") {
                addButton("Move") { mouse.setLocation(315, 349) }
                addButton("Train") { 
                    mouse.setLocation(315, 349)
                    workspace.iterateSuspend()
                    val winner = somNet.som.neuronList.maxByOrNull { it.activation }
                    winner?.let { labelTracker.updateWinner("Flax", it) }
                }
            }
            addRow("Tulip") {
                addButton("Move") { mouse.setLocation(55, 349) }
                addButton("Train") { 
                    mouse.setLocation(55, 349)
                    workspace.iterateSuspend()
                    val winner = somNet.som.neuronList.maxByOrNull { it.activation }
                    winner?.let { labelTracker.updateWinner("Tulip", it) }
                }
            }
            addRow("Pansy") {
                addButton("Move") { mouse.setLocation(198, 385) }
                addButton("Train") { 
                    mouse.setLocation(198, 385)
                    workspace.iterateSuspend()
                    val winner = somNet.som.neuronList.maxByOrNull { it.activation }
                    winner?.let { labelTracker.updateWinner("Pansy", it) }
                }
            }
        }
    }

    addSidebarInfo(
        """
        # SOM Network (Smell)
        
        Self-Organizing Map (SOM) networks learn to classify inputs in an unsupervised way, 
        by making nodes respond to similar inputs. Nodes near each other end up representing 
        similar inputs.
        
        ## Using this Simulation
        
        **Training Mode:**
        
        Begin by "training" the network. Press the workspace play button and use the control 
        panel buttons to move the mouse to each object. The network will only learn while the 
        learning rate (in the SOM properties) is above 0, so you should expose the mouse to 
        all the smells relatively quickly during the initial training phase.
        
        The learning rate and neighborhood size will gradually decrease as the network trains, 
        allowing the network to make finer distinctions between similar smells.
        
        **Testing Mode:**
        
        After training, keep the simulation running and move the mouse to different objects 
        using the control panel buttons. Observe which SOM neurons activate for each smell.
        
        **Expected Behavior:**
        
        The cheese response nodes should cluster together, and the flower response nodes should 
        cluster together, since cheeses smell more similar to each other than to flowers, and 
        vice versa.
        
        The SOM neurons are automatically labeled when you use the "Train" buttons, showing 
        which smells they respond to. Similar smells (e.g., Swiss and Gouda) should activate 
        nearby neurons in the SOM grid.
        
        ## Tips
        
        - Use "Reset Network" to randomize the SOM weights and start training over
        - Right-click the SOM layer to adjust learning parameters
        - Training works best when you expose the mouse to all objects multiple times
        - The hexagonal layout helps visualize the topological organization
        
        """.trimIndent()
    )
}

