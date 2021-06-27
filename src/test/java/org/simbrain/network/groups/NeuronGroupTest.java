package org.simbrain.network.groups;

import org.junit.jupiter.api.Test;
import org.simbrain.network.connectors.WeightMatrix;
import org.simbrain.network.core.Network;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NeuronGroupTest {

    Network net = new Network();
    NeuronGroup ng = new NeuronGroup(net, 2);

    {
        ng.setLabel("test");
        net.addNetworkModel(ng);
    }


    @Test
    public void testDeepCopy() {
        NeuronGroup ng2 = ng.deepCopy(net);
        net.addNetworkModel(ng2);
        assertEquals(2, ng2.getNeuronList().size());
        // Labels should not be copied
        assertNotEquals("test", ng2.getLabel());
    }

    @Test
    void propagateLooseActivations() {
        ng.getNeuron(0).setActivation(1.0);
        ng.getNeuron(1).setActivation(-1.0);
        NeuronGroup ng2 = new NeuronGroup(net, 2);
        WeightMatrix wm = new WeightMatrix(net, ng, ng2);
        net.addNetworkModels(List.of(ng2, wm));
        net.update();
        assertArrayEquals(new double[]{1.0, -1.0}, ng2.getActivations());
    }

    @Test
    void propagateGroupActivation() {
        ng.setActivations(new double[]{1.0, -1.0});
        NeuronGroup ng2 = new NeuronGroup(net, 2);
        WeightMatrix wm = new WeightMatrix(net, ng, ng2);
        net.addNetworkModels(List.of(ng2, wm));
        net.update();
        assertArrayEquals(new double[]{1.0, -1.0}, ng2.getActivations());
    }

    @Test
    void getThenSetActivations() {
        ng.getActivations(); // validates the cache. be sure propagation still works
        ng.getNeuron(0).setActivation(1.0);
        ng.getNeuron(1).setActivation(-1.0);
        NeuronGroup ng2 = new NeuronGroup(net, 2);
        WeightMatrix wm = new WeightMatrix(net, ng, ng2);
        net.addNetworkModels(List.of(ng2, wm));
        net.update();
        assertArrayEquals(new double[]{1.0, -1.0}, ng2.getActivations());
    }
}