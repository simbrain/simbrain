package org.simbrain.custom_sims.simulations.behaviorism;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulation to demonstrate classical and operant conditioning.
 * Discriminative case
 *
 * @author Tim Meyer
 * @author Jeff Yoshimi
 */
public class OperantWithEnvironment extends RegisteredSimulation {

    // Network
    NetBuilder netBuilder;
    Network network;
    ControlPanel panel;
    NeuronGroup behaviorNet;
    NeuronGroup stimulusNet;
    Neuron rewardNeuron, punishNeuron;
    Map<Neuron, String> nodeToLabel = new HashMap();
    final int numNeurons = 3;
    double[] firingProbabilities = new double[numNeurons];
    int winningNode;

    // World
    OdorWorldBuilder world;
    RotatingEntity mouse;
    OdorWorldEntity cheese, flower, fish;

    public OperantWithEnvironment() {
        super();
    }

    public OperantWithEnvironment(SimbrainDesktop desktop) {
        super(desktop);
    }

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();
        netBuilder = sim.addNetwork(155,9,575,500, "Brain");
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
        rewardNeuron.setLabel("Food Pellet");
        punishNeuron = netBuilder.addNeuron((int) rewardNeuron.getX() + 100,
            (int) stimulusNet.getCenterY());
        punishNeuron.setLabel("Shock");

        // Set base text for behavior labels
        nodeToLabel.put(behaviorNet.getNeuron(0), "Wiggle");
        nodeToLabel.put(behaviorNet.getNeuron(1), "Explore");
        nodeToLabel.put(behaviorNet.getNeuron(2), "Spin");

        // Set stimulus labels
        stimulusNet.getNeuron(0).setLabel("Cheese");
        stimulusNet.getNeuron(1).setLabel("Flower");
        stimulusNet.getNeuron(2).setLabel("Bell");

        // Use aux values to store "intrinsic" firing probabilities for behaviors
        behaviorNet.getNeuron(0).setAuxValue(.33); // Node 0
        behaviorNet.getNeuron(1).setAuxValue(.33); // Node 1
        behaviorNet.getNeuron(2).setAuxValue(.33); // Node 2

        // Initialize behaviorism labels
        updateNodeLabels();

        // Connect the layers together
        List<Synapse> syns = netBuilder.connectAllToAll(stimulusNet, behaviorNet);
        for(Synapse s : syns) {
            s.setStrength(0);
        }
        network.fireSynapsesUpdated();

        // Create the odor world
        world = sim.addOdorWorld(730,7,315,383, "Three Objects");
        world.getWorld().setObjectsBlockMovement(false);
        mouse = world.addAgent(120, 245, "Mouse");
        mouse.setHeading(90);

        // Set up world
        cheese = world.addEntity(27, 20, "Swiss.gif",
            new double[] { 1, 0, 0 });
        cheese.getSmellSource().setDispersion(65);
        flower = world.addEntity(79, 20, "Pansy.gif",
            new double[] { 0, 1, 0 });
        cheese.getSmellSource().setDispersion(65);
        fish = world.addEntity(125, 20, "Bell.gif",
            new double[] { 0, 0, 1 });
        cheese.getSmellSource().setDispersion(65);

        // Couple agent to network
        sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), 0,
            stimulusNet.getNeuron(0));
        sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), 1,
            stimulusNet.getNeuron(1));
        sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), 2,
            stimulusNet.getNeuron(2));

        // Add custom network update action
        network.getUpdateManager().addAction(0,new NetworkUpdateAction() {

            @Override
            public void invoke() {
                updateNetwork();
                updateBehaviors();
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

    /**
     * Update behavior of odor world agent based on which node is active.
     * Assumes behaviors partitioned into increments of (currently) 100 time steps
     */
    private void updateBehaviors() {

        int loopTime = sim.getWorkspace().getTime() % 10;

        // Node 0: Wiggle
        if(winningNode == 0) {
            if (loopTime < 5) {
                mouse.setHeading(mouse.getHeading() + 5);
            } else {
                mouse.setHeading(mouse.getHeading() - 5);
            }
        }
        // Node 1: Explore
        else if (winningNode == 1) {
            if (Math.random() < .2) {
                mouse.setHeading(mouse.getHeading() + Math.random()*20-10);
            }
            mouse.goStraight(2.5);
        }
        // Node 2: Spin
        else {
            mouse.setHeading(mouse.getHeading()+20);
        }
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
        winningNode = nodeIndex;
        for (int i = 0; i < behaviorNet.size(); i++) {
            if (i == nodeIndex) {
                behaviorNet.getNeuron(i).setInputValue(1);
            } else {
                behaviorNet.getNeuron(i).setInputValue(0);
            }
        }
    }

    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10);

        panel.addButton("Reward", () -> {
            learn(1);
            rewardNeuron.setInputValue(1);
            punishNeuron.forceSetActivation(0);
            sim.iterate();
        });

        panel.addButton("Punish", () -> {
            learn(-1);
            rewardNeuron.forceSetActivation(0);
            punishNeuron.setInputValue(1);
            sim.iterate();
        });

        panel.addButton("Do nothing", () -> {
            rewardNeuron.forceSetActivation(0);
            punishNeuron.setInputValue(0);
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
        return "Behaviorism: Operant with Mouse";
    }

    @Override
    public OperantWithEnvironment instantiate(SimbrainDesktop desktop) { return new OperantWithEnvironment(desktop);
    }

}
