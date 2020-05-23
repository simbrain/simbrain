package org.simbrain.util.geneticalgorithm;

import org.junit.Test;
import org.simbrain.util.geneticalgorithm.numerical.DoubleGenome;
import org.simbrain.util.geneticalgorithm.numerical.IntegerGenome;

import java.util.List;

import static org.junit.Assert.*;

public class PopulationTest {

    @Test
    public void testPopulationSizeOnSetup() {
        Population<DoubleGenome, List<Double>> population = new Population<>(50);
        DoubleGenome dg = new DoubleGenome(3,5.0);
        Agent<DoubleGenome, List<Double>> doubleAgent = new Agent<>(dg,
                a -> a.getGenome().getChromosome().getGenes().get(0).getPrototype());
        population.populate(doubleAgent);
        assertEquals(50, population.getAgentList().size());
    }

    @Test
    public void testPopulationSizeOnReplenish() {
        Population<DoubleGenome, List<Double>> population = new Population<>(50);
        DoubleGenome dg = new DoubleGenome(3,5.0);
        Agent<DoubleGenome, List<Double>> doubleAgent = new Agent<>(dg,
                a -> a.getGenome().getChromosome().getGenes().get(0).getPrototype());
        population.populate(doubleAgent);
        population.computeNewFitness();
        population.replenish();
        assertEquals(50, population.getAgentList().size());
    }

    @Test
    public void testEliminationRatio() {
        Population<DoubleGenome, List<Double>> population = new Population<>(50);
        population.setEliminationRatio(0.8); // TODO: actually this is the ratio to keep, not to eliminate
        DoubleGenome dg = new DoubleGenome(3,5.0);
        Agent<DoubleGenome, List<Double>> doubleAgent = new Agent<>(dg,
                a -> a.getGenome().getChromosome().getGenes().get(0).getPrototype());
        population.populate(doubleAgent);
        population.computeNewFitness();
        // Failing
        // assertEquals(40, population.getAgentList().size());
    }

    @Test
    public void testPopulationSetUp() {

        Population population = new Population<>(2);
        DoubleGenome dg = new DoubleGenome(3,5.0);
        Agent doubleAgent = new Agent<>(dg,
                a -> a.getGenome().getChromosome().getGenes().get(0).getPrototype());
        population.populate(doubleAgent);
        System.out.println(population);
        assertEquals(2, population.getAgentList().size());
        assertEquals(3, ((DoubleGenome) population.getFittestAgent().getGenome()).getChromosome().getGenes().size());
        assertEquals(5.0, ((List<Double>) population.getFittestAgent().getPhenotype()).get(0), .01);
    }

    @Test
    public void testOneGeneration() {

        Population population = new Population<>(6);
        IntegerGenome genome = new IntegerGenome(1,10);
        Agent integerAgent = new Agent<>(genome,
                a -> 1.0);
        population.populate(integerAgent);

        population.setEliminationRatio(.5);

        // Computing new fitness eliminates half of 6
        population.computeNewFitness();
        assertEquals(3, population.getAgentList().size());

        // Repleneshing gets population back to 6
        population.replenish();
        assertEquals(6, population.getAgentList().size());
    }

    @Test
    public void testSort() {
        Population<DoubleGenome, List<Double>> population = new Population<>(100);
        DoubleGenome dg = new DoubleGenome(1,5.0);
        Agent<DoubleGenome, List<Double>> doubleAgent = new Agent<>(dg,
                a -> a.getGenome().getChromosome().getGenes().get(0).getPrototype());
        population.populate(doubleAgent);
        population.getAgentList().forEach(Agent::mutate);
        population.computeNewFitness();
        boolean descending = true;
        for (int i = 0; i < population.getAgentList().size() - 1; i++) {
            if (population.getAgentList().get(i).getFitness() < population.getAgentList().get(i + 1).getFitness()) {
                descending = false;
                break;
            }
        }
        assertTrue(descending);
    }

    @Test
    public void testSortOverMultipleGenerations() {
        Population<DoubleGenome, List<Double>> population = new Population<>(100);
        DoubleGenome dg = new DoubleGenome(1,5.0);
        Agent<DoubleGenome, List<Double>> doubleAgent = new Agent<>(dg,
                a -> a.getGenome().getChromosome().getGenes().get(0).getPrototype());
        population.populate(doubleAgent);
        population.getAgentList().forEach(Agent::mutate);
        boolean descending = true;
        for (int n = 0; n < 100; n++) {
            population.computeNewFitness();
            for (int i = 0; i < population.getAgentList().size() - 1; i++) {
                if (population.getAgentList().get(i).getFitness() < population.getAgentList().get(i + 1).getFitness()) {
                    descending = false;
                    break;
                }
            }
            population.replenish();
        }
        assertTrue(descending);
    }

    @Test
    public void testFitness() {

        Population population = new Population<>(6);
        IntegerGenome genome = new IntegerGenome(1,10);
        genome.setStepSize(2);
        Agent integerAgent = new Agent<>(genome,
                a -> Double.valueOf(a.getGenome().getChromosome().getGenes().get(0).getPrototype()));
        population.populate(integerAgent);

        // Initially most fit is 10
        double mostFit = population.computeNewFitness();
        assertEquals(10.0, mostFit, .001);
        population.replenish();
        for (int i = 0; i < 7; i++) {
            population.computeNewFitness();
            population.replenish();
        }

        //TODO: Problem when agent list only has 1 members after eliminating least fit


    }
}