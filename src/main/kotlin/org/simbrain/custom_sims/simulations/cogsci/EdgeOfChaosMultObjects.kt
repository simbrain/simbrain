package org.simbrain.custom_sims.simulations.patterns_of_activity

import org.simbrain.custom_sims.*
import org.simbrain.custom_sims.simulations.AllostaticUpdateRule
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaos
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.SynapseGroup2
import org.simbrain.network.core.addNeuronGroup
import org.simbrain.network.layouts.LineLayout
import org.simbrain.util.environment.SmellSource
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.SmellSensor

/**
 * Demonstration of representational capacities of recurrent networks based on
 * Bertschinger, Nils, and Thomas Natschläger. "Real-time computation at the
 * edge of chaos in recurrent neural networks." Neural computation 16.7 (2004):
 * 1413-1436.
 */
val edgeOfChaosThreeObjects = newSim {

    // Simulation Parameters
    val NUM_NEURONS = 120
    val GRID_SPACE = 25
    // Since mean is 0, lower variance means lower average weight strength
    //  For 120 neurons: .01,.1, and > .4
    val variance = .1
    val K = 4 // in-degree (num connections to each neuron)


    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    place(networkComponent,0, 6, 480, 522)

    network.timeStep = 0.5

    // Make reservoir
    val reservoir = EdgeOfChaos.createReservoir(network, 10, 10, NUM_NEURONS)
    reservoir.prototypeRule = AllostaticUpdateRule()
    reservoir.label = "Reservoir"
    network.addNetworkModel(reservoir)

    // Connect reservoir
    val sgReservoir = EdgeOfChaos.connectReservoir(network, reservoir, variance, K)
    sgReservoir.label = "Synapses"
    network.addNetworkModel(sgReservoir)

    // Inputs

    // ConnectionStrategy recConnection = new RadialGaussian(DEFAULT_EE_CONST * 1, DEFAULT_EI_CONST * 3,
    //     DEFAULT_IE_CONST * 3, DEFAULT_II_CONST * 0, .25, 50);
    // SynapseGroup2 recSyns = new SynapseGroup2(reservoirNet, reservoirNet, recConnection);
    // net.addNetworkModelAsync(recSyns);
    // recSyns.setLabel("Recurrent");

    // Inputs to reservoir
    val inputNetwork = network.addNeuronGroup(1.0, 1.0, 3)
    inputNetwork.setLowerBound(-100.0)
    inputNetwork.setUpperBound(100.0)
    // (inputNetwork.prototypeRule as LinearRule).noiseGenerator = NormalDistribution(0.0, .1)
    // (inputNetwork.prototypeRule as LinearRule).addNoise = true
    inputNetwork.label = "Sensory Neurons"
    inputNetwork.layout = LineLayout()
    inputNetwork.applyLayout()
    network.addNetworkModelAsync(inputNetwork)

    val sparseExcitatory = Sparse(0.7, true, false)
    sparseExcitatory.percentExcitatory = 100.0
    val inputToRes = SynapseGroup2(inputNetwork, reservoir, sparseExcitatory)

    inputToRes.excitatoryRandomizer
        .probabilityDistribution = NormalDistribution(10.0, 1.0)
    inputToRes.displaySynapses = false
    inputToRes.label = "Sparse Excitatory"
    inputToRes.randomizeExcitatory()
    network.addNetworkModelAsync(inputToRes)
    inputNetwork.setLocation(360.0, 351.0)

    // World
    val dispersion = 100.0
    val mouseLocation = point(204.0, 343.0)
    val cheeseLocation = point(200.0, 250.0)
    val flowerLocation = point(330.0, 100.0)
    val fishLocation = point(50.0, 100.0)

    val odorWorldComponent = addOdorWorldComponent("World")

    withGui {
        place(odorWorldComponent) {
            location = point(469, 4)
            width = 472
            height = 516
        }
    }

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

    val (straightMovement, turnLeft, turnRight) = mouse.effectors
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
    withGui {
        place(projectionPlot) {
            location = point(930, 0)
            width = 518
            height = 520
        }
    }

    // Couple the neuron array to the projection plot
    with(couplingManager) {
        reservoir couple projectionPlot
    }

}

