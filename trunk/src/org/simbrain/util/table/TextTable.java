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

import java.io.File;
import java.util.Set;

import org.simbrain.util.Utils;

/**
 * A  mutable text table.
 *
 * @author jyoshimi
 */
public class TextTable extends MutableTable<String> {

    /**
     * Construct a text table.
     * @param rows
     * @param cols
     */
    public TextTable(final int rows, final int cols) {
        init(rows, cols);
        for (int i = 0; i < rows; i++) {
            rowData.add(createNewRow(""));
        }
    }

    /**
     * Construct table from a set of strings.
     * @param dictionary
     */
    public TextTable(final Set<String> dictionary) {
        init(dictionary.size(), 1);
        int i = 0;
        for (String string : dictionary) {
            setLogicalValue(i, 0, string, false);
            i++;
        }
        fireTableDataChanged();
    }

    /**
     * No arg constructor.
     */
    public TextTable() {
    }

    /**
     * Initialize the table.
     *
     * @param rows num rows
     * @param cols num cols
     */
    protected void init(int rows, int cols) {
        rowData.clear();
        for (int i = 0; i < rows; i++) {
            rowData.add(createNewRow(" ", cols));
        }
        fireTableStructureChanged();
    }

    @Override
    String getDefaultValue() {
        return "";
    }

    @Override
    public Class getDataType() {
        return String.class;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Double.class;
        } else {
            return String.class;
        }
    }

    /**
     * Load a .csv file.
     *
     * @param file the CSV file
     * @param allowRowChanges whether to allow data with a different number of
     *            rows
     * @param allowColumnChanges whether to allow data with a different number
     *            of columns
     * @exception TableDataException
     */
    public void readData(final File file, final boolean allowRowChanges,
            final boolean allowColumnChanges) throws TableDataException {
        String[][] values = Utils.getStringMatrix(file);
        try {
            checkData(allowRowChanges, allowColumnChanges, values);
            reset(values.length, values[0].length);
            for (int i = 0; i < values.length; i++) {
                for (int j = 0; j < values[0].length; j++) {
                    if ((values[i][j]).length() > 0) {
                        setLogicalValue(i, j, values[i][j], false);
                    }
                }
            }
            fireTableStructureChanged();
        } catch (TableDataException tde) {
            throw tde;
        }
    }

}
