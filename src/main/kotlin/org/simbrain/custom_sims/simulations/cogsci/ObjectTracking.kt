package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.*
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.AllostaticUpdateRule
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.StepDecayFunction
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.workspace.updater.updateAction
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.lang.Double.max
import javax.swing.JTextField
import kotlin.math.cos
import kotlin.math.sin

/**
 * Create a reservoir simulation...
 */
val objectTrackingSim = newSim {

    // Number of reservoir neurons
    val numResNeurons = 200
    // Number of left and right sensory neurons. Total sensory neurons is twice this.
    val sensoryNeurons = 31
    // Radius in pixels of the cheese's revolution around the agent.
    val radiusOfRevolution = 100.0
    // Varaables to make cheese change direction once in a while
    var counter = 0
    var direction = 1 // 1 for counterclockwise -1 for clockwise
    var isRecording = false
    var recordSpikes = false

    // Record direction variables followed by reservoir activations
    val data = mutableListOf<List<Double>>()

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Spontaneous Object Tracking")
    val network = networkComponent.network

    // Most connections in network use 10% density
    val sparse = Sparse()
    sparse.connectionDensity = .1

    // Add a self-connected neuron array to the network
    val resNeurons = (0..numResNeurons).map {
        Neuron(AllostaticUpdateRule())
    }
    network.addNetworkModels(resNeurons).awaitAll()
    val reservoir = NeuronCollection(resNeurons)
    network.addNetworkModel(reservoir)?.await()
    reservoir.label = "Reservoir"
    reservoir.layout(GridLayout())
    reservoir.location = point(0, 0)
    val reservoirSynapseGroup = SynapseGroup(reservoir, reservoir, sparse)
    network.addNetworkModel(reservoirSynapseGroup)?.await()
    val dist = NormalDistribution(1.0, .1)
    reservoirSynapseGroup.synapses.forEach { s ->
        s.strength = dist.sampleDouble()
    }

    // Left inputs
    val leftInputNeurons = (0 until sensoryNeurons).map {
        val rule = LinearRule()
        val neuron = Neuron(rule)
        neuron
    }
    network.addNetworkModels(leftInputNeurons).awaitAll()
    val leftInputs = NeuronCollection(leftInputNeurons)
    network.addNetworkModel(leftInputs)?.await()
    leftInputs.label = "Left Inputs"
    leftInputs.layout(GridLayout())
    leftInputs.location = point(-616, -195)

    // Right inputs
    val rightInputNeurons = (0 until sensoryNeurons).map {
        val rule = LinearRule()
        val neuron = Neuron(rule)
        neuron
    }
    network.addNetworkModels(rightInputNeurons).awaitAll()
    val rightInputs = NeuronCollection(rightInputNeurons)
    network.addNetworkModel(rightInputs)?.await()
    rightInputs.label = "Right Inputs"
    rightInputs.layout(GridLayout())
    rightInputs.location = point(-616, 225)

    // Connect input nodes to reservoir
    val leftInputsToRes = SynapseGroup(leftInputs, reservoir, sparse)
    network.addNetworkModel(leftInputsToRes)?.await()
    leftInputsToRes.synapses.forEach { s ->
        s.strength = 0.75
    }
    val rightInputsToRes = SynapseGroup(rightInputs, reservoir, sparse)
    network.addNetworkModel(rightInputsToRes)?.await()
    rightInputsToRes.synapses.forEach { s ->
        s.strength = 0.75
    }

    // Output neurons
    val leftTurnNeuron = Neuron(PercentIncomingNeuronRule())
    val rightTurnNeuron = Neuron(PercentIncomingNeuronRule())
    network.addNetworkModel(leftTurnNeuron)?.await()
    network.addNetworkModel(rightTurnNeuron)?.await()
    leftTurnNeuron.upperBound = 100.0
    rightTurnNeuron.upperBound = 100.0
    val leftTurnCollection = NeuronCollection(listOf(leftTurnNeuron))
    leftTurnCollection.label = "Left Turn"
    network.addNetworkModel(leftTurnCollection)
    val rightTurnCollection = NeuronCollection(listOf(rightTurnNeuron))
    rightTurnCollection.label = "Right Turn"
    network.addNetworkModel(rightTurnCollection)
    leftTurnNeuron.location = point(546, -203)
    rightTurnNeuron.location = point(573, 323)
    val resToLeftTurn = SynapseGroup(reservoir, leftTurnCollection, sparse)
    network.addNetworkModel(resToLeftTurn)?.await()
    val resToRightTurn = SynapseGroup(reservoir, rightTurnCollection, sparse)
    network.addNetworkModel(resToRightTurn)?.await()

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(183, 0)
            width = 600
            height = 600
        }
    }

    network.addUpdateAction(updateAction("Record activations") {
        if (isRecording) {
            if(recordSpikes) {
                with(network) {
                    data.add(listOf(direction.toDouble()) + resNeurons.map { n -> if (n.isSpike) 1.0 else 0.0 })
                }
            } else {
                data.add(listOf(direction.toDouble()) + resNeurons.map { n -> n.activation })
            }
        }
    })

    // network.addUpdateAction(updateAction("Allostatic Learning Rule") {
    //     println("Custom update....")
    // })

    // ODOR WORLD STUFF

    val odorWorldComponent = OdorWorldComponent("World")
    val odorWorld = odorWorldComponent.world
    odorWorld.isObjectsBlockMovement = false

    // Agent
    val agent = odorWorld.addEntity(EntityType.Circle).apply {
        location = point(odorWorld.width / 2.0, odorWorld.height / 2.0)
        heading = 90.0
        addDefaultEffectors()
    }

    // Effectors
    val (_, turnLeftEffector, turnRightEffector) = agent.effectors

    val fudge = 36.0 // to get the sensor range right

    val leftSensors = linspace(-31, 89, 31)
    // Left sensors (30 - sensoryNeurons / 2 until 30 + sensoryNeurons / 2).forEachIndexed { counter, position ->//
    leftSensors.forEachIndexed { counter, position ->
        val cheeseSensorLeft = ObjectSensor(EntityType.Swiss)
        cheeseSensorLeft.theta = position.toDouble()
        cheeseSensorLeft.radius = EntityType.Circle.height / 2.0
        cheeseSensorLeft.decayFunction = StepDecayFunction()
        cheeseSensorLeft.decayFunction.dispersion = radiusOfRevolution - fudge
        with(couplingManager) {
            cheeseSensorLeft couple leftInputNeurons[counter]
        }
        agent.addSensor(cheeseSensorLeft)
    }

    // Right sensors (-30 - sensoryNeurons / 2 until -30 + sensoryNeurons / 2).forEachIndexed { counter, position ->//
    val rightSensors = linspace(-90, 30, 31)
    rightSensors.forEachIndexed { counter, position ->
        val cheeseSensorRight = ObjectSensor(EntityType.Swiss)
        cheeseSensorRight.theta = position.toDouble()
        cheeseSensorRight.radius = EntityType.Circle.height / 2.0
        cheeseSensorRight.decayFunction = StepDecayFunction()
        cheeseSensorRight.decayFunction.dispersion = radiusOfRevolution - fudge
        with(couplingManager) {
            cheeseSensorRight couple rightInputNeurons[counter]
        }
        agent.addSensor(cheeseSensorRight)
    }

    // Objects
    val cheese = odorWorld.addEntity(EntityType.Swiss).apply {
        val (x, y) = point(200.0, 250.0)
        setLocation(x, y)
        smellSource = SmellSource(doubleArrayOf(1.0, .2, .5, .1, 1.0)).apply {
            this.dispersion = 200.0
        }
    }

    fun updateCheeseLocation() {
        val (agentx, agenty) = agent.location

        // Change direction every 2 rotations
        counter += 1
        if (counter % 720 == 0) {
            direction *= -1
        }

        cheese.location = point(
            agentx + radiusOfRevolution * cos((direction * counter).toRadian()),
            agenty + radiusOfRevolution * sin((direction * counter).toRadian())
        )
    }

    updateCheeseLocation()
    workspace.addUpdateAction(updateAction("Move cheese") {
        // println(reservoir.activations.mean)
        updateCheeseLocation()
    })

    workspace.addWorkspaceComponent(odorWorldComponent)
    withGui {
        place(odorWorldComponent) {
            location = point(783, 0)
            width = 600
            height = 600
        }
    }

    // Couple output neurons to effectors
    with(couplingManager) {
        leftTurnNeuron couple turnLeftEffector
        rightTurnNeuron couple turnRightEffector
    }

    withGui {
        createControlPanel("Control Panel", 0, 0) {
            val tfNumIterations: JTextField = addTextField("Number of iterations", "1000")
            val cbRecordSpikes = addCheckBox("Record spikes", recordSpikes)
            addButton("Run trial") {
                isRecording = true
                recordSpikes = cbRecordSpikes.isSelected
                workspace.simpleIterate(tfNumIterations.text.toInt())
                isRecording = false
                showSaveDialog("", "reservoirdata.csv") {
                    writeText(data.toCsvString())
                }
            }
        }
    }

    addSidebarInfo(
        """
        # Introduction
        
        This simulation, in principle, builds upon the theoretical foundations that are mentioned in the `Edge of Chaos bitstream` simulation. 
        
        This is a simulation of an agent learning how to track an object using a reservoir network. The simulation consists of two sensory neuron groups, two effector 
        nodes (e.g., left or, right turn), a reservoir network, and an odor world with an object (e.g., cheese) and the agent in it. The input activations are outputted into 
        the reservoir network where the reservoir network processes the pattern and learns how to track the object. 
        
        ## Simulation Details
        
        The reservoir network uses an [**unsupervised allostatic learning rule**](https://sciencedirect.com/science/article/pii/S0006899321004352?via%3Dihub) developed by Ben 
        Falandays. The unsupervised allostatic learning rule allows the reservoir network to adapt and adjust its weights accordingly to the movement of cheese (i.e., 
        the activation values received from sensing the cheese). Through this process, the network learns to anticipate the cheese's movement correctly in order to stabilize 
        the network's input arrays. Through this stabilization phase, the network activity will also be stabilized. Sometimes, the agent can break out of this stable state
        as a result of the object changing directions. And this process repeats. 
        
        The agent's ability to track the cheese is not pre-built in the agent (i.e., it is not being told to track the object), rather, this ability emerges from the dynamics 
        within the network. When the agent tracks the object, the entire agent-environment system falls into stable, attracting states that corresponds to states where the 
        object stays in front of the object.
        
        ## Connection with Representational Drift
        
        When the network activity becomes stable, it stops developing new activation patterns. Sometimes, the agent breaks out of its stable state as a result of the object
        moving into a opposite direction. When this occurs, the agent will not utilize its pre-existing representation of the object's movement. It instead develops new 
        representations (i.e., patterns) of the object's movement. The developed representation is always new, so there is no stable permanent representation. It is always 
        coming up with new representation on the fly for a given rotational direction. This shift in network activity is [**representational drift**](https://pmc.ncbi.nlm.nih.gov/articles/PMC7385530/).
        
        # What to Do
        
        In this simulation, the only configurations are `Number of iterations` and `Record spikes`. Before explaining the utilization of the configurations, this is the general
        method that you can utilize this simulation:
        
        1) Run the simulation.
        
        2) While it runs, look at the reservoir network. To observe its network activity (i.e., state): right-click the reservoir network, click on `plot`, and click on `projection 
        plot`. 
        
        3) Observe the agent's behavior in correspondence to the reservoir network's activity.
        
        ## Conducting Research With The Object Tracking Simulation
         
        The other method to utilize this simulation is using it to conduct research. The `Number of iterations` is the number of timesteps before the reservoir network's activation 
        is saved into an Excel spreadsheet. If you also want to record the activation spikes that occurs in the reservoir network, check the `Record spikes` box. After saving the data,
        you can analyze the data. For example, analyzing when representational drift happens and when it happens. When doing this, you can also analyze how changes in the [simulation code](https://docs.simbrain.net/docs/simulations/)
        can affect the agent's learning performance like changing the learning rate or, the synaptic weight values.
        
        """.trimIndent()
    )
}

/**
 * Activation set = to number of positive inputs / total number of inputs.
 */
class PercentIncomingNeuronRule : LinearRule() {
    val maxVal = 10.0
    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        neuron.activation = maxVal * neuron.fanIn.count { it.source.isSpike }
            .toDouble() / neuron.fanIn.size
    }
}

/**
 * See equation (1) in Falandays et. al. 2021
 */
context(Network)
fun Neuron.getAllostaticInput(): Double {
    // Treat linear inputs as sensors and do normal connectionist updating
    val sensorInputs = fanIn.filter { it.source.updateRule is LinearRule }.sumOf { it.source.activation * it.strength }
    // For spiking inputs sum weight strengths for pre-synaptic nodes that fired
    val weightsOfSpikingNodes = fanIn.filter { it.source.isSpike }.sumOf { it.strength }
    return sensorInputs + weightsOfSpikingNodes
}

class AllostaticDataHolder(
    target: Double = 1.0,

    @UserParameter(label = "threshold", minimumValue = 2.0)
    var threshold: Double = 2.0

) : SpikingScalarData() {

    @UserParameter(label = "target", minimumValue = 1.0)
    var target = target

    override fun copy(): SpikingScalarData {
        return AllostaticDataHolder(target, threshold)
    }
}
