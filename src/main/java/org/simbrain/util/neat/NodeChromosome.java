package org.simbrain.util.neat;

import org.simbrain.network.core.Neuron;
import org.simbrain.util.geneticalgorithm.Chromosome;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A set of node genes and machinery for crossing them over properly.
 */
public class NodeChromosome extends Chromosome<Neuron, NodeChromosome> {

    /**
     * A set of node genes indexed by an integer id.
     */
    private Map<Integer, NodeGene> genes = new TreeMap<>();

    private int maxNodeID;

    @Override
    public NodeChromosome crossOver(NodeChromosome other) {

        NodeChromosome ret = new NodeChromosome();

        // Align the node genes by their integer index and if both exist, (currently), choose
        // the gene from the current chromosome. TODO: Choose randomly in this case
        Set<Integer> nodeIDUnionSet = new HashSet<>();
        nodeIDUnionSet.addAll(genes.keySet());
        nodeIDUnionSet.addAll(other.genes.keySet());
        ret.maxNodeID = nodeIDUnionSet.stream().reduce(Integer::max).orElse(0);
        ret.genes = nodeIDUnionSet.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> {
                            if (genes.containsKey(id)) {
                                return genes.get(id).copy();
                            } else {
                                return other.genes.get(id).copy();
                            }
                        }
                ));

        return ret;
    }

    @Override
    public List<NodeGene> getGenes() {
        return new ArrayList<>(genes.values());
    }

    public void addGene(NodeGene gene) {
        addGenes(Collections.singleton(gene));
    }

    public void addGenes(Collection<NodeGene> nodeGenes) {
        for (NodeGene nodeGene : nodeGenes) {
            genes.put(genes.size(), nodeGene);
        }
        maxNodeID = genes.keySet().stream().reduce(Integer::max).orElse(0);
    }

    public NodeChromosome copy() {
        NodeChromosome ret = new NodeChromosome();
        ret.maxNodeID = maxNodeID;
        ret.genes = genes.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().copy()
                ));
        return ret;
    }

    public boolean contains(int nodeID) {
        return genes.containsKey(nodeID);
    }

    public NodeGene getByID(int nodeID) {
        return genes.get(nodeID);
    }

    public int getMaxNodeID() {
        return maxNodeID;
    }
}
