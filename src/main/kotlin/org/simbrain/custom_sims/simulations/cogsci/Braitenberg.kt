package org.simbrain.custom_sims.simulations

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
 * Braitenberg sim to accompany "Open Dynamics of Braitenberg Vehicles"
 */
val braitenbergSim = newSim {

    workspace.clearWorkspace()

    val oc = addOdorWorldComponent {
        world.tileMap.updateMapSize(20, 18)
    }
    oc.world.isObjectsBlockMovement = false
    oc.world.isUseCameraCentering = false

    class Vehicle(name: String, entityType: EntityType, entityOffset: Point2D) {

        val networkComponent = addNetworkComponent(name)

        val network get() = networkComponent.network

        val agent = oc.world.addEntity(entityOffset.x, entityOffset.y, entityType).apply {
            addLeftRightSensors(entityType, 270.0)
            addDefaultEffectors()
        }

        val leftInput = network.addNeuron(0, 100).apply {
            label = "$entityType (L)"
            clamped = true
        }

        val rightInput = network.addNeuron(100, 100).apply {
            label = "$entityType (R)"
            clamped = true
        }

        val straight = network.addNeuron(50, 0).apply {
            label = "Speed"
            activation = 1.0
            clamped = true
        }

        val leftTurn = network.addNeuron(0, 0).apply {
            label = "Left"
            lowerBound = -200.0
            upperBound = 200.0
        }
        val rightTurn: Neuron = network.addNeuron(100, 0).apply {
            label = "Right"
            lowerBound = -200.0
            upperBound = 200.0
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

    val vehicle1 = Vehicle("Vehicle 1", EntityType.CIRCLE, Point2D.Double(120.0, 245.0))
    val vehicle2 = Vehicle("Vehicle 2", EntityType.CIRCLE, Point2D.Double(320.0, 245.0))

    val docViewer = addDocViewer("Information", "Braitenberg.html")

    withGui {
        place(vehicle1.networkComponent, 251, 1, 359, 327)
        place(vehicle2.networkComponent, 249, 329, 361, 319)
        place(oc, 610, 3, 496, 646)
        place(docViewer, 0, 0, 253, 313)
    }

    var leftWeight = 100.0
    var rightWeight = 50.0
    var velocity = 1.0

    withGui {
        createControlPanel("Control Panel", 5, 320) {
            addFormattedNumericTextField("Left weight", initValue = 100.0) {
                leftWeight = it
            }
            addFormattedNumericTextField("Right weight", initValue = 50.0) {
                rightWeight = it
            }
            addFormattedNumericTextField("Velocity", initValue = 1.0) {
                velocity = it
            }
            fun setVelocity() {
                vehicle1.straight.activation = velocity
                vehicle2.straight.activation = velocity
            }
            // Update neuron and weight bounds to reasonable values given weight values
            fun updateBounds(w1: Double, w2: Double) {
                val bound = graphicalUpperBound(max(w1, w2))
                vehicle1.leftSynapse.upperBound = bound
                vehicle1.rightSynapse.upperBound = bound
                vehicle2.leftSynapse.upperBound = bound
                vehicle2.rightSynapse.upperBound = bound
                vehicle1.leftSynapse.lowerBound = -bound
                vehicle1.rightSynapse.lowerBound = -bound
                vehicle2.leftSynapse.lowerBound = -bound
                vehicle2.rightSynapse.lowerBound = -bound
                vehicle1.leftTurn.upperBound = bound
                vehicle1.rightTurn.upperBound = bound
                vehicle2.leftTurn.upperBound = bound
                vehicle2.rightTurn.upperBound = bound
                vehicle1.leftTurn.lowerBound = -bound
                vehicle1.rightTurn.lowerBound = -bound
                vehicle2.leftTurn.lowerBound = -bound
                vehicle2.rightTurn.lowerBound = -bound
            }
            addButton("Same pair") {
                vehicle1.leftSynapse.strength = leftWeight
                vehicle1.rightSynapse.strength = rightWeight
                vehicle2.leftSynapse.strength = leftWeight
                vehicle2.rightSynapse.strength = rightWeight
                setVelocity()
                updateBounds(leftWeight, rightWeight)
            }
            addButton("Reversed pair") {
                vehicle1.leftSynapse.strength = leftWeight
                vehicle1.rightSynapse.strength = rightWeight
                vehicle2.leftSynapse.strength = rightWeight
                vehicle2.rightSynapse.strength = leftWeight
                setVelocity()
                updateBounds(leftWeight, rightWeight)
            }
            addButton("Opposite pair") {
                vehicle1.leftSynapse.strength = leftWeight
                vehicle1.rightSynapse.strength = rightWeight
                vehicle2.leftSynapse.strength = -leftWeight
                vehicle2.rightSynapse.strength = -rightWeight
                setVelocity()
                updateBounds(leftWeight, rightWeight)
            }
        }
    }

}





