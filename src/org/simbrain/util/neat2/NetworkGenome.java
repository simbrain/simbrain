package org.simbrain.util.neat2;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.neat.NEATRandomizer;

import java.util.*;
import java.util.stream.Collectors;

public class NetworkGenome extends Genome<Network> {

    //TODO: Redo below using chromosomes

    /**
     * A genome of NodeGene
     */
    private List<NodeGene> nodeGenes;

    /**
     * A map of innovation numbers to connections genes
     */
    private Map<Integer, ConnectionGene> connectionGenes = new TreeMap<>();

    private NEATRandomizer randomizer;


    @Override
    public Network build() {
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

    @Override
    public Genome<Network> crossOver(Genome<Network> otherGenome) {

        NetworkGenome networkGenome = (NetworkGenome)otherGenome;

        NetworkGenome ret = new NetworkGenome();

        Set<NodeGene> newNodeGeneSet = new HashSet<>();
        newNodeGeneSet.addAll(nodeGenes);
        newNodeGeneSet.addAll(networkGenome.nodeGenes);

        ret.nodeGenes = new ArrayList<>(newNodeGeneSet);

        Set<Integer> allInnovationNumbers = new TreeSet<>(this.connectionGenes.keySet());
        allInnovationNumbers.addAll(networkGenome.connectionGenes.keySet());

        for (Integer i : allInnovationNumbers) {
            if (this.connectionGenes.containsKey(i) && networkGenome.connectionGenes.containsKey(i)) {
                double decision = randomizer.nextDouble(0, 1);
                if (decision < 0.5) {
                    ret.connectionGenes.put(i, this.connectionGenes.get(i));
                } else {
                    ret.connectionGenes.put(i, networkGenome.connectionGenes.get(i));
                }
            } else if (this.connectionGenes.containsKey(i)) {
                ret.connectionGenes.put(i, this.connectionGenes.get(i));
            } else {
                ret.connectionGenes.put(i, networkGenome.connectionGenes.get(i));
            }
        }

        return ret;
    }


    @Override
    public NetworkGenome copy() {
        NetworkGenome ret = new NetworkGenome();
        ret.nodeGenes = nodeGenes.stream()
                        .map(NodeGene::copy)
                        .collect(Collectors.toList());
        ret.connectionGenes = connectionGenes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy()));
        return ret;
    }
}
