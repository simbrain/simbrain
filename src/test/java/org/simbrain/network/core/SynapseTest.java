package org.simbrain.network.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class SynapseTest {

    @Test
    public void testAddSynapses() {

        Network net = new Network();

        Neuron n1 = new Neuron(net);
        Neuron n2 = new Neuron(net);
        net.addNetworkModel(n1);
        net.addNetworkModel(n2);

        // Adding one synapse to a network
        Synapse s1 = new Synapse(n1,n2);
        net.addNetworkModel(s1);

        // There should now be one synapse
        assertEquals(1, net.getModels(Synapse.class).size(), 0.0);

        // Test source and target
        assertEquals(n1, s1.getSource());
        assertEquals(n2, s1.getTarget());

        // A second synapse with the same source and target should not be added
        Synapse s2_redundant = new Synapse(n1, n2);
        net.addNetworkModel(s2_redundant);
        assertEquals(1, net.getModels(Synapse.class).size(), 0.0);

    }

    @Test
    public void testLength() {

        Network net = new Network();

        Neuron n1 = new Neuron(net);
        n1.setLocation(0,0);
        Neuron n2 = new Neuron(net);
        n2.setLocation(100,0);

        Synapse length100 = new Synapse(n1,n2);
        Synapse selfConnection = new Synapse(n1, n1);

        assertEquals(100.0, length100.getLength(), 0.0);
        assertEquals(0.0, selfConnection.getLength(), 0.0);

    }

}