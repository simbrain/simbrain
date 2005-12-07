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

import java.util.ArrayList;


/**
 * <B>ProjectSammon.java</B> Implements gradient descent to compute image of Sammon projection.
 */
public class ProjectSammon extends Projector {
    private ArrayList Y;
    private double[] X_i; // temporary variables
    private double[] X_j; // temporary variables
    private double[] Y_i; // temporary variables
    private double[] Y_j; // temporary variables
    private double[] Y_m; // temporary variables
    private double[] Y_n; // temporary variables
    private double[] Y_new; // temporary variables
    private double[][] dstar; // matrix of "upstairs" interpoint distances
    private double[][] d; // matrix of "downstairs" interpoint distances
    private double dstarSum;
    private double PartSum;
    private double currentCloseness;
    private double E;
    private int lowDimension;
    private int numPoints;
    private int highDimension;

    public ProjectSammon() {
    }

    public ProjectSammon(final Settings set) {
        theSettings = set;
    }

    /**
     * Perform necessary initialization
     */
    public void init(final Dataset up, final Dataset down) {
        super.init(up, down);

        setLowDimension(downstairs.getDimensions());
        setNumPoints(upstairs.getNumPoints());
        setHighDimension(upstairs.getDimensions());
        upstairs.calculateDistances();
        setDstar(upstairs.getDistances());
        setDstarSum(upstairs.getSumDistances());
        downstairs.perturbOverlappingPoints(theSettings.getPerturbationAmount());
    }

    /**
     * Iterate the Sammon algorithm and return currentCloseness
     */
    public double iterate() {
        if (upstairs.getNumPoints() < 2) {
            return 0;
        }

        //Question: Why do I need the new below?  Why can't I use refs for Y_m and Y_i?
        setY(new ArrayList(downstairs.getDataset()));
        downstairs.calculateDistances();
        setD(downstairs.getDistances());

        // Computes partials
        for (int m = 0; m < getNumPoints(); m++) {
            setY_m(new double[getLowDimension()]);
            setY_m((double[]) getY().get(m));
            setY_new(new double[getLowDimension()]);

            for (int n = 0; n < getLowDimension(); n++) {
                setPartSum(0.0);

                for (int i = 0; i < getNumPoints(); i++) {
                    if (i == m) {
                        continue;
                    }

                    setY_i(new double[getLowDimension()]);
                    setY_i((double[]) getY().get(i));
                    setPartSum(getPartSum() + (((getDstar()[i][m] - getD()[i][m]) * (getY_i()[n] - getY_m()[n])) / getDstar()[i][m] / getD()[i][m]));
                }

                getY_new()[n] = getY_m()[n] - ((theSettings.getEpsilon() * 2 * getPartSum()) / getDstarSum());
            }

            downstairs.setPoint(m, getY_new());
        }

        // Computes Closeness
        setE(0.0);

        for (int i = 0; i < getNumPoints(); i++) {
            for (int j = i + 1; j < getNumPoints(); j++) {
                setE(getE() + (((getDstar()[i][j] - getD()[i][j]) * (getDstar()[i][j] - getD()[i][j])) / getDstar()[i][j]));
            }
        }

        setCurrentCloseness(getE() / getDstarSum());

        //System.out.println("currentCloseness = " + currentCloseness);
        return getCurrentCloseness();
    }

    public boolean isIterable() {
        return true;
    }

    public boolean isExtendable() {
        return false;
    }

    public void project() {
    }

    /**
     * @return step size for Sammon map
     */
    public double getEpsilon() {
        return theSettings.getEpsilon();
    }

    /**
     * @param d step size for Sammon map
     */
    public void setEpsilon(final double d) {
        theSettings.setEpsilon(d);
    }

    /**
     * @param y The y to set.
     */
    void setY(final ArrayList y) {
        Y = y;
    }

    /**
     * @return Returns the y.
     */
    ArrayList getY() {
        return Y;
    }

    /**
     * @param x_i The x_i to set.
     */
    void setX_i(final double[] x_i) {
        X_i = x_i;
    }

    /**
     * @return Returns the x_i.
     */
    double[] getX_i() {
        return X_i;
    }

    /**
     * @param x_j The x_j to set.
     */
    void setX_j(final double[] x_j) {
        X_j = x_j;
    }

    /**
     * @return Returns the x_j.
     */
    double[] getX_j() {
        return X_j;
    }

    /**
     * @param y_i The y_i to set.
     */
    void setY_i(final double[] y_i) {
        Y_i = y_i;
    }

    /**
     * @return Returns the y_i.
     */
    double[] getY_i() {
        return Y_i;
    }

    /**
     * @param y_j The y_j to set.
     */
    void setY_j(final double[] y_j) {
        Y_j = y_j;
    }

    /**
     * @return Returns the y_j.
     */
    double[] getY_j() {
        return Y_j;
    }

    /**
     * @param y_m The y_m to set.
     */
    void setY_m(final double[] y_m) {
        Y_m = y_m;
    }

    /**
     * @return Returns the y_m.
     */
    double[] getY_m() {
        return Y_m;
    }

    /**
     * @param y_n The y_n to set.
     */
    void setY_n(final double[] y_n) {
        Y_n = y_n;
    }

    /**
     * @return Returns the y_n.
     */
    double[] getY_n() {
        return Y_n;
    }

    /**
     * @param y_new The y_new to set.
     */
    void setY_new(final double[] y_new) {
        Y_new = y_new;
    }

    /**
     * @return Returns the y_new.
     */
    double[] getY_new() {
        return Y_new;
    }

    /**
     * @param dstar The dstar to set.
     */
    void setDstar(final double[][] dstar) {
        this.dstar = dstar;
    }

    /**
     * @return Returns the dstar.
     */
    double[][] getDstar() {
        return dstar;
    }

    /**
     * @param d The d to set.
     */
    void setD(final double[][] d) {
        this.d = d;
    }

    /**
     * @return Returns the d.
     */
    double[][] getD() {
        return d;
    }

    /**
     * @param dstarSum The dstarSum to set.
     */
    void setDstarSum(final double dstarSum) {
        this.dstarSum = dstarSum;
    }

    /**
     * @return Returns the dstarSum.
     */
    double getDstarSum() {
        return dstarSum;
    }

    /**
     * @param partSum The partSum to set.
     */
    void setPartSum(final double partSum) {
        PartSum = partSum;
    }

    /**
     * @return Returns the partSum.
     */
    double getPartSum() {
        return PartSum;
    }

    /**
     * @param currentCloseness The currentCloseness to set.
     */
    void setCurrentCloseness(final double currentCloseness) {
        this.currentCloseness = currentCloseness;
    }

    /**
     * @return Returns the currentCloseness.
     */
    double getCurrentCloseness() {
        return currentCloseness;
    }

    /**
     * @param e The e to set.
     */
    void setE(final double e) {
        E = e;
    }

    /**
     * @return Returns the e.
     */
    double getE() {
        return E;
    }

    /**
     * @param lowDimension The lowDimension to set.
     */
    void setLowDimension(final int lowDimension) {
        this.lowDimension = lowDimension;
    }

    /**
     * @return Returns the lowDimension.
     */
    int getLowDimension() {
        return lowDimension;
    }

    /**
     * @param numPoints The numPoints to set.
     */
    void setNumPoints(final int numPoints) {
        this.numPoints = numPoints;
    }

    /**
     * @return Returns the numPoints.
     */
    int getNumPoints() {
        return numPoints;
    }

    /**
     * @param highDimension The highDimension to set.
     */
    void setHighDimension(final int highDimension) {
        this.highDimension = highDimension;
    }

    /**
     * @return Returns the highDimension.
     */
    int getHighDimension() {
        return highDimension;
    }
}
