package org.simbrain.network.core;

import org.junit.Test;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.NonResponder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SynapseTest {

    /**
     * If source neuron is spiking, a default spike responder should be created.
     * In all other cases a non-responder should be used,.
     */
    @Test
    public void initSpikeResponder() {

        Network net = new Network();
        Neuron spiking = new Neuron(net, new IntegrateAndFireRule());
        Neuron nonSpiking = new Neuron(net, new LinearRule());

        Synapse nonSpikingtoSpiking = new Synapse(nonSpiking, spiking);
        Synapse spikingToSpiking = new Synapse(spiking, spiking);
        Synapse nonSpikingToNonSpiking = new Synapse(nonSpiking, nonSpiking);
        Synapse spikingToNonspiking = new Synapse(spiking, nonSpiking);

        // Source neuron is non spiking.
        assertEquals(nonSpikingToNonSpiking.getSpikeResponder().getClass(), NonResponder.class);
        assertEquals(nonSpikingtoSpiking.getSpikeResponder().getClass(),  NonResponder.class);

        // Source neuron is spiking
        assertNotEquals(spikingToNonspiking.getSpikeResponder().getClass(), NonResponder.class);
        assertNotEquals(spikingToSpiking.getSpikeResponder().getClass(), NonResponder.class);
    }
}