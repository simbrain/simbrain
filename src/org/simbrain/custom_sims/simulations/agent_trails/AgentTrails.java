package org.simbrain.custom_sims.simulations.agent_trails;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.helper_classes.PlotBuilder;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.piccolo.TileMap;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Todo Stop button.
 * TODO: Redo with object sensors
 */
// CHECKSTYLE:OFF
public class AgentTrails extends RegisteredSimulation {

    NetBuilder net;
    OdorWorldEntity mouse;
    OdorWorldEntity cheese, flower, fish;
    ControlPanel panel;
    NeuronGroup sensoryNet, actionNet, predictionNet;
    Neuron leftNeuron, straightNeuron, rightNeuron;
    Neuron cheeseNeuron, flowerNeuron, fishNeuron;
    Neuron errorNeuron;
    Path csvFile;
    List<String> activationList = new ArrayList<String>();
    PlotBuilder plot;
    OdorWorldBuilder worldBuilder;

    // Default values for these used by buttons
    int dispersion = 45;
    int fishX = 50;
    int fishY = 100;
    int flowerX = 200;
    int flowerY = 100;
    int cheeseX = 120;
    int cheeseY = 180;

    public AgentTrails() {
        super();
    }

    /**
     * @param desktop
     */
    public AgentTrails(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * Run the simulation!
     */
    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build a network
        net = sim.addNetwork(195, 9, 447, 296, "Simple Predicter");
        sensoryNet = net.addNeuronGroup(-9.25, 95.93, 3);
        //sensoryNet.setClamped(true);
        sensoryNet.setLabel("Sensory");
        cheeseNeuron = sensoryNet.getNeuronList().get(0);
        cheeseNeuron.setLabel("Cheese");
        flowerNeuron = sensoryNet.getNeuronList().get(1);
        flowerNeuron.setLabel("Flower");
        fishNeuron = sensoryNet.getNeuronList().get(2);
        fishNeuron.setLabel("Fish");

        actionNet = net.addNeuronGroup(0, -0.79, 3);
        actionNet.setLabel("Actions");
        //actionNet.setClamped(true);
        actionNet.setLabel("Actions");
        leftNeuron = actionNet.getNeuronList().get(0);
        leftNeuron.setLabel("Left");
        straightNeuron = actionNet.getNeuronList().get(1);
        straightNeuron.setLabel("Straight");
        rightNeuron = actionNet.getNeuronList().get(2);
        rightNeuron.setLabel("Right");
        predictionNet = net.addNeuronGroup(231.02, 24.74, 3);
        predictionNet.setLabel("Predicted");

        net.connectAllToAll(sensoryNet, predictionNet);
        net.connectAllToAll(actionNet, predictionNet);

        errorNeuron = net.addNeuron(268, 108);
        //errorNeuron.setClamped(true);
        errorNeuron.setLabel("Error");

        // Create the odor world
        worldBuilder = sim.addOdorWorld(629, 9, 315, 383, "Three Objects");
        worldBuilder.getWorld().setObjectsBlockMovement(false);
        worldBuilder.getWorld().setTileMap(TileMap.create("empty.tmx"));
        mouse = worldBuilder.addAgent(120, 245, "Mouse");
        mouse.setHeading(90);
        mouse.addDefaultSensorsEffectors();
        cheese = worldBuilder.addEntity(cheeseX, cheeseY, OdorWorldEntity.EntityType.SWISS, new double[]{1, 0, 0});
        cheese.getSmellSource().setDispersion(dispersion);
        flower = worldBuilder.addEntity(flowerX, flowerY, OdorWorldEntity.EntityType.FLOWER, new double[]{0, 1, 0});
        flower.getSmellSource().setDispersion(dispersion);
        fish = worldBuilder.addEntity(fishX, fishY, OdorWorldEntity.EntityType.FISH, new double[]{0, 0, 1});
        fish.getSmellSource().setDispersion(dispersion);
        worldBuilder.getWorld().update();

        // Couple network to agent
        sim.couple(straightNeuron, mouse.getEffector("Go-straight"));
        sim.couple(rightNeuron, mouse.getEffector("Go-left"));
        sim.couple(leftNeuron, mouse.getEffector("Go-right"));

        // Couple agent to network
        sim.couple((SmellSensor) mouse.getSensor("Smell-Center"),sensoryNet);

        setUpControlPanel();

        // Set up Plot
        // Create a time series plot
        plot = sim.addProjectionPlot(194, 312, 441, 308, "Sensory states + Predictions");

        plot.getProjectionModel().getProjector().getColorManager().setColoringMethod(null);
        plot.getProjectionModel().init(3);
        plot.getProjectionModel().getProjector().setUseColorManager(false);

        sim.couple(net.getNetworkComponent(), sensoryNet, plot.getProjectionPlotComponent());
        plot.getProjectionModel().getProjector().setTolerance(.01);

        // Label PCA points based on closest object
        // (how nice this looks depends on tolerance. if tolerance too low then too many labels are created)
        //Producer currentObject = sim.getProducer(mouse, "getNearbyObjects");
        //Consumer plotText = sim.getConsumer(plot.getProjectionPlotComponent(), "setLabel");
        //sim.tryCoupling(currentObject, plotText);

        // Custom training
        net.getNetwork().addUpdateAction(new TrainPredictionNet(this));

        // Uncomment lines below to Log activations, as well as save file below
        // csvFile = Paths.get("agentTrails.csv");
        // net.getNetwork().addUpdateAction(new LogActivations(this));

        // Add workspace level update action
        sim.getWorkspace().addUpdateAction(new ColorPlot(this));

    }

    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10);

        // Move past cheese
        panel.addButton("Cheese", () -> {
            net.getNetwork().clearActivations();
            mouse.setLocation(cheeseX, cheeseY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(180);
        });

        // Move past Fish
        panel.addButton("Fish", () -> {
            net.getNetwork().clearActivations();
            mouse.setLocation(fishX, fishY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(180);
        });

        // Move past flower
        panel.addButton("Flower", () -> {
            net.getNetwork().clearActivations();
            mouse.setLocation(flowerX, flowerY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(180);
        });

        // Cheese > Fish
        panel.addButton("Cheese > Flower", () -> {
            net.getNetwork().clearActivations();
            mouse.setLocation(cheeseX, cheeseY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(50);
            rightNeuron.forceSetActivation(1.5);
            sim.iterate(25);
            rightNeuron.forceSetActivation(0);
            sim.iterate(220);
        });

        // Cheese > Flower
        panel.addButton("Cheese > Fish", () -> {
            net.getNetwork().clearActivations();
            mouse.setLocation(cheeseX, cheeseY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(50);
            leftNeuron.forceSetActivation(1.5);
            sim.iterate(25);
            leftNeuron.forceSetActivation(0);
            sim.iterate(220);
        });

        // TODO: Factor the velocity settings in to another method
        // also need to reset velocities to 0 after pressing this
        panel.addButton("Solar System", () -> {
            net.getNetwork().clearActivations();
            cheese.setVelocityX(2.05f);
            cheese.setVelocityY(2.05f);
            flower.setVelocityX(2.5f);
            flower.setVelocityY(2.1f);
            fish.setVelocityX(-2.5f);
            fish.setVelocityY(1.05f);
            mouse.setLocation(cheeseX, cheeseY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(0);
            sim.iterate(200);
            cheese.setVelocityX(0);
            cheese.setVelocityY(0);
            flower.setVelocityX(0);
            flower.setVelocityY(0);
            fish.setVelocityX(0);
            fish.setVelocityY(0);
        });

//        // Save File
//        panel.addButton("Save", () -> {
//            try {
//                Files.write(csvFile, activationList);
//            } catch (IOException ioe) {
//                ioe.printStackTrace();
//            }
//        });

    }

    @Override
    public String getName() {
        return "Agent Trails";
    }

    @Override
    public AgentTrails instantiate(SimbrainDesktop desktop) {
        return new AgentTrails(desktop);
    }

}
