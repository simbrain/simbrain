package org.simbrain.network.gui;

import org.junit.jupiter.api.Test;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NetworkPanelTest {

    @Test
    public void testAddingScreenElements() {

        Network net = new Network();
        NetworkComponent nc = new NetworkComponent("Test", net);
        NetworkPanel np = new NetworkPanel(nc);

        Neuron n1 = new Neuron(net);
        Neuron n2 = new Neuron(net);
        net.addNetworkModel(n1);
        net.addNetworkModel(n2);

        assertEquals(2,np.getScreenElements().size());

    }
}