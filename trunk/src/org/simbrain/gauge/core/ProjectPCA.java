/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.gauge.core;

import java.util.Arrays;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;


/**
 * <B>ProjectPCA</B> Projects the high-dimensional dataset along its two principal components to the low-d dataset.
 */
public class ProjectPCA extends Projector {
    public ProjectPCA() {
    }

    public ProjectPCA(Settings set) {
        theSettings = set;
    }

    public void project() {
        if (upstairs.getNumPoints() < 1) {
            return;
        }

        int lowdim = downstairs.getDimensions();
        int updim = upstairs.getDimensions();

        // Get e-vals and e-vectors of covariance matrix
        Matrix M = upstairs.getCovarianceMatrix();
        EigenvalueDecomposition ed = M.eig();
        Matrix e_vecs = ed.getV().transpose();
        double[] evals_array = ed.getRealEigenvalues();
        Matrix e_vals = new Matrix(evals_array, updim);

        //printMatrix(e_vecs);
        //printMatrix(e_vals);
        // Sort e-vectors and place them in a separate matrix, "matrix_projector"
        Matrix combined = new Matrix(updim, updim + 1);
        Matrix matrix_projector = new Matrix(lowdim, updim);
        combined.setMatrix(0, updim - 1, 1, updim, e_vecs);
        combined.setMatrix(0, updim - 1, 0, 0, e_vals);
        Arrays.sort(evals_array);

        //printMatrix(combined);
        //printArray(evals_array);
        //Go through the evals_array, starting with the largest value
        for (int i = updim - 1, k = 0; i >= (updim - lowdim); i--) {
            double val = evals_array[i];

            //find the row this corresponds to and set that row
            for (int j = 0; j < updim; j++) {
                if (combined.get(j, 0) == val) {
                    if (k >= lowdim) {
                        break; // needed for cases of repeated e-vals?  
                    }

                    matrix_projector.setMatrix(k, k, 0, updim - 1, combined.getMatrix(j, j, 1, updim));
                    k++;
                }
            }
        }

        //printMatrix(matrix_projector);
        //project the points along the principal components
        for (int k = 0; k < upstairs.getNumPoints(); k++) {
            Matrix uppoint = new Matrix(updim, 1);

            for (int j = 0; j < updim; ++j) {
                uppoint.set(j, 0, upstairs.getComponent(k, j));
            }

            Matrix lowpoint = matrix_projector.times(uppoint);

            for (int j = 0; j < lowdim; ++j) {
                downstairs.setPoint(k, lowpoint.getColumnPackedCopy());
            }
        }
    }

    public boolean isExtendable() {
        return true;
    }

    public boolean isIterable() {
        return false;
    }

    public double iterate() {
        return 0;
    }
}
