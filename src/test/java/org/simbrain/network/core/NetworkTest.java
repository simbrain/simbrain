package org.simbrain.network.core;

import org.junit.Before;
import org.junit.Test;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.Utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NetworkTest {

    Network net;
    Neuron n1, n2;
    NeuronGroup ng1, ng2;
    // TODO: Collections, neuronarrays, synapsegroups, update, copy/paste

    @Before
    public void setUpNetwork() {
        net = new Network();

        n1 = new Neuron(net);
        n1.setLabel("neuron1");
        net.addNetworkModel(n1);
        n2 = new Neuron(net);
        n2.setLabel("neuron2");
        net.addNetworkModel(n2);

        ng1 = new NeuronGroup(net);
        ng1.setLabel("neuron_group_1");
        net.addNetworkModel(ng1);
        ng2 = new NeuronGroup(net);
        ng2.setLabel("ng2");
        net.addNetworkModel(ng2);
    }

    @Test
    public void getByLabel() {
        assertEquals(n1, net.getNeuronByLabel("neuron1"));
        assertEquals(n2, net.getNeuronByLabel("neuron2"));
        assertEquals(ng1, net.getNeuronGroupByLabel("neuron_group_1"));
        assertEquals(ng2, net.getNeuronGroupByLabel("ng2"));
    }

    @Test
    public void testXML() {
        String xmlRep = Utils.getSimbrainXStream().toXML(net);
        Network fromXml = (Network) Utils.getSimbrainXStream().fromXML(xmlRep);

        assertNotNull(fromXml.getNeuronByLabel("neuron1") );
        assertNotNull(fromXml.getNeuronByLabel("neuron2") );
        assertNotNull(fromXml.getNeuronGroupByLabel("neuron_group_1") );
        assertNotNull(fromXml.getNeuronGroupByLabel("ng2") );

    }
}