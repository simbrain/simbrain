package org.simbrain.custom_sims.simulations.behaviorism;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;

import java.util.*;

/**
 * Simulation to demonstrate classical and operant conditioning.
 * Discriminative case
 *
 * @author Tim Meyer
 * @author Jeff Yoshimi
 */
public class Behaviorism2 extends RegisteredSimulation {

    //TODOS

    // Add odor world in subsequent simulation

    NetBuilder netBuilder;
    Network network;
    ControlPanel panel;
    NeuronGroup behaviorNet;
    NeuronGroup stimulusNet;
    Neuron rewardNeuron, punishNeuron;

    Map<Neuron, String> nodeToLabel = new HashMap();

    final int numNeurons = 3;

    double[] firingProbabilities = new double[numNeurons];

    public Behaviorism2() {
        super();
    }

    public Behaviorism2(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * Run the simulation!
     */
    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();
        netBuilder = sim.addNetwork(195, 9, 624, 500, "Behaviorism");
        network = netBuilder.getNetwork();

        // Behavioral nodes
        behaviorNet = netBuilder.addNeuronGroup(-9.25, 95.93, numNeurons);
        ((LineLayout) behaviorNet.getLayout()).setSpacing(100);
        behaviorNet.applyLayout();
        behaviorNet.setLabel("Behaviors");

        // Stimulus nodes
        stimulusNet = netBuilder.addNeuronGroup(-9.25, 295.93, numNeurons);
        ((LineLayout) stimulusNet.getLayout()).setSpacing(100);
        stimulusNet.applyLayout();
        stimulusNet.setClamped(true);
        stimulusNet.setLabel("Stimuli");
        stimulusNet.setIncrement(1);

        // Reward and punish nodes
        rewardNeuron = netBuilder.addNeuron((int)stimulusNet.getMaxX() + 100,
            (int) stimulusNet.getCenterY());
        rewardNeuron.setUpperBound(.4);
        rewardNeuron.setLabel("Food Pellet");
        punishNeuron = netBuilder.addNeuron((int) rewardNeuron.getX() + 100,
            (int) stimulusNet.getCenterY());
        punishNeuron.setUpperBound(.4);
        punishNeuron.setLabel("Shock");

        // Set base text for behavior labels
        nodeToLabel.put(behaviorNet.getNeuron(0), "Bar Press");
        nodeToLabel.put(behaviorNet.getNeuron(1), "Spin");
        nodeToLabel.put(behaviorNet.getNeuron(2), "Sit");

        // Set stimulus labels
        stimulusNet.getNeuron(0).setLabel("Light");
        stimulusNet.getNeuron(1).setLabel("Speaker");
        stimulusNet.getNeuron(2).setLabel("Person");

        // Use aux values to store "intrinsict" firing probabilities for behaviors
        behaviorNet.getNeuron(0).setAuxValue(.33);
        behaviorNet.getNeuron(1).setAuxValue(.33);
        behaviorNet.getNeuron(2).setAuxValue(.34);

        // Initialize behaviorism labels
        updateNodeLabels();

        // Connect the layers together
        List<Synapse> syns = netBuilder.connectAllToAll(stimulusNet, behaviorNet);
        for(Synapse s : syns) {
            s.setStrength(0);
        }
        network.fireSynapsesUpdated();

        // Add custom network update action
        network.getUpdateManager().addAction(new NetworkUpdateAction() {

            @Override
            public void invoke() {
                updateNetwork();
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

        setUpControlPanel();

    }

    private void updateNetwork() {

        // Update firing probabilities
        for (int i = 0; i < behaviorNet.size(); i++) {
            Neuron n = behaviorNet.getNeuron(i);
            firingProbabilities[i] = n.getWeightedInputs() + n.getAuxValue();
        }
        firingProbabilities = SimbrainMath.normalizeVec(firingProbabilities);
        System.out.println(Arrays.toString(firingProbabilities));

        // Select "winning" neuron based on its probability
        double random = Math.random();
        if(random < firingProbabilities[0]){
            setWinningNode(0);
        } else if(random < firingProbabilities[0] + firingProbabilities[1]) {
            setWinningNode(1);
        } else{
            setWinningNode(2);
        }
    }

    private void setWinningNode(int nodeIndex) {
        for (int i = 0; i < behaviorNet.size(); i++) {
            if (i == nodeIndex) {
                behaviorNet.getNeuron(i).forceSetActivation(1);
            } else {
                behaviorNet.getNeuron(i).forceSetActivation(0);
            }
        }
    }

    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10);

        panel.addButton("Reward", () -> {
            learn(.1);
            rewardNeuron.setInputValue(.1);
            punishNeuron.forceSetActivation(0);
            sim.iterate();
       });

        panel.addButton("Punish", () -> {
            learn(-.1);
            rewardNeuron.forceSetActivation(0);
            punishNeuron.setInputValue(.1);
            sim.iterate();
        });

    }

    private void learn(double valence) {
        // todo: possibly separate learning rates out
        for(Neuron tar : behaviorNet.getNeuronList()) {

            // The "winning" node
            if(tar.getActivation() > 0){
                // Update intrinsic probability
                double p = tar.getAuxValue();
                tar.setAuxValue(Math.max(p + valence * p, 0));

                // Update weight on active node
                for(Neuron src : stimulusNet.getNeuronList()) {
                    if (src.getActivation() > 0) {
                        Synapse s = Network.getSynapse(src,tar);
                        s.setStrength(Math.max(s.getStrength() + valence, 0));
                    }
                }
            }
        }
        normIntrinsicProbabilities();
        updateNodeLabels();
        sim.iterate();
    }

    private void normIntrinsicProbabilities() {
        double totalMass = 0;
        for(Neuron n : behaviorNet.getNeuronList()) {
            totalMass += n.getAuxValue();
        }
        for(Neuron n : behaviorNet.getNeuronList()) {
            n.setAuxValue(n.getAuxValue()/totalMass);
        }
    }

    private void updateNodeLabels() {
        for(Neuron n : behaviorNet.getNeuronList()) {
            n.setLabel(nodeToLabel.get(n) + ": "
                + SimbrainMath.roundDouble(n.getAuxValue(), 2));
        }
    }

    @Override
    public String getName() {
        return "Behaviorism 2";
    }

    @Override
    public Behaviorism2 instantiate(SimbrainDesktop desktop) { return new Behaviorism2(desktop);
    }

}
