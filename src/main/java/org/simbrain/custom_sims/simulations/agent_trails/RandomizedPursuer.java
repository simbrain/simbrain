package org.simbrain.custom_sims.simulations.agent_trails;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetworkWrapper;
import org.simbrain.custom_sims.helper_classes.OdorWorldWrapper;
import org.simbrain.custom_sims.helper_classes.Vehicle;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

import java.util.Arrays;

/**
 * Create a Braitenberg pursuer with a projection plot to sensor neurons.
 * Button to run it from randomized spots to create a set a cognitive map
 * of its input space.
 */
public class RandomizedPursuer extends RegisteredSimulation {

    NetworkWrapper networkWrapper;
    OdorWorldEntity mouse;
    OdorWorldEntity cheese, flower, fish;
    ControlPanel panel;
    NeuronGroup vehicleNetwork;
    NeuronCollection sensorNodes, motorNodes;

    ProjectionComponent plot;
    OdorWorldWrapper worldBuilder;

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

        // Set up Plot
        setUpPlot();

    }

    private void createOdorWorld() {

        worldBuilder = sim.addOdorWorldTMX(629, 9, "empty.tmx");
        worldBuilder.getWorld().setObjectsBlockMovement(false);

        mouse = worldBuilder.addEntity(204, 343, EntityType.MOUSE);
        mouse.setHeading(90);
        mouse.addLeftRightSensors(EntityType.SWISS, 200);
        mouse.addDefaultEffectors();

        cheese = worldBuilder.addEntity(cheeseX, cheeseY, EntityType.SWISS, new double[]{1, 0, 0});

        worldBuilder.getWorld().update();

    }

    private void buildNetwork() {
        networkWrapper = sim.addNetwork(195, 9, 447, 296, "Pursuer");
        Network net = networkWrapper.getNetwork();
        Vehicle pursuer = new Vehicle(sim, networkWrapper, worldBuilder);
        vehicleNetwork = pursuer.addPursuer(10, 10,
                mouse, EntityType.SWISS,
                (ObjectSensor) mouse.getSensors().get(0),
                (ObjectSensor)  mouse.getSensors().get(1));

        Neuron sensor1 = net.getNeuronByLabel("Swiss (L)");
        Neuron sensor2 = net.getNeuronByLabel("Swiss (R)");
        sensorNodes = new NeuronCollection(net, Arrays.asList(sensor1, sensor2));
        sensorNodes.setLabel("Sensor Nodes");
        net.addNeuronCollection(sensorNodes);
    }

    private void setUpPlot() {
        plot = sim.addProjectionPlot(194, 312, 441, 308, "Sensory states");
        plot.getProjector().setTolerance(.01);
        plot.getProjector().getColorManager().setColoringMethod("Bayesian");
        sim.couple(sensorNodes, plot);

    }

    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10);

        panel.addButton("Run", () -> {
            double height = worldBuilder.getWorld().getHeight();
            double width =  worldBuilder.getWorld().getWidth();
            for (int trial = 0; trial  < 5; trial ++) {
                mouse.randomizeLocation();
                sim.iterate(300);
            }
        });

    }

    @Override
    public String getSubmenuName() {
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
