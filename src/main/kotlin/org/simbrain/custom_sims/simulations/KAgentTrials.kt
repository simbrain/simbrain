package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.launch
import org.simbrain.custom_sims.*

import org.simbrain.network.core.activations
import org.simbrain.network.core.auxValues
import org.simbrain.network.core.connectAllToAll
import org.simbrain.network.core.labels
import org.simbrain.network.layouts.LineLayout
import org.simbrain.util.component1
import org.simbrain.util.component2
import org.simbrain.util.environment.SmellSource
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.projection.Halo
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.awt.geom.Point2D
import kotlin.math.sqrt

val kAgentTrails = newSim {

    val dispersion = 100.0
    val mouseLocation = point(204.0, 343.0)
    val cheeseLocation = point(200.0, 250.0)
    val flowerLocation = point(330.0, 100.0)
    val fishLocation = point(50.0, 100.0)

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Simple Predicter")

    withGui {
        place(networkComponent) {
            location = point(190,10)
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

    connectAllToAll(sensoryNet, predictionNet)
    connectAllToAll(actionNet, predictionNet)

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
        place(odorWorldComponent) {
            location = point(629, 9)
        }
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
            location = point(190,306)
            width = 441
            height = 308
        }

        couplingManager.createCoupling(sensoryNet, plot)

        createControlPanel("Control Panel", 5, 10) {


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
                workspace.coroutineScope.launch {
                    resetObjects()
                    network.clearActivations()
                    val (x, y) = location
                    mouse.location = point(x, y + dispersion)
                    mouse.heading = 90.0
                    straightNeuron.forceSetActivation(1.0)
                    workspace.iterateSuspend((2 * dispersion).toInt())
                    straightNeuron.forceSetActivation(0.0)
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
                workspace.coroutineScope.launch {
                    resetObjects()
                    network.clearActivations()
                    val (x, y) = cheeseLocation
                    mouse.location = point(x, y + dispersion)
                    mouse.heading = 90.0
                    straightNeuron.forceSetActivation(1.0)
                    workspace.iterateSuspend((dispersion * .80).toInt())
                    rightNeuron.forceSetActivation(1.5)
                    workspace.iterateSuspend(25)
                    rightNeuron.forceSetActivation(0.0)
                    workspace.iterateSuspend((dispersion * 1.5).toInt())
                    straightNeuron.forceSetActivation(0.0)
                }
            }
            addButton("Cheese > Fish") {
                workspace.coroutineScope.launch {
                    resetObjects()
                    network.clearActivations()
                    val (x, y) = cheeseLocation
                    mouse.location = point(x, y + dispersion)
                    mouse.heading = 90.0
                    straightNeuron.forceSetActivation(1.0)
                    workspace.iterateSuspend((dispersion * .80).toInt())
                    leftNeuron.forceSetActivation(1.5)
                    workspace.iterateSuspend(25)
                    leftNeuron.forceSetActivation(0.0)
                    workspace.iterateSuspend((dispersion * 1.5).toInt())
                    straightNeuron.forceSetActivation(0.0)
                }
            }
            addButton("Random motion") {
                workspace.coroutineScope.launch {
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
                    straightNeuron.forceSetActivation(0.0)
                    workspace.iterateSuspend(200)
                    cheese.speed = 0.0
                    flower.speed = 0.0
                    fish.speed = 0.0
                }
            }
        }

        // Prediction halo
        plot.projector.isUseColorManager = false
        workspace.addUpdateAction(updateAction("Color projection points") {
            val predictedState: DoubleArray = predictionNet.activations
            Halo.makeHalo(plot.projector, predictedState, errorNeuron.activation.toFloat())
        })
    }


}