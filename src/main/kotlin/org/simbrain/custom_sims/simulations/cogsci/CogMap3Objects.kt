package org.simbrain.custom_sims.simulations.patterns_of_activity

import org.simbrain.custom_sims.*
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

    val NUM_NEURONS = 120
    val spectralRadius = .9

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    place(networkComponent,0, 6, 480, 522)

    // Make reservoir
    val recurrent = NeuronGroup(NUM_NEURONS).apply {
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
    network.addNetworkModelAsync(inputToRes)

    // World
    val dispersion = 100.0
    val mouseLocation = point(204.0, 343.0)
    val cheeseLocation = point(200.0, 250.0)
    val flowerLocation = point(330.0, 100.0)
    val fishLocation = point(50.0, 100.0)

    val odorWorldComponent = addOdorWorldComponent("World")

    place(odorWorldComponent,469, 4, 472, 516)

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
    projectionPlot.projector.connectPoints = true
    projectionPlot.projector.baseColor = Color.GRAY.brighter()
    projectionPlot.projector.coloringManager = HaloColoringManager().also{
        it.radius = 50.0
    }

    place(projectionPlot,930, 0, 518, 520)
    with(couplingManager) {
        recurrent couple projectionPlot
        mouse.getProducer(OdorWorldEntity::getNearbyObjectName) couple
                projectionPlot.getConsumer(ProjectionComponent::setLabel)
    }

}

