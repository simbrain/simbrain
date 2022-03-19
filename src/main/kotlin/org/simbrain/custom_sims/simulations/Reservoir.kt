package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.updateAction
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neuron_update_rules.DecayRule
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.util.*
import org.simbrain.util.environment.SmellSource
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.SmellSensor
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.random.Random

/**
 * Create a reservoir simulation...
 */
val reservoir = newSim {

    // TODO: activation rule, synapse rule

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Reservoir Sim")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val resNeurons = (0..100).map {
        val rule = DecayRule() // TODO: Use your own rule
        rule.decayAmount = .02
        val neuron = Neuron(network, rule)
        neuron
    }
    network.addNetworkModels(resNeurons)
    val reservoir = NeuronCollection(network, resNeurons)
    network.addNetworkModel(reservoir)
    reservoir.label = "Reservoir"
    reservoir.layout(GridLayout())
    reservoir.location = point(0, 0)
    // TODO: Custom connectivity
    resNeurons.forEach() { n1 ->
        resNeurons.forEach() { n2 ->
            if (Math.random() > .9) {
                val synapse = Synapse(n1, n2, 0.0)
                network.addNetworkModel(synapse)
            }
        }
    }

    val inputNeurons = (0 until 4).map {
        val rule = LinearRule()
        val neuron = Neuron(network, rule)
        neuron
    }
    network.addNetworkModels(inputNeurons)
    val inputs = NeuronCollection(network, inputNeurons)
    network.addNetworkModel(inputs)
    inputs.label = "Inputs"
    inputs.layout(LineLayout())
    inputs.location = point(500, -200)

    // Connect input nodes to reservoir
    inputNeurons.forEach { input ->
        (0 until  10).forEach {
            // Select a random neuron from the reservoir
            val resNeuron = resNeurons[Random.nextInt(resNeurons.size)]
            if (Math.random() > .9) {
                val synapse = Synapse(input, resNeuron, 1.0)
                network.addNetworkModel(synapse)
            }
        }
    }

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    // Custom actions to perform when pressing run button
    network.addUpdateAction(updateAction("K Custom Learning Rule") {
        println("Custom update....")
    })

    // ODOR WORLD STUFF

    val cheeseLocation = point(200.0, 250.0)
    val flowerLocation = point(330.0, 100.0)
    val fishLocation = point(50.0, 100.0)

    val odorWorldComponent = OdorWorldComponent("World")
    val odorWorld = odorWorldComponent.world

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

    // odorWorld.update()

    // workspace.addWorkspaceComponent(odorWorldComponent)
    // withGui {
    //     place(odorWorldComponent) {
    //         location = point(403,14)
    //         height = 524
    //         width = 568
    //     }
    // }

    // with(couplingManager) {
    //     straightNeuron couple straightMovement
    //     leftNeuron couple turnLeft
    //     rightNeuron couple turnRight
    //     smellSensors couple sensoryNet
    // }



}