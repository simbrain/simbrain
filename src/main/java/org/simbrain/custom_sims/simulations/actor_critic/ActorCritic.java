package org.simbrain.custom_sims.simulations.actor_critic;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.util.decayfunctions.DecayFunction;
import org.simbrain.util.decayfunctions.StepDecayFunction;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.couplings.Coupling;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateAction;
import org.simbrain.workspace.updater.UpdateActionKt;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.GridSensor;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import static org.simbrain.network.core.NetworkUtilsKt.connectAllToAll;

/**
 * Create the actor-critic simulation.
 */
// CHECKSTYLE:OFF
public class ActorCritic extends Simulation {

    /**
     * Number of trials per run.
     */
    int numTrials = 5;

    /**
     * Learning Rate.
     */
    double alpha = .25;

    /**
     * Eligibility trace. 0 for no trace; 1 for permanent trace. .9 default. Not
     * currently used.
     */
    double lambda = 0;

    /**
     * Prob. of taking a random action. "Exploitation" vs. "exploration".
     */
    double epsilon = .25;

    /**
     * Discount factor . 0-1. 0 predict next value only. .5 predict future
     * values. As it increases toward one, values of y in the more distant
     * future become more significant.
     */
    double gamma = 1;

    /**
     * GUI Variables.
     */
    ControlPanel controlPanel;
    JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Other variables and references.
     */
    boolean stop = false;
    boolean goalAchieved = false;
    OdorWorld world;
    OdorWorldComponent oc;
    NetworkComponent nc;

    /**
     * Tile World.
     */
    int tileSets = 1; // Number of tilesets
    int numTiles = 5; // Number of rows / cols in each tileset
    int worldWidth = 320;
    int worldHeight = 320;
    double initTilesX = 100;
    double initTilesY = 100;
    int tileSize = worldHeight / numTiles;
    double rewardDispersionFactor = 2; // Number of tiles for reward to disperse
    double movementFactor = 1; // Number of tiles to move
    double tileIncrement = (worldHeight / numTiles) / tileSets;
    double hitRadius = rewardDispersionFactor * (tileSize / 2);
    int mouseHomeLocation = (tileSize * numTiles) - tileSize / 2;

    /**
     * Entities that a simulation can refer to.
     */
    OdorWorldEntity mouse;
    OdorWorldEntity cheese; // TODO: Change to goal or generify like RL_Sim?

    /**
     * Couplings.
     */
    List<Coupling> effectorCouplings = new ArrayList<>();
    List<Coupling> sensorCouplings = new ArrayList<>();

    /**
     * Neural net variables.
     */
    Network net;
    List<Neuron> tileNeurons;
    Neuron reward;
    Neuron value;
    Neuron tdError;
    NeuronGroup sensorNeurons;
    double predictionError; // used to set "confidence interval" on plot halo
    WinnerTakeAll outputs;
    JTextField trialField = new JTextField();
    JTextField discountField = new JTextField();
    JTextField alphaField = new JTextField();
    JTextField lambdaField = new JTextField();
    JTextField epsilonField = new JTextField();
    RL_Update updateMethod;

    /**
     * Construct the reinforcement learning simulation.
     *
     * @param desktop
     */
    public ActorCritic(SimbrainDesktop desktop) {
        super(desktop);
    }

    public ActorCritic() {
        super();
    }

    /**
     * Initialize the simulation
     */
    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Create the network wrapper
        nc = sim.addNetwork(283,0,522,595, "Neural Network");
        net = nc.getNetwork();

        // Set up the control panel and tabbed pane
        makeControlPanel();
        controlPanel.addBottomComponent(tabbedPane);

        // Set up the main input-output network that is trained via RL
        setUpInputOutputNetwork(net);

        // Set up the reward and td error nodes
        setUpRLNodes(net);

        // Set up the tile world
        setUpWorldAndNetwork();

        // Set up the time series plot
        setUpPlot(nc);

        // Set custom network update
        net.getUpdateManager().clear();
        updateMethod = new RL_Update(this);
        net.addUpdateAction(updateMethod);

        // Add docviewer
        sim.addDocViewer(0,301,279,336, "Information",
                "ActorCritic.html");

        // Add method for custom update
        addCustomWorkspaceUpdate();

    }

    /**
     * Add custom workspace update method.
     */
    private void addCustomWorkspaceUpdate() {
        // Custom workspace update rule
        UpdateAction workspaceUpdateAction = UpdateActionKt.create("Actor Critic", () -> {
            // Update net > movement couplings
            sim.getWorkspace().getCouplingManager().updateCouplings(effectorCouplings);

            // Update world
            oc.getWorld().update();

            // Update world > tile neurons
            // sensorcoupling also couples to the time series plot
            sim.getWorkspace().getCouplingManager().updateCouplings(sensorCouplings);

            // Fourth: update network
            nc.update();
        });
        sim.getWorkspace().getUpdater().getUpdateManager().clear();
        sim.getWorkspace().addUpdateAction(workspaceUpdateAction);
    }

    /**
     * Set up the "grid world" and tile sensors.
     */
    void setUpWorldAndNetwork() {

        oc = sim.addOdorWorld(807,0,462,503, "actor-critic.tmx");
        world = oc.getWorld();
        world.setObjectsBlockMovement(false);
        world.setWrapAround(false);

        mouse = new OdorWorldEntity(world, EntityType.MOUSE);
        world.addEntity(mouse);
        resetMouse();

        // Set up cheese
        cheese = new OdorWorldEntity(world, EntityType.SWISS);
        cheese.setCenterLocation(tileSize / 2, tileSize / 2);
        world.addEntity(cheese);

        // Set up cheese sensor
        ObjectSensor cheeseSensor = new ObjectSensor(mouse);
        cheeseSensor.setLabel("Cheese sensor");
        double dispersion = rewardDispersionFactor * (tileSize / 2);
        DecayFunction decayFunction = new StepDecayFunction();
        cheeseSensor.setDecayFunction(decayFunction);
        mouse.addSensor(cheeseSensor);

        tileNeurons = new ArrayList<>();
        sensorCouplings = new ArrayList<>();

        // Create grid sensor
        GridSensor sensor = new GridSensor(
                mouse,
                0, 0,
                oc.getWorld().getWidth() / numTiles, oc.getWorld().getHeight() / numTiles
        );
        mouse.addSensor(sensor);

        // Set up location sensor neurons
        sensorNeurons = net.addNeuronGroup(initTilesX, initTilesY, numTiles*numTiles, "Grid"
        );
        sensorNeurons.setLabel("Sensor Nodes");
        List<Synapse> wts = connectAllToAll(sensorNeurons, value, 0);
        wts.forEach(w -> w.setLowerBound(0));
        List<Synapse> wts2 =  connectAllToAll(sensorNeurons, outputs, 0);
        wts2.forEach(w -> w.setLowerBound(0));

        // Set up couplings
        Producer gridProducer = sim.getProducer(sensor, "getValues");
        gridProducer.setDescription(sensor.getLabel());
        Consumer ngConsumer = sim.getConsumer(sensorNeurons, "addInputs");
        Coupling gridCoupling = sim.couple(gridProducer, ngConsumer);
        sensorCouplings.add(gridCoupling);
        setCouplings(oc, nc);
    }

    /**
     * Set up the couplings.
     *
     * @param oc odor world component
     * @param nc network component
     */
    private void setCouplings(OdorWorldComponent oc, NetworkComponent nc) {
        effectorCouplings = new ArrayList();

        // Absolute movement couplings
        outputs.getNeuronList().get(0).setLabel("North");
        Producer northProducer = sim.getProducer(outputs.getNeuronList().get(0), "getActivation");
        Consumer northMovement = sim.getConsumer(mouse, "moveNorth");
        northMovement.setDescription("North");
        Coupling northCoupling = sim.couple(northProducer, northMovement);
        effectorCouplings.add(northCoupling);

        outputs.getNeuronList().get(1).setLabel("South");
        Producer southProducer = sim.getProducer(outputs.getNeuronList().get(1), "getActivation");
        Consumer southMovement = sim.getConsumer(mouse, "moveSouth");
        southMovement.setDescription("South");
        Coupling southCoupling = sim.couple(southProducer, southMovement);
        effectorCouplings.add(southCoupling);

        outputs.getNeuronList().get(2).setLabel("East");
        Producer eastProducer = sim.getProducer(outputs.getNeuronList().get(2), "getActivation");
        Consumer eastMovement = sim.getConsumer(mouse, "moveEast");
        eastMovement.setDescription("East");
        Coupling eastCoupling = sim.couple(eastProducer, eastMovement);
        effectorCouplings.add(eastCoupling);

        outputs.getNeuronList().get(3).setLabel("West");
        Producer westProducer = sim.getProducer(outputs.getNeuronList().get(3), "getActivation");
        Consumer westMovement = sim.getConsumer(mouse, "moveWest");
        westMovement.setDescription("West");
        Coupling westCoupling = sim.couple(westProducer, westMovement);
        effectorCouplings.add(westCoupling);

        // Add reward smell coupling
        Producer smell = sim.getProducer(mouse.getSensor("Cheese sensor"), "getCurrentValue");
        smell.setDescription("Reward");
        Consumer rewardConsumer = sim.getConsumer(reward, "forceSetActivation");
        Coupling rewardCoupling = sim.couple(smell, rewardConsumer);
        sensorCouplings.add(rewardCoupling);
    }

    /**
     * Set up the time series plot.
     */
    private void setUpPlot(NetworkComponent net) {
        TimeSeriesPlotComponent plot = sim.addTimeSeries(759, 377, 363, 285, "Reward, TD Error");
        plot.getModel().setAutoRange(false);
        plot.getModel().setRangeUpperBound(2);
        plot.getModel().setRangeLowerBound(-1);

        plot.getModel().removeAllScalarTimeSeries();
        TimeSeriesModel.ScalarTimeSeries ts1 = plot.getModel().addScalarTimeSeries("Reward");
        TimeSeriesModel.ScalarTimeSeries ts2 = plot.getModel().addScalarTimeSeries("TD Error");
        TimeSeriesModel.ScalarTimeSeries ts3  = plot.getModel().addScalarTimeSeries("Value");

        Coupling rewardCoupling = sim.couple(reward, ts1);
        Coupling tdCoupling = sim.couple(tdError, ts2);
        Coupling valueCoupling = sim.couple(value, ts3);

        sensorCouplings.add(rewardCoupling);
        sensorCouplings.add(tdCoupling);
        sensorCouplings.add(valueCoupling);
    }

    /**
     * Add main input-output network to be trained by RL.
     */
    private void setUpInputOutputNetwork(Network net) {

        // Outputs
        outputs = new WinnerTakeAll(this.net, 4);
        this.net.addNetworkModel(outputs);
        outputs.setUseRandom(true);
        outputs.setRandomProb(epsilon);
        outputs.setWinValue(tileSize * movementFactor);
        // Add a little extra spacing between neurons to accommodate labels
        outputs.setLayout(new LineLayout(80, LineLayout.LineOrientation.HORIZONTAL));
        outputs.applyLayout(-5,-85);
        outputs.setLabel("Outputs");
    }

    /**
     * Set up the reward, value and td nodes
     */
    private void setUpRLNodes(Network net) {
        reward = net.addNeuron(300, 0);
        reward.setClamped(true);
        reward.setLabel("Reward");
        // sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), 5, reward);
        value = net.addNeuron(350, 0);
        value.setLabel("Value");

        tdError = net.addNeuron(400, 0);
        tdError.setLabel("TD Error");
    }

    /**
     * Run one trial from an initial state until it reaches cheese.
     */
    void runTrial() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {

                // At the beginning of each trial, load the values
                // from the control panel in.
                numTrials = Integer.parseInt(trialField.getText());
                gamma = Double.parseDouble(discountField.getText());
                // lambda = Double.parseDouble(lambdaField.getText());
                epsilon = Double.parseDouble(epsilonField.getText());
                alpha = Double.parseDouble(alphaField.getText());
                outputs.setRandomProb(epsilon);
                stop = false;

                // Run the trials
                for (int i = 1; i < numTrials + 1; i++) {
                    if (stop) {
                        return;
                    }

                    trialField.setText("" + ((numTrials + 1) - i));

                    goalAchieved = false;

                    net.clearActivations();

                    resetMouse();

                    // Keep iterating until the mouse achieves its goal
                    // Goal is currently to get near the cheese
                    while (!goalAchieved) {
                        int distance = (int) SimbrainMath.distance(mouse.getCenterLocation(), cheese.getCenterLocation());
                        if (distance < hitRadius) {
                            goalAchieved = true;
                        }
                        sim.iterate();
                    }
                }

                // Reset the text in the trial field
                trialField.setText("" + numTrials);
            }
        });
    }

    /**
     * Init mouse position.
     */
    private void resetMouse() {
        mouse.setCenterLocation(mouseHomeLocation, mouseHomeLocation);
        mouse.setHeading(90);
    }

    /**
     * Set up the top-level control panel.
     */
    void makeControlPanel() {

        // Create control panel
        controlPanel = ControlPanel.makePanel(sim, "RL Controls", 121,-1,161,234);

        // Set up text fields
        trialField = controlPanel.addTextField("Trials", "" + numTrials);
        discountField = controlPanel.addTextField("Discount (gamma)", "" + gamma);
        // lambdaField = controlPanel.addTextField("Lambda", "" + lambda);
        epsilonField = controlPanel.addTextField("Epsilon", "" + epsilon);
        alphaField = controlPanel.addTextField("Learning rt.", "" + alpha);

        // Run Button
        controlPanel.addButton("Run", () -> {
            runTrial();
        });

        // Stop Button
        controlPanel.addButton("Stop", () -> {
            goalAchieved = true;
            stop = true;
        });

    }

    private String getSubmenuName() {
        return "Reinforcement Learning";
    }

    @Override
    public String getName() {
        return "Actor-Critic";
    }

    @Override
    public ActorCritic instantiate(SimbrainDesktop desktop) {
        return new ActorCritic(desktop);
    }

}
