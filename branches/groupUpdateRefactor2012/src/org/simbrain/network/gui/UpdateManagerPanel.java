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
package org.simbrain.network.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.TransferHandler;
import javax.swing.border.LineBorder;

import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.UpdateAction;
import org.simbrain.network.interfaces.UpdateManager.UpdateManagerListener;
import org.simbrain.resource.ResourceManager;

/**
 * (Prototype) Panel for display and ordering of network update actions.
 *
 * TODO
 *   - Show numbers for list
 *   - Add button
 *   - Clean up layout
 */
public class UpdateManagerPanel extends JPanel {

    /** The JList which represents actions. */
    private JList actionList = new JList();
    
    /** The JList model object. */
    final DefaultListModel model  = new DefaultListModel();
    
    /** List of actions that can be added. */
    final JComboBox potentialActionsComboBox = new JComboBox();

    /** Reference to root network. */
    private final RootNetwork network;
    
    /**
     * Creates a new action list panel.
     */
    public UpdateManagerPanel(final RootNetwork network) {

        super(new BorderLayout());
        this.network = network;

        // Set up the list
        actionList.setModel(model);
        actionList.setDragEnabled(true);
        actionList.setTransferHandler(createTransferHandler());
        actionList.setDropMode(DropMode.INSERT);
        updateList();
        configureJList();

        // Scroll pane for showing lists larger than viewing window and setting
        // maximum size
        final JScrollPane listScroll = new JScrollPane(actionList);
        listScroll
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        listScroll
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Allows the user to delete couplings within the list frame.
        JPanel buttonPanel = new JPanel();
        JButton deleteActionsButton = new JButton(deleteActionsAction);
        
        buttonPanel.add(deleteActionsButton);
        buttonPanel.add(potentialActionsComboBox);
        updatePotentialActionList();
        potentialActionsComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                network.getUpdateManager().addAction(
                        (UpdateAction) potentialActionsComboBox
                                .getSelectedItem());
            }
        });
        

        // Add scroll pane to JPanel
        add(listScroll, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Listen for network updates
        network.getUpdateManager().addListener(listener);
        
        // TODO: Handle closing event
        // Should remove listener from update manager when this is closed
                
    }
    
    /**
     * Configure the JList.
     */
    private void configureJList() {
        actionList.setCellRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                    Object updateAction, int index, boolean isSelected,
                    boolean cellHasFocus) {
                
                JLabel label = new JLabel((index + 1) + ": "
                        + ((UpdateAction) updateAction).getDescription());
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                } else {
                    label.setForeground(list.getForeground());
                    label.setBackground(list.getBackground());
                }
                label.setEnabled(list.isEnabled());
                label.setFont(list.getFont());
                label.setOpaque(true);
                return label;
            }
        });
    }
    
    
    /** Action which deletes selected actions. */
    Action deleteActionsAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Eraser.png"));
            putValue(NAME, "Delete actions");
            putValue(SHORT_DESCRIPTION, "Delete selected actions");
            UpdateManagerPanel.this.getInputMap(
                    JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke("BACK_SPACE"), this);
            UpdateManagerPanel.this.getInputMap(
                    JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke("DELETE"), this);
            UpdateManagerPanel.this.getActionMap().put(this, this);
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            for (Object action : actionList.getSelectedValues()) {
                network.getUpdateManager().removeAction((UpdateAction) action);
            }
        }
    };

    
    /**
     * Update the JList's model.
     */
    private void updateList() {
        model.clear();
        for (UpdateAction action : network.getUpdateManager().getActionList()) {
            model.addElement(action);
        }
        repaint();
    }
    
    /**
     * Update the combo box showing potential actions.
     */
    private void updatePotentialActionList() {
        potentialActionsComboBox.removeAllItems();
        for (UpdateAction action : network.getUpdateManager().getPotentialActionList()) {
            potentialActionsComboBox.addItem(action);
        }
        repaint();
    }
    
    /**
     * Listener for update manager changes.
     */
    private UpdateManagerListener listener = new UpdateManagerListener() {

        public void actionAdded(UpdateAction action) {
            updateList();
        }

        public void actionRemoved(UpdateAction action) {
            updateList();
        }

        public void actionOrderChanged() {
            updateList();
        }

        public void potentialActionAdded(UpdateAction action) {
            updatePotentialActionList();
        }

        public void potentialActionRemoved(UpdateAction action) {
            updatePotentialActionList();
        }
        
    };

    /**
     * Handle drag and drop events
     * @return
     */
    private TransferHandler createTransferHandler() {
        return new TransferHandler() {

            public boolean canImport(TransferHandler.TransferSupport info) {
                // we only import Strings
                if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    return false;
                }

                JList.DropLocation dl = (JList.DropLocation) info
                        .getDropLocation();
                if (dl.getIndex() == -1) {
                    return false;
                }
                return true;
            }

            public boolean importData(TransferHandler.TransferSupport info) {
                // if (!info.isDrop()) {
                // return false;
                // }

                JList.DropLocation dl = (JList.DropLocation) info
                        .getDropLocation();
                int index = dl.getIndex();

                // Get the string that is being dropped.
                Transferable t = info.getTransferable();
                String data;
                try {
                    data = (String) t.getTransferData(DataFlavor.stringFlavor);
                } catch (Exception e) {
                    System.out.println("NO!!!");
                    return false;
                }

                // Perform the actual import.
                if (index < model.size()) {
                    model.removeElement(data);
                    model.add(index, data);
                } else {
                    model.removeElement(data);
                    model.addElement(data);
                }
                return true;
            }

            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }

            protected Transferable createTransferable(JComponent c) {
                Object[] values = actionList.getSelectedValues();

                StringBuffer buff = new StringBuffer();

                for (int i = 0; i < values.length; i++) {
                    Object val = values[i];
                    buff.append(val == null ? "" : val.toString());
                    if (i != values.length - 1) {
                        buff.append("\n");
                    }
                }
                return new StringSelection(buff.toString());
            }

        };
    }


//    /**
//     * Test panel.
//     */
//    public static void main(String[] args) {
//        JFrame frame = new JFrame();
//        List<String> actions = new ArrayList<String>();
//        actions.add("Buffered Update");
//        actions.add("Group 1");
//        actions.add("Group 2");
//        actions.add("Neuron 1");
//        UpdateManagerPanel panel = new UpdateManagerPanel(frame, actions);
//        frame.setContentPane(panel);
//        frame.setVisible(true);
//        frame.pack();
//    }

}
