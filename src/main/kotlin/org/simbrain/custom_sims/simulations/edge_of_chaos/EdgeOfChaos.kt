package org.simbrain.custom_sims.simulations.edge_of_chaos

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.Direction
import org.simbrain.network.connections.FixedDegree
import org.simbrain.network.core.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.BinaryRule
import org.simbrain.util.decayfunctions.StepDecayFunction
import org.simbrain.util.place
import org.simbrain.util.projection.DecayColoringManager
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.awt.geom.Point2D
import kotlin.math.sqrt

/**
 * Demonstration of representational capacities of recurrent networks based on
 * Bertschinger, Nils, and Thomas Natschläger. "Real-time computation at the
 * edge of chaos in recurrent neural networks." Neural computation 16.7 (2004):
 * 1413-1436.
 */
val edgeOfChaos = newSim("edgeOfChaos") {
    // Simulation Parameters
    var NUM_NEURONS: Int = 120

    //  For 120 neurons: .01,.1, and > .4
    var variance = 1.4
    val K = 4 // in-degree (num connections to each neuron)

    val seed = 42L

    // Clear workspace
    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Edge of Chaos")
    val net = networkComponent.network

    net.timeStep = 0.5

    // Make reservoir
    val reservoir = createReservoir(net, 10, 10, NUM_NEURONS).apply {
        label = "Reservoir"
    }

    // Connect reservoir
    val sgReservoir = connectReservoir(net, reservoir, variance, K, seed)

    // Set up sensor nodes
    val sensorNodes = net.addNeuronCollection(6).apply {
        label = "Sensors"
        setLocation(229.0, 561.0)
        applyLayout()
    }

    // Make custom connections from sensor nodes to upper-left and
    // lower-right quadrants of the reservoir network to ensure visually
    // distinct patterns.
    createSensorConnections(net, sensorNodes, reservoir, intArrayOf(0, 1, 2), .6, 1)
    createSensorConnections(net, sensorNodes, reservoir, intArrayOf(3, 4, 5), .6, 3)

    // Projection plot
    val pc = addProjectionPlot("PCA")
    pc.projector.coloringManager = DecayColoringManager()
    couplingManager.createCoupling(reservoir, pc)

    // Odor world sim
    val oc = addOdorWorldComponent("Two objects")
    oc.world.isObjectsBlockMovement = false
    oc.world.isUseCameraCentering = false
    val mouse = oc.world.addEntity(200, 110, EntityType.Mouse)
    mouse.heading = 90.0

    // Set up world
    val dispersion = 65.0
    val cheese1: OdorWorldEntity =
        oc.world.addEntity(50, 110, EntityType.Swiss, doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val cheese2: OdorWorldEntity =
        oc.world.addEntity(110, 40, EntityType.Gouda, doubleArrayOf(0.0, 1.0, 0.0, 0.0, 0.0, 0.0))
    val cheese3: OdorWorldEntity =
        oc.world.addEntity(110, 180, EntityType.BlueCheese, doubleArrayOf(0.0, 0.0, 1.0, 0.0, 0.0, 0.0))
    val flower1: OdorWorldEntity =
        oc.world.addEntity(350, 110, EntityType.Pansy, doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 1.0))
    val flower2: OdorWorldEntity =
        oc.world.addEntity(290, 40, EntityType.Flax, doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.0, 0.0))
    val flower3: OdorWorldEntity =
        oc.world.addEntity(290, 180, EntityType.Tulip, doubleArrayOf(0.0, 0.0, 0.0, 1.0, 0.0, 0.0))

    cheese1.smellSource.dispersion = dispersion
    cheese2.smellSource.dispersion = dispersion
    cheese3.smellSource.dispersion = dispersion
    flower1.smellSource.dispersion = dispersion
    flower2.smellSource.dispersion = dispersion
    flower3.smellSource.dispersion = dispersion
    cheese1.smellSource.decayFunction = StepDecayFunction()
    cheese2.smellSource.decayFunction = StepDecayFunction()
    cheese3.smellSource.decayFunction = StepDecayFunction()
    flower1.smellSource.decayFunction = StepDecayFunction()
    flower2.smellSource.decayFunction = StepDecayFunction()
    flower3.smellSource.decayFunction = StepDecayFunction()

    // Couple agent to cheese and flower nodes
    val smellSensor = SmellSensor()
    mouse.addSensor(smellSensor)
    couplingManager.createCoupling(smellSensor, sensorNodes)

    withGui {
        place(networkComponent, 0, 0, 590, 675)
        place(oc, 590, 0, 420, 312)
        place(pc, 590, 312, 422, 363)

        // Set up control panel
        createControlPanel("Controller", 1010, 0) {
            val tf_stdev = addTextField("Weight stdev", variance.toString())
            addButton("Update") {
                // Update variance of weight strengths
                variance = tf_stdev.text.toDouble()
                val normalDist = NormalDistribution(0.0, variance)
                sgReservoir.randomize(normalDist)
            }
        }
    }

    addSidebarInfo(
        """
        # Edge of Chaos Embodied
                
        This simulation is an experimental study of how representations in a reservoir network are effected in different dynamic regimes: chaos, edge of chaos, and ordered. This simulation
        builds upon the work in the `Edge of Chaos bitstream` simulation, specifically, the work of Nils Bertschinger and Thomas Natschläger, _Real-time computation at the edge of chaos in
        recurrent neural networks_. For an in-depth description and illustration of the different dynamical regimes, refer to the `Edge of Chaos bitstream` simulation. 
        The main goal of this simulation, similar to the other `Edge Of Chaos` simulation, is to find the edge of chaos and visualize its effects on an agent's representation of an object. Please note,
        we have not finished studying this network so if you find any patterns or structure, let us know!
                
        # Simulation Details
                
        This simulation models how an agent visually represents an object that exists in an environment (i.e., in the odor world) using a reservoir network. In the odor world, there are 
        two different groups of objects: flowers and cheeses. There are three different "species" in each group that will be utilized to interact with the agent. In principle,
        the objects in each group have similar representations with one another due to similarities in their structure. The object groups (i.e., cheese or flower) are
        projected to different locations in the state space if the reservoir is not in a chaotic state. To visualize this, the agent is moved towards the objects where their states are
        represented in a PCA plot as points. The PCA plot tracks the agent's representation of the objects using a decaying color scheme. It uses the [coloring manager](https://docs.simbrain.net/docs/plots/projectionPlot.html#coloring-manager) 
        to make the states of the reservoir more red if they are closer to the real-time timestep, and they become more transparent as the simulation runs (moving away from the 
        real-time timestep).
         
        # What To Do
                
        In this simulation, the only configuration for the simulation is the `weight stdev`. To find each state, follow the steps below.
                
        1) Change the `weight stdev` value and press the `update` button to change the reservoir's responses to the object, which will be shown in the PCA plot.
                        
        2) Start the simulation by clicking on the `run` button in the top-left corner.
                
        3) Click on the `cursor` icon, drag the agent (mouse) to each object until it stabilizes.
                
        4) Observe changes in the reservoir's representation in the PCA plot.
                
            - The flowers and cheeses are all represented similarly with their corresponding group type where, their representations branch into two different state spaces in the
            PCA plot (looks like a pair of "wings"). If the weight stdev is very high (i.e., in the chaos state), the state space will become blurred with representations all over the place. If
            the weight stdev is very low (i.e., in the ordered state), the state space collapses to a few points in the state space.
                
        5) To `reset` the simulation, stop the simulation by clicking the `run` button again and press `k`. Then clear all points in the PCA plot by clicking on the `clear` button.
                
        6) Afterwards, click back on the `cursor` icon, and left-click outside of the reservoirs to unselect all neurons.
         
        ## Observing The Representations Of The Objects
                
        To observe the object's representation in the network, delete the recurrent connections (the recurrent synapses) by clicking on them and pressing backspace. Then run 
        the simulation and repeat steps 2 to 6.
                
        # References
                
        Bertschinger, N., & Natschläger, T. (2004). [Real-Time Computation at the Edge of Chaos in Recurrent Neural Networks](https://doi.org/10.1162/089976604323057443). _Neural Computation_, _16_(7), 1413\u20131436.
                
        # Credits
                
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        Kanly Thao

        """.trimIndent()
    )
}

/**
 * Creates connections between a specified set of source neurons and a designated quadrant
 * of the target neuron group (reservoir). The connections are created with a specified
 * sparsity level, and only neurons within the given quadrant are considered as targets.
 *
 * @param net the network to add synapses to
 * @param sourceGroup the group of source neurons to connect from
 * @param targetGroup the group of target neurons (reservoir) to connect to
 * @param sourceNodeIndices an array of indices specifying which neurons in the source group to connect
 * @param sparsity the probability of creating a connection between a source neuron and a target neuron (0.0 to 1.0)
 * @param quadrant the target quadrant of the reservoir (1: upper-left, 2: upper-right, 3: lower-right, 4: lower-left)
 */
suspend fun createSensorConnections(
    net: Network,
    sourceGroup: NeuronCollection,
    targetGroup: NeuronCollection,
    sourceNodeIndices: IntArray,
    sparsity: Double,
    quadrant: Int
) {
    // Define quadrant boundaries
    val xStart: Double
    val xEnd: Double
    val yStart: Double
    val yEnd: Double

    if (quadrant < 3) { // Top quadrants
        yStart = targetGroup.minY
        yEnd = targetGroup.centerY
    } else { // Bottom quadrants
        yStart = targetGroup.centerY
        yEnd = targetGroup.maxY
    }

    if (quadrant == 1 || quadrant == 4) { // Left quadrants
        xStart = targetGroup.minX
        xEnd = targetGroup.centerX
    } else { // Right quadrants
        xStart = targetGroup.centerX
        xEnd = targetGroup.maxX
    }

    // Create synapses list first based on custom quadrant logic
    val synapses: MutableList<Synapse> = ArrayList()

    // Iterate over neurons in the target group
    val targetNeurons: MutableList<Neuron> = targetGroup.neuronList
    for (targetNeuron in targetNeurons) {
        val x = targetNeuron.x
        val y = targetNeuron.y

        // Check if the neuron lies within the specified quadrant
        if (x >= xStart && x < xEnd && y >= yStart && y < yEnd) {
            // Connect the source neurons to this target neuron
            for (sourceIndex in sourceNodeIndices) {
                if (Math.random() < sparsity) {
                    val sourceNeuron = sourceGroup.neuronList[sourceIndex]
                    val synapse = Synapse(sourceNeuron, targetNeuron)
                    synapse.strength = 1.0 // Default strength
                    synapses.add(synapse)
                }
            }
        }
    }
    net.addNetworkModels(synapses)
}

const val GRID_SPACE: Int = 25

suspend fun createReservoir(parentNet: Network, x: Int, y: Int, numNeurons: Int): NeuronCollection {
    val layout = GridLayout(GRID_SPACE.toDouble(), GRID_SPACE.toDouble(), sqrt(numNeurons.toDouble()).toInt())
    val thresholdUnit = BinaryRule()
    val nc = parentNet.addNeuronCollection(numNeurons) { updateRule = thresholdUnit.copy() }

    nc.layout = layout
    nc.applyLayout(Point2D.Double(x.toDouble(), y.toDouble()))
    return nc
}


suspend fun connectReservoir(parentNet: Network, res: NeuronCollection, variance: Double, k: Int, seed: Long): SynapseGroup {
    val con = FixedDegree(k, Direction.IN, false, 20.0, false, seed)

    val reservoir = SynapseGroup(res, res, con)
    reservoir.label = "Recurrent synapses"
    parentNet.addNetworkModel(reservoir)

    return reservoir
}