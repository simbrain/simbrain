package org.simbrain.world.dataworld;

import java.lang.reflect.Type;

import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.SingleAttributeProducer;

/**
 * Wraps a column of the table with a producer object, so other components 
 * can write read data from a data world column.
 */
public class ProducingColumn<E> extends SingleAttributeProducer<E> {

    /** The number of the column being represented. */
    private int columnNumber;

    /** Reference to table model. */
    private DataModel<E> tableModel;

    /**
     * Construct producing column.
     *
     * @param table reference to parent table
     * @param columnNumber the column number to set
     */
    public ProducingColumn(final DataModel<E> table, final int columnNumber) {
        this.tableModel = table;
        this.columnNumber = columnNumber;
    }

    /**
     * From consuming attribute.  Should not be used.
     */
    public String getAttributeDescription() {
        return "Column " + columnNumber;
    }

    /**
     * {@inheritDoc}
     */
    public E getValue() {
        return tableModel.get(columnNumber);
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
    public void setDefaultProducingAttribute(ProducingAttribute<?> consumingAttribute) {
        // TODO
    }

    /**
     * {@inheritDoc}
     */
    public Producer getParent() {
        return this;
    }
    
    public Type getType() {
        return Double.TYPE;
    }

    public DataWorldComponent getParentComponent() {
        // TODO Auto-generated method stub
        return null;
    }
}
