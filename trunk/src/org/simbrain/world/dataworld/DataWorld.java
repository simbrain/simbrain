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
package org.simbrain.world.dataworld;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXTable;
import org.simbrain.util.StandardDialog;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.gui.ConsumingAttributeMenu;
import org.simbrain.workspace.gui.CouplingMenuItem;
import org.simbrain.workspace.gui.CouplingMenus;
import org.simbrain.workspace.gui.ProducingAttributeMenu;

/**
 * <b>DataWorld</b> is a jpanel which contains a table object and a that table's model object.
 *
 * @author rbartley, jyoshimi
 */
public class DataWorld extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Data table. */
    private JXTable table;

    /** Underlying data. */
    private final DataModel<Double> dataModel;

    /** Back reference. */
    final DataWorldDesktopComponent ws;

    /** Point selected. */
    private Point selectedPoint;

    /** Inserts a new row. */
    private JMenuItem addRow = new JMenuItem("Insert row");

    /** Inserts a new column. */
    private JMenuItem addCol = new JMenuItem("Insert column");

    /** Removes a row. */
    private JMenuItem remRow = new JMenuItem("Delete row");

    /** Removes a column. */
    private JMenuItem remCol = new JMenuItem("Delete column");

    /** Local variable used for iterating. */
    private int thisRowCount = 0;

    /** Local variable used for iterating. */
    private int currentRowCounter = 0;
    
    /** Grid Color. */
    private Color gridColor =  Color.LIGHT_GRAY;
        
    
    /**
     * Creates a new instance of the data world.
     *
     * @param ws World frame to create a new data world within
     */
    public DataWorld(final DataWorldDesktopComponent ws) {
        super(new BorderLayout());
        this.ws = ws;

        this.dataModel = ws.getWorkspaceComponent().getDataModel();
        table = new JXTable(new DataTableModel(dataModel));
        add(table, BorderLayout.CENTER);
        add(table.getTableHeader(), BorderLayout.NORTH);
        
        table.addKeyListener(keyListener);
        table.addMouseListener(mouseListener);
        table.setColumnSelectionAllowed(true);
        table.setRolloverEnabled(true);
        table.setRowSelectionAllowed(true);
        table.setGridColor(gridColor);
        
        addRow.addActionListener(addRowHereListener);
        addCol.addActionListener(addColHereListener);
        
        remRow.addActionListener(remRowHereListener);
        remCol.addActionListener(remColHereListener);
        
//        for(int i = 0; i < table.getColumnCount(); i++) {
//            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
//        }
        
   }
    
    private ActionListener addRowHereListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            if (getSelectedPoint().x < (getTable().getRowHeight() * getTable().getRowCount())) {
                getDataModel().insertNewRow(getSelectedRow(), new Double(0));
            } else {
                getDataModel().addNewRow(new Double(0));
            }
            ws.getWorkspaceComponent().setChangedSinceLastSave(true);
            ws.pack();
        }
    };

    private ActionListener addColHereListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            getDataModel().insertNewColumn(getSelectedColumn(), new Double(0));
            ws.getWorkspaceComponent().setChangedSinceLastSave(true);
            ws.pack();
        }
    };

    private ActionListener remRowHereListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            getDataModel().removeRow(getSelectedRow());
            ws.getWorkspaceComponent().setChangedSinceLastSave(true);
            ws.pack();
        }
    };

    private ActionListener remColHereListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            getDataModel().removeColumn(getSelectedColumn());
            ws.getWorkspaceComponent().setChangedSinceLastSave(true);
            ws.pack();
        }
    };

    /**
     * Returns the currently selected column.
     *
     * @return the currently selected column
     */
    public int getSelectedColumn() {
        return getTable().columnAtPoint(selectedPoint);
    }

    /**
     * Returns the currently selected row.
     *
     * @return the currently selected row
     */
    public int getSelectedRow() {
        return getTable().rowAtPoint(selectedPoint);
    }

    private MouseListener mouseListener = new MouseAdapter() {
        /**
         * Responds to mouse pressed event.
         *
         * @param e Mouse event
         */
        public void mousePressed(final MouseEvent e) {
            selectedPoint = e.getPoint();
            // TODO: should use isPopupTrigger, see e.g. ContextMenuEventHandler
            boolean isRightClick = (e.isControlDown() || (e.getButton() == 3));
            if (isRightClick) {
                JPopupMenu menu = buildPopupMenu();
                menu.show(DataWorld.this, (int) selectedPoint.getX(), (int) selectedPoint.getY());
            }
        }
    };

    /**
     * @return The pop up menu to be built.
     */
    public JPopupMenu buildPopupMenu() {
        JPopupMenu ret = new JPopupMenu();
        ret.add(addRow);
        if (getSelectedColumn() != 0) {
            ret.add(addCol);
        }
        ret.add(remRow);
        if (getSelectedColumn() != 0) {
            ret.add(remCol);
        }
        ret.addSeparator();
        JMenu producerMenu = new ProducingAttributeMenu("Receive coupling from", ws.getWorkspaceComponent().getWorkspace(), ws.getWorkspaceComponent().getConsumingAttributes().get(getSelectedColumn()));
        ret.add(producerMenu);
        JMenu consumerMenu = new ConsumingAttributeMenu("Send coupling to", ws.getWorkspaceComponent().getWorkspace(), ws.getWorkspaceComponent().getProducingAttributes().get(getSelectedColumn()));
        ret.add(consumerMenu);

        return ret;
    }

    /**
     * @return World type.
     */
    public String getType() {
        return "DataWorld";
    }

    /**
     * @return Returns the table.
     */
    public JXTable getTable() {
        return table;
    }
    
    /**
     * Displays the randomize dialog.
     */
    public void displayRandomizeDialog() {
        StandardDialog rand = new StandardDialog();
        JPanel pane = new JPanel();
        JTextField lower = new JTextField();
        JTextField upper = new JTextField();
        lower.setText(Integer.toString(dataModel.getLowerBound()));
        lower.setColumns(3);
        upper.setText(Integer.toString(dataModel.getUpperBound()));
        upper.setColumns(3);
        pane.add(new JLabel("Lower Bound"));
        pane.add(lower);
        pane.add(new JLabel("Upper Bound"));
        pane.add(upper);

        rand.setContentPane(pane);
        rand.pack();
        rand.setLocationRelativeTo(ws);
        rand.setVisible(true);
        if (!rand.hasUserCancelled()) {
            dataModel.setLowerBound(Integer.parseInt(lower.getText()));
            dataModel.setUpperBound(Integer.parseInt(upper.getText()));
        }

        repaint();
    }
    

    /**
     * Increment a number of times equal to the last column.
     */
    public void incrementUsingLastColumn() {
        if (currentRowCounter < thisRowCount) {
            currentRowCounter++;
        } else {
            incrementCurrentRow();
            currentRowCounter = 0;
            thisRowCount = (int) Double.parseDouble(""
                    + table.getModel().getValueAt(dataModel.getCurrentRow(),
                            table.getModel().getColumnCount() - 1));
        }
    }

    /**
     * Increment current row by 1.
     */
    public void incrementCurrentRow() {
        if (dataModel.isIterationMode()) {
            table.setColumnSelectionAllowed(false);
            if (dataModel.getCurrentRow() >= (table.getRowCount() - 1)) {
                dataModel.setCurrentRow(0);
            } else {
                dataModel.setCurrentRow(dataModel.getCurrentRow() + 1);
            }
            table.setRowSelectionInterval(dataModel.getCurrentRow(), dataModel.getCurrentRow());
        } else {
            table.setColumnSelectionAllowed(true);
        }
    }


    /**
     * @return Returns the model.
     */
    public DataModel<Double> getDataModel() {
        return dataModel;
    }

    /**
     * @return The selected point.
     */
    public Point getSelectedPoint() {
        return selectedPoint;
    }

    /**
     * Sets the selected point.
     *
     * @param selectedPoint Valuet to set
     */
    public void setSelectedPoint(final Point selectedPoint) {
        this.selectedPoint = selectedPoint;
    }

    private KeyListener keyListener = new KeyAdapter() {
        /**
         * Responds to key typed events.
         *
         * @param arg0 Key event
         */
        public void keyTyped(final KeyEvent arg0) {
            ws.getWorkspaceComponent().setChangedSinceLastSave(true);
        }
    };


    /**
     * Handle iteration mode and column updating.
     */
    // TODO associate to update event
    public void completedInputRound() {
        if (dataModel.isIterationMode()) {
            if (dataModel.isLastColumnBasedIteration()) {
                incrementUsingLastColumn();
            } else {
                incrementCurrentRow();
            }
        }
    }

    private ActionListener couplingMenuItemListener = new ActionListener() {
        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent event) {
            if (event.getSource() instanceof CouplingMenuItem) {
                CouplingMenuItem menuItem = (CouplingMenuItem) event.getSource();
                Collection<? extends Producer> producers = menuItem.getWorkspaceComponent().getProducers();
                
                ws.getWorkspaceComponent().wireCouplings(producers);
            }
        }
    };
    
//    class DataWorldCellRenderer extends DefaultTableCellRenderer {
//        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column){
//            setEnabled(table == null || table.isEnabled()); // see question above
//        
//            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
//            this.setBorder(BorderFactory.createLineBorder(Color.black));
//
//            return this;
//        }
//    }
    
}
