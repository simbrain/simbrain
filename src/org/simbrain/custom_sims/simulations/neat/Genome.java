package org.simbrain.custom_sims.simulations.neat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
    private ArrayList<ConnectionGene> connectionGenes;

    /**
     * A {@code innovationNumber} to {@link ConnectionGene} map for faster lookup during crossover
     */
    private Map<Integer, ConnectionGene> connectionGeneMap;

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
     *
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
        connectionGeneMap = new TreeMap<>();
        rand = new NEATRandomizer(seed);
        this.pool = requireNonNull(pool);
        fitness = Double.NaN;
        newConnectionMutation();
    }

    /**
     * Construct a copy of genome from an existing genome.
     *
     * @param cpy The genome to copy
     */
    public Genome(Genome cpy) {
        this.nodeGenes = new ArrayList<>();
        for (NodeGene ng : cpy.nodeGenes) {
            this.nodeGenes.add(new NodeGene(ng));
        }

        Map<Integer, ConnectionGene> newMap = new TreeMap<>();
        newMap.putAll(cpy.connectionGeneMap);
        this.connectionGeneMap = newMap;

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
     * Crossover constructor.
     * Construct a new genome by crossing over two existing genmoes.
     *
     * @param g1 parent 1
     * @param g2 parent 2
     */
    public Genome(Genome g1, Genome g2) {
        if (g1.getPool() != g2.getPool()) {
            throw new IllegalStateException("g1 and g2 have to be in the same pool.");
        }

        // make g1 always the more fit parent.
        if (g2.getFitness() > g1.getFitness()) {
            Genome temp = g1;
            g1 = g2;
            g2 = temp;
        }

        this.nodeGenes = new ArrayList<>();

        Genome longerNodeGeneGenome = g1.nodeGenes.size() > g2.nodeGenes.size() ? g1 : g2;

        // It is only okay for now to just pick the longer list to copy, because the neuron update rule is constant.
        // In the future, find an alternative way that takes inconsistent neuron update rules into consideration.
        for (NodeGene ng : longerNodeGeneGenome.nodeGenes) {
            this.nodeGenes.add(new NodeGene(ng));
        }

        connectionGenes = new ArrayList<>();
        connectionGeneMap = new TreeMap<>();

        inNodes = new ArrayList<>();
        for (int in : longerNodeGeneGenome.inNodes) {
            inNodes.add(in);
        }

        outNodes = new ArrayList<>();
        for (int ou : longerNodeGeneGenome.outNodes) {
            outNodes.add(ou);
        }

        this.rand = new NEATRandomizer(g1.rand.nextLong());
        this.pool = g1.pool;
        fitness = Double.NaN;

        Set<Integer> allInnovationNumber = new TreeSet<>(g1.connectionGeneMap.keySet());

        allInnovationNumber.addAll(g2.connectionGeneMap.keySet());

        for (int innov : allInnovationNumber) {
            if (g1.connectionGeneMap.containsKey(innov) && g2.connectionGeneMap.containsKey(innov)) {
                if (rand.nextBoolean()) {
                    addConnectionGene(new ConnectionGene(g1.connectionGeneMap.get(innov)));
                } else {
                    addConnectionGene(new ConnectionGene(g2.connectionGeneMap.get(innov)));
                }
            } else {
                if (g1.connectionGeneMap.containsKey(innov)) {
                    addConnectionGene(new ConnectionGene(g1.connectionGeneMap.get(innov)));
                } else {
                    addConnectionGene(new ConnectionGene(g2.connectionGeneMap.get(innov)));
                }
            }
        }
    }

    /**
     * Construct a copy of genome from an existing genome and allow mutation during creation.
     * Useful for creating offspring.
     *
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
     * Crossover and mutate constructor.
     * Construct a new genome by crossing over two existing genmoes and allow mutation during creation.
     *
     * @param g1 parent 1
     * @param g2 parent 2
     * @param mutate indicator for mutation
     */
    public Genome(Genome g1, Genome g2, boolean mutate) {
        this(g1, g2);
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

        ConnectionGene existingConnectionGene = connectionGenes.get(rand.nextInt(connectionGeneMap.size()));
        while (!existingConnectionGene.isEnabled()) {
            existingConnectionGene = connectionGenes.get(rand.nextInt(connectionGeneMap.size()));
        }

        int inNodeIndex = existingConnectionGene.getInNode();
        int outNodeIndex = existingConnectionGene.getOutNode();

        // TODO: add innovation number
        // TODO: consider non-static synapse update rule
        ConnectionGene newCG = new ConnectionGene(inNodeIndex, newNodeIndex, randConnectionStrength());
        pool.assignNextInnovationNumber(newCG);
        addConnectionGene(newCG);
        newCG = new ConnectionGene(newNodeIndex, outNodeIndex, randConnectionStrength());
        pool.assignNextInnovationNumber(newCG);
        addConnectionGene(newCG);
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
        ConnectionGene newCG = new ConnectionGene(inNodeIndex, outNodeIndex, randConnectionStrength());

        // if the new connection gene with the same innovation number already existed in the genome, don't add.
        if (pool.getInnovationNumber(newCG) != null && connectionGeneMap.containsKey(pool.getInnovationNumber(newCG))) {
            return;
        }
        pool.assignNextInnovationNumber(newCG);
        addConnectionGene(newCG);
    }

    /**
     * Apply a mutation to a specific connection that changes the weight strength.
     *
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
     *
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

    /**
     * Add connection gene to both the map and the list.
     *
     * @param cg the connection gene to be added
     */
    private void addConnectionGene(ConnectionGene cg) {
        connectionGeneMap.put(cg.getInnovationNumber(), cg);
        connectionGenes.add(cg);
    }

    public double getFitness() {
        return fitness;
    }

    public Pool getPool() {
        return pool;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    @Override
    public int compareTo(Genome o) {
        return this.fitness.compareTo(o.fitness);
    }

    @Override
    public String toString() {
        String ret = "---- Genome ----\n";
        ret += String.format("Fitness: %.8f", fitness) + "\n";
        ret += "\nNode genes: \n";
        for (NodeGene ng : nodeGenes) {
            ret +=  ng +  "\n";
        }
        ret += "\nConnection genes: \n";
        for (ConnectionGene cg : connectionGenes) {
            ret += cg +  "\n";
        }
        return ret;
    }
}
