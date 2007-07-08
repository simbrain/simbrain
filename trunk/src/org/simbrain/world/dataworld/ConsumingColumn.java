package org.simbrain.world.dataworld;

import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;

/**
 * Wraps a column of the table with a consumer object, so other components 
 * can write numerical data to a data world column.
 */
public class ConsumingColumn implements Consumer, ConsumingAttribute<Double> {

    /** The number of the column being represented. */
    private int columnNumber;

    /** Current value of this consumer.  To be put in to the table. */
    private double currentValue;

    /**
     * @param columnNumber
     */
    public ConsumingColumn(final int columnNumber) {
        this.columnNumber = columnNumber;
    }

    /**
     * From consuming attribute.
     */
    public String getName() {
        return "Column " + columnNumber;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Double value) {
        currentValue = value;
    }

    /**
     * {@inheritDoc}
     */
    public String getConsumerDescription() {
        return "Column " + columnNumber;
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
    public void setDefaultConsumingAttribute(ConsumingAttribute consumingAttribute) {
    }

    /**
     * {@inheritDoc}
     */
    public Consumer getParent() {
        return this;
    }
}
