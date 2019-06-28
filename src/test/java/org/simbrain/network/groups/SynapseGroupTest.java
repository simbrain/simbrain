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
        NeuronGroup spikingSource = new NeuronGroup(net, 2);
        NeuronGroup nonSpikingTarget = new NeuronGroup(net, 2);


        spikingSource.setNeuronType(new IntegrateAndFireRule());
        SynapseGroup sg = SynapseGroup.createSynapseGroup(spikingSource,nonSpikingTarget, new AllToAll());

        // TODO: failing.
        assertTrue(sg.getExcitatoryPrototype().getSpikeResponder().getClass() != NonResponder.class);
        assertTrue(sg.getInhibitoryPrototype().getSpikeResponder().getClass() != NonResponder.class);

    }

}