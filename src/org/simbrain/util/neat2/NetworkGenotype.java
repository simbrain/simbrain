package org.simbrain.util.neat2;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.neat.NEATRandomizer;

import java.util.*;
import java.util.stream.Collectors;

public class NetworkGenotype extends Genotype<Network, NetworkGenotype> {

    /**
     * A genome of NodeGene
     */
    private List<NodeGene> nodeGenes;

    /**
     * A map of innovation numbers to connections genes
     */
    private Map<Integer, ConnectionGene> connectionGenes = new TreeMap<>();

    private NEATRandomizer randomizer;

    /**
     * Creating a network from this genotype.
     *
     * @return The network this genotype encodes
     */
    public Network assemble() {
        Network ret = new Network();
        List<Neuron> neurons = new ArrayList<>();

        nodeGenes.forEach(n -> {
            Neuron neuron = new Neuron(ret, n.getPrototype());
            neurons.add(neuron);

            if (n.getNeuronGroupName() != null && !n.getNeuronGroupName().isEmpty()) {
                Group group = ret.getGroup(n.getNeuronGroupName());
                if (group == null) {
                    NeuronGroup neuronGroup = new NeuronGroup(ret);
                    neuronGroup.setLabel(n.getNeuronGroupName());
                    ret.addGroup(neuronGroup);
                } else {
                    if (group instanceof NeuronGroup) {
                        ((NeuronGroup) group).addNeuron(neuron);
                    }
                }
            }
            ret.addNeuron(neuron);
        });

        connectionGenes.values().forEach(c -> {
            Synapse synapse = new Synapse(
                    ret,
                    neurons.get(c.getSourceIndex()),
                    neurons.get(c.getTargetIndex()),
                    c.getPrototype().getLearningRule(),
                    c.getPrototype()
            );
            ret.addSynapse(synapse);
        });

        return ret;
    }

    public NetworkGenotype crossOver(NetworkGenotype other) {

        NetworkGenotype ret = new NetworkGenotype();

        Set<NodeGene> newNodeGeneSet = new HashSet<>();
        newNodeGeneSet.addAll(nodeGenes);
        newNodeGeneSet.addAll(other.nodeGenes);

        ret.nodeGenes = new ArrayList<>(newNodeGeneSet);

        Set<Integer> allInnovationNumbers = new TreeSet<>(this.connectionGenes.keySet());
        allInnovationNumbers.addAll(other.connectionGenes.keySet());

        for (Integer i : allInnovationNumbers) {
            if (this.connectionGenes.containsKey(i) && other.connectionGenes.containsKey(i)) {
                double decision = randomizer.nextDouble(0, 1);
                if (decision < 0.5) {
                    ret.connectionGenes.put(i, this.connectionGenes.get(i));
                } else {
                    ret.connectionGenes.put(i, other.connectionGenes.get(i));
                }
            } else if (this.connectionGenes.containsKey(i)) {
                ret.connectionGenes.put(i, this.connectionGenes.get(i));
            } else {
                ret.connectionGenes.put(i, other.connectionGenes.get(i));
            }
        }


        return ret;
    }

    @Override
    public NetworkGenotype copy() {
        NetworkGenotype ret = new NetworkGenotype();
        ret.nodeGenes = nodeGenes.stream()
                        .map(NodeGene::copy)
                        .collect(Collectors.toList());
        ret.connectionGenes = connectionGenes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy()));
        return ret;
    }
}
