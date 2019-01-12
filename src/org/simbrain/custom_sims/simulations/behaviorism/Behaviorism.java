package org.simbrain.custom_sims.simulations.behaviorism;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulation to demonstrate classical and operant conditioning.
 *
 * @author Tim Meyer
 * @author Jeff Yoshimi
 */
public class Behaviorism extends RegisteredSimulation {

    NetBuilder netBuilder;
    ControlPanel panel;
    NeuronGroup behaviorNet;

    Map<Neuron, String> nodeToLabel = new HashMap();

    final int numNeurons = 3;

    public Behaviorism() {
        super();
    }

    public Behaviorism(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * Run the simulation!
     */
    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build a network
        netBuilder = sim.addNetwork(195, 9, 447, 296, "Behaviors");
        behaviorNet = netBuilder.addNeuronGroup(-9.25, 95.93, numNeurons);
        ((LineLayout) behaviorNet.getLayout()).setSpacing(100);
        behaviorNet.applyLayout();
        behaviorNet.setClamped(true);

        // Set base text for labels
        nodeToLabel.put(behaviorNet.getNeuron(0), "Yell");
        nodeToLabel.put(behaviorNet.getNeuron(1), "Sit");
        nodeToLabel.put(behaviorNet.getNeuron(2), "Run");

        // Use aux values to store firing probabilities
        behaviorNet.getNeuron(0).setAuxValue(.34);
        behaviorNet.getNeuron(1).setAuxValue(.33);
        behaviorNet.getNeuron(2).setAuxValue(.33);

        // Initialize labels
        updateNodeLabels();

        // Add custom network update action
        netBuilder.getNetwork().addUpdateAction(new NetworkUpdateAction() {

            @Override
            public void invoke() {
                // Select "winning" neuron based on its probability
                // TODO: There must be a better, generalizable way to do this
                double random = Math.random();
                if(random < behaviorNet.getNeuron(0).getAuxValue()){
                    setWinningNode(0);
                } else if(random < behaviorNet.getNeuron(0).getAuxValue()
                    + behaviorNet.getNeuron(1).getAuxValue()) {
                    setWinningNode(1);
                } else{
                    setWinningNode(2);
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

        setUpControlPanel();

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

        panel.addButton("Reward Agent", () -> {

            for(Neuron n : behaviorNet.getNeuronList()) {
                if(n.getActivation() > 0){
                    double p = n.getAuxValue();
                    n.setAuxValue(Math.max(p + .1 * p, 0));
                }
            }
            normalizeProbabilities();
            updateNodeLabels();
            sim.iterate();
        });

        panel.addButton("Punish Agent", () -> {
            for(Neuron n : behaviorNet.getNeuronList()) {
                if(n.getActivation() > 0){
                    double p = n.getAuxValue();
                    n.setAuxValue(Math.max(p - .1 * p, 0));
                }
            }
            normalizeProbabilities();
            updateNodeLabels();
            sim.iterate();
        });

    }

    private void normalizeProbabilities() {
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
        return "Behaviorism";
    }

    @Override
    public Behaviorism instantiate(SimbrainDesktop desktop) { return new Behaviorism(desktop);
    }

}
