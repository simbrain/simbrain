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
package org.simbrain.plot.barchart;

import java.awt.EventQueue;

import org.simbrain.workspace.SingleAttributeConsumer;

/**
 * Represents a bar in a bar chart.
 */
public class BarChartConsumer extends SingleAttributeConsumer<Double> {

    /** Reference to BarChartComponent. */
    private BarChartComponent component;

    /** Index. */
    private Integer index;

    /**
     * Construct  BarChartConsumer.
     *
     * @param plot the parent component
     * @param name the name of this consumer (displayed in the plot)
     */
    public BarChartConsumer(final BarChartComponent component, final Integer index) {
        this.component = component;
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(final Double val) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                component.getModel().getDataset().setValue(val, new Integer(1),
                        index);
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    public String getKey() {
        return "BarChartData" + index;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return getKey();
    }

    /**
     * {@inheritDoc}
     */
    public BarChartComponent getParentComponent() {
        return component;
    }

    /**
     * Return index.
     *
     * @return index
     */
    public Integer getIndex() {
            return index;
    }
    
}

