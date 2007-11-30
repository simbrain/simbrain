package org.simbrain.world.dataworld;

import java.lang.reflect.Type;

import org.simbrain.workspace.SingleAttributeConsumer;

/**
 * Wraps a column of the table with a consumer object, so other components 
 * can write numerical data to a data world column.
 */
public class ConsumingColumn<E> extends SingleAttributeConsumer<E> {

    /** The number of the column being represented. */
    private final int columnNumber;

    /** Reference to table model. */
    private DataModel<E> tableModel;

    /**
     * @param columnNumber
     */
    public ConsumingColumn(final DataModel<E> table, final int columnNumber) {
        this.tableModel = table;
        this.columnNumber = columnNumber;
    }

    /**
     * From consuming attribute.
     */
    public String getAttributeDescription() {
        return "Column " + (columnNumber + 1);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(E value) {
        tableModel.set(columnNumber, value);
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "Column " + (columnNumber + 1);
    }

    public Type getType() {
        return Double.TYPE;
    }

    public DataWorldComponent getParentComponent() {
        // TODO Auto-generated method stub
        return null;
    }
}
