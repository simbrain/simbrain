package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.AllostaticUpdateRule
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.StepDecayFunction
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.ObjectSensor
import javax.swing.JTextField
import kotlin.math.cos
import kotlin.math.sin

/**
 * Create a reservoir simulation...
 */
val objectTrackingSim = newSim("object_tracking") {

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
    exposeTypes(AllostaticUpdateRule::class)
    val networkComponent = addNetworkComponent("Spontaneous Object Tracking")
    val network = networkComponent.network

    // Most connections in network use 10% density
    val sparse = Sparse()
    sparse.connectionDensity = .1

    // Add a self-connected neuron array to the network
    val resNeurons = (0..numResNeurons).map {
        Neuron(AllostaticUpdateRule())
    }
    network.addNetworkModels(resNeurons)
    val reservoir = NeuronCollection(resNeurons)
    network.addNetworkModel(reservoir)
    reservoir.label = "Reservoir"
    reservoir.layout(GridLayout())
    reservoir.location = point(0, 0)
    val reservoirSynapseGroup = SynapseGroup(reservoir, reservoir, sparse)
    network.addNetworkModel(reservoirSynapseGroup)
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
    network.addNetworkModels(leftInputNeurons)
    val leftInputs = NeuronCollection(leftInputNeurons)
    network.addNetworkModel(leftInputs)
    leftInputs.label = "Left Inputs"
    leftInputs.layout(GridLayout())
    leftInputs.location = point(-816, -250)

    // Right inputs
    val rightInputNeurons = (0 until sensoryNeurons).map {
        val rule = LinearRule()
        val neuron = Neuron(rule)
        neuron
    }
    network.addNetworkModels(rightInputNeurons)
    val rightInputs = NeuronCollection(rightInputNeurons)
    network.addNetworkModel(rightInputs)
    rightInputs.label = "Right Inputs"
    rightInputs.layout(GridLayout())
    rightInputs.location = point(-816, 280)

    // Connect input nodes to reservoir
    val leftInputsToRes = SynapseGroup(leftInputs, reservoir, sparse)
    network.addNetworkModel(leftInputsToRes)
    leftInputsToRes.synapses.forEach { s ->
        s.strength = 0.75
    }
    val rightInputsToRes = SynapseGroup(rightInputs, reservoir, sparse)
    network.addNetworkModel(rightInputsToRes)
    rightInputsToRes.synapses.forEach { s ->
        s.strength = 0.75
    }

    // Output neurons
    val leftTurnNeuron = Neuron(PercentIncomingNeuronRule())
    val rightTurnNeuron = Neuron(PercentIncomingNeuronRule())
    network.addNetworkModel(leftTurnNeuron)
    network.addNetworkModel(rightTurnNeuron)
    leftTurnNeuron.upperBound = 100.0
    rightTurnNeuron.upperBound = 100.0
    val leftTurnCollection = NeuronCollection(listOf(leftTurnNeuron))
    leftTurnCollection.label = "Left Turn"
    network.addNetworkModel(leftTurnCollection)
    val rightTurnCollection = NeuronCollection(listOf(rightTurnNeuron))
    rightTurnCollection.label = "Right Turn"
    network.addNetworkModel(rightTurnCollection)
    leftTurnNeuron.location = point(700, -300)
    rightTurnNeuron.location = point(700, 350)
    val resToLeftTurn = SynapseGroup(reservoir, leftTurnCollection, sparse)
    network.addNetworkModel(resToLeftTurn)
    resToLeftTurn.displaySynapses = false
    val resToRightTurn = SynapseGroup(reservoir, rightTurnCollection, sparse)
    network.addNetworkModel(resToRightTurn)
    resToRightTurn.displaySynapses = false

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

    // Couple output neurons to effectors
    with(couplingManager) {
        leftTurnNeuron couple turnLeftEffector
        rightTurnNeuron couple turnRightEffector
    }

    withGui {
        val controlPanel = createControlPanel("Control Panel", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
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
        }.awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 600, 600)
        place(odorWorldComponent, controlPanel.rightEdgeWithGap() + 600 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 600, 600)
    }

    addSidebarInfo(
        """
        # Object Tracking Reservoir
        
        This is one of the models presented in the paper, *A potential mechanism for Gibsonian resonance: behavioral entrainment emerges from local homeostasis 
        in an unsupervised reservoir network*. This simulation simulates an agent learning how to visually track an object using a reservoir network. 
        
        # Simulation Details
        
        The reservoir network uses an unsupervised allostatic learning rule that Ben Falandays and his colleagues developed (Falandays et al., 2021). 
        The unsupervised allostatic learning rule allows the reservoir network to adapt and adjust its weights in response to the movement of the cheese (i.e., the activation values received 
        from sensing the cheese). Through this process, the agent learns to accurately anticipate the cheese's movement in order to stabilize its sensory neuron inputs (i.e., left and right).
        In addition, the agent's learned representations of the cheese's movement also stabilize. When the agent's representations become stable, it stops developing new representations.
        But sometimes, the agent can break out of this stable state as a result of the object moving in the opposite direction. When this occurs, the agent generates new, unique representations
        of the object's movement on the fly for a given rotational direction, never reusing its pre-existing representations, creating no stable permanent representations. 
        
        This transition from stabilization to destabilization is considered as a representational drift (Rule et al., 2019). This cycle of stabilization to destabilization
        creates an illustration of how an agent can behave consistently to its learned information and how its behavior can adapt in response to new ongoing changes in neural activity
        (i.e., new incoming information).
        
        Note that the agent's ability to track the cheese is not pre-built in the agent (i.e., it is not being told to track the object). Instead, it emerges from the dynamics within the reservoir network.
        
        # What to Do
        
        In this simulation, the only configurations are `Number of iterations` and `Record spikes`. Before explaining the utilization of these configurations, to explore this simulation:
        
        1) Run the simulation.
        
        2) While it runs, look at the reservoir network. To observe its state: right-click the `Reservoir` group tag → `plot` → `projection plot`. 
        
            - In this projection plot, each point represents a unique reservoir state; as the object moves in one direction, its representations stabilize, where the activation will alternate
            between the pre-existing states. Once the object changes directions, the reservoir's representations destabilize and it re-generates new states for the new pattern of movement.
            This transition illustrates representational drift.
         
        3) Observe the agent's behavior in correspondence to the reservoir network's representations in the PCA plot.
        
        ## Conducting Research With The Object Tracking Simulation
         
        Using the configurations, you can utilize this simulation to conduct research. The `Number of iterations` is the number of timesteps before the reservoir network's activation 
        is saved into an Excel spreadsheet. If you also want to record the activation spikes that occur in the reservoir network, check the `Record spikes` box. After saving the data,
        you can analyze the data. For example, analyzing when representational drift happens and how often it occurs. When doing this, you can also analyze how changes in the [simulation 
        code](https://docs.simbrain.net/docs/simulations/) can affect the agent's behaviors like changing the learning rate or the synaptic weight values.
        
        # References
        
        Falandays, J. B., Yoshimi, J., Warren, W., & Spivey, M. J. (2023). [_A potential mechanism for Gibsonian resonance: behavioral entrainment emerges from local homeostasis in an unsupervised reservoir network_](https://doi.org/10.1007/s11571-023-09988-2). ([Preprint](https://doi.org/10.31234/osf.io/pt7bn)). _Cognitive Neurodynamics_, _18_(4), 1811–1834.
                
        Falandays, J. B., Nguyen, B., & Spivey, M. J. (2021). [_Is prediction nothing more than multi-scale pattern completion of the future?_](https://doi.org/10.1016/j.brainres.2021.147578) _Brain Research_, _1768_, 147578.
         
        Rule, M. E., O'Leary, T., & Harvey, C. D. (2019). [_Causes and consequences of representational drift_](https://doi.org/10.1016/j.conb.2019.08.005). _Current opinion in neurobiology_, _58_, 141–147.
        
        # Credits
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        Kanly Thao
        
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
