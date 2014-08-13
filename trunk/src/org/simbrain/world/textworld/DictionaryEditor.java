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
import java.util.Iterator;
import java.util.Set;

import javax.swing.JPanel;

import org.simbrain.util.StandardDialog;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;
import org.simbrain.util.table.TextTable;

/**
 * Dialog for showing and editing a basic dictionary (for display world); a list
 * of text items that can be coupled to and then displayed in the interface.
 * 
 * @author Jeff Yoshimi
 * 
 */
public class DictionaryEditor extends StandardDialog {

    /** The world to edit. */
    private final DisplayWorld world;

    /** The jtable. */
    private final SimbrainJTable table;

    /**
     * Create the dictionary editor.
     *
     * @param world the display world whose dictionary to display and edit
     * @return the constructed dictionary editor
     */
    public static DictionaryEditor createDictionaryEditor(DisplayWorld world) {
        DictionaryEditor editor = new DictionaryEditor(world);
        return editor;
    }

    /**
     * Construct the editor.
     *
     * @param world the world whose dictionary is being edited.
     */
    private DictionaryEditor(final DisplayWorld world) {
        super();
        this.world = world;
        setTitle("Dictionary");
        JPanel mainPanel = new JPanel(new BorderLayout());
        table = SimbrainJTable.createTable(new DictionaryTable(
                world.getDictionary()));
        table.setShowCSVInPopupMenu(true);
        table.setShowDeleteColumnPopupMenu(false);
        table.setShowInsertColumnPopupMenu(false);
        table.setShowEditInPopupMenu(false);
        table.setDisplayColumnHeadings(true);
        table.setColumnHeadings(Arrays.asList("Entry"));
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
        world.resetDictionary(table.getData().asStringArray());
    }

    /**
     * Mutable text table customized for display of lists of strings.
     */
    private class DictionaryTable extends TextTable {

        /**
         * Construct the table.
         */
        private DictionaryTable(final Set<String> set) {
            super();
            init(set.size(), 1);
            int i = 0;
            for (Iterator<String> iterator = set.iterator(); iterator.hasNext();) {
                String token = (String) iterator.next();
                setLogicalValue(i, 0, token, false);
                i++;
            }
            fireTableDataChanged();
        }
    }
}
