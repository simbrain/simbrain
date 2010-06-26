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

    /** Reference to parent. */
    private DataWorldComponent parent;

    /**
     * Creates a new instance.
     *
     * @param table The table this column is a member of.
     * @param columnNumber The number of this column.
     */
    public ConsumingColumn(final DataWorldComponent parent, final int columnNumber) {
        this.parent = parent;
        this.columnNumber = columnNumber;
    }

    /**
     * {@inheritDoc}
     */
    public String getAttributeDescription() {
        return "Column" + (columnNumber + 1);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(final E value) {
        // TODO: The cast to double is there to make the compiler happy; not
        // sure why it was needed.
        parent.getDataModel().setValueCurrentRow(columnNumber, (Double) value);
    }

    /**
     * {@inheritDoc}
     */
    public String getKey() {
        return String.valueOf(columnNumber + 1);
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "Column " + getKey();
    }

    /**
     * {@inheritDoc}
     */
    public DataWorldComponent getParentComponent() {
        return parent;
    }
}
