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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.simbrain.network.core.Neuron;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.math.NumericMatrix;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;
import org.simbrain.util.table.SimbrainTableListener;
import org.simbrain.util.table.TableActionManager;

/**
 * A simple wrapper for a jtable used to represent input and target data for
 * neural networks.
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
     * Panel which represents input or target data. Can be created without data
     * initially, in which case a default dataset is created.
     *
     * @param neurons neurons corresponding to columns
     * @param data the numerical data
     * @param numVisibleColumns how many columns to try to make visible
     * @param name name for panel
     */
    public DataPanel(final List<Neuron> neurons, final NumericMatrix data,
            final int numVisibleColumns, final String name) {

        // If no data exists, create it!
        if (data.getData() == null) {
            table = new SimbrainJTable(new NumericTable(DEFAULT_NUM_ROWS,
                    neurons.size()));
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
        scroller.setMinimumSize(new Dimension(200, 500));
        scroller.setMaxVisibleColumns(numVisibleColumns);

        setLayout(new BorderLayout());
        add("Center", scroller);

        // Toolbars
        JPanel toolbars = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Open / Save Tools
        JToolBar fileToolBar = new JToolBar();
        fileToolBar.add(TrainerGuiActions.getOpenCSVAction(table, data));
        fileToolBar.add(TableActionManager
                .getSaveCSVAction((NumericTable) table.getData()));
        toolbars.add(fileToolBar);

        // Edit tools
        JToolBar editToolBar = new JToolBar();
        editToolBar.add(TableActionManager.getInsertRowAction(table));
        editToolBar.add(TableActionManager.getDeleteRowAction(table));
        // toolbars.add(editToolBar);

        // Randomize tools
        toolbars.add(table.getToolbarRandomize());

        add("North", toolbars);

        // Initialize listener
        table.getData().addListener(new SimbrainTableListener() {

            // TODO: These should make targeted changes rather than blanket changes
            //      to the whole table.  Inefficient for large tables.

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
        int additionalParentHeight = (parentFrame.getBounds().height - scroller
                .getHeight());
        scroller.resize();
        int newHeight = scroller.getPreferredSize().height
                + additionalParentHeight;
        if (parentFrame != null) {
            // Reset height of parent frame
            if (newHeight > 300) {
                parentFrame.setPreferredSize(new Dimension(parentFrame
                        .getPreferredSize().width, newHeight));
                parentFrame.pack();
                parentFrame.setLocationRelativeTo(null);
            }
        }
    }

    /**
     * Set the frame. Used for dynamically resizing the internal frame.
     *
     * @param parentFrame the parentFrame to set
     */
    public void setFrame(GenericFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    /**
     * @return the scroller
     */
    public SimbrainJTableScrollPanel getScroller() {
        return scroller;
    }

    /**
     * @return the table
     */
    public SimbrainJTable getTable() {
        return table;
    }

    /**
     * @param table the table to set
     */
    public void setTable(SimbrainJTable table) {
        this.table = table;
    }

}
