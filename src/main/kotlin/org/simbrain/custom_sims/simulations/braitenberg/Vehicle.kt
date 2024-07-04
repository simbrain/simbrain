package org.simbrain.custom_sims.simulations.braitenberg

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.helper_classes.SimulationUtils
import org.simbrain.network.core.*
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor

/**
 * A custom class that makes it easy to add Braitenberg vehicles to a
 * simulation. A vehicle network is added to a network and it is coupled to a
 * corresponding sensor in a world.
 */
class Vehicle
/**
 * Construct the vehicle builder. Needs a reference to the network to build
 * the network, the world to reference the sensor, and the sim to make the
 * couplings.
 *
 * @param sim   the parent simulation object
 * @param net   the network to add the vehicle subnetworks to
 */(
    /**
     * The simulation object.
     */
    private val sim: SimulationUtils,
    /**
     * Reference to the network to put the vehicle in.
     */
    private val net: Network
) {
    /**
     * Size of sensor-motor weights. Determines how "sharply" agents turn.
     */
    private val weightSize = 250

    /**
     * Size of weights from sensors to straight movement.
     */
    private val forwardWeights = 5

    /**
     * If true connect sensor nodes to straight movement node.
     */
    private val connectSensorToStraightMovement = true

    /**
     * What type of vehicle to add.
     */
    enum class VehicleType {
        PURSUER, AVOIDER
    }

    /**
     * Add a vehicle.
     *
     * @param x           x location
     * @param y           y location
     * @param agent       reference to the agent to couple to
     * @param vehicleType Pursuer, Avoider, etc.
     * @param objectType  what kind of object this vehicles pursues or avoids
     * @return a reference to the resulting neuron group
     */
    context(Network)
    fun addVehicle(
        x: Int, y: Int, agent: OdorWorldEntity, vehicleType: VehicleType, objectType: EntityType,
        leftSensor: ObjectSensor?, rightSensor: ObjectSensor?
    ): NeuronCollection {
        val neurons: MutableList<Neuron> = ArrayList()

        // These have to be updated first to update properly
        // unless priority is used
        val leftInput = runBlocking { net.addNeuron(x, y + 100) }
        leftInput.label = "$objectType (L)"
        leftInput.clamped = true
        neurons.add(leftInput)

        val rightInput = runBlocking { net.addNeuron(x + 100, y + 100) }
        rightInput.label = "$objectType (R)"
        rightInput.clamped = true
        neurons.add(rightInput)

        val leftTurn = runBlocking { net.addNeuron(x, y) }
        leftTurn.label = "Left"
        neurons.add(leftTurn)

        val straight = runBlocking { net.addNeuron(x + 50, y) }
        straight.label = "Speed"
        straight.activation = 3.0
        straight.clamped = true
        neurons.add(straight)

        val rightTurn = runBlocking { net.addNeuron(x + 100, y) }
        rightTurn.label = "Right"
        neurons.add(rightTurn)

        val vehicle = NeuronCollection(neurons)
        setNodeDefaults(leftInput)
        setNodeDefaults(rightInput)
        setNodeDefaults(straight)
        setNodeDefaults(rightTurn)
        setNodeDefaults(leftTurn)
        net.addNetworkModel(vehicle)

        // Set weights here
        if (vehicleType == VehicleType.PURSUER) {
            connect(
                leftInput,
                leftTurn,
                weightSize.toDouble(),
                (-2 * weightSize).toDouble(),
                (2 * weightSize).toDouble()
            )
            connect(
                rightInput,
                rightTurn,
                weightSize.toDouble(),
                (-2 * weightSize).toDouble(),
                (2 * weightSize).toDouble()
            )
        } else if (vehicleType == VehicleType.AVOIDER) {
            connect(
                leftInput,
                rightTurn,
                weightSize.toDouble(),
                (-2 * weightSize).toDouble(),
                (2 * weightSize).toDouble()
            )
            connect(
                rightInput,
                leftTurn,
                weightSize.toDouble(),
                (-2 * weightSize).toDouble(),
                (2 * weightSize).toDouble()
            )
        }

        if (connectSensorToStraightMovement) {
            connect(leftInput, straight, forwardWeights.toDouble())
            connect(rightInput, straight, forwardWeights.toDouble())
        }

        // Update entity effectors
        agent.removeAllEffectors()
        val eStraight = StraightMovement()
        val eLeft = Turning(Turning.LEFT)
        val eRight = Turning(Turning.RIGHT)
        agent.addEffector(eStraight)
        agent.addEffector(eLeft)
        agent.addEffector(eRight)

        // Couple network to agent.
        sim.couple(leftSensor, leftInput)
        sim.couple(rightSensor, rightInput)
        sim.couple(straight, eStraight)
        sim.couple(leftTurn, eLeft)
        sim.couple(rightTurn, eRight)

        return vehicle
    }

    /**
     * Add a pursuer.
     */
    context(Network)
    fun addPursuer(
        x: Int,
        y: Int,
        agent: OdorWorldEntity,
        objectType: EntityType,
        left: ObjectSensor?,
        right: ObjectSensor?
    ): NeuronCollection {
        return addVehicle(x, y, agent, VehicleType.PURSUER, objectType, left, right)
    }

    /**
     * Add an avoider.
     */
    context(Network)
    fun addAvoider(
        x: Int,
        y: Int,
        agent: OdorWorldEntity,
        objectType: EntityType,
        left: ObjectSensor?,
        right: ObjectSensor?
    ): NeuronCollection {
        return addVehicle(x, y, agent, VehicleType.AVOIDER, objectType, left, right)
    }

    /**
     * Helper method to set default value for vehicle nodes.
     */
    private fun setNodeDefaults(neuron: Neuron) {
        neuron.lowerBound = (-100).toDouble()
        neuron.upperBound = 200.0
    }
}
