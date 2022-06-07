package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.updateAction
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.network.core.SpikingNeuronUpdateRule
import org.simbrain.network.core.SynapseGroup2
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.*
import org.simbrain.util.environment.SmellSource
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.lang.Double.max
import java.lang.Double.min
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Create a reservoir simulation...
 */
val objectTrackingSim = newSim {

    val numResNeurons = 200
    val sensoryNeurons = 25

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Spontaneous Object Tracking")
    val network = networkComponent.network

    // Most connections in network use 10% density
    val sparse = Sparse()
    sparse.connectionDensity = .1


    // Add a self-connected neuron array to the network
    val resNeurons = (0..numResNeurons).map {
        val rule = AllostaticUpdateRule()
        val neuron = Neuron(network, rule)
        neuron
    }
    network.addNetworkModels(resNeurons)
    val reservoir = NeuronCollection(network, resNeurons)
    network.addNetworkModel(reservoir)
    reservoir.label = "Reservoir"
    reservoir.layout(GridLayout())
    reservoir.location = point(0, 0)
    val reservoirSynapseGroup = SynapseGroup2(reservoir, reservoir, sparse)
    network.addNetworkModel(reservoirSynapseGroup)
    val dist = NormalDistribution(0.0, 2.0)
    reservoirSynapseGroup.synapses.forEach { s ->
        s.strength = dist.sampleDouble()
    }

    // Left inputs
    val leftInputNeurons = (0 until sensoryNeurons).map {
        val rule = LinearRule()
        val neuron = Neuron(network, rule)
        neuron
    }
    network.addNetworkModels(leftInputNeurons)
    val leftInputs = NeuronCollection(network, leftInputNeurons)
    network.addNetworkModel(leftInputs)
    leftInputs.label = "Left Inputs"
    leftInputs.layout(GridLayout())
    leftInputs.location = point(-616, -195)

    // Right inputs
    val rightInputNeurons = (0 until sensoryNeurons).map {
        val rule = LinearRule()
        val neuron = Neuron(network, rule)
        neuron
    }
    network.addNetworkModels(rightInputNeurons)
    val rightInputs = NeuronCollection(network, rightInputNeurons)
    network.addNetworkModel(rightInputs)
    rightInputs.label = "Right Inputs"
    rightInputs.layout(GridLayout())
    rightInputs.location = point(-616, 225)

    // Connect input nodes to reservoir
    val leftInputsToRes = SynapseGroup2(leftInputs, reservoir, sparse)
    network.addNetworkModel(leftInputsToRes)
    val rightInputsToRes = SynapseGroup2(rightInputs, reservoir, sparse)
    network.addNetworkModel(rightInputsToRes)

    // Effectors
    val leftTurnNeuron = Neuron(network)
    val rightTurnNeuron = Neuron(network)
    network.addNetworkModel(leftTurnNeuron)
    network.addNetworkModel(rightTurnNeuron)
    leftTurnNeuron.upperBound = 100.0
    rightTurnNeuron.upperBound = 100.0
    val leftTurn = NeuronCollection(network, listOf(leftTurnNeuron))
    leftTurn.label = "Left Turn"
    network.addNetworkModel(leftTurn)
    val rightTurn = NeuronCollection(network, listOf(rightTurnNeuron))
    rightTurn.label = "Right Turn"
    network.addNetworkModel(rightTurn)
    leftTurnNeuron.location = point(546, -203)
    rightTurnNeuron.location = point(573, 323)
    val resToLeftTurn = SynapseGroup2(reservoir, leftTurn, sparse)
    network.addNetworkModel(resToLeftTurn)
    val resToRightTurn = SynapseGroup2(reservoir, rightTurn, sparse)
    network.addNetworkModel(resToRightTurn)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    // network.addUpdateAction(updateAction("Allostatic Learning Rule") {
    //     println("Custom update....")
    // })

    // ODOR WORLD STUFF

    val odorWorldComponent = OdorWorldComponent("World")
    val odorWorld = odorWorldComponent.world

    // Agent
    val agent = odorWorld.addEntity(EntityType.CIRCLE).apply {
        location = point(odorWorld.width / 2.0, odorWorld.height / 2.0)
        heading = 90.0
        addDefaultEffectors()
    }

    // Effectors
    val (_, turnLeft, turnRight) = agent.effectors

    // Left sensors
    (30 - sensoryNeurons / 2..30 + sensoryNeurons / 2).forEachIndexed { counter, position ->
        val cheeseSensorLeft = ObjectSensor(agent, EntityType.SWISS)
        cheeseSensorLeft.theta = Math.toRadians(position.toDouble())
        cheeseSensorLeft.radius = EntityType.CIRCLE.imageHeight / 2.0
        cheeseSensorLeft.decayFunction.dispersion = 100.0
        with(couplingManager) {
            cheeseSensorLeft couple leftInputNeurons[counter]
        }
        // cheeseSensorLeft.decayFunction = StepDecayFunction().also {
        //     dispersion = 200.0
        // }
        agent.addSensor(cheeseSensorLeft)
    }

    (-30 - sensoryNeurons / 2..-30 + sensoryNeurons / 2).forEachIndexed { counter, position ->
        val cheeseSensorRight = ObjectSensor(agent, EntityType.SWISS)
        cheeseSensorRight.theta = Math.toRadians(position.toDouble())
        cheeseSensorRight.radius = EntityType.CIRCLE.imageHeight / 2.0
        cheeseSensorRight.decayFunction.dispersion = 100.0
        with(couplingManager) {
            cheeseSensorRight couple rightInputNeurons[counter]
        }
        agent.addSensor(cheeseSensorRight)
    }

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
        cheese.location = point(agent.x + 100 * cos(network.time), agent.y - 100 * sin(network.time))
    }
    updateCheeseLocation()
    workspace.addUpdateAction(updateAction("Move cheese") {
        updateCheeseLocation()
    })

    workspace.addWorkspaceComponent(odorWorldComponent)
    withGui {
        place(odorWorldComponent) {
            location = point(403, 14)
            height = 524
            width = 568
        }
    }

    with(couplingManager) {
        leftTurnNeuron couple turnLeft
        rightTurnNeuron couple turnRight
    }

}

class AllostaticUpdateRule: SpikingNeuronUpdateRule() {

    var target = 1.0
    var threshold = 1.0
    val leakRate = .25
    val learningRate = .01

    override fun apply(n: Neuron, data: ScalarDataHolder) {
        // TODO.  Min below is a bandaid, those values are blowing up
        val newActivation = n.activation * (1-leakRate) + min(n.weightedInputs, 100.0)
        n.activation = max(0.0, newActivation )

        if (n.activation > threshold) {
            n.isSpike = true
            println("Spike!")
            setHasSpiked(true, n)
        }

        val error = n.activation - target

        // Weights
        val toTrain= n.fanIn.filter{it.source.isSpike}
        toTrain.forEach {  s ->
            s.strength -= error/toTrain.size
        }

        target += error * learningRate
        target = max(target, 1.0)
        threshold = 2*target

        println("$target, $threshold, ${n.activation}")
    }

    override fun deepCopy(): NeuronUpdateRule {
        return AllostaticUpdateRule()
    }

    override val name = "Allostatic Update Rule"


}