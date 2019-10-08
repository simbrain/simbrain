package org.simbrain.network.core;

import org.junit.Before;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.FloatBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.*;

public class NeuronArrayTest {

    private NeuronArray neuronArray;

    @Before
    public void setUp() {
        Network network = new Network();
        neuronArray = new NeuronArray(network, 10);
    }

    @Test
    public void testInputMatchesOutput() {
        INDArray inputs = Nd4j.rand(1, 10);
        neuronArray.setInputArray(inputs);
        assertEquals(0, (neuronArray.getNeuronArray().sub(inputs)).sumNumber().intValue());
    }

    @Test
    public void testSetValuesAsConsumable() {
        double[] values = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        neuronArray.setValues(values);
        assertArrayEquals(neuronArray.getNeuronArray().toDoubleVector(), values, 0.01);
    }

    @Test
    public void testIfBufferTypeIsFloatAfterCreation() {
        assertTrue(neuronArray.getNeuronArray().data() instanceof FloatBuffer);
    }

    @Test
    public void testIfBufferTyeIsFloatAfterSetValue() {
        double[] values = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        neuronArray.setValues(values);
        assertTrue(neuronArray.getNeuronArray().data() instanceof FloatBuffer);
    }

    @Test
    public void testIfBufferTypeIsFloatAfterUpdate() {
        neuronArray.update();
        assertTrue(neuronArray.getNeuronArray().data() instanceof FloatBuffer);
    }
}