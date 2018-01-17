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

import org.jblas.DoubleMatrix;

/**
 * An enumerated type containing methods for calculating values of different
 * squashing functions, their inverses, and their derivatives.
 *
 * @author Scott Hotton
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public enum SquashingFunction {

    // TODO:Rename to SquashingFunctionEnum?

    /** Arctangent. */
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
            return SquashingFunctions.atan(val, ceil, floor, slope);
        }

        @Override
        public double inverseVal(double val, double ceil, double floor, double slope) {
            return SquashingFunctions.invAtan(val, ceil, floor, slope);
        }

        @Override
        public double derivVal(double val, double ceil, double floor, double slope) {
            return SquashingFunctions.derivAtan(val, ceil, floor, slope);
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
		public void valueOf(DoubleMatrix in, DoubleMatrix out, double ceil, double floor, double slope) {
			SquashingFunctions.atan(in, out, ceil, floor, slope);
		}

		@Override
		public void inverseVal(DoubleMatrix in, DoubleMatrix out, double ceil, double floor, double slope) {
			SquashingFunctions.invAtan(in, out, ceil, floor, slope);
		}

		@Override
		public void derivVal(DoubleMatrix in, DoubleMatrix out, double ceil, double floor, double slope) {
			SquashingFunctions.derivAtan(in, out, ceil, floor, slope);
		}

		@Override
        public void valueAndDeriv(DoubleMatrix in, DoubleMatrix out, DoubleMatrix deriv, double ceil, double floor,
                                  double slope) {
            valueOf(in, out, ceil, floor, slope);
            derivVal(in, out, ceil, floor, slope);
        }

    },

    /** Logistic Function. */
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
        public double valueOf(double val, double ceil, double floor,
                double slope) {
            return SquashingFunctions.logistic(val, ceil, floor, slope);
        }

        @Override
        public double inverseVal(double val, double ceil, double floor, double slope) {
            return SquashingFunctions.invLogistic(val, ceil, floor, slope);
        }

        @Override
        public double derivVal(double val, double ceil, double floor, double slope) {
            return SquashingFunctions.derivLogistic(val, ceil, floor, slope);
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
		public void valueOf(DoubleMatrix in, DoubleMatrix out, double ceil, double floor, double slope) {
		    SquashingFunctions.logistic(in, out, ceil, floor, slope);
		}

		@Override
		public void inverseVal(DoubleMatrix in, DoubleMatrix out, double ceil, double floor, double slope) {
			SquashingFunctions.invLogistic(in, out, ceil, floor, slope);
		}

		@Override
		public void derivVal(DoubleMatrix in, DoubleMatrix out, double ceil, double floor, double slope) {
		    SquashingFunctions.derivLogistic(in, out, ceil, floor, slope);
		}

		@Override
        public void valueAndDeriv(DoubleMatrix in, DoubleMatrix out, DoubleMatrix deriv, double ceil, double floor,
                                  double slope) {
            SquashingFunctions.logisticWithDerivative(in, out, deriv, ceil, floor, slope);
        }

    },

    /** Hyperbolic Tangent. */
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
        public double valueOf(double val, double ceil, double floor,
                double slope) {
            return SquashingFunctions.tanh(val, ceil, floor, slope);
        }

        @Override
        public double inverseVal(double val, double ceil, double floor,
                double slope) {
            return SquashingFunctions.invTanh(val, ceil, floor, slope);
        }

        @Override
        public double derivVal(double val, double ceil, double floor,
                double slope) {
            return SquashingFunctions.derivTanh(val, ceil, floor, slope);
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
		public void valueOf(DoubleMatrix in, DoubleMatrix out, double ceil, double floor, double slope) {
			SquashingFunctions.derivTanh(in, out, ceil, floor, slope);
		}

		@Override
		public void inverseVal(DoubleMatrix in, DoubleMatrix out, double ceil, double floor, double slope) {
			SquashingFunctions.invTanh(in, out, ceil, floor, slope);
		}

		@Override
		public void derivVal(DoubleMatrix in, DoubleMatrix out, double ceil, double floor, double slope) {
			SquashingFunctions.derivTanh(in, out, ceil, floor, slope);
		}

		@Override
        public void valueAndDeriv(DoubleMatrix in, DoubleMatrix out, DoubleMatrix deriv, double ceil, double floor,
                                  double slope) {
            SquashingFunctions.tanh(in, out, ceil, floor, slope);
            SquashingFunctions.derivTanh(in, out, ceil, floor, slope);
        }

    },

    /**
     * The Null String "..." used in cases where neurons with different
     * squashing functions are selected simultaneously. Note this must be the
     * last item for consistency with the names function below.
     */
    NULL_STRING {

        @Override
        public String toString() {
            return "...";
        }

        @Override
        public double valueOf(double val, double ceil, double floor, double slope) {
            return 0;
        }

        @Override
        public double inverseVal(double val, double ceil, double floor, double slope) {
            return 0;
        }

        @Override
        public double derivVal(double val, double ceil, double floor, double slope) {
            return 0;
        }

        @Override
        public double getDefaultUpperBound() {
            return 0;
        }

        @Override
        public double getDefaultLowerBound() {
            return 0;
        }

		@Override
		public void valueOf(DoubleMatrix valIn, DoubleMatrix valOut, double ceil, double floor, double slope) {}

		@Override
		public void inverseVal(DoubleMatrix valIn, DoubleMatrix valOut, double ceil, double floor, double slope) {}

		@Override
		public void derivVal(DoubleMatrix valIn, DoubleMatrix valOut, double ceil, double floor, double slope) {}

		@Override
        public void valueAndDeriv(DoubleMatrix in, DoubleMatrix out, DoubleMatrix deriv, double ceil, double floor,
                                  double slope) {}
    };

    /*
     * ****************************************************************
     * ________________Universal Abstract Methods_____________________*
     * ****************************************************************
     */

    /**
     * Gives the value of the given squashing function for some input value, a
     * ceiling, floor, and slope.
     *
     * @param val the base value to pass the function
     * @param ceil the upper limit of the curve
     * @param floor the lower limit of the curve
     * @param slope the slope of the curve at zero
     * @return the output of the given squashing function
     */
    public abstract double valueOf(double val, double ceil, double floor,
            double slope);

    public abstract void valueOf(DoubleMatrix valIn, DoubleMatrix valOut, double ceil, double floor,
            double slope);
    
    /**
     * Gives the value of the inverse of the given squashing function for some
     * input value, a ceiling, floor, and slope.
     *
     * @param val the base value to pass the inverse function
     * @param ceil the upper limit of the squashing function being inverted
     * @param floor the lower limit of the squashing function being inverted
     * @param slope the slope of the squashing function being inverted at zero
     * @return the output of the given squashing function's inverse
     */
    public abstract double inverseVal(double val, double ceil, double floor,
            double slope);
    
    public abstract void inverseVal(DoubleMatrix valIn, DoubleMatrix valOut, double ceil, double floor,
            double slope);

    /**
     * Gives the value of the derivative of the given squashing function for
     * some input value, a ceiling, floor, and slope. All parameters are fed to
     * the squashing function and the return value represents the derivative of
     * THAT function.
     *
     * @param val the base value to pass the function
     * @param ceil the upper limit of the curve
     * @param floor the lower limit of the curve
     * @param slope the slope of the curve at zero
     * @return the output of the given squashing function's derivative
     */
    public abstract double derivVal(double val, double ceil, double floor,
            double slope);
    
    public abstract void derivVal(DoubleMatrix valIn, DoubleMatrix valOut, double ceil, double floor, double slope);

    public abstract void valueAndDeriv(DoubleMatrix in, DoubleMatrix out, DoubleMatrix deriv, double ceil,
                                       double floor, double slope);

    /**
     * @return the default upper boundary (ceiling) of this particular squashing
     *         function.
     */
    public abstract double getDefaultUpperBound();

    /**
     * @return the default lower boundary (floor) of this particular squashing
     *         function.
     */
    public abstract double getDefaultLowerBound();


    /**
     * Helper method to get the list of squashing function names as an array
     * Used to populate combo box.
     *
     * @return list of squashing function names, as an array.
     */
    public static String[] names() {
        SquashingFunction[] states = values();
        String[] names = new String[states.length - 1];
        // The last item is the ... item which should not be part of the list
        for (int i = 0; i < states.length - 1; i++) {
            names[i] = states[i].toString();
        }
        return names;
    }

    /**
     * Helper function to make it easy to go from integer index to Squashing
     * function (so that choiceboxes can interact with this).
     * 
     * @param index integer index
     * @return the associated squashing function
     */
    public static SquashingFunction getFunctionFromIndex(int index) {
        return SquashingFunction.values()[index];
    }
    
    /**
     * Another helper function for, in this case, going from functions
     * to indices.
     *
     * @param function the Squashing Function who index is sought
     * @return the index of that function
     */
    public static int getIndexFromFunction(SquashingFunction function) {
        return function.ordinal();
    }

}
