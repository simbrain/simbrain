package org.simbrain.network.matrix;

import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WeightMatrixTest {

    Network net = new Network();
    NeuronArray na1 = new NeuronArray(net, 2);
    NeuronArray na2 = new NeuronArray(net, 2);
    WeightMatrix wm = new WeightMatrix(net, na1, na2);

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
        na1.setActivations(new double[]{1, 2});
        wm.setWeights(new double[]{1, 2, 3, 4});
        assertArrayEquals(new double[]{5,11}, wm.weightsTimesSource(), 0.0);
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