package org.simbrain.network.groups;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;

import static org.junit.Assert.*;

public class NeuronGroupTest {

    @Test
    public void testDeepCopy() {
        Network net = new Network();
        NeuronGroup ng = new NeuronGroup(net);
        ng.setLabel("test");
        for (int i = 0; i < 10; i++) {
            ng.addNeuron(new Neuron(net));
        }
        NeuronGroup ng2 = ng.deepCopy(net);
        assertTrue(ng2.getLabel().equals("test"));
        assertTrue(ng2.getNeuronList().size() == 10);
    }
}