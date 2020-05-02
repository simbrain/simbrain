package org.simbrain.network.gui;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.nodes.NeuronNode;

import static org.junit.Assert.*;

public class NetworkPanelTest {

    @Test
    public void basicTest() {
        Network net = new Network();
        NetworkPanel np = new NetworkPanel(net);

        Neuron n1 = new Neuron(net);
        Neuron n2 = new Neuron(net);
        net.addLooseNeuron(n1);
        net.addLooseNeuron(n2);

        NeuronNode nn1 = np.getNode(n1);
        NeuronNode nn2 = np.getNode(n2);

        System.out.println(nn1);
        System.out.println(nn2);

    }
}