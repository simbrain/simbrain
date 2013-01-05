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

import java.util.ArrayList;

/**
 * <B>ProjectSammon.java</B> implements gradient descent to compute image of
 * Sammon projection.
 *
 * TODO: Possibly convert all arrays to datapoints.
 */
public class ProjectSammon extends IterableProjectionMethod {

    /** Array of datasets. */
    private ArrayList<DataPoint> yArray;

    /** Amount by which to perturb overlapping points. */
    protected double perturbationAmount = ProjectorPreferences
            .getPerturbationAmount();

    /**
     * Sammon Map Settings. epsilon or "magic factor".
     */
    private double epsilon = ProjectorPreferences.getEpsilon();

    /** Temporary variables. */
    private double[] xI;

    /** Temporary variables. */
    private double[] xJ;

    /** Temporary variables. */
    private double[] yI;

    /** Temporary variables. */
    private double[] yJ;

    /** Temporary variables. */
    private double[] yM;

    /** Temporary variables. */
    private double[] yN;

    /** Temporary variables. */
    private double[] yNew;

    /** Matrix of "upstairs" interpoint distances. */
    private double[][] dstar;

    /** Matrix of "downstairs" interpoint distances. */
    private double[][] d;

    /** Sum distances. */
    private double dstarSum;

    /** Parital sum. */
    private double partialSum;

    /** Current closeness of datapoints. */
    private double currentCloseness;

    /** Temporary variable. */
    private double e;

    /**
     * Default sammon projector constructor.
     */
    public ProjectSammon(Projector projector) {
        super(projector);
    }

    @Override
    public void init() {
        dstar = projector.getUpstairs().getDistances();
        dstarSum = projector.getUpstairs().getSumDistances();
        projector.getDownstairs().perturbOverlappingPoints(perturbationAmount);
        setNeedsReInit(false);
    }

    @Override
    public void project() {
    }

    @Override
    public void iterate() {

        if (projector.getUpstairs().getNumPoints() < 2) {
            return;
        }

        // If new points were added re-initialize
        if (needsReInit()) {
            init();
        }

        yArray = projector.getDownstairs().getDatasetCopy();
        d = projector.getDownstairs().getDistances();

        // Computes partials
        for (int m = 0; m < projector.getNumPoints(); m++) {
            yM = yArray.get(m).getVector();
            yNew = new double[projector.getDownstairs().getDimensions()];

            for (int n = 0; n < projector.getDownstairs().getDimensions(); n++) {
                partialSum = 0;

                for (int i = 0; i < projector.getNumPoints(); i++) {
                    if (i == m) {
                        continue;
                    }

                    yI = yArray.get(i).getVector();
                    partialSum += (((dstar[i][m] - d[i][m]) * (yI[n] - yM[n]))
                            / dstar[i][m] / d[i][m]);
                }

                yNew[n] = yM[n] - ((epsilon * 2 * partialSum) / dstarSum);
            }

            projector.getDownstairs().getPoint(m).setData(yNew);
        }

        // Computes Closeness
        e = 0;
        for (int i = 0; i < projector.getNumPoints(); i++) {
            for (int j = i + 1; j < projector.getNumPoints(); j++) {
                e += ((dstar[i][j] - d[i][j]) * (dstar[i][j] - d[i][j]))
                        / dstar[i][j];
            }
        }

        currentCloseness = e / dstarSum;
        setError(currentCloseness);
        projector.fireProjectorColorsChanged();
        //System.out.println("currentCloseness = " + currentCloseness);
    }

    /**
     * @return the epsilon
     */
    public double getEpsilon() {
        return epsilon;
    }

    /**
     * @param epsilon the epsilon to set
     */
    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

}
