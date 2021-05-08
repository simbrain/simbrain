package org.simbrain.network.groups;

import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.matrix.WeightMatrix;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NeuronGroupTest {

    Network net = new Network();
    NeuronGroup ng = new NeuronGroup(net);

    {
        ng.setLabel("test");
        for (int i = 0; i < 2; i++) {
            ng.addNeuron(new Neuron(net));
        }
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
    public void testInitSpikeResponder() {
        Network net = new Network();

        // Make a new neuron group
        ng = new NeuronGroup(net);
        assertFalse(ng.isSpikingNeuronGroup());

        // Now it should become spiking
        for (int i = 0; i < 10; i++) {
            ng.addNeuron(new Neuron(net));
            ng.setNeuronType(new IntegrateAndFireRule());
        }
        assertTrue(ng.isSpikingNeuronGroup());

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
}