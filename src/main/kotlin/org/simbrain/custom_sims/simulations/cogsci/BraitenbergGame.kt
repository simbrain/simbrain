package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.util.graphicalUpperBound
import org.simbrain.util.place
import org.simbrain.world.odorworld.entities.EntityType
import java.awt.geom.Point2D
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.math.max

/**
 * Testing
 */
val braitenbergGame = newSim {

    var leftWeight = 1.0
    var rightWeight = 1.0
    var velocity = .1

    workspace.clearWorkspace()

    val oc = addOdorWorldComponent {
        world.tileMap.updateMapSize(20, 18)
    }
    desktop?.getDesktopComponent(oc)?.title = "Obstacle Course"
    //oc.world.isObjectsBlockMovement = false
    oc.world.isUseCameraCentering = false
    oc.world.addEntity(257, 191, EntityType.POISON)
    oc.world.addEntity(323, 286, EntityType.POISON)
    oc.world.addEntity(398, 335, EntityType.POISON)
    oc.world.addEntity(500, 184, EntityType.SWISS)

    class Vehicle(name: String, entityType: EntityType, entityOffset: Point2D) {

        val networkComponent = addNetworkComponent(name)

        val network get() = networkComponent.network

        val agent = oc.world.addEntity(entityOffset.x, entityOffset.y, entityType).apply {
            addLeftRightSensors(EntityType.SWISS, 270.0)
            addDefaultEffectors()
        }

        val leftInput = runBlocking {
            network.addNeuron(0, 100).apply {
                label = "$entityType (L)"
                clamped = true
            }
        }

        val rightInput = runBlocking {
            network.addNeuron(100, 100).apply {
                label = "$entityType (R)"
                clamped = true
            }
        }

        val straight = runBlocking {
            network.addNeuron(50, 0).apply {
                label = "Speed"
                activation = 1.0
                clamped = true
            }
        }

        val leftTurn = runBlocking {
            network.addNeuron(0, 0).apply {
                label = "Left"
                lowerBound = -200.0
                upperBound = 200.0
            }
        }
        val rightTurn: Neuron = runBlocking {
            network.addNeuron(100, 0).apply {
                label = "Right"
                lowerBound = -200.0
                upperBound = 200.0
            }

        }
        val leftSynapse = network.addSynapse(leftInput, leftTurn)

        val rightSynapse = network.addSynapse(rightInput, rightTurn)

        // val neuronCollection = network.addNetworkModelAsync(
        //     NeuronCollection(network, listOf(leftInput, rightInput, straight, leftTurn, rightTurn))
        // )

        init {
            val (leftSensor, rightSensor) = agent.sensors
            val (eStraight, eLeft, eRight) = agent.effectors
            with(couplingManager) {
                leftSensor couple leftInput
                rightSensor couple rightInput
                straight couple eStraight
                leftTurn couple eLeft
                rightTurn couple eRight
            }
        }

    }

    val vehicle1 = Vehicle("Vehicle 1", EntityType.CIRCLE, Point2D.Double(194.0, 407.0))

    withGui {
        place(vehicle1.networkComponent, 53, 282, 359, 327)
        place(oc, 462, 19, 600, 600)
    }

    withGui {
        createControlPanel("Control Panel", 64, 38) {
            // Update neuron and weight bounds to reasonable values given weight values
            fun updateBounds(w1: Double, w2: Double) {
                val bound = graphicalUpperBound(max(w1, w2))
                vehicle1.leftSynapse.upperBound = bound
                vehicle1.rightSynapse.upperBound = bound
                vehicle1.leftSynapse.lowerBound = -bound
                vehicle1.rightSynapse.lowerBound = -bound
                vehicle1.leftTurn.upperBound = bound
                vehicle1.rightTurn.upperBound = bound
                vehicle1.leftTurn.lowerBound = -bound
                vehicle1.rightTurn.lowerBound = -bound
            }
            fun updateVehicle() {
                vehicle1.leftSynapse.strength = leftWeight
                vehicle1.rightSynapse.strength = rightWeight
                vehicle1.straight.activation = velocity
                updateBounds(leftWeight, rightWeight)
            }
            addSlider("Left weight", -10.0, 10.0, 1.0, .01) {
                leftWeight = it
                updateVehicle()
            }
            addSlider("Right weight", -10.0, 10.0, 1.0, .01) {
                rightWeight = it
                updateVehicle()
            }
            addSlider("Velocity", -10.0, 10.0, .02, .01) {
                velocity = it
                updateVehicle()
            }
            updateVehicle()

        }
    }

}





