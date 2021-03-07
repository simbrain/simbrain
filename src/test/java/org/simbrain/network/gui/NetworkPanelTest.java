package org.simbrain.network.gui;

import org.junit.Test;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.nodes.NeuronNode;

import java.util.List;

public class NetworkPanelTest {

    @Test
    public void basicTest() {
        Network net = new Network();
        NetworkComponent nc = new NetworkComponent("Test", net);
        NetworkPanel np = new NetworkPanel(nc);

        Neuron n1 = new Neuron(net);
        Neuron n2 = new Neuron(net);
        net.addNetworkModel(n1);
        net.addNetworkModel(n2);

        List<NeuronNode> nodes =  np.filterScreenElements(NeuronNode.class);

        System.out.println(nodes.get(0));
        System.out.println(nodes.get(1));

    }
}