package org.simbrain.util.neat;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.geneticalgorithm.Genome;
import org.simbrain.util.math.SimbrainRandomizer;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    private List<Supplier<NodeChromosome>> externalNodes = new ArrayList<>();

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
            nodeGenes.addGene(nodeGene);
        }
        for (int i = 0; i < configuration.numOutputs; i++) {
            NodeGene nodeGene = new NodeGene();
            nodeGene.setMutable(false);
            nodeGene.setType(NodeGene.NodeType.output);
            nodeGene.setConfiguration(configuration);
            nodeGenes.addGene(nodeGene);
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public NodeChromosome getNodeGenes() {
        return nodeGenes;
    }

    @Override
    public Network express() {

        Network network = new Network();

        List<Neuron> neurons = new ArrayList<>();

        NeuronGroup inputGroup = new NeuronGroup(network);
        NeuronGroup outputGroup = new NeuronGroup(network);

        Map<Integer, NodeGene> combinedNodeGenes = getCombinedNodeGenes();

        Map<Integer, Neuron> phenotypes = new HashMap<>();

        combinedNodeGenes.forEach((innovationNumber, nodeGene) -> {
            Neuron neuron = new Neuron(network, nodeGene.getPrototype());
            phenotypes.put(innovationNumber, neuron);
            neurons.add(neuron);

            if (nodeGene.getType() == NodeGene.NodeType.input) {
                inputGroup.addNeuron(neuron);
                neuron.setClamped(true);
            } else if (nodeGene.getType() == NodeGene.NodeType.output) {
                outputGroup.addNeuron(neuron);
            } else {
                // Set locations based on where the input and outputs are set
                neuron.setX(SimbrainRandomizer.rand.nextDouble(-200, 200));
                neuron.setY(SimbrainRandomizer.rand.nextDouble(0, 300));
                network.addLooseNeuron(neuron);
            }
        });

        network.addNeuronGroup(inputGroup);
        network.addNeuronGroup(outputGroup);
        inputGroup.setLabel("inputs");
        inputGroup.applyLayout();
        inputGroup.offset(0, 300);
        outputGroup.setLabel("outputs");
        outputGroup.applyLayout();
        connectionGenes.getGenes().forEach(connectionGene -> {
            Synapse synapse = new Synapse(
                    network,
                    phenotypes.get(connectionGene.getSourceIndex()),
                    phenotypes.get(connectionGene.getTargetIndex()),
                    connectionGene.getPrototype().getLearningRule(),
                    connectionGene.getPrototype()
            );
            network.addLooseSynapse(synapse);
        });

        return network;
    }

    @Override
    public NetworkGenome crossOver(NetworkGenome otherGenome) {

        NetworkGenome ret = new NetworkGenome(configuration);

        ret.nodeGenes = nodeGenes.crossOver(otherGenome.nodeGenes);

        ret.connectionGenes = connectionGenes.crossOver(otherGenome.connectionGenes);

        return ret;
    }


    @Override
    public void mutate() {

        nodeGenes.mutate();

        // New node mutation
        if (nodeGenes.getGenes().size() < configuration.maxNodes &&
                SimbrainRandomizer.rand.nextDouble(0, 1) < configuration.newNodeMutationProbability) {
            NodeGene newNodeGene = new NodeGene();
            newNodeGene.setConfiguration(configuration);
            //newNodeGene.setRandomizer(nodeGenes.getRandomizer());
            nodeGenes.addGenes(Collections.singleton(newNodeGene));
        }

        // Connection weight mutation
        connectionGenes.mutate();

        // New connection mutation
        if ( SimbrainRandomizer.rand.nextDouble(0, 1) < configuration.newConnectionMutationProbability) {

            // Create a new connection

            Map<Integer, NodeGene> combinedGenes = getCombinedNodeGenes();

            // First, select a source neuron.
            Integer sourceNodeID =  getPotentialSourceNodeID(combinedGenes);
            if (sourceNodeID == null) {
                return;
            }

            // Now select a target neuron
            Integer destinationNodeID =  getPotentialTargetNodeID(combinedGenes);
            if (destinationNodeID == null || (sourceNodeID.equals(destinationNodeID) && configuration.allowSelfConnection)) {
                return;
            }

            // Create the new connection gene
            ConnectionGene newConnectionGene =
                    new ConnectionGene(
                            sourceNodeID,
                            destinationNodeID,
                            SimbrainRandomizer.rand.nextDouble(
                                    configuration.minConnectionStrength,
                                    configuration.maxConnectionStrength
                            ));
            newConnectionGene.setConfiguration(configuration);
            // If this is a new connection gene, add a new innovation number entry.
            // otherwise we are just mutating an existing connection gene
            if (!innovationNumberMap.containsKey(newConnectionGene)) {
                innovationNumberMap.put(newConnectionGene, innovationNumberMap.size());
            }
            //newConnectionGene.setRandomizer(connectionGenes.getRandomizer());
            connectionGenes.addGene(innovationNumberMap.get(newConnectionGene), newConnectionGene);
        }
    }

    @Override
    public NetworkGenome copy() {
        NetworkGenome ret = new NetworkGenome(configuration);
        ret.nodeGenes = nodeGenes.copy();
        ret.connectionGenes = connectionGenes.copy();
        return ret;
    }

    public void addExternalNodeGenes(Supplier<NodeChromosome> nodeGenes) {
        externalNodes.add(nodeGenes);
    }

    public void resetExternalNodeGenes() {
        externalNodes = new ArrayList<>();
    }

    public Map<Integer, NodeGene> getCombinedNodeGenes() {

        HashMap<Integer, NodeGene> combinedGenes = new HashMap<>(nodeGenes.getGeneMap());

        externalNodes.stream()
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .map(NodeChromosome::getGeneMap)
                .forEach(combinedGenes::putAll);

        return combinedGenes;
    }

    private Integer getPotentialSourceNodeID(Map<Integer, NodeGene> genes) {
        List<Integer> temp = genes.entrySet().stream()
                .filter(entry -> entry.getValue().getType() != NodeGene.NodeType.output)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return SimbrainRandomizer.rand.randomPick(
                temp
        );
    }

    private Integer getPotentialTargetNodeID(Map<Integer, NodeGene> genes) {
        return SimbrainRandomizer.rand.randomPick(
                genes.entrySet().stream()
                        .filter(entry -> entry.getValue().getType() != NodeGene.NodeType.input)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList())
        );
    }

    /**
     * These configuration parameters constrain what kind of network can be evolved.
     */
    public static class Configuration {

        private int numInputs;
        private int numOutputs;
        private int maxNodes = Integer.MAX_VALUE;
        private double nodeMaxBiasMutation = 0.1;
        private double nodeMaxBias = 5;
        private boolean allowSelfConnection = true;
        private double newConnectionMutationProbability = 0.05;
        private double newNodeMutationProbability = 0.05;
        private double maxConnectionStrength = 10;
        private double minConnectionStrength = -10;
        private double maxConnectionMutation = .1;
        private boolean allowFreeFloatingNodes = false;
        private double minNeuronActivation = 0;
        private double maxNeuronActivation = 1;

        /**
         * Node gene mutations will select from the the list of neuron update rules
         * included in this list.
         */
        private List<Class> rules = NeuronUpdateRule.getTypes();

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

        public int getMaxNodes() {
            return maxNodes;
        }

        public void setMaxNodes(int maxNodes) {
            this.maxNodes = maxNodes;
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

        public double getMinNeuronActivation() {
            return minNeuronActivation;
        }

        public void setMinNeuronActivation(double minNeuronActivation) {
            this.minNeuronActivation = minNeuronActivation;
        }

        public double getMaxNeuronActivation() {
            return maxNeuronActivation;
        }

        public void setMaxNeuronActivation(double maxNeuronActivation) {
            this.maxNeuronActivation = maxNeuronActivation;
        }

        public List<Class> getRules() {
            return rules;
        }

        public void setRules(List<Class> rules) {
            this.rules = rules;
        }
    }
}
