package org.simbrain.util.geneticalgorithm.numerical;

import org.junit.Test;

import static org.junit.Assert.*;

public class IntegerChromosomeTest {


    @Test
    public void testMutation() {
        IntegerChromosome ic = new IntegerChromosome(10, 1);
        double sumBefore = ic.getGenes().stream().map(IntegerGene::getPrototype).reduce(Integer::sum).get();
        System.out.println(ic);
        ic.mutate();
        System.out.println(ic);
        double sumAfter = ic.getGenes().stream().map(IntegerGene::getPrototype).reduce(Integer::sum).get();
        // Make sure the genes are mutated to some degree, but not more than what is consistent with the step size
        assertNotEquals(sumBefore, sumAfter);
        assertTrue(Math.abs(sumBefore- sumAfter) < 10*ic.getGenes().get(0).getStepSize());
    }

}