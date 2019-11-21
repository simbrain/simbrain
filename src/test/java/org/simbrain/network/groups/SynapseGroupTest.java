package org.simbrain.network.groups;

import org.junit.Test;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Network;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.NonResponder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SynapseGroupTest {

    @Test
    public void testCreation() {
        Network net = new Network();
        NeuronGroup source = new NeuronGroup(net, 2);
        NeuronGroup target = new NeuronGroup(net, 2);
        SynapseGroup sg = SynapseGroup.createSynapseGroup(source,target, new AllToAll());
        assertEquals(sg.getAllSynapses().size(), 4);
    }

    /**
     * When the source neuron group is spiking and the target neuron group is not, the
     * prototype synapses should have spike responders
     */
    @Test
    public void spikeResponderTest() {
        Network net = new Network();

        NeuronGroup spikingNg = new NeuronGroup(net, 2);
        spikingNg.setNeuronType(new IntegrateAndFireRule());
        NeuronGroup nonSpikingNg = new NeuronGroup(net, 2);

        // Spike to non-spiking synapse group should have spike responders
        SynapseGroup sg1 = SynapseGroup.createSynapseGroup(spikingNg,nonSpikingNg, new AllToAll());
        assertTrue(sg1.getExcitatoryPrototype().getSpikeResponder().getClass() != NonResponder.class);
        assertTrue(sg1.getInhibitoryPrototype().getSpikeResponder().getClass() != NonResponder.class);

        // Non-spiking to to non-spiking synapse group should have non-responders
        SynapseGroup sg2 = SynapseGroup.createSynapseGroup(nonSpikingNg, spikingNg, new AllToAll());
        assertTrue(sg2.getExcitatoryPrototype().getSpikeResponder().getClass() == NonResponder.class);
        assertTrue(sg2.getInhibitoryPrototype().getSpikeResponder().getClass() == NonResponder.class);
        
    }

}