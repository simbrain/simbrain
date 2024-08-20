package org.simbrain.custom_sims.simulations.patterns_of_activity

import org.simbrain.custom_sims.Simulation
import org.simbrain.network.NetworkComponent
import org.simbrain.network.connections.*
import org.simbrain.network.core.Network
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.addNeuronGroup
import org.simbrain.network.core.connect
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.updaterules.BinaryRule
import org.simbrain.network.updaterules.DecayRule
import org.simbrain.network.updaterules.KuramotoRule
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.util.SimbrainConstants
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor

/**
 * Simulate a set of oscillatory brain networks and display their projected
 * activity when exposed to inputs in a simple 2d world.
 */
class ModularOscillatoryNetwork : Simulation {
    // References
    lateinit var nc: NetworkComponent
    lateinit var net: Network
    lateinit var sensory: NeuronGroup
    lateinit var motor: NeuronGroup
    lateinit var inputGroup: NeuronGroup
    lateinit var mouse: OdorWorldEntity
    var worldEntities: MutableList<OdorWorldEntity> = ArrayList()

    private val dispersion = 140

    override fun run() {
        // Clear workspace

        sim.workspace.clearWorkspace()

        // Set up world
        setUpWorld()

        // Set up network
        setUpNetwork()

        // Set up separate projections for each module
        addProjection(inputGroup, 8, 304, .01)
        addProjection(sensory, 359, 304, .1)
        addProjection(motor, 706, 304, .5)

        // Set up workspace updating
        // sim.getWorkspace().addUpdateAction((new ColorPlotKuramoto(this)));
    }

    private fun setUpNetwork() {
        // Set up network

        nc = sim.addNetwork(
            9, 8, 581, 297,
            "Patterns of Activity"
        )
        net = nc.network

        // Sensory network
        sensory = addModule(-115, 10, 49, "Sensory", DecayRule())
        val recSensory = connectRadialGaussian(sensory, sensory)
        recSensory.label = "Recurrent Sensory"

        // Motor Network
        motor = addModule(322, 10, 16, "Motor", KuramotoRule())
        val recMotor = connectRadialGaussian(motor, motor)
        recMotor.label = "Recurrent Motor"

        // Sensori-Motor Connection
        connectModules(sensory, motor, .3, .5)

        // Input Network
        with(net) {
            inputGroup = addInputGroup(-385, 107)
        }
    }

    context(Network)
    private fun addInputGroup(x: Int, y: Int): NeuronGroup {
        // Alternate form would be based on vectors

        val ng = net.addNeuronGroup(x.toDouble(), y.toDouble(), mouse.sensors.size)
        ng.layout = LineLayout(LineLayout.LineOrientation.VERTICAL)
        ng.applyLayout(-5, -85)
        ng.label = "Object Sensors"
        var i = 0
        for (sensor in mouse.sensors) {
            val neuron = ng.getNeuron(i++)
            neuron.label = sensor.label
            neuron.clamped = true
            sim.couple(sensor, neuron)
        }

        // Hard coded for two input neurons
        val neuron1 = ng.getNeuron(0)
        val neuron2 = ng.getNeuron(1)

        // Make spatial connections to sensory group
        val yEdge = sensory.centerY
        for (j in sensory.neuronList.indices) {
            val tarNeuron = sensory.neuronList[j]
            val yloc = tarNeuron.y
            if (yloc < yEdge) {
                connect(neuron1, tarNeuron, 1.0)
            } else {
                connect(neuron2, tarNeuron, 1.0)
            }
        }

        return ng
    }

    private fun addBinaryModule(x: Int, y: Int, numNeurons: Int, name: String): NeuronGroup {
        val ng = net.addNeuronGroup(x.toDouble(), y.toDouble(), numNeurons)
        val rule = BinaryRule()
        ng.setUpdateRule(rule)
        HexagonalGridLayout.layoutNeurons(ng.neuronList, 40, 40)
        ng.setLocation(x.toDouble(), y.toDouble())
        ng.label = name
        return ng
    }

    private fun addModule(x: Int, y: Int, numNeurons: Int, name: String, rule: NeuronUpdateRule<*, *>): NeuronGroup {
        val ng = net.addNeuronGroup(x.toDouble(), y.toDouble(), numNeurons)
        // KuramotoRule rule = new KuramotoRule();
        // NakaRushtonRule rule = new NakaRushtonRule();
        // rule.setNaturalFrequency(.1);
        ng.setUpdateRule(rule)
        for (neuron in ng.neuronList) {
            if (Math.random() < .5) {
                neuron.polarity = SimbrainConstants.Polarity.EXCITATORY
            } else {
                neuron.polarity = SimbrainConstants.Polarity.INHIBITORY
            }
        }
        HexagonalGridLayout.layoutNeurons(ng.neuronList, 40, 40)
        ng.setLocation(x.toDouble(), y.toDouble())
        ng.label = name
        return ng
    }

    private fun connectRadialGaussian(sourceNg: NeuronGroup?, targetNg: NeuronGroup?): SynapseGroup {
        val radialConnection: ConnectionStrategy = RadialGaussian(
            DEFAULT_EE_CONST * 1, DEFAULT_EI_CONST * 2,
            DEFAULT_IE_CONST * 3, DEFAULT_II_CONST * 0, .25, 50.0
        )
        val sg = SynapseGroup(sourceNg!!, targetNg!!, radialConnection)
        net.addNetworkModel(sg)
        sg.displaySynapses = false
        return sg
    }

    private fun connectModules(
        sourceNg: NeuronGroup?,
        targetNg: NeuronGroup?,
        density: Double,
        exRatio: Double
    ): SynapseGroup {
        val sparse = Sparse(density)
        val sg = SynapseGroup(sourceNg!!, targetNg!!)
        // TODO!
        // , exRatio)
        //        sparse.connectNeurons(sg);
        net.addNetworkModel(sg)
        sg.displaySynapses = false
        return sg
    }

    private fun setUpWorld() {
        val oc = sim.addOdorWorld(590, 9, 505, 296, "World")

        // Mouse
        mouse = oc.world.addEntity(187, 113, EntityType.MOUSE)

        // Objects
        val cheese = oc.world.addEntity(315, 31, EntityType.SWISS)
        worldEntities.add(cheese)
        val flower = oc.world.addEntity(41, 31, EntityType.FLOWER)
        flower.smellSource.dispersion = dispersion.toDouble()
        worldEntities.add(flower)

        // Add sensors
        for (entity in worldEntities) {
            val sensor = ObjectSensor(entity.entityType)
            sensor.decayFunction.dispersion = dispersion.toDouble()
            mouse.addSensor(sensor)
        }
    }

    private fun addProjection(toPlot: NeuronGroup?, x: Int, y: Int, tolerance: Double) {
        // Create projection component

        val pc = sim.addProjectionPlot(x, y, 362, 320, toPlot!!.label)
        pc.projector.initProjector()
        pc.projector.tolerance = tolerance

        // plot.getProjector().useColorManager = false;

        // Coupling
        val inputProducer = sim.getProducer(toPlot, "getActivationArray")
        val plotConsumer = sim.getConsumer(pc, "addPoint")
        sim.couple(inputProducer, plotConsumer)

        // Text of nearest world object to projection plot current dot
        val currentObject = sim.getProducer(mouse, "getNearbyObjects")
        val plotText = sim.getConsumer(pc, "setLabel")
        sim.couple(currentObject, plotText)
    }

    constructor(desktop: SimbrainDesktop?) : super(desktop)

    constructor() : super()

    private val submenuName: String
        get() = "Cognitive Maps"

    override fun getName(): String {
        return "Modular Oscillatory Network"
    }

    override fun instantiate(desktop: SimbrainDesktop): ModularOscillatoryNetwork {
        return ModularOscillatoryNetwork(desktop)
    }
}