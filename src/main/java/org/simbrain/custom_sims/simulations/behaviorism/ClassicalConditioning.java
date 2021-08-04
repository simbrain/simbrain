package org.simbrain.custom_sims.simulations.behaviorism;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorldComponent;
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
    NetworkComponent nc;
    Network net;
    ControlPanel panel;

    // World stuff
    OdorWorldComponent oc;
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
        nc = sim.addNetwork(0,14,350,443, "Agent Brain (Black Box)");
        net = nc.getNetwork();

        // Construct the network
        Neuron bellDetectorNeuron = new Neuron(net);
        bellDetectorNeuron.setLabel("Bell Detector");
        bellDetectorNeuron.setClamped(true);
        net.addNetworkModel(bellDetectorNeuron);
        bellDetectorNeuron.setLocation(295, 194);

        Neuron cheeseDetectorNeuron = new Neuron(net);
        cheeseDetectorNeuron.setLabel("Cheese Detector");
        cheeseDetectorNeuron.setClamped(false);
        net.addNetworkModel(cheeseDetectorNeuron);
        cheeseDetectorNeuron.setLocation(160, 194);

        BinaryRule responseRule = new BinaryRule();
        responseRule.setThreshold(.5);
        responseRule.setLowerBound(0);
        Neuron salivationResponse = new Neuron(net, responseRule);
        salivationResponse.setLabel("Salivation");
        net.addNetworkModel(salivationResponse);
        salivationResponse.setLocation(160, 60);

        Synapse cheeseToSalivation = new Synapse(cheeseDetectorNeuron, salivationResponse,1);
        cheeseToSalivation.setUpperBound(1);
        net.addNetworkModel(cheeseToSalivation);

        Synapse association = new Synapse(bellDetectorNeuron, cheeseDetectorNeuron);
        association.setStrength(0);
        association.setLowerBound(0);
        association.setUpperBound(1);

        net.addNetworkModel(association);
        // TODO
        // networkWrapper.getNetworkPanel().getSelectionManager().clear(); // todo: why needed?

        // Create the odor world
        oc = sim.addOdorWorld(351,13,377,444, "Environment");
        oc.getWorld().setObjectsBlockMovement(false);
        mouse = oc.getWorld().addEntity(125, 211, EntityType.MOUSE);
        mouse.setHeading(90);

        // Set up world
        cheese = oc.getWorld().addEntity(13, 67, EntityType.SWISS);
        bell = oc.getWorld().addEntity(234, 67, EntityType.BELL);

        // Set up object sensors
        ObjectSensor swissSensor = mouse.addObjectSensor(EntityType.SWISS, 50, 0, 65);
        ObjectSensor bellSensor = mouse.addObjectSensor(EntityType.BELL, 50, 0, 65);

        // Couple agent to network
        sim.couple(swissSensor, cheeseDetectorNeuron);
        sim.couple(bellSensor, bellDetectorNeuron);

        // Create a time series plot
        plot = sim.addTimeSeries(728,13,406,444, "Association Strength");
        plot.getModel().removeAllScalarTimeSeries();
        plot.getModel().setAutoRange(false);
        plot.getModel().setFixedWidth(true);
        plot.getModel().setWindowSize(1500);
        TimeSeriesModel.ScalarTimeSeries ts1 = plot.getModel().addScalarTimeSeries("Association Strength");
        sim.couple(association, ts1);

        // Add custom network update action
        nc.getNetwork().addUpdateAction(new NetworkUpdateAction() {

            @Override
            public void invoke() {

                if ((bellDetectorNeuron.getActivation() > 0) && (cheeseDetectorNeuron.getActivation() > 0)) {
                    // Learning
                    association.setIncrement(.001); // learning rate
                    association.increment();
                } else if ((bellDetectorNeuron.getActivation() > 0) && (cheeseDetectorNeuron.getActivation() <= 0)) {
                    // Extinction
                    association.setIncrement(.0005); // extinction rate
                    association.decrement();
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
