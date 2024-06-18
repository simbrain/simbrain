package org.simbrain.network.neuron_update_rules;

import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.updaterules.DecayRule;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DecayRuleTest {

    @Test
    public void testAbsoluteDecay() {

        Network net = new Network();
        Neuron n = new Neuron(new DecayRule());
        net.addNetworkModel(n);

        // Set decay method to absolute
        DecayRule dr = (DecayRule) n.getUpdateRule();
        dr.setUpdateType(DecayRule.UpdateType.Absolute);

        // Set to 1 and decay by .2
        n.setActivation(1);
        dr.setDecayAmount(.2);
        net.update();
        assertEquals(.8, n.getActivation(), .01);
        net.update();
        assertEquals(.6, n.getActivation(), .01);
        IntStream.range(0, 10).forEach(i-> net.update());

        // Should decay to 0 by this point
        assertEquals(0, n.getActivation(), .01);
        
        // TODO: Test from "bottom" up and with various parameter values

    }

    @Test
    public void testRelativeDecay() {
        Network net = new Network();
        Neuron n = new Neuron(new DecayRule());
        net.addNetworkModel(n);
        DecayRule dr = (DecayRule) n.getUpdateRule();

        // Set to 1 and decay, with fraction of .1.  We are expecting
        //  1 -> .9 -> .81 -> .729 ->  .6561
        n.setActivation(1);
        dr.setDecayFraction(.1);
        net.update();
        assertEquals(.9,n.getActivation(),.001);
        net.update();
        assertEquals(.81,n.getActivation(),.001);
        net.update();
        assertEquals(.729,n.getActivation(),.001);
        net.update();
        assertEquals(.6561,n.getActivation(),.001);

        // Try with larger number
        n.setUpperBound(10);
        n.setActivation(10);
        net.update();
        assertEquals(9,n.getActivation(),.001);

        // Try with negative number
        n.setActivation(-1);
        net.update();
        assertEquals(-.9,n.getActivation(),.001);

        // Try with different baseline
        n.setActivation(1);
        dr.setBaseLine(.5);
        net.update();
        assertEquals(.95,n.getActivation(),.001);
        
    }

    @Test
    public void testClipping() {
        DecayRule dr = new DecayRule();
        dr.setUpperBound(10);
        dr.setLowerBound(-10);

        // Above upper bound should be upper bound
        assertEquals(10, dr.clip(100), 0);

        // Below lower bound should be lower bound
        assertEquals(-10, dr.clip(-100), 0);

        // Between bounds returns input value
        assertEquals(1, dr.clip(1), 0);

    }

}