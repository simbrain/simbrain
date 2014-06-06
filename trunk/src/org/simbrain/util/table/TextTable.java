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
import java.util.HashMap;
import java.util.Map;
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
     */
    public TextTable(final int rows, final int cols) {
        init(rows, cols);
        for (int i = 0; i < rows; i++) {
            rowData.add(getNewRow(""));
        }
    }

    /**
     * Construct table from a set of strings.
     */
    public TextTable(final Set<String> dictionary) {
        init(dictionary.size(), 1);
        int i = 0;
        for (String string : dictionary) {
            setValue(i, 0, string, false);
            i++;
        }
        fireTableDataChanged();
    }

    /**
     * Construct table that associates tokens with vector strings.
     */
    public TextTable(final HashMap<String, double[]> dictionary) {
        //TODO: This should happen outside of this class.
        init(dictionary.size(), 2);
        int i = 0;
        for (Map.Entry<String, double[]> entry : dictionary.entrySet()) {
            String token = entry.getKey();
            double[] vals = entry.getValue();
            setValue(i, 0, token, false);
            setValue(i, 1, Utils.doubleArrayToString(vals), false);
            i++;
        }
        fireTableDataChanged();
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
            rowData.add(getNewRow(" ", cols));
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

    /**
     * Load a .csv file.
     *
     * @param file the CSV file
     * @param allowRowChanges whether to allow data with a different number of
     *            rows
     * @param allowColumnChanges whether to allow data with a different number
     *            of columns
     * @throws Exception
     */
    public void readData(final File file, final boolean allowRowChanges,
            final boolean allowColumnChanges) throws TableDataException {
        String[][] values = Utils.getStringMatrix(file);
        // TODO: Code below duplicates part of NumericTable.readData
        if (!allowRowChanges && values.length != getRowCount()) {
            throw new TableDataException("Trying to import data with "
                    + values.length + " rows into a table with "
                    + getRowCount() + " rows.");
        } else if (!allowColumnChanges && values[0].length != getColumnCount()) {
            throw new TableDataException("Trying to import data with "
                    + values[0].length + " columns into a table with "
                    + getColumnCount() + " columns.");
        } else {
            reset(values.length, values[0].length);
            for (int i = 0; i < values.length; i++) {
                for (int j = 0; j < values[0].length; j++) {
                    if ((values[i][j]).length() > 0) {
                        setValue(i, j, values[i][j], false);
                    }
                }
            }
            fireTableStructureChanged();
        }
    }

}
