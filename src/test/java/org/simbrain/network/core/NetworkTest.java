package org.simbrain.network.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.matrix.NeuronArray;
import org.simbrain.network.matrix.WeightMatrix;
import org.simbrain.util.Utils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NetworkTest {

    Network net;
    Neuron n1, n2;
    Synapse s1;
    NeuronGroup ng1, ng2;
    NeuronArray na1, na2;
    NeuronCollection nc1;
    WeightMatrix wm1;

    @BeforeEach
    public void setUpNetwork() {
        net = new Network();

        n1 = new Neuron(net);
        n1.setLabel("neuron1");
        net.addNetworkModel(n1);
        n2 = new Neuron(net);
        n2.setLabel("neuron2");
        net.addNetworkModel(n2);

        s1 = new Synapse(n1, n2);
        net.addNetworkModel(s1);

        nc1 = new NeuronCollection(net, List.of(n1, n2));
        net.addNetworkModel(nc1);

        ng1 = new NeuronGroup(net, 10);
        ng1.setLabel("neuron_group_1");
        net.addNetworkModel(ng1);
        ng2 = new NeuronGroup(net, 10);
        ng2.setLabel("ng2");
        net.addNetworkModel(ng2);

        SynapseGroup sg1 = SynapseGroup.createSynapseGroup(ng1, ng2);
        net.addNetworkModel(sg1);

        na1 = new NeuronArray(net, 10);
        na2 = new NeuronArray(net, 10);
        wm1 = new WeightMatrix(net ,na1, na2);
        net.addNetworkModels(List.of(na1,na2, wm1));
    }

    @Test
    void testDeleteObjects() {

        // Neurons and Synapses
        n1.delete();
        assertEquals(1, net.getModels(Neuron.class).size());
        // Deleting the neuron should also delete the synapse
        assertEquals(0, net.getModels(Synapse.class).size());

        // Deleting all neurons should delete the neuron collection
        assertEquals(1, net.getModels(NeuronCollection.class).size());
        n2.delete();
        assertEquals(0, net.getModels(NeuronCollection.class).size());

        // Neuron Groups and Synapse Groups
        ng1.delete();
        assertEquals(1, net.getModels(NeuronGroup.class).size());
        // Deleting the neuron group should also delete the synapse group
        assertEquals(0, net.getModels(SynapseGroup.class).size());

        // Neuron Arrays and WeightMatrices
        na1.delete();
        assertEquals(1, net.getModels(NeuronArray.class).size());
        // Deleting the neuron group should also delete the synapse group
        assertEquals(0, net.getModels(WeightMatrix.class).size());

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