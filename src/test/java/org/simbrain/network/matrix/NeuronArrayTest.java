package org.simbrain.network.matrix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import smile.math.matrix.Matrix;

import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NeuronArrayTest {

    Network net = new Network();
    NeuronArray na =  new NeuronArray(net, 10);

    @BeforeEach
    public void setUp() {
        net.addNetworkModelAsync(na);
    }

    @Test
    public void testUpdate() {
        Matrix inputs = new Matrix(10, 1);
        inputs.fill(1.0);
        na.addInputs(inputs);
        assertEquals(10, na.getInputs().sum(), 0.0);
        na.update();
        assertEquals(10, na.getActivations().sum(), 0.0);
        na.update(); // Should clear to 0
        assertEquals(0, na.getActivations().sum(), 0.0);
    }
    
    @Test
    public void testSetLocation() {
        Point2D location = na.getLocation();
        na.setLocation(location);
        assertEquals(location.getX(), na.getLocation().getX(), 0.001);
        assertEquals(location.getY(), na.getLocation().getY(), 0.001);
    }

    @Test
    public void testExcitatoryInputs() {
        var na1= new NeuronArray(net, 2);
        na1.setActivations(new double[]{1, 1});
        var naTarget = new NeuronArray(net, 3);
        WeightMatrix wm1 = new WeightMatrix(net, na1, naTarget);
        wm1.setWeights(new double[]{5, -1, 1, 1,-1,-1});
        net.addNetworkModelsAsync(na1, naTarget, wm1);
        // Expecting 5 for first row, 2 for second row, and 0 for the last row
        assertArrayEquals(new double[]{5,2,0}, naTarget.getExcitatoryInputs());

        // Add a second input array
        var na2 = new NeuronArray(net, 2);
        na2.setActivations(new double[]{1, 1});
        WeightMatrix wm2 = new WeightMatrix(net, na2, naTarget);
        wm2.setWeights(new double[]{1, 0, -1, -1,1,1});
        net.addNetworkModelsAsync(na2, wm2);
        // Now expecting 6, 2, 2
        assertArrayEquals(new double[]{6,2,2}, naTarget.getExcitatoryInputs());
    }

    @Test
    public void testInhibitoryInputs() {
        var na1 = new NeuronArray(net, 2);
        na1.setActivations(new double[]{1, 1});
        var naTarget = new NeuronArray(net, 3);
        WeightMatrix wm1 = new WeightMatrix(net, na1, naTarget);
        wm1.setWeights(new double[]{5, -1, 1, 1,-1,-1});
        net.addNetworkModelsAsync(na1, naTarget, wm1);
        // Expecting -1 for first row, 0 for second row, and -2 for the last row
        assertArrayEquals(new double[]{-1,0,-2}, naTarget.getInhibitoryInputs());

        // Add a second input array
        var na2 = new NeuronArray(net, 2);
        na2.setActivations(new double[]{1, 1});
        WeightMatrix wm2 = new WeightMatrix(net, na2, naTarget);
        wm2.setWeights(new double[]{1, 0, -1, -1,1,1});
        net.addNetworkModelsAsync(na2, wm2);
        // Now expecting -1, -2, -2
        assertArrayEquals(new double[]{-1,-2,-2}, naTarget.getInhibitoryInputs());
    }
}