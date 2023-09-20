package org.simbrain.network.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.matrix.NeuronArray;
import org.simbrain.network.matrix.WeightMatrix;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.simbrain.network.core.NetworkUtilsKt.getModelByLabel;

public class NetworkTest {
    Network net;
    Neuron n1, n2;
    Synapse s1;
    NeuronGroup ng1, ng2;
    NeuronArray na1, na2;
    NeuronCollection nc1;
    WeightMatrix wm1;

    SynapseGroup2 sg1;

    @BeforeEach
    public void setUpNetwork() {
        net = new Network();

        n1 = new Neuron(net);
        n1.setLabel("neuron1");
        net.addNetworkModelAsync(n1);
        n2 = new Neuron(net);
        n2.setLabel("neuron2");
        net.addNetworkModelAsync(n2);

        s1 = new Synapse(n1, n2);
        net.addNetworkModelAsync(s1);

        nc1 = new NeuronCollection(net, List.of(n1, n2));
        net.addNetworkModelAsync(nc1);

        ng1 = new NeuronGroup(net, 10);
        ng1.setLabel("neuron_group_1");
        net.addNetworkModelAsync(ng1);
        ng2 = new NeuronGroup(net, 10);
        ng2.setLabel("ng2");
        net.addNetworkModelAsync(ng2);

        sg1 = new SynapseGroup2(ng1, ng2);
        net.addNetworkModelAsync(sg1);

        na1 = new NeuronArray(net, 10);
        na2 = new NeuronArray(net, 10);
        wm1 = new WeightMatrix(net ,na1, na2);
        net.addNetworkModelsAsync(List.of(na1,na2, wm1));
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
        assertEquals(n1, getModelByLabel(net, Neuron.class, "neuron1"));
        assertEquals(n2, getModelByLabel(net, Neuron.class, "neuron2"));
        assertEquals(ng1, getModelByLabel(net, NeuronGroup.class, "neuron_group_1"));
        assertEquals(ng2, getModelByLabel(net, NeuronGroup.class, "ng2"));
    }

    @Test
    public void testXML() {
        String xmlRep = NetworkUtilsKt.getNetworkXStream().toXML(net);
        Network fromXml = (Network) NetworkUtilsKt.getNetworkXStream().fromXML(xmlRep);

        assertNotNull(getModelByLabel(fromXml, Neuron.class, "neuron1"));
        assertNotNull(getModelByLabel(fromXml, Neuron.class, "neuron2"));
        assertNotNull(getModelByLabel(fromXml, NeuronGroup.class, "neuron_group_1"));
        assertNotNull(getModelByLabel(fromXml, NeuronGroup.class, "ng2"));
    }

    @Test
    public void testSynapseCounts() {

        // 1 free synapse
        assertEquals(1, net.getFreeSynapses().size());

        // 1 free synapse + 100 in the synapseGroup = 101
        assertEquals(101, net.getFlatSynapseList().size());
    }

    @Test
    public void testNeuronCounts() {

        // 2 free neurons
        assertEquals(2, net.getFreeNeurons().size());

        // 2 free neurons, 2 x 10 in each of two neuron groups = 22
        // (2 in neuron collection are free neurons)
        assertEquals(22, net.getFlatNeuronList().size());
    }
}