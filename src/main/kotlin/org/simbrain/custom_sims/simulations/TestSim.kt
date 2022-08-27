package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.*
import org.simbrain.network.connections.RadialProbabilistic
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.createNeurons
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.neuron_update_rules.DecayRule
import org.simbrain.util.environment.SmellSource
import org.simbrain.util.piccolo.asGridCoordinate
import org.simbrain.util.piccolo.makeLake
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.Sensor
import org.simbrain.world.odorworld.sensors.SmellSensor
import javax.swing.JInternalFrame

/**
 * Sample simulation showing how to do basic stuff.
 */
val testSim = newSim {

    workspace.clearWorkspace()

    // ----- Network construction ------

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Subnetwork 1
    val neuronList1 = network.createNeurons(6)
    val region1 = NeuronCollection(network, neuronList1)
    network.addNetworkModel(region1)
    region1.apply {
        label = "Region 1"
        layout(GridLayout())
        location = point(-100, -100)
    }

    // Connection strategies to use below
    val radial = RadialProbabilistic().apply {
        percentExcitatory = 20.0
    }
    val sparse = Sparse(connectionDensity = 0.3).apply {
        percentExcitatory = 10.0
    }

    val syns = sparse.connectNeurons(network, neuronList1, neuronList1)
    network.addNetworkModels(syns)
    // Set up some references
    val straightNeuron = neuronList1[2]
    val leftNeuron = neuronList1[3]
    val rightNeuron =  neuronList1[4]

    // Subnetwork 2
    val neuronList2 = network.createNeurons(20) {
        updateRule = DecayRule().apply {
            decayFraction = .2
        }
    }
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
    val region1_to_2_weights = sparse.connectNeurons(network, neuronList1, neuronList2)
    network.addNetworkModels(region1_to_2_weights)

    // TODO: Temp because excitatory ratio not working
    region1.randomizeIncomingWeights()
    region2.randomizeIncomingWeights()


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

    val cow : OdorWorldEntity

    odorWorld.apply {

        wrapAround = false

        // tileMap = loadTileMap("yulins_world.tmx")
        // tileMap.editTile(1,1,1)
        // tileMap.editTile(5,5,12)

        tileMap.makeLake(point(1, 1).asGridCoordinate(), 5, 2)
        tileMap.makeLake(point(4, 2).asGridCoordinate(), 2, 5)

        cow = addEntity(50, 50, EntityType.COW).apply {
            heading = 90.0
            addDefaultEffectors()
            addSensor(SmellSensor())
            addSensor(SmellSensor().apply { theta = 22.5  })
            addSensor(SmellSensor().apply { theta = -22.5 })
            manualMovement.manualStraightMovementIncrement = 2.0
            manualMovement.manualMotionTurnIncrement = 2.0
        }

        odorWorld.addEntity(200, 260, EntityType.FLAX).apply {
            smellSource = SmellSource(doubleArrayOf(1.0, 0.0, 0.0))
        }
        odorWorld.addEntity(210, 120, EntityType.SWISS).apply {
            smellSource = SmellSource(doubleArrayOf(1.0, 0.5, 0.0))
        }
        odorWorld.addEntity(10, 10, EntityType.FLOWER).apply {
            smellSource = SmellSource(doubleArrayOf(0.0, 0.5, 0.5))
        }
        odorWorld.addEntity(59, 200, EntityType.CANDLE).apply {
            smellSource = SmellSource(doubleArrayOf(0.0, 1.0, 0.0))
        }

        straightMovement = cow.effectors[0]
        turnLeft = cow.effectors[1]
        turnRight = cow.effectors[2]
        smellSensors = cow.sensors

    }

    withGui {
        place(odorWorldComponent) {
            location = point(403, 0)
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

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot("Activations")
    withGui {
        place(projectionPlot) {
            location = point(810, 0)
            width = 400
            height = 400
        }
    }

    // Couple the neuron array to the projection plot
    with(couplingManager) {
        region2 couple projectionPlot
    }

    // val odorWorldStuff = object : UpdateActionAdapter("WorldStuff") {
    //     override fun invoke() {
    //         println(cow)
    //     }
    // }
    // workspace.addUpdateAction(odorWorldStuff)
    // workspace.iterate(100)
    // workspace.removeUpdateAction(odorWorldStuff)


    // TODO: For reservoir
    // // ----- Train network ------
    //
    // val initialLearning = networkUpdateAction("Learning") {
    //     neuronList1.forEach { n -> n.randomizeBias(0.0, 1.0) }
    //     region1_to_2_weights.forEach { w -> w.strength += .1 * Math.random() }
    //     println("Learning update: ${workspace.time}")
    // }
    // network.addUpdateAction(initialLearning)
    // workspace.iterate(100)
    // network.removeUpdateAction(initialLearning)

}


val linkedNeuronList = newSim {
    repeat(1) {
        val networkComponent = addNetworkComponent("Neuron List $it")

        val network = networkComponent.network

        workspace.coroutineScope.launch(Dispatchers.Main) {
            val neurons = (1..10000).map {
                async(Dispatchers.Default) { Neuron(network) }
            }.awaitAll()

            neurons.forEach { network.addNetworkModel(it) }
            val (first) = neurons
            first.activation = 1.0

            val synapses = with(network) {
                neurons.windowed(2) { (n1, n2) ->
                    addSynapse(n1, n2) {
                        strength = 1.0
                    }
                }
            }
            HexagonalGridLayout.layoutNeurons(neurons, 40, 40)
        }
        withGui {
            getDesktopComponent(networkComponent).apply {
                parentFrame.let { frame ->
                    if (frame is JInternalFrame) {
                        frame.setIcon(true)
                    }
                }
            }
        }
    }
}