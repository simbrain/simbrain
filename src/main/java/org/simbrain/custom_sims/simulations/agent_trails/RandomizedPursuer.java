package org.simbrain.custom_sims.simulations.agent_trails;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.Vehicle;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.util.projection.Projector;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateComponent;
import org.simbrain.workspace.updater.WorkspaceUpdater;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

import java.util.Arrays;

/**
 * Create a Braitenberg pursuer with a projection plot to sensor neurons.
 * Button to run it from randomized spots to creates a cognitive map
 * of its input space.
 */
public class RandomizedPursuer extends Simulation {

    NetworkComponent nc;
    OdorWorldEntity mouse;
    OdorWorldEntity cheese, flower, fish;
    ControlPanel panel;
    NeuronCollection vehicleNetwork;
    NeuronCollection sensorNodes, motorNodes;

    OdorWorldComponent oc;

    int cheeseX = 200;
    int cheeseY = 250;

    public RandomizedPursuer() {
        super();
    }

    public RandomizedPursuer(SimbrainDesktop desktop) {
        super(desktop);
    }

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Create the odor world
        createOdorWorld();

        // Build the network
        buildNetwork();

        // Set up control panel
        setUpControlPanel();

        // Set up Plots
        setUpPlots();

        // Set up custom update
        WorkspaceUpdater updater = sim.getWorkspace().getUpdater();
        updater.getUpdateManager().addAction(
                new UpdateComponent(oc),0);

    }

    private void createOdorWorld() {

        oc = sim.addOdorWorld(626,8,378,297, "World");
        oc.getWorld().setObjectsBlockMovement(false);

        mouse = oc.getWorld().addEntity(0, 0, EntityType.MOUSE);
        mouse.setHeading(90);
        mouse.setLocationRelativeToCenter(0, 70);
        mouse.addLeftRightSensors(EntityType.SWISS, 200);
        mouse.addDefaultEffectors();

        cheese = oc.getWorld().addEntity(0,0, EntityType.SWISS, new double[]{1, 0, 0});
        cheese.setLocationRelativeToCenter(0,-30);
        oc.getWorld().update();

    }

    private void buildNetwork() {
        nc = sim.addNetwork(179,8,447,296, "Pursuer");
        Network net = nc.getNetwork();
        Vehicle pursuer = new Vehicle(sim, net);
        vehicleNetwork = pursuer.addPursuer(10, 10,
                mouse, EntityType.SWISS,
                (ObjectSensor) mouse.getSensors().get(0),
                (ObjectSensor)  mouse.getSensors().get(1));
        vehicleNetwork.setLabel("Pursuer");
        Neuron sensor1 = net.getNeuronByLabel("Swiss (L)");
        Neuron sensor2 = net.getNeuronByLabel("Swiss (R)");
        sensorNodes = new NeuronCollection(net, Arrays.asList(sensor1, sensor2));
        sensorNodes.setLabel("Sensor Nodes");
        net.addNetworkModel(sensorNodes);
    }

    private void setUpPlots() {

        // Projection plot
        ProjectionComponent projComp = sim.addProjectionPlot(178,304,441,308, "Sensory states");
        Projector proj = projComp.getProjector();
        proj.setTolerance(1);
        //proj.setProjectionMethod("Coordinate Projection");
        //((ProjectCoordinate)proj.getProjectionMethod()).setAutoFind(false);
        //((ProjectCoordinate)proj.getProjectionMethod()).setHiD1(0);
        //((ProjectCoordinate)proj.getProjectionMethod()).setHiD2(1);
        // TODO: Below can be sensorNodes or vehicleNetwork
        sim.couple(vehicleNetwork, projComp);
        projComp.getProjector().getColorManager().setColoringMethod("Bayesian");

        // Time series
        TimeSeriesPlotComponent tsPlot = sim.addTimeSeries(619,304,384,308, "Prediction");
        tsPlot.getModel().setAutoRange(false);
        tsPlot.getModel().setFixedWidth(false);
        tsPlot.getModel().setWindowSize(1000);
        tsPlot.getModel().setRangeUpperBound(1.1);
        tsPlot.getModel().setRangeLowerBound(-.1);

        tsPlot.getModel().removeAllScalarTimeSeries();
        TimeSeriesModel.ScalarTimeSeries ts1 = tsPlot.getModel().addScalarTimeSeries("Current State Probability / Fulfillment");

        Producer probability = sim.getProducer(projComp, "getCurrentStateProbability");
        Consumer timeSeries = sim.getConsumer(ts1, "setValue");
        sim.couple(probability, timeSeries);

    }

    // TODO: Current set up just runs the mouse by the cheese once
    // for simple testing while debugging the prediction stuff
    // Should start off low probability and the probability should slowly rise
    int numTrials = 5;

    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 77,8,100,76);

        panel.addButton("Run", () -> {
            for (int trial = 0; trial  < numTrials; trial ++) {
                mouse.randomizeLocation();
                sim.iterate(300);
            }
        });

    }

    private String getSubmenuName() {
        return "Cognitive Maps";
    }

    @Override
    public String getName() {
        return "Randomized Pursuer";
    }

    @Override
    public RandomizedPursuer instantiate(SimbrainDesktop desktop) {
        return new RandomizedPursuer(desktop);
    }

}
