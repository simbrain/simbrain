/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.simbrain.util.StandardDialog;
import org.simbrain.world.Agent;
import org.simbrain.world.World;
import org.simnet.coupling.CouplingMenuItem;
import org.simnet.coupling.MotorCoupling;
import org.simnet.coupling.SensoryCoupling;


/**
 * <b>DataWorld</b> creates a table and then adds it to the viewport.
 *
 * @author rbartley
 */
public class DataWorld extends World implements MouseListener, Agent, KeyListener {

    /** Edit buttons boolean. */
    public static boolean editButtons = false;

    /** Table model. */
    private TableModel model = new TableModel(this);

    /** Data table. */
    private JTable table = new JTable(model);

    /** Parent frame that calls world. */
    private DataWorldFrame parentFrame;

    /** Button renderer/editor composite. */
    private ButtonEditor buttonEditor;

    /** Upper bound. */
    private int upperBound = 0;

    /** Lower bound. */
    private int lowerBound = 0;

    /** Current row. */
    private int currentRow = 0;

    /** Iteration mode. */
    private boolean iterationMode = false;

    /** Use last column for iteration. */
    private boolean columnIteration = false;

    /** Name. */
    private String name;

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

    /**
     * Creates a new instance of the data world.
     *
     * @param ws World frame to create a new data world within
     */
    public DataWorld(final DataWorldFrame ws) {
        super(new BorderLayout());
        setParentFrame(ws);

        buttonEditor = new ButtonEditor(new AbstractAction()
            {
                /** @see ActionListener */
                public void actionPerformed(final ActionEvent event) {
                    int selectedIndex = table.getSelectionModel().getMinSelectionIndex();
                    currentRow = (selectedIndex == -1) ? currentRow : selectedIndex;
                    fireWorldChanged();
                }
            });

        addRow.addActionListener(parentFrame);
        addRow.setActionCommand("addRowHere");
        addCol.addActionListener(parentFrame);
        addCol.setActionCommand("addColHere");
        remRow.addActionListener(parentFrame);
        remRow.setActionCommand("remRowHere");
        remCol.addActionListener(parentFrame);
        remCol.setActionCommand("remColHere");

        add("Center", table);
        init();
    }

    /**
     * Add listeners.
     */
    private void init() {

        table.addKeyListener(this);
        table.addMouseListener(this);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setCellEditor(buttonEditor);
        table.getColumnModel().getColumn(0).setCellRenderer(buttonEditor);
        table.getModel().addTableModelListener(new TableModelListener()
            {
                /** @see TableModelListener */
                public void tableChanged(final TableModelEvent e) {
                    // heavy-handed way of dealing with column add/removes
                    table.getColumnModel().getColumn(0).setCellEditor(buttonEditor);
                    table.getColumnModel().getColumn(0).setCellRenderer(buttonEditor);
                }
            });

    }

    /**
     * Resets the model.
     *
     * @param data Data to reset
     */
    public void resetModel(final String[][] data) {
        model = new TableModel(data);
        table.setModel(model);
        init();
        repaint();
        parentFrame.pack();
        currentRow = 0;
        currentRowCounter = 0;
    }

    /**
     * Responds to mouse clicked event.
     *
     * @param e Mouse event
     */
    public void mouseClicked(final MouseEvent e) {
    }

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
            menu.show(this, (int) selectedPoint.getX(), (int) selectedPoint.getY());
        }
    }

    /**
     * Responds to mouse released event.
     *
     * @param e Mouse event
     */
    public void mouseReleased(final MouseEvent e) {
    }

    /**
     * Responds to mouse entered events.
     *
     * @param e Mouse event
     */
    public void mouseEntered(final MouseEvent e) {
    }

    /**
     * Responds to mouse exited event.
     *
     * @param e Mouse event
     */
    public void mouseExited(final MouseEvent e) {
    }

    /**
     * @return The pop up menu to be built.
     */
    public JPopupMenu buildPopupMenu() {
        JPopupMenu ret = new JPopupMenu();

        ret.add(addRow);

        if (this.getTable().columnAtPoint(selectedPoint) != 0) {
            ret.add(addCol);
        }

        ret.add(remRow);

        if (this.getTable().columnAtPoint(selectedPoint) != 0) {
            ret.add(remCol);
        }

        return ret;
    }

    /**
     * @return World type.
     */
    public String getType() {
        return "DataWorld";
    }

    /**
     * @return Name of data world.
     */
    public String getName() {
        return this.getParentFrame().getTitle();
    }

    /**
     * @return Returns the parentFrame.
     */
    public DataWorldFrame getParentFrame() {
        return parentFrame;
    }

    /**
     * @param parentFrame The parentFrame to set.
     */
    public void setParentFrame(final DataWorldFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    /**
     * @return Returns the table.
     */
    public JTable getTable() {
        return table;
    }

    /**
     * @param table The table to set.
     */
    public void setTable(final JTable table) {
        this.table = table;
    }

    /**
     * Dataworlds contain one agent, themselves.
     *
     * @return Returns the agentList.
     */
    public ArrayList getAgentList() {
        ArrayList ret = new ArrayList();
        ret.add(this);

        return ret;
    }

    /**
     * Dataworlds are agents, hence this returns itself.
     *
     * @return Returns the world this agent is associated with, itself
     */
    public World getParentWorld() {
        return this;
    }

    /**
     * Randomizes the values.
     *
     */
    public void randomize() {
        if (upperBound <= lowerBound) {
            displayRandomizeDialog();
        }

        for (int i = 1; i < table.getColumnCount(); i++) {
            for (int j = 0; j < table.getRowCount(); j++) {
                table.setValueAt(randomInteger(), j, i);
            }
        }
    }

    /**
     * @return A random integer.
     */
    public Double randomInteger() {
        if (upperBound >= lowerBound) {
            double drand = Math.random();
            drand = (drand * (upperBound - lowerBound)) + lowerBound;

            Double element = new Double(drand);

            return element;
        }

        return new Double(0);
    }

    /**
     * Displays the randomize dialog.
     */
    public void displayRandomizeDialog() {
        StandardDialog rand = new StandardDialog(this.getParentFrame().getWorkspace(), "randomize Bounds");
        JPanel pane = new JPanel();
        JTextField lower = new JTextField();
        JTextField upper = new JTextField();
        lower.setText(Integer.toString(getLowerBound()));
        lower.setColumns(3);
        upper.setText(Integer.toString(getUpperBound()));
        upper.setColumns(3);
        pane.add(new JLabel("Lower Bound"));
        pane.add(lower);
        pane.add(new JLabel("Upper Bound"));
        pane.add(upper);

        rand.setContentPane(pane);

        rand.pack();

        rand.setLocationRelativeTo(getParentFrame());
        rand.setVisible(true);

        if (!rand.hasUserCancelled()) {
            setLowerBound(Integer.parseInt(lower.getText()));
            setUpperBound(Integer.parseInt(upper.getText()));
        }

        repaint();
    }

    /**
     * Unused stub; data worlds don't receive commands.
     *
     * @param commandList List of commands
     * @param value Value
     */
    public void setMotorCommand(final String[] commandList, final double value) {
        int col = Integer.parseInt(commandList[0]);

        table.setValueAt(new Double(value), currentRow, col);
    }

    /**
     * Unused stub; data worlds don't receive commands.
     *
     * @param al Action listener
     */
    public JMenu getMotorCommandMenu(final ActionListener al) {
        JMenu ret = new JMenu("" + this.getWorldName());

        for (int i = 1; i < table.getColumnCount(); i++) {
            CouplingMenuItem motorItem = new CouplingMenuItem("Column " + i,
                                                              new MotorCoupling(this, new String[] {"" + i }));
            motorItem.addActionListener(al);
            ret.add(motorItem);
        }

        return ret;
    }

    /**
     * Returns the value in the given column of the table uses the current row.
     *
     * @param sensorId Sensor identification
     */
    public double getStimulus(final String[] sensorId) {
        int i = Integer.parseInt(sensorId[0]);

        // If invalid requeset return 0
        if (i >= table.getModel().getColumnCount()) {
            return 0;
        }
        return Double.parseDouble("" + table.getModel().getValueAt(currentRow, i));
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
                    + table.getModel().getValueAt(currentRow,
                            table.getModel().getColumnCount() - 1));
        }
    }

    /**
     * Increment current row by 1.
     */
    public void incrementCurrentRow() {
        if (iterationMode) {
            if (currentRow >= (table.getRowCount() - 1)) {
                currentRow = 0;
            } else {
                currentRow++;
            }
            table.setRowSelectionInterval(currentRow, currentRow);
        }
    }

    /**
     * Returns a menu with on id, "Column X" for each column.
     *
     * @param al Action listener
     */
    public JMenu getSensorIdMenu(final ActionListener al) {
        JMenu ret = new JMenu("" + this.getWorldName());

        for (int i = 1; i < (table.getColumnCount()); i++) {
            CouplingMenuItem stimItem = new CouplingMenuItem("Column " + i,
                                                             new SensoryCoupling(this, new String[] {"" + i }));
            stimItem.addActionListener(al);
            ret.add(stimItem);
        }

        return ret;
    }
    /**
     * @return Returns the name.
     */
    public String getWorldName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setWorldName(final String name) {
        this.getParentFrame().setTitle(name);
        this.name = name;
    }

    /**
     * @return Returns the model.
     */
    public TableModel getModel() {
        return model;
    }

    /**
     * @param model The model to set.
     */
    public void setModel(final TableModel model) {
        this.model = model;
    }

    /**
     * @return The lower bound.
     */
    public int getLowerBound() {
        return lowerBound;
    }

    /**
     * Sets the lower bound value.
     *
     * @param lowerBound Value to set
     */
    public void setLowerBound(final int lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * @return The upper bound value.
     */
    public int getUpperBound() {
        return upperBound;
    }

    /**
     * Sets the upper bound value.
     *
     * @param upperBound Value to set
     */
    public void setUpperBound(final int upperBound) {
        this.upperBound = upperBound;
    }

    /**
     * @return The current row value.
     */
    public int getCurrentRow() {
        return currentRow;
    }

    /**
     * Sets the current row.
     *
     * @param currentRow Value to set
     */
    public void setCurrentRow(final int currentRow) {
        this.currentRow = currentRow;
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

    /**
     * Responds to key typed events.
     *
     * @param arg0 Key event
     */
    public void keyTyped(final KeyEvent arg0) {
        this.getParentFrame().setChangedSinceLastSave(true);
    }

    /**
     * Responds to key pressed events.
     *
     * @param arg0 Key event
     */
    public void keyPressed(final KeyEvent arg0) {
    }

    /**
     * Responds to key released events.
     *
     * @param arg0 Key event
     */
    public void keyReleased(final KeyEvent arg0) {
    }


    /**
     * Button renderer/editor composite.
     */
    private class ButtonEditor extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
        /** Renderer button. */
        private JButton renderButton;

        /** Editor button. */
        private JButton editButton;

        /** Cached text. */
        private String text;


        /**
         * Create a new button renderer/editor composite with the specified action.
         *
         * @param action action
         */
        public ButtonEditor(final Action action) {
            renderButton = new JButton(action);
            editButton = new JButton(action);

            editButton.addActionListener(new ActionListener()
                {
                    /** @see ActionListener */
                    public void actionPerformed(final ActionEvent event) {
                        fireEditingStopped();
                    }
                });
        }


        /** @see TableCellRenderer */
        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row,
                                                       final int column) {
            if (isSelected) {
                renderButton.setForeground(table.getSelectionForeground());
                renderButton.setBackground(table.getSelectionBackground());
            } else {
                renderButton.setForeground(table.getForeground());
                renderButton.setBackground(UIManager.getColor("Button.background"));
            }

            renderButton.setText((value == null) ? "" : value.toString());
            return renderButton;
        }

        /** @see TableCellEditor */
        public Component getTableCellEditorComponent(final JTable table,
                                                     final Object value,
                                                     final boolean isSelected,
                                                     final int row,
                                                     final int column)  {
            text = (value == null) ? "" : value.toString();
            editButton.setText(text);
            return editButton;
        }

        /** @see TableCellEditor */
        public Object getCellEditorValue() {
            return text;
        }
    }


    /** @see Agent */
    public void completedInputRound() {
        if (iterationMode) {
            if (columnIteration) {
                incrementUsingLastColumn();
            } else {
                incrementCurrentRow();
            }
        }
    }

    /**
     * @return Returns the iterationMode.
     */
    public boolean isIterationMode() {
        return iterationMode;
    }

    /**
     * @param iterationMode The iterationMode to set.
     */
    public void setIterationMode(final boolean iterationMode) {
        this.iterationMode = iterationMode;
    }

    /**
     * @return Returns the columnIteration.
     */
    public boolean getColumnIteration() {
        return columnIteration;
    }

    /**
     * @param columnIteration The columnIteration to set.
     */
    public void setColumnIteration(final boolean columnIteration) {
        this.columnIteration = columnIteration;
    }
}
