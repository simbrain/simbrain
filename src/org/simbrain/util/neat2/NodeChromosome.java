package org.simbrain.util.neat2;

import org.simbrain.network.core.Neuron;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NodeChromosome extends Chromosome<Neuron, NodeChromosome> {


    private List<NodeGene> genes = new ArrayList<>();

    @Override
    public NodeChromosome crossOver(NodeChromosome other) {

        NodeChromosome ret = new NodeChromosome();

        Set<NodeGene> newNodeGeneSet = new HashSet<>();
        newNodeGeneSet.addAll(genes);
        newNodeGeneSet.addAll(other.genes);

        ret.genes = new ArrayList<>(newNodeGeneSet);

        return ret;
    }

    @Override
    public List<NodeGene> getGenes() {
        return genes;
    }

    public NodeChromosome copy() {
        NodeChromosome ret = new NodeChromosome();
        ret.genes = genes.stream()
                .map(NodeGene::copy)
                .collect(Collectors.toList());
        return ret;
    }
}
