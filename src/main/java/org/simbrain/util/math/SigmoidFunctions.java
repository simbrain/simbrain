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

import smile.math.matrix.Matrix;

/**
 * Static sigmoidal or "squashing" function methods.
 *
 * @author Scott Hotton
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public class SigmoidFunctions {

    /**
     * The hyperbolic tangent given an upper and lower limit and slope for a
     * particular value.
     *
     * @param val   the input to the tanh function
     * @param ceil  the desired maximum value (upper boundary) of tanh
     * @param floor the desired minimum value (lower boundary) of tanh
     * @param slope the desired slope of the tanh function for val == 0
     * @return the value of val after being passed through a tanh function with
     * these parameters
     */
    public static double tanh(double val, double ceil, double floor, double slope) {
        double diff = ceil - floor;
        double a = (2 * slope) / diff;
        return (diff / 2) * Math.tanh(a * val) + ((ceil + floor) / 2);
    }

    public static Matrix tanh(Matrix in, double ceil, double floor, double slope) {
        // Smile does not use BLAS for any of these operations so we are reusing the scalar function
        var output = new Matrix(in.nrow(), in.ncol());
        for (int i = 0; i < output.nrow(); i++) {
            output.set(i, 0, tanh(in.get(i, 0), ceil, floor, slope));
        }
        return output;
    }

    /**
     * Returns the standard logistic. Helper method so that the full logistic
     * function doesn't have to be written out every time it's invoked.
     *
     * @param x input argument
     * @return result of logistic
     */
    private static double logistic(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    /**
     * The logistic function given an upper and lower limit and slope for a
     * particular value.
     *
     * @param val   the input to the logistic function
     * @param ceil  the desired maximum value (upper boundary) of logistic
     * @param floor the desired minimum value (lower boundary) of logistic
     * @param slope the desired slope of the logistic function for val == 0
     * @return the value of val after being passed through a logistic function with these parameters
     * @author Scott Hotton
     */
    public static double logistic(double val, double ceil, double floor, double slope) {
        return (ceil - floor) * logistic((4 * slope * val) / (ceil - floor)) + floor;
    }

    /**
     * Compute the scaled logistic function on a Matrix of input values.
     *
     * @param in    The input array
     * @param ceil  The upper limit of the logistic curve.
     * @param floor The lower limit of the logistic curve.
     * @param slope The slope of the curve at val == 0.
     */
    public static Matrix logistic(Matrix in, double ceil, double floor, double slope) {
        // Smile does not use BLAS for any of these operations so we are reusing the scalar function
        var output = new Matrix(in.nrow(), in.ncol());
        for (int i = 0; i < output.nrow(); i++) {
            output.set(i, 0, logistic(in.get(i, 0), ceil, floor, slope));
        }
        return output;
    }

    /**
     * The arctan function given an upper and lower limit and slope for a
     * particular value.
     *
     * @param val   the input to the arctan function
     * @param ceil  the desired maximum value (upper boundary) of arctan
     * @param floor the desired minimum value (lower boundary) of arctan
     * @param slope the desired slope of the arctan function for val == 0
     * @return the value of val after being passed through a arctan function
     * with these parameters
     */
    public static double atan(double val, double ceil, double floor, double slope) {
        double diff = ceil - floor;
        double a = (Math.PI * slope) / diff;
        return (diff / Math.PI) * Math.atan(a * val) + ((ceil + floor) / 2);
    }

    public static Matrix atan(Matrix in, double ceil, double floor, double slope) {
        // Smile does not use BLAS for any of these operations so we are reusing the scalar function
        var output = new Matrix(in.nrow(), in.ncol());
        for (int i = 0; i < output.nrow(); i++) {
            output.set(i, 0, atan(in.get(i, 0), ceil, floor, slope));
        }
        return output;
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
     * @param val   the input to the inverse tanh function
     * @param ceil  the desired maximum value (upper boundary) of the tanh
     *              function that this is an inverse of
     * @param floor the desired minimum value (lower boundary) of the tanh
     *              function that this is an inverse of
     * @param slope the desired slope of the tanh function function that this is
     *              an inverse of for val == 0
     * @return the value of val after being passed through the inverse tanh
     * function with these parameters
     *
     * TODO: Slope is ignored, and this is failing for values near ceil and floor
     */
    public static double invTanh(double val, double ceil, double floor, double slope) {
        double z = 0.5 * (((val - floor) / (ceil - floor)) - 0.5);
        return Math.log((1 + z)) / (1 - z);
    }

    /**
     * Returns the results of the inverse of the standard sigmoidal (logistic)
     * function.
     *
     * @param val   the input to the inverse logistic function
     * @param ceil  the desired maximum value (upper boundary) of the logistic
     *              function that this is an inverse of
     * @param floor the desired minimum value (lower boundary) of the logistic
     *              function that this is an inverse of
     * @param slope the desired slope of the logistic function function that
     *              this is an inverse of for val == 0
     * @return the value of val after being passed through the inverse logistic
     * function with these parameters.
     */
    public static double invLogistic(double val, double ceil, double floor, double slope) {
        double diff = ceil - floor;
        return diff * -Math.log(diff / (val - floor) - 1) / slope;
    }

    /**
     * Returns the result of the inverse arctangent or tangent function.
     *
     * @param val   the input to the inverse arctan (tan) function
     * @param ceil  the desired maximum value (upper boundary) of the arctan
     *              function that this is an inverse of
     * @param floor the desired minimum value (lower boundary) of the arctan
     *              function that this is an inverse of
     * @param slope the desired slope of the arctan function function that this
     *              is an inverse of for val == 0
     * @return the value of val after being passed through the inverse arctan
     * function with these parameters
     */
    public static double invAtan(double val, double ceil, double floor, double slope) {
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
     * @param val   the input to the derivative of the tanh function
     * @param ceil  the desired maximum value (upper boundary) of the tanh
     *              function that this is the derivative of
     * @param floor the desired minimum value (lower boundary) of the tanh
     *              function that this is the derivative of
     * @param slope the desired slope of the tanh function function that this is
     *              the derivative of for val == 0
     * @return the value of val after being passed through the derivative of the
     * tanh function with these parameters
     */
    public static double derivTanh(double val, double ceil, double floor, double slope) {
        double diff = ceil - floor;
        double a = (2 * slope) / diff;
        double t = 1 / Math.cosh(a * val);
        return slope * t * t;
    }

    /**
     * The derivative of the logistic function given the original function's
     * upper and lower bounds, and slope for a given value.
     *
     * @param val   the input to the derivative of the logistic function
     * @param ceil  the desired maximum value (upper boundary) of the logistic
     *              function that this is the derivative of
     * @param floor the desired minimum value (lower boundary) of the logistic
     *              function that this is the derivative of
     * @param slope the desired slope of the logistic function function that this is
     *              the derivative of for val == 0
     * @return the value of val after being passed through the derivative of the
     * logistic function with these parameters.
     * @author Scott Hotton
     */
    public static double derivLogistic(double val, double ceil, double floor, double slope) {
        double logisticOutput = logistic(val, ceil, floor, slope);
        return (4 * slope) / ((ceil - floor) * (ceil - floor)) * (logisticOutput - floor) * (ceil - logisticOutput);
    }

    /**
     * The derivative of the arc tangent given the original function's upper and
     * lower bounds, and slope for a given value.
     *
     * @param val   the input to the derivative of arctan (tan) function
     * @param ceil  the desired maximum value (upper boundary) of the arctan
     *              function that this is the derivative of
     * @param floor the desired minimum value (lower boundary) of the arctan
     *              function that this is the derivative of
     * @param slope the desired slope of the arctan function function that this
     *              is the derivative of for val == 0
     * @return the value of val after being passed through the derivative of the
     * arctan function with these parameters
     */
    public static double derivAtan(double val, double ceil, double floor, double slope) {
        double diff = ceil - floor;
        double a = (Math.PI * slope) / diff;
        return a * (diff / Math.PI) * (1 / (1 + Math.pow(a * val, 2)));
    }

}
