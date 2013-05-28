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
package org.simbrain.network.gui.actions;

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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.NetworkSelectionEvent;
import org.simbrain.network.gui.NetworkSelectionListener;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;

/**
 * Action to send inputs from a table to a network. The user of this class
 * provides the input neurons and network panel from which the action gets
 * the network to be updated. If input neurons are not provided, selected
 * neurons are used as input neurons.
 *
 * @author Jeff Yoshimi
 * @author Lam Nguyen
 */
public class TestInputAction extends AbstractAction {

    {
        putValue(NAME, "Test network...");
        putValue(SHORT_DESCRIPTION, "Test network...");
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Trainer.png"));
    }

    /** Network panel. */
    private NetworkPanel networkPanel;

    /** The panel used to test inputs to a network. */
    private JPanel testInputPanel;

    /** JTable contained in scroller. */
    private SimbrainJTable table;

    /** Scroll panel for table. */
    private SimbrainJTableScrollPanel scroller;

    /**
     * This is the network that should be updated whenever the input neurons are
     * updated. If null, update the whole network
     */
    private Network network;

    /** The nodes to test. */
    private List<Neuron> inputNeurons;

    /**
     * Construct action.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public TestInputAction(NetworkPanel networkPanel) {

        super("Test inputs...");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        //        updateAction();

        // add a selection listener to update state based on selection
        networkPanel.addSelectionListener(new NetworkSelectionListener() {
            /** @see NetworkSelectionListener */
            public void selectionChanged(NetworkSelectionEvent event) {
                updateAction();
            }
        });
    }

    /**
     * Construct action.
     *
     * @param networkPanel networkPanel, must not be null
     * @param inputNeurons input neurons of the network to be tested
     */
    public TestInputAction(NetworkPanel networkPanel, List<Neuron> inputNeurons) {

        super("Test inputs...");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        this.inputNeurons = inputNeurons;
    }

    /**
     * Set test input panel based on number of selected neurons.
     */
    private void updateAction() {
        int numNeurons = networkPanel.getSelectedNeurons().size();

        if (numNeurons > 0) {
            inputNeurons = networkPanel.getSelectedModelNeurons();
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

    /**
     * Initialize and display the test input panel.
     */
    public void actionPerformed(ActionEvent event) {
        initTestInputPanel();
        networkPanel.displayPanel(testInputPanel, "Test inputs");
    }

    /**
     * Construct a test network panel using the network panel.
     */
    private void initTestInputPanel() {
        testInputPanel = new JPanel();
        network = networkPanel.getNetwork();
        table = new SimbrainJTable(new NumericTable(5, inputNeurons.size()));
        ((NumericTable) table.getData()).setIterationMode(true);
        // Set up column headings
        List<String> colHeaders = new ArrayList<String>();
        for (int i = 0; i < inputNeurons.size(); i++) {
            colHeaders.add(new String("" + (i + 1) + " ("
                    + inputNeurons.get(i).getId()) + ")");
        }
        table.setColumnHeadings(colHeaders);
        table.getData().fireTableStructureChanged();
        scroller = new SimbrainJTableScrollPanel(table);
        testInputPanel.setLayout(new BorderLayout());
        testInputPanel.add("Center", scroller);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JToolBar editRowToolBar = new JToolBar();
        toolbar.add(table.getToolbarCSV(true, false));
        toolbar.add(editRowToolBar);
        editRowToolBar.add(table.getToolbarEditRows());
        toolbar.add(table.getToolbarRandomize());
        JButton test = new JButton(testRowAction);
        JButton advance = new JButton(advanceRowAction);
        JToolBar testToolBar = new JToolBar();
        testToolBar.add(test);
        testToolBar.add(advance);
        toolbar.add(testToolBar);
        testInputPanel.add("North", toolbar);
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
        if (testRow >= ((NumericTable) table.getData()).getRowCount()) {
            testRow = 0;
        }
        table.updateRowSelection();
        for (int j = 0; j < inputNeurons.size(); j++) {
            inputNeurons.get(j).setInputValue(
                    ((NumericTable) table.getData()).getValue(testRow, j));
        }
        if (network != null) {
            network.update();
            network.fireNetworkChanged();
        } else {
            inputNeurons.get(0).getParentNetwork().update();
            inputNeurons.get(0).getParentNetwork().fireNetworkChanged();
        }
    }

    /**
     * @return the table
     */
    public SimbrainJTable getTable() {
        return table;
    }
}
