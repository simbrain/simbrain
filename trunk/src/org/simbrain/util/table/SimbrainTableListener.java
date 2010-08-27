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
package org.simbrain.util.table;

/**
 * Listener interface for receiving table structure and data change events.
 */
public interface SimbrainTableListener {

    /**
     * A column was added.
     *
     * @param column index of new column
     */
    void columnAdded(int column);

    /**
     * A column was removed.
     *
     * @param column index of removed column
     */
    void columnRemoved(int column);

    /**
     * A row was added.
     *
     * @param row index of added row.
     */
    void rowAdded(int row);

    /**
     * A row was removed.
     *
     * @param row index of removed row
     */
    void rowRemoved(int row);

    /**
     * A cell was changed.
     *
     * @param row row index
     * @param column column index
     */
    void itemChanged(int row, int column);

    /**
     * The table structure changed.
     */
    void structureChanged();

    /**
     * The table data changed.
     */
    void dataChanged();

}