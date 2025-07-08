package org.simbrain.custom_sims.simulations.braitenberg

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.util.graphicalUpperBound
import org.simbrain.util.place
import org.simbrain.world.odorworld.entities.EntityType
import java.awt.geom.Point2D
import kotlin.math.abs
import kotlin.math.max

/**
 * Braitenberg sim to accompany "Open Dynamics of Braitenberg Vehicles"
 */
val braitenbergSim = newSim {

    workspace.clearWorkspace()

    val oc = addOdorWorldComponent("World") {
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

    val vehicle1 = Vehicle("Vehicle 1", EntityType.Circle, Point2D.Double(120.0, 245.0))
    val vehicle2 = Vehicle("Vehicle 2", EntityType.Circle, Point2D.Double(320.0, 245.0))

    withGui {
        place(vehicle1.networkComponent, 230, 0, 360, 323)
        place(vehicle2.networkComponent, 230, 323, 360, 323)
        place(oc, 590, 0, 496, 646)
    }

    var leftWeight = 30.0
    var rightWeight = 50.0
    var velocity = 1.0

    withGui {
        createControlPanel("Control Panel", 5, 5) {
            addFormattedNumericTextField("Left weight", initValue = leftWeight) {
                leftWeight = it
            }
            addFormattedNumericTextField("Right weight", initValue = rightWeight) {
                rightWeight = it
            }
            addFormattedNumericTextField("Velocity", initValue = velocity) {
                velocity = it
            }
            fun setVelocity() {
                vehicle1.straight.activation = velocity
                vehicle2.straight.activation = velocity
            }

            // Update neuron and weight bounds to reasonable values given weight values
            fun updateBounds(w1: Double, w2: Double) {
                val upperBound = graphicalUpperBound(max(abs(w1), abs(w2)))

                vehicle1.leftSynapse.upperBound = upperBound
                vehicle1.rightSynapse.upperBound = upperBound
                vehicle2.leftSynapse.upperBound = upperBound
                vehicle2.rightSynapse.upperBound = upperBound
                vehicle1.leftSynapse.lowerBound = -upperBound
                vehicle1.rightSynapse.lowerBound = -upperBound
                vehicle2.leftSynapse.lowerBound = -upperBound
                vehicle2.rightSynapse.lowerBound = -upperBound
                vehicle1.leftTurn.upperBound = upperBound
                vehicle1.rightTurn.upperBound = upperBound
                vehicle2.leftTurn.upperBound = upperBound
                vehicle2.rightTurn.upperBound = upperBound
                vehicle1.leftTurn.lowerBound = -upperBound
                vehicle1.rightTurn.lowerBound = -upperBound
                vehicle2.leftTurn.lowerBound = -upperBound
                vehicle2.rightTurn.lowerBound = -upperBound
            }
            fun initSamePair() {
                vehicle1.leftSynapse.strength = leftWeight
                vehicle1.rightSynapse.strength = rightWeight
                vehicle2.leftSynapse.strength = leftWeight
                vehicle2.rightSynapse.strength = rightWeight
                setVelocity()
                updateBounds(leftWeight, rightWeight)
            }
            addButton("Same pair") {
                initSamePair()
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
            initSamePair() // Default to same pair / revolving config
        }
    }

    addSidebarInfo(
        """     
        # Introduction
        
        This simulation accompanies the book _The Open Dynamics of Braitenberg Vehicles_ by Scott Hotton and Jeff Yoshimi.
        
        In this simulation, you can set the weights of two Braitenberg vehicles and observe the resulting dynamical behavior. By using the sample parameters below, you can produce revolving behaviors (where the two 
        vehicles revolve around each other), translating behaviors (where they move alongside one another), and various types of _meandering_ behaviors where they spin while they revolve around each other or travel 
        side by side. Other behaviors are possible, many of which are described in the book.
        
        For an introduction to some of the behaviors of Braitenberg vehicles individually, see the `Pursuer` and `Avoider` simulations.
        
        For a quick sense of what is possible in this simulation, press `play` to run the simulation, click in the 
        world window and press `T` to turn on trails, and [observe the pattern they make](https://bsky.app/profile/jyoshimi.bsky.social/post/3lsbun2wtdc2d).
         
        For more simulations you can skip to __What to do__ below.
        
        # Simulation Details
        
        The simulation explores the reactive behaviors in the Braitenberg vehicles from sensorimotor connections. The vehicles react to their environment based on connections between its sensors and 
        actuators (e.g., turn response). The sensors detect the stimuli, which determine the strength and direction of movement, and the actuators control the actual movement of the vehicles.
        
        The simulation shows two vehicles, Vehicle `1` and Vehicle `2`. You can set the properties of the vehicles directly in the network windows although this simulation is designed to make it easy for 
        you to reproduce the behaviors described in the book.
        
        In each vehicle, the `Circle (L)` neuron and the `Circle (R)` neuron are the sensors that detects stimuli in the environment (e.g., odor world). The weights connected between the `Circle (L)` and `Circle (R)` 
        to their actuators links the stimuli to the vehicles' motor response. The weight strength influences how the vehicle behaves; for example, a higher weight strength increases the likelihood and intensity of 
        the behavior (e.g., turn left, turn right). The vehicles will move towards the other's positive sensor motors.
        
        ## Button Panel
        
        The button panel contains two text boxes labeled `left weight` and `right weight` where you can set the weights of the two vehicles, using one of the three buttons described below. In general, pairs of 
        positive weights create pursuers, and pairs of negative weights create avoiders. The buttons work as follows:
        
        1) `Same pair`: Copy left and right weight text values to both vehicles. Thus, the two vehicles have the same pair of weights. This is a point in what we call W<sub>same</sub> 
        in the book. These vehicles can pursue each other in circles or in meanders.
        
        2) `Reversed pair`: Copy left and right weight text values and applies them directly to Vehicle `1`, and in reverse order to Vehicle `2`. Thus, we have a pair of vehicles with reversed weights. 
        This is a point in what we call W<sub>rev</sub> in the book. These vehicles can move side by side, sometimes while meandering or counter-rotating.
        
            - Example: Vehicle `1` has weights `(100, 50)` and Vehicle `2` has weights `(50, 100)`. 
        
        3) `Opposite pair`: Copy left and right weight text values and applies them directly to Vehicle `1`, then multiplies each by `-1` and applies these opposite values to Vehicle `2`. This produces a pair of vehicles
        with opposite weights. This forms a pursuer-avoider pair (repels one another).
        
            - Example: Vehicle `1` has weights `(100, 50)` and Vehicle `2` has weights `(-50, -100)`. 
        
        # What to Do
        
        In this simulation similar to the other Braitenberg simulations, simply press the `play` button on the top toolbar for the simulation to run and observe the behaviors. Below are the steps:
        
        1. Press the `play` button on the top menu bar and observe how the vehicles reacts to its environment.        
        1. Edit the `Left weight`, `Right weight`, and the `Velocity` text boxes to adjust the vehicles' parameters.
        1. Press either `Same pair`, `Reversed pair`, and `Opposite pair` to confirm the weight changes and to observe changes in the vehicles' behaviors.
        1. If you press `T` it will toggle trails. This can be useful to show what patterns the vehicles are making. Press it once to turn on trails and again to turn it off (it can slow down a simulation after a while). 
        1. If the vehicles get too far from one another, press the `play` button again to pause the simulation and manually move the vehicles near each other.
        1. After doing so, press the `play` button to start the simulation again.
        
        **Note**: After changing the weight strength values, press one of the three buttons to confirm the change.
        
        ## Some Sample Behaviors
        
        Below are some example behaviors that you can use to observe a variety of behaviors that emerge from the interaction of two Braitenberg vehicles.
        
        1) `(100, 50)` and `Same pair`: Revolving behavior (attracting revolving type relative equilibrium).
        2) `(100, 80)` and `Same pair`: Revolving meander (attracting relative periodic orbit).
        3) `(100, 50)` and `Reversed pair`: Side by side behavior (attracting translating type relative equilibrium).
        4) `(20, 25)` and `Reversed pair`: Side by side meander (attracting translating type relative periodic orbit).
        5) `(100, 50)` and `Opposite pair`: Pursuer-avoider behavior.
        
        ## Observing Bifurcations
        
        You can see several bifurcations by setting the `left weight` to `100` and then varying the `right weight` (each time you change the number, press `Same pair` and run the simulation to get a feel for what 
        happens). 
        
        You can start at `75` and slowly raise the value past `80`, and then past `100`. The Hopf-like bifurcation occurs around `80`, and at `100` we pass through W<sub>eq</sub> (equal weights) and the 
        vehicles change direction. Other behaviors from the book can be observed; for example, around `76`, we see _billiard-like_ behaviors.
        
        # References
        
        Braitenberg, V. (1986). [_Vehicles: Experiments in synthetic psychology_](https://mitpress.mit.edu/9780262521123/vehicles/). MIT press.
        
        Hotton, S., & Yoshimi, J. (2024). [_The Open Dynamics of Braitenberg Vehicles_](https://mitpress.mit.edu/9780262548199/the-open-dynamics-of-braitenberg-vehicles/). MIT Press.
        
        # Credits
        
        Jasmine Lau
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html) 
        
        Kanly Thao
            
        """.trimIndent()
    )

}


