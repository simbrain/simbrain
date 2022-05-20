package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.updateAction
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.util.*
import org.simbrain.util.environment.SmellSource
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.SmellSensor
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.math.cos
import kotlin.math.sin
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
        val rule = IntegrateAndFireRule()
        val neuron = Neuron(network, rule)
        neuron
    }
    network.addNetworkModels(resNeurons)
    val reservoir = NeuronCollection(network, resNeurons)
    network.addNetworkModel(reservoir)
    reservoir.label = "Reservoir"
    reservoir.layout(GridLayout())
    reservoir.location = point(0, 0)
    val sparse = Sparse()
    sparse.connectionDensity = .1
    val syns = sparse.connectNeurons(network, resNeurons, resNeurons)
    val dist = NormalDistribution(0.0, 1.0)
    syns.forEach { synapse ->
        synapse.strength = dist.sampleDouble()
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
        (0 until 10).forEach {
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
    // network.addUpdateAction(updateAction("K Custom Learning Rule") {
    //     println("Custom update....")
    // })

    // ODOR WORLD STUFF

    val odorWorldComponent = OdorWorldComponent("World")
    val odorWorld = odorWorldComponent.world

    // Agent
    val agent = odorWorld.addEntity(EntityType.CIRCLE).apply {
        setCenterLocation(odorWorld.width/2.0,odorWorld.height/2.0)
        heading = 90.0
        addDefaultEffectors()
    }

    // Effectors
    val (_, turnLeft, turnRight) = agent.effectors

    // Sensor
    val smell = SmellSensor(agent)
    smell.theta = 0.0
    smell.radius = EntityType.CIRCLE.imageHeight/2.0
    agent.addSensor(smell)

    // Object sensor option
    // val cheeseSensor1 = ObjectSensor(agent, EntityType.SWISS)
    // cheeseSensor1.theta = 0.0
    // cheeseSensor1.radius = EntityType.CIRCLE.imageHeight/2.0
    // cheeseSensor1.decayFunction.dispersion = 200.0
    // inputs.setClamped(true)// If this sensor is used must clamp the nodes
    // agent.addSensor(cheeseSensor1)

    // Objects
    val cheese = odorWorld.addEntity(EntityType.SWISS).apply {
        val (x, y) = point(200.0, 250.0)
        setLocation(x, y)
        smellSource = SmellSource(doubleArrayOf(1.0, .2, .5, .1, 1.0)).apply {
            this.dispersion = 200.0
        }
    }
    fun updateCheeseLocation() {
        //TODO: Update when Yulin's refactor is done
        cheese.setCenterLocation(agent.centerX + 100 * cos(network.time), agent.centerY - 100 * sin(network.time))
    }
    updateCheeseLocation()
    workspace.addUpdateAction(updateAction("Move cheese") {
        updateCheeseLocation()
    })

    workspace.addWorkspaceComponent(odorWorldComponent)
    withGui {
        place(odorWorldComponent) {
            location = point(403,14)
            height = 524
            width = 568
        }
    }

    with(couplingManager) {
        smell couple inputs
        // cheeseSensor1 couple inputs

        // Arbitrary for now
        // resNeurons[2] couple turnLeft
        // resNeurons[3] couple turnLeft
    }


}