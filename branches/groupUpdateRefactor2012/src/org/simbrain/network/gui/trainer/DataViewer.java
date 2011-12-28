/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.trainer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.simbrain.network.gui.trainer.TrainerPanel.TrainerDataType;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.util.table.DefaultNumericTable;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;
import org.simbrain.util.table.SimbrainTableListener;
import org.simbrain.util.table.TableActionManager;

/**
 * Widget to display data used in training a neural network using supervised
 * learning.
 *
 * @author jyoshimi
 */
public class DataViewer extends SimbrainJTableScrollPanel {

    /** JTable contained in scroller. */
    private SimbrainJTable table;

    /** Does this represent training or input data. */
    private TrainerDataType type;

    /**
     * Reference to parent trainer panel, which in turn has a reference to the
     * trainer.
     */
    private TrainerPanel parent;

    /** Default number of rows to open new table with. */
    private static final int DEFAULT_NUM_ROWS = 5;

    /**
     * Create a panel for viewing the matrices connecting a set of source and
     * target neuron lists.
     *
     * @param panel the panel from which to draw the matrix.
     */
    public DataViewer(final TrainerPanel trainerPanel,
            final TrainerDataType type) {

        this.type = type;
        this.parent = trainerPanel;
        Trainer trainer = trainerPanel.getTrainer();

        // Create names for column headings
        List<String> colHeaders = new ArrayList<String>();
        int i = 0;

        // Populate data in simbrain table
        if (type == TrainerDataType.Input) {
            if (trainer.getInputData() == null) {
                System.out.println("Input data is null");
                table = new SimbrainJTable(new DefaultNumericTable(
                        DEFAULT_NUM_ROWS, trainer.getInputLayer().size()));
            } else {
                table = new SimbrainJTable(new DefaultNumericTable(
                        trainer.getInputData()));
            }
            for (Neuron neuron : trainer.getInputLayer()) {
                colHeaders.add(new String("" + (i++ + 1) + " ("
                        + neuron.getId())
                        + ")");
            }
        } else {
            if (trainer.getTrainingData() == null) {
                System.out.println("Training data is null");
                table = new SimbrainJTable(new DefaultNumericTable(
                        DEFAULT_NUM_ROWS, trainer.getOutputLayer().size()));
            } else {
                table = new SimbrainJTable(new DefaultNumericTable(
                        trainer.getTrainingData()));
            }
            for (Neuron neuron : trainer.getOutputLayer()) {
                colHeaders.add(new String("" + (i++ + 1) + " ("
                        + neuron.getId())
                        + ")");
            }
        }
        table.setColumnHeadings(colHeaders);
        table.getData().fireTableStructureChanged();

        // Initialize listener
        initListener();

        // Set the table
        this.setTable(table);

    }

    /**
     * Listen for changes in the data, and update the trainer data as they
     * occur.
     */
    private void initListener() {
        table.getData().addListener(new SimbrainTableListener() {

            public void columnAdded(int column) {
                // Should not happen.
            }

            public void columnRemoved(int column) {
                // Should not happen.
            }

            public void rowAdded(int row) {
                updateTrainerData();
            }

            public void rowRemoved(int row) {
                updateTrainerData();
            }

            public void cellDataChanged(int row, int column) {
                updateTrainerData();
            }

            public void tableDataChanged() {
                updateTrainerData();
            }

            public void tableStructureChanged() {
                updateTrainerData();
            }

        });

    }

    /**
     * Update the trainer data.
     */
    private void updateTrainerData() {
        if (type == TrainerDataType.Input) {
            parent.getTrainer().setInputData(
                    ((NumericTable) table.getData()).asArray());
        } else {
            parent.getTrainer().setTrainingData(
                    ((NumericTable) table.getData()).asArray());
        }
    }

    /**
     * @return the table
     */
    public SimbrainJTable getTable() {
        return table;
    }
    
    public static JPanel getDataViewerPanel(final TrainerPanel trainerPanel,
            final TrainerDataType type) {
        final DataViewer viewer = new DataViewer(trainerPanel, type);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add("Center", viewer);

        // Toolbars
        JPanel toolbars = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Open / Save Tools
        JToolBar fileToolBar = new JToolBar();
        fileToolBar
                .add(TrainerGuiActions.getOpenCSVAction(trainerPanel, viewer.getTable(), type));
        fileToolBar.add(TableActionManager
                .getSaveCSVAction((NumericTable) viewer.getTable().getData()));
        toolbars.add(fileToolBar);

        // Edit tools
        JToolBar editToolBar = new JToolBar();
        editToolBar
                .add(TableActionManager.getInsertRowAction(viewer.getTable()));
        editToolBar
                .add(TableActionManager.getDeleteRowAction(viewer.getTable()));
        toolbars.add(editToolBar);

        // Randomize tools
        toolbars.add(viewer.getTable().getToolbarRandomize());

        mainPanel.add("North", toolbars);
        return mainPanel;
    }

}
