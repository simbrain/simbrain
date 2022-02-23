package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addOdorWorldComponent
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.connections.RadialSimple
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.createNeurons
import org.simbrain.network.core.networkUpdateAction
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.neuron_update_rules.DecayRule
import org.simbrain.util.Utils
import org.simbrain.util.environment.SmellSource
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.toDoubleArray
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.Sensor
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3

/**
 * Sample simulation showing how to do basic stuff.
 */
val testSim = newSim {

    workspace.clearWorkspace()

    // ----- Network construction ------

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Subnetwork 1
    val neuronList1 = network.createNeurons(30)
    network.addNetworkModels(neuronList1)
    val region1 = NeuronCollection(network, neuronList1)
    network.addNetworkModel(region1)
    region1.apply {
        label = "Region 1"
        layout(GridLayout())
        location = point(-100, -100)
    }
    val radial = RadialSimple().apply {
        excitatoryRatio = .2
    }
    val syns = radial.connectNeurons(network, neuronList1, neuronList1)
    network.addNetworkModels(syns)
    // Set up some references
    val (straightNeuron, leftNeuron, rightNeuron) = neuronList1

    // Subnetwork 2
    val neuronList2 = network.createNeurons(20) {
        updateRule = DecayRule().apply {
            decayFraction = .2
        }
    }
    network.addNetworkModels(neuronList2)
    val region2 = NeuronCollection(network, neuronList2)
    network.addNetworkModel(region2)
    region2.apply {
        label = "Region 2"
        layout(GridLayout())
        location = point(500, -50)
    }
    val region2weights = radial.connectNeurons(network, neuronList2, neuronList2)
    network.addNetworkModels(region2weights)

    // Make connections between regions
    val sparse = Sparse().apply {
        connectionDensity = .3
        excitatoryRatio = .2
    }
    val region1_to_2_weights = sparse.connect(neuronList1, neuronList2)
    network.addNetworkModels(region1_to_2_weights)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    // ----- Build 2d World ------

    val odorWorldComponent = addOdorWorldComponent()
    val odorWorld = odorWorldComponent.world
    var straightMovement: Effector
    var turnLeft: Effector
    var turnRight: Effector
    var smellSensors: List<Sensor>

    odorWorld.apply {



        val cow = addEntity(50, 50, EntityType.COW).apply {
            heading = 90.0
            addDefaultEffectors()
            addSensor(SmellSensor(this))
            addSensor(SmellSensor(this).apply { theta = Math.PI / 8  })
            addSensor(SmellSensor(this).apply { theta = -Math.PI / 8  })
            manualStraightMovementIncrement = 2.0
            manualMotionTurnIncrement = 2.0
        }

        odorWorld.addEntity(100, 160, EntityType.FLAX).apply {
            smellSource = SmellSource(doubleArrayOf(1.0, 0.0, 0.0))
        }
        odorWorld.addEntity(110, 120, EntityType.FLAX).apply {
            smellSource = SmellSource(doubleArrayOf(1.0, 0.0, 0.0))
        }
        odorWorld.addEntity(10, 10, EntityType.FLOWER).apply {
            smellSource = SmellSource(doubleArrayOf(0.0, 1.0, 0.0))
        }
        odorWorld.addEntity(15, 25, EntityType.FLOWER).apply {
            smellSource = SmellSource(doubleArrayOf(0.0, 1.0, 0.0))
        }

        straightMovement = cow.effectors[0]
        turnLeft = cow.effectors[1]
        turnRight = cow.effectors[2]
        smellSensors = cow.sensors

    }

    withGui {
        place(odorWorldComponent) {
            location = point(386, 0)
            width = 400
            height = 400
        }
    }

    // ----- Make Couplings ------

    with(couplingManager) {
        straightNeuron couple straightMovement
        leftNeuron couple turnLeft
        rightNeuron couple turnRight
        // TODO: Make a separate bank of input nodes
        smellSensors[0] couple region1
        smellSensors[1] couple region1
        smellSensors[2] couple region1
    }

    // ----- Train network ------

    val initialLearning = networkUpdateAction("Learning") {
        neuronList1.forEach { n -> n.randomizeBias(0.0, 1.0) }
        region1_to_2_weights.forEach { w -> w.strength += .1 * Math.random() }
        println("Learning update: ${workspace.time}")
    }
    network.addUpdateAction(initialLearning)
    workspace.iterate(100)
    network.removeUpdateAction(initialLearning)

    // ----- Add pulse and record activations  ------

    val activations = mutableListOf<List<Double>>()
    region1.randomize()
    val recordActivations = networkUpdateAction("Record activations") {
        val acts = network.looseNeurons.map { n -> n.activation }
        activations.add(acts)
    }
    network.addUpdateAction(recordActivations)
    workspace.iterate(10)
    network.removeUpdateAction(recordActivations)
    // Save activations
    val actArray = activations.toDoubleArray()
    println(actArray.contentDeepToString())
    Utils.writeMatrix(actArray, File("activations.csv"))
}

