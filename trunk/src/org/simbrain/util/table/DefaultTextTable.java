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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A simple text table.
 *
 * @author jyoshimi
 */
public class DefaultTextTable  extends SimbrainDataTable<String> {

    /** The data. */
    private List<List<String>> rowData = new ArrayList<List<String>>();

    //TODO: Add more constructors

    /**
     * Construct from list of strings.
     */
    public DefaultTextTable(Set<String> dictionary) {
        init(dictionary.size(), 1);
        int i = 0;
        for (String string : dictionary) {
            setValue(i, 0, string);
            i++;
        }
    }

    /**
     * Initialize the table the table.
     *
     * @param rows num rows
     * @param cols num cols
     */
    private void init(int rows, int cols) {
        rowData.clear();
        for (int i = 0; i < rows; i++) {
            rowData.add((List<String>) getNewRow(" ", cols));
        }

        fireTableStructureChanged();
    }

    /**
     * Create a new row for the table, with a specified value.
     *
     * @param value value for columns of new row
     * @return the new row
     */
    private List<String> getNewRow(final String value, int cols) {
        ArrayList<String> row = new ArrayList<String>();

        for (int i = 0; i < cols; i++) {
            row.add(value);
        }
        return row;
    }

    @Override
    public void setValue(int row, int col, String value) {
        rowData.get(row).set(col, value);
        fireTableDataChanged();
    }

    @Override
    public String getValue(int row, int col) {
        return rowData.get(row).get(col);
    }

    @Override
    public int getColumnCount() {
        if (rowData.size() > 0) {
            return rowData.get(0).size();
        } else {
            return 0;
        }
    }

    @Override
    public int getRowCount() {
        return rowData.size();
    }

}
