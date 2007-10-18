package org.simbrain.world.dataworld;

import java.lang.reflect.Type;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.SingleAttributeConsumer;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Wraps a column of the table with a consumer object, so other components 
 * can write numerical data to a data world column.
 */
public class ConsumingColumn extends SingleAttributeConsumer<Double> {

    /** The number of the column being represented. */
    private int columnNumber;

    /** Reference to table model. */
    private TableModel tableModel;

    /**
     * @param columnNumber
     */
    public ConsumingColumn(final TableModel table, final int columnNumber) {
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
        tableModel.setValueAt(columnNumber, value);
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "Column " + columnNumber;
    }

    /**
     * {@inheritDoc}
     */
//    public void setDefaultConsumingAttribute(ConsumingAttribute<?> consumingAttribute) {
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

    public WorkspaceComponent getParentComponent() {
        // TODO Auto-generated method stub
        return null;
    }
}
