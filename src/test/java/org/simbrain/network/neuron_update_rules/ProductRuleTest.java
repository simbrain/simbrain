package org.simbrain.network.neuron_update_rules;

import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProductRuleTest {
    @Test
    public void testUpdate2InputsToOutput() {
        Network net = new Network();
        ProductRule productRule = new ProductRule();

        // Setup the input neuron
        Neuron input1 =  new Neuron(net);
        input1.setActivation(0.4);
        input1.setClamped(true);
        net.addNetworkModelAsync(input1);

        // Setup the input neuron
        Neuron input2 =  new Neuron(net);
        input2.setActivation(0.8);
        input2.setClamped(true);
        net.addNetworkModelAsync(input2);

        // Set up the rule.
        productRule.setUpperBound(1);
        productRule.setLowerBound(-1);
        productRule.setUseWeights(false);

        // Set up the output neuron
        Neuron output = new Neuron(net, productRule);
        output.setActivation(0.0);
        net.addNetworkModelAsync(output);

        // Connect the input to the output
        Synapse w13 = new Synapse(input1, output, 1);
        net.addNetworkModelAsync(w13);

        // Connect the input to the output
        Synapse w23 = new Synapse(input2, output, 1);
        net.addNetworkModelAsync(w23);


        // Upper Bound (u) = 1, lower bound (l) = -1, use Weight = false,
        // weighted input (W) = 0.4, 0.8
        net.update();
        // Product Rule (Use Weight = false)
        // 0.4 * 0.8 = 0.32
        assertEquals(0.32, output.getActivation(), 0.00001);
    }

    @Test
    public void testUpdate3InputsToOutput() {
        Network net = new Network();
        ProductRule productRule = new ProductRule();

        // Setup the input neuron
        Neuron input1 =  new Neuron(net);
        input1.setActivation(0.4);
        input1.setClamped(true);
        net.addNetworkModelAsync(input1);

        Neuron input2 =  new Neuron(net);
        input2.setActivation(0.8);
        input2.setClamped(true);
        net.addNetworkModelAsync(input2);

        Neuron input3 =  new Neuron(net);
        input3.setActivation(-0.5);
        input3.setClamped(true);
        net.addNetworkModelAsync(input3);

        // Set up the rule.
        productRule.setUpperBound(1);
        productRule.setLowerBound(-1);
        productRule.setUseWeights(false);

        // Set up the output neuron
        Neuron output = new Neuron(net, productRule);
        output.setActivation(0.0);
        net.addNetworkModelAsync(output);

        // Connect the input to the output
        Synapse w14 = new Synapse(input1, output, 1);
        net.addNetworkModelAsync(w14);

        Synapse w24 = new Synapse(input2, output, 1);
        net.addNetworkModelAsync(w24);

        Synapse w34 = new Synapse(input3, output, 1);
        net.addNetworkModelAsync(w34);

        // Upper Bound (u) = 1, lower bound (l) = -1, use Weight = false,
        // weighted input (W) = 0.4, 0.8, -0.5
        net.update();
        // Product Rule (Use Weight = false)
        // 0.4 * 0.8 * -0.5 = -0.16
        assertEquals(-0.16, output.getActivation(), 0.00001);
    }


}
