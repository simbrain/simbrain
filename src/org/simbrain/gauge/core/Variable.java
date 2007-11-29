package org.simbrain.gauge.core;

import java.lang.reflect.Type;

import org.simbrain.gauge.GaugeComponent;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.SingleAttributeConsumer;

/**
 * Wraps a column of the table with a consumer object, so other components
 * can write numerical data to a data world column.
 */
public class Variable extends SingleAttributeConsumer<Double> {

    /** The number of the dimension being represented. */
    private int dimension;

    /** Reference to gauge. */
    private Gauge gauge;

    /** Reference to parent component */
    private final GaugeComponent parent;

    /**
     * @param columnNumber
     */
    public Variable(final Gauge gauge, final GaugeComponent parent, final int dimension) {
        this.dimension = dimension;
        this.parent = parent;
        this.gauge = gauge;
    }

    /**
     * From consuming attribute.
     */
    public String getAttributeDescription() {
        return "Dimension " + dimension;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(final Double value) {
        gauge.setValue(dimension, value);
    }

    public Type getType() {
        return Double.TYPE;
    }

    public String getDescription() {
        return "Dimension " + dimension;
    }

    public GaugeComponent getParentComponent() {
        return parent;
    }
}
