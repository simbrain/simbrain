package org.simbrain.custom_sims.simulations.patterns_of_activity

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.RandomWeightInitializer
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.util.SmellSource
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.projection.HaloColoringManager
import org.simbrain.util.setSpectralRadius
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.awt.Color

/**
 * Generic 3 object -> recurrent net example using neuron array
 */
val cogMap3Objects = newSim {

    workspace.clearWorkspace()

    val numNeurons = 120
    val spectralRadius = .99

    //workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Make reservoir
    val recurrent = network.addNeuronCollection(numNeurons).apply {
        label = "Recurrent"
        betweenNeuronInterval = 30
        setLayoutBasedOnSize(point(0.0, 0.0))
        applyLayout()
    }
    val weightMatrix = WeightMatrix(recurrent, recurrent)
    weightMatrix.randomize()
    weightMatrix.weights.setSpectralRadius(spectralRadius)
    network.addNetworkModel(weightMatrix)

    // Inputs to reservoir
    val inputNetwork = network.addNeuronCollection(3).apply {
        setLowerBound(-1.0)
        setUpperBound(1.0)
        label = "Sensory Neurons"
        betweenNeuronInterval = 30
        setLayoutBasedOnSize(point(0.0, 751.0))
        applyLayout()
    }

    val sparseExcitatory = Sparse(0.7, true, false)
    sparseExcitatory.percentExcitatory = 100.0
    sparseExcitatory.weightInitializer = RandomWeightInitializer().apply {
        exRandomizer = NormalDistribution(10.0, 1.0)
    }
    val inputToRes = SynapseGroup(inputNetwork, recurrent, sparseExcitatory)
    inputToRes.displaySynapses = false
    inputToRes.label = "Sparse Excitatory"
    inputToRes.randomizeExcitatory()
    network.addNetworkModel(inputToRes)

    // World
    val dispersion = 100.0
    val mouseLocation = point(204.0, 343.0)
    val cheeseLocation = point(200.0, 250.0)
    val flowerLocation = point(330.0, 100.0)
    val fishLocation = point(50.0, 100.0)

    val odorWorldComponent = addOdorWorldComponent("World")

    val odorWorld = odorWorldComponent.world.apply {
        isObjectsBlockMovement = false
    }

    val mouse = odorWorld.addEntity(EntityType.Mouse).apply {
        location = mouseLocation
        heading = 90.0
        addDefaultEffectors()
        addSensor(SmellSensor())
        manualMovement.manualStraightMovementIncrement = 2.0
        manualMovement.manualMotionTurnIncrement = 2.0
    }

    val (smellSensors) = mouse.sensors

    val cheese = odorWorld.addEntity(EntityType.Swiss).apply {
        location = cheeseLocation
        smellSource = SmellSource(doubleArrayOf(1.0, 0.0, 0.0)).apply {
            this.dispersion = dispersion
        }
    }

    val flower = odorWorld.addEntity(EntityType.Flower).apply {
        location = flowerLocation
        smellSource = SmellSource(doubleArrayOf(0.0, 1.0, 0.0)).apply {
            this.dispersion = dispersion
        }
    }

    val fish = odorWorld.addEntity(EntityType.Fish).apply {
        location = fishLocation
        smellSource = SmellSource(doubleArrayOf(0.0, 0.0, 1.0)).apply {
            this.dispersion = dispersion
        }
    }

    odorWorld.update()

    with(couplingManager) {
        smellSensors couple inputNetwork
    }

    // Plot
    val projectionPlot = addProjectionPlot("Cognitive Map")
    projectionPlot.projector.tolerance = .9
    projectionPlot.projector.connectPoints = false
    projectionPlot.projector.baseColor = Color.GRAY.brighter()
    projectionPlot.projector.coloringManager = HaloColoringManager().also{
        it.radius = 50.0
    }

    with(couplingManager) {
        recurrent couple projectionPlot
        mouse.getProducer(OdorWorldEntity::getNearbyObjectName) couple
                projectionPlot.getConsumer(ProjectionComponent::setLabel)
    }

    addSidebarInfo(
    """ 
    # Recurrent 3 Object Detector
    
    The Generic 3 Objects Simulation consists of a central recurrent network where, sparse input from the distributed olfactory neuron group (`Sensory Neurons`) are sent into the recurrent network. The inputs function 
    similarly to the `Agent Trails` simulation; objects sensed by the agent's receptors are plotted into the plot where, the agent's responses to each object are recorded.  
    
    # Simulation Details
    
    This simulation models the distance-based interactions between the agent and objects, showing how networks develop cognitive maps of their environment. Each object is associated with "smell vectors", where an object
    has a stronger impact on the agent's sensory receptors the closer an object is. Because there are multiple objects in this environment, the agent is presented with multiple overlapping patterns of activation. 
    The total pattern of activation of the agent's olfactory receptors is a sum of scaled smell vectors. 
    
    In this simulation, the agent will be dragged to each of the objects where, its sensory activations are recorded and plotted into the `Cognitive Maps` window. In this PCA plot is a graph of the agent's sensory receptors
    and the objects that it has interacted with at different points. Points are labeled when the agent is directly on top of an object. This PCA plot can be thought of as the agent's perception of its environment where,
    through the agent's movement in the odor world, it develops an understanding of its environment.
    
    # What to Do
    
    In this simulation, the mouse is the agent with `3` objects, the cheese, fish, and flower. To get a quick feel of the simulation, run the simulation and drag the agent to each of the objects and observe changes
    in the `Cognitive Maps` window. Below are the step-by-step instructions:
    
    1. Run the simulation with the `Run workspace` button in the toolbar.
    
    2. In the `World` window, drag the agent (the mouse) towards each of the objects (cheese, flower, fish).
    
    3. Observe how the elements in the `Network` and `Cognitive Map` windows change when the agent is moved around.

    4. If the map development slows down, restart the `Cognitive Map` window with the `Eraser` button in the plot toolbar.
    
        - Note: You can also run the simulation again to start the mapping again and reset the weights. 
        
    # Credits
    
    Jasmine Lau
    
    [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
    
    Kanly Thao
    """.trimIndent(),
    )

    withGui{
        place(networkComponent,470, 450, 470, 427)
        place(odorWorldComponent,0, 0, 470, 593)
        place(projectionPlot,470, 0, 470, 450)
    }

}
