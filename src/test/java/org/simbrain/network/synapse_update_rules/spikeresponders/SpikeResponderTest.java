package org.simbrain.network.synapse_update_rules.spikeresponders;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.neuron_update_rules.IzhikevichRule;
import org.simbrain.network.neuron_update_rules.LinearRule;

import static org.junit.Assert.*;

public class SpikeResponderTest {


    @Test
    public void basicTest() {

        Network net = new Network();
        IzhikevichRule ir = new IzhikevichRule();
        ir.setiBg(15);
        Neuron spiking = new Neuron(net, ir);
        LinearRule lr = new LinearRule();
        lr.setClipped(false);
        Neuron output = new Neuron(net, lr );
        Synapse s = new Synapse(spiking, output);
        net.addNeuron(spiking);
        net.addNeuron(output);

        for (int i = 0; i < 100 ; i++) {
            net.update();
        }

        // TODO: Figure out expected number of spikes


    }




}