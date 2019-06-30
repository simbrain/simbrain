package org.simbrain.util.geneticalgorithm;

import org.junit.Test;
import org.simbrain.util.geneticalgorithm.numerical.DoubleGenome;
import org.simbrain.util.geneticalgorithm.numerical.IntegerGenome;

import java.util.List;

import static org.junit.Assert.*;

public class PopulationTest {

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

        population.setEliminationPercent(.5);

        // Computing new fitness eliminates half of 6
        population.computeNewFitness();
        assertEquals(3, population.getAgentList().size());

        // Repleneshing gets population back to 6
        population.replenish();
        assertEquals(6, population.getAgentList().size());
    }

    @Test
    public void testFitness() {

        Population population = new Population<>(6);
        IntegerGenome genome = new IntegerGenome(1,10);
        genome.setStepSize(2);
        Agent integerAgent = new Agent<>(genome,
                a -> Double.valueOf(a.getGenome().getChromosome().getGenes().get(0).getPrototype()));
        population.populate(integerAgent);

        // Initially most fit is 5.
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