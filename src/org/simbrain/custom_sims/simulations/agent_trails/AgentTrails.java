package org.simbrain.custom_sims.simulations.agent_trails;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.helper_classes.PlotBuilder;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Create images and data used in this paper
 * https://mindmodeling.org/cogsci2014/papers/542/paper542.pdf
 */
public class AgentTrails extends RegisteredSimulation {

    NetBuilder netBuilder;
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
    int dispersion = 100;
    int fishX = 50;
    int fishY = 100;
    int flowerX = 330;
    int flowerY = 100;
    int cheeseX = 200;
    int cheeseY = 250;

    public AgentTrails() {
        super();
    }

    /**
     * @param desktop
     */
    public AgentTrails(SimbrainDesktop desktop) {
        super(desktop);
    }

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build the network
        buildNetwork();

        // Create the odor world
        createOdorWorld();

        // Set up control panel
        setUpControlPanel();

        // Set up Plot
        setUpPlot();

        // Uncomment lines below to Log activations, as well as save file below
        // csvFile = Paths.get("agentTrails.csv");
        // net.getNetwork().addUpdateAction(new LogActivations(this));

    }


    private void buildNetwork() {
        netBuilder = sim.addNetwork(195, 9, 447, 296, "Simple Predicter");
        sensoryNet = netBuilder.addNeuronGroup(-9.25, 95.93, 3);
        //sensoryNet.setClamped(true);
        sensoryNet.setLabel("Sensory");
        cheeseNeuron = sensoryNet.getNeuronList().get(0);
        cheeseNeuron.setLabel("Cheese");
        flowerNeuron = sensoryNet.getNeuronList().get(1);
        flowerNeuron.setLabel("Flower");
        fishNeuron = sensoryNet.getNeuronList().get(2);
        fishNeuron.setLabel("Fish");

        actionNet = netBuilder.addNeuronGroup(0, -0.79, 3);
        actionNet.setLabel("Actions");
        actionNet.setClamped(true);
        actionNet.setLabel("Actions");

        straightNeuron = actionNet.getNeuronList().get(1);
        straightNeuron.setLabel("Straight");
        rightNeuron = actionNet.getNeuronList().get(2);
        rightNeuron.setLabel("Right");
        leftNeuron = actionNet.getNeuronList().get(0);
        leftNeuron.setLabel("Left");

        predictionNet = netBuilder.addNeuronGroup(231.02, 24.74, 3);
        predictionNet.setLabel("Predicted");

        netBuilder.connectAllToAll(sensoryNet, predictionNet);
        netBuilder.connectAllToAll(actionNet, predictionNet);

        errorNeuron = netBuilder.addNeuron(268, 108);
        //errorNeuron.setClamped(true);
        errorNeuron.setLabel("Error");

        netBuilder.getNetwork().addUpdateAction(new TrainPredictionNet(this));

    }

    private void createOdorWorld() {

        worldBuilder = sim.addOdorWorldTMX(629, 9, "empty.tmx");
        worldBuilder.getWorld().setObjectsBlockMovement(false);

        mouse = worldBuilder.addAgent(204, 343, "Mouse");
        mouse.setHeading(90);
        mouse.addDefaultSensorsEffectors();
        mouse.setManualStraightMovementIncrement(2);
        mouse.setManualMotionTurnIncrement(2);

        cheese = worldBuilder.addEntity(cheeseX, cheeseY, EntityType.SWISS, new double[]{1, 0, 0});
        cheese.getSmellSource().setDispersion(dispersion);
        flower = worldBuilder.addEntity(flowerX, flowerY, EntityType.FLOWER, new double[]{0, 1, 0});
        flower.getSmellSource().setDispersion(dispersion);
        fish = worldBuilder.addEntity(fishX, fishY, EntityType.FISH, new double[]{0, 0, 1});
        fish.getSmellSource().setDispersion(dispersion);
        worldBuilder.getWorld().update();

        // Couple network to agent
        sim.couple(straightNeuron, mouse.getEffector("Go-straight"));
        sim.couple(rightNeuron, mouse.getEffector("Go-left"));
        sim.couple(leftNeuron, mouse.getEffector("Go-right"));

        // Couple agent to network
        sim.couple((SmellSensor) mouse.getSensor("Smell-Center"),sensoryNet);
    }

    private void setUpPlot() {
        plot = sim.addProjectionPlot(194, 312, 441, 308, "Sensory states + Predictions");
        plot.getProjectionModel().getProjector().setTolerance(.001);
        sim.couple(netBuilder.getNetworkComponent(), sensoryNet, plot.getProjectionPlotComponent());

        // Uncomment for prediction halo
        plot.getProjectionModel().getProjector().setUseColorManager(false);
        sim.getWorkspace().addUpdateAction(new ColorPlot(this));

        // Label PCA points based on closest object
        // (how nice this looks depends on tolerance. if tolerance too low then too many labels are created)
        // Producer currentObject = sim.getProducer(mouse, "getNearbyObjects");
        // Consumer plotText = sim.getConsumer(plot.getProjectionPlotComponent(), "setLabel");
        // sim.tryCoupling(currentObject, plotText);
    }

    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10);

        // TODO: Finish fine-tuning all these values so that appropriate "trails" are created
        // Move past cheese
        panel.addButton("Cheese", () -> {
            netBuilder.getNetwork().clearActivations();
            mouse.setLocation(cheeseX, cheeseY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(dispersion*2);
            straightNeuron.forceSetActivation(0);
            netBuilder.getNetwork().fireNeuronsUpdated();
        });

        // Move past Fish
        panel.addButton("Fish", () -> {
            netBuilder.getNetwork().clearActivations();
            mouse.setLocation(fishX, fishY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(dispersion*2);
            straightNeuron.forceSetActivation(0);
            netBuilder.getNetwork().fireNeuronsUpdated();
        });

        // Move past flower
        panel.addButton("Flower", () -> {
            netBuilder.getNetwork().clearActivations();
            mouse.setLocation(flowerX, flowerY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(dispersion*2);
            straightNeuron.forceSetActivation(0);
            netBuilder.getNetwork().fireNeuronsUpdated();
        });

        // Cheese > Fish
        panel.addButton("Cheese > Flower", () -> {
            netBuilder.getNetwork().clearActivations();
            mouse.setLocation(cheeseX, cheeseY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(50);
            rightNeuron.forceSetActivation(1.5);
            sim.iterate(25);
            rightNeuron.forceSetActivation(0);
            sim.iterate(220);
            straightNeuron.forceSetActivation(0);
            netBuilder.getNetwork().fireNeuronsUpdated();
        });

        // Cheese > Flower
        panel.addButton("Cheese > Fish", () -> {
            netBuilder.getNetwork().clearActivations();
            mouse.setLocation(cheeseX, cheeseY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(50);
            leftNeuron.forceSetActivation(1.5);
            sim.iterate(25);
            leftNeuron.forceSetActivation(0);
            sim.iterate(220);
            straightNeuron.forceSetActivation(0);
            netBuilder.getNetwork().fireNeuronsUpdated();
        });

        panel.addButton("Solar System", () -> {
            netBuilder.getNetwork().clearActivations();
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
            netBuilder.getNetwork().fireNeuronsUpdated();
        });

      //// Save File
      //panel.addButton("Save", () -> {
      //    try {
      //        Files.write(csvFile, activationList);
      //    } catch (IOException ioe) {
      //        ioe.printStackTrace();
      //    }
      //});

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
