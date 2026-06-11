package org.simbrain.world.odorworld.dialogs;

import org.simbrain.util.CmdOrCtrl;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.SwingUtilsKt;
import org.simbrain.util.Theme;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.PeripheralAttribute;
import org.simbrain.world.odorworld.sensors.Sensor;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel showing an agent's sensors or effectors.
 *
 * @author Jeff Yoshimi
 * @author Lam Nguyen
 */
public class SensorEffectorPanel extends JPanel {

    /**
     * Table representing sensors / effectors.
     */
    private final JTable table;

    /**
     * Table model.
     */
    private final AttributeModel model;

    /**
     * Whether this is a sensor or effector panel.
     */
    public enum PanelType {Sensor, Effector}

    /**
     * Initial sensor or effector to edit, if any.  Also used to determine the type for this panel.
     */
    private final PanelType type;

    /**
     * Currently selected attribute
     */
    private PeripheralAttribute selectedAttribute;

    /**
     * Parent entity.
     */
    private final OdorWorldEntity parentEntity;

    /**
     * Parent window.
     */
    private final Window parentWindow;

    /**
     * Construct the SensorEffectorPanel.
     */
    public SensorEffectorPanel(OdorWorldEntity parentEntity, final PanelType type, final Window parentWindow) {

        this.parentEntity = parentEntity;
        this.type = type;
        this.parentWindow = parentWindow;

        model = new AttributeModel();
        table = new JTable(model);
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setGridColor(Theme.getDivider());
        table.setFocusable(false);

        // Context menu
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(final MouseEvent e) {
                if (e.isControlDown() || (e.getButton() == 3)) {
                    final int row = table.rowAtPoint(e.getPoint());
                    table.setRowSelectionInterval(row, row);
                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenuItem menuItem = new JMenuItem("Edit...");
                    popupMenu.add(menuItem);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    final PeripheralAttribute attribute = model.getAttribute(row);
                    menuItem.addMouseListener(new MouseAdapter() {
                        public void mouseReleased(MouseEvent e) {
                            editAttribute(attribute);
                        }
                    });
                    popupMenu.add(menuItem);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    final int row = table.rowAtPoint(e.getPoint());
                    table.setRowSelectionInterval(row, row);
                    final PeripheralAttribute sensor = model.getAttribute(row);
                    editAttribute(sensor);
                }
            }
        });

        // Set selected item
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (table.getSelectedRow() >= 0) {
                    selectedAttribute = model.getAttribute(table.getSelectedRow());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        JPanel buttonBar = new JPanel();

        // Add attribute
        JButton addAttribute = new JButton("Add", ResourceManager.getSmallIcon("menu_icons/plus.png"));
        addAttribute.setToolTipText("Add...");
        buttonBar.add(addAttribute);
        addAttribute.addActionListener(e -> {
            StandardDialog dialog;
            if (type == PanelType.Sensor) {
                dialog = new AddSensorDialog(parentEntity, parentWindow);
            } else {
                dialog = new AddEffectorDialog(parentEntity, parentWindow);
            }
            dialog.setModal(true);
            dialog.makeVisible();
        });

        // Delete attribute
        JButton deleteAttribute = new JButton();
        deleteAttribute.setToolTipText("Delete...");
        buttonBar.add(deleteAttribute);
        DeleteItems deleteAction = new DeleteItems();
        deleteAttribute.setAction(deleteAction);

        // Add a key command to the delete action
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), "delete");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), "delete");
        getActionMap().put("delete", deleteAction);

        // Edit attribute
        JButton editAttribute = new JButton("Edit", ResourceManager.getSmallIcon("menu_icons/Properties.png"));
        editAttribute.setToolTipText("Edit...");
        buttonBar.add(editAttribute);
        editAttribute.addActionListener(e -> editAttribute(selectedAttribute));

        // Cmd-Ctrl A to select all items
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                (KeyStroke.getKeyStroke('A', CmdOrCtrl.INSTANCE.getKeyCode())), "selectAll");
        getActionMap().put("selectAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                table.selectAll();
            }
        });


        // Final GUI setup
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, scrollPane);
        add(BorderLayout.SOUTH, buttonBar);

        // Populate table
        if (type == PanelType.Sensor) {
            for (Sensor sensor : parentEntity.getSensors()) {
                model.addAttribute(sensor);
            }
        } else {
            for (Effector effector : parentEntity.getEffectors()) {
                model.addAttribute(effector);
            }
        }

        // Set up event listeners
        if (type == PanelType.Sensor) {
            parentEntity.getEvents().getSensorAdded().on(s -> model.addAttribute(s));
            parentEntity.getEvents().getSensorRemoved().on(s -> model.removeAttribute(s));
        } else {
            parentEntity.getEvents().getEffectorAdded().on(e -> model.addAttribute(e));
            parentEntity.getEvents().getEffectorRemoved().on(e -> model.removeAttribute(e));
        }

    }

    class DeleteItems extends AbstractAction {

        public DeleteItems() {
            super("Delete", ResourceManager.getSmallIcon("menu_icons/minus.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selectedRows = table.getSelectedRows();
            List<PeripheralAttribute> toDelete = new ArrayList();
            for (int i = 0; i < selectedRows.length; i++) {
                toDelete.add(model.getAttribute(selectedRows[i]));
            }
            for (PeripheralAttribute attribute : toDelete) {
                if (attribute != null) {
                    if (attribute instanceof Sensor) {
                        parentEntity.removeSensor((Sensor) attribute);

                    } else {
                        parentEntity.removeEffector((Effector) attribute);
                    }
                }
            }
        }
    }


    /**
     * Edit an attribute.
     */
    private void editAttribute(PeripheralAttribute attribute) {
        // Panel is null when no item is selected on opening.
        // TODO: Disable the edit button in this case.
        if (attribute == null) {
            return;
        }

        StandardDialog dialog = SwingUtilsKt.createEditorDialog(attribute, "Edit " + attribute.getName(), parentWindow, (obj) -> {
            attribute.getEvents().getPropertyChanged().fire();
            return null;
        });
        dialog.setModal(true);
        dialog.makeVisible();
    }

    /**
     * Table model which represents sensors and effectors.
     */
    class AttributeModel extends AbstractTableModel {

        /**
         * Column names.
         */
        String[] columnNames = {"Id", "Label", "Type"};

        /**
         * Internal list of components.
         */
        private final List<PeripheralAttribute> data = new ArrayList();

        /**
         * Helper method to get a reference to the attribute displayed in a row.
         *
         * @param row the row index
         * @return the attribute displayed in that row.
         */
        public PeripheralAttribute getAttribute(int row) {
            if (row < data.size()) {
                return data.get(row);
            } else {
                return null;
            }
        }

        /**
         * Remove an attribute from the table representation.
         *
         * @param attribute the attribute to remove
         */
        public void removeAttribute(PeripheralAttribute attribute) {
            data.remove(attribute);
            fireTableDataChanged();
        }

        /**
         * Add an attribute
         *
         * @param attribute the attribute to add
         */
        public void addAttribute(PeripheralAttribute attribute) {
            data.add(attribute);
            model.fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            switch (col) {
                case 0:
                    return data.get(row).getId();
                case 1:
                    return data.get(row).getLabel();
                case 2:
                    return data.get(row).getName();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            switch (col) {
                case 0:
                    return;
                case 1:
                    data.get(row).setLabel((String) value);
                    return;
                case 2:
                    return;
            }
            this.fireTableDataChanged();
        }

        //        @Override
        //        public boolean isCellEditable(int row, int col) {
        //            switch (col) {
        //                case 0:
        //                    return false;
        //                case 1:
        //                    return true;
        //                case 2:
        //                    return false;
        //                default:
        //                    return false;
        //            }
        //        }

        @Override
        public Class getColumnClass(int col) {
            switch (col) {
                case 0:
                    return String.class;
                case 1:
                    return String.class;
                case 2:
                    return String.class;
                default:
                    return null;
            }
        }

    }

}
