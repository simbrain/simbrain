package org.simbrain.network.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simbrain.network.neurongroups.*;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.subnetworks.SRNNetwork;
import org.simbrain.network.subnetworks.Subnetwork;
import org.simbrain.network.util.BiasedScalarData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.simbrain.network.core.NetworkUtilsKt.getModelByLabel;
import static org.simbrain.util.GeomKt.point;

public class NetworkTest {
    Network net;
    Neuron n1, n2;
    Synapse s1;
    NeuronGroup ng1, ng2;
    NeuronArray na1, na2;
    NeuronCollection nc1;
    WeightMatrix wm1;
    SynapseGroup sg1;
    SoftmaxGroup softmax;
    SOMGroup som;
    WinnerTakeAll wta;
    CompetitiveGroup competitive;

    // TODO: Other subneworks
    BackpropNetwork bp;
    SRNNetwork srn;


    @BeforeEach
    public void setUpNetwork() {
        net = new Network();

        n1 = new Neuron();
        n1.setLabel("neuron1");
        ((BiasedScalarData) n1.getDataHolder()).setBias(1.0);
        net.addNetworkModel(n1);
        n2 = new Neuron();
        n2.setLabel("neuron2");
        net.addNetworkModel(n2);

        s1 = new Synapse(n1, n2);
        net.addNetworkModel(s1);

        nc1 = new NeuronCollection(List.of(n1, n2));
        net.addNetworkModel(nc1);

        ng1 = new NeuronGroup(10);
        ng1.setLabel("neuron_group_1");
        net.addNetworkModel(ng1);
        ng2 = new NeuronGroup(10);
        ng2.setLabel("ng2");
        net.addNetworkModel(ng2);

        sg1 = new SynapseGroup(ng1, ng2);
        net.addNetworkModel(sg1);

        na1 = new NeuronArray(10);
        na2 = new NeuronArray(10);
        wm1 = new WeightMatrix(na1, na2);
        net.addNetworkModels(List.of(na1,na2, wm1));

        softmax = new SoftmaxGroup(5);
        softmax.setLabel("softmax");
        som = new SOMGroup(5);
        som.setLabel("som");
        competitive = new CompetitiveGroup(5);
        competitive.setLabel("competitive");
        wta = new WinnerTakeAll(net, 5);
        wta.setLabel("wta");

        net.addNetworkModels(softmax, som, competitive, wta);

        bp = new BackpropNetwork(new int[] {3,5,4}, point(0,0));
        bp.setLabel("backprop");
        srn = new SRNNetwork(5,5,5, point(0,0));
        srn.setLabel("srn");
        net.addNetworkModels(bp, srn);

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

        // Subnets and custom groups
        assertEquals(1, net.getModels(SoftmaxGroup.class).size());
        // TODO: getModels(BackpropNetwork.class) fails, because of how the
        //  NetworkModelList is created (see NetworkModel.add). But changing that
        //  breaks things and we don't yet have use cases for getmodels on specific subnets
        assertEquals(2, net.getModels(Subnetwork.class).size());
        softmax.delete();
        bp.delete();
        srn.delete();
        assertEquals(0, net.getModels(SoftmaxGroup.class).size());
        assertEquals(0, net.getModels(Subnetwork.class).size());

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

        var n1 = getModelByLabel(fromXml, Neuron.class, "neuron1");
        assertNotNull(n1);
        assertEquals(1.0, ((BiasedScalarData) n1.getDataHolder()).getBias());
        assertNotNull(getModelByLabel(fromXml, Neuron.class, "neuron2"));
        assertNotNull(getModelByLabel(fromXml, NeuronGroup.class, "neuron_group_1"));
        assertNotNull(getModelByLabel(fromXml, NeuronGroup.class, "ng2"));
        assertNotNull(getModelByLabel(fromXml, SoftmaxGroup.class, "softmax"));
        assertNotNull(getModelByLabel(fromXml, SOMGroup.class, "som"));
        assertNotNull(getModelByLabel(fromXml, CompetitiveGroup.class, "competitive"));
        assertNotNull(getModelByLabel(fromXml, WinnerTakeAll.class, "wta"));
        assertNotNull(getModelByLabel(fromXml, BackpropNetwork.class, "backprop"));
        assertNotNull(getModelByLabel(fromXml, SRNNetwork.class, "srn"));
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