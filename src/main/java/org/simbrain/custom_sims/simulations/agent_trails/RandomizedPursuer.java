package org.simbrain.custom_sims.simulations.agent_trails;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetworkWrapper;
import org.simbrain.custom_sims.helper_classes.OdorWorldWrapper;
import org.simbrain.custom_sims.helper_classes.Vehicle;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Create images and data used in this paper
 * https://mindmodeling.org/cogsci2014/papers/542/paper542.pdf
 */
public class RandomizedPursuer extends RegisteredSimulation {

    NetworkWrapper networkWrapper;
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
    OdorWorldWrapper worldBuilder;

    // Default values for these used by buttons
    int dispersion = 100;
    int fishX = 50;
    int fishY = 100;
    int flowerX = 330;
    int flowerY = 100;
    int cheeseX = 200;
    int cheeseY = 250;

    public RandomizedPursuer() {
        super();
    }

    /**
     * @param desktop
     */
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

        // Set up Plot
        //setUpPlot();

        // Uncomment lines below to Log activations, as well as save file below
        // csvFile = Paths.get("agentTrails.csv");
        // net.getNetwork().addUpdateAction(new LogActivations(this));

    }

    private void createOdorWorld() {

        worldBuilder = sim.addOdorWorldTMX(629, 9, "empty.tmx");
        worldBuilder.getWorld().setObjectsBlockMovement(false);

        mouse = worldBuilder.addEntity(204, 343, EntityType.MOUSE);
        mouse.setHeading(90);
        mouse.addLeftRightSensors(EntityType.SWISS, 200);
        mouse.addDefaultEffectors();
        //SmellSensor smellSensor = new SmellSensor(mouse);
        //mouse.addSensor(smellSensor);
        //mouse.setManualStraightMovementIncrement(2);
        //mouse.setManualMotionTurnIncrement(2);

        cheese = worldBuilder.addEntity(cheeseX, cheeseY, EntityType.SWISS, new double[]{1, 0, 0});
        cheese.getSmellSource().setDispersion(dispersion);
        flower = worldBuilder.addEntity(flowerX, flowerY, EntityType.FLOWER, new double[]{0, 1, 0});
        flower.getSmellSource().setDispersion(dispersion);
        fish = worldBuilder.addEntity(fishX, fishY, EntityType.FISH, new double[]{0, 0, 1});
        fish.getSmellSource().setDispersion(dispersion);
        worldBuilder.getWorld().update();

        //// Couple network to agent
        //sim.couple(straightNeuron, mouse.getEffector("Move straight"));
        //sim.couple(rightNeuron, mouse.getEffector("Turn left"));
        //sim.couple(leftNeuron, mouse.getEffector("Turn right"));
        //
        //// Couple agent to network
        //sim.couple(smellSensor,sensoryNet);
    }



    private void buildNetwork() {
        networkWrapper = sim.addNetwork(195, 9, 447, 296, "Pursuer");
        Vehicle pursuer = new Vehicle(sim, networkWrapper, worldBuilder);
        pursuer.addPursuer(10, 10,
                mouse, EntityType.SWISS,
                (ObjectSensor) mouse.getSensors().get(0),
                (ObjectSensor)  mouse.getSensors().get(1));


    }


    private void setUpPlot() {
        plot = sim.addProjectionPlot(194, 312, 441, 308, "Sensory states + Predictions");
        plot.getProjector().setTolerance(.001);
        sim.couple(networkWrapper.getNetworkComponent(), sensoryNet, plot);

        //// Uncomment for prediction halo
        //plot.getProjectionModel().getProjector().setUseColorManager(false);
        //sim.getWorkspace().addUpdateAction(new ColorPlot(this));
        //
        //// Label PCA points based on closest object
        //// (how nice this looks depends on tolerance. if tolerance too low then too many labels are created)
        //// Producer currentObject = sim.getProducer(mouse, "getNearbyObjects");
        //// Consumer plotText = sim.getConsumer(plot.getProjectionPlotComponent(), "setLabel");
        //// sim.tryCoupling(currentObject, plotText);
    }

    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10);

        panel.addButton("Run", () -> {
            double height = worldBuilder.getWorld().getHeight();
            double width =  worldBuilder.getWorld().getWidth();
            for (int trial = 0; trial  < 5; trial ++) {
                mouse.setLocation(SimbrainRandomizer.rand.nextDouble(0.0, height),
                        SimbrainRandomizer.rand.nextDouble(0.0, width));
                sim.iterate(300);
            }
        });

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
