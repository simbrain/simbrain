/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.dataworld;

import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.SingleAttributeProducer;

/**
 * Wraps a column of the table with a producer object, so other components
 * can write read data from a data world column.
 *
 * @param <E> The type this producer produces.
 */
public class ProducingColumn<E> extends SingleAttributeProducer<E> {

    /** The number of the column being represented. */
    private int columnNumber;

    /** Reference to table model. */
    private DataWorldComponent parent;

    /**
     * Construct producing column.
     *
     * @param dataWorldComponent reference to parent table
     * @param columnNumber the column number to set
     */
    public ProducingColumn(final DataWorldComponent parent, final int columnNumber) {
        this.parent = parent;
        this.columnNumber = columnNumber;
    }

    /**
     * {@inheritDoc}
     */
    public String getKey() {
        return String.valueOf(columnNumber + 1);
    }

    /**
     * From consuming attribute.  Should not be used.
     *
     * @return The name of the attribute.
     */
    public String getAttributeDescription() {
        return getDescription();
    }

    /**
     * {@inheritDoc}
     */
    public E getValue() {
        return (E) parent.getDataModel().getValueCurrentRow(columnNumber);
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "Column" + getKey();
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultProducingAttribute(final ProducingAttribute<?> consumingAttribute) {
        // TODO
    }

    /**
     * {@inheritDoc}
     */
    public Producer getParent() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DataWorldComponent getParentComponent() {
        return parent;
    }
}
