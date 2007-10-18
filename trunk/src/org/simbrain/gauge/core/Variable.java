package org.simbrain.gauge.core;

import java.lang.reflect.Type;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.SingleAttributeConsumer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Wraps a column of the table with a consumer object, so other components
 * can write numerical data to a data world column.
 */
public class Variable extends SingleAttributeConsumer<Double> {

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
    public String getAttributeDescription() {
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
//    public void setDefaultConsumingAttribute(final ConsumingAttribute consumingAttribute) {
//    }

    /**
     * {@inheritDoc}
     */
    public Consumer getParent() {
        return this;
    }

    public Type getType() {
        return Double.TYPE;
    }

    public String getDescription() {
        return "Dimension " + dimension;
    }

    public WorkspaceComponent getParentComponent() {
        // TODO Auto-generated method stub
        return null;
    }
}
