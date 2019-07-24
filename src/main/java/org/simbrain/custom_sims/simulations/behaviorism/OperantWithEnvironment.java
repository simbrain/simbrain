package org.simbrain.custom_sims.simulations.behaviorism;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetworkWrapper;
import org.simbrain.custom_sims.helper_classes.OdorWorldWrapper;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

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
    NetworkWrapper networkWrapper;
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
    OdorWorldWrapper world;
    OdorWorldEntity mouse;
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
        networkWrapper = sim.addNetwork(155,9,575,500, "Brain");
        network = networkWrapper.getNetwork();

        // Behavioral nodes
        behaviorNet = networkWrapper.addNeuronGroup(-9.25, 95.93, numNeurons);
        ((LineLayout) behaviorNet.getLayout()).setSpacing(100);
        behaviorNet.applyLayout();
        behaviorNet.setLabel("Behaviors");

        // Stimulus nodes
        stimulusNet = networkWrapper.addNeuronGroup(-9.25, 295.93, numNeurons);
        ((LineLayout) stimulusNet.getLayout()).setSpacing(100);
        stimulusNet.applyLayout();
        stimulusNet.setClamped(true);
        stimulusNet.setLabel("Stimuli");
        stimulusNet.setIncrement(1);

        // Reward and punish nodes
        rewardNeuron = networkWrapper.addNeuron((int)stimulusNet.getMaxX() + 100,
            (int) stimulusNet.getCenterY());
        rewardNeuron.setLabel("Food Pellet");
        punishNeuron = networkWrapper.addNeuron((int) rewardNeuron.getX() + 100,
            (int) stimulusNet.getCenterY());
        punishNeuron.setLabel("Shock");

        // Set base text for behavior labels
        nodeToLabel.put(behaviorNet.getNeuron(0), "Wiggle");
        nodeToLabel.put(behaviorNet.getNeuron(1), "Explore");
        nodeToLabel.put(behaviorNet.getNeuron(2), "Spin");

        // Set stimulus labels
        stimulusNet.getNeuron(0).setLabel("Candle");
        stimulusNet.getNeuron(1).setLabel("Flower");
        stimulusNet.getNeuron(2).setLabel("Bell");

        // Use aux values to store "intrinsic" firing probabilities for behaviors
        behaviorNet.getNeuron(0).setAuxValue(.33); // Node 0
        behaviorNet.getNeuron(1).setAuxValue(.33); // Node 1
        behaviorNet.getNeuron(2).setAuxValue(.33); // Node 2

        // Initialize behaviorism labels
        updateNodeLabels();

        // Clear selection
        networkWrapper.getNetworkPanel().clearSelection(); // todo: why needed?

        // Connect the layers together
        List<Synapse> syns = networkWrapper.connectAllToAll(stimulusNet, behaviorNet);
        for(Synapse s : syns) {
            s.setStrength(0);
        }
        network.fireSynapsesUpdated();

        // Create the odor world
        world = sim.addOdorWorld(730,7,315,383, "Three Objects");
        world.getWorld().setObjectsBlockMovement(false);
        mouse = world.addEntity(120, 245, EntityType.MOUSE);
        mouse.setHeading(90);

        // Set up world
        cheese = world.addEntity(27, 20, EntityType.CANDLE);
        flower = world.addEntity(79, 20, EntityType.PANSY);
        fish = world.addEntity(125, 20, EntityType.FISH);


        // Set up object sensors
        ObjectSensor cheeseSensor = mouse.addObjectSensor(EntityType.SWISS, 50, 0, 65);
        ObjectSensor flowerSensor = mouse.addObjectSensor(EntityType.PANSY, 50, 0, 65);
        ObjectSensor fishSensor = mouse.addObjectSensor(EntityType.FISH, 50, 0, 65);

        // Couple agent to network
        sim.couple(cheeseSensor,stimulusNet.getNeuron(0));
        sim.couple(flowerSensor,stimulusNet.getNeuron(1));
        sim.couple(fishSensor,stimulusNet.getNeuron(2));

        // Add custom network update action
        network.getUpdateManager().clear();
        network.getUpdateManager().addAction(new NetworkUpdateAction() {

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

        // Update actual firing probabilities, which combine
        // intrinsic probabilities with weighted inputs
        for (int i = 0; i < behaviorNet.size(); i++) {
            Neuron n = behaviorNet.getNeuron(i);
            firingProbabilities[i] = n.getWeightedInputs() + n.getAuxValue();
        }
        if (SimbrainMath.getMinimum(firingProbabilities) < 0) {
            firingProbabilities = SimbrainMath.minMaxNormalize(firingProbabilities);
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

        network.bufferedUpdateAllNeurons();

    }

    private void setWinningNode(int nodeIndex) {
        winningNode = nodeIndex;
        for (int i = 0; i < behaviorNet.size(); i++) {
            if (i == nodeIndex) {
                behaviorNet.getNeuron(i).setInputValue(1);
                behaviorNet.getNeuron(i).setActivation(1);
            } else {
                behaviorNet.getNeuron(i).setInputValue(0);
                behaviorNet.getNeuron(i).setActivation(0);
            }
        }
    }

    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10);

        panel.addButton("Reward", () -> {
            learn(1);
            rewardNeuron.forceSetActivation(1);
            punishNeuron.forceSetActivation(0);
            sim.iterate();
        });

        panel.addButton("Punish", () -> {
            learn(-1);
            rewardNeuron.forceSetActivation(0);
            punishNeuron.forceSetActivation(1);
            sim.iterate();
        });

        panel.addButton("Do nothing", () -> {
            rewardNeuron.forceSetActivation(0);
            punishNeuron.forceSetActivation(0);
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

        double totalActivation = stimulusNet.getNeuronList().stream().mapToDouble(Neuron::getActivation).sum();
        Neuron winner = WinnerTakeAll.getWinner(behaviorNet.getNeuronList(), true);

        // If there are inputs, update weights
        if(totalActivation > .1) {
            Neuron src = WinnerTakeAll.getWinner(stimulusNet.getNeuronList(), true);
            Synapse s_r = Network.getLooseSynapse(src,winner);
            // Strengthen or weaken active S-R Pair
            s_r.setStrength(s_r.getStrength() + valence);

        } else {
            // Else update intrinsic probability
            double p = winner.getAuxValue();
            winner.setAuxValue(Math.max(p + valence * p, 0));
            normIntrinsicProbabilities();
            updateNodeLabels();
        }

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
        return "Behaviorism: Operant Conditioning (Environment)";
    }

    @Override
    public OperantWithEnvironment instantiate(SimbrainDesktop desktop) { return new OperantWithEnvironment(desktop);
    }

}
