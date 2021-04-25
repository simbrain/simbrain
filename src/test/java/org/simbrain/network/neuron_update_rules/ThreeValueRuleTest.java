package org.simbrain.network.neuron_update_rules;

import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreeValueRuleTest {
    @Test
    public  void testUpdateLowerValue() {
        Network net = new Network();
        ThreeValueRule threeValRule = new ThreeValueRule();

        // Setup the input neuron
        Neuron input1 =  new Neuron(net);
        input1.setActivation(-0.1);
        input1.setClamped(true);
        net.addNetworkModel(input1);

        // Set up the rule
        threeValRule.setLowerValue(-0.71);
        threeValRule.setMiddleValue(0.1);
        threeValRule.setUpperValue(0.92);

        threeValRule.setLowerThreshold(0.3);
        threeValRule.setUpperThreshold(0.65);

        // Set up the output neuron
        Neuron output = new Neuron(net, threeValRule);
        output.setActivation(0.0);
        net.addNetworkModel(output);

        // Connect the input to the output
        Synapse w12 = new Synapse(input1, output, 1);
        net.addNetworkModel(w12);

        // Lower value = -0.71, middle vaue = 0.1, upper value = 0.92
        // activation value = 0.4
        net.update();
        assertEquals(-0.71, output.getActivation(), 0.00001);
    }

    @Test
    public  void testUpdateMiddleValue() {
        Network net = new Network();
        ThreeValueRule threeValRule = new ThreeValueRule();

        // Setup the input neuron
        Neuron input1 =  new Neuron(net);
        input1.setActivation(0.4);
        input1.setClamped(true);
        net.addNetworkModel(input1);

        // Set up the rule
        threeValRule.setLowerValue(-0.71);
        threeValRule.setMiddleValue(0.1);
        threeValRule.setUpperValue(0.92);

        threeValRule.setLowerThreshold(0.3);
        threeValRule.setUpperThreshold(0.65);

        // Set up the output neuron
        Neuron output = new Neuron(net, threeValRule);
        output.setActivation(0.0);
        net.addNetworkModel(output);

        // Connect the input to the output
        Synapse w12 = new Synapse(input1, output, 1);
        net.addNetworkModel(w12);

        // Lower value = -0.71, middle vaue = 0.1, upper value = 0.92
        // activation value = 0.1
        net.update();
        assertEquals(0.1, output.getActivation(), 0.00001);
    }

    @Test
    public  void testUpdateUpperValue() {
        Network net = new Network();
        ThreeValueRule threeValRule = new ThreeValueRule();

        // Setup the input neuron
        Neuron input1 =  new Neuron(net);
        input1.setActivation(0.8);
        input1.setClamped(true);
        net.addNetworkModel(input1);

        // Set up the rule
        threeValRule.setLowerValue(-0.71);
        threeValRule.setMiddleValue(0.1);
        threeValRule.setUpperValue(0.92);

        threeValRule.setLowerThreshold(0.3);
        threeValRule.setUpperThreshold(0.65);

        // Set up the output neuron
        Neuron output = new Neuron(net, threeValRule);
        output.setActivation(0.0);
        net.addNetworkModel(output);

        // Connect the input to the output
        Synapse w12 = new Synapse(input1, output, 1);
        net.addNetworkModel(w12);

        // Lower value = -0.71, middle vaue = 0.1, upper value = 0.92
        // activation value = 0.1
        net.update();
        assertEquals(0.92, output.getActivation(), 0.00001);
    }


}
