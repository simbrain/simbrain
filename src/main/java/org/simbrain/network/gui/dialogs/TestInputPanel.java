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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.trainer.DataPanel;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.math.NumericMatrix;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.table.SimbrainJTable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Panel for sending inputs from a table to a network. The action that calls
 * this class provides the input neurons and network panel from which the action
 * gets the network to be updated.
 *
 * @author Jeff Yoshimi
 * @author Lam Nguyen
 */
public class TestInputPanel extends DataPanel {

    /**
     * Network panel.
     */
    private NetworkPanel networkPanel;

    /**
     * True when iteration mode is on.
     */
    private boolean iterationMode = true;

    /**
     * Button used to advance row. Disabled when iteration mode is on.
     */
    private JButton advance;

    /**
     * This is the network that should be updated whenever the input neurons are
     * updated. If null, update the whole network
     */
    private Network network;

    /**
     * Reference to neuron group for cases when that is what's being edited.
     */
    private NeuronGroup neuronGroup;

    public static TestInputPanel createTestInputPanel(NetworkPanel networkPanel, NeuronGroup neuronGroup) {
        TestInputPanel tip = TestInputPanel.createTestInputPanel(networkPanel, neuronGroup.getNeuronList());
        tip.neuronGroup = neuronGroup;
        return tip;
    }

    /**
     * Create panel using a network panel and a list of selected neurons for
     * case where no data holder is provided (currently, applying test inputs to
     * an arbitrary set of loose neurons).
     *
     * @param networkPanel networkPanel, must not be null
     * @param inputNeurons input neurons of the network to be tested
     * @return the constructed panel
     */
    public static TestInputPanel createTestInputPanel(NetworkPanel networkPanel, final List<Neuron> inputNeurons) {

        NumericMatrix dataHolder = new NumericMatrix() {
            double[][] dataMatrix = new double[5][inputNeurons.size()];

            @Override
            public void setData(double[][] data) {
                dataMatrix = data;
            }

            @Override
            public double[][] getData() {
                return dataMatrix;
            }

        };
        final TestInputPanel panel = new TestInputPanel(networkPanel, inputNeurons, dataHolder);
        panel.addPropertyChangeListener(evt -> {
            panel.commitChanges(); //TODO: More efficient way?
        });
        return panel;
    }

    /**
     * Create the test input panel.
     *
     * @param networkPanel networkPanel, must not be null.
     * @param inputNeurons input neurons of the network to be tested.
     * @param dataHolder   the class whose data should be edited.
     * @return the constructed panel.
     */
    public static TestInputPanel createTestInputPanel(NetworkPanel networkPanel, List<Neuron> inputNeurons, NumericMatrix dataHolder) {
        final TestInputPanel panel = new TestInputPanel(networkPanel, inputNeurons, dataHolder);
        panel.addPropertyChangeListener(evt -> {
            panel.commitChanges(); //TODO: More efficient way?
        });
        return panel;
    }

    /**
     * Construct the panel using a reference to a class that has a double array.
     * Changes to the table will change the data in that class.
     *
     * @param networkPanel networkPanel, must not be null
     * @param inputNeurons input neurons of the network to be tested
     * @param dataHolder   the class whose data should be edited.
     */
    private TestInputPanel(NetworkPanel networkPanel, List<Neuron> inputNeurons, NumericMatrix dataHolder) {
        super(inputNeurons, dataHolder, 5, "Test Inputs");
        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }
        this.dataHolder = dataHolder;
        this.networkPanel = networkPanel;
        initTestInputPanel();
    }

    // TODO
    // LMSNetwork lms;
    // public TestInputPanel(NetworkPanel networkPanel, LMSNetwork lms) {
    //     super(lms.getInputData());
    //     this.lms = lms;
    //     if (networkPanel == null) {
    //         throw new IllegalArgumentException("networkPanel must not be null");
    //     }
    //     this.networkPanel = networkPanel;
    //     initTestInputPanel();
    // }

    /**
     * Initiate the test network panel using the network panel.
     */
    private void initTestInputPanel() {
        network = networkPanel.getNetwork();
        ((NumericTable) table.getData()).setIterationMode(iterationMode);
        JButton test = new JButton(testRowAction);
        advance = new JButton(advanceRowAction);
        JButton testTable = new JButton(testTableAction);
        JCheckBox iterationCheckBox = new JCheckBox(iterationModeAction);
        iterationCheckBox.setSelected(iterationMode);
        toolbars.add(table.getToolbarEditRows());
        JToolBar testToolBar = new JToolBar();
        testToolBar.add(test);
        testToolBar.add(advance);
        testToolBar.add(testTable);
        testToolBar.add(iterationCheckBox);
        toolbars.add(testToolBar);
    }

    /**
     * Action for advancing a row to be tested.
     */
    private Action advanceRowAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/plus.png"));
            putValue(SHORT_DESCRIPTION, "Advance row");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            advanceRow();
        }
    };

    /**
     * Action to test a row.
     */
    private Action testRowAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Step.png"));
            putValue(SHORT_DESCRIPTION, "Test row");
        }

        @Override
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

        @Override
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
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Play.png"));
            putValue(SHORT_DESCRIPTION, "Test table");
        }

        @Override
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
        table.scrollRectToVisible(table.getCellRect(((NumericTable) table.getData()).getCurrentRow(), table.getColumnCount(), true));
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

        // TODO: Replace with explicit boolean
        if (inputNeurons == null) {
            // lms.getNAList().get(0).setValues(((NumericTable) table.getData()).getVectorCurrentRow());
        } else {
            for (int j = 0; j < inputNeurons.size(); j++) {
                inputNeurons.get(j).forceSetActivation(((NumericTable) table.getData()).getLogicalValueAt(testRow, j));
            }
        }
        if (network != null) {
            network.update();
        } else {
            inputNeurons.get(0).getNetwork().update();
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
            table.scrollRectToVisible(table.getCellRect(((NumericTable) table.getData()).getCurrentRow(), table.getColumnCount(), true));
            testRow();
        }
    }

    public SimbrainJTable getTable() {
        return table;
    }

    /**
     * Resest the data in this panel.
     *
     * @param data the data to set
     */
    public void setData(double[][] data) {
        if (data != null) {
            ((NumericTable) table.getData()).setData(data);
        }
        if (neuronGroup != null) {
            neuronGroup.getInputManager().setData(data);
        }
    }

}
