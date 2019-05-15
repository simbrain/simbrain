package org.simbrain.util.neat2;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.neat.NEATRandomizer;

import java.util.*;
import java.util.stream.Collectors;

public class NetworkGenome extends Genome<Network, NetworkGenome> {

    //TODO: Redo below using chromosomes

    /**
     * A genome of NodeGene
     */
    private NodeChromosome nodeGenes = new NodeChromosome();

    /**
     * A map of innovation numbers to connections genes
     */
    private ConnectionChromosome connectionGenes = new ConnectionChromosome();

    private NEATRandomizer randomizer;


    @Override
    public Network build() {
        Network ret = new Network();
        List<Neuron> neurons = new ArrayList<>();

        nodeGenes.getGenes().forEach(n -> {
            Neuron neuron = new Neuron(ret, n.getPrototype());
            neurons.add(neuron);

            if (n.getNeuronGroupName() != null && !n.getNeuronGroupName().isEmpty()) {
                Group group = ret.getGroupByLabel(n.getNeuronGroupName());
                if (group == null) {
                    NeuronGroup neuronGroup = new NeuronGroup(ret);
                    neuronGroup.setLabel(n.getNeuronGroupName());
                    ret.addGroup(neuronGroup);
                    neuronGroup.addNeuron(neuron);
                } else {
                    if (group instanceof NeuronGroup) {
                        ((NeuronGroup) group).addNeuron(neuron);
                    }
                }
            }
            ret.addNeuron(neuron);
        });

        connectionGenes.getGenes().forEach(c -> {
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

    public NetworkGenome addGroup(String name, int size, boolean mutable) {

        List<NodeGene> toAdd = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            NodeGene nodeGene = new NodeGene();
            nodeGene.setNeuronGroupName(name);
            nodeGene.setMutable(mutable);
            toAdd.add(nodeGene);
        }
        nodeGenes.getGenes().addAll(toAdd);

        return this;
    }



    @Override
    public NetworkGenome crossOver(NetworkGenome otherGenome) {

        NetworkGenome ret = new NetworkGenome();

        ret.nodeGenes = nodeGenes.crossOver(otherGenome.nodeGenes);

        ret.connectionGenes = connectionGenes.crossOver(otherGenome.connectionGenes);

        return ret;
    }


    @Override
    public NetworkGenome copy() {
        NetworkGenome ret = new NetworkGenome();
        ret.nodeGenes = nodeGenes.copy();
        ret.connectionGenes = connectionGenes.copy();
        return ret;
    }
}
