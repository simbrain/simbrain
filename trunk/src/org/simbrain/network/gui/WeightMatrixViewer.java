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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.util.Comparators;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;

/**
 * Widget to display the synaptic connections between two layers of neurons as a
 * matrix, in a jtable.
 *
 * @author jyoshimi
 */
public class WeightMatrixViewer extends SimbrainJTableScrollPanel {

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
        double[][] matrix = SimnetUtils.getWeights(sourceList, targetList);
        SimbrainJTable table = new SimbrainJTable(matrix.length,
                matrix[0].length);
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                table.getData().setValue(i, j, matrix[i][j]);
            }
        }

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

    }

}
