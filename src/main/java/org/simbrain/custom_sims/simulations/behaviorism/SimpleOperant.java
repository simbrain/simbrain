package org.simbrain.custom_sims.simulations.behaviorism;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.neurongroups.NeuronGroup;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateActionKt;

import java.util.HashMap;
import java.util.Map;

import static org.simbrain.network.core.NetworkUtilsKt.addNeuronGroup;

/**
 * Simulation to demonstrate classical and operant conditioning.
 *
 * @author Tim Meyer
 * @author Jeff Yoshimi
 */
public class SimpleOperant extends Simulation {

    NetworkComponent nc;
    ControlPanel panel;
    NeuronGroup behaviorNet;

    Map<Neuron, String> nodeToLabel = new HashMap();

    final int numNeurons = 3;

    public SimpleOperant() {
        super();
    }

    public SimpleOperant(SimbrainDesktop desktop) {
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
        nc = sim.addNetwork(195, 9, 447, 296, "Behaviors");
        behaviorNet = addNeuronGroup(nc.getNetwork(), -9.25, 95.93, numNeurons);
        behaviorNet.setLabel("Behaviors");
        behaviorNet.setLayout(new LineLayout(100, LineLayout.LineOrientation.HORIZONTAL));
        behaviorNet.applyLayout(-5, -85);
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
        nc.getNetwork().addUpdateAction(UpdateActionKt.create("Custom behaviorism update", () -> {
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

        }));

        setUpControlPanel();

    }

    private void setWinningNode(int nodeIndex) {
        for (int i = 0; i < behaviorNet.size(); i++) {
            if (i == nodeIndex) {
                behaviorNet.getNeuron(i).setActivation(1);
            } else {
                behaviorNet.getNeuron(i).setActivation(0);
            }
        }
    }

    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 5,9,190,109);

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
            n.setAuxValue(n.getAuxValue() /totalMass);
        }
    }

    private void updateNodeLabels() {
        for(Neuron n : behaviorNet.getNeuronList()) {
            n.setLabel(nodeToLabel.get(n) + ": "
                + SimbrainMath.roundDouble(n.getAuxValue(), 2));
        }
    }

    private String getSubmenuName() {
        return "Behaviorism";
    }

    @Override
    public String getName() {
        return "Simple Operant Conditioning";
    }

    @Override
    public SimpleOperant instantiate(SimbrainDesktop desktop) { return new SimpleOperant(desktop);
    }

}
