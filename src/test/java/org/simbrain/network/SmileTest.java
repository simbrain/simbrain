package org.simbrain.network;

import org.junit.Test;
import smile.math.matrix.Matrix;

import static org.junit.Assert.assertEquals;

import smile.math.matrix.Matrix.EVD;
import smile.stat.distribution.GaussianDistribution;

/**
 * Testing the Smile package. https://haifengl.github.io/
 */
public class SmileTest {

    long start_time;
    long stop_time;
    long difference;

    @Test
    public void initialize_2d_matrix() {

        // Create a 2x3 zero matrix
        start_time = System.currentTimeMillis();
        var matrix_a = Matrix.eye(2, 3);

        var rows = matrix_a.nrows();
        var cols = matrix_a.ncols();
        var result = "Shape: " + rows + " x " + cols;
        // System.out.println("Shape: "+rows+" x "+cols);
        assertEquals("Shape: 2 x 3", result);
        stop_time = System.currentTimeMillis();
        difference = stop_time - start_time;
        System.out.println("Compute time for 2d init: " + difference + " ms");
    }

    @Test
    public void matrix_2x3_ones() {
        double[][] ones = {{1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}};
        var matrix_ones = new Matrix(2, 3, ones);
        // System.out.println(matrix_ones);
        var sums = matrix_ones.sum();
        // System.out.println("Sums: "+sums);
        assertEquals(6.0, sums, 0.0);
    }

    @Test
    public void set_entry_matrix() {

        double[][] ones = {{1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}};
        var matrix_ones = new Matrix(2, 3, ones);
        // Set the value of entry 1,3 to 3

        matrix_ones.set(0, 2, 3);

        // Retrieve the value of that entry and confirm it is 3

        // System.out.println("Value at 1,3: "+matrix_ones.get(0,2));
        assertEquals(3, matrix_ones.get(0, 2), 0.0);
    }

    @Test
    public void matrix_multiplication() {
        start_time = System.currentTimeMillis();
        double[][] twos = {{2.0, 2.0, 2.0}, {2.0, 2.0, 2.0}};
        var matrix_twos = new Matrix(2, 3, twos);
        double[][] threes = {{3.0, 3.0}, {3.0, 3.0}, {3.0, 3.0}};
        var matrix_threes = new Matrix(3, 2, threes);
        stop_time = System.currentTimeMillis();
        // System.out.println(result_matrix);
        difference = stop_time - start_time;
        System.out.println("Compute time for matrix product: " + difference + " ms");
    }

    // @Test
    public void large_matrix_multiplication() {
        var large_matrix = new Matrix(1000, 1000);
        large_matrix.add(1.0001);
        start_time = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            large_matrix.mm(large_matrix);
        }
        stop_time = System.currentTimeMillis();
        difference = stop_time - start_time;
        System.out.println("Compute time for large matrix product: " + difference + " ms");
    }

    // @Test
    public void compute_matrix_eigenvalue() {

        var large_matrix = Matrix.rand(50, 50, new GaussianDistribution(0, 1));

        // Testing for eigenvalue

        start_time = System.currentTimeMillis();
        var eigenvalue = large_matrix.eigen();
        stop_time = System.currentTimeMillis();
        difference = stop_time - start_time;
        var eigen_matrix = new Matrix.EVD(eigenvalue.wr, eigenvalue.wi, eigenvalue.Vl, eigenvalue.Vr);
        System.out.println("Compute time for eigenvalue computation: " + difference + " ms");
        // System.out.println(eigen_matrix.diag());
    }

    // @Test
    public void compute_matrix_LU_Decomposition() {
        //Matrix Decomposition

        var large_matrix = Matrix.rand(50, 50, new GaussianDistribution(0, 1));
        start_time = System.currentTimeMillis();
        var decompose = large_matrix.lu();
        stop_time = System.currentTimeMillis();
        difference = stop_time - start_time;
        System.out.println("Compute time for LU Decomposition: " + difference + " ms");
        // System.out.println(decompose.lu);
    }

}
