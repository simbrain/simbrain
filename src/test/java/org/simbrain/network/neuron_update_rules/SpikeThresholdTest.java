package org.simbrain.network.neuron_update_rules;

import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.updaterules.SpikingThresholdRule;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpikeThresholdTest {

    @Test
    public void testUpdate1InputToOutput() {
        Network net = new Network();
        SpikingThresholdRule spRule = new SpikingThresholdRule();

        // Setup the input neuron
        Neuron input1 =  new Neuron();
        input1.setActivation(0.675);
        input1.setClamped(true);
        net.addNetworkModelAsync(input1);

        // Set up the rule
        spRule.threshold = 0.4;

        // Set up the output neuron
        Neuron output = new Neuron(spRule);
        output.setActivation(0.0);
        net.addNetworkModelAsync(output);

        // Connect the input to the output
        Synapse w12 = new Synapse(input1, output, 2);
        net.addNetworkModelAsync(w12);

        // Threshold = 0.4, Activation = 0.675
        net.update();
        assertEquals(1, output.getActivation(), 0.00001);

    }
}
