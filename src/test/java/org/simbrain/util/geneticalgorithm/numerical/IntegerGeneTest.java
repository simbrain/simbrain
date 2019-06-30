package org.simbrain.util.geneticalgorithm.numerical;

import org.junit.Test;

import static org.junit.Assert.*;

public class IntegerGeneTest {

    @Test
    public void testMutationMinMax() {

        IntegerGene dg = new IntegerGene(5);
        dg.setMinimum(-10);
        dg.setMaximum(10);
        dg.setStepSize(1);
        for (int i = 0; i < 100 ; i++) {
            dg.mutate();
        }
        assertTrue(dg.getPrototype() >= -10);
        assertTrue(dg.getPrototype() <= 10);
    }

}