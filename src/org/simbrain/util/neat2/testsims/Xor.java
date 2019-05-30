package org.simbrain.util.neat2.testsims;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.math.SimbrainRandomizer;
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

    private Population<NetworkGenome, NetworkBasedAgent> agentPopulation;

    public SimbrainRandomizer randomizer;

    public int populationSize;

    public int maxIteration;

    public Xor(long seed, int popuation, int maxIteration) {
        this.randomizer = new SimbrainRandomizer(seed);
        this.populationSize = popuation;
        this.maxIteration = maxIteration;
    }

    public Xor() {
        this(System.nanoTime(), 200, 1000);
    }

    public static final List<TrainingExample> TRAINING_SET = List.of(
        new TrainingExample(List.of(0.0, 0.0), List.of(0.0)),
        new TrainingExample(List.of(0.0, 1.0), List.of(1.0)),
        new TrainingExample(List.of(1.0, 0.0), List.of(1.0)),
        new TrainingExample(List.of(1.0, 1.0), List.of(0.0))
    );

    public static Double eval(NetworkBasedAgent agent) {

        List<Neuron> inputs = ((NeuronGroup) (agent.getAgent().getGroupByLabel("inputs"))).getNeuronList();
        List<Neuron> outputs = ((NeuronGroup) (agent.getAgent().getGroupByLabel("outputs"))).getNeuronList();
        double sse = 0.0;
        agent.activationRecoding.add("New Generation");
        for (TrainingExample trainingEntry : TRAINING_SET) {
            String inputValues = "input: ";
            for (int i = 0; i < trainingEntry.getInput().size(); i++) {
                inputs.get(i).forceSetActivation(trainingEntry.getInput().get(i));
                inputValues = inputValues + trainingEntry.getInput().get(i) + " ";
            }
            agent.activationRecoding.add("Wait for network to stabilize");
            String output = "output/expected: ";
            // for the network to stabilize
            for (int i = 0; i < 10; i++) {
                agent.getAgent().update();
            }
            for (int i = 0; i < 50; i++) {
                agent.getAgent().update();
                String outputValues = output + "";
                for (int n = 0; n < trainingEntry.getOutput().size(); n++) {
                    double error = outputs.get(n).getActivation() - trainingEntry.getOutput().get(n);
                    outputValues += outputs.get(n).getActivation() + "/" + trainingEntry.getOutput().get(n);
                    sse += error * error;
                }
                agent.activationRecoding.add(outputValues);
            }
        }
        return -sse;
    }

    public void init() {
        agentPopulation = new Population<>(this.populationSize);

        NetworkGenome networkPrototype = new NetworkGenome();
        networkPrototype.addGroup("inputs", 2, false);
        networkPrototype.addGroup("outputs", 1, false);
        networkPrototype.setRandomizer(randomizer);

        NetworkBasedAgent prototype = new NetworkBasedAgent(networkPrototype, Xor::eval);
        agentPopulation.populate(prototype);
    }

    public void run() {
        for (int i = 0; i < 1000; i++) {
            double bestFitness = agentPopulation.computeNewFitness();
            System.out.printf("Fitness %.2f\n", bestFitness);
            if (bestFitness > -5) {
                break;
            }
            agentPopulation.replenish();
        }
    }

    public static void main(String[] args) {
        Xor test = new Xor();
        test.init();
        test.run();
    }

    public static class TrainingExample {
        private List<Double> input;

        private List<Double> output;

        public TrainingExample(List<Double> input, List<Double> output) {
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
