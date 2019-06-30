package org.simbrain.util.geneticalgorithm.numerical;

import org.junit.Test;

import static org.junit.Assert.*;

public class DoubleChromosomeTest {

    @Test
    public void testCrossoverSize() {

        DoubleChromosome mom = new DoubleChromosome(5);
        DoubleChromosome dad = new DoubleChromosome(5);
        DoubleChromosome child = new DoubleChromosome(0);

        NumericalGeneticAlgUtils.singlePointCrossover(mom, dad,child);
        assertEquals(5, child.getGenes().size());

        dad = new DoubleChromosome(10);
        child = new DoubleChromosome(0);
        NumericalGeneticAlgUtils.singlePointCrossover(mom, dad,child);
        assertTrue(child.getGenes().size() == 5 || child.getGenes().size() == 10);
    }

    @Test
    public void testCrossoverValues() {
        DoubleChromosome mom = new DoubleChromosome(10, 1.0);
        DoubleChromosome dad = new DoubleChromosome(10, 2.0);
        DoubleChromosome child = new DoubleChromosome(0);

        NumericalGeneticAlgUtils.singlePointCrossover(mom, dad,child);
        //System.out.println(child);
        double sum = child.getGenes().stream().map(DoubleGene::getPrototype).reduce(Double::sum).get();
        assertTrue(sum >= 11);
        assertTrue(sum <= 19);
    }

    @Test
    public void testMutation() {
        DoubleChromosome dc = new DoubleChromosome(10, 1.0);
        double sumBefore = dc.getGenes().stream().map(DoubleGene::getPrototype).reduce(Double::sum).get();
        dc.mutate();
        double sumAfter = dc.getGenes().stream().map(DoubleGene::getPrototype).reduce(Double::sum).get();

        // Make sure the genes are mutated to some degree, but not more than what is consistent with the step size
        assertNotEquals(sumBefore, sumAfter);
        assertTrue(Math.abs(sumBefore- sumAfter) < 10*dc.getGenes().get(0).getStepSize());
    }

    @Test
    public void testValidCopy() {
        DoubleChromosome dc = new DoubleChromosome(5);
        DoubleChromosome copy = dc.copy();

        assertEquals(dc.getGenes().size(), copy.getGenes().size());
        assertNotEquals(copy, dc);
    }

}