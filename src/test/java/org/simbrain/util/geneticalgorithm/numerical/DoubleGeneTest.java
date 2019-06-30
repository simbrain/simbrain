package org.simbrain.util.geneticalgorithm.numerical;

import org.junit.Test;

import static org.junit.Assert.*;

public class DoubleGeneTest {

    @Test
    public void testMutationMinMax() {

        DoubleGene dg = new DoubleGene(.5);
        dg.setMinimum(0);
        dg.setMaximum(1);
        for (int i = 0; i < 100 ; i++) {
            dg.mutate();
        }
        assertTrue(dg.getPrototype() > 0);
        assertTrue(dg.getPrototype() < 1);
    }

    @Test
    public void testValidCopy() {
        DoubleGene dg = new DoubleGene(.5);
        dg.setMinimum(0);
        dg.setMaximum(1);
        dg.setStepSize(2);

        DoubleGene copy = dg.copy();

        assertEquals(copy.getPrototype(), dg.getPrototype());
        assertEquals(copy.getMinimum(), dg.getMinimum(), .001);
        assertEquals(copy.getMaximum(), dg.getMaximum(), .001);
        assertEquals(copy.getStepSize(), dg.getStepSize(), .001);
        assertNotEquals(copy, dg);
    }
}