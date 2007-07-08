package org.simbrain.world.dataworld;

import java.util.List;

import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;

/**
 * Wraps a column of the table with a producer object, so other components 
 * can write read data from a data world column.
 */
public class ProducingColumn implements Producer, ProducingAttribute<Double> {

    /** The number of the column being represented. */
    private int columnNumber;

    /** Reference to table model. */
    private TableModel tableModel;

    /**
     * @param columnNumber
     */
    public ProducingColumn(final TableModel table, final int columnNumber) {
        this.tableModel = table;
        this.columnNumber = columnNumber;
    }

    /**
     * From consuming attribute.  Should not be used.
     */
    public String getName() {
        return "Column " + columnNumber;
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue() {
        return (Double) tableModel.getValueAt(columnNumber);
    }

    /**
     * {@inheritDoc}
     */
    public String getProducerDescription() {
        return "Column " + columnNumber;
    }

    /**
     * {@inheritDoc}
     */
    public List<ProducingAttribute> getProducingAttributes() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ProducingAttribute getDefaultProducingAttribute() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultProducingAttribute(ProducingAttribute consumingAttribute) {
    }

    /**
     * {@inheritDoc}
     */
    public Producer getParent() {
        return this;
    }
}
