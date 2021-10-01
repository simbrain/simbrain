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
package org.simbrain.util.widgets;

import javax.swing.*;


/**
 * A SpinnerNumberModel that allows a null state.
 * When the edited objects return different states the field is shown as empty.
 *
 * @author O. J. Coleman
 * @see JNumberSpinnerWithNull
 */
public class SpinnerNumberModelWithNull extends SpinnerNumberModel {
    private Number stepSize, value;
    private Comparable minimum, maximum;


    /**
     * Constructs a <code>SpinnerModel</code> that represents
     * a closed sequence of
     * numbers from <code>minimum</code> to <code>maximum</code>.  The
     * <code>nextValue</code> and <code>previousValue</code> methods
     * compute elements of the sequence by adding or subtracting
     * <code>stepSize</code> respectively.  All of the parameters
     * must be mutually <code>Comparable</code>, <code>value</code>
     * and <code>stepSize</code> must be instances of <code>Integer</code>
     * <code>Long</code>, <code>Float</code>, or <code>Double</code>.
     * <p>
     * The <code>minimum</code> and <code>maximum</code> parameters
     * can be <code>null</code> to indicate that the range doesn't
     * have an upper or lower bound.
     * If <code>value</code> or <code>stepSize</code> is <code>null</code>,
     * or if both <code>minimum</code> and <code>maximum</code>
     * are specified and <code>minimum &gt; maximum</code> then an
     * <code>IllegalArgumentException</code> is thrown.
     * Similarly if <code>(minimum &lt;= value &lt;= maximum</code>) is false,
     * an <code>IllegalArgumentException</code> is thrown.
     *
     * @param value    the current (non <code>null</code>) value of the model
     * @param minimum  the first number in the sequence or <code>null</code>
     * @param maximum  the last number in the sequence or <code>null</code>
     * @param stepSize the difference between elements of the sequence
     * @throws IllegalArgumentException if stepSize or value is
     *                                  <code>null</code> or if the following expression is false:
     *                                  <code>minimum &lt;= value &lt;= maximum</code>
     */
    public SpinnerNumberModelWithNull(Number value, Comparable minimum, Comparable maximum, Number stepSize) {
        if (stepSize == null) {
            throw new IllegalArgumentException("stepSize must be non-null");
        }
        this.value = value;
        this.minimum = minimum;
        this.maximum = maximum;
        this.stepSize = stepSize;
    }


    /**
     * Changes the lower bound for numbers in this sequence.
     * If <code>minimum</code> is <code>null</code>,
     * then there is no lower bound.  No bounds checking is done here;
     * the new <code>minimum</code> value may invalidate the
     * <code>(minimum &lt;= value &lt;= maximum)</code>
     * invariant enforced by the constructors.  This is to simplify updating
     * the model, naturally one should ensure that the invariant is true
     * before calling the <code>getNextValue</code>,
     * <code>getPreviousValue</code>, or <code>setValue</code> methods.
     * <p>
     * Typically this property is a <code>Number</code> of the same type
     * as the <code>value</code> however it's possible to use any
     * <code>Comparable</code> with a <code>compareTo</code>
     * method for a <code>Number</code> with the same type as the value.
     * For example if value was a <code>Long</code>,
     * <code>minimum</code> might be a Date subclass defined like this:
     * <pre>
     * MyDate extends Date {  // Date already implements Comparable
     *     public int compareTo(Long o) {
     *         long t = getTime();
     *         return (t &lt; o.longValue() ? -1 : (t == o.longValue() ? 0 : 1));
     *     }
     * }
     * </pre>
     * <p>
     * This method fires a <code>ChangeEvent</code>
     * if the <code>minimum</code> has changed.
     *
     * @param minimum a <code>Comparable</code> that has a
     *                <code>compareTo</code> method for <code>Number</code>s with
     *                the same type as <code>value</code>
     * @see #getMinimum
     * @see #setMaximum
     * @see SpinnerModel#addChangeListener
     */
    public void setMinimum(Comparable minimum) {
        if ((minimum == null) ? (this.minimum != null) : !minimum.equals(this.minimum)) {
            this.minimum = minimum;
            fireStateChanged();
        }
    }


    /**
     * Returns the first number in this sequence.
     *
     * @return the value of the <code>minimum</code> property
     * @see #setMinimum
     */
    public Comparable getMinimum() {
        return minimum;
    }


    /**
     * Changes the upper bound for numbers in this sequence.
     * If <code>maximum</code> is <code>null</code>, then there
     * is no upper bound.  No bounds checking is done here; the new
     * <code>maximum</code> value may invalidate the
     * <code>(minimum &lt;= value &lt; maximum)</code>
     * invariant enforced by the constructors.  This is to simplify updating
     * the model, naturally one should ensure that the invariant is true
     * before calling the <code>next</code>, <code>previous</code>,
     * or <code>setValue</code> methods.
     * <p>
     * Typically this property is a <code>Number</code> of the same type
     * as the <code>value</code> however it's possible to use any
     * <code>Comparable</code> with a <code>compareTo</code>
     * method for a <code>Number</code> with the same type as the value.
     * See <a href="#setMinimum(java.lang.Comparable)">
     * <code>setMinimum</code></a> for an example.
     * <p>
     * This method fires a <code>ChangeEvent</code> if the
     * <code>maximum</code> has changed.
     *
     * @param maximum a <code>Comparable</code> that has a
     *                <code>compareTo</code> method for <code>Number</code>s with
     *                the same type as <code>value</code>
     * @see #getMaximum
     * @see #setMinimum
     * @see SpinnerModel#addChangeListener
     */
    public void setMaximum(Comparable maximum) {
        if ((maximum == null) ? (this.maximum != null) : !maximum.equals(this.maximum)) {
            this.maximum = maximum;
            fireStateChanged();
        }
    }


    /**
     * Returns the last number in the sequence.
     *
     * @return the value of the <code>maximum</code> property
     * @see #setMaximum
     */
    public Comparable getMaximum() {
        return maximum;
    }


    /**
     * Changes the size of the value change computed by the
     * <code>getNextValue</code> and <code>getPreviousValue</code>
     * methods.  An <code>IllegalArgumentException</code>
     * is thrown if <code>stepSize</code> is <code>null</code>.
     * <p>
     * This method fires a <code>ChangeEvent</code> if the
     * <code>stepSize</code> has changed.
     *
     * @param stepSize the size of the value change computed by the
     *                 <code>getNextValue</code> and <code>getPreviousValue</code> methods
     * @see #getNextValue
     * @see #getPreviousValue
     * @see #getStepSize
     * @see SpinnerModel#addChangeListener
     */
    public void setStepSize(Number stepSize) {
        if (stepSize == null) {
            throw new IllegalArgumentException("null stepSize");
        }
        if (!stepSize.equals(this.stepSize)) {
            this.stepSize = stepSize;
            fireStateChanged();
        }
    }


    /**
     * Returns the size of the value change computed by the
     * <code>getNextValue</code>
     * and <code>getPreviousValue</code> methods.
     *
     * @return the value of the <code>stepSize</code> property
     * @see #setStepSize
     */
    public Number getStepSize() {
        return stepSize;
    }


    private Number incrValue(int dir) {
        if (value == null) {
            return null;
        }

        Number newValue;
        if ((stepSize instanceof Float) || (stepSize instanceof Double)) {
            newValue = value.doubleValue() + (stepSize.doubleValue() * (double) dir);
        } else {
            long v = value.longValue() + (stepSize.longValue() * (long) dir);

            if (value instanceof Long) {
                newValue = v;
            } else if (value instanceof Integer) {
                newValue = (int) v;
            } else if (value instanceof Short) {
                newValue = (short) v;
            } else {
                newValue = (byte) v;
            }
        }

        if ((maximum != null) && (maximum.compareTo(newValue) < 0)) {
            return null;
        }
        if ((minimum != null) && (minimum.compareTo(newValue) > 0)) {
            return null;
        } else {
            return newValue;
        }
    }


    /**
     * Returns the next number in the sequence.
     *
     * @return <code>value + stepSize</code> or <code>null</code> if the sum
     * exceeds <code>maximum</code>.
     * @see SpinnerModel#getNextValue
     * @see #getPreviousValue
     * @see #setStepSize
     */
    public Object getNextValue() {
        return incrValue(+1);
    }


    /**
     * Returns the previous number in the sequence.
     *
     * @return <code>value - stepSize</code>, or
     * <code>null</code> if the sum is less
     * than <code>minimum</code>.
     * @see SpinnerModel#getPreviousValue
     * @see #getNextValue
     * @see #setStepSize
     */
    public Object getPreviousValue() {
        return incrValue(-1);
    }


    /**
     * Returns the value of the current element of the sequence,
     * or null if no value is set.
     *
     * @return the value property, or null if no value is set.
     * @see #setValue
     */
    public Number getNumber() {
        return value;
    }


    /**
     * Returns the value of the current element of the sequence,
     * or null if no value is set.
     *
     * @return the value property, or null if no value is set.
     * @see #setValue
     * @see #getNumber
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the current value for this sequence.
     * <p>
     * This method fires a <code>ChangeEvent</code> if the value has changed.
     *
     * @param value the current <code>Number</code>
     *              for this sequence, or null for no value.
     * @throws IllegalArgumentException if <code>value</code> is
     *                                  not null and is not a <code>Number</code>
     * @see #getNumber
     * @see #getValue
     * @see SpinnerModel#addChangeListener
     */
    public void setValue(Object value) {
        if (value == null) {
            if (this.value != null) {
                this.value = null;
                fireStateChanged();
            }
        } else {
            if (!(value instanceof Number)) {
                throw new IllegalArgumentException("illegal value");
            }
            if (!value.equals(this.value)) {
                Number newValue = (Number) value;

                if ((maximum != null) && (maximum.compareTo(newValue) < 0)) {
                    newValue = (Number) maximum;
                } else if ((minimum != null) && (minimum.compareTo(newValue) > 0)) {
                    newValue = (Number) minimum;
                }

                this.value = newValue;
                fireStateChanged();
            }
        }
    }
}
