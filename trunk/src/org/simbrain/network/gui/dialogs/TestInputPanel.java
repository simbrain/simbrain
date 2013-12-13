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
package org.simbrain.network.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;

/**
 * Panel for sending inputs from a table to a network. The action that calls
 * this class provides the input neurons and network panel from which the action
 * gets the network to be updated.
 *
 * @author Jeff Yoshimi
 * @author Lam Nguyen
 */
public class TestInputPanel extends JPanel {

    /** Network panel. */
    private NetworkPanel networkPanel;

    /** JTable contained in scroller. */
    private SimbrainJTable table;

    /** Scroll panel for table. */
    private SimbrainJTableScrollPanel scroller;

    /** True when iteration mode is on. */
    private boolean iterationMode = true;

    /** Button used to advance row. Disabled when iteration mode is on. */
    private JButton advance;

    /** The training input data. */
    private double[][] data;

    /**
     * This is the network that should be updated whenever the input neurons are
     * updated. If null, update the whole network
     */
    private Network network;

    /** The nodes to test. */
    private List<Neuron> inputNeurons;

    /**
     * Construct panel using a network panel and a list of selected neurons.
     *
     * @param networkPanel networkPanel, must not be null
     * @param inputNeurons input neurons of the network to be tested
     */
    public TestInputPanel(NetworkPanel networkPanel, List<Neuron> inputNeurons) {
        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        this.inputNeurons = inputNeurons;
        initTestInputPanel();
    }

    /**
     * Construct panel using data as default input data.
     *
     * @param networkPanel networkPanel, must not be null
     * @param inputNeurons input neurons of the network to be tested
     * @param data input data to test
     */

    public TestInputPanel(NetworkPanel networkPanel, List<Neuron> inputNeurons,
            double[][] data) {
        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        this.inputNeurons = inputNeurons;
        this.data = data;
        initTestInputPanel();
    }

    NumericTable numericTable;

    /**
     * Initiate the test network panel using the network panel.
     */
    private void initTestInputPanel() {
        network = networkPanel.getNetwork();
        numericTable = new NumericTable(5, inputNeurons.size());
        if (data != null) {
            numericTable.setData(data);
        }
        table = new SimbrainJTable(numericTable);
        ((NumericTable) table.getData()).setIterationMode(iterationMode);
        // Set up column headings
        List<String> colHeaders = new ArrayList<String>();
        for (int i = 0; i < inputNeurons.size(); i++) {
            colHeaders.add(new String("" + (i + 1) + " ("
                    + inputNeurons.get(i).getId())
                    + ")");
        }
        table.setColumnHeadings(colHeaders);
        table.getData().fireTableStructureChanged();
        scroller = new SimbrainJTableScrollPanel(table);
        this.setLayout(new BorderLayout());
        this.add("Center", scroller);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JToolBar editRowToolBar = new JToolBar();
        toolbar.add(table.getToolbarCSV(true, false));
        toolbar.add(editRowToolBar);
        editRowToolBar.add(table.getToolbarEditRows());
        toolbar.add(table.getToolbarRandomize());
        JButton test = new JButton(testRowAction);
        advance = new JButton(advanceRowAction);
        JButton testTable = new JButton(testTableAction);
        JCheckBox iterationCheckBox = new JCheckBox(iterationModeAction);
        iterationCheckBox.setSelected(iterationMode);
        JToolBar testToolBar = new JToolBar();
        testToolBar.add(test);
        testToolBar.add(advance);
        testToolBar.add(testTable);
        testToolBar.add(iterationCheckBox);
        toolbar.add(testToolBar);
        this.add("North", toolbar);
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
            putValue(SHORT_DESCRIPTION, "Test row");
        }

        /**
         * {@ineritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            testRow();
        }
    };

    /**
     * Action to test a row.
     */
    private Action iterationModeAction = new AbstractAction() {
        {
            putValue(NAME, "Iteration mode");
        }

        /**
         * {@ineritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            if (iterationMode) {
                iterationMode = false;
                advance.setEnabled(true);
            } else {
                iterationMode = true;
                advance.setEnabled(false);
            }
        }
    };

    /**
     * Action to test the entire table.
     */
    private Action testTableAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Play.png"));
            putValue(SHORT_DESCRIPTION, "Test table");
        }

        /**
         * {@ineritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            testTable();
        }
    };

    /**
     * Advances the row to test.
     */
    private void advanceRow() {
        ((NumericTable) table.getData()).updateCurrentRow();
        table.updateRowSelection();
        table.scrollRectToVisible(table.getCellRect(
                ((NumericTable) table.getData()).getCurrentRow(),
                table.getColumnCount(), true));
    }

    /**
     * Test the selected row.
     */
    private void testRow() {
        int testRow = ((NumericTable) table.getData()).getCurrentRow();
        if (testRow >= ((NumericTable) table.getData()).getRowCount()) {
            testRow = 0;
        }
        table.updateRowSelection();
        for (int j = 0; j < inputNeurons.size(); j++) {
            inputNeurons.get(j).forceSetActivation(
                    ((NumericTable) table.getData()).getValue(testRow, j));
        }
        if (network != null) {
            network.update();
            network.fireNetworkChanged();
        } else {
            inputNeurons.get(0).getNetwork().update();
            inputNeurons.get(0).getNetwork().fireNetworkChanged();
        }
        if (iterationMode) {
            advanceRow();
        }
    }

    /**
     * Advance through the entire table and test each row.
     */
    private void testTable() {
        for (int j = 0; j < ((NumericTable) table.getData()).getRowCount(); j++) {
            ((NumericTable) table.getData()).setCurrentRow(j);
            table.scrollRectToVisible(table.getCellRect(
                    ((NumericTable) table.getData()).getCurrentRow(),
                    table.getColumnCount(), true));
            testRow();
        }
    }

    /**
     * @return the table
     */
    public SimbrainJTable getTable() {
        return table;
    }

    /**
     * Reset the data in this panel.
     *
     * @param data the data to set
     */
    public void setData(double[][] data) {
        this.data = data;
        if (data != null) {
            numericTable.setData(data);
        }

    }
}
