/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util.projection;

import java.util.Arrays;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;


/**
 * <B>ProjectPCA</B> Projects the high-dimensional dataset along its two principal components to the low-d dataset.
 */
public class ProjectPCA extends ProjectionMethod {

    /**
     * Defalut PCA project.
     */
    public ProjectPCA() {
    }

    /**
     * PCA project.
     * @param set Project Settings
     */
    public ProjectPCA(final Settings set) {
        theSettings = set;
    }

    /**
     * Projects data points.
     */
    public void project() {
        if (upstairs.getNumPoints() < 1) {
            return;
        }

        int lowdim = downstairs.getDimensions();
        int updim = upstairs.getDimensions();

        // Get e-vals and e-vectors of covariance matrix
        Matrix m = upstairs.getCovarianceMatrix();
        EigenvalueDecomposition ed = m.eig();
        Matrix eVecs = ed.getV().transpose();
        double[] evalsArray = ed.getRealEigenvalues();
        Matrix eVals = new Matrix(evalsArray, updim);

        //printMatrix(e_vecs);
        //printMatrix(e_vals);
        // Sort e-vectors and place them in a separate matrix, "matrix_projector"
        Matrix combined = new Matrix(updim, updim + 1);
        Matrix matrixProjector = new Matrix(lowdim, updim);
        combined.setMatrix(0, updim - 1, 1, updim, eVecs);
        combined.setMatrix(0, updim - 1, 0, 0, eVals);
        Arrays.sort(evalsArray);

        //printMatrix(combined);
        //printArray(evals_array);
        //Go through the evals_array, starting with the largest value
        for (int i = updim - 1, k = 0; i >= (updim - lowdim); i--) {
            double val = evalsArray[i];

            //find the row this corresponds to and set that row
            for (int j = 0; j < updim; j++) {
                if (combined.get(j, 0) == val) {
                    if (k >= lowdim) {
                        break; // needed for cases of repeated e-vals?
                    }

                    matrixProjector.setMatrix(k, k, 0, updim - 1, combined.getMatrix(j, j, 1, updim));
                    k++;
                }
            }
        }

        downstairs.clear();
        
        //printMatrix(matrix_projector);
        //project the points along the principal components
        for (int i = 0; i < upstairs.getNumPoints(); i++) {
            Matrix uppoint = new Matrix(updim, 1);

            for (int j = 0; j < updim; ++j) {
                uppoint.set(j, 0, upstairs.getComponent(i, j));
            }

            Matrix lowpoint = matrixProjector.times(uppoint);
            double[] columnPackedCopy = lowpoint.getColumnPackedCopy();
            
            downstairs.addPoint(columnPackedCopy);
        }
    }

    /**
     * @return is projector extendable.
     */
    public boolean isExtendable() {
        return true;
    }

    /**
     * @return is project iterable.
     */
    public boolean isIterable() {
        return false;
    }

    /**
     * @return default times to iterate.
     */
    public double iterate() {
        return 0;
    }

    /**
     * @see ProjectionMethod
     */
    public boolean hasDialog() {
        return false;
    }
}
