package org.simbrain.util.geneticalgorithm.numerical;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class IntegerGenomeTest {

    @Test
    public void testMutation() {

        IntegerGenome ig = new IntegerGenome(10, 1);
        double sumBefore = ig.getChromosome().getGenes().stream().map(IntegerGene::getPrototype).reduce(Integer::sum).get();
        System.out.println(ig.getChromosome());
        ig.setStepSize(5);
        ig.mutate();
        System.out.println(ig.getChromosome());
        double sumAfter = ig.getChromosome().getGenes().stream().map(IntegerGene::getPrototype).reduce(Integer::sum).get();
        // Make sure the genes are mutated to some degree, but not more than what is consistent with the step size
        assertNotEquals(sumBefore, sumAfter);
        assertTrue(Math.abs(sumBefore-sumAfter) <
                10*ig.getChromosome().getGenes().get(0).getStepSize());
    }

    @Test
    public void testBinary() {

        IntegerGenome ig = new IntegerGenome(10, 1);
        ig.setMin(0);
        ig.setMax(1);
        System.out.println(ig.getChromosome());
        ig.mutate();
        System.out.println(ig.getChromosome());
        List<Integer> bits = ig.express();
        bits.forEach(b -> assertTrue((b==0) || (b==1)));

    }


}