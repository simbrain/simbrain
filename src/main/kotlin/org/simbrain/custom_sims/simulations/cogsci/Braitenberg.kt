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

    val vehicle1 = Vehicle("Vehicle 1", EntityType.CIRCLE, Point2D.Double(120.0, 245.0))
    val vehicle2 = Vehicle("Vehicle 2", EntityType.CIRCLE, Point2D.Double(320.0, 245.0))

    addSidebarInfoFromFile("Braitenberg.html")

    withGui {
        place(vehicle1.networkComponent, 186, 4, 359, 327)
        place(vehicle2.networkComponent, 186, 332, 361, 319)
        place(oc, 548, 3, 496, 646)
    }

    var leftWeight = 100.0
    var rightWeight = 50.0
    var velocity = 1.0

    withGui {
        createControlPanel("Control Panel", 5, 5) {
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

    addSidebarInfo(
        """ 
            # Introduction
            The Braitenberg Two Braitenberg Vehicles Simulation explores the reactive behaviors in agents, the vehicles, from sensorimotor connections. The vehicles reacts to their environment, based on connections between its sensors and actuators. The sensors detect the stimuli, which determine the strength and direction of movement, and the actuators control the actual movement of the vehicles.
            
            # What to Do
            1. "Run workspace" on the top menu bar to see how the vehicles reacts to its environment. 
            2. Edit the "Left weight", "Right weight", and the "Velocity" to see how the vehicles adapt to the change.
                - The vehicles react to these changes, and react to each other's changes based off their sensor motors. 
            3. The weights in the "Network" window changes according to the isopod's actions. 
                - The "Circle (L)" and "Circle (R)" weights are the inputs that detect the stimuli in the environment of the "empty.tmx" window.
                - The "Left", "Straight", and "Right" weights are the outputs of the isopod's motor actions. 
                - The magnitude of the weights indicate the strength of the connection and influence the output has on the vehicle's actions, with a higher weight increasing the likelihood and intensity of the action.
            3. Click "Same pair", "Reversed pair", and "Opposite pair" to observe the vehicles' behaviors. The vehicles move towards the other's positive sensor motors.
                - in "Same pair", the vehicles have the same weight connections between their inputs and outputs, causing them to move in a circle.
                - in "Reversed pair", the vehicles have the same weight connections on opposite outputs, causing them to move alongside to each other.
                - in "Opposite pair", one vehicle has positive weight connections, while the other has negative weight connections, causing them to repel each other. 
        """.trimIndent()
    )

}





