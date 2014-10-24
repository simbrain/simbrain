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
package org.simbrain.world.textworld.dictionary;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;

/**
 * Superclass for VectorToTokenPanel and TokenToVectorPanel.
 *
 * Todo: Note that both vectorToTokenPanel and TokenToVectorPanel use a table
 * that encodes vectors in a somewhat unusual fashion, as strings, rather than
 * individual cells. This is because SimbrainJTable does not (as of 9/14)
 * support multiple data types. Ultimately should have a text column followed by
 * a numerical matrix editor.
 *
 * @author Jeff Yoshimi
 */
public class VectorDictionaryPanel extends JPanel {

    /** Description of the vector dictionary displayed in the panel. */
    private final String infoText;

    /**
     * Construct the panel.
     *
     * @param text description of what the panel does, displayed within it
     */
    public VectorDictionaryPanel(final String text) {
        super();
        this.infoText = text;
    }

    /**
     * Build and initialize the panel.
     *
     * @param table the Simbrain table with the data
     */
    protected void initPanel(final SimbrainJTable table) {

        table.setShowCSVInPopupMenu(true);
        table.setShowDeleteColumnPopupMenu(false);
        table.setShowInsertColumnPopupMenu(false);
        table.setShowEditInPopupMenu(false);
        table.setColumnHeadings(Arrays.asList("Token", "Vector"));
        SimbrainJTableScrollPanel vectorScroller = new SimbrainJTableScrollPanel(
                table);
        vectorScroller.setPreferredSize(new Dimension(300, 200));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setMaxWidth(70);
        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        toolbarPanel.add(table.getToolbarCSV(true, false));
        toolbarPanel.add(table.getToolbarEditRows());
        Box vectorBox = Box.createHorizontalBox();
        vectorBox.add(vectorScroller);
        vectorBox.add(Box.createRigidArea(new Dimension(15, 0)));
        JLabel vectorInfo = new JLabel(infoText);
        vectorInfo.setPreferredSize(new Dimension(200, 200));
        vectorBox.add(vectorInfo);

        this.setLayout(new BorderLayout());
        add(toolbarPanel, BorderLayout.NORTH);
        add(vectorBox, BorderLayout.CENTER);
    }

}
