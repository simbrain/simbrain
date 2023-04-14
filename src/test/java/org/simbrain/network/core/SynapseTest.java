package org.simbrain.network.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class SynapseTest {

    Network net;
    Neuron n1, n2;
    Synapse s1;

    @BeforeEach
    void setUp() {

        net = new Network();
        n1 = new Neuron(net);
        n2 = new Neuron(net);
        s1 = new Synapse(n1,n2);

        net.addNetworkModelAsync(n1);
        net.addNetworkModelAsync(n2);
        net.addNetworkModelAsync(s1);
    }

    @Test
    public void testInitialization() {

        // There should now be one synapse
        assertEquals(1, net.getModels(Synapse.class).size(), 0.0);

        // Test source and target
        assertEquals(n1, s1.getSource());
        assertEquals(n2, s1.getTarget());

        // A second synapse with the same source and target should not be added
        Synapse s2_redundant = new Synapse(n1, n2);
        net.addNetworkModelAsync(s2_redundant);
        assertEquals(1, net.getModels(Synapse.class).size(), 0.0);

    }

    @Test
    void testDelays() {
        n1.setActivation(1);
        s1.setDelay(3);
        net.update();
        assertEquals(0, n1.getActivation(), 0.0);
        net.update();
        net.update();
        assertEquals(0, n1.getActivation(), 1.0);
        net.update();
        assertEquals(0, n1.getActivation(), 0.0);
    }

    @Test
    void testEnabled() {
        n1.setActivation(1);
        n1.setClamped(true);
        s1.setEnabled(false);
        net.update();
        assertEquals(0, n2.getActivation(), 0.0);
        s1.setEnabled(true);
        net.update();
        assertEquals(1, n2.getActivation(), 0.0);
    }

    @Test
    public void testLength() {

        n1.setLocation(0,0);
        n2.setLocation(100,0);

        Synapse length100 = new Synapse(n1,n2);
        Synapse selfConnection = new Synapse(n1, n1);

        assertEquals(100.0, length100.getLength(), 0.0);
        assertEquals(0.0, selfConnection.getLength(), 0.0);

    }

}