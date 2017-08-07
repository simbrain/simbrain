package org.simbrain.custom_sims.simulations.cerebellum;

import java.util.List;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.PlotBuilder;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkTextObject;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.neuron_update_rules.DecayRule;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 * TODO.
 */
// CHECKSTYLE:OFF
public class Cerebellum extends RegisteredSimulation {

    // TODO: Stop button

    /** Other variables. */
    NetBuilder net;
    Network network;
    ControlPanel panel;
    public int currentTrialLength = 200;
    public boolean toggleLearning = true;
    double eta = 0.01;
    double alpha = 2; // TODO: This should be based on a running average?
    double xi = 1;// Balances alpha with c (climbing fiber activity). TODO: Not
                  // sure what to set it to
    double gamma = 0.5; // Basal ganglia time constant
    double zeta = 0.5; // BG damped target time constant
    double dampedTarget = 0;
    double alphaTimeConstant = 0.1; // running average constant for alpha

    private Neuron dopamine;

    private Neuron output;

    public Cerebellum() {super();}
    
    /**
     * Set up the simulation object
     */
    public Cerebellum(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * Run the simulation!
     */
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build the network
        buildNetwork();
        loadCustomUpdater();

        // Time series
        addTimeSeries();

        // Set up control panel
        setUpControlPanel();

        // Add docviewer
        sim.addDocViewer(0, 279, 261, 325, "Information",
                "src/org/simbrain/custom_sims/simulations/cerebellum/cerebellum.html");

    }

    /**
     * Build the main network.
     */
    void buildNetwork() {

        // Set up network
        net = sim.addNetwork(246, 9, 538, 595, "Cerebellum");
        network = net.getNetwork();

        DecayRule generalRule = new DecayRule();
        generalRule.setDecayFraction(.25);

        DecayRule DCNRule = new DecayRule();
        DCNRule.setDecayFraction(.25);
        DCNRule.setBaseLine(1);

        DecayRule inferiorOliveRule = new DecayRule();
        inferiorOliveRule.setDecayFraction(.25);
        inferiorOliveRule.setBaseLine(.3);

        DecayRule targetRule = new DecayRule();
        targetRule.setDecayFraction(0);
        targetRule.setDecayAmount(0);
        targetRule.setBaseLine(0);

        // Cortex (assume it's at 0,0)
        NeuronGroup cortex = new NeuronGroup(network, 1);
        cortex.setLabel("Cerebral Cortex");
        cortex.setNeuronType(generalRule);
        cortex.setLocation(50, 0);
        cortex.setLowerBound(0);
        network.addGroup(cortex);

        // Red Nucleus
        NeuronGroup redNucleus = new NeuronGroup(network, 2);
        redNucleus.setLabel("Red Nucleus");
        redNucleus.setNeuronType(generalRule);
        redNucleus.setLocation(50, 200);
        redNucleus.setLowerBound(0);
        network.addGroup(redNucleus);

        // Cortex to Red Nucleus
        network.addSynapse(new Synapse(cortex.getNeuronList().get(0),
                redNucleus.getNeuronList().get(0), 1));

        // Inferior Olive
        Neuron inferiorOlive = new Neuron(network, new DecayRule());
        inferiorOlive.setLabel("Inferior Olive");
        inferiorOlive.setUpdateRule(inferiorOliveRule);
        inferiorOlive.setLocation(200, 275);
        inferiorOlive.setLowerBound(0);
        inferiorOlive.forceSetActivation(0.3);
        network.addNeuron(inferiorOlive);

        // Red Nucleus to Inf. Olive
        network.addSynapse(new Synapse(redNucleus.getNeuronList().get(0),
                inferiorOlive, 1));

        // To Spinal Cord
        NeuronGroup toSpinalCord = new NeuronGroup(network, 2);
        toSpinalCord.setLabel("To Spinal Cord");
        toSpinalCord.setNeuronType(generalRule);
        toSpinalCord.setLocation(-25, 275);
        toSpinalCord.setLowerBound(0);
        network.addGroup(toSpinalCord);

        // Cortex to Spinal Cord
        network.addSynapse(new Synapse(cortex.getNeuronList().get(0),
                toSpinalCord.getNeuronList().get(0), 1));

        // Red Nucleus to Spinal Cord
        network.addSynapse(new Synapse(redNucleus.getNeuronList().get(1),
                toSpinalCord.getNeuronList().get(1), 1));

        // Output
        output = new Neuron(network, new DecayRule());
        output.setLabel("Output");
        output.setUpdateRule(generalRule);
        output.setLocation(0, 325);
        output.setLowerBound(0);
        network.addNeuron(output);

        // Spinal Cord to Output
        network.addSynapse(
                new Synapse(toSpinalCord.getNeuronList().get(0), output, 1));
        network.addSynapse(
                new Synapse(toSpinalCord.getNeuronList().get(1), output, 1));

        // Thalamus
        Neuron thalamus = new Neuron(network);
        thalamus.setLabel("Thalamus");
        thalamus.setUpdateRule(generalRule);
        thalamus.setLocation(100, 100);
        thalamus.setLowerBound(0);
        network.addNeuron(thalamus);

        // Thalamus to Cortex
        network.addSynapse(
                new Synapse(thalamus, cortex.getNeuronList().get(0), 1));

        // Cerebellum
        NeuronGroup cerebellum = new NeuronGroup(network, 5);
        cerebellum.setLabel("Cerebellum");
        cerebellum.setNeuronType(generalRule);
        cerebellum.setLocation(175, 125);
        network.addGroup(cerebellum);
        cerebellum.getNeuronList().get(0).setLabel("Purkinje");
        cerebellum.getNeuronList().get(0).offset(25, 0);
        cerebellum.getNeuronList().get(0).forceSetActivation(0.4);
        cerebellum.getNeuronList().get(1).setLabel("DCNe");
        cerebellum.getNeuronList().get(1).setUpdateRule(DCNRule);
        cerebellum.getNeuronList().get(1).offset(-50, 75);
        cerebellum.getNeuronList().get(2).setLabel("DCNi");
        cerebellum.getNeuronList().get(2).setUpdateRule(DCNRule);
        cerebellum.getNeuronList().get(2).offset(-50, 75);
        cerebellum.getNeuronList().get(3).setLabel("Granule");
        cerebellum.getNeuronList().get(3).offset(-50, 75);
        cerebellum.getNeuronList().get(4).setLabel("Granule");
        cerebellum.getNeuronList().get(4).offset(-50, 75);
        cerebellum.setLowerBound(0);

        // Purkinje to DCN and Granule to Purkinje
        network.addSynapse(new Synapse(cerebellum.getNeuronList().get(0),
                cerebellum.getNeuronList().get(1), -2));
        network.addSynapse(new Synapse(cerebellum.getNeuronList().get(0),
                cerebellum.getNeuronList().get(2), -2));
        network.addSynapse(new Synapse(cerebellum.getNeuronList().get(3),
                cerebellum.getNeuronList().get(0), 0.02));
        network.addSynapse(new Synapse(cerebellum.getNeuronList().get(4),
                cerebellum.getNeuronList().get(0), 0.02));

        // DCNe to RedNucleus
        network.addSynapse(new Synapse(cerebellum.getNeuronList().get(1),
                redNucleus.getNeuronList().get(1), 1));

        // DCNi to inferior Olive
        network.addSynapse(new Synapse(cerebellum.getNeuronList().get(2),
                inferiorOlive, -1));

        // Inferior Olive to DCN, Pukinje
        network.addSynapse(new Synapse(inferiorOlive,
                cerebellum.getNeuronList().get(0), 0.3));
        network.addSynapse(new Synapse(inferiorOlive,
                cerebellum.getNeuronList().get(1), 1));
        network.addSynapse(new Synapse(inferiorOlive,
                cerebellum.getNeuronList().get(2), 1));

        // // DCNe to Thalamus
        // network.addSynapse(new Synapse(cerebellum.getNeuronList().get(1),
        // thalamus, 1));

        // From Spinal Cord
        NeuronGroup fromSpinalCord = new NeuronGroup(network, 2);
        fromSpinalCord.setLabel("From Spinal Cord");
        fromSpinalCord.setClamped(true);
        fromSpinalCord.setNeuronType(generalRule);
        fromSpinalCord.setLocation(275, 300);
        fromSpinalCord.setLowerBound(0);
        network.addGroup(fromSpinalCord);
        fromSpinalCord.getNeuronList().get(0).setLabel("Go");
        fromSpinalCord.getNeuronList().get(1).setLabel("No Go");

        // From Spinal Cord to Granules
        network.addSynapse(new Synapse(fromSpinalCord.getNeuronList().get(0),
                cerebellum.getNeuronList().get(3), 1));
        network.addSynapse(new Synapse(fromSpinalCord.getNeuronList().get(1),
                cerebellum.getNeuronList().get(4), 1));

        // DA
        dopamine = new Neuron(network);
        dopamine.setLabel("Basal Ganglia (GPi)");
        dopamine.setLocation(150, 50);
        // dopamine.setLowerBound(0);
        network.addNeuron(dopamine);

        // DA to Thalamus
        network.addSynapse(new Synapse(dopamine, thalamus, 1));

        // Target
        Neuron target = new Neuron(network);
        target.setLabel("Target");
        target.setClamped(true);
        // target.setUpdateRule(targetRule);
        target.setLocation(240, 50);
        // target.setLowerBound(0);
        network.addNeuron(target);

        // Labels
        NetworkTextObject parallelFiberLabel = new NetworkTextObject(network,
                230, 150, "Parallel Fibers");
        NetworkTextObject mossyFiberLabel = new NetworkTextObject(network, 265,
                240, "Mossy Fibers");
        NetworkTextObject climbingFiberLabel = new NetworkTextObject(network,
                191, 160, "CF");
        // NetworkTextObject goInputLabel = new NetworkTextObject(network, 266,
        // 320, "Go");
        // NetworkTextObject noGoInputLabel = new NetworkTextObject(network,
        // 308, 320, "No Go");
        network.addText(parallelFiberLabel);
        network.addText(mossyFiberLabel);
        network.addText(climbingFiberLabel);
        // network.addText(goInputLabel);
        // network.addText(noGoInputLabel);

    }

    //
    // Load the custom network updater
    //
    void loadCustomUpdater() {

        // References
        Neuron dopamine = network.getNeuronByLabel("Basal Ganglia (GPi)");
        Neuron output = network.getNeuronByLabel("Output");
        Neuron target = network.getNeuronByLabel("Target");
        Neuron purkinje = network.getNeuronByLabel("Purkinje");
        List<Synapse> p_fibers = purkinje.getFanIn();
        Neuron i_olive = network.getNeuronByLabel("Inferior Olive");

        NetworkUpdateAction networkUpdateAction = new NetworkUpdateAction() {
            public String getDescription() {
                return "Update basal ganglia";
            }

            public String getLongDescription() {
                return "Update basal ganglia";
            }

            public void invoke() {
                if (toggleLearning) {
                    // dampedTarget + zeta * (target.getActivation() -
                    // dampedTarget);
                    // dampedTarget = target.getActivation();
                    // double dopDelt = gamma * (dampedTarget -
                    // output.getActivation());
                    // dopamine.setInputValue(dopamine.getActivation() +
                    // dopDelt);
                    dopamine.setInputValue(
                            target.getActivation() - output.getActivation());

                    // Update parallel fiber weights
                    for (Synapse p_fiber : p_fibers) {
                        if (!p_fiber.getSource().getLabel()
                                .equalsIgnoreCase("Inferior Olive")) {
                            double delta_w = eta
                                    * p_fiber.getSource().getActivation()
                                    * (alpha - xi * i_olive.getActivation());
                            p_fiber.setStrength(
                                    p_fiber.getStrength() + delta_w);
                        }
                    }

                    // Update Alpha (stable IO firing rate)
                    alpha = (1 - alphaTimeConstant) * i_olive.getActivation();
                    // + alpha * alphaTimeConstant;
                    // System.out.println("IO: " + i_olive.getActivation()
                    // + " | alpha:" + alpha);
                    // System.out.println("Target: " + target.getActivation() +
                    // " | Damped Target:" + dampedTarget + " | Damped Damped
                    // DA:" + dopamine.getActivation());
                }
            }

        };
        network.addUpdateAction(networkUpdateAction);

        // Moves the custom update to the top of the update sequence
        network.getUpdateManager().swapElements(6, 0);
    }

    //
    // Slider bar listener
    //
    class SliderListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) {
                currentTrialLength = (int) source.getValue();
            }
        }
    }

    /**
     * Set up the controls.
     */
    void setUpControlPanel() {
        panel = ControlPanel.makePanel(sim, "Train / Test", 5, 10);

        NeuronGroup inputs = (NeuronGroup) network
                .getGroupByLabel("From Spinal Cord");
        Neuron target = network.getNeuronByLabel("Target");
        Neuron dopamine = network.getNeuronByLabel("Basal Ganglia (GPi)");

        // Just give the input of each to the model, without giving it a target
        // (and hence no Dopamine)
        panel.addButton("No Dopamine", () -> {
            inputs.getNeuronList().get(0).forceSetActivation(1);
            inputs.getNeuronList().get(1).forceSetActivation(0);
            // Turn off dopamine!
            dopamine.forceSetActivation(0);
            dopamine.setClamped(true);
            // Turn off learning
            toggleLearning = false;
            sim.iterate(currentTrialLength / 2);

            inputs.getNeuronList().get(0).forceSetActivation(0);
            inputs.getNeuronList().get(1).forceSetActivation(1);
            sim.iterate(currentTrialLength / 2);
            dopamine.setClamped(false);
            toggleLearning = true;

        });

        panel.addButton("Test", () -> {
            inputs.getNeuronList().get(0).forceSetActivation(1);
            inputs.getNeuronList().get(1).forceSetActivation(0);
            target.forceSetActivation(1);
            sim.iterate(currentTrialLength / 2);

            inputs.getNeuronList().get(0).forceSetActivation(0);
            inputs.getNeuronList().get(1).forceSetActivation(1);
            target.forceSetActivation(0);
            sim.iterate(currentTrialLength / 2);
        });

        panel.addButton("10 Trials", () -> {
            for (int ii = 0; ii <= 10; ii++) {
                inputs.getNeuronList().get(0).forceSetActivation(1);
                inputs.getNeuronList().get(1).forceSetActivation(0);
                target.forceSetActivation(1);
                sim.iterate(currentTrialLength / 2);

                inputs.getNeuronList().get(0).forceSetActivation(0);
                inputs.getNeuronList().get(1).forceSetActivation(1);
                target.forceSetActivation(0);
                sim.iterate(currentTrialLength / 2);
            }
        });

        // // Individual trial runs
        // JButton button2 = new JButton("Train Pattern 1");
        // button2.addActionListener(new ActionListener() {
        // public void actionPerformed(ActionEvent arg0) {
        // inputs.getNeuronList().get(0).forceSetActivation(1);
        // inputs.getNeuronList().get(1).forceSetActivation(0);
        // target.forceSetActivation(1);
        // workspace.iterate(currentTrialLength/2);
        // }});
        // //panel.addItem("Pattern 1", button2);

        // JButton button3 = new JButton("Train Pattern 2");
        // button3.addActionListener(new ActionListener() {
        // public void actionPerformed(ActionEvent arg0) {
        // inputs.getNeuronList().get(0).forceSetActivation(0);
        // inputs.getNeuronList().get(1).forceSetActivation(1);
        // target.forceSetActivation(0);
        // workspace.iterate(currentTrialLength/2);
        // }});
        // //panel.addItem("Pattern 2", button3);

        // JSlider to manually set for how long it runs for
        JSlider trialLengthSlider = new JSlider(JSlider.HORIZONTAL, 0, 400,
                currentTrialLength);
        trialLengthSlider.addChangeListener(new SliderListener());
        // Turn on labels at major tick marks.
        trialLengthSlider.setMajorTickSpacing(100);
        trialLengthSlider.setMinorTickSpacing(25);
        trialLengthSlider.setPaintTicks(true);
        trialLengthSlider.setPaintLabels(true);
        trialLengthSlider.setSnapToTicks(true);
        panel.addItem("", new JLabel("Trial Length"));
        panel.addItem("", trialLengthSlider);
        panel.pack();

    }

    //
    // Add the time series component
    //
    void addTimeSeries() {

        PlotBuilder plot = sim.addTimeSeriesPlot(768, 9, 363, 285,
                "dopamine, output");

        sim.couple(net.getNetworkComponent(),
                dopamine, plot.getTimeSeriesComponent(), 0);
        sim.couple(net.getNetworkComponent(), output,
                plot.getTimeSeriesComponent(), 1);

    }

    @Override
    public String getName() {
        return "Cerebellum";
    }

    @Override
    public Cerebellum instantiate(SimbrainDesktop desktop) {
        return new Cerebellum(desktop);
    }

}
