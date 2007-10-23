package org.simbrain.plot;

import java.lang.reflect.Type;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.SingleAttributeConsumer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Wraps a column of the table with a consumer object, so other components
 * can write numerical data to a data world column.
 */
public class Variable extends SingleAttributeConsumer<Double> {

    /** Reference to gauge. */
    private PlotComponent gauge;

    /**
     * @param columnNumber
     */
    public Variable(final PlotComponent gauge) {
        this.gauge = gauge;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Double value) {
        gauge.setValue(value);
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

    public WorkspaceComponent getParentComponent() {
        return gauge;
    }

    public String getAttributeDescription() {
        return "Variable";
    }
}
