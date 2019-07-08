package org.simbrain.custom_sims.simulations.test;

import org.simbrain.network.connections.RadialSimple;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.util.geneticalgorithm.Genome;
import org.simbrain.util.geneticalgorithm.numerical.DoubleChromosome;
import org.simbrain.util.geneticalgorithm.numerical.DoubleGene;
import org.simbrain.util.geneticalgorithm.numerical.IntegerChromosome;
import org.simbrain.util.geneticalgorithm.numerical.IntegerGene;

/**
 * Represent a network using integers to determine number and type
 *  of neurons and doubles to represent connectivity pattern
 *  and layout.
 */
class SimpleNetGenome extends Genome<SimpleNetGenome, Network> {

    /**
     * Integer genes.
     */
    private IntegerChromosome intChromosome = new IntegerChromosome(2);

    /**
     * Dobule genes.
     */
    private DoubleChromosome doubleChromosome = new DoubleChromosome(5);

    /**
     * Set up all the chromosome min, max, and default values.
     */
    public SimpleNetGenome() {
        ((IntegerGene)intChromosome.getGene(0)).setValue(20);
        ((IntegerGene)intChromosome.getGene(0)).setMinimum(5);
        ((IntegerGene)intChromosome.getGene(0)).setMaximum(100);
        ((IntegerGene)intChromosome.getGene(1)).setMinimum(0);
        ((IntegerGene)intChromosome.getGene(1)).setMaximum(4);

        ((DoubleGene)doubleChromosome.getGene(0)).setMinimum(100);
        ((DoubleGene)doubleChromosome.getGene(0)).setMaximum(400);
        ((DoubleGene)doubleChromosome.getGene(1)).setMinimum(0);
        ((DoubleGene)doubleChromosome.getGene(1)).setMaximum(1);
        ((DoubleGene)doubleChromosome.getGene(2)).setMinimum(100);
        ((DoubleGene)doubleChromosome.getGene(2)).setMaximum(300);
        ((DoubleGene)doubleChromosome.getGene(3)).setMinimum(0);
        ((DoubleGene)doubleChromosome.getGene(3)).setMaximum(1);
        ((DoubleGene)doubleChromosome.getGene(4)).setMinimum(100);
        ((DoubleGene)doubleChromosome.getGene(4)).setMaximum(300);

    }

    @Override
    public SimpleNetGenome crossOver(SimpleNetGenome other) {
        SimpleNetGenome ret = new SimpleNetGenome();
        ret.intChromosome = this.intChromosome.crossOver(other.intChromosome);
        ret.doubleChromosome = this.doubleChromosome.crossOver(other.doubleChromosome);
        return ret;
    }

    @Override
    public void mutate() {
        intChromosome.mutate();
        doubleChromosome.mutate();
    }

    @Override
    public SimpleNetGenome copy() {
        SimpleNetGenome ret = new SimpleNetGenome();
        ret.intChromosome = this.intChromosome.copy();
        ret.doubleChromosome = this.doubleChromosome.copy();
        return ret;
    }

    @Override
    public Network express() {

        Network network = new Network();

        // The basic description of the network using a chromosome
        // Currently the emphasis is on networks with radial connections
        int numNeurons = intChromosome.getGene(0).getPrototype();
        int neuronType = intChromosome.getGene(1).getPrototype();

        double gridSpace = doubleChromosome.getGene(0).getPrototype();
        double inhibProb = doubleChromosome.getGene(1).getPrototype();
        double inhibRadius = doubleChromosome.getGene(2).getPrototype();
        double excitProb = doubleChromosome.getGene(3).getPrototype();
        double excitRadius = doubleChromosome.getGene(4).getPrototype();

        // Build the network based on the current chromosome
        for (int i = 0; i < numNeurons; i++) {
            String type = getNeuron(neuronType);
            Neuron neuron = new Neuron(network, type);
            neuron.setLowerBound(-10);
            neuron.setUpperBound(10);
            network.addNeuron(neuron);
        }
        GridLayout layout = new GridLayout(gridSpace, gridSpace, (int) Math
                .sqrt(numNeurons));
        layout.layoutNeurons(network.getFlatNeuronList());

        RadialSimple connection = new RadialSimple(network, network.getFlatNeuronList());
        connection.setExcitatoryProbability(excitProb);
        connection.setInhibitoryProbability(inhibProb);
        connection.setExcitatoryRadius(excitRadius);
        connection.setInhibitoryRadius(inhibRadius);
        connection.connectNeurons(true);

        // Currently, the default behavior is to randomize the neurons of a
        // network and update it for a set number of iterations.
        network.randomizeNeurons();
        for (int i = 0; i < 50; i++) {
            network.update();
        }

        return network;
    }

    public IntegerChromosome getIntChromosome() {
        return intChromosome;
    }

    public DoubleChromosome getDoubleChromosome() {
        return doubleChromosome;
    }

    /**
     * Helper method which returns a neuron type based on an integer value. More
     * options can obviously be added here.
     *
     * @param neuronType
     *            integer value
     * @return a neuron of the corresponding type
     */
    private String getNeuron(int neuronType) {

        switch (neuronType) {
            case 0:
                return "LinearRule";
            case 1:
                return "BinaryRule";
            case 2:
                return "SigmoidalRule";
            case 3:
                return "DecayRule";
            default:
                return "NakaRushtonRule";
        }
    }




}
