package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.updateAction
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.*
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.StepDecayFunction
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

    // Number of reservoir neurons
    val numResNeurons = 200
    // Number of left and right sensory neurons. Total sensory neurons is twice this.
    val sensoryNeurons = 25
    // Radius in pixels of the cheese's revolution around the agent.
    val radiusOfRevolution = 100.0

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
        val neuron = AllostaticNeuron(network, rule)
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

    // Output neurons
    val leftTurnNeuron = Neuron(network, PercentIncomingNeuronRule())
    val rightTurnNeuron = Neuron(network, PercentIncomingNeuronRule())
    network.addNetworkModel(leftTurnNeuron)
    network.addNetworkModel(rightTurnNeuron)
    leftTurnNeuron.upperBound = 100.0
    rightTurnNeuron.upperBound = 100.0
    val leftTurnCollection = NeuronCollection(network, listOf(leftTurnNeuron))
    leftTurnCollection.label = "Left Turn"
    network.addNetworkModel(leftTurnCollection)
    val rightTurnCollection = NeuronCollection(network, listOf(rightTurnNeuron))
    rightTurnCollection.label = "Right Turn"
    network.addNetworkModel(rightTurnCollection)
    leftTurnNeuron.location = point(546, -203)
    rightTurnNeuron.location = point(573, 323)
    val resToLeftTurn = SynapseGroup2(reservoir, leftTurnCollection, sparse)
    network.addNetworkModel(resToLeftTurn)
    val resToRightTurn = SynapseGroup2(reservoir, rightTurnCollection, sparse)
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
    odorWorld.isObjectsBlockMovement = false

    // Agent
    val agent = odorWorld.addEntity(EntityType.CIRCLE).apply {
        location = point(odorWorld.width / 2.0, odorWorld.height / 2.0)
        heading = 90.0
        addDefaultEffectors()
    }

    // Effectors
    val (_, turnLeftEffector, turnRightEffector) = agent.effectors

    val fudge = 35.0 // to get the sensor range right

    // Left sensors
    (30 - sensoryNeurons / 2 until 30 + sensoryNeurons / 2).forEachIndexed { counter, position ->
        val cheeseSensorLeft = ObjectSensor(EntityType.SWISS)
        cheeseSensorLeft.theta = position.toDouble()
        cheeseSensorLeft.radius = EntityType.CIRCLE.imageHeight / 2.0
        cheeseSensorLeft.decayFunction = StepDecayFunction()
        cheeseSensorLeft.decayFunction.dispersion = radiusOfRevolution - fudge
        with(couplingManager) {
            cheeseSensorLeft couple leftInputNeurons[counter]
        }
        agent.addSensor(cheeseSensorLeft)
    }

    (-30 - sensoryNeurons / 2 until -30 + sensoryNeurons / 2).forEachIndexed { counter, position ->
        val cheeseSensorRight = ObjectSensor(EntityType.SWISS)
        cheeseSensorRight.theta = position.toDouble()
        cheeseSensorRight.radius = EntityType.CIRCLE.imageHeight / 2.0
        cheeseSensorRight.decayFunction = StepDecayFunction()
        cheeseSensorRight.decayFunction.dispersion = radiusOfRevolution - fudge
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
        val (x,y) = agent.location
        cheese.location = point(
            x + radiusOfRevolution * cos(network.time),
            y - radiusOfRevolution * sin(network.time)
        )
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

    // Couple output neurons to effectors
    with(couplingManager) {
        leftTurnNeuron couple turnLeftEffector
        rightTurnNeuron couple turnRightEffector
    }

}

/**
 * Activation set = to number of positive inputs / total number of inputs.
 */
class PercentIncomingNeuronRule: LinearRule() {
    val maxVal = 10.0
    override fun apply(n: Neuron, data: ScalarDataHolder) {
        n.activation = maxVal * n.fanIn
            .filter { it.source.activation > 0 }
            .count()
            .toDouble() / n.fanIn.size
    }
}

class AllostaticNeuron(parent: Network, rule: NeuronUpdateRule) : Neuron(parent, rule) {
    var target = 1.0
    var threshold = 2.0
    var applyLearning = false
}

class AllostaticUpdateRule: SpikingNeuronUpdateRule() {

    val leakRate = .25
    val learningRate = .01

    override fun apply(n: Neuron, data: ScalarDataHolder) {

        n as AllostaticNeuron

        val newActivation = n.activation * (1-leakRate) + min(n.weightedInputs, 100.0)
        n.activation = max(0.0, newActivation ) // Prevent from going below 0

        // Only apply learning if neuron has just spiked
        n.applyLearning = n.isSpike
        n.isSpike = false

        if (n.activation > n.threshold) {
            n.isSpike = true
            // println("Spike!")
            n.activation -= n.threshold
        }

        val error = n.activation - n.target

        // Weights
        val toTrain= n.fanIn
            .filter { it.source is AllostaticNeuron}
            .filter { (it.source as AllostaticNeuron).applyLearning }

        toTrain.forEach {  s ->
            if (toTrain.size > 0) {
                s.strength -= error/toTrain.size
            }
        }

        n.target += error * learningRate
        n.target = max(n.target, 1.0)
        n.threshold = 2*n.target

        println("target = ${n.target}, threshold = ${n.threshold}, activation = ${n.activation}")
    }

    override fun deepCopy(): NeuronUpdateRule {
        return AllostaticUpdateRule()
    }

    override val name = "Allostatic Update Rule"

}