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

import org.nd4j.linalg.api.ndarray.INDArray;
import org.simbrain.network.core.Neuron;
import org.simbrain.util.Utils;
import org.simbrain.util.math.NumericMatrix;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;
import org.simbrain.util.table.TableActionManager;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A data table used to represent input or target data for a group of neurons in
 * a Simbrain neuron. Each column of the table is linked to a neuron.
 *
 * @author Jeff Yoshimi
 */
public class DataPanel extends JPanel {

    /**
     * Scrollpane.
     */
    protected SimbrainJTableScrollPanel scroller;

    /**
     * JTable contained in scroller.
     */
    protected SimbrainJTable table;

    /**
     * Default number of rows to open new table with.
     */
    private static final int DEFAULT_NUM_ROWS = 5;

    /**
     * Parent frame.
     */
    private Window parentFrame;

    /**
     * The external object that has the data being edited.
     */
    protected NumericMatrix dataHolder;

    /**
     * The neurons that this data will be sent as input to.
     */
    protected List<Neuron> inputNeurons;

    /**
     * The toolbar panel.
     */
    protected JPanel toolbars;

    // TODO: Document and remove redundancies
    public DataPanel(final INDArray ndArray) {

        // Wrap the ndarray in a numeric matrix
        NumericMatrix dataHolder = new NumericMatrix() {
            @Override
            public void setData(double[][] data) {
                ndArray.data().setData(Utils.flatten(data));
            }

            @Override
            public double[][] getData() {
                return ndArray.toDoubleMatrix();
            }
        };

        //TODO: Duplicated code

        // If no data exists, create it!
        if (dataHolder.getData() == null) {
            table = SimbrainJTable.createTable(new NumericTable(DEFAULT_NUM_ROWS, ndArray.columns()));
        } else {
            table = SimbrainJTable.createTable(new NumericTable(dataHolder.getData()));
        }

        // Set up scrollbar
        scroller = new SimbrainJTableScrollPanel(table);
        scroller.setMinimumSize(new Dimension(200, 500));
        scroller.setMaxVisibleColumns(5);

        // Setting up necessary elements.
        constructorSetUp();

    }


    //TODO
    // This was done quickly and it's not clear it's taking advantage of the dl4j dataset's features
    public DataPanel(final NumericMatrix dataHolder, int numNeurons, final int numVisibleColumns, final String name) {
        this.dataHolder = dataHolder;
        this.inputNeurons = Collections.EMPTY_LIST;
        // If no data exists, create it!
        if (dataHolder.getData() == null) {
            table = SimbrainJTable.createTable(new NumericTable(DEFAULT_NUM_ROWS, numNeurons));
        } else {
            table = SimbrainJTable.createTable(new NumericTable(dataHolder.getData()));
        }

        // Set up scrollbar
        scroller = new SimbrainJTableScrollPanel(table);
        scroller.setMinimumSize(new Dimension(200, 500));
        scroller.setMaxVisibleColumns(numVisibleColumns);

        // Open / Save Tools
        // Setting up necessary elements.
        constructorSetUp();


    }

    /**
     * Panel which represents input or target data. Can be created without data
     * initially, in which case a default dataset is created.
     *
     * @param neurons           neurons corresponding to columns
     * @param dataHolder        the numerical data
     * @param numVisibleColumns how many columns to try to make visible
     * @param name              name for panel
     */
    public DataPanel(final List<Neuron> neurons, final NumericMatrix dataHolder, final int numVisibleColumns, final String name) {

        this.dataHolder = dataHolder;
        this.inputNeurons = neurons;

        // If no data exists, create it!
        if (dataHolder.getData() == null) {
            table = SimbrainJTable.createTable(new NumericTable(DEFAULT_NUM_ROWS, neurons.size()));
        } else {
            table = SimbrainJTable.createTable(new NumericTable(dataHolder.getData()));
        }

        // Set up column headings
        List<String> colHeaders = new ArrayList<String>();
        for (Neuron neuron : neurons) {
            colHeaders.add(neuron.getId());
        }
        table.setColumnHeadings(colHeaders);
        table.getData().fireTableStructureChanged();
        table.setShowInsertColumnPopupMenu(false);
        table.setShowDeleteColumnPopupMenu(false);
        table.setShowEditInPopupMenu(false);

        // Set up scrollbar
        scroller = new SimbrainJTableScrollPanel(table);
        scroller.setMinimumSize(new Dimension(200, 500));
        scroller.setMaxVisibleColumns(numVisibleColumns);

        // Set up necessary elements.
        constructorSetUp();

        // TODO: This is something that the other constructors do not have.
        // Edit tools
        JToolBar editToolBar = new JToolBar();
        editToolBar.add(TableActionManager.getInsertRowAction(table));
        editToolBar.add(TableActionManager.getDeleteRowAction(table));
        // toolbars.add(editToolBar);


    }


    /*

     */
    private void constructorSetUp() {
        setLayout(new BorderLayout());
        add("Center", scroller);

        // Toolbars
        toolbars = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Open / Save Tools
        JToolBar fileToolBar = new JToolBar();
        fileToolBar.add(TrainerGuiActions.getOpenCSVAction(table, dataHolder));
        fileToolBar.add(TableActionManager.getSaveCSVAction((NumericTable) table.getData()));
        toolbars.add(fileToolBar);

        // Randomize tools
        toolbars.add(table.getToolbarRandomize());

        add("North", toolbars);

        table.getData().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                resizePanel();
            }
        });

        return;
    }

    /**
     * Called externally when the data in the visible table can be converted to
     * a double array and applied to the data holder. Only apply the data (which
     * can be quite large) if the data have changed.
     *
     * @return
     */
    public boolean commitChanges() {
        //System.out.println("DataPanel commit changes " + table.hasChanged());
        if (table.hasChanged()) {
            dataHolder.setData(((NumericTable) table.getData()).asDoubleArray());
            // Set has changed to false, so that the data only re-committed if
            // the data have changed.
            table.setHasChangedSinceLastSave(false);
            return true;
        }
        return false;
    }

    /**
     * Resize the panel and parent frame.
     */
    private void resizePanel() {
        if (parentFrame != null) {
            int additionalParentHeight = (parentFrame.getBounds().height - scroller.getHeight());
            scroller.resize();
            int newHeight = scroller.getPreferredSize().height + additionalParentHeight;
            // Reset height of parent frame
            if (newHeight > 300) {
                //                parentFrame.setPreferredSize(new Dimension(parentFrame
                //                    .getPreferredSize().width, newHeight));
                parentFrame.pack();
                //parentFrame.setLocationRelativeTo(null);
            }
        }
    }

    /**
     * Set the frame. Used for dynamically resizing the internal frame.
     *
     * @param parentFrame the parentFrame to set
     */
    public void setFrame(Window parentFrame) {
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
