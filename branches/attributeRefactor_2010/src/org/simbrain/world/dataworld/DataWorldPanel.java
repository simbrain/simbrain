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

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXTable;
import org.simbrain.util.StandardDialog;

/**
 * <b>DataWorldPanel</b> is a jpanel which contains a table object and a that
 * table's model object.
 * 
 * @author jyoshimi
 */
public class DataWorldPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Data table. */
    private JXTable table;

    /** Underlying data. */
    private final DataTableModel tableModel;

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

    /** Grid Color. */
    private Color gridColor =  Color.LIGHT_GRAY;

    /**
     * Creates a new instance of the data world.
     * @param dataModel 
     *
     * @param ws World frame to create a new data world within
     */
    public DataWorldPanel(DataModel<Double> dataModel) {
        super(new BorderLayout());
        this.tableModel= new DataTableModel(dataModel);

        table = new JXTable(tableModel);
        add(table, BorderLayout.CENTER);
        add(table.getTableHeader(), BorderLayout.NORTH);

        table.addKeyListener(keyListener);
        table.addMouseListener(mouseListener);
        table.setColumnSelectionAllowed(true);
        table.setRolloverEnabled(true);
        table.setRowSelectionAllowed(true);
        table.setGridColor(gridColor);
        updateRowSelection();

        addRow.addActionListener(addRowHereListener);
        addCol.addActionListener(addColHereListener);

        remRow.addActionListener(remRowHereListener);
        remCol.addActionListener(remColHereListener);

   }

    private ActionListener addRowHereListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            if (getSelectedPoint().x < (table.getRowHeight() * table
                    .getRowCount())) {
                tableModel.getModel().insertNewRow(getSelectedRow(),
                        new Double(0));
            } else {
                tableModel.getModel().addNewRow(new Double(0));
            }
        }
    };

    private ActionListener addColHereListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            tableModel.getModel().insertNewColumn(getSelectedColumn(),
                    new Double(0));
        }
    };

    private ActionListener remRowHereListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            tableModel.getModel().removeRow(getSelectedRow());
        }
    };

    private ActionListener remColHereListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            tableModel.getModel().removeColumn(getSelectedColumn());
        }
    };

    /**
     * Returns the currently selected column.
     *
     * @return the currently selected column
     */
    public int getSelectedColumn() {
        return table.columnAtPoint(selectedPoint);
    }

    /**
     * Returns the currently selected row.
     *
     * @return the currently selected row
     */
    public int getSelectedRow() {
        return table.rowAtPoint(selectedPoint);
    }

    private MouseListener mouseListener = new MouseAdapter() {
        /**
         * Responds to mouse pressed event.
         *
         * @param e Mouse event
         */
        public void mousePressed(final MouseEvent e) {
            selectedPoint = e.getPoint();
            tableModel.getModel().setCurrentRow(getSelectedRow());
            // TODO: should use isPopupTrigger, see e.g. ContextMenuEventHandler
            boolean isRightClick = (e.isControlDown() || (e.getButton() == 3));
            if (isRightClick) {
                JPopupMenu menu = buildPopupMenu();
                menu.show(DataWorldPanel.this, (int) selectedPoint.getX(),
                        (int) selectedPoint.getY());
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
//        JMenu producerMenu = new ProducingAttributeMenu(
//                "Receive coupling from", component.getWorkspace(), component
//                        .getConsumingAttributes().get(getSelectedColumn()));
//        ret.add(producerMenu);
//        JMenu consumerMenu = new ConsumingAttributeMenu("Send coupling to",
//                component.getWorkspace(), component.getProducingAttributes()
//                        .get(getSelectedColumn()));
//        ret.add(consumerMenu);

        return ret;
    }

    /**
     * Displays the randomize dialog.
     */
    public void displayRandomizeDialog() {
        StandardDialog rand = new StandardDialog();
        JPanel pane = new JPanel();
        JTextField lower = new JTextField();
        JTextField upper = new JTextField();
        lower.setText(Integer.toString(tableModel.getModel().getLowerBound()));
        lower.setColumns(3);
        upper.setText(Integer.toString(tableModel.getModel().getUpperBound()));
        upper.setColumns(3);
        pane.add(new JLabel("Lower Bound"));
        pane.add(lower);
        pane.add(new JLabel("Upper Bound"));
        pane.add(upper);

        rand.setContentPane(pane);
        rand.pack();
        rand.setLocationRelativeTo(this);
        rand.setVisible(true);
        if (!rand.hasUserCancelled()) {
            tableModel.getModel().setLowerBound(Integer.parseInt(lower.getText()));
            tableModel.getModel().setUpperBound(Integer.parseInt(upper.getText()));
        }

        repaint();
    }

    /**
     * Select current row.
     */
    public void updateRowSelection() {
        table.selectAll(); // TODO: If I don't call this, the line below does
                           // not work. Not sure why.
        table.setRowSelectionInterval(tableModel.getModel().getCurrentRow(), tableModel.getModel().getCurrentRow());
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
            System.out.println("Key typed");
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
