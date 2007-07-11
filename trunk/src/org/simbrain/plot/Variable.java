package org.simbrain.plot;

import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;

/**
 * Wraps a column of the table with a consumer object, so other components
 * can write numerical data to a data world column.
 */
public class Variable implements Consumer, ConsumingAttribute<Double> {

    /** Reference to gauge. */
    private PlotComponent gauge;

    /**
     * @param columnNumber
     */
    public Variable(final PlotComponent gauge) {
        this.gauge = gauge;
    }

    /**
     * From consuming attribute.
     */
    public String getName() {
        return "Variable";
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
    public String getConsumerDescription() {
        return "Dimension ";
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
