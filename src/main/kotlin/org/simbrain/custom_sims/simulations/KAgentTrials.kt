package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.util.*
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

val kAgentTrials = newSim {

    val dispersion = 100.0

    val cheeseLocation = point(200.0, 250.0)
    val flowerLocation = point(330.0, 100.0)
    val fishLocation = point(50.0, 100.0)

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Simple Predicter")

    withGui {
        place(networkComponent) {
            location = point(195, 9)
            size = point(447, 296)
        }
    }

    val network = networkComponent.network

    val sensoryNet = network.addNeuronGroup(3, point(-9.25, 95.93)).apply {
        label = "Sensory"
        neuronList.labels = listOf("Cheese", "Flower", "Fish")
    }

    val actionNet = network.addNeuronGroup(3, point(0.0, -0.79)).apply {
        label = "Actions"
        setClamped(true)
        neuronList.labels = listOf("Straight", "Right", "Left")
    }
    val (straightNeuron, rightNeuron, leftNeuron) = actionNet.neuronList

    val predictionNet = network.addNeuronGroup(3, point(231.02, 24.74)).apply {
        label = "Predicted"
    }

    with(network) {
        connectAllToAll(sensoryNet, predictionNet)
        connectAllToAll(actionNet, predictionNet)
    }

    val errorNeuron = network.addNeuron {
        label = "Error"
        setLocation(268.0, 108.0)
    }

    var lastPredicted = predictionNet.neuronList.activations

    network.addUpdateAction(networkUpdateAction("K Custom Learning Rule") {
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
        setLocation(204.0, 343.0)
        heading = 90.0
        addDefaultEffectors()
        addSensor(SmellSensor(this))
        manualStraightMovementIncrement = 2.0
        manualMotionTurnIncrement = 2.0
    }

    val (straightMovement, turnLeft, turnRight) = mouse.effectors
    val (smellSensors) = mouse.sensors

    val cheese = odorWorld.addEntity(EntityType.SWISS).apply {
        val (x, y) = cheeseLocation
        setLocation(x, y)
        smellSource = SmellSource(doubleArrayOf(1.0, 0.0, 0.0)).apply {
            this.dispersion = dispersion
        }
    }

    val flower = odorWorld.addEntity(EntityType.FLOWER).apply {
        val (x, y) = flowerLocation
        setLocation(x, y)
        smellSource = SmellSource(doubleArrayOf(0.0, 1.0, 0.0)).apply {
            this.dispersion = dispersion
        }
    }

    val fish = odorWorld.addEntity(EntityType.FISH).apply {
        val (x, y) = fishLocation
        setLocation(x, y)
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
            location = point(194,312)
            size = point(441, 308)
        }

        couplingManager.createCoupling(sensoryNet, plot)

        createControlPanel("Control Panel", 5, 10) {

            fun positionMouse(location: Point2D) {
                network.clearActivations()
                val (x, y) = location
                mouse.setLocation(x, y + dispersion)
                mouse.heading = 90.0
                straightNeuron.forceSetActivation(1.0)
                // TODO: move to a location by iterating while not at location
                workspace.iterate((4 * dispersion).toInt())
                straightNeuron.forceSetActivation(0.0)
            }

            addButton("Cheese") {
                positionMouse(cheeseLocation)
            }
            addButton("Fish") {
                positionMouse(fishLocation)
            }
            addButton("Flower") {
                positionMouse(flowerLocation)
            }
            addButton("Cheese > Flower") {
                network.clearActivations()
                val (x, y) = cheeseLocation
                mouse.setLocation(x, y + dispersion)
                mouse.heading = 90.0
                straightNeuron.forceSetActivation(1.0)
                workspace.iterate(50)
                rightNeuron.forceSetActivation(1.5)
                workspace.iterate(25)
                rightNeuron.forceSetActivation(0.0)
                workspace.iterate(220)
                straightNeuron.forceSetActivation(0.0)
            }
            addButton("Cheese > Fish") {
                network.clearActivations()
                val (x, y) = cheeseLocation
                mouse.setLocation(x, y + dispersion)
                mouse.heading = 90.0
                straightNeuron.forceSetActivation(1.0)
                workspace.iterate(50)
                leftNeuron.forceSetActivation(1.5)
                workspace.iterate(25)
                leftNeuron.forceSetActivation(0.0)
                workspace.iterate(220)
                straightNeuron.forceSetActivation(0.0)
            }
            addButton("Solar System") {
                network.clearActivations()
                cheese.velocityX = 2.05
                cheese.velocityY = 2.05
                flower.velocityX = 2.5
                flower.velocityY = 2.1
                fish.velocityX = -2.5
                fish.velocityY = 1.05
                val (x, y) = cheeseLocation
                mouse.setLocation(x, y + dispersion)
                mouse.heading = 90.0
                straightNeuron.forceSetActivation(0.0)
                workspace.iterate(200)
                cheese.velocityX = 0.0
                cheese.velocityY = 0.0
                flower.velocityX = 0.0
                flower.velocityY = 0.0
                fish.velocityX = 0.0
                fish.velocityY = 0.0
            }
        }

        // Uncomment for prediction halo
        plot.projectionModel.projector.isUseColorManager = false
        workspace.addUpdateAction(updateAction("Color projection points") {
            val predictedState: DoubleArray = predictionNet.activations
            Halo.makeHalo(plot.projectionModel.projector, predictedState, errorNeuron.activation.toFloat())
        })
    }


}