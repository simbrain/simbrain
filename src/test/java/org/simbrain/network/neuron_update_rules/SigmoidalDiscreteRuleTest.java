package org.simbrain.network.neuron_update_rules;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

import static org.junit.Assert.assertEquals;

public class SigmoidalDiscreteRuleTest {

    @Test
    public void testUpdate1Input1Output() {
        Network net = new Network();
        SigmoidalRule sig = new SigmoidalRule();

        // Setup the input neuron
        Neuron input1 =  new Neuron(net);
        input1.setActivation(0.5);
        input1.setClamped(true);
        net.addLooseNeuron(input1);

        // the slope and bias need to be changed here
        // Set up the sigmoidal function
        sig.setUpperBound(2);
        sig.setLowerBound(-1);
        sig.setSlope(1.7);
        sig.setBias(0.42);

        // Set up the output neuron with the sigmoidal activation
        Neuron output = new Neuron(net, sig);
        output.setActivation(0.0);
        net.addLooseNeuron(output);

        // Connect the input to the output
        Synapse w12 = new Synapse(input1, output, 1.5);
        net.addLooseSynapse(w12);

        // Upper Bound (u) = 2, lower bound (l) = -1, slope (m) = 1.7, bias (b) = 0.42, weighted input (W) = 0.5 * 1.5
        // Discrete sigmoidal arctan
        net.update();
        assertEquals(1.57256, output.getActivation(), 0.00001);
    }

    @Test
    public void testUpdate1Input2Outputs() {
        Network net = new Network();
        SigmoidalRule sig = new SigmoidalRule();

        // Setup the input neuron
        Neuron input1 =  new Neuron(net);
        input1.setActivation(0.5);
        input1.setClamped(true);
        net.addLooseNeuron(input1);

        Neuron input2 = new Neuron(net);
        input2.setActivation(0.3);
        input2.setClamped(true);
        net.addLooseNeuron(input2);

        // the slope and bias need to be changed here
        // Set up the sigmoidal function, default is arctan
        sig.setUpperBound(2);
        sig.setLowerBound(-1);
        sig.setSlope(1.7);
        sig.setBias(0.42);

        // Set up the output neuron with the sigmoidal activation
        Neuron output = new Neuron(net, sig);
        output.setActivation(0.0);
        net.addLooseNeuron(output);

        // Connect the twp inputs to the output
        // input 1 to output
        Synapse w12 = new Synapse(input1, output, 1.5);
        net.addLooseSynapse(w12);

        // input 2 to output
        Synapse w23 = new Synapse(input2, output, 1.4);
        net.addLooseSynapse(w23);

        // Upper Bound (u) = 2, lower bound (l) = -1, slope (m) = 1.7, bias (b) = 0.42, weighted input (W) = 0.5 * 1.5
        // + 0.3 * 1.4 = 1.17
        // Discrete sigmoidal arctan
        net.update();
        assertEquals(1.67571, output.getActivation(), 0.00001);
    }


    @Test
    public void testUpdate1Input3Outputs() {
        Network net = new Network();
        SigmoidalRule sig = new SigmoidalRule();

        // Setup the input neuron
        Neuron input1 =  new Neuron(net);
        input1.setActivation(0.5);
        input1.setClamped(true);
        net.addLooseNeuron(input1);

        Neuron input2 = new Neuron(net);
        input2.setActivation(0.3);
        input2.setClamped(true);
        net.addLooseNeuron(input2);

        Neuron input3 = new Neuron(net);
        input3.setActivation(-0.2);
        input3.setClamped(true);
        net.addLooseNeuron(input3);

        // the slope and bias need to be changed here
        // Set up the sigmoidal function, default is arctan
        sig.setUpperBound(1);
        sig.setLowerBound(0);
        sig.setSlope(1.89);
        sig.setBias(-0.45);

        // Set up the output neuron with the sigmoidal activation
        Neuron output = new Neuron(net, sig);
        output.setActivation(0.0);
        net.addLooseNeuron(output);

        // Connect the twp inputs to the output
        // input 1 to output
        Synapse w12 = new Synapse(input1, output, 1.5);
        net.addLooseSynapse(w12);

        // input 2 to output
        Synapse w23 = new Synapse(input2, output, -1.4);
        net.addLooseSynapse(w23);

        // input 3 to output
        Synapse w33 = new Synapse(input3, output, 1.23);


        // Upper Bound (u) = 1, lower bound (l) = 0, slope (m) = 1.89, bias (b) = -.45, weighted input (W) = 0.5 * 1.5
        // + 0.3 * -1.4 + -0.2 * 1.23  = 0.084
        // Discrete sigmoidal arctan
        net.update();
        assertEquals(0.13727, output.getActivation(), 0.00001);
    }

}




