package org.simbrain.network.groups;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.matrix.WeightMatrix;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class NeuronCollectionTest {

    Network net = new Network();
    Neuron n1 = new Neuron(net);
    Neuron n2 = new Neuron(net);
    NeuronCollection nc1 = new NeuronCollection(net, List.of(n1,n2));
    Neuron n3 = new Neuron(net);
    Neuron n4 = new Neuron(net);
    NeuronCollection nc2 = new NeuronCollection(net, List.of(n2,n3));
    WeightMatrix wm = new WeightMatrix(net, nc1, nc2);

    {
        net.addNetworkModels(List.of(n1,n2,n3,n4,nc1,nc2,wm));
    }

    @BeforeEach
    private void clearAll() {
        net.clearActivations();
    }

    @Test
    void propagateLooseActivations() {
        n1.addInputValue(1.0);
        n2.addInputValue(-1.0);
        net.update();
        assertArrayEquals(new double[]{1.0, -1.0}, nc2.getActivations());
    }

    @Test
    void propagateCollectionActivations() {
        nc1.addInputs(new double[]{1.0,-1.0});
        net.update();
        assertArrayEquals(new double[]{1.0, -1.0}, nc2.getActivations());
    }
}