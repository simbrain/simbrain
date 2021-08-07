package org.simbrain.network.matrix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import smile.math.matrix.Matrix;

import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NeuronArrayTest {

    private NeuronArray neuronArray;

    @BeforeEach
    public void setUp() {
        Network network = new Network();
        neuronArray = new NeuronArray(network, 10);
    }

    @Test
    public void testUpdate() {
        Matrix inputs = new Matrix(10, 1);
        inputs.fill(1.0);
        neuronArray.addInputs(inputs);
        assertEquals(10, neuronArray.getInputs().sum(), 0.0);
        neuronArray.update();
        assertEquals(10, neuronArray.getActivations().sum(), 0.0);
        neuronArray.update(); // Should clear to 0
        assertEquals(0, neuronArray.getActivations().sum(), 0.0);
    }

    @Test
    public void testSetLocation() {
        Point2D location = neuronArray.getLocation();
        neuronArray.setLocation(location);
        assertEquals(location.getX(), neuronArray.getLocation().getX(), 0.001);
        assertEquals(location.getY(), neuronArray.getLocation().getY(), 0.001);
    }
}