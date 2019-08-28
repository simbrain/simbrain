package org.simbrain.custom_sims.simulations.behaviorism;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetworkDesktopWrapper;
import org.simbrain.custom_sims.helper_classes.NetworkWrapper;
import org.simbrain.custom_sims.helper_classes.OdorWorldWrapper;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

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
    NetworkDesktopWrapper networkWrapper;
    Network network;
    ControlPanel panel;

    // World stuff
    OdorWorldWrapper world;
    OdorWorldEntity mouse;
    OdorWorldEntity cheese, bell;

    // Plot stuff
    TimeSeriesPlotComponent plot;

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
        networkWrapper = (NetworkDesktopWrapper)
                sim.addNetwork(0,14,350,444, "Agent Brain (Black Box)");
        network = networkWrapper.getNetwork();

        // Construct the network
        Neuron bellDetectorNeuron = new Neuron(network);
        bellDetectorNeuron.setX(295);
        bellDetectorNeuron.setY(194);
        bellDetectorNeuron.setLabel("Bell Detector");
        bellDetectorNeuron.setClamped(true);
        network.addLooseNeuron(bellDetectorNeuron);

        Neuron cheeseDetectorNeuron = new Neuron(network);
        cheeseDetectorNeuron.setX(160);
        cheeseDetectorNeuron.setY(194);
        cheeseDetectorNeuron.setLabel("Cheese Detector");
        cheeseDetectorNeuron.setClamped(false);
        network.addLooseNeuron(cheeseDetectorNeuron);

        BinaryRule responseRule = new BinaryRule();
        responseRule.setThreshold(.5);
        responseRule.setLowerBound(0);
        Neuron salivationResponse = new Neuron(network, responseRule);
        salivationResponse.setX(160);
        salivationResponse.setY(60);
        salivationResponse.setLabel("Salivation");
        network.addLooseNeuron(salivationResponse);

        Synapse cheeseToSalivation = new Synapse(cheeseDetectorNeuron, salivationResponse,1);
        cheeseToSalivation.setUpperBound(1);
        network.addLooseSynapse(cheeseToSalivation);

        Synapse association = new Synapse(bellDetectorNeuron, cheeseDetectorNeuron);
        association.setStrength(0);
        association.setLowerBound(0);
        association.setUpperBound(1);

        network.addLooseSynapse(association);
        networkWrapper.getNetworkPanel().clearSelection(); // todo: why needed?

        // Create the odor world
        world = sim.addOdorWorld(340,13,377,442, "Environment");
        world.getWorld().setObjectsBlockMovement(false);
        mouse = world.addEntity(125, 211, EntityType.MOUSE);
        mouse.setHeading(90);

        // Set up world
        cheese = world.addEntity(13, 67, EntityType.SWISS);
        bell = world.addEntity(234, 67, EntityType.BELL);

        // Set up object sensors
        ObjectSensor swissSensor = mouse.addObjectSensor(EntityType.SWISS, 50, 0, 65);
        ObjectSensor bellSensor = mouse.addObjectSensor(EntityType.BELL, 50, 0, 65);

        // Couple agent to network
        sim.couple(swissSensor, cheeseDetectorNeuron, false);
        sim.couple(bellSensor, bellDetectorNeuron);

        // Create a time series plot
        plot = sim.addTimeSeriesPlot(805,16,406,448, "Association Strength");
        plot.getModel().removeAllScalarTimeSeries();
        plot.getModel().setAutoRange(false);
        plot.getModel().setFixedWidth(true);
        plot.getModel().setWindowSize(1500);
        TimeSeriesModel.ScalarTimeSeries ts1 = plot.getModel().addScalarTimeSeries("Association Strength");
        sim.couple(association, ts1);

        // Add custom network update action
        networkWrapper.getNetwork().addUpdateAction(new NetworkUpdateAction() {

            @Override
            public void invoke() {

                if ((bellDetectorNeuron.getActivation() > 0) && (cheeseDetectorNeuron.getActivation() > 0)) {
                    // Learning
                    association.setIncrement(.001); // learning rate
                    association.incrementWeight();
                } else if ((bellDetectorNeuron.getActivation() > 0) && (cheeseDetectorNeuron.getActivation() <= 0)) {
                    // Extinction
                    association.setIncrement(.0005); // extinction rate
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
    public String getSubmenuName() {
        return "Behaviorism";
    }

    @Override
    public String getName() {
        return "Classical Conditioning";
    }

    @Override
    public ClassicalConditioning instantiate(SimbrainDesktop desktop) { return new ClassicalConditioning(desktop);
    }

}
