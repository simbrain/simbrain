package org.simbrain.network.matrix;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;

import static org.junit.Assert.assertEquals;

public class WeightMatrixTest {

    @Test
    public void testMatrixOperations() {

        Network net = new Network();
        NeuronGroup ng1 = new NeuronGroup(net, 2);
        NeuronGroup ng2 = new NeuronGroup(net, 2);
        WeightMatrix wm = new WeightMatrix(net, ng1, ng2);

        // Set first entry to 4
        long start_time = System.currentTimeMillis();
        wm.getWeightMatrix().set(0, 0, 4);
        assertEquals(4, wm.getWeightMatrix().get(0,0), 0.0);

        // Set to ((0,0);(0,0)) and check sum
        start_time = System.currentTimeMillis();
        wm.setWeights(new double[]{0, 0, 0, 0});
        // System.out.println(wm);
        assertEquals(0.0, wm.getWeightMatrix().sum(), 0.0);

        // Add 1 to each entry. Should get ((1,1);(1,1))
        wm.getWeightMatrix().add(1.0);
        // System.out.println(wm);
        assertEquals(4.0, wm.getWeightMatrix().sum(), 0.0);

        // TODO: Failing
        // Multiply by itself.  Should get ((2,2);(2,2))
        start_time = System.currentTimeMillis();
        // wm.getWeightMatrix().mm(wm.getWeightMatrix());
        // System.out.println(wm);
        // assertEquals(8.0, wm.getWeightMatrix().sum(), 0.0);
    }

    // @Test
    public void large_matrix_multiplication() {

        Network net = new Network();
        NeuronGroup ng1 = new NeuronGroup(net, 1000);
        NeuronGroup ng2 = new NeuronGroup(net, 1000);
        WeightMatrix wm = new WeightMatrix(net, ng1, ng2);
        wm.getWeightMatrix().add(1.0001);

        long start_time = System.currentTimeMillis();
        // for (int i = 0; i < 1000; i++) {
        wm.getWeightMatrix().mm(wm.getWeightMatrix());
        // }
        long stop_time = System.currentTimeMillis();
        long difference = stop_time - start_time;
        System.out.println("Compute time for large matrix product: " + difference + " ms");
    }

}