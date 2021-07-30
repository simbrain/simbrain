package org.simbrain.custom_sims.simulations.hippocampus;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

/**
 * Simulation of the Squire Alvarez Hippocampus model (PNAS, 1994). TODO: This
 * model illustrates serious issues with synapse group arrow rendering
 *
 * @author Jeff Yoshimi
 * @author Jeff Rodny
 * @author Alex Pabst
 */
// CHECKSTYLE:OFF
public class Hippocampus extends RegisteredSimulation {

    /**
     * Randomizer for creating new synapse groups.
     */
    ProbabilityDistribution exRand =
            UniformDistribution.builder()
                    .polarity(Polarity.EXCITATORY)
                    .build();

    /**
     * Other variables.
     */
    NetworkComponent nc;
    Network net;
    ControlPanel panel;
    boolean hippoLesioned = false;
    boolean learningEnabled = true;
    JLabel errorLabel;
    JLabel perfLabel;
    JTextField conslidationField;

    /**
     * References to main neuron and synapse groups.
     */
    AlvarezSquire LC1, LC2, RC1, RC2, hippocampus;
    SynapseGroup HtoLC1, HtoLC2, HtoRC1, HtoRC2, LC1toH, LC2toH, RC1toH, RC2toH;

    /**
     * The four main network patterns.
     */
    double[] pattern1 = new double[] {1, 0, 0, 0};
    double[] pattern2 = new double[] {0, 1, 0, 0};
    double[] pattern3 = new double[] {0, 0, 1, 0};
    double[] pattern4 = new double[] {0, 0, 0, 1};

    public Hippocampus() {
        super();
    }

    /**
     * @param desktop
     */
    public Hippocampus(SimbrainDesktop desktop) {
        super(desktop);
        exRand.setUpperBound(0.02); // Set up randomizer
    }

    /**
     * Run the simulation!
     */
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build the network
        buildNetwork();

        // Set up control panel
        setUpControlPanel();

        // Add docviewer
        sim.addDocViewer(807, 12, 307, 591, "Information",
            "Hippocampus.html");

    }

    /**
     * Build hippocampal model.
     */
    private void buildNetwork() {

        // Set network variables
        nc = sim.addNetwork(193, 12, 632, 585, "Hippocampus");
        net = nc.getNetwork();

        // Add all neuron groups
        LC1 = addCompetitiveGroup("Left Cortex Top", 185, -114);
        LC2 = addCompetitiveGroup("Left Cortex Bottom", 185, -28);
        RC1 = addCompetitiveGroup("Right Cortex Top", 597, -114);
        RC2 = addCompetitiveGroup("Right Cortex Bottom", 597, -28);
        hippocampus = addCompetitiveGroup("Hippocampus", 368, 179);

        // Cortex to MTL connections
        HtoLC1 = addSynapseGroup(hippocampus, LC1, "H to LC1");
        HtoLC2 = addSynapseGroup(hippocampus, LC2, "H to LC2");
        HtoRC1 = addSynapseGroup(hippocampus, RC1, "H to RC1");
        HtoRC2 = addSynapseGroup(hippocampus, RC2, "H to RC2");
        LC1toH = addSynapseGroup(LC1, hippocampus, "LC1 to H");
        LC2toH = addSynapseGroup(LC2, hippocampus, "LC2 to H");
        RC1toH = addSynapseGroup(RC1, hippocampus, "RC1 to H");
        RC2toH = addSynapseGroup(RC2, hippocampus, "RC2 to H");
        // Cortico-corticol connections
        addSynapseGroup(LC1, RC1, "LC1 to RC1");
        addSynapseGroup(LC1, RC2, "LC1 to RC2");
        addSynapseGroup(LC2, RC1, "LC2 to RC1");
        addSynapseGroup(LC2, RC2, "LC2 to RC2");
        addSynapseGroup(RC1, LC1, "RC1 to LC1");
        addSynapseGroup(RC1, LC2, "RC1 to LC2");
        addSynapseGroup(RC2, LC1, "RC2 to LC1");
        addSynapseGroup(RC2, LC2, "RC2 to LC2");

        // Initialize the synapses
        initWeights();

    }

    /**
     * Add and properly initialize a competitive neuron group.
     */
    private AlvarezSquire addCompetitiveGroup(String label, double x,
                                              double y) {
        AlvarezSquire cg = new AlvarezSquire(this, 4);
        cg.setLabel(label);
        cg.setLayout(new LineLayout());
        cg.applyLayout();
        cg.setUpdateMethod("AS");
        net.addNetworkModel(cg);
        cg.setLocation(x, y);
        return cg;
    }

    /**
     * Add and properly initialize a synapse group.
     */
    private SynapseGroup addSynapseGroup(NeuronGroup source, NeuronGroup target,
                                         String name) {

        // Initialize with uniform distribution from 0 to .1
        SynapseGroup synGroup = SynapseGroup.createSynapseGroup(source, target,
               new AllToAll());
        synGroup.setLabel(name);
        // TODO: Weight matrices?
        // synGroup.setLowerBound(0, Polarity.EXCITATORY);
        // synGroup.setUpperBound(1, Polarity.EXCITATORY);
        net.addNetworkModel(synGroup);
        return synGroup;
    }

    /**
     * Set up the controls.
     */
    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10);
        Random generator = new Random();

        // Show pattern one
        panel.addButton("Train All", () -> {
            train(net, pattern1);
            train(net, pattern2);
            train(net, pattern3);
            train(net, pattern4);
            train(net, pattern1);
            train(net, pattern2);
            train(net, pattern3);
            train(net, pattern4);
        });

        // Test all the patterns
        panel.addButton("Test All", () -> {
            double error = 0;
            test(net, pattern1);
            error += getError(pattern1);
            test(net, pattern2);
            error += getError(pattern2);
            test(net, pattern3);
            error += getError(pattern3);
            test(net, pattern4);
            error += getError(pattern4);
            error = SimbrainMath.roundDouble(error, 2);
            errorLabel.setText("" + error);
            double perf = SimbrainMath.roundDouble(8 - error, 2);
            perfLabel.setText("" + (perf > 0 ? perf : 0));
        });

        // Reset weights
        panel.addButton("Reset weights", () -> {
            initWeights();
        });

        panel.addSeparator();

        // Show pattern one
        panel.addButton("Test 1", () -> {
            test(net, pattern1);
        });

        // Show pattern two
        panel.addButton("Test 2", () -> {
            test(net, pattern2);
        });

        // Show pattern three
        panel.addButton("Test 3", () -> {
            test(net, pattern3);
        });

        // Show pattern four
        panel.addButton("Test 4", () -> {
            test(net, pattern4);
        });

        panel.addSeparator();

        ControlPanel bottomPanel = new ControlPanel();
        errorLabel = bottomPanel.addLabel("Error:", "");
        perfLabel = bottomPanel.addLabel("Performance:", "");

        // Lesion checkbox
        bottomPanel.addCheckBox("Lesion MTL", hippoLesioned, () -> {
            if (hippoLesioned == true) {
                hippoLesioned = false;
            } else {
                hippoLesioned = true;
            }
            enableHippocampus(hippoLesioned);
        });

        // Freeze weights checkbox
        // bottomPanel.addCheckBox("Learning", learningEnabled, () -> {
        // if (learningEnabled == true) {
        // learningEnabled = false;
        // } else {
        // learningEnabled = true;
        // }
        // network.freezeSynapses(!learningEnabled);
        // });

        // Consolidate
        JPanel consolidationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        JButton consolidationButton = new JButton("Consolidate");
        consolidationPanel.add(consolidationButton);
        conslidationField = new JTextField();
        conslidationField.setColumns(2);
        conslidationField.setText("100");
        consolidationPanel.add(conslidationField);
        consolidationButton.addActionListener(e -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                double[] activations = new double[4];
                int actNeuron = generator.nextInt(4);
                if (actNeuron == 0) {
                    activations = pattern1;
                } else if (actNeuron == 1) {
                    activations = pattern2;
                } else if (actNeuron == 2) {
                    activations = pattern3;
                } else if (actNeuron == 3) {
                    activations = pattern4;
                }
                net.clearActivations();
                hippocampus.setClamped(true);
                hippocampus.forceSetActivations(activations);
                sim.iterate(Integer.parseInt(conslidationField.getText()));
                hippocampus.setClamped(false);
            });
        });
        panel.addComponent(consolidationPanel);

        panel.addBottomComponent(bottomPanel);

    }

    /**
     * Initialize weights randomly and uniformly between 0 and .02.
     */
    private void initWeights() {
        List<Synapse> synapses = net.getFlatSynapseList();
        for (Synapse synapse : synapses) {
            synapse.setStrength(.2 * Math.random());
        }
//        network.fireSynapsesUpdated(synapses); // TODO: [event]
    }

    /**
     * Returns error  for a specific pattern
     *
     * @pattern the pattern to check all cortical areas against
     */
    public double getError(double[] pattern) {
        double retVal = 0;
        for (int i = 0; i < 4; i++) {
            double diff = LC1.getNeuronList().get(i).getActivation() - pattern[i];
            retVal += diff * diff;
            diff = LC2.getNeuronList().get(i).getActivation() - pattern[i];
            retVal += diff * diff;
            diff = RC1.getNeuronList().get(i).getActivation() - pattern[i];
            retVal += diff * diff;
            diff = RC2.getNeuronList().get(i).getActivation() - pattern[i];
            retVal += diff * diff;
        }
        return retVal;
    }

    /**
     * Set up a train button that will clamp the the cortical neurons, then
     * train the network for three iterations, then unclamp the cortical
     * neurons.
     *
     * @param network
     * @param activations
     */
    void train(Network network, double[] activations) {
        // Clamp nodes and set activations
        LC1.setClamped(true);
        LC2.setClamped(true);
        RC1.setClamped(true);
        RC2.setClamped(true);
        LC1.forceSetActivations(activations);
        LC2.forceSetActivations(activations);
        RC1.forceSetActivations(activations);
        RC2.forceSetActivations(activations);
        hippocampus.forceSetActivations(new double[] {0, 0, 0, 0});

        // Iterate 3 times
        sim.iterate(3);

        // Unclamp nodes
        LC1.setClamped(false);
        LC2.setClamped(false);
        RC1.setClamped(false);
        RC2.setClamped(false);
    }

    /**
     * Set up a test button that will clamp half of the cortical neurons, then
     * runs the network for three iterations, then unclamp the cortical
     * neurons.
     *
     * @param network
     * @param activations
     */
    void test(Network network, double[] activations) {

        // Turn off learning during testing
        network.freezeSynapses(true);
        network.clearActivations();

        // Clamp nodes and set activations
        LC1.setClamped(true);
        LC2.setClamped(true);
        LC1.forceSetActivations(activations);
        LC2.forceSetActivations(activations);

        // Iterate 3 times
        sim.iterate(3);

        // Unclamp nodes
        LC1.setClamped(false);
        LC2.setClamped(false);

        // Turn on learning after testing
        network.freezeSynapses(false);

    }

    /**
     * Enable / disable the hippocampus
     */
    private void enableHippocampus(boolean lesioned) {
        // TODO Weight matrices
        // HtoLC1.setEnabled(!lesioned);
        // HtoLC2.setEnabled(!lesioned);
        // HtoRC1.setEnabled(!lesioned);
        // HtoRC2.setEnabled(!lesioned);
        // LC1toH.setEnabled(!lesioned);
        // LC2toH.setEnabled(!lesioned);
        // RC1toH.setEnabled(!lesioned);
        // RC2toH.setEnabled(!lesioned);
    }

    @Override
    public String getSubmenuName() {
        return "Brain";
    }

    @Override
    public String getName() {
        return "Hippocampus";
    }

    @Override
    public Hippocampus instantiate(SimbrainDesktop desktop) {
        return new Hippocampus(desktop);
    }

}
