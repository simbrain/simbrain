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
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

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
public class TestNetworkPanel extends SimbrainJTableScrollPanel {

    /** JTable contained in scroller. */
    private SimbrainJTable table;

    /** The Backprop network to test. */
    private BackpropNetwork network;

    /**
     * Embed the scrollpanel in a panel with a toolbar.
     *
     * @param scroller the scroller to embed
     * @return the formatted jpanel.
     */
    public static JPanel getTestNetworkPanel(final TestNetworkPanel scroller) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add("Center", scroller);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(scroller.getTable().getToolbarCSV());
        toolbar.add(scroller.getTable().getToolbarRandomize());
        JButton step = new JButton(ResourceManager.getImageIcon("Step.png"));
        step.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scroller.advanceRow();
            }
        });
        JToolBar stepToolBar = new JToolBar();
        stepToolBar.add(step);
        toolbar.add(stepToolBar);
        panel.add("North", toolbar);
        return panel;
    }

    /**
     * Construct a test network panel using a specified Backprop network.
     *
     * @param network the Backprop network to test
     */
    public TestNetworkPanel(final BackpropNetwork network) {
        this.network = network;
        table = new SimbrainJTable(new NumericTable(5, network.
                getInputNeurons().size()));
        ((NumericTable) table.getData()).setIterationMode(true);
        // Set up column headings
        List<String> colHeaders = new ArrayList<String>();
        for (int i = 0; i < network.getInputNeurons().size(); i++) {
            colHeaders.add(new String("" + (i + 1) + " (" + network.
                    getInputNeurons().get(i).getId()) + ")");
        }
        table.setColumnHeadings(colHeaders);
        table.getData().fireTableStructureChanged();

        this.setTable(table);
    }

    /**
     * Advances the row to test.
     */
    private void advanceRow() {
        // Set input layer activation
        int testRow = ((NumericTable) table.getData()).getCurrentRow() + 1;
        if (testRow >=  ((NumericTable) table.getData()).getRowCount()) {
            testRow = 0;
        }
        for (int j = 0; j < network.getInputNeurons().size(); j++) {
            network.getInputNeurons().get(j)
            .setInputValue(((NumericTable) table.getData()).
                    getValue(testRow, j));
        }
        ((NumericTable) table.getData()).updateCurrentRow();
        table.updateRowSelection();
        repaint();

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
