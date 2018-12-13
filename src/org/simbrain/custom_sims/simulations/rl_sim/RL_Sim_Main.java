package org.simbrain.custom_sims.simulations.rl_sim;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.*;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.piccolo.TileMap;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Producible;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Class to build RL Simulation.
 *
 * <p>
 * TODO: Add .htmlfile to folder and make docs based on that
 * TODO: A number of things have been disabled (e.g. right/left prediction nets)
 * while I rebuild the original simulation in the new 3.1 framework.
 *
 * At any time, only the "winning" vehicle subnetwork is updated.
 */
// CHECKSTYLE:OFF
public class RL_Sim_Main extends RegisteredSimulation {

    /**
     * List of "sub-simulations" available from this one.
     */
    List<RL_Sim> simList = new ArrayList<>();

    /**
     * List of vehicles.
     */
    List<NeuronGroup> vehicles = new ArrayList<>();

    /**
     * Number of trials per run.
     */
    int numTrials = 5;

    /**
     * Learning Rate.
     */
    double alpha = 5;

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
    double gamma = .4;

    /**
     * Distance in pixels within which a goal object is counted as being arrived
     * at.
     */
    double hitRadius = 70;

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
    OdorWorldBuilder worldBuilder;
    PlotBuilder plot;

    SmellSensor leftSmell, rightSmell;
    ObjectSensor flowerLeft, flowerRight, cheeseLeft, cheeseRight, candleLeft, candleRight;

    /**
     * Entities that a simulation can refer to.
     */
    OdorWorldEntity mouse;
    OdorWorldEntity flower;
    OdorWorldEntity cheese;
    OdorWorldEntity candle;

    /**
     * Neural net variables.
     */
    Network network;
    Neuron reward;
    Neuron value;
    Neuron tdError;
    double preditionError; // used to set "confidence interval" on plot halo
    Neuron deltaReward;
    NeuronGroup rightInputs, leftInputs;
    SynapseGroup rightInputOutput, leftInputOutput;
    WinnerTakeAll wtaNet;
    JTextField trialField = new JTextField();
    JTextField discountField = new JTextField();
    JTextField alphaField = new JTextField();
    JTextField lambdaField = new JTextField();
    JTextField epsilonField = new JTextField();
    RL_Update updateMethod;
    double[] combinedInputs;
    double[] combinedPredicted;
    NeuronGroup predictionLeft, predictionRight;
    SynapseGroup rightInputToRightPrediction, outputToRightPrediction, leftInputToLeftPrediction, outputToLeftPrediction;
    List<Synapse> rightToWta;
    List<Synapse> leftToWta;

    /**
     * Construct the reinforcement learning simulation.
     *
     * @param desktop
     */
    public RL_Sim_Main(SimbrainDesktop desktop) {
        super(desktop);
    }

    public RL_Sim_Main() {
        super();
    }

    /**
     * Initialize the simulation.
     */
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Create the network builder
        NetBuilder net = sim.addNetwork(228, 3,563,597, "Neural Network");
        network = net.getNetwork();

        // Set up the control panel and tabbed pane
        setUpControlPanel();

        // Create the odor world builder with default vals
        worldBuilder = sim.addOdorWorld(778,3,472,330, "Virtual World");
        world = worldBuilder.getWorld();
        world.setObjectsBlockMovement(false);
        //world.setWrapAround(false);
        world.setTileMap(TileMap.create("empty.tmx"));
        initializeWorldObjects();

        // Add all simulations (first added is default)
        addSim("All-Three", new ThreeObjects(this));
        addSim("Cheese-Flower", new CheeseFlower(this));
        addSim("One Object", new OneCheese(this));
        simList.get(0).load();

        // Force an update on the world so that graphics show properly
        world.update();

        // Set up the main input-output network that is trained via RL
        setupNetworks(net);

        // Set up the reward and td error nodes
        setUpRLNodes(net);

        // Clear all learnable weights
        clearWeights();

        // Set up the vehicle networks
        setUpVehicleNets(net, worldBuilder);

        // Initialize arrays for concatenating left/right inputs
        combinedInputs = new double[leftInputs.size() + rightInputs.size()];
        combinedPredicted = new double[leftInputs.size() + rightInputs.size()];

        // Set up the time series plot
        //setUpTimeSeries(net);

        // Set up projection plot
        setUpProjectionPlot();

        // Set custom network update
        network.getUpdateManager().clear();
        updateMethod = new RL_Update(this);
        network.addUpdateAction(updateMethod);

    }

    /**
     * Manually create the mouse and all agents that can be used in any RL
     * "sub-simulation."
     */
    private void initializeWorldObjects() {

        mouse = worldBuilder.addAgent(43, 110, "Mouse");
        mouse.setHeading(0);
        // Add default effectors
        mouse.addEffector(new StraightMovement(mouse,
            "Go-straight"));
        mouse.addEffector(new Turning(mouse, "Go-left",
            Turning.LEFT));
        mouse.addEffector(new Turning(mouse, "Go-right",
            Turning.RIGHT));

        leftSmell = new SmellSensor(mouse, "Smell-Left", -Math.PI / 8,
            50);
        rightSmell = new SmellSensor(mouse, "Smell-Right", Math.PI / 8,
            50);
        mouse.addSensor(leftSmell);
        mouse.addSensor(rightSmell);

        // Set up smell sources
        cheese = worldBuilder.addEntity(350, 29, EntityType.SWISS, new double[] {1, 0, 0, 0, 0, 1});
        cheese.getSmellSource().setDispersion(350);
        candle = worldBuilder.addEntity(350, 29, EntityType.CANDLE,  new double[] { 0, 1, 0, 0, 0, -1 });
        candle.getSmellSource().setDispersion(350);
        flower = worldBuilder.addEntity(350, 212, EntityType.FLOWER, new double[] {0, 0, 1, 0, 0, 1});
        flower.getSmellSource().setDispersion(350);

        // Used in Vehicle class
        double dispersion = 300;
        cheeseLeft = new ObjectSensor(mouse, EntityType.SWISS, Math.PI/8, 50);
        cheeseLeft.getDecayFunction().setDispersion(dispersion);
        mouse.addSensor(cheeseLeft);
        cheeseRight = new ObjectSensor(mouse, EntityType.SWISS, -Math.PI/8, 50);
        cheeseRight.getDecayFunction().setDispersion(dispersion);
        mouse.addSensor(cheeseRight);
        flowerLeft = new ObjectSensor(mouse, EntityType.FLOWER, Math.PI/8, 50);
        flowerLeft.getDecayFunction().setDispersion(dispersion);
        mouse.addSensor(flowerLeft);
        flowerRight = new ObjectSensor(mouse, EntityType.FLOWER, -Math.PI/8, 50);
        flowerRight.getDecayFunction().setDispersion(dispersion);
        mouse.addSensor(flowerRight);
        candleLeft = new ObjectSensor(mouse, EntityType.CANDLE, Math.PI/8, 50);
        candleLeft.getDecayFunction().setDispersion(dispersion);
        mouse.addSensor(candleLeft);
        candleRight = new ObjectSensor(mouse, EntityType.CANDLE, -Math.PI/8, 50);
        candleRight.getDecayFunction().setDispersion(dispersion);
        mouse.addSensor(candleRight);

    }


    /**
     * Set up main networks
     */
    private void setupNetworks(NetBuilder net) {

        // WTA network that routes to vehicles
        wtaNet = net.addWTAGroup(-234, 58, 3);
        wtaNet.setUseRandom(true);
        wtaNet.setRandomProb(epsilon);
        // Add a little extra spacing between neurons to accommodate labels
        wtaNet.setLayout(new LineLayout(80, LineLayout.LineOrientation.HORIZONTAL));
        wtaNet.applyLayout();
        wtaNet.setLabel("Outputs");

        // Inputs
        rightInputs = net.addNeuronGroup(-104, 350, 6);
        rightInputs.setLabel("Right Inputs");
        // rightInputs.setClamped(true);
        leftInputs = net.addNeuronGroup(-481, 350, 6);
        leftInputs.setLabel("Left Inputs");
        // leftInputs.setClamped(true);

        // Couple distributed smell sensors to neuron groups
        sim.couple(rightSmell, rightInputs);
        sim.couple(leftSmell, leftInputs);


        // Prediction Network
        predictionLeft = net.addNeuronGroup(-589.29, 188.50, 6);
        predictionLeft.setLabel("Predicted (L)");
        predictionRight = net.addNeuronGroup(126, 184, 6);
        predictionRight.setLabel("Predicted (R)");

        // Connect input networks to prediction networks
        rightInputToRightPrediction = net.addSynapseGroup(rightInputs, predictionRight);
        leftInputToLeftPrediction = net.addSynapseGroup(leftInputs, predictionLeft);
        outputToRightPrediction = net.addSynapseGroup(wtaNet, predictionRight);
        outputToLeftPrediction = net.addSynapseGroup(wtaNet, predictionLeft);

        // Connect input nodes to wta network
        rightToWta  = net.connectAllToAll(rightInputs, wtaNet);
        leftToWta = net.connectAllToAll(leftInputs, wtaNet);

    }

    /**
     * Set up the reward, value and td nodes
     */
    private void setUpRLNodes(NetBuilder net) {
        reward = net.addNeuron(300, 0);
        //reward.setClamped(true);
        reward.setLabel("Reward");
        //sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), reward);
        net.connect(leftInputs.getNeuron(5),reward,1);
        value = net.addNeuron(350, 0);
        value.setLabel("Value");
        net.connectAllToAll(rightInputs, value);
        net.connectAllToAll(leftInputs, value);

        tdError = net.addNeuron(400, 0);
        tdError.setLabel("TD Error");
        tdError.setClamped(true);

        deltaReward = net.addNeuron(300, -50);
        deltaReward.setClamped(true);
        deltaReward.setLabel("Delta Reward");
    }

    /**
     * Set up the vehicle networks
     */
    private void setUpVehicleNets(NetBuilder net, OdorWorldBuilder world) {
        // Labels for vehicles, which must be the same as the label for
        // the corresponding output node
        String strPursueCheese = "Pursue Cheese";
        String strPursueFlower = "Pursue Flower";
        String strPursueCandle = "Pursue Candle";
        String strAvoidFlower = "Avoid Flower";
        String strAvoidCheese = "Avoid Cheese";
        String strAvoidCandle = "Avoid Candle";

        // Make the vehicle networks
        // Positions determined by laying by hand and in console running
        // print(getNetwork("Neural Network"));
        Vehicle vehicleBuilder = new Vehicle(sim, net, world);
        NeuronGroup pursueCheese = vehicleBuilder.addPursuer(-509, -460, mouse, EntityType.SWISS, cheeseLeft, cheeseRight);
        pursueCheese.setLabel(strPursueCheese);
        NeuronGroup pursueFlower = vehicleBuilder.addPursuer(-171, -469, mouse, EntityType.FLOWER, flowerLeft, flowerRight);
        pursueFlower.setLabel(strPursueFlower);
        NeuronGroup pursueCandle = vehicleBuilder.addPursuer(163, -475, mouse, EntityType.CANDLE, candleLeft, candleRight);
        pursueCandle.setLabel(strPursueCandle);

        // NeuronGroup avoidCheese = vehicleBuilder.addAvoider(-340, -247, mouse, EntityType.SWISS, cheeseLeft ,cheeseRight);
        // avoidCheese.setLabel(strAvoidCheese);
        // NeuronGroup avoidFlower = vehicleBuilder.addAvoider(-41, -240, mouse, EntityType.FLOWER, flowerLeft, flowerRight);
        // avoidFlower.setLabel(strAvoidFlower);
        // NeuronGroup avoidCandle = vehicleBuilder.addAvoider(218, -239, mouse, EntityType.CANDLE, candleLeft, candleRight);
        // avoidCandle.setLabel(strAvoidCandle);

        setUpVehicle(pursueCheese);
        setUpVehicle(pursueFlower);
        setUpVehicle(pursueCandle);
        //setUpVehicle(avoidCheese);
        //setUpVehicle(avoidFlower);
        //setUpVehicle(avoidCandle);

        // Label output nodes according to the subnetwork they control.
        // The label is also used in RL_Update to enable or disable vehicle nets
        wtaNet.getNeuronList().get(0).setLabel(strPursueCheese);
        wtaNet.getNeuronList().get(1).setLabel(strPursueFlower);
        wtaNet.getNeuronList().get(2).setLabel(strPursueCandle);
        //wtaNet.getNeuronList().get(3).setLabel(strAvoidCheese);
        //wtaNet.getNeuronList().get(4).setLabel(strAvoidFlower);
        //wtaNet.getNeuronList().get(5).setLabel(strAvoidCandle);

        // Connect output nodes to vehicle nodes
        net.connect(wtaNet.getNeuronByLabel(strPursueCheese), pursueCheese.getNeuronByLabel("Speed"), 10);
        net.connect(wtaNet.getNeuronByLabel(strPursueFlower), pursueFlower.getNeuronByLabel("Speed"), 10);
        net.connect(wtaNet.getNeuronByLabel(strPursueCandle), pursueCandle.getNeuronByLabel("Speed"), 10);
        //net.connect(wtaNet.getNeuronByLabel(strAvoidCheese), avoidCheese.getNeuronByLabel("Speed"), 10);
        //net.connect(wtaNet.getNeuronByLabel(strAvoidFlower), avoidFlower.getNeuronByLabel("Speed"), 10);
        //net.connect(wtaNet.getNeuronByLabel(strAvoidCandle), avoidCandle.getNeuronByLabel("Speed"), 10);
    }

    /**
     * Helper method to set up vehicles to this sim's specs.
     *
     * @param vehicle vehicle to modify
     */
    private void setUpVehicle(NeuronGroup vehicle) {
        Neuron speedNeuron = vehicle.getNeuronByLabel("Speed");
        speedNeuron.setUpdateRule("LinearRule");
        // ((LinearRule)speedNeuron.getUpdateRule()).setBias(1); // Just so things move a bit
        speedNeuron.setUpperBound(100);
        speedNeuron.setClamped(false);
        Neuron turnLeft = vehicle.getNeuronByLabel("Left");
        turnLeft.setUpperBound(200);
        turnLeft.setUpperBound(200);
        Neuron turnRight = vehicle.getNeuronByLabel("Right");
        turnRight.setUpperBound(200);
        vehicles.add(vehicle);
    }

    /**
     * Clear all learnable weights
     */
    void clearWeights() {
        for (Synapse synapse : value.getFanIn()) {
            synapse.setStrength(0);
        }
        network.fireNeuronsUpdated();
        if (updateMethod != null) {
            // TODO: Is this needed?
            updateMethod.initMap();
        }
    }

    /**
     * Run one trial from an initial state until it reaches cheese.
     */
    void runTrial() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            public void run() {

                // At the beginning of each trial, load the values
                // from the control panel in.
                numTrials = Integer.parseInt(trialField.getText());
                gamma = Double.parseDouble(discountField.getText());
                lambda = Double.parseDouble(lambdaField.getText());
                epsilon = Double.parseDouble(epsilonField.getText());
                alpha = Double.parseDouble(alphaField.getText());
                wtaNet.setRandomProb(epsilon);
                stop = false;

                // Run the trials
                for (int i = 1; i < numTrials + 1; i++) {
                    if (stop) {
                        return;
                    }
                    resetTrial(i);

                    // Keep iterating until the mouse achieves its goal
                    // Goal is currently to get near a cheese
                    while (!goalAchieved) {
                        sim.iterate();
                        updateGoalState();
                    }
                }

                // Reset the text in the trial field
                trialField.setText("" + numTrials);
            }
        });
    }

    /**
     * Decide if the goal has been achived.
     */
    void updateGoalState() {
        for (OdorWorldEntity entity : getCurrentSim().goalEntities) {
            int distance = (int) SimbrainMath.distance(mouse.getCenterLocation(), entity.getCenterLocation());
            if (distance < hitRadius) {
                goalAchieved = true;
            }
        }
    }

    /**
     * Set up a new trial. Reset things as needed.
     *
     * @param trialNum the trial to set
     */
    void resetTrial(int trialNum) {
        // Set up the trial
        trialField.setText("" + ((numTrials + 1) - trialNum));
        goalAchieved = false;

        // Clear network activations between trials
        network.clearActivations();

        resetMouse();
    }

    /**
     * Resets the position of the mouse.
     */
    void resetMouse() {
        mouse.setLocation(getCurrentSim().mouse_x, getCurrentSim().mouse_y);
        mouse.setHeading(getCurrentSim().mouse_heading);
    }

    /**
     * Set up the top-level control panel.
     */
    void setUpControlPanel() {

        // Create control panel
        controlPanel = ControlPanel.makePanel(sim, "RL Controls", -6,1,246,597);

        // Set up text fields
        trialField = controlPanel.addTextField("Trials", "" + numTrials);
        discountField = controlPanel.addTextField("Discount (gamma)", "" + gamma);
        lambdaField = controlPanel.addTextField("Lambda", "" + lambda);
        epsilonField = controlPanel.addTextField("Epsilon", "" + epsilon);
        alphaField = controlPanel.addTextField("Learning rt.", "" + alpha);

        controlPanel.addBottomComponent(tabbedPane);

        // Run Button
        controlPanel.addButton("Run", () -> {
            runTrial();
        });

        // Stop Button
        controlPanel.addButton("Stop", () -> {
            goalAchieved = true;
            stop = true;
        });

        // Clear Weights Button
        controlPanel.addButton("Clear Weights", () -> {
            clearWeights();
        });
        
    }

    /**
     * Returns a reference to the currently open RL Sim.
     *
     * @return the rl sim in the current tab
     */
    public RL_Sim getCurrentSim() {
        return simList.get(tabbedPane.getSelectedIndex());
    }

    /**
     * Add a new RL Sim to the tab at the bottom of the control panel.
     *
     * @param simName the name of the sim
     * @param sim     the sim itself
     */
    private void addSim(String simName, RL_Sim sim) {
        simList.add(sim);
        tabbedPane.add(simName, sim.controls);
    }

    /**
     * Remove the custom action which handles RL Updates. Useful to be able to
     * remove it sometimes while running other simulations.
     */
    void removeCustomAction() {
        network.getUpdateManager().clear();
    }

    /**
     * Helper method for "combined input" coupling.
     */
    @Producible
    public double[] getCombinedInputs() {
        System.arraycopy(leftInputs.getActivations(), 0, combinedInputs, 0, leftInputs.size() - 1);
        System.arraycopy(rightInputs.getActivations(), 0, combinedInputs, leftInputs.size(), rightInputs.size());
        // Why copy needed?
        return Arrays.copyOf(combinedInputs, combinedInputs.length);
    }

    /**
     * Helper method for getting combined prediction.
     */
    @Producible
    public double[] getCombinedPredicted() {
        System.arraycopy(predictionLeft.getActivations(), 0, combinedPredicted, 0, predictionLeft.size() - 1);
        System.arraycopy(predictionRight.getActivations(), 0, combinedPredicted, predictionLeft.size(), predictionRight.size());
        return Arrays.copyOf(combinedPredicted, combinedPredicted.length);
    }

    /**
     * Set up the time series plot.
     */
    private void setUpTimeSeries(NetBuilder net) {
        // Create a time series plot
        PlotBuilder plot = sim.addTimeSeriesPlot(0, 328, 293, 332, "Reward, TD Error");
        sim.couple(net.getNetworkComponent(), reward, plot.getTimeSeriesComponent(), 0);
        sim.couple(net.getNetworkComponent(), tdError, plot.getTimeSeriesComponent(), 1);
        plot.getTimeSeriesModel().setAutoRange(false);
        plot.getTimeSeriesModel().setRangeUpperBound(2);
        plot.getTimeSeriesModel().setRangeLowerBound(-1);
    }

    private void setUpProjectionPlot() {
        plot = sim.addProjectionPlot(779,339,355,330, "Sensory states + Predictions");
        plot.getProjectionModel().getProjector().setUseColorManager(false);
        plot.getProjectionModel().getProjector().setTolerance(.01);
        Producer inputProducer = sim.getProducer(this, "getCombinedInputs");
        Consumer plotConsumer = sim.getConsumer(plot.getProjectionPlotComponent(), "addPoint");
        sim.tryCoupling(inputProducer, plotConsumer);
        sim.getWorkspace().addUpdateAction(new ColorPlot(this));

        // Label PCA points based on closest object
        Producer currentObject = sim.getProducer(mouse, "getNearbyObjects");
        Consumer plotText = sim.getConsumer(plot.getProjectionPlotComponent(), "setLabel");
        sim.tryCoupling(currentObject, plotText);
    }

    @Override
    public String getName() {
        return "RL Vehicles";
    }

    @Override
    public RL_Sim_Main instantiate(SimbrainDesktop desktop) {
        return new RL_Sim_Main(desktop);
    }

    public Simulation getSimulation() {
        return sim;
    }

}
