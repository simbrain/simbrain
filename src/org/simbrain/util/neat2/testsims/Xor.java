package org.simbrain.util.neat2.testsims;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.neat.NEATRandomizer;
import org.simbrain.util.neat2.NetworkGenome;
import org.simbrain.util.neat2.NetworkBasedAgent;
import org.simbrain.util.neat2.Population;

import java.util.List;

public class Xor {

    public static final double NEW_CONNECTION_MUTATION_PROBABILITY = 0.05;
    public static final double NEW_NODE_MUTATION_PROBABILITY = 0.05;
    public static final double MAX_CONNECTION_STRENGTH = 10;
    public static final double MIN_CONNECTION_STRENGTH = -10;
    public static final double MAX_CONNECTION_MUTATION = 1;

    private Population<NetworkGenome, NetworkBasedAgent> networks;

    public NEATRandomizer randomizer;

    public int popuation;

    public int maxIteration;

    public Xor(long seed, int popuation, int maxIteration) {
        this.randomizer = new NEATRandomizer(seed);
        this.popuation = popuation;
        this.maxIteration = maxIteration;
    }

    public Xor() {
        this(System.nanoTime(), 1000, 1000);
    }

    public static final List<TrainingEntry> TRAINING_SET = List.of(
        new TrainingEntry(List.of(0.0, 0.0), List.of(0.0)),
        new TrainingEntry(List.of(0.0, 1.0), List.of(1.0)),
        new TrainingEntry(List.of(1.0, 0.0), List.of(1.0)),
        new TrainingEntry(List.of(1.0, 1.0), List.of(0.0))
    );

    public static Double eval(NetworkBasedAgent agent) {

        List<Neuron> inputs = ((NeuronGroup) (agent.getAgent().getGroupByLabel("inputs"))).getNeuronList();
        List<Neuron> outputs = ((NeuronGroup) (agent.getAgent().getGroupByLabel("outputs"))).getNeuronList();
        double sse = 0.0;
        for (TrainingEntry trainingEntry : TRAINING_SET) {
            for (int i = 0; i < trainingEntry.getInput().size(); i++) {
                inputs.get(i).forceSetActivation(trainingEntry.getInput().get(i));
            }
            for (int i = 0; i < 50; i++) {
                agent.getAgent().update();
                for (int n = 0; n < trainingEntry.getOutput().size(); n++) {
                    double error = outputs.get(n).getActivation() - trainingEntry.getOutput().get(n);
                    sse += error * error;
                }
            }
        }
        return -sse;
    }

    public void init() {
        networks = new Population<>(1000);
        // TODO: the NetworkGenotype and the fitnessFunction is empty. populate.
        NetworkGenome networkPrototype = new NetworkGenome();
        networkPrototype.addGroup("inputs", 2, false);
        networkPrototype.addGroup("outputs", 1, false);
        networkPrototype.setRandomizer(randomizer);

        NetworkBasedAgent prototype = new NetworkBasedAgent(networkPrototype, Xor::eval);
        networks.populate(prototype);
    }

    public void run() {
        for (int i = 0; i < 1000; i++) {
            double bestFitness = networks.computeNewFitness();
            if (bestFitness > -10) {
                break;
            }
            System.out.printf("Fitness %.2f\n", bestFitness);
            networks.replenish();
        }
    }

    public static void main(String[] args) {
        Xor test = new Xor();
        test.init();
        test.run();
    }

    public static class TrainingEntry {
        private List<Double> input;

        private List<Double> output;

        public TrainingEntry(List<Double> input, List<Double> output) {
            this.input = input;
            this.output = output;
        }

        public List<Double> getInput() {
            return input;
        }

        public List<Double> getOutput() {
            return output;
        }
    }
}
