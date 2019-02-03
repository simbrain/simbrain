package org.simbrain.custom_sims.simulations.behaviorism;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.helper_classes.PlotBuilder;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.network.synapse_update_rules.HebbianRule;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

/**
 * Simulation to demonstrate classical and operant conditioning.
 * Discriminative case
 *
 * @author Tim Meyer
 * @author Jeff Yoshimi
 */
public class ClassicalConditioning extends RegisteredSimulation {

    // TODO: Factor common methods out to a utility class in this directory

    // Network stuff
    NetBuilder netBuilder;
    Network network;
    ControlPanel panel;

    // World stuff
    OdorWorldBuilder world;
    RotatingEntity mouse;
    OdorWorldEntity cheese, bell;

    // Plot stuff
    PlotBuilder plot;

    public ClassicalConditioning() {
        super();
    }

    public ClassicalConditioning(SimbrainDesktop desktop) {
        super(desktop);
    }

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();
        netBuilder = sim.addNetwork(0,14,350,444, "Agent Brain (Black Box)");
        network = netBuilder.getNetwork();

        // Construct the network
        Neuron bellDetector = new Neuron(network);
        bellDetector.setX(295);
        bellDetector.setY(194);
        bellDetector.setLabel("Bell Detector");
        network.addNeuron(bellDetector);

        Neuron cheeseDetector = new Neuron(network);
        cheeseDetector.setX(160);
        cheeseDetector.setY(194);
        cheeseDetector.setLabel("Cheese Detector");
        network.addNeuron(cheeseDetector);

        BinaryRule responseRule = new BinaryRule();
        responseRule.setThreshold(.5);
        responseRule.setLowerBound(0);
        Neuron salivationResponse = new Neuron(network, responseRule);
        salivationResponse.setX(160);
        salivationResponse.setY(60);
        salivationResponse.setLabel("Salivation");
        network.addNeuron(salivationResponse);

        Synapse cheeseToSalivation = new Synapse(cheeseDetector, salivationResponse,1);
        cheeseToSalivation.setUpperBound(1);
        network.addSynapse(cheeseToSalivation);

        Synapse association = new Synapse(bellDetector, cheeseDetector);
        association.setStrength(.2);
        association.setLowerBound(0);
        association.setUpperBound(1);

        network.addSynapse(association);
        netBuilder.getNetworkPanel(sim).clearSelection(); // todo: why needed?

        // Create the odor world
        world = sim.addOdorWorld(340,13,324,447, "Environment");
        world.getWorld().setObjectsBlockMovement(false);
        mouse = world.addAgent(125, 211, "Mouse");
        mouse.setHeading(90);

        // Set up world
        cheese = world.addEntity(13, 67, "Swiss.gif",
            new double[] { 1, 0, 0 });
        cheese.getSmellSource().setDispersion(65);
        bell = world.addEntity(234, 67, "Bell.gif",
            new double[] { 0, 1, 0 });
        bell.getSmellSource().setDispersion(65);

        // Couple agent to network
        sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), 0,
            cheeseDetector, "setInputValue");
        sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), 1,
            bellDetector, "setInputValue");

        // Create a time series plot
        plot = sim.addTimeSeriesPlot(660,13,406,448, "Association Strength");
        Coupling rewardCoupling = sim.couple(netBuilder.getNetworkComponent(), association,
            plot.getTimeSeriesComponent(), 0);
        plot.getTimeSeriesModel().setAutoRange(false);
        plot.getTimeSeriesModel().setRangeUpperBound(1.1);
        plot.getTimeSeriesModel().setRangeLowerBound(-.1);

        // Add custom network update action
        netBuilder.getNetwork().addUpdateAction(new NetworkUpdateAction() {

            @Override
            public void invoke() {

                if ((bellDetector.getInputValue() > 0) && (cheeseDetector.getInputValue() > 0)) {
                    // Learning
                    association.setIncrement(.001); // learning rate
                    association.incrementWeight();
                } else if ((bellDetector.getInputValue() > 0) && (cheeseDetector.getInputValue() <= 0)) {
                    // Extinction
                    association.setIncrement(.0005); // exctinction rate
                    association.decrementWeight();
                }
            }

            @Override
            public String getDescription() {
                return "Custom behaviorism update";
            }

            @Override
            public String getLongDescription() {
                return "Custom behaviorism update";
            }
        });

    }

    @Override
    public String getName() {
        return "Behaviorism: Classical Conditioning";
    }

    @Override
    public ClassicalConditioning instantiate(SimbrainDesktop desktop) { return new ClassicalConditioning(desktop);
    }

}
