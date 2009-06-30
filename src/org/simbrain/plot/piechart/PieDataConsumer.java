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
package org.simbrain.plot.piechart;

import java.awt.EventQueue;

import org.simbrain.workspace.SingleAttributeConsumer;

/**
 * Represents one possible data value in a pie chart.
 */
public class PieDataConsumer extends SingleAttributeConsumer<Double> {

    /** Reference to gauge. */
    private PieChartComponent component;
        
    /** Index. */
    private Integer index;
    
    /**
     * Construct a PieDataConsumer.
     * 
     * @param data the parent component
     * @param name the name of this consumer (displayed in the plot)
     * @param index of the plot item
     */
    public PieDataConsumer(final PieChartComponent component,
            final Integer index) {
        this.component = component;
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(final Double val) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                double total = component.getModel().getTotal();
                if (total == 0) {
                    return;
                } 
                component.getModel().getDataset().setValue(index, val / total);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public String getKey() {
        return "PieData-"+index;
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
    public PieChartComponent getParentComponent() {
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

