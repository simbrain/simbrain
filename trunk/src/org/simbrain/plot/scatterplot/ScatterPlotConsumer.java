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
package org.simbrain.plot.scatterplot;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;

/**
 * Represents scatter plot data.  Has two attributes, x and y.
 */
public class ScatterPlotConsumer implements Consumer {

    /** Reference to gauge. */
    private ScatterPlotComponent plot;
        
    /** Name. */
    private final String name;
    
    /** Index. */
    private Integer index;

    /**  X and Y Attributes for this consumer. */
    private ArrayList<ConsumingAttribute<Double>> attributeList = new ArrayList<ConsumingAttribute<Double>>();
    
    /** The X Attribute. */
    private XAttribute xAttribute = new XAttribute();

    /** The X Attribute. */
    private YAttribute yAttribute = new YAttribute();


    /**
     * Construct a ScatterPlotConsumer.
     * 
     * @param plot the parent component
     * @param name the name of this consumer (displayed in the plot)
     */
    public ScatterPlotConsumer(final ScatterPlotComponent plot, final String name, final Integer index) {
        this.plot = plot;
        this.name = name;
        this.index = index;
        attributeList.add(xAttribute);
        attributeList.add(yAttribute);
    }

    /** 
     * Return value for x attribute.
     * 
     * @return value for x attribute.
     *
     */
    public Double getX() {
        return xAttribute.getValue();
    }

    /** 
     * Return value for y attribute.
     * 
     * @return value for y attribute.
     *
     */
    public Double getY() {
        return yAttribute.getValue();
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return name;
    }
    
    /**
     * {@inheritDoc}
     */
    public ScatterPlotComponent getParentComponent() {
        return plot;
    }

    /**
     * {@inheritDoc}
     */
    public String getAttributeDescription() {
        return getDescription();
    }
    
    /**
     * Return index.
     *
     * @return index
     */
    public Integer getIndex() {
            return index;
    }

    /**
     * {@inheritDoc}
     */
    public List<? extends ConsumingAttribute<?>> getConsumingAttributes() {
        return attributeList;
    }

    /**
     * {@inheritDoc}
     */
    public ConsumingAttribute<Double> getDefaultConsumingAttribute() {
        return attributeList.get(0);
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultConsumingAttribute(ConsumingAttribute<?> consumingAttribute) {
    }
    
    /**
     * Scatter plot attribute.
     */
    private abstract class ScatterPlotAttribute extends AbstractAttribute implements ConsumingAttribute<Double> {

        /** Value. */
        private Double value = new Double(0);

        /**
         * {@inheritDoc}
         */
        public Consumer getParent() {
            return ScatterPlotConsumer.this;
        }

        /**
         * {@inheritDoc}
         */
        public Double getValue() {
                return value;
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double val) {
            value = val;
        }

        /**
         * {@inheritDoc}
         */
        public Type getType() {
            return Double.TYPE;
        }
    }
    
    /**
     * X Attribute.
     */
    private class XAttribute extends ScatterPlotAttribute {
        public String getAttributeDescription() {
            return "X";
        }
    }

    /**
     * Y Attribute.
     */
    private class YAttribute extends ScatterPlotAttribute {
        public String getAttributeDescription() {
            return "Y";
        }
    }
    
}

