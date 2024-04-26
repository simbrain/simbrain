package org.simbrain.custom_sims.simulations.braitenberg

import org.simbrain.custom_sims.Simulation
import org.simbrain.custom_sims.helper_classes.ControlPanel
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.getModelByLabel
import org.simbrain.util.projection.MarkovColoringManager
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.updater.UpdateComponent
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.util.*

/**
 * Create a Braitenberg pursuer with a projection plot to sensor neurons and study the maps that develop within it.
 * Prediction plot gives a sense of when the model is working well.
 */
class RandomizedPursuer : Simulation {
    lateinit var nc: NetworkComponent
    lateinit var mouse: OdorWorldEntity
    lateinit var cheese: OdorWorldEntity
    lateinit var flower: OdorWorldEntity
    lateinit var fish: OdorWorldEntity
    lateinit var panel: ControlPanel
    lateinit var vehicleNetwork: NeuronCollection
    lateinit var sensorNodes: NeuronCollection
    lateinit var motorNodes: NeuronCollection

    lateinit var oc: OdorWorldComponent

    var cheeseX: Int = 200
    var cheeseY: Int = 250

    constructor() : super()

    constructor(desktop: SimbrainDesktop?) : super(desktop)

    override fun run() {
        // Clear workspace

        sim.workspace.clearWorkspace()

        // Create the odor world
        createOdorWorld()

        // Build the network
        buildNetwork()

        // TODO: Commenting this out for now. The button doesn't do much
        // Set up control panel
        // setUpControlPanel();
        mouse.setLocation(50, 50)
        mouse.heading = (-45).toDouble()

        // Set up Plots
        setUpPlots()

        // Set up custom update
        val updater = sim.workspace.updater
        updater.updateManager.addAction(UpdateComponent(oc), 0)
    }

    private fun createOdorWorld() {
        oc = sim.addOdorWorld(440, 8, 378, 297, "World")
        oc.world.isObjectsBlockMovement = false

        mouse = oc.world.addEntity(0, 0, EntityType.MOUSE)
        mouse.heading = 90.0
        mouse.setLocationRelativeToCenter(0, 70)
        mouse.addLeftRightSensors(EntityType.SWISS, 200.0)
        mouse.addDefaultEffectors()

        cheese = oc.world.addEntity(0, 0, EntityType.SWISS, doubleArrayOf(1.0, 0.0, 0.0))
        cheese.setLocationRelativeToCenter(0, -30)
        oc.world.update()
    }

    private fun buildNetwork() {
        nc = sim.addNetwork(10, 8, 447, 296, "Pursuer")
        val net = nc.network
        val pursuer = Vehicle(sim, net)
        with(net) {
            vehicleNetwork = pursuer.addPursuer(
                10, 10,
                mouse, EntityType.SWISS,
                mouse.sensors[0] as ObjectSensor,
                mouse.sensors[1] as ObjectSensor
            )
        }
        vehicleNetwork.label = "Pursuer"
        val sensor1 = net.getModelByLabel(Neuron::class.java, "Swiss (L)")
        val sensor2 = net.getModelByLabel(Neuron::class.java, "Swiss (R)")
        sensorNodes = NeuronCollection(Arrays.asList(sensor1, sensor2))
        sensorNodes.label = "Sensor Nodes"
        net.addNetworkModel(sensorNodes)
    }

    private fun setUpPlots() {
        // Projection plot

        val projComp = sim.addProjectionPlot(10, 304, 441, 308, "Sensory states")
        val proj = projComp.projector
        proj.tolerance = 1.0
        // proj.setProjectionMethod("Coordinate Projection");
        //((ProjectCoordinate)proj.getProjectionMethod()).setAutoFind(false);
        //((ProjectCoordinate)proj.getProjectionMethod()).setHiD1(0);
        //((ProjectCoordinate)proj.getProjectionMethod()).setHiD2(1);
        // TODO: Below can be sensorNodes or vehicleNetwork
        sim.couple(vehicleNetwork, projComp)
        proj.coloringManager = MarkovColoringManager()

        // Time series
        val tsPlot = sim.addTimeSeries(440, 304, 384, 308, "Prediction Error")
        tsPlot.model.isAutoRange = false
        tsPlot.model.fixedWidth = false
        tsPlot.model.windowSize = 1000
        tsPlot.model.rangeUpperBound = 1.1
        tsPlot.model.rangeLowerBound = -.1

        tsPlot.model.removeAllScalarTimeSeries()
        val ts1 = tsPlot.model.addScalarTimeSeries("Current State Probability / Fulfillment")

        val probability = sim.getProducer(projComp, "getCurrentPointActivation")
        val timeSeries = sim.getConsumer(ts1, "setValue")
        sim.couple(probability, timeSeries)
    }

    // TODO: Current set up just runs the mouse by the cheese once
    // for simple testing while debugging the prediction stuff
    // Should start off low probability and the probability should slowly rise
    var numTrials: Int = 5

    private fun setUpControlPanel() {
        panel = ControlPanel.makePanel(sim, "Control Panel", 77, 8, 100, 76)

        panel.addButton("Run", Runnable {
            for (trial in 0 until numTrials) {
                mouse.randomizeLocationAndHeading()
                sim.iterate(300)
            }
        })
    }

    private val submenuName: String
        get() = "Cognitive Maps"

    override fun getName(): String {
        return "Randomized Pursuer"
    }

    override fun instantiate(desktop: SimbrainDesktop): RandomizedPursuer {
        return RandomizedPursuer(desktop)
    }
}
