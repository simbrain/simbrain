package org.simbrain.network.groups;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronCollection;
import org.simbrain.network.core.WeightMatrix;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

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
        net.addNetworkModels(List.of(n1,n2,n3,n4,nc1,nc2,wm));
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
        assertArrayEquals(new double[]{1.0, -1.0}, nc2.getActivations());
    }

    @Test
    void propagateLooseInputValues() {
        n1.addInputValue(1.0);
        n2.addInputValue(-1.0);
        net.update(); // This iteration moves inputs to activations
        net.update(); //This one actually propagates from one layer to the next
        assertArrayEquals(new double[]{1.0, -1.0}, nc2.getActivations());
    }

    @Test
    void propagateCollectionActivations() {
        nc1.setActivations(new double[]{1.0,-1.0});
        net.update();
        assertArrayEquals(new double[]{1.0, -1.0}, nc2.getActivations());
    }
}