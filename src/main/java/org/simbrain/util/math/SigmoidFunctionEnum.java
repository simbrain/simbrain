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

        @Override
        public String toString() {
            return "Arctan";
        }

        @Override
        public double valueOf(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.atan(val, ceil, floor, slope);
        }

        @Override
        public double derivVal(double val, double ceil, double floor, double slope) {
           return SigmoidFunctions.derivAtan(val, ceil, floor, slope);
        }

        @Override
        public Matrix valueOf(Matrix in, double ceil, double floor, double slope) {
            return SigmoidFunctions.atan(in, ceil, floor, slope);
        }

    },

    /**
     * Logistic Function.
     */
    LOGISTIC {

        @Override
        public String toString() {
            return "Logistic";
        }

        @Override
        public double valueOf(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.logistic(val, ceil, floor, slope);
        }

        @Override
        public double derivVal(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.derivLogistic(val, ceil, floor, slope);
        }

        @Override
        public Matrix valueOf(Matrix in, double ceil, double floor, double slope) {
            return SigmoidFunctions.logistic(in, ceil, floor, slope);
        }

    },

    /**
     * Hyperbolic Tangent.
     */
    TANH {

        @Override
        public String toString() {
            return "Tanh";
        }

        @Override
        public double valueOf(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.tanh(val, ceil, floor, slope);
        }


        @Override
        public double derivVal(double val, double ceil, double floor, double slope) {
            return SigmoidFunctions.derivTanh(val, ceil, floor, slope);
        }

        @Override
        public Matrix valueOf(Matrix in, double ceil, double floor, double slope) {
            return SigmoidFunctions.tanh(in, ceil, floor, slope);
        }

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
