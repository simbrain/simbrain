
package org.simbrain.network.neuron_update_rules;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;

import static org.junit.Assert.*;

public class BinaryRuleTest {

    @Test
    public void testUpdate() {

        Network net = new Network();
        Neuron n = new Neuron(net,  new BinaryRule());
        net.addLooseNeuron(n);
        BinaryRule br = (BinaryRule) n.getUpdateRule();

        // Set up rule
        br.setThreshold(.5);
        br.setUpperBound(1);
        br.setLowerBound(0);

        // Below threshold
        n.setInputValue(.4);
        net.bufferedUpdateAllNeurons();
        assertEquals(0 ,n.getActivation(),.001);

        // Above threshold
        n.setInputValue(.6);
        net.bufferedUpdateAllNeurons();
        assertEquals(1 ,n.getActivation(),.001);

        // Test new Threshold
        br.setThreshold(.2);
        // Below
        n.setInputValue(.19);
        net.bufferedUpdateAllNeurons();
        assertEquals(0 ,n.getActivation(),.001);
        // Above
        n.setInputValue(.3);
        net.bufferedUpdateAllNeurons();
        assertEquals(1 ,n.getActivation(),.001);

    }
}