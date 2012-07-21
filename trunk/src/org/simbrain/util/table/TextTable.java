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
 * A default mutable text table.
 *
 * @author jyoshimi
 */
public class TextTable extends MutableTable<String> {

    /**
     * Construct A text table.
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
            setValue(i, 0, string);
            i++;
        }
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


}
