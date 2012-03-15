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
package org.simbrain.network.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.listeners.NetworkListener;
import org.simbrain.network.util.Comparators;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;

/**
 * Widget to display the synaptic connections between two layers of neurons as a
 * matrix, in a jtable.
 *
 * @author jyoshimi
 */
public class WeightMatrixViewer extends SimbrainJTableScrollPanel {

    /** JTable contained in scroller. */
    private SimbrainJTable table;

   /**
     * Embed the scrollpanel in a widget with a toolbar.
     *
     * @param scroller the scroller to embed
     * @return the formatted jpanel.
     */
    public static JPanel getWeightMatrixPanel(WeightMatrixViewer scroller) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add("Center", scroller);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(scroller.getTable().getToolbarRandomize());
        toolbar.add(scroller.getTable().getToolbarCSV());
        panel.add("North", toolbar);
        return panel;
    }

    /**
     * Create a panel for viewing the matrices connecting a set of source and
     * target neuron lists.
     *
     * @param panel the panel from which to draw the matrix.
     */
    public WeightMatrixViewer(NetworkPanel panel) {

        // Get source and target lists
        ArrayList<Neuron> sourceList = panel.getSourceModelNeurons();
        ArrayList<Neuron> targetList = panel.getSelectedModelNeurons();

        // By default the lists are sorted horizontally.
        //  TODO: Allow for vertical sorting, or for some appropriate sorting
        //      when displaying an adjacency matrix
        Collections.sort(sourceList, Comparators.X_ORDER);
        Collections.sort(targetList, Comparators.X_ORDER);

        // Populate data in simbrain table
        Synapse[][] weights = SimnetUtils.getWeightMatrix(sourceList,
                targetList);
        WeightMatrix weightMatrix = new WeightMatrix(weights);
        table = new SimbrainJTable(weightMatrix);

        // Create names for row headings
        List<String> rowHeaders = new ArrayList<String>();
        int i = 0;
        for (Neuron neuron : sourceList) {
            rowHeaders.add(new String("" + (i++ + 1) + " (" + neuron.getId())
                    + ")");
        }

        // Create names for column headings
        List<String> colHeaders = new ArrayList<String>();
        i = 0;
        for (Neuron neuron : targetList) {
            colHeaders.add(new String("" + (i++ + 1) + " (" + neuron.getId())
                    + ")");
        }
        table.setColumnHeadings(colHeaders);
        table.setRowHeadings(rowHeaders);
        table.getData().fireTableStructureChanged();

        // Set the table
        this.setTable(table);

        // Add network listener
        panel.getRootNetwork().addNetworkListener(new NetworkListener() {

            public void networkChanged() {
                repaint();
            }

            public void networkUpdateMethodChanged() {
            }

            public void neuronClampToggled() {
            }

            public void synapseClampToggled() {
            }

        });
    }

    /**
     * Matrix of synapses to be viewed in a SimbrainJTable.
     */
    private class WeightMatrix extends NumericTable {

        /** Underlying data. */
        private Synapse[][] weights;

        /** Reference to root network. */
        private RootNetwork parentNetwork;

        /**
         * @param weights the weights to set
         */
        public WeightMatrix(Synapse[][] weights) {
            this.weights = weights;
        }

        @Override
        public void setValue(int row, int col, Double value) {
            if (weights[row][col] != null) {
                weights[row][col].setStrength(value);
                /**
                 * Save reference when a non-null is found (important for
                 * networks with null vals)
                 */
                parentNetwork = weights[row][col].getRootNetwork();
            }
            //TODO: Below ok with large changes?
            if (parentNetwork != null) {
                parentNetwork.fireNetworkChanged();
            }
        }

        @Override
        public Double getValue(int row, int col) {
            // TODO: For null case render cell in some special way.
            // Other null handling also needed.
            if (weights[row][col] != null) {
                return weights[row][col].getStrength();
            } else {
                return new Double(0);
            }
        }

        @Override
        public int getRowCount() {
            return weights.length;
        }

        @Override
        public int getColumnCount() {
            return weights[0].length;
        }

    }

    /**
     * @return the table
     */
    public SimbrainJTable getTable() {
        return table;
    }

}
