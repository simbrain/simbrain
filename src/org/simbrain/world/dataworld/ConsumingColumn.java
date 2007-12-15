package org.simbrain.world.dataworld;

import java.lang.reflect.Type;

import org.simbrain.workspace.SingleAttributeConsumer;

/**
 * Wraps a column of the table with a consumer object, so other components
 * can write numerical data to a data world column.
 * 
 * @param <E> The type that this column handles.
 */
public class ConsumingColumn<E> extends SingleAttributeConsumer<E> {

    /** The number of the column being represented. */
    private final int columnNumber;

    /** Reference to table model. */
    private DataModel<E> tableModel;

    /**
     * Creates a new instance.
     * 
     * @param table The table this column is a member of.
     * @param columnNumber The number of this column.
     */
    public ConsumingColumn(final DataModel<E> table, final int columnNumber) {
        this.tableModel = table;
        this.columnNumber = columnNumber;
    }

    /**
     * {@inheritDoc}
     */
    public String getAttributeDescription() {
        return "Column " + (columnNumber + 1);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(final E value) {
        tableModel.set(columnNumber, value);
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "Column " + (columnNumber + 1);
    }

    /**
     * {@inheritDoc}
     */
    public Type getType() {
        return Double.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    public DataWorldComponent getParentComponent() {
        return tableModel.getParent();
    }
}
