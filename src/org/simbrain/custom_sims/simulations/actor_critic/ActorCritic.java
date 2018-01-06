package org.simbrain.custom_sims.simulations.actor_critic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.helper_classes.PlotBuilder;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.Coupling2;
import org.simbrain.workspace.MismatchedAttributesException;
import org.simbrain.workspace.Consumer2;
import org.simbrain.workspace.Producer2;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateAction;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.BasicEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.TileSensor;

/**
 * Create the actor-critic simulation.
 */
// CHECKSTYLE:OFF
public class ActorCritic extends RegisteredSimulation {

    /** Number of trials per run. */
    int numTrials = 5;

    /** Learning Rate. */
    double alpha = .25;

    /**
     * Eligibility trace. 0 for no trace; 1 for permanent trace. .9 default. Not
     * currently used.
     */
    double lambda = 0;

    /** Prob. of taking a random action. "Exploitation" vs. "exploration". */
    double epsilon = .25;

    /**
     * Discount factor . 0-1. 0 predict next value only. .5 predict future
     * values. As it increases toward one, values of y in the more distant
     * future become more significant.
     */
    double gamma = 1;

    /** GUI Variables. */
    ControlPanel controlPanel;
    JTabbedPane tabbedPane = new JTabbedPane();

    /** Other variables and references. */
    boolean stop = false;
    boolean goalAchieved = false;
    OdorWorld world;
    OdorWorldBuilder ob;
    NetBuilder net;
    PlotBuilder plot;

    /** Tile World. */
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

    /** Entities that a simulation can refer to. */
    RotatingEntity mouse;
    BasicEntity cheese; // TODO: Change to goal or generify like RL_Sim?

    /** Couplings. */
    List<Coupling2<?>> effectorCouplings = new ArrayList<>();
    List<Coupling2<?>> sensorCouplings = new ArrayList<>();

    /** Neural net variables. */
    Network network;
    List<Neuron> tileNeurons;
    Neuron reward;
    Neuron value;
    Neuron tdError;
    double preditionError; // used to set "confidence interval" on plot halo
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
     * Run the simulation!
     */
    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Create the network builder
        net = sim.addNetwork(236, 4, 522, 595, "Neural Network");
        network = net.getNetwork();

        // Set up the control panel and tabbed pane
        makeControlPanel();
        controlPanel.addBottomComponent(tabbedPane);

        // Set up the main input-output network that is trained via RL
        setUpInputOutputNetwork(net);

        // Set up the reward and td error nodes
        setUpRLNodes(net);

        // Set up the tile world
        setUpWorld();

        // Set up the time series plot
        setUpPlot(net);

        // Set custom network update
        network.getUpdateManager().clear();
        updateMethod = new RL_Update(this);
        network.addUpdateAction(updateMethod);

        // Add docviewer
        sim.addDocViewer(0, 301, 253, 313, "Information",
                "src/org/simbrain/custom_sims/simulations/actor_critic/ActorCritic.html");

        // Add method for custom update
        addCustomWorkspaceUpdate();

    }

    /**
     * Add custom workspace upadate method.
     */
    private void addCustomWorkspaceUpdate() {
        // Custom workspace update rule
        UpdateAction workspaceUpdateAction = new UpdateAction() {
            @Override
            public String getDescription() {
                return "Actor Critic";
            }

            @Override
            public String getLongDescription() {
                return "Actor Critic";
            }

            @Override
            public void invoke() {

                // Update net > movement couplings
                sim.getWorkspace().updateCouplings(effectorCouplings);

                // Update world
                ob.getOdorWorldComponent().update();

                // Update world > tile neurons and plot couplings
                sim.getWorkspace().updateCouplings(sensorCouplings);

                // Fourth: update network
                net.getNetworkComponent().update();

                // TODO: Why don't we have to call plot update?
            }

        };
        sim.getWorkspace().getUpdater().getUpdateManager().clear();
        sim.getWorkspace().addUpdateAction(workspaceUpdateAction);
    }

    /**
     * Set up the "grid world" and tile sensors.
     */
    void setUpWorld() {
        // TODO: Why can't I use worldwidth and worldheight below? I had to
        // manually set size.
        ob = sim.addOdorWorld(761, 8, 347, 390, "Tile World");
        world = ob.getWorld();
        world.setObjectsBlockMovement(true);
        world.setWrapAround(false);

        mouse = new RotatingEntity(world);
        world.addAgent(mouse);
        resetMouse();

        cheese = new BasicEntity("Swiss.gif", world);
        double dispersion = rewardDispersionFactor * (tileSize / 2);
        cheese.setCenterLocation(tileSize / 2, tileSize / 2);
        cheese.setSmellSource(new SmellSource(new double[] { 1, 0 },
                SmellSource.DecayFunction.STEP, dispersion,
                cheese.getCenterLocation()));
        world.addEntity(cheese);

        OdorWorldComponent oc = ob.getOdorWorldComponent();
        NetworkComponent nc = net.getNetworkComponent();

        tileNeurons = new ArrayList<Neuron>();
        sensorCouplings = new ArrayList<Coupling2<?>>();
        for (int i = 0; i < tileSets; i++) {
            for (int j = 0; j < numTiles; j++) {
                for (int k = 0; k < numTiles; k++) {

                    // Set location of tile
                    double x = (j * tileSize) - i * tileIncrement;
                    double y = (k * tileSize) - i * tileIncrement;

                    // Create tile sensor
                    TileSensor sensor = new TileSensor(mouse, (int) x, (int) y,
                            tileSize, tileSize);
                    mouse.addSensor(sensor);

                    // Create corresponding neuron
                    Neuron tileNeuron = new Neuron(network, "LinearRule");
                    tileNeurons.add(tileNeuron);
                    tileNeuron.setX(initTilesX + x);
                    tileNeuron.setY(initTilesY + y);
                    network.addNeuron(tileNeuron);

                    Producer2 tileProducer = oc.getProducer(sensor, "getValue");
                    tileProducer.setDescription(sensor.getLabel());
                    Consumer2 neuronConsumer = nc.getConsumer(tileNeuron, "setActivation");
                    neuronConsumer.setDescription(tileNeuron.getId());
                    try {
                        Coupling2<?> tileCoupling = new Coupling2(tileProducer, neuronConsumer);
                        sensorCouplings.add(tileCoupling);
                        sim.getWorkspace().addCoupling(tileCoupling);
                    } catch (MismatchedAttributesException e) {
                        e.printStackTrace();
                    }

                    // TODO: Put in group and use sim.connectAllToAll
                    // why does using 0 make weights non-existent
                    // Connect tile neuron to output / action neurons
                    for (Neuron actionNeuron : outputs.getNeuronList()) {
                        net.connect(tileNeuron, actionNeuron, 0);
                    }

                    // Connect tile neuron to value neuron
                    net.connect(tileNeuron, value, 0);
                }
            }

        }

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
        Producer2 northProducer = nc.getProducer(outputs.getNeuronList().get(0), "getActivation");
        Consumer2 northMovement = oc.getConsumer(mouse, "moveNorth");
        northMovement.setDescription("North");
        try {
            Coupling2 northCoupling = sim.createCoupling(northProducer, northMovement);
            effectorCouplings.add(northCoupling);
        } catch (MismatchedAttributesException e) {
            e.printStackTrace();
        }

        outputs.getNeuronList().get(1).setLabel("South");
        Producer2 southProducer = nc.getProducer(outputs.getNeuronList().get(1), "getActivation");
        Consumer2 southMovement = oc.getConsumer(mouse, "moveSouth");
        southMovement.setDescription("South");
        try {
            Coupling2 southCoupling = sim.createCoupling(southProducer, southMovement);
            effectorCouplings.add(southCoupling);
        } catch (MismatchedAttributesException e) {
            e.printStackTrace();
        }

        outputs.getNeuronList().get(2).setLabel("East");
        Producer2 eastProducer = nc.getProducer(outputs.getNeuronList().get(2), "getActivation");
        Consumer2 eastMovement = oc.getConsumer(mouse, "moveEast");
        eastMovement.setDescription("East");
        try {
            Coupling2 eastCoupling = sim.createCoupling(eastProducer, eastMovement);
            effectorCouplings.add(eastCoupling);
        } catch (MismatchedAttributesException e) {
            e.printStackTrace();
        }

        outputs.getNeuronList().get(3).setLabel("West");
        Producer2 westProducer = nc.getProducer(outputs.getNeuronList().get(3), "getActivation");
        Consumer2 westMovement = oc.getConsumer(mouse, "moveWest");
        westMovement.setDescription("West");
        try {
            Coupling2 westCoupling = sim.createCoupling(westProducer, westMovement);
            effectorCouplings.add(westCoupling);
        } catch (MismatchedAttributesException e) {
            e.printStackTrace();
        }

        // Add reward smell coupling
        Producer2 smell = oc.getProducer(world.getSensor(mouse.getId(), "Sensor_2"), "getCurrentValue");
        smell.setDescription("Reward");
        Consumer2 rewardConsumer = nc.getConsumer(reward, "forceSetActivation");
        try {
            Coupling2 rewardCoupling = sim.createCoupling(smell, rewardConsumer);
            sensorCouplings.add(rewardCoupling);
        } catch (MismatchedAttributesException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set up the time series plot.
     */
    private void setUpPlot(NetBuilder net) {
        // Create a time series plot
        plot = sim.addTimeSeriesPlot(759, 377, 363, 285, "Reward, TD Error");
        Coupling2 rewardCoupling = sim.couple(net.getNetworkComponent(), reward,
                plot.getTimeSeriesComponent(), 0);
        Coupling2 tdCoupling = sim.couple(net.getNetworkComponent(), tdError,
                plot.getTimeSeriesComponent(), 1);
        Coupling2 valueCoupling = sim.couple(net.getNetworkComponent(), value,
                plot.getTimeSeriesComponent(), 2);
        plot.getTimeSeriesModel().setAutoRange(false);
        plot.getTimeSeriesModel().setRangeUpperBound(2);
        plot.getTimeSeriesModel().setRangeLowerBound(-1);
        sensorCouplings.add(rewardCoupling);
        sensorCouplings.add(tdCoupling);
        sensorCouplings.add(valueCoupling);
    }

    /**
     * Add main input-output network to be trained by RL.
     */
    private void setUpInputOutputNetwork(NetBuilder net) {

        // Outputs
        outputs = net.addWTAGroup(-43, 7, 4);
        outputs.setUseRandom(true);
        outputs.setRandomProb(epsilon);
        outputs.setWinValue(tileSize * movementFactor);
        // Add a little extra spacing between neurons to accommodate labels
        outputs.setLayout(
                new LineLayout(80, LineLayout.LineOrientation.HORIZONTAL));
        outputs.applyLayout();
        outputs.setLabel("Outputs");
    }

    /**
     * Set up the reward, value and td nodes
     */
    private void setUpRLNodes(NetBuilder net) {
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

                    network.clearActivations();

                    resetMouse();

                    // Keep iterating until the mouse achieves its goal
                    // Goal is currently to get near the cheese
                    while (!goalAchieved) {
                        int distance = (int) SimbrainMath.distance(
                                mouse.getCenterLocation(),
                                cheese.getCenterLocation());
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
        controlPanel = ControlPanel.makePanel(sim, "RL Controls", -6, 1);

        // Set up text fields
        trialField = controlPanel.addTextField("Trials", "" + numTrials);
        discountField = controlPanel.addTextField("Discount (gamma)",
                "" + gamma);
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

    @Override
    public String getName() {
        return "Actor-Critic";
    }

    @Override
    public ActorCritic instantiate(SimbrainDesktop desktop) {
        return new ActorCritic(desktop);
    }

}
