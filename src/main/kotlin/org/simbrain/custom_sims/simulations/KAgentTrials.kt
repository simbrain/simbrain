package org.simbrain.custom_sims.simulations

import org.simbrain.network.util.*
import org.simbrain.util.environment.SmellSource
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.squaredError
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.SmellSensor

val kAgentTrials = newSim {

    val dispersion = 100.0

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
    val (cheeseNeuron, flowerNeuron, fishNeuron) = sensoryNet.neuronList

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

        val squareError = sensoryNet.neuronList.activations squaredError lastPredicted
        predictionNet.neuronList.auxValues = squareError

        val sumError = squareError.sum()
        errorNeuron.activation = sumError

        network.flatSynapseList.forEach {
            it.strength = it.strength + learningRate * it.source.activation * it.target.auxValue
        }

        lastPredicted = sensoryNet.neuronList.activations
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
        addDefaultSensorsEffectors()
        addSensor(SmellSensor(this))
        manualStraightMovementIncrement = 2.0
        manualMotionTurnIncrement = 2.0
    }

    val cheese = odorWorld.addEntity(EntityType.SWISS).apply {
        smellSource = SmellSource(doubleArrayOf(1.0, 0.0, 0.0)).apply {
            this.dispersion = dispersion
        }
    }

    val flower = odorWorld.addEntity(EntityType.SWISS).apply {
        smellSource = SmellSource(doubleArrayOf(0.0, 1.0, 0.0)).apply {
            this.dispersion = dispersion
        }
    }

    val fish = odorWorld.addEntity(EntityType.SWISS).apply {
        smellSource = SmellSource(doubleArrayOf(0.0, 0.0, 1.0)).apply {
            this.dispersion = dispersion
        }
    }

    odorWorld.update()


}