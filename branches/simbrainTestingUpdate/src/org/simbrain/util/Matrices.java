package org.simbrain.util;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class Matrices {
    /**
     * The difference between 1 and the smallest exactly representable number
     * greater than one. Gives an upper bound on the relative error due to
     * rounding of floating point numbers.
     *
     * @author Ahmed Abdelkader
     *         (http://www.blogger.com/profile/01141303576931872803)
     * @retrived:
     *            http://the-lost-beauty.blogspot.com/2009/04/moore-penrose-pseudoinverse
     *            -in-jama.html
     */
    public static double MACHEPS = 2E-16;

    /**
     * Updates MACHEPS for the executing machine.
     */
    public static void updateMacheps() {
        MACHEPS = 1;
        do
            MACHEPS /= 2;
        while (1 + MACHEPS / 2 != 1);
    }

    /**
     * Computes the Moore–Penrose pseudoinverse using the SVD method.
     *
     * Modified version of the original implementation by Kim van der Linde.
     */
    public static Matrix pinv(Matrix x) {
        if (x.rank() < 1)
            return null;
        if (x.getColumnDimension() > x.getRowDimension())
            return pinv(x.transpose()).transpose();
        SingularValueDecomposition svdX = new SingularValueDecomposition(x);
        double[] singularValues = svdX.getSingularValues();
        double tol = Math.max(x.getColumnDimension(), x.getRowDimension())
                * singularValues[0] * MACHEPS;
        double[] singularValueReciprocals = new double[singularValues.length];
        for (int i = 0; i < singularValues.length; i++)
            singularValueReciprocals[i] = Math.abs(singularValues[i]) < tol ? 0
                    : (1.0 / singularValues[i]);
        double[][] u = svdX.getU().getArray();
        double[][] v = svdX.getV().getArray();
        int min = Math.min(x.getColumnDimension(), u[0].length);
        double[][] inverse = new double[x.getColumnDimension()][x
                .getRowDimension()];
        for (int i = 0; i < x.getColumnDimension(); i++)
            for (int j = 0; j < u.length; j++)
                for (int k = 0; k < min; k++)
                    inverse[i][j] += v[i][k] * singularValueReciprocals[k]
                            * u[j][k];
        return new Matrix(inverse);
    }
}
