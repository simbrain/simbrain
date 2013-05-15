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

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

import org.simbrain.network.core.Neuron;
import org.simbrain.util.genericframe.GenericFrame;
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
public class DataPanel extends JPanel {

    /** Scrollpane. */
    private SimbrainJTableScrollPanel scroller;

    /** JTable contained in scroller. */
    private SimbrainJTable table;

    /** Default number of rows to open new table with. */
    private static final int DEFAULT_NUM_ROWS = 5;

    /** Parent frame. */
    private GenericFrame parentFrame;

    /**
     * Panel which represents input or target data.
     *
     * @param neurons neurons corresponding to columns
     * @param data the numerical data
     * @param name name for panel
     */
    public DataPanel(final List<Neuron> neurons, final DataMatrix data,
            final String name) {

        // If no data exists, create it!
        if (data.getData() == null) {
            table = new SimbrainJTable(new NumericTable(
                    DEFAULT_NUM_ROWS, neurons.size()));
        } else {
            table = new SimbrainJTable(new NumericTable(data.getData()));
        }

        // Set up column headings
        List<String> colHeaders = new ArrayList<String>();
        int i = 0;
        for (Neuron neuron : neurons) {
            colHeaders.add(new String("" + (i++ + 1) + " (" + neuron.getId())
                    + ")");
        }
        table.setColumnHeadings(colHeaders);
        table.getData().fireTableStructureChanged();
        scroller = new SimbrainJTableScrollPanel(table);
        scroller.setMaxVisibleColumns(5);

        setLayout(new BorderLayout());
        add("Center", scroller);

        // Toolbars
        JPanel toolbars = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Open / Save Tools
        JToolBar fileToolBar = new JToolBar();
        fileToolBar.add(TrainerGuiActions.getOpenCSVAction(table,
                data));
        fileToolBar.add(TableActionManager
                .getSaveCSVAction((NumericTable) table.getData()));
        toolbars.add(fileToolBar);

        // Edit tools
        JToolBar editToolBar = new JToolBar();
        editToolBar
                .add(TableActionManager.getInsertRowAction(table));
        editToolBar
                .add(TableActionManager.getDeleteRowAction(table));
        toolbars.add(editToolBar);

        // Randomize tools
        toolbars.add(table.getToolbarRandomize());

        add("North", toolbars);

        // Initialize listener
        table.getData().addListener(new SimbrainTableListener() {

            public void columnAdded(int column) {
                // Should not happen.
            }

            public void columnRemoved(int column) {
                // Should not happen.
            }

            public void rowAdded(int row) {
                data.setData(((NumericTable) table.getData()).asDoubleArray());
                resizePanel();
            }

            public void rowRemoved(int row) {
                data.setData(((NumericTable) table.getData()).asDoubleArray());
                resizePanel();
            }

            public void cellDataChanged(int row, int column) {
                data.setData(((NumericTable) table.getData()).asDoubleArray());
            }

            public void tableDataChanged() {
                data.setData(((NumericTable) table.getData()).asDoubleArray());
            }

            public void tableStructureChanged() {
                data.setData(((NumericTable) table.getData()).asDoubleArray());
                resizePanel();
            }

        });

    }

    /**
     * Resize the panel and parent frame.
     */
    private void resizePanel() {
        scroller.resize();
        if (parentFrame != null) {
            parentFrame.setMaximumSize(parentFrame.getPreferredSize());
            parentFrame.pack();
        }
    }

    /**
     * @param parentFrame the parentFrame to set
     */
    public void setFrame(GenericFrame parentFrame) {
        this.parentFrame = parentFrame;
        resizePanel();
    }

    /**
     * Interface that indicates where the data is held in a class, and allows it
     * to be used with the data viewer.
     *
     */
    public interface DataMatrix {

        /**
         * Set the data.
         *
         * @param data the data to set
         */
        public void setData(double[][] data);

        /**
         * Get the data.
         *
         * @return the data to get
         */
        public double[][] getData();
    }

    /**
     * @return the scroller
     */
    public SimbrainJTableScrollPanel getScroller() {
        return scroller;
    }

}
