package org.simbrain.util.neat2;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
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

    // TODO
    int numInputs = 2;
    int numOutputs = 1;

    public NetworkGenome() {

        // Set up default input and output genes
        for (int i = 0; i < numInputs; i++) {
            NodeGene nodeGene = new NodeGene();
            nodeGene.setMutable(false);
            nodeGene.setType(NodeGene.NodeType.input);
            nodeGene.getPrototype().setIncrement(1);
            nodeGene.getPrototype().setClamped(true);
            nodeGenes.addGene(nodeGene);
        }
        for (int i = 0; i < numOutputs; i++) {
            NodeGene nodeGene = new NodeGene();
            nodeGene.setMutable(false);
            nodeGene.setType(NodeGene.NodeType.output);
            nodeGenes.addGene(nodeGene);
        }
    }

    @Override
    public Network build() {

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

        NetworkGenome ret = new NetworkGenome();

        ret.inheritRandomizer(getRandomizer());

        ret.nodeGenes = nodeGenes.crossOver(otherGenome.nodeGenes);

        ret.connectionGenes = connectionGenes.crossOver(otherGenome.connectionGenes);
        ret.connectionGenes.setRandomizer(ret.getRandomizer());

        return ret;
    }


    @Override
    public void mutate() {

        // TODO: Decouple from Xor

        // New node mutation
        if (getRandomizer().nextDouble(0, 1) < Xor.NEW_NODE_MUTATION_PROBABILITY) {
            nodeGenes.addGenes(Collections.singleton(new NodeGene()));
        }

        // Connection weight mutation
        connectionGenes.mutate();

        // New connection mutation
        // TODO: Disallow self connections
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
