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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;

import org.simbrain.network.groups.Group;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.UpdateAction;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.genericframe.GenericFrame;

/**
 * (Prototype) Panel for display and ordering of network update actions.
 *
 * TODO
 *   - Listen for relevant changes?
 *   - Show numbers for list
 *   - Add button
 *   - Clean up layout
 */
public class UpdateManagerPanel extends JPanel {

    /** List of updatable actions. */
    private JList actionList = new JList();

    /** Instance of parent frame. */
    private GenericFrame parentFrame;

    /** Reference to root network. */
    private final RootNetwork network;
    
    /**
     * Creates a new action list panel.
     */
    public UpdateManagerPanel(RootNetwork network) {

        super(new BorderLayout());
        this.network = network;

        // // Listens for frame closing for removal of listener.
        // couplingFrame.addWindowListener(new WindowAdapter() {
        // public void windowClosing(final WindowEvent w) {
        // desktop.getWorkspace().getCouplingManager().removeCouplingListener(UpdateManagerPanel.this);
        // }
        // });
        // desktop.getWorkspace().getCouplingManager().addCouplingListener(this);

        // Populates the action list with data.
        final DefaultListModel model = new DefaultListModel();
        for (UpdateAction action : network.getUpdateManager().getActionList()) {
            model.addElement(action);
        }
        actionList.setModel(model);
        actionList.setDragEnabled(true);
        actionList.setTransferHandler(createTransferHandler());
        actionList.setDropMode(DropMode.INSERT);

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
//        final JComboBox addActionsBox = new JComboBox();
//        addActionsBox.addItem("Update Root Network (buffered)");
//        addActionsBox.addItem("Update Loose Neurons");
//        addActionsBox.addItem("Update Neurons");
//        addActionsBox.addItem("Update Synapses");
//        addActionsBox.addItem("Update Groups");
//        addActionsBox.addItem("Group...");
//        addActionsBox.addItem("Neuron...");
//        addActionsBox.addItem("Synapse...");
//        addActionsBox.addItem("Script...");
//        buttonPanel.add(addActionsBox);
//        addActionsBox.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent arg0) {
//                model.add(model.size(), addActionsBox.getSelectedItem());
//            }
//        });
        
        // Below not needed because it is currently displayed in a modal dialog
        
//
//        // TODO: When disposed remove listener.
//        // Update panel when groups added.  TODO: Feels a bit indirect.
//        //  Should listen for changes in the update
//        network.addGroupListener(new GroupListener() {
//
//            public void groupAdded(NetworkEvent<Group> e) {
//                repaint();
//            }
//
//            public void groupRemoved(NetworkEvent<Group> e) {
//                repaint();
//            }
//
//            public void groupChanged(NetworkEvent<Group> networkEvent,
//                    String changeDescription) {
//            }
//
//            public void groupParameterChanged(NetworkEvent<Group> networkEvent) {
//            }
//            
//        });
//        

        // Add scroll pane to JPanel
        add(listScroll, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Set the frame.
     */
    private void setFrame(final GenericFrame frame) {
        parentFrame = frame;
    }
    
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
                DefaultListModel listModel = (DefaultListModel) actionList
                        .getModel();
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
                if (index < listModel.size()) {
                    listModel.removeElement(data);
                    listModel.add(index, data);
                } else {
                    listModel.removeElement(data);
                    listModel.addElement(data);
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
            DefaultListModel listModel = (DefaultListModel) actionList
                    .getModel();
            // TODO: This should directly modify the update manager, which should fire
            //      an event which this listens to
            for (int i = 0; i < actionList.getSelectedIndices().length; i++) {
                listModel.remove(actionList.getSelectedIndices()[i]);
            }
        }
    };

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
