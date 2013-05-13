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
package org.simbrain.network.gui.trainer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;

/**
 * Panel to test a Backprop network by allowing user to cycle
 * through a set of inputs.
 *
 * @author jyoshimi
 * @author Lam Nguyen
 */
public class TestNetworkPanel extends JPanel {

    /** JTable contained in scroller. */
    private SimbrainJTable table;

    /** Scroll panel for table. */
    private SimbrainJTableScrollPanel scroller;

    /** The Backprop network to test. */
    private BackpropNetwork network;

    /**
     * Construct a test network panel using a specified Backprop network.
     *
     * @param network the Backprop network to test
     */
    public TestNetworkPanel(final BackpropNetwork network) {
        this.network = network;
        table = new SimbrainJTable(new NumericTable(5, network
                .getInputNeurons().size()));
        ((NumericTable) table.getData()).setIterationMode(true);
        // Set up column headings
        List<String> colHeaders = new ArrayList<String>();
        for (int i = 0; i < network.getInputNeurons().size(); i++) {
            colHeaders.add(new String("" + (i + 1) + " ("
                    + network.getInputNeurons().get(i).getId())
                    + ")");
        }
        table.setColumnHeadings(colHeaders);
        table.getData().fireTableStructureChanged();
        scroller = new SimbrainJTableScrollPanel(table);
        setLayout(new BorderLayout());
        add("Center", scroller);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JToolBar editRowToolBar = new JToolBar();
        toolbar.add(table.getToolbarCSV());
        toolbar.add(editRowToolBar);
        editRowToolBar.add(table.getToolbarEditRows());
        toolbar.add(table.getToolbarRandomize());
        JButton test = new JButton(testRowAction);
        JButton advance = new JButton(advanceRowAction);
        JToolBar testToolBar = new JToolBar();
        testToolBar.add(test);
        testToolBar.add(advance);
        toolbar.add(testToolBar);
        add("North", toolbar);
    }

    /**
     * Action for advancing a row to be tested.
     */
    private Action advanceRowAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("plus.png"));
            putValue(SHORT_DESCRIPTION, "Advance row");
        }
        /**
         * {@ineritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            advanceRow();
        }
    };

    /**
     * Action to test a row.
     */
    private Action testRowAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Step.png"));
            putValue(SHORT_DESCRIPTION, "Test network");
        }
        /**
         * {@ineritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            testRow();
        }
    };

    /**
     * Advances the row to test.
     */
    private void advanceRow() {
        ((NumericTable) table.getData()).updateCurrentRow();
        table.updateRowSelection();
    }

    /**
     * Test the selected row.
     */
    private void testRow() {
        int testRow = ((NumericTable) table.getData()).getCurrentRow();
        if (testRow >=  ((NumericTable) table.getData()).getRowCount()) {
            testRow = 0;
        }
        table.updateRowSelection();
        for (int j = 0; j < network.getInputNeurons().size(); j++) {
            network.getInputNeurons().get(j)
            .setInputValue(((NumericTable) table.getData()).
                    getValue(testRow, j));
        }
        network.update();
        network.getParentNetwork().fireNetworkChanged();
    }

    /**
     * @return the table
     */
    public SimbrainJTable getTable() {
        return table;
    }

}
