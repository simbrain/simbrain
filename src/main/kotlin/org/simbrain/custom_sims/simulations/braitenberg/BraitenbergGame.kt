package org.simbrain.custom_sims.simulations.braitenberg

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.graphicalUpperBound
import org.simbrain.util.place
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.fitWorldToFrameSize
import java.awt.geom.Point2D
import kotlin.math.max

/**
 * Testing
 */
val braitenbergGame = newSim {

    var leftWeight = 1.0
    var rightWeight = 1.0
    var velocity = .1

    workspace.clearWorkspace()

    val oc = addOdorWorldComponent("Obstacle Course")
    //oc.world.isObjectsBlockMovement = false
    oc.world.isUseCameraCentering = false
    oc.world.addEntity(257, 191, EntityType.Poison)
    oc.world.addEntity(323, 286, EntityType.Poison)
    oc.world.addEntity(398, 335, EntityType.Poison)
    oc.world.addEntity(500, 184, EntityType.Swiss)

    class Vehicle(name: String, entityType: EntityType, entityOffset: Point2D) {

        val networkComponent = addNetworkComponent(name)

        val network get() = networkComponent.network

        val agent = oc.world.addEntity(entityOffset.x, entityOffset.y, entityType).apply {
            addLeftRightSensors(EntityType.Swiss, 270.0)
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

    val vehicle1 = Vehicle("Vehicle 1", EntityType.Circle, Point2D.Double(194.0, 407.0))

    withGui {
        place(vehicle1.networkComponent, 53, 282, 359, 327)
        place(oc, 462, 19, 600, 600)
        oc.getDesktopComponentAs<OdorWorldDesktopComponent>().fitWorldToFrameSize()
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

    addSidebarInfo(
    """ 
            
    # Braitenberg Game
        
    Work in progress.        

    ### References
        
    1) Braitenberg, V. (1986). [_Vehicles: Experiments in synthetic psychology_](https://mitpress.mit.edu/9780262521123/vehicles/). MIT press.
            
    2) Hotton, S., & Yoshimi, J. (2024). [_The Open Dynamics of Braitenberg Vehicles_](https://mitpress.mit.edu/9780262548199/the-open-dynamics-of-braitenberg-vehicles/). MIT Press.
            
    ### Credits
            
    ...
            
    """.trimIndent()
    )

}





