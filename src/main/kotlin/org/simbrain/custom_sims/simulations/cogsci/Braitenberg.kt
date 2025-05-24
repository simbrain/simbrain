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
        place(vehicle1.networkComponent, 240, 4, 359, 327)
        place(vehicle2.networkComponent, 240, 332, 361, 319)
        place(oc, 590, 3, 496, 646)
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
            
            # Braitenberg Vehicles

            This simulation accompanies the book *The Open Dynamics of Braitenberg Vehicles*, MIT Press, 2023, by Scott Hotton and Jeff Yoshimi.

            In this simulation, you can set the weights of two Braitenberg vehicles and observe the resulting behavior. By using the sample parameters below, you can produce revolving behaviors (where the two vehicles revolve around each other), translating behaviors (where they move alongside one another), and various types of "meandering" behaviors where they spin while they revolve around each other or travel side by side. Other behaviors are possible, many of which are described in the book.
            
            The simulation explores the reactive behaviors in agents, the vehicles, from sensorimotor connections. The vehicles reacts to their environment, based on connections between its sensors and actuators. The sensors detect the stimuli, which determine the strength and direction of movement, and the actuators control the actual movement of the vehicles.
                
            The simulation shows two vehicles, Vehicle 1 and Vehicle 2. You can set the properties of the vehicles directly in the network windows, as with any Simbrain simulation, although this simulation is designed to make it easy for you to reproduce the behaviors described in the book.

            In general, set the weights directly or with the button panel, press the play button, and watch the vehicles go! If they move too far away from each other, you can grab them and pull them next to each other.

            # What to Do (First Pass)
            
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
            
            ## The Button Panel
            
            The button panel contains two fields labeled **"left weight"** and **"right weight"** where you can set the weights of the two vehicles, using one of the three buttons described below. In general, pairs of positive weights create pursuers, and pairs of negative weights create avoiders. The buttons work as follows:
            
            - **Same pair**: Takes the left and right weights in those text fields and copies them to both vehicles. Thus, the two vehicles have the same pair of weights. This is a point in what we call \(W_{same}\) in the book. These vehicles can pursue each other in circles or in meanders.
            - **Reversed pair**: Takes the left and right weights and applies them directly to Vehicle 1, and in reverse order to Vehicle 2. Thus, we have a pair of vehicles with reversed weights. Example: Vehicle 1 has weights (100, 50) and Vehicle 2 has weights (50, 100). This is a point in what we call \(W_{rev}\) in the book. These vehicles can move side by side, sometimes while meandering or counter-rotating.
            - **Opposite pair**: Takes the left and right weights and applies them directly to Vehicle 1, then multiplies each by -1 and applies these "opposite" values to Vehicle 2. This produces a pair of vehicles with opposite weights. These form pursuer-avoider pairs.
            
            # Example Behaviors
            
            Enter the values below in the left weight and right weight fields, press the button indicated, and press run to see the corresponding behavior.
            
            - **(100, 50)** and **Same pair**: Revolving behavior (attracting revolving type relative equilibrium).
            - **(100, 80)** and **Same pair**: Revolving meander (attracting relative periodic orbit).
            - **(100, 50)** and **Reversed pair**: Side by side behavior (attracting translating type relative equilibrium).
            - **(20, 25)** and **Reversed pair**: Side by side meander (attracting translating type relative periodic orbit).
            - **(100, 50)** and **Opposite pair**: Pursuer-avoider behavior.
            
            # Bifurcations
            You can see several bifurcations by setting the **left weight** to 100 and then varying the **right weight** (each time you change the number, press **Same pair** and run the simulation to get a feel for what happens). You can start at 75 and slowly raise the value past 80, and then past 100. The Hopf-like bifurcation occurs around 80, and at 100 we pass through \(W_{eq}\) (equal weights) and the vehicles change direction. Other behaviors from the book can be observed; for example, around 76, we see "billiard-like" behaviors.
            
            
            
            
            

        """.trimIndent()
    )

}


