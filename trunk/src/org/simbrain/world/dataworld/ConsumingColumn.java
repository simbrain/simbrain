package org.simbrain.world.dataworld;

import java.lang.reflect.Type;

import org.simbrain.workspace.SingleAttributeConsumer;

/**
 * Wraps a column of the table with a consumer object, so other components 
 * can write numerical data to a data world column.
 */
public class ConsumingColumn extends SingleAttributeConsumer<Double> {

    /** The number of the column being represented. */
    private final int columnNumber;

    /** Reference to table model. */
    private DataModel<Double> tableModel;

    /**
     * @param columnNumber
     */
    public ConsumingColumn(final DataModel<Double> table, final int columnNumber) {
        this.tableModel = table;
        this.columnNumber = columnNumber;
    }

    /**
     * From consuming attribute.
     */
    public String getAttributeDescription() {
        return "Column " + columnNumber;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Double value) {
        tableModel.set(columnNumber, value);
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "Column " + columnNumber;
    }
    
    public Type getType() {
        return Double.TYPE;
    }

    public DataWorldComponent getParentComponent() {
        // TODO Auto-generated method stub
        return null;
    }
}
