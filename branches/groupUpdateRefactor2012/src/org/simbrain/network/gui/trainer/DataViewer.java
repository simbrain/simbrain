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
import javax.swing.JToolBar;

import org.simbrain.network.interfaces.Neuron;
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

    /** Default number of rows to open new table with. */
    private static final int DEFAULT_NUM_ROWS = 5;

    /**
     * @return the table
     */
    SimbrainJTable getTable() {
        return table;
    }
    
    /**
     * Create a panel for viewing input or training data in a trainer.
     *
     * @param network the network to be trained
     * @param type whether this is input or training data
     */
    public DataViewer(final List<Neuron> neurons,
            final DataHolder data, final String name) {
    	
    	// If no data exists, create it!
    	if (data.getData() == null)  {
            table = new SimbrainJTable(new DefaultNumericTable(
                    DEFAULT_NUM_ROWS, neurons.size()));    		
    	} else {
    		table = new SimbrainJTable(new DefaultNumericTable(data.getData()));
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

        // Initialize listener
        table.getData().addListener(new SimbrainTableListener() {

            public void columnAdded(int column) {
                // Should not happen.
            }

            public void columnRemoved(int column) {
                // Should not happen.
            }

            public void rowAdded(int row) {
				data.setData(((NumericTable) table.getData()).asArray());
           }

            public void rowRemoved(int row) {
				data.setData(((NumericTable) table.getData()).asArray());
            }

            public void cellDataChanged(int row, int column) {
				data.setData(((NumericTable) table.getData()).asArray());
            }

            public void tableDataChanged() {
				data.setData(((NumericTable) table.getData()).asArray());
            }

            public void tableStructureChanged() {
				data.setData(((NumericTable) table.getData()).asArray());
            }

        });
        // Set the table
        this.setTable(table);

    }

    /**
     * Factory method for creating a data viewer panel.
     *
     * @param trainerPanel parent trainer panel.
     * @param type whether this is input or training data.
     * @return the panel
     */
    public static JPanel createDataViewerPanel(final List<Neuron> neurons,
            final DataHolder data, final String name) {
        final DataViewer viewer = new DataViewer(neurons, data, name);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add("Center", viewer);

        // Toolbars
        JPanel toolbars = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Open / Save Tools
        JToolBar fileToolBar = new JToolBar();
        fileToolBar
                .add(TrainerGuiActions.getOpenCSVAction(viewer.getTable(), data));
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
    
    /**
     * Interface that indicates where the data is held in a class, and allows it to be used
     * with the data viewer.
     *
     */
    public interface DataHolder {
    	
    	/**
    	 * Set the data
    	 *
    	 * @param data the data to set
    	 */
    	public void setData(double[][] data);
    	
    	/**
    	 * Get the data
    	 *
    	 * @return the data to get
    	 */
    	public double[][] getData();
    }
    
    
}
