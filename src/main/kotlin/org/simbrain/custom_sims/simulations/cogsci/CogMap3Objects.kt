package org.simbrain.custom_sims.simulations.patterns_of_activity

import org.simbrain.custom_sims.*
import org.simbrain.custom_sims.addDocViewer
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neurongroups.NeuronGroup
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
    val spectralRadius = .9

    //workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Make reservoir
    val recurrent = NeuronGroup(numNeurons).apply {
        // layout(GridLayout())
        label = "Recurrent"
        // setNeuronType(LinearRule())
        applyLayout()
    }
    val weightMatrix = WeightMatrix(recurrent, recurrent)
    weightMatrix.randomize()
    weightMatrix.weightMatrix.setSpectralRadius(spectralRadius)
    network.addNetworkModels(recurrent, weightMatrix)

    // Inputs to reservoir
    val inputNetwork = NeuronGroup(3)
    inputNetwork.setLowerBound(-1.0)
    inputNetwork.setUpperBound(1.0)
    inputNetwork.label = "Sensory Neurons"
    inputNetwork.layout = LineLayout()
    inputNetwork.applyLayout()
    network.addNetworkModel(inputNetwork)
    inputNetwork.setLocation(0.0, 751.0)

    val sparseExcitatory = Sparse(0.7, true, false)
    sparseExcitatory.percentExcitatory = 100.0
    val inputToRes = SynapseGroup(inputNetwork, recurrent, sparseExcitatory)

    inputToRes.connectionStrategy.exRandomizer = NormalDistribution(10.0, 1.0)
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

    val mouse = odorWorld.addEntity(EntityType.MOUSE).apply {
        location = mouseLocation
        heading = 90.0
        addDefaultEffectors()
        addSensor(SmellSensor())
        manualMovement.manualStraightMovementIncrement = 2.0
        manualMovement.manualMotionTurnIncrement = 2.0
    }

    val (smellSensors) = mouse.sensors

    val cheese = odorWorld.addEntity(EntityType.SWISS).apply {
        location = cheeseLocation
        smellSource = SmellSource(doubleArrayOf(1.0, 0.0, 0.0)).apply {
            this.dispersion = dispersion
        }
    }

    val flower = odorWorld.addEntity(EntityType.FLOWER).apply {
        location = flowerLocation
        smellSource = SmellSource(doubleArrayOf(0.0, 1.0, 0.0)).apply {
            this.dispersion = dispersion
        }
    }

    val fish = odorWorld.addEntity(EntityType.FISH).apply {
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
    val projectionPlot = addProjectionPlot2("Cognitive Map")
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
        # Introduction
        
        The Generic 3 Objects Simulation consists of a central recurrent network, where sparse input from the distributed olfactor inputs into the network. The inputs function similarly to the Agent Trails, as the objects sensed by the agent's receptors are plotted into the graph, modelling the reponse of the agent to its world.  
        
        Objects in this network are each associated with "smell vectors", which have a stronger impact the closer they are to the agent. Because there are multiple objects in this environment, the agent is presented with multiple overlapping patterns of activation. The total pattern of inputs into the agent's olfactory receptors is a sum of scaled smell vectors. 
        
        This simulation models distance based interactions between the agent and objects, showing how networks develop cognitive maps of their environment. 
        
        # What to Do
        1. Run the simulation with the "Run workspace" button in the toolbar. 
        2. In the "World" window, drag the mouse (the agent) towards the cheese, flower, and fish objects.
        3. Observe how the elements in the "Network" window and map "Cognitive Map" windows develop when moving the agent
            - The "Cognitive Map" creates a graph of where the agent senses the objects it interacts with. 
            - When the agent is directly on top of an object, it labels the point with the corresponding object. 
        4. The map showcases how an agent develops a sense of an environment. 
        5. If the map development slows down, restart the "Cognitive Map" window with the "Eraser" button in the tool bar
            - You can run the simulation again to start mapping again. 
        """.trimIndent(),
    )

    withGui{
        place(networkComponent,463, 450, 483, 427)
        place(odorWorldComponent,0, 0, 470, 593)
        place(projectionPlot,463, 0, 478, 448)
    }

}