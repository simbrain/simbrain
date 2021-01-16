package org.simbrain.network.neuron_update_rules;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.util.math.SquashingFunctionEnum;

import static org.junit.Assert.assertEquals;

public class IAC_Test {
    @Test
    public void testUpdate1InputToOutput() {
        Network net = new Network();
        IACRule iacRule = new IACRule();

        // Setup the input neuron
        Neuron input1 =  new Neuron(net);
        input1.setActivation(0.5);
        input1.setClamped(true);
        net.addLooseNeuron(input1);

        // Set up the IAC rule.
        iacRule.setUpperBound(1);
        iacRule.setLowerBound(-1);
        iacRule.setDecay(0.05);
        iacRule.setRest(0.1);

        // Set up the output neuron
        Neuron output = new Neuron(net, iacRule);
        output.setActivation(0.0);
        net.addLooseNeuron(output);

        // Connect the input to the output
        Synapse w12 = new Synapse(input1, output, 1);
        net.addLooseSynapse(w12);

        // Upper Bound (u) = 1, lower bound (l) = -1, decay rate(λ) = 0.05, resting value (b) = 0.1,
        // weighted input (W) = 0.5 * 1, where w > 0
        // The interactive activation and competition networks
        net.update();
        // a = 0 + ((1 - 0)(0.5) - 0.05 (0 - 0.1)) = 0.505, where w > 0
        // Timestampd is 0.1, so it's 0.0505 instead of 0.505
        assertEquals(0.505, output.getActivation(), 0.00001);

    }


}
