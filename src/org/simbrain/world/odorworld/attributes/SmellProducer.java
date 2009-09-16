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
package org.simbrain.world.odorworld.attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.odorworld.sensors.SmellSensor;

/**
 * Represents a smell sensor as a workspace producer, providing attributes for couplings.
 *
 * @author jyoshimi
 */
public class SmellProducer implements Producer {
    
    /** Parent component for this attribute holder. */
    WorkspaceComponent parentComponent;

    /** The underlying smell sensor. */
    private final SmellSensor sensor;
    
    /** The producing attributes. */
    private ArrayList<ProducingAttribute<?>> producingAttributes
        = new ArrayList<ProducingAttribute<?>>();

    /**
     * Construct a smell producer using a specified smell sensor.
     *
     * @param component parent component
     * @param sensor smell sensor to use
     */
    public SmellProducer(final WorkspaceComponent component, final SmellSensor sensor) {
        this.parentComponent = component;
        this.sensor = sensor;
        
        for (int i = 0; i < sensor.getCurrentValue().length; i++) {
            producingAttributes.add(new SmellAttribute(i));
        }
    }

    /**
     * Smell attribute.
     */
    class SmellAttribute extends AbstractAttribute implements ProducingAttribute<Double> {

        /** Index of the component of the smell vector to sample. */
        int index;

        /**
         * Construct a smell attribute.
         *
         * @param i index of smell vector to sample.
         */
        public SmellAttribute(final int i) {
            index = i;
        }

        /**
         * {@inheritDoc}
         */
        public Producer getParent() {
            return SmellProducer.this;
        }

        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            //System.out.println("-->" + Arrays.toString(sensor.getCurrentValue()));
            return Double.valueOf(sensor.getCurrentValue()[index]);
        }

        /**
         * {@inheritDoc}
         */
        public String getKey() {
            return "Smell-" + (index + 1);
        }

    }

    /**
     * {@inheritDoc}
     */
    public List<? extends ProducingAttribute<?>> getProducingAttributes() {
        return producingAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return sensor.getParent().getName() + "-" + sensor.getName();
    }

    /**
     * {@inheritDoc}
     */
    public WorkspaceComponent getParentComponent() {
        return parentComponent;
    }

}
