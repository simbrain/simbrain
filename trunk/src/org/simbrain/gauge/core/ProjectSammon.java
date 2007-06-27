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
package org.simbrain.gauge.core;

import java.util.ArrayList;

/**
 * <B>ProjectSammon.java</B> Implements gradient descent to compute image of
 * Sammon projection.
 */
public class ProjectSammon extends Projector {
    /** Array of datasets. */
    private ArrayList yArray;

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

    /** Number of low dimension datasets. */
    private int lowDimension;

    /** Number of points. */
    private int numPoints;

    /** Number of high dimension datasets. */
    private int highDimension;

    /**
     * Default sammon projector constructor.
     */
    public ProjectSammon() {
    }

    /**
     * Sammon projector constructor.
     * @param set projector settings
     */
    public ProjectSammon(final Settings set) {
        theSettings = set;
    }

    /**
     * Perform necessary initialization.
     * @param up Upstairs dataset
     * @param down Downstairs dataset
     */
    public void init(final Dataset up, final Dataset down) {
        super.init(up, down);

        setLowDimension(downstairs.getDimensions());
        setNumPoints(upstairs.getNumPoints());
        setHighDimension(upstairs.getDimensions());
        setDstar(upstairs.getDistances());
        setDstarSum(upstairs.getSumDistances());
        downstairs
                .perturbOverlappingPoints(theSettings.getPerturbationAmount());
    }

    /**
     * Iterate the Sammon algorithm and return currentCloseness.
     * @return closeness of points
     */
    public double iterate() {
        if (upstairs.getNumPoints() < 2) {
            return 0;
        }

        // Question: Why do I need the new below? Why can't I use refs for Y_m
        // and Y_i?
        setYArray(downstairs.getDatasetCopy());
        setD(downstairs.getDistances());

        // Computes partials
        for (int m = 0; m < getNumPoints(); m++) {
            setYM(new double[getLowDimension()]);
            setYM((double[]) getYArray().get(m));
            setYNew(new double[getLowDimension()]);

            for (int n = 0; n < getLowDimension(); n++) {
                setPartialSum(0.0);

                for (int i = 0; i < getNumPoints(); i++) {
                    if (i == m) {
                        continue;
                    }

                    setYI(new double[getLowDimension()]);
                    setYI((double[]) getYArray().get(i));
                    setPartialSum(getPartialSum()
                            + (((getDstar()[i][m] - getD()[i][m]) * (getYI()[n] - getYM()[n]))
                                    / getDstar()[i][m] / getD()[i][m]));
                }

                getYNew()[n] = getYM()[n]
                        - ((theSettings.getEpsilon() * 2 * getPartialSum()) / getDstarSum());
            }

            downstairs.setPoint(m, getYNew());
        }

        // Computes Closeness
        setE(0.0);

        for (int i = 0; i < getNumPoints(); i++) {
            for (int j = i + 1; j < getNumPoints(); j++) {
                setE(getE()
                        + (((getDstar()[i][j] - getD()[i][j]) * (getDstar()[i][j] - getD()[i][j])) / getDstar()[i][j]));
            }
        }

        setCurrentCloseness(getE() / getDstarSum());

        // System.out.println("currentCloseness = " + currentCloseness);
        return getCurrentCloseness();
    }

    /**
     * @return is projection iterable.
     */
    public boolean isIterable() {
        return true;
    }

    /**
     * @return is projection extendable.
     */
    public boolean isExtendable() {
        return false;
    }

    /**
     * Default projection constructor.
     */
    public void project() {
    }

    /**
     * @return step size for Sammon map
     */
    public double getEpsilon() {
        return theSettings.getEpsilon();
    }

    /**
     * @param d
     *            step size for Sammon map
     */
    public void setEpsilon(final double d) {
        theSettings.setEpsilon(d);
    }

    /**
     * @param y
     *            The yArray to set.
     */
    void setYArray(final ArrayList y) {
        yArray = y;
    }

    /**
     * @return Returns the yArray.
     */
    ArrayList getYArray() {
        return yArray;
    }

    /**
     * @param xI
     *            The xI to set.
     */
    void setXI(final double[] xI) {
        this.xI = xI;
    }

    /**
     * @return Returns the xI.
     */
    double[] getXI() {
        return xI;
    }

    /**
     * @param xJ
     *            The xJ to set.
     */
    void setXJ(final double[] xJ) {
        this.xJ = xJ;
    }

    /**
     * @return Returns the xJ.
     */
    double[] getXJ() {
        return xJ;
    }

    /**
     * @param yI
     *            The yI to set.
     */
    void setYI(final double[] yI) {
        this.yI = yI;
    }

    /**
     * @return Returns the yI.
     */
    double[] getYI() {
        return yI;
    }

    /**
     * @param yJ
     *            The yJ to set.
     */
    void setYJ(final double[] yJ) {
        this.yJ = yJ;
    }

    /**
     * @return Returns the yJ.
     */
    double[] getYJ() {
        return yJ;
    }

    /**
     * @param yM
     *            The yM to set.
     */
    void setYM(final double[] yM) {
        this.yM = yM;
    }

    /**
     * @return Returns the yM.
     */
    double[] getYM() {
        return yM;
    }

    /**
     * @param yN
     *            The yN to set.
     */
    void setYN(final double[] yN) {
        this.yN = yN;
    }

    /**
     * @return Returns the yN.
     */
    double[] getYN() {
        return yN;
    }

    /**
     * @param yNew
     *            The yNew to set.
     */
    void setYNew(final double[] yNew) {
        this.yNew = yNew;
    }

    /**
     * @return Returns the yNew.
     */
    double[] getYNew() {
        return yNew;
    }

    /**
     * @param dstar
     *            The dstar to set.
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
     * @param d
     *            The d to set.
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
     * @param dstarSum
     *            The dstarSum to set.
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
     * @param partSum
     *            The partSum to set.
     */
    void setPartialSum(final double partSum) {
        partialSum = partSum;
    }

    /**
     * @return Returns the partSum.
     */
    double getPartialSum() {
        return partialSum;
    }

    /**
     * @param currentCloseness
     *            The currentCloseness to set.
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
     * @param e
     *            The e to set.
     */
    void setE(final double e) {
        this.e = e;
    }

    /**
     * @return Returns the e.
     */
    double getE() {
        return e;
    }

    /**
     * @param lowDimension
     *            The lowDimension to set.
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
     * @param numPoints
     *            The numPoints to set.
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
     * @param highDimension
     *            The highDimension to set.
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

    /**
     * @see Projector
     */
    public boolean hasDialog() {
        return true;
    }
}
