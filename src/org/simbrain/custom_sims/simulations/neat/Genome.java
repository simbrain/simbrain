package org.simbrain.custom_sims.simulations.neat;

import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import org.simbrain.custom_sims.simulations.neat.NodeGene.NodeType;
import org.simbrain.custom_sims.simulations.neat.util.NEATRandomizer;
import static org.simbrain.custom_sims.simulations.neat.util.Math.clipping;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

/**
 * This class consists of the list of node genes, connection genes to build a network, and
 * the implementation of genome mutation.
 * @author LeoYulinLi
 *
 */
public class Genome implements Comparable<Genome> {

    /**
     * Randomizer for mutation
     */
    private NEATRandomizer rand;

    /**
     * List of all node genes
     */
    private List<NodeGene> nodeGenes;

    /**
     * List of {@code nodeGenes} indices that can be an in-node of a connection.
     */
    private List<Integer> inNodes;

    /**
     * List of {@code nodeGenes} indices that can be an out-node of a connection.
     */
    private List<Integer> outNodes;

    /**
     * List of all connection genes
     */
    private List<ConnectionGene> connectionGenes;

    /**
     * Fitness of this genome. Default to NaN. To be set after evaluation in {@code Environment}.
     */
    private Double fitness;

    /**
     * The pool this genome belongs.
     */
    private Pool pool;

    /**
     * Construct a new genome.
     * @param inputCount Number of input(sensor) nodes
     * @param outputCount Number of output nodes
     * @param seed Seed for randomizer used in mutation
     * @param pool The pool this genome belongs
     */
    public Genome(int inputCount, int outputCount, long seed, Pool pool) {
        nodeGenes = new ArrayList<>();
        inNodes = new ArrayList<>();
        outNodes = new ArrayList<>();
        for (int i = 0; i < inputCount; i++) {
            nodeGenes.add(new NodeGene(NodeType.input));
            inNodes.add(nodeGenes.size() - 1);
        }
        for (int i = 0; i < outputCount; i++) {
            nodeGenes.add(new NodeGene(NodeType.output));
            outNodes.add(nodeGenes.size() - 1);
        }
        connectionGenes = new ArrayList<>();
        rand = new NEATRandomizer(seed);
        this.pool = requireNonNull(pool);
        fitness = Double.NaN;
        newConnectionMutation();
    }

    /**
     * Construct a copy of genome from an existing genome.
     * @param cpy The genome to copy
     */
    public Genome(Genome cpy) {
        this.nodeGenes = new ArrayList<>();
        for (NodeGene ng : cpy.nodeGenes) {
            this.nodeGenes.add(new NodeGene(ng));
        }

        this.connectionGenes = new ArrayList<>();
        for (ConnectionGene cg : cpy.connectionGenes) {
            this.connectionGenes.add(new ConnectionGene(cg));
        }

        inNodes = new ArrayList<>();
        for (int in : cpy.inNodes) {
            inNodes.add(in);
        }

        outNodes = new ArrayList<>();
        for (int ou : cpy.outNodes) {
            outNodes.add(ou);
        }

        this.rand = new NEATRandomizer(cpy.rand.nextLong());
        this.pool = cpy.pool;
        fitness = Double.NaN;
    }

    /**
     * Construct a copy of genome from an existing genome and allow mutation during creation.
     * Useful for creating offspring.
     * @param cpy The genome to copy
     * @param mutate Indicator for mutation
     */
    public Genome(Genome cpy, boolean mutate) {
        this(cpy);
        if (mutate) {
            mutate();
        }
    }

    /**
     * Mutate the current genome base on the pool config.
     */
    public void mutate() {
        if (rand.nextDouble() < pool.getNewNodeMutationRate()) {
            newNodeMutation();
        }
        if (rand.nextDouble() < pool.getNewConnectionMutationRate()) {
            newConnectionMutation();
        }
        for (ConnectionGene cg : connectionGenes) {
            weightStrengthMutation(cg);
        }
    }

    /**
     * Apply a mutation to current genome that insert a new node into an existing connection.
     */
    private void newNodeMutation() {
        int newNodeIndex = nodeGenes.size();
        NodeGene newNodeGene = new NodeGene(); // TODO: add random update rule
        nodeGenes.add(newNodeGene);
        inNodes.add(nodeGenes.size() - 1);
        outNodes.add(nodeGenes.size() - 1);

        ConnectionGene existingConnectionGene = connectionGenes.get(rand.nextInt(connectionGenes.size()));
        while (!existingConnectionGene.isEnabled()) {
            existingConnectionGene = connectionGenes.get(rand.nextInt(connectionGenes.size()));
        }

        int inNodeIndex = existingConnectionGene.getInNode();
        int outNodeIndex = existingConnectionGene.getOutNode();

        // TODO: add innovation number
        // TODO: consider non-static synapse update rule
        connectionGenes.add(new ConnectionGene(inNodeIndex, newNodeIndex, randConnectionStrength()));
        connectionGenes.add(new ConnectionGene(newNodeIndex, outNodeIndex, randConnectionStrength()));
        existingConnectionGene.setEnabled(false);
    }

    /**
     * Apply a mutation to current genome that connect two existing nodes with a synapse.
     */
    private void newConnectionMutation() {
        int inNodeIndex = inNodes.get(rand.nextInt(inNodes.size()));
        int outNodeIndex = outNodes.get(rand.nextInt(outNodes.size()));

        // TODO: add innovation number
        // TODO: consider non-static synapse update rule
        connectionGenes.add(new ConnectionGene(inNodeIndex, outNodeIndex, randConnectionStrength()));
    }

    /**
     * Apply a mutation to a specific connection that changes the weight strength.
     * @param cg {@code ConnectionGene} to mutate
     */
    private void weightStrengthMutation(ConnectionGene cg) {
        // TODO: add innovation number
        // TODO: consider non-static synapse update rule
        cg.setWeightStrength(
            clipping(
                randConnectionStrength() * pool.getConnectionStrengthMutationAmplitude() + cg.getWeightStrength(),
                pool.getConnectionStrengthFloor(),
                pool.getConnectionStrengthCeiling()
            )
        );
    }

    /**
     * A helper function to generate a double value based on the pool config for assigning to connection strength.
     * @return A randomly generated connection strength
     */
    private double randConnectionStrength() {
        return rand.nextDouble(pool.getConnectionStrengthFloor(), pool.getConnectionStrengthCeiling());
    }

    /**
     * Construct a network from the genome.
     * @return The network that this genome encoded
     */
    public Network buildNetwork() {
        Network net = new Network();
        for (NodeGene n : nodeGenes) {
            Neuron newNeuron = new Neuron(net, n.getUpdateRule());
            if (n.getType() == NodeType.input) {
                newNeuron.setClamped(true);
            }
            net.addNeuron(newNeuron);
        }
        for (ConnectionGene c : connectionGenes) {
            Synapse newConnection = new Synapse(net.getNeuron(c.getInNode()), net.getNeuron(c.getOutNode()));
            newConnection.setStrength(c.getWeightStrength());
            net.addSynapse(newConnection);
        }
        return net;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    @Override
    public int compareTo(Genome o) {
        return this.fitness.compareTo(o.fitness);
    }
}
