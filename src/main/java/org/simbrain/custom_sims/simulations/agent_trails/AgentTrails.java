package org.simbrain.custom_sims.simulations.agent_trails;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.simulations.utils.ColorPlotKt;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.simbrain.network.core.NetworkUtilsKt.connectAllToAll;

/**
 * Create images and data used in this paper
 * https://mindmodeling.org/cogsci2014/papers/542/paper542.pdf
 */
public class AgentTrails extends Simulation {

    NetworkComponent nc;
    OdorWorldEntity mouse;
    OdorWorldEntity cheese, flower, fish;
    ControlPanel panel;
    NeuronGroup sensoryNet, actionNet, predictionNet;
    Neuron leftNeuron, straightNeuron, rightNeuron;
    Neuron cheeseNeuron, flowerNeuron, fishNeuron;
    Neuron errorNeuron;
    Path csvFile;
    List<String> activationList = new ArrayList<String>();
    ProjectionComponent plot;
    OdorWorldComponent oc;

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
        nc = sim.addNetwork(195, 9, 447, 296, "Simple Predicter");
        sensoryNet = nc.getNetwork().addNeuronGroup(-9.25, 95.93, 3);
        //sensoryNet.setClamped(true);
        sensoryNet.setLabel("Sensory");
        cheeseNeuron = sensoryNet.getNeuronList().get(0);
        cheeseNeuron.setLabel("Cheese");
        flowerNeuron = sensoryNet.getNeuronList().get(1);
        flowerNeuron.setLabel("Flower");
        fishNeuron = sensoryNet.getNeuronList().get(2);
        fishNeuron.setLabel("Fish");

        actionNet = nc.getNetwork().addNeuronGroup(0, -0.79, 3);
        actionNet.setLabel("Actions");
        actionNet.setClamped(true);
        actionNet.setLabel("Actions");

        straightNeuron = actionNet.getNeuronList().get(1);
        straightNeuron.setLabel("Straight");
        rightNeuron = actionNet.getNeuronList().get(2);
        rightNeuron.setLabel("Right");
        leftNeuron = actionNet.getNeuronList().get(0);
        leftNeuron.setLabel("Left");

        predictionNet = nc.getNetwork().addNeuronGroup(231.02, 24.74, 3);
        predictionNet.setLabel("Predicted");

        connectAllToAll(sensoryNet, predictionNet);
        connectAllToAll(actionNet, predictionNet);

        errorNeuron = nc.getNetwork().addNeuron(268, 108);
        //errorNeuron.setClamped(true);
        errorNeuron.setLabel("Error");

        nc.getNetwork().addUpdateAction(new TrainPredictionNet(this));

    }

    private void createOdorWorld() {

        oc = sim.addOdorWorld(629, 9, 441, 508, "World");
        oc.getWorld().setObjectsBlockMovement(false);
        oc.getWorld().setUseCameraCentering(false);

        mouse = oc.getWorld().addEntity(204, 343, EntityType.MOUSE);
        mouse.setHeading(90);
        mouse.addDefaultSensorsEffectors();
        SmellSensor smellSensor = new SmellSensor();
        mouse.addSensor(smellSensor);
        mouse.getManualMovement().setManualStraightMovementIncrement(2);
        mouse.getManualMovement().setManualMotionTurnIncrement(2);

        cheese = oc.getWorld().addEntity(cheeseX, cheeseY, EntityType.SWISS, new double[]{1, 0, 0});
        cheese.getSmellSource().setDispersion(dispersion);
        flower = oc.getWorld().addEntity(flowerX, flowerY, EntityType.FLOWER, new double[]{0, 1, 0});
        flower.getSmellSource().setDispersion(dispersion);
        fish = oc.getWorld().addEntity(fishX, fishY, EntityType.FISH, new double[]{0, 0, 1});
        fish.getSmellSource().setDispersion(dispersion);
        oc.getWorld().update();

        // Couple network to agent
        // TODO: Labelling has changed; fix below
        sim.couple(straightNeuron, mouse.getEffector("Move straight"));
        sim.couple(rightNeuron, mouse.getEffector("Turn left"));
        sim.couple(leftNeuron, mouse.getEffector("Turn right"));

        // Couple agent to network
        sim.couple(smellSensor,sensoryNet);
    }

    private void setUpPlot() {
        plot = sim.addProjectionPlot(194, 312, 441, 308, "Sensory states + Predictions");
        plot.getProjector().setTolerance(.001);
        sim.couple(sensoryNet, plot);

        // Uncomment for prediction halo
        plot.getProjector().setUseColorManager(false);
        sim.getWorkspace().addUpdateAction(ColorPlotKt.createColorPlotUpdateAction(
                plot.getProjector(),
                predictionNet,
                errorNeuron.getActivation()
        ));

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
            nc.getNetwork().clearActivations();
            mouse.setLocation(cheeseX, cheeseY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(dispersion*2);
            straightNeuron.forceSetActivation(0);
        });

        // Move past Fish
        panel.addButton("Fish", () -> {
            nc.getNetwork().clearActivations();
            mouse.setLocation(fishX, fishY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(dispersion*2);
            straightNeuron.forceSetActivation(0);
//            networkWrapper.getNetwork().fireNeuronsUpdated(); // TODO: [event]
        });

        // Move past flower
        panel.addButton("Flower", () -> {
            nc.getNetwork().clearActivations();
            mouse.setLocation(flowerX, flowerY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(dispersion*2);
            straightNeuron.forceSetActivation(0);
//            networkWrapper.getNetwork().fireNeuronsUpdated(); // TODO: [event]
        });

        // Cheese > Fish
        panel.addButton("Cheese > Flower", () -> {
            nc.getNetwork().clearActivations();
            mouse.setLocation(cheeseX, cheeseY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(50);
            rightNeuron.forceSetActivation(1.5);
            sim.iterate(25);
            rightNeuron.forceSetActivation(0);
            sim.iterate(220);
            straightNeuron.forceSetActivation(0);
//            networkWrapper.getNetwork().fireNeuronsUpdated(); // TODO: [event]
        });

        // Cheese > Flower
        panel.addButton("Cheese > Fish", () -> {
            nc.getNetwork().clearActivations();
            mouse.setLocation(cheeseX, cheeseY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(1);
            sim.iterate(50);
            leftNeuron.forceSetActivation(1.5);
            sim.iterate(25);
            leftNeuron.forceSetActivation(0);
            sim.iterate(220);
            straightNeuron.forceSetActivation(0);
//            networkWrapper.getNetwork().fireNeuronsUpdated(); // TODO: [event]
        });

        panel.addButton("Solar System", () -> {
            nc.getNetwork().clearActivations();
            // TODO: use polar
            // cheese.setVelocityX(2.05f);
            // cheese.setVelocityY(2.05f);
            // flower.setVelocityX(2.5f);
            // flower.setVelocityY(2.1f);
            // fish.setVelocityX(-2.5f);
            // fish.setVelocityY(1.05f);
            mouse.setLocation(cheeseX, cheeseY + dispersion);
            mouse.setHeading(90);
            straightNeuron.forceSetActivation(0);
            sim.iterate(200);
            // cheese.setVelocityX(0);
            // cheese.setVelocityY(0);
            // flower.setVelocityX(0);
            // flower.setVelocityY(0);
            // fish.setVelocityX(0);
            // fish.setVelocityY(0);
//            networkWrapper.getNetwork().fireNeuronsUpdated(); // TODO: [event]
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

    private String getSubmenuName() {
        return "Cognitive Maps";
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
