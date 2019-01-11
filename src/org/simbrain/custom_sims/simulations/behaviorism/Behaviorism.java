package org.simbrain.custom_sims.simulations.behaviorism;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

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
    NeuronGroup sensoryNet;

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
        sensoryNet = netBuilder.addNeuronGroup(-9.25, 95.93, numNeurons);
        sensoryNet.setClamped(true);

        // Set base text for labels
        nodeToLabel.put(sensoryNet.getNeuron(0), "B1");
        nodeToLabel.put(sensoryNet.getNeuron(1), "B2");
        nodeToLabel.put(sensoryNet.getNeuron(2), "B3");

        // Use aux values to store firing probabilities
        sensoryNet.getNeuron(0).setAuxValue(.34);
        sensoryNet.getNeuron(1).setAuxValue(.33);
        sensoryNet.getNeuron(2).setAuxValue(.33);

        // Add custom network update action
        netBuilder.getNetwork().addUpdateAction(new NetworkUpdateAction() {

            @Override
            public void invoke() {
                // Select "winning" neuron based on its probability
                // TODO: There must be a better, generalizable way to do this
                double random = Math.random();
                if(random < sensoryNet.getNeuron(0).getAuxValue()){
                    setWinningNode(0);
                } else if(random < sensoryNet.getNeuron(0).getAuxValue()
                    + sensoryNet.getNeuron(1).getAuxValue()) {
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
        System.out.println("Winner: " + nodeIndex);
        for (int i = 0; i < sensoryNet.size(); i++) {
            if (i == nodeIndex) {
                sensoryNet.getNeuron(i).forceSetActivation(1);
            } else {
                sensoryNet.getNeuron(i).forceSetActivation(0);
            }
        }
    }

    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10);

        panel.addButton("Reward Agent", () -> {

            for(Neuron n : sensoryNet.getNeuronList()) {
                if(n.getActivation() > 0){
                    n.setAuxValue(n.getAuxValue() + .1); ;
                }
            }
            normalizeProbabilities();
            updateNodeLabels();
            sim.iterate();
        });

        panel.addButton("Punish Agent", () -> {
            for(Neuron n : sensoryNet.getNeuronList()) {
                if(n.getActivation() > 0){
                    n.setAuxValue(n.getAuxValue() - .1); ;
                }
            }
            normalizeProbabilities();
            updateNodeLabels();
            sim.iterate();
        });

    }

    private void normalizeProbabilities() {
        int totalMass = 0;
        for(Neuron n : sensoryNet.getNeuronList()) {
            totalMass += n.getAuxValue();
        }
        System.out.println("Total mass = " + totalMass);
        if (totalMass == 0) {
            System.err.println("Mass was 0!");
            return;
        }
        for(Neuron n : sensoryNet.getNeuronList()) {
            n.setAuxValue(n.getAuxValue()/totalMass);
        }
    }

    private void updateNodeLabels() {
        for(Neuron n : sensoryNet.getNeuronList()) {
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
