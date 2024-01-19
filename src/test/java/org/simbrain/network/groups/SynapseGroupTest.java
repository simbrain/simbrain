package org.simbrain.network.groups;

import org.junit.jupiter.api.Test;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.SynapseGroup;
import org.simbrain.network.neurongroups.NeuronGroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SynapseGroupTest {

    @Test
    public void testCreation() {
        Network net = new Network();
        NeuronGroup source = new NeuronGroup(net, 2);
        NeuronGroup target = new NeuronGroup(net, 2);
        SynapseGroup sg = new SynapseGroup(source, target, new AllToAll());
        assertEquals(sg.size(), 4);
    }

    /**
     * When the source neuron group is spiking and the target neuron group is not, the
     * prototype synapses should have spike responders.
     */
    // @Test
    // public void spikeResponderTest() {
    //     Network net = new Network();
    //
    //     NeuronGroup spikingNg = new NeuronGroup(net, 2);
    //     spikingNg.setNeuronType(new IntegrateAndFireRule());
    //     NeuronGroup nonSpikingNg = new NeuronGroup(net, 2);
    //
    //     // Spike to non-spiking synapse group should have spike responders
    //     SynapseGroup sg1 = new SynapseGroup2(spikingNg,nonSpikingNg, new AllToAll());
    //     assertTrue(sg1.getExcitatoryPrototype().getSpikeResponder().getClass() != NonResponder.class);
    //     assertTrue(sg1.getInhibitoryPrototype().getSpikeResponder().getClass() != NonResponder.class);
    //
    //     // Non-spiking to to non-spiking synapse group should have non-responders
    //     SynapseGroup sg2 = new SynapseGroup2(nonSpikingNg, spikingNg, new AllToAll());
    //     assertTrue(sg2.getExcitatoryPrototype().getSpikeResponder().getClass() == NonResponder.class);
    //     assertTrue(sg2.getInhibitoryPrototype().getSpikeResponder().getClass() == NonResponder.class);
    //
    // }

}