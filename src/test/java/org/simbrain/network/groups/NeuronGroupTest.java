package org.simbrain.network.groups;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.network.neuron_update_rules.UpdateRuleEnum;

import static org.junit.Assert.*;

public class NeuronGroupTest {

    @Test
    public void testDeepCopy() {
        Network net = new Network();
        NeuronGroup ng = new NeuronGroup(net);
        ng.setLabel("test");
        for (int i = 0; i < 10; i++) {
            ng.addNeuron(new Neuron(net));
        }
        NeuronGroup ng2 = ng.deepCopy(net);
        assertEquals(10, ng2.getNeuronList().size());
        // Labels should not be copied
        assertNotEquals("test", ng2.getLabel());
    }

    @Test
    public void testInitSpikeResponder() {
        Network net = new Network();
        NeuronGroup ng = new NeuronGroup(net);
        assertFalse(ng.isSpikingNeuronGroup());

        // Now it should become spiking
        for (int i = 0; i < 10; i++) {
            ng.addNeuron(new Neuron(net));
            ng.setNeuronType(new IntegrateAndFireRule());
        }
        assertTrue(ng.isSpikingNeuronGroup());

    }
}