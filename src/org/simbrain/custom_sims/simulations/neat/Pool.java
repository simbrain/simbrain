package org.simbrain.custom_sims.simulations.neat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.simbrain.custom_sims.simulations.neat.util.NEATRandomizer;

import static java.util.Objects.requireNonNull;
import static org.simbrain.custom_sims.simulations.neat.util.Math.probClipping;
import static org.simbrain.custom_sims.simulations.neat.util.Math.assertBound;

/**
 * This class consists of the config for evolution, status of the evolution, and the implementation
 * of the evolution process after evaluation in {@code Environment}.
 * @author LeoYulinLi
 *
 */
public class Pool {

    /**
     * Pool State. Used to check before the execution of some method. Will be used for GUI.
     *
     * TODO: Add more docs to explain these.
     */
    public enum PoolState { newGen, evaluated, sorted, eliminated };

    /**
     * Randomizer for mutation
     */
    private NEATRandomizer rand;

    /**
     * The current generation number of this pool.
     */
    private int generation;

    /**
     * Current pool state.
     */
    private PoolState poolState;

    /**
     * A collection of all genomes of this pool.
     */
    private List<Genome> genomes;

    /**
     * Number of genomes in this pool.
     */
    private int instanceCount;

    /**
     * The percentage of the population to eliminateLeastFit at each new generation.
     * Ranging from 0 to 1.
     */
    private double eliminationRate = 0.5;

    /**
     * The probability of applying newNodeMutation on a offspring genome during a new generation.
     * Ranging from 0 to 1.
     */
    private double newNodeMutationRate = 0.05;

    /**
     * The probability of applying newConnectionMutation on a offspring genome during a new generation.
     * Ranging from 0 to 1.
     */
    private double newConnectionMutationRate = 0.05;

    /**
     * The range of the strength can change during a connectionStrengthMutation.
     * Use as in {@code newStrength = currentStrength ± randConnectionStrength() * connectionStrengthMutationAmplitude}
     * Ranging from 0 to 1.
     */
    private double connectionStrengthMutationAmplitude = 0.1;

    /**
     * The minimum value a connection strength can have
     */
    private double connectionStrengthFloor = -10;

    /**
     * The maximum value a connections strength can have
     */
    private double connectionStrengthCeiling = 10;

    /**
     * The method to set the fitness score of the agent being evaluated.
     */
    private Consumer<Agent> evaluationMethod;

    /**
     * Construct a pool based on input output count with a specified seed.
     * @param inputCount Number of input nodes
     * @param outputCount Number os output nodes
     * @param seed Seed for randomizer
     * @param instanceCount Number of genomes
     * @param evaluationMethod The method to set the fitness score
     */
    public Pool(int inputCount, int outputCount, long seed, int instanceCount,
            Consumer<Agent> evaluationMethod) {
        rand = new NEATRandomizer(seed);
        this.instanceCount = instanceCount;
        genomes = new ArrayList<>();
        for (int i = 0; i < instanceCount; i++) {
            Genome newGenome = new Genome(inputCount, outputCount, rand.nextLong(), this);
            genomes.add(newGenome);
        }
        poolState = PoolState.newGen;
        setEvaluationMethod(evaluationMethod);
    }

    /**
     * Construct a pool based on input output count with automatically generated seed.
     * @param inputCount Number of input nodes
     * @param outputCount Number os output nodes
     * @param instanceCount Number of genomes
     * @param evaluationMethod The method to set the fitness score
     */
    public Pool(int inputCount, int outputCount, int instanceCount,
            Consumer<Agent> evaluationMethod) {
        this(inputCount, outputCount, System.currentTimeMillis(), instanceCount, evaluationMethod);
    }


    /**
     * Run the evolutionary algorithm on this pool.
     *
     * @param maxGenerations give up after this many generations
     * @param threshold if the top agent's fitness is above this value, stop the evolutionary process.
     *
     * @return top genome
     */
    public Genome evolve(int maxGenerations, double threshold) {

        for (int i = 0; i < maxGenerations; i++) {
            evaluate();

            sort();

            // Early termination if fitness is above threshold
            if (getTopGenome().getFitness() > threshold) {
                return getTopGenome();
            }

            // Eliminate the least fit
            eliminateLeastFit();

            // Refill pool using mutations of the most fit
            replenishPool();

        }
        return getTopGenome();
    }


    /**
     * Create agents from genomes and determine their fitness.
     */
    public void evaluate() {
        if (poolState == PoolState.evaluated) {
            return;
        }
        assertPoolState(PoolState.newGen);
        List<Agent> agents = new ArrayList<>();

        // Create agents from genomes
        for (Genome g : genomes) {
            agents.add(new Agent(g));
        }

        // Set fitness for all agents
        agents.parallelStream()
            .forEach(agent -> {
                evaluationMethod.accept(agent);
            });
        poolState = PoolState.evaluated;
    }

    /**
     * Sort the genomes base on fitness score in descending order.
     */
    public void sort() {
        if (poolState == PoolState.sorted) {
            return;
        }
        assertPoolState(PoolState.evaluated);
        Collections.sort(genomes, Comparator.reverseOrder());
        poolState = PoolState.sorted;
    }

    /**
     * Eliminate the least fit genomes base on the pool {@code eliminationRate} config.
     */
    public void eliminateLeastFit() {
        if (poolState == PoolState.eliminated) {
            return;
        }
        assertPoolState(PoolState.sorted);
        genomes = genomes.stream()
                .limit((long) (genomes.size() * (1.0 - eliminationRate)))
                .collect(Collectors.toList());
        poolState = PoolState.eliminated;
    }

    /**
     * Reproduce genomes to fill the population.
     * Currently the reproduction is simply making a mutated offspring from the successors.
     */
    public void replenishPool() {
        assertPoolState(PoolState.eliminated);
        int remainingPopulation = genomes.size();
        int reproduceSize = instanceCount - remainingPopulation;
        for (int i = 0; i < reproduceSize; i++) {
            genomes.add(new Genome(genomes.get(rand.nextInt(remainingPopulation)), true));
        }
        poolState = PoolState.newGen;
        generation += 1;
    }

    /**
     * Get the genome with the highest fitness score.
     * @return The genome with the highest fitness score
     */
    public Genome getTopGenome() {
        if (poolState == PoolState.evaluated) {
            sort();
        }
        return genomes.get(0);
    }

    /**
     * Check if poolState is the expected state.
     * @param ps The expected state.
     */
    private void assertPoolState(PoolState ps) {
        if (poolState != ps) {
            throw new IllegalStateException("Invalid PoolState: " + poolState + "; expecting: " + ps + ".");
        }
    }

    public int getGeneration() {
        return generation;
    }

    public PoolState getPoolState() {
        return poolState;
    }

    public List<Genome> getGenomes() {
        return genomes;
    }

    public double getEliminationRate() {
        return eliminationRate;
    }

    public void setEliminationRate(double eliminationRate) {
        this.eliminationRate = probClipping(eliminationRate);
    }

    public double getNewNodeMutationRate() {
        return newNodeMutationRate;
    }

    public void setNewNodeMutationRate(double newNodeMutationRate) {
        this.newNodeMutationRate = probClipping(newNodeMutationRate);
    }

    public double getNewConnectionMutationRate() {
        return newConnectionMutationRate;
    }

    public void setNewConnectionMutationRate(double newConnectionMutationRate) {
        this.newConnectionMutationRate = probClipping(newConnectionMutationRate);
    }

    public double getConnectionStrengthMutationAmplitude() {
        return connectionStrengthMutationAmplitude;
    }

    public void setConnectionStrengthMutationAmplitude(double connectionStrengthMutationAmplitude) {
        this.connectionStrengthMutationAmplitude = probClipping(connectionStrengthMutationAmplitude);
    }

    public double getConnectionStrengthFloor() {
        return connectionStrengthFloor;
    }

    /**
     * Setting the minimum value of connection strength.
     * @param connectionStrengthFloor The lower bound to set
     */
    public void setConnectionStrengthFloor(double connectionStrengthFloor) {
        assertBound(connectionStrengthFloor, this.connectionStrengthCeiling);
        this.connectionStrengthFloor = connectionStrengthFloor;
    }

    public double getConnectionStrengthCeiling() {
        return connectionStrengthCeiling;
    }

    /**
     * Setting the maximum value of connection strength.
     * @param connectionStrengthCeiling The upper bound to set
     */
    public void setConnectionStrengthCeiling(double connectionStrengthCeiling) {
        assertBound(this.connectionStrengthFloor, connectionStrengthCeiling);
        this.connectionStrengthCeiling = connectionStrengthCeiling;
    }

    /**
     * Setting the minimum and maximum value of connection strength at the same time.
     * Mainly here to avoid unintentionally trigger of the bound check due to invalid old value.
     * @param connectionStrengthFloor The lower bound to set
     * @param connectionStrengthCeiling The upper bound to set
     */
    public void setconnectionStrengthBound(double connectionStrengthFloor, double connectionStrengthCeiling) {
        assertBound(this.connectionStrengthFloor, connectionStrengthCeiling);
        this.connectionStrengthFloor = connectionStrengthFloor;
        this.connectionStrengthCeiling = connectionStrengthCeiling;
    }

    public Consumer<Agent> getEvaluationMethod() {
        return evaluationMethod;
    }

    public void setEvaluationMethod(Consumer<Agent> evaluationMethod) {
        this.evaluationMethod = requireNonNull(evaluationMethod);
    }
}
