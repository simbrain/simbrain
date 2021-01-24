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

        // If source neuron is non spiking, the synapse should not have a spike responder
        // (i.e. it's spike responder should be NonResponder)
        assertEquals(nonSpikingToNonSpiking.getSpikeResponder().getClass(), NonResponder.class);
        assertEquals(nonSpikingtoSpiking.getSpikeResponder().getClass(),  NonResponder.class);

        // If source neuron is spiking, the synapse should have a spike responder
        assertNotEquals(spikingToNonspiking.getSpikeResponder().getClass(), NonResponder.class);
        assertNotEquals(spikingToSpiking.getSpikeResponder().getClass(), NonResponder.class);
    }

    @Test
    public void testSetUp() {

        Network net = new Network();

        Neuron n1 = new Neuron(net);
        Neuron n2 = new Neuron(net);
        net.addLooseNeuron(n1);
        net.addLooseNeuron(n2);

        // Adding one synapse to a network
        Synapse s1 = new Synapse(n1,n2);
        net.addLooseSynapse(s1);

        // There should now be one synapse
        assertEquals(1, net.getSynapseCount(), 0.0);

        // Test source and target
        assertEquals(n1, s1.getSource());
        assertEquals(n2, s1.getTarget());

        // A second synapse with the same source and target should not be added
        Synapse s2_redundant = new Synapse(n1, n2); // TODO: network.looseSynapses becomes empty at this point.
        net.addLooseSynapse(s2_redundant);
        System.out.println(net.getFlatSynapseList().get(0));
        assertEquals(1, net.getSynapseCount(), 0.0);
        // assertEquals("Synapse_1", net.getFlatSynapseList().get(0).getId());

    }

    @Test
    public void testLength() {

        Network net = new Network();

        Neuron n1 = new Neuron(net);
        n1.setLocation(0,0);
        Neuron n2 = new Neuron(net);
        n2.setLocation(100,0);

        Synapse length100 = new Synapse(n1,n2);
        Synapse selfConnection = new Synapse(n1, n1);

        assertEquals(100.0, length100.getLength(), 0.0);
        assertEquals(0.0, selfConnection.getLength(), 0.0);

    }

}