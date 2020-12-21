package org.simbrain.custom_sims.simulations.test;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.NetworkWrapper;
import org.simbrain.custom_sims.helper_classes.OdorWorldWrapper;
import org.simbrain.custom_sims.helper_classes.Simulation;
import org.simbrain.custom_sims.helper_classes.Vehicle;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.ProductRule;
import org.simbrain.network.neuron_update_rules.TimedAccumulatorRule;
import org.simbrain.plot.barchart.BarChartComponent;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistributions.LogNormalDistribution;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import org.simbrain.custom_sims.helper_classes.*;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.*;
import org.simbrain.network.groups.*;
import org.simbrain.network.connections.*;
import org.simbrain.network.desktop.*;
import org.simbrain.network.layouts.*;
import org.simbrain.network.neuron_update_rules.*;
import org.simbrain.network.synapse_update_rules.*;
import org.simbrain.workspace.*;
import org.simbrain.util.*;
import javax.swing.JInternalFrame;
import java.util.*;
import org.simbrain.world.odorworld.entities.*;

/**
 * Playground for testing new features. A lot of stuff is commented out but
 * should work.
 *
 * To add a sim, copy paste this, put in whatever menu you want it to be in, at {@link #getSubmenuName()}, and be
 * sure to register it at {@link RegisteredSimulation}.
 */
public class Braitenberg extends RegisteredSimulation {


    public Braitenberg() {
        super();
    }

    /**
     * @param desktop
     */
    public Braitenberg(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * Run the simulation!
     */
    public void run() {

        Workspace workspace = sim.getWorkspace();
        // Clear workspace
        workspace.clearWorkspace();
        SimbrainDesktop desktop = sim.getDesktop();
        Simulation sim = new Simulation(desktop);
        OdorWorldWrapper world = sim.addOdorWorld(610,3,496,646, "World");
        world.getWorld().setObjectsBlockMovement(false);
        OdorWorldEntity agent1 = world.addEntity(120, 245, EntityType.CIRCLE);
        OdorWorldEntity agent2 = world.addEntity(320, 245, EntityType.CIRCLE);
        agent1.addLeftRightSensors(EntityType.CIRCLE, 270);
        agent1.addDefaultEffectors();
        agent2.addLeftRightSensors(EntityType.CIRCLE, 270);
        agent2.addDefaultEffectors();

        NetworkWrapper vehicle1 = sim.addNetwork(260,5,359,342, "Vehicle 1");
        Vehicle vb1 = new Vehicle(sim, vehicle1, world);
        NeuronGroup ng1 = vb1.addPursuer(1, 1, agent1, EntityType.CIRCLE, (ObjectSensor)agent1.getSensors().get(0), (ObjectSensor) agent1.getSensors().get(1));

        NetworkWrapper vehicle2 = sim.addNetwork(259,329,361,321, "Vehicle 2");
        Vehicle vb2 = new Vehicle(sim, vehicle2, world);
        NeuronGroup ng2 = vb2.addPursuer(1, 1, agent2, EntityType.CIRCLE,
                (ObjectSensor)agent1.getSensors().get(0), (ObjectSensor)agent1.getSensors().get(1));


        // Make buttons
        JInternalFrame internalFrame = new JInternalFrame("Set weights", true, true);
        LabelledItemPanel panel = new LabelledItemPanel();

        JTextField weightl = new JTextField("100");
        panel.addItem("Left weight", weightl);
        JTextField weightr = new JTextField("50");
        panel.addItem("Right weight", weightr);

        // Pursuers
        JButton buttonPursuer = new JButton("Make");
        buttonPursuer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                ng1.setLabel("Pursuer");
                vehicle1.getNetwork().getLooseSynapse("Synapse_1").setStrength(1*Double.parseDouble(weightl.getText()));
                vehicle1.getNetwork().getLooseSynapse("Synapse_2").setStrength(1*Double.parseDouble(weightr.getText()));
                ng2.setLabel("Pursuer");
                vehicle2.getNetwork().getLooseSynapse("Synapse_2").setStrength(1*Double.parseDouble(weightl.getText()));
                vehicle2.getNetwork().getLooseSynapse("Synapse_1").setStrength(1*Double.parseDouble(weightr.getText()));
                workspace.iterate();
            }});
        panel.addItem("Pursuers", buttonPursuer);

        // Avoiders
        JButton buttonAvoider = new JButton("Make");
        buttonAvoider.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                ng1.setLabel("Avoider");
                vehicle1.getNetwork().getLooseSynapse("Synapse_1").setStrength(-1*Double.parseDouble(weightl.getText()));
                vehicle1.getNetwork().getLooseSynapse("Synapse_2").setStrength(-1*Double.parseDouble(weightr.getText()));
                ng2.setLabel("Avoider");
                vehicle2.getNetwork().getLooseSynapse("Synapse_2").setStrength(-1*Double.parseDouble(weightl.getText()));
                vehicle2.getNetwork().getLooseSynapse("Synapse_1").setStrength(-1*Double.parseDouble(weightr.getText()));
                workspace.iterate();

            }});
        panel.addItem("Avoiders", buttonAvoider);

        // Avoider-pursuer
        JButton buttonAvoiderPursuer = new JButton("Make");
        buttonAvoiderPursuer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                ng1.setLabel("Avoider");
                vehicle1.getNetwork().getLooseSynapse("Synapse_1").setStrength(-1*Double.parseDouble(weightl.getText()));
                vehicle1.getNetwork().getLooseSynapse("Synapse_2").setStrength(-1*Double.parseDouble(weightr.getText()));
                ng2.setLabel("Pursuer");
                vehicle2.getNetwork().getLooseSynapse("Synapse_2").setStrength(1*Double.parseDouble(weightl.getText()));
                vehicle2.getNetwork().getLooseSynapse("Synapse_1").setStrength(1*Double.parseDouble(weightr.getText()));
                workspace.iterate();
            }});
        panel.addItem("Avoider-Pursuer", buttonAvoiderPursuer);

        // Reverse weights
        JButton buttonReverseWeights = new JButton("Do it!");
        buttonReverseWeights.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                String label1 = ng1.getLabel();
                ng1.setLabel(ng2.getLabel());
                ng2.setLabel(label1);
                vehicle2.getNetwork().getLooseSynapse("Synapse_2").setStrength(vehicle1.getNetwork().getLooseSynapse("Synapse_2").getStrength());
                vehicle2.getNetwork().getLooseSynapse("Synapse_1").setStrength(vehicle1.getNetwork().getLooseSynapse("Synapse_1").getStrength());
                workspace.iterate();
            }});
        panel.addItem("Reverse weights", buttonReverseWeights);

        // Set up Frame
        internalFrame.setLocation(8,365);
        internalFrame.getContentPane().add(panel);
        internalFrame.setVisible(true);
        internalFrame.pack();
        desktop.addInternalFrame(internalFrame);
    }

    @Override
    public String getSubmenuName() {
        return "Braitenberg";
    }

    @Override
    public String getName() {
        return "Braitenberg";
    }

    @Override
    public Braitenberg instantiate(SimbrainDesktop desktop) {
        return new Braitenberg(desktop);
    }

}
