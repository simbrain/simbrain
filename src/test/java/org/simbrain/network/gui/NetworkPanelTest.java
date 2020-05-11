package org.simbrain.network.gui;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.nodes.NeuronNode;

import java.util.List;

public class NetworkPanelTest {

    @Test
    public void basicTest() {
        Network net = new Network();
        NetworkPanel np = new NetworkPanel(null, net);

        Neuron n1 = new Neuron(net);
        Neuron n2 = new Neuron(net);
        net.addLooseNeuron(n1);
        net.addLooseNeuron(n2);

        List<NeuronNode> nodes =  np.getScreenElementsOf(NeuronNode.class);

        System.out.println(nodes.get(0));
        System.out.println(nodes.get(1));

    }
}