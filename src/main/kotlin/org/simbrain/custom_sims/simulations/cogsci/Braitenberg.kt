package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.custom_sims.helper_classes.Vehicle.VehicleType
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuron
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.util.place
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor

/**
 * Braitenberg sim to accompany "Open Dynamics of Braitenberg Vehicles"
 */
val braitenbergSim = newSim {

    workspace.clearWorkspace()

    val vehicle1 = addNetworkComponent("Vehicle 1")
    // val vb1 = Vehicle(sim, vehicle1.network)
    // val ng1 = vb1.addPursuer(
    //     0, 0, agent1, EntityType.CIRCLE,
    //     agent1.sensors.get(0) as ObjectSensor,
    //     agent1.sensors.get(1) as ObjectSensor
    // )
    // ng1.label = "Vehicle 1"

    val vehicle2: NetworkComponent = addNetworkComponent("Vehicle 2")
    // val vb2 = Vehicle(sim, vehicle2.network)
    // val ng2 = vb2.addPursuer(
    //     0, 0, agent2, EntityType.CIRCLE,
    //     agent1.sensors.get(0) as ObjectSensor,
    //     agent1.sensors.get(1) as ObjectSensor
    // )
    // ng2.label = "Vehicle 2"

    val oc = addOdorWorldComponent()
    oc.world.isObjectsBlockMovement = false
    oc.world.isUseCameraCentering = false
    val agent1: OdorWorldEntity = oc.getWorld().addEntity(120, 245, EntityType.CIRCLE)
    val agent2: OdorWorldEntity = oc.getWorld().addEntity(320, 245, EntityType.CIRCLE)
    agent1.addLeftRightSensors(EntityType.CIRCLE, 270.0)
    agent2.addLeftRightSensors(EntityType.CIRCLE, 270.0)

    val docViewer = addDocViewer("Information", "Braitenberg.html")

    withGui {
        place(vehicle1, 251, 1, 359, 327)
        place(vehicle2, 249, 329, 361, 319)
        place(oc, 610, 3, 496, 646)
        place(docViewer, 0, 0, 253, 313)
    }


    // with(couplingManager) {
    //     neuronStraight couple straightMovement
    //     neuronLeftTurning couple turnLeft
    //     neuronRightTurning couple turnRight
    //     leftSensor couple neuronLeftSensor
    //     rightSensor couple neuronRightSensor
    // }


    withGui {
        createControlPanel("Control Panel", 5, 320) {
            addButton("Todo") {}
        }
    }

    fun addVehicle(
        net: Network, x: Int, y: Int, agent: OdorWorldEntity, vehicleType: VehicleType, objectType: EntityType,
        leftSensor: ObjectSensor, rightSensor: ObjectSensor
    ): NeuronCollection {

        val neurons = mutableListOf<Neuron>()

        // These have to be updated first to update properly
        // unless priority is used
        val leftInput: Neuron = net.addNeuron(x, y + 100)
        leftInput.label = "$objectType (L)"
        leftInput.isClamped = true
        neurons.add(leftInput)
        val rightInput: Neuron = net.addNeuron(x + 100, y + 100)
        rightInput.label = "$objectType (R)"
        rightInput.isClamped = true
        neurons.add(rightInput)
        val leftTurn: Neuron = net.addNeuron(x, y)
        leftTurn.label = "Left"
        neurons.add(leftTurn)
        val straight: Neuron = net.addNeuron(x + 50, y)
        straight.label = "Speed"
        straight.activation = 3.0
        straight.isClamped = true
        neurons.add(straight)
        val rightTurn: Neuron = net.addNeuron(x + 100, y)
        rightTurn.label = "Right"
        neurons.add(rightTurn)
        val vehicle = NeuronCollection(net, neurons)

        fun setNodeDefaults(neuron: Neuron) {
            neuron.lowerBound = -100.0
            neuron.upperBound = 200.0
        }
        setNodeDefaults(leftInput)
        setNodeDefaults(rightInput)
        setNodeDefaults(straight)
        setNodeDefaults(rightTurn)
        setNodeDefaults(leftTurn)
        net.addNetworkModelAsync(vehicle)

        // Set weights here
        // if (vehicleType == VehicleType.PURSUER) {
        //     connect(leftInput, leftTurn, weightSize.toDouble(), (-2 * weightSize).toDouble(), (2 * weightSize).toDouble())
        //     connect(rightInput, rightTurn, weightSize.toDouble(), (-2 * weightSize).toDouble(), (2 * weightSize).toDouble())
        // } else if (vehicleType == VehicleType.AVOIDER) {
        //     connect(leftInput, rightTurn, weightSize.toDouble(), (-2 * weightSize).toDouble(), (2 * weightSize).toDouble())
        //     connect(rightInput, leftTurn, weightSize.toDouble(), (-2 * weightSize).toDouble(), (2 * weightSize).toDouble())
        // }
        // if (connectSensorToStraightMovement) {
        //     connect(leftInput, straight, forwardWeights.toDouble())
        //     connect(rightInput, straight, forwardWeights.toDouble())
        // }

        // Update entity effectors
        agent.removeAllEffectors()
        val eStraight = StraightMovement()
        val eLeft = Turning(Turning.LEFT)
        val eRight = Turning(Turning.RIGHT)
        agent.addEffector(eStraight)
        agent.addEffector(eLeft)
        agent.addEffector(eRight)

        // Couple network to agent.
        with(couplingManager) {
            leftSensor couple leftInput
            rightSensor couple rightInput
            straight couple eStraight
            leftTurn couple eLeft
            rightTurn couple eRight
        }
        return vehicle
    }

}





