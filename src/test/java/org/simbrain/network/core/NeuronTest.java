package org.simbrain.network.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class NeuronTest {

    // Stupid test just to get testing started..

    @Test
    public void basicTest() {
        Neuron neuron = new Neuron(new Network(), "LinearRule");
        neuron.setActivation(1);
        assert (neuron.getActivation() == 1);
    }



}