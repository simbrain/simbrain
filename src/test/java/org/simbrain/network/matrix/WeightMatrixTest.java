package org.simbrain.network.matrix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import smile.math.matrix.Matrix;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WeightMatrixTest {

    Network net;
    NeuronArray na1;
    NeuronArray na2;
    WeightMatrix wm;

    @BeforeEach
    public void setUp() {
        net = new Network();
        na1 = new NeuronArray(net, 2);
        na2 = new NeuronArray(net, 2);
        wm = new WeightMatrix(net, na1, na2);
        net.addNetworkModels(List.of(na1, na2, wm));
    }
    @Test
    public void testMatrixOperations() {

        // Set first entry to 4
        wm.getWeightMatrix().set(0, 0, 4);
        assertEquals(4, wm.getWeightMatrix().get(0,0), 0.0);

        // Set to ((0,0);(0,0)) and check sum
        wm.setWeights(new double[]{0, 0, 0, 0});
        assertEquals(0.0, wm.getWeightMatrix().sum(), 0.0);

        // Add 1 to each entry. Should get ((1,1);(1,1))
        wm.getWeightMatrix().add(1.0);
        assertEquals(4.0, wm.getWeightMatrix().sum(), 0.0);
    }

    @Test
    public void testSetWeights() {
        wm.setWeights(new double[]{1, 2, 3, 4});
        assertEquals(1.0, wm.getWeightMatrix().get(0,0), 0.0);
        assertEquals(2.0, wm.getWeightMatrix().get(0,1), 0.0);
        assertEquals(3.0, wm.getWeightMatrix().get(1,0), 0.0);
        assertEquals(4.0, wm.getWeightMatrix().get(1,1), 0.0);
    }

    @Test
    public void testDiagonalize() {
        wm.diagonalize(); // assume wm is 2x2
        assertEquals(2, wm.getWeightMatrix().sum(), 0.0);
    }

    @Test
    public void testMatrixProduct() {
        na1.setActivations(new Matrix(new double[]{1, 2}));
        wm.setWeights(new double[]{1, 2, 3, 4});
        assertArrayEquals(new double[]{5,11}, wm.getOutput().col(0), 0.0);
    }

    @Test
    public void testArrayToArray() {
        na1.setActivations(new Matrix(new double[]{.5, -.5}));
        wm.diagonalize();
        net.bufferedUpdate(); // input should be cleared and second array updated
        assertArrayEquals(new double[]{0,0}, na1.getActivations().col(0), 0.0);
        assertArrayEquals(new double[]{.5,-.5}, na2.getActivations().col(0), 0.0);
        net.bufferedUpdate(); // All should be cleared on second update
        assertArrayEquals(new double[]{0,0}, na1.getActivations().col(0), 0.0);
        assertArrayEquals(new double[]{0,0}, na2.getActivations().col(0), 0.0);
    }

    @Test
    public void testArrayToNeuronGroup() {
        na1.setActivations(new Matrix(new double[]{.5, -.5}));
        NeuronGroup ng = new NeuronGroup(net, 2);
        WeightMatrix wm2 = new WeightMatrix(net, na1, ng);
        wm2.diagonalize();
        net.addNetworkModels(List.of(ng, wm2));
        net.update();
        assertArrayEquals(new double[]{.5,-.5}, ng.getActivations(), 0.0);
        net.update(); // All should be cleared on second update
        assertArrayEquals(new double[]{0,0}, ng.getActivations(), 0.0);
    }

    // @Test
    public void large_matrix_multiplication() {

        Network net = new Network();
        NeuronGroup ng1 = new NeuronGroup(net, 1000);
        NeuronGroup ng2 = new NeuronGroup(net, 1000);
        WeightMatrix wm = new WeightMatrix(net, ng1, ng2);
        wm.getWeightMatrix().add(1.0001);

        long start_time = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            wm.getWeightMatrix().mm(wm.getWeightMatrix());
        }
        long stop_time = System.currentTimeMillis();
        long difference = stop_time - start_time;
        System.out.println("Compute time for large matrix product: " + difference + " ms");
    }

}