package org.simbrain.network.neuron_update_rules;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;

import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class IAC_Test {
    @Test
    public void testUpdate1InputToOutput() {

        Network net = new Network();
        net.setTimeStep(1); // Simplifies computations

        // Set up the IAC node.
        IACRule iacRule = new IACRule();
        iacRule.setUpperBound(1);
        iacRule.setLowerBound(-1);
        Neuron iacNeuron = new Neuron(net, iacRule);
        iacNeuron.setActivation(0.0);
        net.addNetworkModel(iacNeuron);

        // See if it decays to provided rest value
        iacRule.setRest(0.1);
        IntStream.range(0, 100).forEach(v -> net.update());
        assertEquals(0.1, iacNeuron.getActivation(), 0.01);

        // Test decay from above
        iacNeuron.setActivation(1);
        iacRule.setDecay(.05);
        // Upper Bound (u) = 1, lower bound (l) = -1, decay rate(λ) = 0.05, resting value (b) = 0.1,
        // weighted input (W) = 0
        // a = 1 - ((1 - 1)0 - 0.05 (1 - 0.1)) = .955, where w > 0
        net.update();
        assertEquals(0.955, iacNeuron.getActivation(), 0.00001);

        // Decay from below
        iacNeuron.setActivation(-1);
        // a = -1 - ((1 - 1)0 - 0.05 (-1 - 0.1)) = -.945, where w < 0
        net.update();
        assertEquals(0-.945, iacNeuron.getActivation(), 0.00001);

    }


}
