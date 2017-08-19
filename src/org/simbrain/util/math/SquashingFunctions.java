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
package org.simbrain.util.math;

import static org.junit.Assert.*;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.junit.Test;

/**
 * Static squashing function methods.
 *
 * @author Scott Hotton
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class SquashingFunctions {
    
    /**
     * The hyperbolic tangent given an upper and lower limit and slope for a
     * particular value.
     *
     * @param val the input to the tanh function
     * @param ceil the desired maximum value (upper boundary) of tanh
     * @param floor the desired minimum value (lower boundary) of tanh
     * @param slope the desired slope of the tanh function for val == 0
     * @return the value of val after being passed through a tanh function with
     *         these parameters
     */
    public static double tanh(double val, double ceil, double floor,
            double slope) {
        double diff = ceil - floor;
        double a = (2 * slope) / diff;
        return (diff / 2) * Math.tanh(a * val) + ((ceil + floor) / 2);
    }
    
    public static void tanh(DoubleMatrix valIn, DoubleMatrix valOut, double ceil, double floor, double slope) {
        double diff = ceil - floor;
        double a = (2 * slope) / diff;
        if (valIn != valOut) {
            valOut.copy(valIn);
        }
        MatrixFunctions.tanhi(valOut.muli(a));
        valOut.muli(diff/2);
        valOut.addi((ceil+floor)/2);
    }

    /**
     * The logistic function given an upper and lower limit and slope for a
     * particular value.
     *
     * @param val the input to the logistic function
     * @param ceil the desired maximum value (upper boundary) of logistic
     * @param floor the desired minimum value (lower boundary) of logistic
     * @param slope the desired slope of the logistic function for val == 0
     * @return the value of val after being passed through a logistic function
     *         with these parameters
     */
    public static double logistic(double val, double ceil, double floor,
            double slope) {
        double diff = ceil - floor;
        return diff * logisticFunc(slope * val / diff) + floor;
    }

    public static void logistic(DoubleMatrix valIn, DoubleMatrix valOut, double ceil, double floor,
            double slope) {
        double diff = ceil - floor; 
        if (valIn != valOut) {
            valOut.copy(valIn);
        }
        valOut.muli(-slope/diff);
        MatrixFunctions.expi(valOut);
        valOut.addi(1);
        valOut.rdivi(diff);
        valOut.addi(floor);
    }
    
    /**
     * Returns the standard logistic. Helper method so that the full logistic
     * function doesn't have to be written out every time it's invoked.
     *
     * @param x input argument
     * @return result of logistic
     */
    private static double logisticFunc(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    /**
     * The arctan function given an upper and lower limit and slope for a
     * particular value.
     *
     * @param val the input to the arctan function
     * @param ceil the desired maximum value (upper boundary) of arctan
     * @param floor the desired minimum value (lower boundary) of arctan
     * @param slope the desired slope of the arctan function for val == 0
     * @return the value of val after being passed through a arctan function
     *         with these parameters
     */
    public static double atan(double val, double ceil, double floor,
            double slope) {
        double diff = ceil - floor;
        double a = (Math.PI * slope) / diff;
        return (diff / Math.PI) * Math.atan(a * val) + ((ceil + floor) / 2);
    }

    public static void atan(DoubleMatrix valIn, DoubleMatrix valOut, double ceil, double floor, double slope) {
        double diff = ceil - floor;
        double a = (Math.PI * slope) / diff;
        if (valIn != valOut) {
            valOut.copy(valIn);
        }
        valOut.muli(a);
        MatrixFunctions.atani(valOut);
        valOut.muli(diff/Math.PI);
        valOut.addi((ceil+floor)/2);
    }
    
    /*
     * ****************************************************************
     * _____________________Inverse Functions ________________________*
     * ****************************************************************
     */

    /**
     * The inverse hyperbolic tangent given an upper and lower limit and slope
     * for a particular value for the hyperbolic tangent before being inverted.
     *
     * @param val the input to the inverse tanh function
     * @param ceil the desired maximum value (upper boundary) of the tanh
     *            function that this is an inverse of
     * @param floor the desired minimum value (lower boundary) of the tanh
     *            function that this is an inverse of
     * @param slope the desired slope of the tanh function function that this is
     *            an inverse of for val == 0
     * @return the value of val after being passed through the inverse tanh
     *         function with these parameters
     */
    public static double invTanh(double val, double ceil, double floor,
            double slope) {
        double z = 0.5 * (((val - floor) / (ceil - floor)) - 0.5);
        return (Math.log((1 + z)) / (1 - z));
    }
    
    public static void invTan(DoubleMatrix valIn, DoubleMatrix valOut, double ceil, double floor, double slope) {
        if (valIn != valOut) {
            valOut.copy(valIn);
        }
        valOut.addi(-floor);
        valOut.rdivi(ceil-floor);
        valOut.addi(-0.5);
        valOut.muli(0.5);
        DoubleMatrix temp;
        if(valOut.isVector())
            temp = new DoubleMatrix(valOut.toArray());
        else
            temp = new DoubleMatrix(valOut.toArray2()); // TODO: make this not dumb
        valOut.addi(1);
        temp.rsubi(1);
        valOut.rdivi(temp);
        MatrixFunctions.logi(valOut);
    }

    /**
     * Returns the results of the inverse of the standard sigmoidal (logistic)
     * function.
     *
     * @param val the input to the inverse logistic function
     * @param ceil the desired maximum value (upper boundary) of the logistic
     *            function that this is an inverse of
     * @param floor the desired minimum value (lower boundary) of the logistic
     *            function that this is an inverse of
     * @param slope the desired slope of the logistic function function that
     *            this is an inverse of for val == 0
     * @return the value of val after being passed through the inverse logistic
     *         function with these parameters.
     */
    public static double invLogistic(double val, double ceil, double floor,
            double slope) {
        double diff = ceil - floor;
        return diff * -Math.log(diff / (val - floor) - 1) / slope;
    }

    /**
     * Returns the result of the inverse arctangent or tangent function.
     *
     * @param val the input to the inverse arctan (tan) function
     * @param ceil the desired maximum value (upper boundary) of the arctan
     *            function that this is an inverse of
     * @param floor the desired minimum value (lower boundary) of the arctan
     *            function that this is an inverse of
     * @param slope the desired slope of the arctan function function that this
     *            is an inverse of for val == 0
     * @return the value of val after being passed through the inverse arctan
     *         function with these parameters
     */
    public static double invAtan(double val, double ceil, double floor,
            double slope) {
        double a = (Math.PI * slope) / (ceil - floor);
        double diff = ceil - floor;
        double z = ((val - ((ceil + floor) / 2)) * (Math.PI / diff));
        return Math.tan(z) / a;
    }

    /*
     * ****************************************************************
     * _____________________Derivative Functions _____________________*
     * ****************************************************************
     */

    /**
     * The derivative of the hyperbolic tangent given the original function's
     * upper and lower bounds, and slope for a given value.
     *
     * @param val the input to the derivative of the tanh function
     * @param ceil the desired maximum value (upper boundary) of the tanh
     *            function that this is the derivative of
     * @param floor the desired minimum value (lower boundary) of the tanh
     *            function that this is the derivative of
     * @param slope the desired slope of the tanh function function that this is
     *            the derivative of for val == 0
     * @return the value of val after being passed through the derivative of the
     *         tanh function with these parameters
     */
    public static double derivTanh(double val, double ceil, double floor,
            double slope) {
        double diff = ceil - floor;
        double a = (2 * slope) / diff;
        return diff / 2 * a * Math.pow(1 / Math.cosh(a * val), 2);
    }
    
    public static void derivTanh(DoubleMatrix valIn, DoubleMatrix valOut,  double ceil, double floor, double slope) {
        double diff = ceil - floor;
        double a = (2 * slope) / diff;
        if(valIn != valOut) {
            valOut.copy(valIn);
            
        }
    }

    /**
     * The derivative of the logistic function given the original function's
     * upper and lower bounds, and slope for a given value.
     *
     * @param val the input to the derivative of the logistic function
     * @param ceil the desired maximum value (upper boundary) of the logistic
     *            function that this is the derivative of
     * @param floor the desired minimum value (lower boundary) of the logistic
     *            function that this is the derivative of
     * @param slope the desired slope of the logistic function function that
     *            this is the derivative of for val == 0
     * @return the value of val after being passed through the derivative of the
     *         logistic function with these parameters.
     */
    public static double derivLogistic(double val, double ceil, double floor,
            double slope) {
        double diff = ceil - floor;
        return slope * logisticFunc(slope * val / diff)
                * (1 - logisticFunc(slope * val / diff));
    }
    
    
    /**
     * ASSUMES valIn is the ACTIVATION NOT NET input since this is how the derivative of the logistic sigmoid works...
     * for this reason ceil and floor do nothing.
     * @param valIn
     * @param valOut
     * @param ceil
     * @param floor
     * @param slope
     */
    public static void derivLogistic(DoubleMatrix valIn, DoubleMatrix valOut,
            double ceil, double floor, double slope) {
        if (valIn != valOut) {
            valOut.copy(valIn);
        }
        for (int ii = 0; ii < valOut.data.length; ++ii) {
            valOut.data[ii] = slope * valOut.data[ii] * (1 - valOut.data[ii]);
        }
    }

    /**
     * The derivative of the arc tangent given the original function's upper and
     * lower bounds, and slope for a given value.
     *
     * @param val the input to the derivative of arctan (tan) function
     * @param ceil the desired maximum value (upper boundary) of the arctan
     *            function that this is the derivative of
     * @param floor the desired minimum value (lower boundary) of the arctan
     *            function that this is the derivative of
     * @param slope the desired slope of the arctan function function that this
     *            is the derivative of for val == 0
     * @return the value of val after being passed through the derivative of the
     *         arctan function with these parameters
     */
    public static double derivAtan(double val, double ceil, double floor,
            double slope) {
        double diff = ceil - floor;
        double a = (Math.PI * slope) / diff;
        return a * (diff / Math.PI) * (1 / (1 + Math.pow(a * val, 2)));
    }

    @Test
    public void testSigmoids() {
        assertEquals(0, atan(0,-1,1,1), 0.01);
        assertEquals(0, logistic(0,-1,1,1), 0.01);
        assertEquals(0, tanh(0,-1,1,1), 0.01);
        
        assertEquals(.5, atan(0,0,1,1), 0.01);
        assertEquals(.5, logistic(0,0,1,1), 0.01);
        assertEquals(.5, tanh(0,0,1,1), 0.01);

        // Below fails at tolerance of .01
        assertEquals(1, atan(10,-1,1,1), 0.1); 
        assertEquals(1, logistic(10,-1,1,1), 0.1);
        assertEquals(1, tanh(10,-1,1,1), 0.1);

        assertEquals(-1, atan(-10,-1,1,1), 0.1);
        assertEquals(-1, logistic(-10,-1,1,1), 0.1);
        assertEquals(-1, tanh(-10,-1,1,1), 0.1);
    }
    
    @Test
    public void testDerivatives() {
    }
    
    @Test
    public void testInverses() {
    }
    
    @Test
    public void testJBlasBackedFunctions() {
        DoubleMatrix testIn = DoubleMatrix.zeros(5);
        DoubleMatrix testOut = DoubleMatrix.zeros(5);
        DoubleMatrix testVals = DoubleMatrix.zeros(5);
        
        tanh(testIn, testOut,-1,1,1);
        assertArrayEquals(testVals.data, testOut.data, .01);
        
        testVals.addi(.5);
        tanh(testIn, testOut,0,1,1);
        assertArrayEquals(testVals.data, testOut.data, .01);
    }
    
}
