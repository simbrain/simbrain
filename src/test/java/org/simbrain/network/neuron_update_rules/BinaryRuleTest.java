
package org.simbrain.network.neuron_update_rules;

import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.updaterules.BinaryRule;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BinaryRuleTest {

    @Test
    public void testUpdate() {

        Network net = new Network();
        Neuron n = new Neuron(new BinaryRule());
        net.addNetworkModel(n);
        BinaryRule br = (BinaryRule) n.getUpdateRule();

        // Set up rule
        br.setThreshold(.5);
        br.setUpperBound(1);
        br.setLowerBound(0);

        // Below threshold
        n.addInputValue(.4);
        net.bufferedUpdate();
        assertEquals(0 ,n.getActivation(),.001);

        // Above threshold
        n.addInputValue(.6);
        net.bufferedUpdate();
        assertEquals(1 ,n.getActivation(),.001);

        // Test new Threshold
        br.setThreshold(.2);
        // Below
        n.addInputValue(.19);
        net.bufferedUpdate();
        assertEquals(0 ,n.getActivation(),.001);
        // Above
        n.addInputValue(.3);
        net.bufferedUpdate();
        assertEquals(1 ,n.getActivation(),.001);

    }
}