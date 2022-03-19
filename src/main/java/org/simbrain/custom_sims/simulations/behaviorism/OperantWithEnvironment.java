package org.simbrain.custom_sims.simulations.behaviorism;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkKt;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateActionKt;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

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
public class OperantWithEnvironment extends RegisteredSimulation {

    // Network
    NetworkComponent nc;
    Network net;
    ControlPanel panel;
    NeuronGroup behaviorNet;
    NeuronGroup stimulusNet;
    Neuron rewardNeuron, punishNeuron;
    Map<Neuron, String> nodeToLabel = new HashMap();
    final int numNeurons = 3;
    double[] firingProbabilities = new double[numNeurons];
    int winningNode;

    // World
    OdorWorldComponent oc;
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
        nc = sim.addNetwork(155,9,575,500, "Brain");
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
        sim.getNetworkPanel(nc).getSelectionManager().clear();

        // Connect the layers together
        List<Synapse> syns = connectAllToAll(stimulusNet, behaviorNet);
        for(Synapse s : syns) {
            s.setStrength(0);
        }
       // network.fireSynapsesUpdated(); // TODO: [event]

        // Create the odor world
        oc = sim.addOdorWorld(730,7,315,383, "Three Objects");
        oc.getWorld().setObjectsBlockMovement(false);
        oc.getWorld().setUseCameraCentering(false);
        mouse = oc.getWorld().addEntity(120, 245, EntityType.MOUSE);
        mouse.setHeading(90);

        // Set up world
        cheese = oc.getWorld().addEntity(27, 20, EntityType.CANDLE);
        flower = oc.getWorld().addEntity(79, 20, EntityType.PANSY);
        fish = oc.getWorld().addEntity(125, 20, EntityType.FISH);


        // Set up object sensors
        ObjectSensor cheeseSensor = mouse.addObjectSensor(EntityType.SWISS, 50, 0, 65);
        ObjectSensor flowerSensor = mouse.addObjectSensor(EntityType.PANSY, 50, 0, 65);
        ObjectSensor fishSensor = mouse.addObjectSensor(EntityType.FISH, 50, 0, 65);

        // Couple agent to network
        sim.couple(cheeseSensor,stimulusNet.getNeuron(0));
        sim.couple(flowerSensor,stimulusNet.getNeuron(1));
        sim.couple(fishSensor,stimulusNet.getNeuron(2));

        // Add custom network update action
        net.getUpdateManager().clear();
        net.getUpdateManager().addAction(UpdateActionKt.create("Custom behaviorism update", () -> {
            updateNetwork();
            updateBehaviors();
        }));

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

        net.bufferedUpdate();

    }

    private void setWinningNode(int nodeIndex) {
        winningNode = nodeIndex;
        for (int i = 0; i < behaviorNet.size(); i++) {
            if (i == nodeIndex) {
                behaviorNet.getNeuron(i).addInputValue(1);
                behaviorNet.getNeuron(i).setActivation(1);
            } else {
                behaviorNet.getNeuron(i).addInputValue(0);
                behaviorNet.getNeuron(i).setActivation(0);
            }
        }
    }

    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10, 150, 189);

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
            Synapse s_r = NetworkKt.getLooseSynapse(src,winner);
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
    public String getSubmenuName() {
        return "Behaviorism";
    }

    @Override
    public String getName() {
        return "Operant Conditioning (Environment)";
    }

    @Override
    public OperantWithEnvironment instantiate(SimbrainDesktop desktop) { return new OperantWithEnvironment(desktop);
    }

}
