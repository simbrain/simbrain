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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.NetworkUpdateManager.Listener;
import org.simbrain.network.update_actions.CustomUpdate;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.scripteditor.ScriptEditor;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Panel for display the current network updates, and editing them (adding,
 * removing, and changing their order).
 *
 * @author Jeff Yoshimi
 */
public class NetworkUpdateManagerPanel extends JPanel {

    /**
     * The JList which represents current actions.
     */
    private final JList currentActionJList = new JList();

    /**
     * The model object for current actions.
     */
    private final DefaultListModel currentActionListModel = new DefaultListModel();

    /**
     * Reference to root network.
     */
    private final Network network;

    /**
     * Script directory for custom workspace updates.
     */
    private static final String SCRIPT_DIR = "." + System.getProperty("file.separator") + "scripts" + System.getProperty("file.separator") + "updateScripts" + System.getProperty("file.separator") + "networkUpdate";

    /**
     * Creates a new update manager panel.
     *
     * @param network
     * @param parentDialog
     */
    public NetworkUpdateManagerPanel(final Network network, final StandardDialog parentDialog) {

        super(new BorderLayout());
        this.network = network;

        // Set up the action list
        currentActionJList.setModel(currentActionListModel);
        configureActionList();
        currentActionJList.setDragEnabled(true);
        currentActionJList.setTransferHandler(createTransferHandler());
        currentActionJList.setDropMode(DropMode.INSERT);
        JScrollPane currentListScroll = new JScrollPane(currentActionJList);
        currentListScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        currentListScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        currentListScroll.setBorder(BorderFactory.createTitledBorder("Current Update Sequence"));
        updateCurrentActionsList();
        add(currentListScroll, BorderLayout.CENTER);

        // Add buttons
        JPanel buttonPanel = new JPanel();
        JButton addActionsButton = new JButton(addPresetAction);
        buttonPanel.add(addActionsButton);
        JButton customActionButton = new JButton(addCustomAction);
        buttonPanel.add(customActionButton);
        JButton deleteActionsButton = new JButton(deleteActionsAction);
        buttonPanel.add(deleteActionsButton);
        // TODO: Make movement actions apply to multiple selections
        JButton upButton = new JButton(ResourceManager.getImageIcon("menu_icons/Up.png"));
        upButton.setToolTipText("Move selected action up in sequence");
        upButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int moveMe = currentActionJList.getSelectedIndex();
                if (moveMe != 0) {
                    swap(moveMe, moveMe - 1);
                    currentActionJList.setSelectedIndex(moveMe - 1);
                    currentActionJList.ensureIndexIsVisible(moveMe - 1);
                }
            }
        });
        buttonPanel.add(upButton);
        JButton upFullButton = new JButton(ResourceManager.getImageIcon("menu_icons/UpFull.png"));
        upFullButton.setToolTipText("Move selected action to top of sequence");
        upFullButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int moveMe = currentActionJList.getSelectedIndex();
                if (moveMe != 0) {
                    swap(moveMe, 0);
                    currentActionJList.setSelectedIndex(0);
                    currentActionJList.ensureIndexIsVisible(0);
                }
            }
        });
        buttonPanel.add(upFullButton);
        JButton downButton = new JButton(ResourceManager.getImageIcon("menu_icons/Down.png"));
        downButton.setToolTipText("Move selected action down in sequence");
        downButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int moveMe = currentActionJList.getSelectedIndex();
                if (moveMe != currentActionListModel.getSize() - 1) {
                    swap(moveMe, moveMe + 1);
                    currentActionJList.setSelectedIndex(moveMe + 1);
                    currentActionJList.ensureIndexIsVisible(moveMe + 1);
                }
            }
        });
        buttonPanel.add(downButton);
        JButton downFullButton = new JButton(ResourceManager.getImageIcon("menu_icons/DownFull.png"));
        downFullButton.setToolTipText("Move selected action to bottom of sequence");
        downFullButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int moveMe = currentActionJList.getSelectedIndex();
                int lastIndex = currentActionListModel.getSize() - 1;
                if (moveMe != lastIndex) {
                    swap(moveMe, lastIndex);
                    currentActionJList.setSelectedIndex(lastIndex);
                    currentActionJList.ensureIndexIsVisible(lastIndex);
                }
            }
        });
        buttonPanel.add(downFullButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Help button
        Action helpAction = new ShowHelpAction("Pages/Network/update.html");
        parentDialog.addButton(new JButton(helpAction));

        // Listen for network updates
        network.getUpdateManager().addListener(listener);

        // TODO: Handle closing event
        // Should remove listener from update manager when this is closed

    }

    /**
     * Swap two elements int the current action list.
     *
     * @param a index for item a
     * @param b index for item b
     */
    private void swap(int a, int b) {
        Object aObject = currentActionListModel.getElementAt(a);
        Object bObject = currentActionListModel.getElementAt(b);
        currentActionListModel.set(a, bObject);
        currentActionListModel.set(b, aObject);
        network.getUpdateManager().swapElements(a, b);
    }

    /**
     * Renderer for lists in this panel
     */
    private ListCellRenderer listRenderer = new ListCellRenderer() {
        public Component getListCellRendererComponent(JList list, Object updateAction, int index, boolean isSelected, boolean cellHasFocus) {

            JLabel label = new JLabel((index + 1) + ": " + ((NetworkUpdateAction) updateAction).getDescription());
            label.setToolTipText(((NetworkUpdateAction) updateAction).getLongDescription());
            if (index == 0) {
                label.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.LIGHT_GRAY));
            } else {
                label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
            }
            label.setBackground(null);
            if (isSelected) {
                label.setForeground(list.getSelectionForeground());
                label.setBackground(list.getSelectionBackground());
            } else {
                label.setForeground(list.getForeground());
                label.setBackground(list.getBackground());
            }
            label.setEnabled(list.isEnabled());
            label.setFont(list.getFont());
            label.setOpaque(true);
            return label;
        }

        ;
    };

    /**
     * Configure the action list.
     */
    private void configureActionList() {
        currentActionJList.setCellRenderer(listRenderer);
        currentActionJList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {

                    // When double clicking on custom actions open an editor
                    NetworkUpdateAction action = (NetworkUpdateAction) currentActionJList.getModel().getElementAt(currentActionJList.locationToIndex(e.getPoint()));
                    if (action instanceof CustomUpdate) {
                        openScriptEditorPanel((CustomUpdate) action);
                    }
                }

            }
        });
    }

    /**
     * Open the script editor panel with appropriate defaults.
     *
     * @param action the action
     */
    private void openScriptEditorPanel(CustomUpdate action) {
        ScriptEditor panel = new ScriptEditor(((CustomUpdate) action).getScriptString(), SCRIPT_DIR);
        StandardDialog dialog = panel.getDialog(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        if (!dialog.hasUserCancelled()) {
            ((CustomUpdate) action).setScriptString(panel.getTextArea().getText());
            ((CustomUpdate) action).init();
        }
    }

    /**
     * Action which deletes selected actions.
     */
    Action deleteActionsAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/minus.png"));
            putValue(NAME, "Remove action(s)");
            putValue(SHORT_DESCRIPTION, "Remove selected action(s) from update sequence");
            NetworkUpdateManagerPanel.this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), this);
            NetworkUpdateManagerPanel.this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), this);
            NetworkUpdateManagerPanel.this.getActionMap().put(this, this);
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            for (Object action : currentActionJList.getSelectedValuesList()) {
                network.getUpdateManager().removeAction((NetworkUpdateAction) action);
            }
        }
    };

    /**
     * Action which allows for creation of custom action.
     */
    Action addCustomAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/plus.png"));
            putValue(NAME, "Add custom action");
            putValue(SHORT_DESCRIPTION, "Add a custom action to the update sequence");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            File defaultScript = new File(System.getProperty("user.dir") + "/etc/customNetworkUpdateTemplate.bsh");
            ScriptEditor panel = new ScriptEditor(Utils.readFileContents(defaultScript), SCRIPT_DIR);
            panel.setScriptFile(defaultScript);
            StandardDialog dialog = panel.getDialog(panel);
            // Setting script file to null prevents the template script from
            // being saved. Forces "save as"
            // if save button pressed.
            panel.setScriptFile(null);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            if (!dialog.hasUserCancelled()) {
                CustomUpdate updateAction = new CustomUpdate(network, panel.getTextArea().getText());
                network.getUpdateManager().addAction(updateAction);
            }

        }
    };

    /**
     * Add a preset action.
     */
    Action addPresetAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/plus.png"));
            putValue(NAME, "Add action");
            putValue(SHORT_DESCRIPTION, "Add an action to the update sequence");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            final JList availableActionJList = new JList();
            final DefaultListModel listModel = new DefaultListModel();
            JScrollPane availableListScroll = new JScrollPane(availableActionJList);
            availableListScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            availableListScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            availableActionJList.setModel(listModel);
            listModel.clear();
            for (NetworkUpdateAction action : network.getUpdateManager().getAvailableActionList()) {
                listModel.addElement(action);
            }
            configureAvailableJList(availableActionJList);
            StandardDialog addActionsDialog = new StandardDialog() {
                @Override
                protected void closeDialogOk() {
                    super.closeDialogOk();
                    for (Object action : availableActionJList.getSelectedValuesList()) {
                        network.getUpdateManager().addAction((NetworkUpdateAction) action);
                    }
                    ;
                }
            };
            addActionsDialog.setTitle("Add predefined action");
            addActionsDialog.setContentPane(availableListScroll);
            addActionsDialog.pack();
            addActionsDialog.setLocationRelativeTo(null);
            addActionsDialog.setVisible(true);

        }
    };

    /**
     * Configure the available JList panel.
     */
    private void configureAvailableJList(final JList availableActionJList) {
        availableActionJList.setCellRenderer(listRenderer);
        availableActionJList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    NetworkUpdateAction action = (NetworkUpdateAction) availableActionJList.getModel().getElementAt(availableActionJList.locationToIndex(e.getPoint()));
                    if (action instanceof CustomUpdate) {
                        openScriptEditorPanel((CustomUpdate) action);
                    }
                }

            }
        });
    }

    /**
     * Update the JList's model.
     */
    private void updateCurrentActionsList() {
        currentActionListModel.clear();
        for (NetworkUpdateAction action : network.getUpdateManager().getActionList()) {
            currentActionListModel.addElement(action);
        }
        repaint();
    }

    /**
     * Listener for update manager changes.
     */
    private Listener listener = new Listener() {

        public void actionAdded(NetworkUpdateAction action) {
            updateCurrentActionsList();
        }

        public void actionRemoved(NetworkUpdateAction action) {
            updateCurrentActionsList();
        }

        public void actionOrderChanged() {
            updateCurrentActionsList();
        }

    };

    /**
     * Handle drag and drop events
     *
     * @return
     */
    private TransferHandler createTransferHandler() {
        return new TransferHandler() {

            public boolean canImport(TransferHandler.TransferSupport info) {
                // if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                // return false;
                // }

                JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
                if (dl.getIndex() == -1) {
                    return false;
                }
                return true;
            }

            public boolean importData(TransferHandler.TransferSupport info) {
                // if (!info.isDrop()) {
                // return false;
                // }

                JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
                int targetIndex = dl.getIndex();
                int sourceIndex = currentActionJList.getSelectedIndex();
                int listSize = network.getUpdateManager().getActionList().size();
                if (targetIndex == listSize) {
                    targetIndex--;
                }
                if (sourceIndex == listSize) {
                    sourceIndex--;
                }
                network.getUpdateManager().swapElements(sourceIndex, targetIndex);
                return true;
            }

            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }

            protected Transferable createTransferable(JComponent c) {
                return new StringSelection("");
            }

        };
    }

    // /**
    // * Test panel.
    // */
    // public static void main(String[] args) {
    // JFrame frame = new JFrame();
    // List<String> actions = new ArrayList<String>();
    // actions.add("Buffered Update");
    // actions.add("Group 1");
    // actions.add("Group 2");
    // actions.add("Neuron 1");
    // UpdateManagerPanel panel = new UpdateManagerPanel(frame, actions);
    // frame.setContentPane(panel);
    // frame.setVisible(true);
    // frame.pack();
    // }

}
