package org.simbrain.network.neuron_update_rules;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;

import static org.junit.Assert.*;

public class LinearRuleTest {

    @Test
    public void testUpdate() {

        LinearRule lr = new LinearRule();
        Neuron n = new Neuron(new Network(), lr);
        n.setInputValue(10);
        lr.update(n);
        assertEquals(1, n.getBuffer(), 0);
        // TODO: Finish testing the update method

    }

    @Test
    public void testClipping() {
        LinearRule lr = new LinearRule();
        lr.setUpperBound(10);
        lr.setLowerBound(-10);

        // Test clipping upper bound
        assertEquals(10, lr.clip(100), 0);
        // Test no clipping
        assertEquals(5, lr.clip(5), 0);
        // Test clipping lower bound
        assertEquals(-10, lr.clip(-20), 0);
    }

    @Test
    public void testDerivative() {

        LinearRule lr = new LinearRule();
        lr.setUpperBound(10);
        lr.setLowerBound(-10);
        lr.setSlope(5.0);

        // Above upper bound should return 0
        assertEquals(0, lr.getDerivative(11), 0);

        // Below lower bound should return 0
        assertEquals(0, lr.getDerivative(-11), 0);

        // Between lower and upper bound returns the slope
        assertEquals(5.0, lr.getDerivative(0), 0);
    }


}