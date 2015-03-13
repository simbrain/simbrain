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
package org.simbrain.network.trainers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.simbrain.util.Utils;
import org.simbrain.util.math.NumericMatrix;

/**
 * Represents input data, target data, a way of iterating through it, a
 * validation subset, etc. Not all of these features will be used by every class
 * that uses this.
 *
 * @author Jeff Yoshimi
 *
 */
public class TrainingSet {

    /**
     * Input data.
     */
    private double[][] inputData;

    /**
     * Target Data
     */
    private double[][] targetData;

    /** Percentage of data to use for validation. */
    private double percentValidation = .25;

    /**
     * Construct training set object.
     */
    public TrainingSet() {
    }

    /**
     * Construct training set from given data.
     *
     * @param inputData input data
     * @param targetData target data
     */
    public TrainingSet(double[][] inputData, double[][] targetData) {
        super();
        this.inputData = inputData;
        this.targetData = targetData;
    }

    /** Indices for validation subset of training data. */
    private List<Integer> validationIndices;

    /** Indices for validation subset of training data. */
    private List<Integer> mainIndices;

    /**
     * Populate the validation and main index sets. TODO: This is a first draft!
     */
    public void setIndexSets() {
        int numRows = (int) (inputData.length * percentValidation);
        System.out.println("num rows:" + numRows);
        validationIndices = new ArrayList<Integer>();
        mainIndices = new ArrayList<Integer>();
        List<Integer> temp = new ArrayList<Integer>();
        for (int i = 0; i < inputData.length; i++) {
            temp.add(new Integer(i));
        }
        Collections.shuffle(temp);
        validationIndices = new ArrayList<Integer>(temp.subList(0, numRows));
        mainIndices = new ArrayList<Integer>(temp.subList(numRows, temp.size()));

        System.out.println(Arrays.asList(temp));
        System.out.println(Arrays.asList(validationIndices));
        System.out.println(Arrays.asList(mainIndices));
    }

    /**
     * Set the input data.
     *
     * @param inputData the data to set.
     */
    public void setInputData(double[][] inputData) {
        this.inputData = inputData;
    }

    /**
     * Set the target data.
     *
     * @param targetData the data to set
     */
    public void setTargetData(double[][] targetData) {
        this.targetData = targetData;
    }

    /**
     * @return the inputData
     */
    public double[][] getInputData() {
        return inputData;
    }

    /**
     * @return the targetData
     */
    public double[][] getTargetData() {
        return targetData;
    }

    /**
     * Wrap input data in a DataMatrix Object.
     *
     * @return the data matrix containing this data.
     */
    public NumericMatrix getInputDataMatrix() {
        return new NumericMatrix() {

            @Override
            public void setData(double[][] data) {
                setInputData(data);
            }

            @Override
            public double[][] getData() {
                return getInputData();
            }
        };
    }

    /**
     * Wrap target data in a DataMatrix Object.
     *
     * @return the data matrix containing this data.
     */
    public NumericMatrix getTargetDataMatrix() {
        return new NumericMatrix() {

            @Override
            public void setData(double[][] data) {
                setTargetData(data);
            }

            @Override
            public double[][] getData() {
                return getTargetData();
            }
        };
    }

    /**
     * @return the percentValidation
     */
    public double getPercentValidation() {
        return percentValidation;
    }

    /**
     * @param percentValidation the percentValidation to set
     */
    public void setPercentValidation(double percentValidation) {
        this.percentValidation = percentValidation;
    }

    /**
     * Add a row of values to the input data table.
     *
     * @param newRow the new values to add.
     */
    public void addRow(double[] newRow) {
        double[][] matActivations = new double[1][newRow.length];
        matActivations[0] = newRow;

        if (inputData == null) {
            inputData = new double[1][newRow.length];
            inputData = matActivations;
        } else {
            double newInputData[][] = Utils.concatenate(inputData,
                    matActivations);
            inputData = newInputData;
        }
    }

}
