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
 * An enumerated type containing methods for calculating values of different
 * sigmoid or "squashing" functions, their inverses, and their derivatives.
 *
 * @author Scott Hotton
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public enum SigmoidFunctionEnum {

    ARCTAN {

        /** The max value as x -> inf of f(x) = arctan(x) . */
        private static final double DEFAULT_ARCTAN_CEIL = Math.PI / 2;

        /** The min value as x -> -inf of f(x) = arctan(x) . */
        private static final double DEFAULT_ARCTAN_FLOOR = -Math.PI / 2;

        @Override
        public String toString() {
            return "Arctan";
        }

        @Override
        public double valueOf(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.atan(val, ceil, floor, slope);
        }

        @Override
        public double inverseVal(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.invAtan(val, ceil, floor, slope);
        }

        @Override
        public double derivVal(double val, double ceil, double floor, double slope) {
           return SigmoidFunctions.derivAtan(val, ceil, floor, slope);
        }

        @Override
        public double getDefaultUpperBound() {
            return DEFAULT_ARCTAN_CEIL;
        }

        @Override
        public double getDefaultLowerBound() {
            return DEFAULT_ARCTAN_FLOOR;
        }

        @Override
        public Matrix valueOf(Matrix in, double ceil, double floor, double slope) {
            return SigmoidFunctions.atan(in, ceil, floor, slope);
        }
        //
        // @Override
        // public void inverseVal(Matrix in, Matrix out, double ceil, double floor, double slope) {
        //     SigmoidFunctions.invAtan(in, out, ceil, floor, slope);
        // }
        //
        // @Override
        // public void derivVal(Matrix in, Matrix out, double ceil, double floor, double slope) {
        //     SigmoidFunctions.derivAtan(in, out, ceil, floor, slope);
        // }
        //
        // @Override
        // public void valueAndDeriv(Matrix in, Matrix out, Matrix deriv, double ceil, double floor, double slope) {
        //     valueOf(in, out, ceil, floor, slope);
        //     derivVal(in, deriv, ceil, floor, slope);
        // }

    },

    /**
     * Logistic Function.
     */
    LOGISTIC {

        /** The max value as x -> inf of f(x) = 1 / (1 + e^x). */
        private static final double DEFAULT_LOGISTIC_CEIL = 1.0;

        /** The min value as x -> -inf of f(x) = 1 / (1 + e^x). */
        private static final double DEFAULT_LOGISTIC_FLOOR = 0.0;

        @Override
        public String toString() {
            return "Logistic";
        }

        @Override
        public double valueOf(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.logistic(val, ceil, floor, slope);
        }

        @Override
        public double inverseVal(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.invLogistic(val, ceil, floor, slope);
        }

        @Override
        public double derivVal(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.derivLogistic(val, ceil, floor, slope);
        }

        @Override
        public double getDefaultUpperBound() {
            return DEFAULT_LOGISTIC_CEIL;
        }

        @Override
        public double getDefaultLowerBound() {
            return DEFAULT_LOGISTIC_FLOOR;
        }

        @Override
        public Matrix valueOf(Matrix in, double ceil, double floor, double slope) {
            return SigmoidFunctions.logistic(in, ceil, floor, slope);
        }

        //
        // @Override
        // public void inverseVal(Matrix in, Matrix out, double ceil, double floor, double slope) {
        //     SigmoidFunctions.invLogistic(in, out, ceil, floor, slope);
        // }
        //
        // @Override
        // public void derivVal(Matrix in, Matrix out, double ceil, double floor, double slope) {
        //     SigmoidFunctions.derivLogistic(in, out, ceil, floor, slope);
        // }
        //
        // @Override
        // public void valueAndDeriv(Matrix in, Matrix out, Matrix deriv, double ceil, double floor, double slope) {
        //     SigmoidFunctions.logisticWithDerivative(in, out, deriv, ceil, floor, slope);
        // }

    },

    /**
     * Hyperbolic Tangent.
     */
    TANH {

        /** The max value as x -> inf of f(x) = tanh(x). */
        private static final double DEFAULT_TANH_CEIL = 1.0;

        /** The max value as x -> -inf of f(x) = tanh(x). */
        private static final double DEFAULT_TANH_FLOOR = -1.0;

        @Override
        public String toString() {
            return "Tanh";
        }

        @Override
        public double valueOf(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.tanh(val, ceil, floor, slope);
        }

        @Override
        public double inverseVal(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.invTanh(val, ceil, floor, slope);
        }

        @Override
        public double derivVal(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.derivTanh(val, ceil, floor, slope);
        }

        @Override
        public double getDefaultUpperBound() {
            return DEFAULT_TANH_CEIL;
        }

        @Override
        public double getDefaultLowerBound() {
            return DEFAULT_TANH_FLOOR;
        }

        @Override
        public Matrix valueOf(Matrix in, double ceil, double floor, double slope) {
            return SigmoidFunctions.tanh(in, ceil, floor, slope);
        }
        //
        // @Override
        // public void inverseVal(Matrix in, Matrix out, double ceil, double floor, double slope) {
        //     SigmoidFunctions.invTanh(in, out, ceil, floor, slope);
        // }
        //
        // @Override
        // public void derivVal(Matrix in, Matrix out, double ceil, double floor, double slope) {
        //     SigmoidFunctions.derivTanh(in, out, ceil, floor, slope);
        // }
        //
        // @Override
        // public void valueAndDeriv(Matrix in, Matrix out, Matrix deriv, double ceil, double floor, double slope) {
        //     SigmoidFunctions.tanh(in, out, ceil, floor, slope);
        //     SigmoidFunctions.derivTanh(in, deriv, ceil, floor, slope);
        // }

    };

    /**
     * Gives the value of the given sigomid function for some input value, a
     * ceiling, floor, and slope.
     *
     * @param val   the base value to pass the function
     * @param ceil  the upper limit of the curve
     * @param floor the lower limit of the curve
     * @param slope the slope of the curve at zero
     * @return the output of the given sigmoid function
     */
    public abstract double valueOf(double val, double ceil, double floor, double slope);

    public abstract Matrix valueOf(Matrix input, double ceil, double floor, double slope);

    /**
     * Gives the value of the inverse of the given sigmoid function for some
     * input value, a ceiling, floor, and slope.
     *
     * @param val   the base value to pass the inverse function
     * @param ceil  the upper limit of the sigmoid function being inverted
     * @param floor the lower limit of the sigmoid function being inverted
     * @param slope the slope of the sigmoid function being inverted at zero
     * @return the output of the given sigmoid function's inverse
     */
    public abstract double inverseVal(double val, double ceil, double floor, double slope);

    // public abstract void inverseVal(Matrix valIn, Matrix valOut, double ceil, double floor, double slope);

    /**
     * Gives the value of the derivative of the given sigmoid function for
     * some input value, a ceiling, floor, and slope. All parameters are fed to
     * the sigmoid function and the return value represents the derivative of
     * THAT function.
     *
     * @param val   the base value to pass the function
     * @param ceil  the upper limit of the curve
     * @param floor the lower limit of the curve
     * @param slope the slope of the curve at zero
     * @return the output of the given sigmoid function's derivative
     */
    public abstract double derivVal(double val, double ceil, double floor, double slope);

    // public abstract void derivVal(Matrix valIn, Matrix valOut, double ceil, double floor, double slope);
    //
    // public abstract void valueAndDeriv(Matrix in, Matrix out, Matrix deriv, double ceil, double floor, double slope);

    /**
     * @return the default upper boundary (ceiling) of this particular sigmoid
     * function.
     */
    public abstract double getDefaultUpperBound();

    /**
     * @return the default lower boundary (floor) of this particular sigmoid
     * function.
     */
    public abstract double getDefaultLowerBound();

    /**
     * Helper method to get the list of sigmoid or "squashing" function names as an array
     * Used to populate combo box.
     *
     * @return list of sigmoid or "squashing" function names, as an array.
     */
    public static String[] names() {
        SigmoidFunctionEnum[] states = values();
        String[] names = new String[states.length - 1];
        // The last item is the ... item which should not be part of the list
        for (int i = 0; i < states.length - 1; i++) {
            names[i] = states[i].toString();
        }
        return names;
    }

}
