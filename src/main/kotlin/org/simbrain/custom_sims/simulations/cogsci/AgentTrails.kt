package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.launch
import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.network.layouts.LineLayout
import org.simbrain.util.SmellSource
import org.simbrain.util.component1
import org.simbrain.util.component2
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.projection.DataPoint
import org.simbrain.util.projection.HaloColoringManager
import org.simbrain.workspace.updater.updateAction
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.awt.geom.Point2D
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.math.sqrt

val kAgentTrails = newSim {

    val dispersion = 70.0
    val mouseLocation = point(120.0, 245.0)
    val cheeseLocation = point(120.0, 180.0)
    val flowerLocation = point(200.0, 100.0)
    val fishLocation = point(50.0, 100.0)

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Simple Predicter")

//Sensory States + Predictions (861, 327, 441, 308)
//Simple Predicter (856, 0, 439, 296)
//empty.tmx (532, 248, 315, 388)
//Control Panel (533, 0, 151, 242)
//Information (0, 0, 516, 632)
//>

val docViewer = addDocViewer(
    "Information",
    """ 
        # Introduction
        In this simulation, the network consists of neurons, which, when activated, make the agent move towards it.
        
        The prediction network predicts the next state of the sensory network based on the current activations of the sensory and motor neurons. Based on what the agent senses and how it moves, it predicts what it will sense next via linear scaling function of the distance between the sensor and that object. 
        
        The points on the plots are colored according to the predictions of the network, with the states that were predicted to occur next colored red with varying degrees of saturation. The red predicted next states moves with the current point up to the end. After the agent has passed the 3 objects, its state in the predictor window can be called a "bouquet of three arcs", each arc corresponding to one of the objects. After the agent has passed 5 excursions, it creates a "spandrelled bouquet", corresponding the flower and fish to the cheese. 
        
        After it has done 5 excursions, it correctly predict the path that it will take. For example, after 5 excursions, it can correctly predict that it will take the mixed cheese/flower state based on what it senses in its sensory nodes and the actions it is taking.  
        
        For more info see <https://escholarship.org/content/qt5x72z7j1/qt5x72z7j1_noSplash_5dcf27b77bcad405b825e567de21a037.pdf>
        
        # What to Do
        In this simulation, the mouse is the agent with 3 objects, the cheese, fish, and flower. The mouse has 5 excursions it can take: to the cheese, to the fish, to the flower, to the cheese then flower, and to the cheese then fish. The sensory states and predictions show each excursion. The red corresponds to the path that the mouse is currently travelling, and the gray shows all the possible paths that the mouse can take. 
        1. Select an object from the "Control Panel", and the mouse will be activated and move towards the object you selected on the "Simple Predictor" window. 
            - For example, if the cheese-sensing neuron is on top of the cheese, the it is maximally activated.
            - As the agent moves away from the cheese, the neuron's activation diminishes to 0.
        2. In the "Sensory States + Predictions" window, the trail that the mouse walks is plotted in gray, and the predicted path of the mouse is red. 
        3. Repeat this until all 5 paths are taken 
        4. After all 5 Paths, Select any object again from the "Control Panel", and the predictor will be able to crrectly predict where the mouse travels
        
        
        
    """.trimIndent()
)


    withGui {
        place(docViewer, 0, 0, 516, 632)
        place(networkComponent) {
            location = point(856, 0)
            width = 439
            height = 296
        }
    }

    val network = networkComponent.network

    val sensoryNet = network.addNeuronGroup(3, point(-9.25, 95.93)).apply {
        label = "Sensory"
        neuronList.labels = listOf("Cheese", "Flower", "Fish")
        applyLayout(LineLayout())
    }

    val actionNet = network.addNeuronGroup(3, point(0.0, -0.79)).apply {
        label = "Actions"
        setClamped(true)
        neuronList.labels = listOf("Straight", "Right", "Left")
        applyLayout(LineLayout())
    }
    val (straightNeuron, rightNeuron, leftNeuron) = actionNet.neuronList

    val predictionNet = network.addNeuronGroup(3, point(231.02, 24.74)).apply {
        label = "Predicted"
        applyLayout(LineLayout())
    }

    network.connectAllToAll(sensoryNet, predictionNet)
    network.connectAllToAll(actionNet, predictionNet)

    val errorNeuron = network.addNeuron {
        label = "Error"
        setLocation(268.0, 108.0)
    }

    var lastPredicted = predictionNet.neuronList.activations

    network.addUpdateAction(updateAction("Train prediction network") {
        val learningRate = 0.1

        val errors = (sensoryNet.neuronList.activations zip lastPredicted).map { (a, b) -> a - b }
        predictionNet.neuronList.auxValues = errors

        val sse = errors.map { it * it }.sum()
        errorNeuron.activation = sqrt(sse)

        network.flatSynapseList.forEach {
            it.strength = it.strength + learningRate * it.source.activation * it.target.auxValue
        }

        lastPredicted = predictionNet.neuronList.activations
    })

    val odorWorldComponent = addOdorWorldComponent()

    withGui {
        place(odorWorldComponent,532, 248, 315, 388)
    }

    val odorWorld = odorWorldComponent.world.apply {
        isObjectsBlockMovement = false
    }

    val mouse = odorWorld.addEntity(EntityType.MOUSE).apply {
        location = mouseLocation
        heading = 90.0
        addDefaultEffectors()
        addSensor(SmellSensor())
        manualMovement.manualStraightMovementIncrement = 2.0
        manualMovement.manualMotionTurnIncrement = 2.0
    }

    val (straightMovement, turnLeft, turnRight) = mouse.effectors
    val (smellSensors) = mouse.sensors

    val cheese = odorWorld.addEntity(EntityType.SWISS).apply {
        location = cheeseLocation
        smellSource = SmellSource(doubleArrayOf(1.0, 0.0, 0.0)).apply {
            this.dispersion = dispersion
        }
    }

    val flower = odorWorld.addEntity(EntityType.FLOWER).apply {
        location = flowerLocation
        smellSource = SmellSource(doubleArrayOf(0.0, 1.0, 0.0)).apply {
            this.dispersion = dispersion
        }
    }

    val fish = odorWorld.addEntity(EntityType.FISH).apply {
        location = fishLocation
        smellSource = SmellSource(doubleArrayOf(0.0, 0.0, 1.0)).apply {
            this.dispersion = dispersion
        }
    }

    odorWorld.update()

    with(couplingManager) {
        straightNeuron couple straightMovement
        leftNeuron couple turnLeft
        rightNeuron couple turnRight
        smellSensors couple sensoryNet
    }

    withGui {
        val plot = addProjectionPlot("Sensory States + Predictions").apply {
            projector.tolerance = .001
        }
        place(plot) {
            location = point(861, 327)
            width = 441
            height = 308
        }

        couplingManager.createCoupling(sensoryNet, plot)

        createControlPanel("Control Panel", 533, 0) {


            fun resetObjects() {
                cheese.location = cheeseLocation
                fish.location = fishLocation
                flower.location = flowerLocation
                mouse.location = mouseLocation
                cheese.heading = 90.0
                fish.heading = 90.0
                flower.heading = 90.0
                mouse.heading = 90.0
            }

            fun moveMouseVerticallyThroughLocation(location: Point2D) {
                workspace.launch {
                    resetObjects()
                    network.clearActivations()
                    val (x, y) = location
                    mouse.location = point(x, y + dispersion)
                    mouse.heading = 90.0
                    straightNeuron.activation = 1.0
                    workspace.iterateSuspend((2 * dispersion).toInt())
                    straightNeuron.activation = 0.0
                }
            }

            addButton("Cheese") {
                moveMouseVerticallyThroughLocation(cheeseLocation)
            }
            addButton("Fish") {
                moveMouseVerticallyThroughLocation(fishLocation)
            }
            addButton("Flower") {
                moveMouseVerticallyThroughLocation(flowerLocation)
            }
            addButton("Cheese > Flower") {
                workspace.launch {
                    resetObjects()
                    network.clearActivations()
                    val (x, y) = cheeseLocation
                    mouse.location = point(x, y + dispersion)
                    mouse.heading = 90.0
                    straightNeuron.activation = 1.0
                    workspace.iterateSuspend((dispersion * .80).toInt())
                    rightNeuron.activation = 1.5
                    workspace.iterateSuspend(25)
                    rightNeuron.activation = 0.0
                    workspace.iterateSuspend((dispersion * 1.5).toInt())
                    straightNeuron.activation = 0.0
                }
            }
            addButton("Cheese > Fish") {
                workspace.launch {
                    resetObjects()
                    network.clearActivations()
                    val (x, y) = cheeseLocation
                    mouse.location = point(x, y + dispersion)
                    mouse.heading = 90.0
                    straightNeuron.activation = 1.0
                    workspace.iterateSuspend((dispersion * .80).toInt())
                    leftNeuron.activation = 1.5
                    workspace.iterateSuspend(25)
                    leftNeuron.activation = 0.0
                    workspace.iterateSuspend((dispersion * 1.5).toInt())
                    straightNeuron.activation = 0.0
                }
            }
            addButton("Random motion") {
                workspace.launch {
                    resetObjects()
                    network.clearActivations()
                    cheese.randomizeLocationAndHeading()
                    flower.randomizeLocationAndHeading()
                    fish.randomizeLocationAndHeading()
                    cheese.speed = 2.0
                    flower.speed = 2.0
                    fish.speed = 2.0
                    val (x, y) = cheeseLocation
                    mouse.location = point(odorWorld.width/2, odorWorld.height/2)
                    mouse.heading = 90.0
                    straightNeuron.activation = 0.0
                    workspace.iterateSuspend(200)
                    cheese.speed = 0.0
                    flower.speed = 0.0
                    fish.speed = 0.0
                }
            }
        }


        val haloColoringManager = HaloColoringManager()
        plot.projector.coloringManager = haloColoringManager

        workspace.addUpdateAction(updateAction("Color projection points") {
            val predictedState = predictionNet.activationArray
            haloColoringManager.customCenter = DataPoint(predictedState)
            haloColoringManager.radius = errorNeuron.activation + 0.2
        })
    }


}