package org.simbrain.plot;

import java.lang.reflect.Type;

import org.simbrain.workspace.SingleAttributeConsumer;

/**
 * Wraps a column of the table with a consumer object, so other components
 * can write numerical data to a data world column.
 */
public class Variable extends SingleAttributeConsumer<Double> {

    /** Reference to gauge. */
    private PlotComponent plot;

    /**
     * @param columnNumber
     */
    public Variable(final PlotComponent plot) {
        this.plot = plot;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Double value) {
        System.out.println("set value");
        plot.setValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "Dimension ";
    }
    
    public Type getType() {
        return Double.TYPE;
    }

    public PlotComponent getParentComponent() {
        return plot;
    }

    public String getAttributeDescription() {
        return "Variable";
    }
}
