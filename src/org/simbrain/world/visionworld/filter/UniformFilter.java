/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.visionworld.filter;

import java.awt.Image;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.simbrain.world.visionworld.Filter;

/**
 * Uniform filter.
 */
public final class UniformFilter
    implements Filter {

    /** Property change support. */
    private final PropertyChangeSupport propertyChangeSupport;

    /** Value. */
    private double value;

    /** Default value. */
    private static final double DEFAULT_VALUE = 1.0d;


    /**
     * Create a new uniform filter.
     */
    public UniformFilter()
    {
        propertyChangeSupport = new PropertyChangeSupport(this);
        setValue(DEFAULT_VALUE);
    }


    /**
     * Set the value for this uniform filter to <code>value</code>.
     *
     * @param value value for this uniform filter
     */
    public void setValue(final double value) {
        double oldValue = this.value;
        this.value = value;
        propertyChangeSupport.firePropertyChange("value", oldValue, this.value);
    }

    /**
     * Return the value for this uniform filter.
     *
     * @return the value for this uniform filter
     */
    public double getValue() {
        return value;
    }

    /** {@inheritDoc} */
    public double filter(final Image image) {
        return value;
    }

    /**
     * Add the specified property change listener.
     *
     * @param listener property change listener to add
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Add the specified property change listener for the specified property.
     *
     * @param propertyName specific property name
     * @param listener property change listener to add
     */
    public void addPropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove the specified property change listener.
     *
     * @param listener property change listener to remove
     */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Remove the specified property change listener for the specified property.
     *
     * @param propertyName specific property name
     * @param listener property change listener to remove
     */
    public void removePropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }
}
