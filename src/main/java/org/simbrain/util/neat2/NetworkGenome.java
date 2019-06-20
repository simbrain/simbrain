package org.simbrain.util.neat2;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.geneticalgorithm.Genome;

import java.util.*;

/**
 * Contains node genes and connection genes (a {@link NodeChromosome} and {@link ConnectionChromosome}
 * which are used to express a {@link Network}.
 * <br>
 * Currently just creates a set of inputs, outputs, and connections between them.
 */
public class NetworkGenome extends Genome<NetworkGenome, Network> {

    /**
     * A label for connection genes, used to align connection genes during crossover.
     * Prevents duplicate connections between nodes.  Mainly here for performance
     * reasons: during crossover can simply match numbers.
     */
    private static Map<ConnectionGene, Integer> innovationNumberMap = new HashMap<>();

    /**
     * A set of node genes.
     */
    private NodeChromosome nodeGenes = new NodeChromosome();

    /**
     * A set of connection genes.
     */
    private ConnectionChromosome connectionGenes = new ConnectionChromosome();

    /**
     * Configuration of allowable networks to evolve.
     */
    private NetworkGenome.Configuration configuration;

    /**
     * Create a network genome from an external configuration template.
     *
     * @param configuration the configuration template
     */
    public NetworkGenome(NetworkGenome.Configuration configuration) {
        this.configuration = configuration;

        // Set up default input and output genes
        for (int i = 0; i < configuration.numInputs; i++) {
            NodeGene nodeGene = new NodeGene();
            nodeGene.setMutable(false);
            nodeGene.setType(NodeGene.NodeType.input);
            nodeGene.getPrototype().setIncrement(1);
            nodeGene.getPrototype().setClamped(true);
            nodeGene.setConfiguration(configuration);
            nodeGene.setRandomizer(this::getRandomizer);
            nodeGenes.addGene(nodeGene);
        }
        for (int i = 0; i < configuration.numOutputs; i++) {
            NodeGene nodeGene = new NodeGene();
            nodeGene.setMutable(false);
            nodeGene.setType(NodeGene.NodeType.output);
            nodeGene.setConfiguration(configuration);
            nodeGene.setRandomizer(this::getRandomizer);
            nodeGenes.addGene(nodeGene);
        }
    }

    @Override
    public Network express() {

        Network network = new Network();

        List<Neuron> neurons = new ArrayList<>();

        NeuronGroup inputGroup = new NeuronGroup(network);
        NeuronGroup outputGroup = new NeuronGroup(network);

        nodeGenes.getGenes().forEach(nodeGene -> {
            Neuron neuron = new Neuron(network, nodeGene.getPrototype());
            neurons.add(neuron);

            if (nodeGene.getType() == NodeGene.NodeType.input) {
                inputGroup.addNeuron(neuron);
            } else if (nodeGene.getType() == NodeGene.NodeType.output) {
                outputGroup.addNeuron(neuron);
            } else {
                // Set locations based on where the input and outputs are set
                neuron.setX(getRandomizer().nextDouble(-200, 200));
                neuron.setY(getRandomizer().nextDouble(0, 300));
                network.addNeuron(neuron);
            }
        });

        network.addGroup(inputGroup);
        network.addGroup(outputGroup);
        inputGroup.setLabel("inputs");
        inputGroup.applyLayout();
        inputGroup.offset(0, 300);
        outputGroup.setLabel("outputs");
        outputGroup.applyLayout();
        connectionGenes.getGenes().forEach(connectionGene -> {
            Synapse synapse = new Synapse(
                    network,
                    neurons.get(connectionGene.getSourceIndex()),
                    neurons.get(connectionGene.getTargetIndex()),
                    connectionGene.getPrototype().getLearningRule(),
                    connectionGene.getPrototype()
            );
            network.addSynapse(synapse);
        });

        return network;
    }

    @Override
    public NetworkGenome crossOver(NetworkGenome otherGenome) {

        NetworkGenome ret = new NetworkGenome(configuration);

        ret.inheritRandomizer(getRandomizer());

        ret.nodeGenes = nodeGenes.crossOver(otherGenome.nodeGenes);
        ret.nodeGenes.setRandomizer(ret.getRandomizer());

        ret.connectionGenes = connectionGenes.crossOver(otherGenome.connectionGenes);
        ret.connectionGenes.setRandomizer(ret.getRandomizer());

        return ret;
    }


    @Override
    public void mutate() {

        nodeGenes.mutate();

        // New node mutation
        if (nodeGenes.getGenes().size() < configuration.maxNode && getRandomizer().nextDouble(0, 1) < configuration.newNodeMutationProbability) {
            NodeGene newNodeGene = new NodeGene();
            newNodeGene.setConfiguration(configuration);
            newNodeGene.setRandomizer(nodeGenes.getRandomizer());
            nodeGenes.addGenes(Collections.singleton(newNodeGene));
        }

        // Connection weight mutation
        connectionGenes.mutate();

        // New connection mutation
        if (getRandomizer().nextDouble(0, 1) < configuration.newConnectionMutationProbability) {

            // Create a new connection

            // First, select a source neuron.
            int sourceNodeID = getRandomizer().nextInt(nodeGenes.getMaxNodeID() + 1);
            // Ensure that the source neuron exists in the node genes, and is not a output neuron
            while (!nodeGenes.contains(sourceNodeID) || nodeGenes.getByID(sourceNodeID).getType() == NodeGene.NodeType.output) {
                sourceNodeID = getRandomizer().nextInt(nodeGenes.getMaxNodeID() + 1);
            }
            // Now select a target neuron
            int destinationNodeID = getRandomizer().nextInt(nodeGenes.getMaxNodeID() + 1);
            // Ensure the the target neuron exists in the node genes, is not an input neuron, and (if set) is not
            // a self-connection
            while (!nodeGenes.contains(destinationNodeID)
                    || nodeGenes.getByID(destinationNodeID).getType() == NodeGene.NodeType.input
                    || (!configuration.allowSelfConnection && sourceNodeID == destinationNodeID)) {
                destinationNodeID = getRandomizer().nextInt(nodeGenes.getMaxNodeID() + 1);
            }

            // Create the new connection gene
            ConnectionGene newConnectionGene =
                    new ConnectionGene(
                            sourceNodeID,
                            destinationNodeID,
                            getRandomizer().nextDouble(
                                    configuration.minConnectionStrength,
                                    configuration.maxConnectionStrength
                            ));
            newConnectionGene.setConfiguration(configuration);
            // If this is a new connection gene, add a new innovation number entry.
            // otherwise we are just mutating an existing connection gene
            if (!innovationNumberMap.containsKey(newConnectionGene)) {
                innovationNumberMap.put(newConnectionGene, innovationNumberMap.size());
            }
            newConnectionGene.setRandomizer(connectionGenes.getRandomizer());
            connectionGenes.addGene(innovationNumberMap.get(newConnectionGene), newConnectionGene);
        }
    }

    @Override
    public NetworkGenome copy() {

        NetworkGenome ret = new NetworkGenome(configuration);

        ret.inheritRandomizer(getRandomizer());

        ret.nodeGenes = nodeGenes.copy();
        ret.nodeGenes.setRandomizer(ret.getRandomizer());

        ret.connectionGenes = connectionGenes.copy();
        ret.connectionGenes.setRandomizer(ret.getRandomizer());

        return ret;
    }

    /**
     * These configuration parameters constrain what kind of network can be evolved.
     */
    public static class Configuration {

        private int numInputs;
        private int numOutputs;
        private int maxNode = Integer.MAX_VALUE;
        private double nodeMaxBiasMutation = 0.1;
        private double nodeMaxBias = 1;
        private boolean allowSelfConnection = true;
        private double newConnectionMutationProbability = 0.05;
        private double newNodeMutationProbability = 0.05;
        private double maxConnectionStrength = 10;
        private double minConnectionStrength = -10;
        private double maxConnectionMutation = 1;
        private boolean allowFreeFloatingNodes = false;

        public Configuration() {
        }

        public int getNumInputs() {
            return numInputs;
        }

        public void setNumInputs(int numInputs) {
            this.numInputs = numInputs;
        }

        public int getNumOutputs() {
            return numOutputs;
        }

        public void setNumOutputs(int numOutputs) {
            this.numOutputs = numOutputs;
        }

        public int getMaxNode() {
            return maxNode;
        }

        public void setMaxNode(int maxNode) {
            this.maxNode = maxNode;
        }

        public boolean isAllowSelfConnection() {
            return allowSelfConnection;
        }

        public void setAllowSelfConnection(boolean allowSelfConnection) {
            this.allowSelfConnection = allowSelfConnection;
        }

        public double getNewConnectionMutationProbability() {
            return newConnectionMutationProbability;
        }

        public void setNewConnectionMutationProbability(double newConnectionMutationProbability) {
            this.newConnectionMutationProbability = newConnectionMutationProbability;
        }

        public double getNewNodeMutationProbability() {
            return newNodeMutationProbability;
        }

        public void setNewNodeMutationProbability(double newNodeMutationProbability) {
            this.newNodeMutationProbability = newNodeMutationProbability;
        }

        public double getMaxConnectionStrength() {
            return maxConnectionStrength;
        }

        public void setMaxConnectionStrength(double maxConnectionStrength) {
            this.maxConnectionStrength = maxConnectionStrength;
        }

        public double getMinConnectionStrength() {
            return minConnectionStrength;
        }

        public void setMinConnectionStrength(double minConnectionStrength) {
            this.minConnectionStrength = minConnectionStrength;
        }

        public double getMaxConnectionMutation() {
            return maxConnectionMutation;
        }

        public void setMaxConnectionMutation(double maxConnectionMutation) {
            this.maxConnectionMutation = maxConnectionMutation;
        }

        public double getNodeMaxBiasMutation() {
            return nodeMaxBiasMutation;
        }

        public void setNodeMaxBiasMutation(double nodeMaxBiasMutation) {
            this.nodeMaxBiasMutation = nodeMaxBiasMutation;
        }

        public double getNodeMaxBias() {
            return nodeMaxBias;
        }

        public void setNodeMaxBias(double nodeMaxBias) {
            this.nodeMaxBias = nodeMaxBias;
        }
    }
}
