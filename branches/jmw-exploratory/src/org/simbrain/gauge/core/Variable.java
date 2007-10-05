package org.simbrain.gauge.core;

import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;

/**
 * Wraps a column of the table with a consumer object, so other components
 * can write numerical data to a data world column.
 */
public class Variable implements Consumer, ConsumingAttribute<Double> {

    /** The number of the dimension being represented. */
    private int dimension;

    /** Reference to gauge. */
    private Gauge gauge;

    /**
     * @param columnNumber
     */
    public Variable(final Gauge gauge, final int dimension) {
        this.dimension = dimension;
        this.gauge = gauge;
    }

    /**
     * From consuming attribute.
     */
    public String getName() {
        return "Dimension " + dimension;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(final Double value) {
        gauge.setValue(dimension, value);
    }

    /**
     * {@inheritDoc}
     */
    public String getConsumerDescription() {
        return "Dimension " + dimension;
    }

    /**
     * {@inheritDoc}
     */
    public List<ConsumingAttribute> getConsumingAttributes() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ConsumingAttribute getDefaultConsumingAttribute() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultConsumingAttribute(final ConsumingAttribute consumingAttribute) {
    }

    /**
     * {@inheritDoc}
     */
    public Consumer getParent() {
        return this;
    }
}
