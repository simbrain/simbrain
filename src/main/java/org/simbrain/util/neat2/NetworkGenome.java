package org.simbrain.util.neat2;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.geneticalgorithm.Genome;
import org.simbrain.util.neat2.testsims.Xor;

import java.util.*;

public class NetworkGenome extends Genome<Network, NetworkGenome> {

    private static Map<ConnectionGene, Integer> innovationNumberMap = new HashMap<>();

    /**
     * A genome of NodeGene
     */
    private NodeChromosome nodeGenes = new NodeChromosome();

    /**
     * A map of innovation numbers to connections genes
     */
    private ConnectionChromosome connectionGenes = new ConnectionChromosome();


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
            if ("inputs".equals(name)) {
                nodeGene.setType(NodeGene.NodeType.input);
                nodeGene.getPrototype().setClamped(true);
            }
            if ("outputs".equals(name)) {
                nodeGene.setType(NodeGene.NodeType.output);
            }
            toAdd.add(nodeGene);
        }
        nodeGenes.addGenes(toAdd);

        return this;
    }

    @Override
    public NetworkGenome crossOver(NetworkGenome otherGenome) {

        NetworkGenome ret = new NetworkGenome();

        ret.inheritRandomizer(getRandomizer());

        ret.nodeGenes = nodeGenes.crossOver(otherGenome.nodeGenes);

        ret.connectionGenes = connectionGenes.crossOver(otherGenome.connectionGenes);
        ret.connectionGenes.setRandomizer(ret.getRandomizer());

        return ret;
    }



    @Override
    public void mutate() {

        // new node mutation
        if (getRandomizer().nextDouble(0, 1) < Xor.NEW_NODE_MUTATION_PROBABILITY) {
            nodeGenes.addGenes(Collections.singleton(new NodeGene()));
        }

        // connection weight mutation
        connectionGenes.mutate();

        // new connection mutation
        // needs to be done here because it has dependency on the node genes
        if (getRandomizer().nextDouble(0, 1) < Xor.NEW_CONNECTION_MUTATION_PROBABILITY) {
            int sourceNodeID = getRandomizer().nextInt(nodeGenes.getMaxNodeID() + 1);
            while (!nodeGenes.contains(sourceNodeID) || nodeGenes.getByID(sourceNodeID).getType() == NodeGene.NodeType.output) {
                sourceNodeID = getRandomizer().nextInt(nodeGenes.getMaxNodeID() + 1);
            }
            int destinationNodeID = getRandomizer().nextInt(nodeGenes.getMaxNodeID() + 1);
            while (!nodeGenes.contains(destinationNodeID) || nodeGenes.getByID(destinationNodeID).getType() == NodeGene.NodeType.input) {
                destinationNodeID = getRandomizer().nextInt(nodeGenes.getMaxNodeID() + 1);
            }
            ConnectionGene newConnectionGene =
                    new ConnectionGene(
                            sourceNodeID,
                            destinationNodeID,
                            getRandomizer().nextDouble(
                                    Xor.MIN_CONNECTION_STRENGTH,
                                    Xor.MAX_CONNECTION_STRENGTH
                            ));
            if (!innovationNumberMap.containsKey(newConnectionGene)) {
                innovationNumberMap.put(newConnectionGene, innovationNumberMap.size());
            }
            newConnectionGene.setRandomizer(connectionGenes.getRandomizer());
            connectionGenes.addGene(innovationNumberMap.get(newConnectionGene), newConnectionGene);
        }
    }

    @Override
    public NetworkGenome copy() {
        NetworkGenome ret = new NetworkGenome();

        ret.inheritRandomizer(getRandomizer());

        ret.nodeGenes = nodeGenes.copy();

        ret.connectionGenes = connectionGenes.copy();
        ret.connectionGenes.setRandomizer(ret.getRandomizer());

        return ret;
    }
}
