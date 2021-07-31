package org.simbrain.custom_sims.simulations.behaviorism;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.*;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.simbrain.network.core.NetworkKt.connectAllToAll;

/**
 * Simulation to demonstrate classical and operant conditioning.
 * Discriminative case
 *
 * @author Tim Meyer
 * @author Jeff Yoshimi
 */
public class OperantConditioning extends RegisteredSimulation {

    //TODO: Test.

    NetworkComponent nc;
    Network net;
    ControlPanel panel;
    NeuronGroup behaviorNet;
    NeuronGroup stimulusNet;
    Neuron rewardNeuron, punishNeuron;

    Map<Neuron, String> nodeToLabel = new HashMap();

    final int numNeurons = 3;

    double[] firingProbabilities = new double[numNeurons];

    public OperantConditioning() {
        super();
    }

    public OperantConditioning(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * Run the simulation!
     */
    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();
        nc = sim.addNetwork(195, 9, 624, 500, "Simulation");
        net = nc.getNetwork();

        // Behavioral nodes
        behaviorNet = net.addNeuronGroup(-9.25, 95.93, numNeurons);
        ((LineLayout) behaviorNet.getLayout()).setSpacing(100);
        behaviorNet.applyLayout();
        behaviorNet.setLabel("Behaviors");

        // Stimulus nodes
        stimulusNet = net.addNeuronGroup(-9.25, 295.93, numNeurons);
        ((LineLayout) stimulusNet.getLayout()).setSpacing(100);
        stimulusNet.applyLayout();
        stimulusNet.setClamped(true);
        stimulusNet.setLabel("Stimuli");
        stimulusNet.setIncrement(1);

        // Reward and punish nodes
        rewardNeuron = net.addNeuron((int)stimulusNet.getMaxX() + 100,
            (int) stimulusNet.getCenterY());
        rewardNeuron.setLabel("Food Pellet");
        punishNeuron = net.addNeuron((int) rewardNeuron.getX() + 100,
            (int) stimulusNet.getCenterY());
        punishNeuron.setLabel("Shock");

        // Set base text for behavior labels
        nodeToLabel.put(behaviorNet.getNeuron(0), "Bar Press");
        nodeToLabel.put(behaviorNet.getNeuron(1), "Jump");
        nodeToLabel.put(behaviorNet.getNeuron(2), "Scratch Nose");

        // Set stimulus labels
        stimulusNet.getNeuron(0).setLabel("Red Light");
        stimulusNet.getNeuron(1).setLabel("Green Light");
        stimulusNet.getNeuron(2).setLabel("Speaker");

        // Use aux values to store "intrinsict" firing probabilities for behaviors
        behaviorNet.getNeuron(0).setAuxValue(.33);
        behaviorNet.getNeuron(1).setAuxValue(.33);
        behaviorNet.getNeuron(2).setAuxValue(.34);

        // Initialize behaviorism labels
        updateNodeLabels();

        // Clear selection
        sim.getNetworkPanel(nc).getSelectionManager().clear();

        // Connect the layers together
        List<Synapse> syns = connectAllToAll(stimulusNet, behaviorNet);
        for(Synapse s : syns) {
            s.setStrength(0);
        }
//        network.fireSynapsesUpdated(); // TODO: [event]

        // Add custom network update action
        net.getUpdateManager().addAction(new NetworkUpdateAction() {

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
        //System.out.println(Arrays.toString(firingProbabilities));

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
            learn(1);
            rewardNeuron.addInputValue(1);
            punishNeuron.forceSetActivation(0);
            sim.iterate();
       });

        panel.addButton("Punish", () -> {
            learn(-1);
            rewardNeuron.forceSetActivation(0);
            punishNeuron.addInputValue(1);
            sim.iterate();
        });

        panel.addButton("Do nothing", () -> {
            rewardNeuron.forceSetActivation(0);
            punishNeuron.addInputValue(0);
            sim.iterate();
        });

    }

    private void learn(double valence) {

        double rewardLearningRate = .1;
        double punishLearningRate = .1;

        if(valence > 0) {
            valence *= rewardLearningRate;
        } else {
            valence *= punishLearningRate;
        }

        for(Neuron tar : behaviorNet.getNeuronList()) {

            // The "winning" node
            if(tar.getActivation() > 0){

                // Update intrinsic probability
                double p = tar.getAuxValue();
                tar.setAuxValue(Math.max(p + valence * p, 0));

                // Update weight on active node
                for(Neuron src : stimulusNet.getNeuronList()) {
                    if (src.getActivation() > 0) {
                        Synapse s = NetworkKt.getLooseSynapse(src,tar);
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
    public String getSubmenuName() {
        return "Behaviorism";
    }

    @Override
    public String getName() {
        return "Operant Conditioning";
    }

    @Override
    public OperantConditioning instantiate(SimbrainDesktop desktop) { return new OperantConditioning(desktop);
    }

}
