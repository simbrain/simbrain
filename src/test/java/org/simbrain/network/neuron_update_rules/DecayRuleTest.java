package org.simbrain.network.neuron_update_rules;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;

import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class DecayRuleTest {

    @Test
    public void testAbsoluteDecay() {

        Network net = new Network();
        Neuron n = new Neuron(net,  new DecayRule());
        net.addLooseNeuron(n);

        // Set decay method to absolute
        DecayRule dr = (DecayRule) n.getUpdateRule();
        dr.setRelAbs(DecayRule.ABSOLUTE);

        // Set to 1 and decay by .2
        n.setActivation(1);
        dr.setDecayAmount(.2);
        net.bufferedUpdateAllNeurons();
        assertEquals(.8, n.getActivation(), .01);
        net.bufferedUpdateAllNeurons();
        assertEquals(.6, n.getActivation(), .01);
        IntStream.range(0, 10).forEach(i-> net.bufferedUpdateAllNeurons());

        // Should decay to 0 by this point
        assertEquals(0, n.getActivation(), .01);
        
        // TODO: Test from "bottom" up and with various parameter values

    }

    @Test
    public void testRelativeDecay() {
        Network net = new Network();
        Neuron n = new Neuron(net,  new DecayRule());
        net.addLooseNeuron(n);
        DecayRule dr = (DecayRule) n.getUpdateRule();

        // Set to 1 and decay, with fraction of .1.  We are expecting
        //  1 -> .9 -> .81 -> .729 ->  .6561
        n.setActivation(1);
        dr.setDecayFraction(.1);
        net.bufferedUpdateAllNeurons();
        assertEquals(.9,n.getActivation(),.001);
        net.bufferedUpdateAllNeurons();
        assertEquals(.81,n.getActivation(),.001);
        net.bufferedUpdateAllNeurons();
        assertEquals(.729,n.getActivation(),.001);
        net.bufferedUpdateAllNeurons();
        assertEquals(.6561,n.getActivation(),.001);

        // Try with larger number
        n.setUpperBound(10);
        n.setActivation(10);
        net.bufferedUpdateAllNeurons();
        assertEquals(9,n.getActivation(),.001);

        // Try with negative number
        n.setActivation(-1);
        net.bufferedUpdateAllNeurons();
        assertEquals(-.9,n.getActivation(),.001);

        // Try with different baseline
        n.setActivation(1);
        dr.setBaseLine(.5);
        net.bufferedUpdateAllNeurons();
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