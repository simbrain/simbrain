package org.simbrain.network.groups;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronCollection;
import org.simbrain.network.core.WeightMatrix;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NeuronCollectionTest {

    Network net = new Network();
    Neuron n1 = new Neuron();
    Neuron n2 = new Neuron();
    NeuronCollection nc1 = new NeuronCollection(List.of(n1,n2));
    Neuron n3 = new Neuron();
    Neuron n4 = new Neuron();
    NeuronCollection nc2 = new NeuronCollection(List.of(n3,n4));
    WeightMatrix wm = new WeightMatrix(nc1, nc2);

    {
        net.addNetworkModelsAsync(List.of(n1,n2,n3,n4,nc1,nc2,wm));
    }

    @BeforeEach
    void clearAll() {
        net.clearActivations();
    }

    @Test
    void propagateLooseActivations() {
        n1.setActivation(1.0);
        n2.setActivation(-1.0);
        net.update();
        assertArrayEquals(new double[]{1.0, -1.0}, nc2.getActivationArray());
    }

    @Test
    void propagateLooseInputValues() {
        n1.addInputValue(1.0);
        n2.addInputValue(-1.0);
        net.update(); // This iteration moves inputs to activations
        net.update(); //This one actually propagates from one layer to the next
        assertArrayEquals(new double[]{1.0, -1.0}, nc2.getActivationArray());
    }

    @Test
    void propagateCollectionActivations() {
        nc1.setActivations(new double[]{1.0,-1.0});
        net.update();
        assertArrayEquals(new double[]{1.0, -1.0}, nc2.getActivationArray());
    }

    @Test
    void testNoDoubleBiasApplication() {
        // Set up neurons with specific bias values
        n1.setBias(0.5);
        n2.setBias(0.3);
        
        // Clear activations to start fresh
        n1.setActivation(0.0);
        n2.setActivation(0.0);
        
        // Update the network - this should apply bias only once per neuron (there was a bug where it was applied twice)
        net.update();

        assertEquals(0.5, n1.getActivation(), 0.001, "Neuron 1 should have activation equal to its bias (0.5), not double bias");
        assertEquals(0.3, n2.getActivation(), 0.001, "Neuron 2 should have activation equal to its bias (0.3), not double bias");
    }
}