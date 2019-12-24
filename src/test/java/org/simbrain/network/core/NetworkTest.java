package org.simbrain.network.core;

import org.junit.Test;
import org.simbrain.network.groups.NeuronGroup;

import static org.junit.Assert.*;

public class NetworkTest {

    @Test
    public void getByLabel() {
        Network net = new Network();

        Neuron n1 = new Neuron(net);
        n1.setLabel("neuron1");
        net.addLooseNeuron(n1);
        Neuron n2 = new Neuron(net);
        n2.setLabel("neuron2");
        net.addLooseNeuron(n2);
        assertEquals(n1, net.getNeuronByLabel("neuron1"));
        assertEquals(n2, net.getNeuronByLabel("neuron2"));

        NeuronGroup ng1 = new NeuronGroup(net);
        ng1.setLabel("neuron_group_1");
        net.addNeuronGroup(ng1);
        NeuronGroup ng2 = new NeuronGroup(net);
        ng2.setLabel("ng2");
        net.addNeuronGroup(ng2);
        assertEquals(ng1, net.getNeuronGroupByLabel("neuron_group_1"));
        assertEquals(ng2, net.getNeuronGroupByLabel("ng2"));

    }
}