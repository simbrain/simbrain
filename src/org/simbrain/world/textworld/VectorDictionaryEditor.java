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
package org.simbrain.world.textworld;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;
import org.simbrain.util.table.TextTable;

/**
 * Dialog for editing vector dictionary (mapping from strings to double arrays).
 *
 * @author jeffyoshimi
 */
public class VectorDictionaryEditor extends StandardDialog {

    /** The world to edit. */
    private final ReaderWorld world;

    /** The jtable. */
    private final SimbrainJTable table;

    /**
     * Factory method to create editor.
     *
     * @param world the world whose dictionary is being edited.
     * @return the constructed editor
     */
    public static VectorDictionaryEditor createVectorDictionaryEditor(
            final ReaderWorld world) {
        VectorDictionaryEditor editor = new VectorDictionaryEditor(world);
        return editor;
    }

    /**
     * Construct the editor.
     *
     * @param world the world whose dictionary is being edited.
     */
    private VectorDictionaryEditor(final ReaderWorld world) {
        super();
        this.world = world;
        setTitle("Vector Dictionary");
        JPanel mainPanel = new JPanel(new BorderLayout());
        table = SimbrainJTable.createTable(new VectorDictionaryTable(
                world.getVectorDictionary()));
        table.setShowCSVInPopupMenu(true);
        table.setShowDeleteColumnPopupMenu(false);
        table.setShowInsertColumnPopupMenu(false);
        table.setShowEditInPopupMenu(false);
        table.setDisplayColumnHeadings(true);
        table.setColumnHeadings(Arrays.asList("Token", "Vector"));
        SimbrainJTableScrollPanel scroller = new SimbrainJTableScrollPanel(
                table);
        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        toolbarPanel.add(table.getToolbarCSV(true, false));
        toolbarPanel.add(table.getToolbarEditRows());
        mainPanel.add(toolbarPanel, BorderLayout.NORTH);
        table.setDisplayColumnHeadings(false);
        mainPanel.add(scroller, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        world.resetVectorDictionary(table.getData().asStringArray());
    }

    /**
     * Mutable text table customized for display of maps from strings to
     * double-arrays.
     */
    private class VectorDictionaryTable extends TextTable {

        /**
         * Construct table that associates tokens with vector strings.
         */
        private VectorDictionaryTable(final HashMap<String, double[]> dictionary) {
            super();
            init(dictionary.size(), 2);
            int i = 0;
            for (Map.Entry<String, double[]> entry : dictionary.entrySet()) {
                String token = entry.getKey();
                double[] vals = entry.getValue();
                setLogicalValue(i, 0, token, false);
                setLogicalValue(i, 1, Utils.doubleArrayToString(vals), false);
                i++;
            }
            fireTableDataChanged();
        }
    }

}
