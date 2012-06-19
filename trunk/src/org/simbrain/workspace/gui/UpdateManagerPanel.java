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
package org.simbrain.workspace.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.TransferHandler;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.ScriptEditor;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.updater.UpdateAction;
import org.simbrain.workspace.updater.UpdateActionCustom;
import org.simbrain.workspace.updater.UpdateActionManager.UpdateManagerListener;
import org.simbrain.workspace.updater.UpdateAllBuffered;

/**
 * Panel for display and ordering of workspace update actions.
 *
 * @author jeff yoshimi
 *
 */
public class UpdateManagerPanel extends JPanel {

    /** The JList which represents current actions. */
    private final JList currentActionJList = new JList();

    /** The model object for current actions. */
    private final DefaultListModel currentActionListModel = new DefaultListModel();

    /** The JList which represents available actions. */
    private final JList availableActionJList = new JList();

    /** The model object for current actions. */
    private final DefaultListModel availableActionListModel = new DefaultListModel();

    /** Reference to workspace. */
    private final Workspace workspace;

    /**
     * Creates a new action list panel.
     */
    public UpdateManagerPanel(final Workspace workspace) {

        super(new BorderLayout());
        this.workspace = workspace;

        // Set up Current Action list
        currentActionJList.setModel(currentActionListModel);
        updateCurrentActionsList();
        configureCurrentJList();
        currentActionJList.setDragEnabled(true);
        currentActionJList.setTransferHandler(createTransferHandler());
        currentActionJList.setDropMode(DropMode.INSERT);
        JScrollPane currentListScroll = new JScrollPane(currentActionJList);
        currentListScroll
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        currentListScroll
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        currentListScroll.setBorder(BorderFactory
                .createTitledBorder("Current Update Sequence"));
        // currentListScroll.setBackground(null);

        // Set up Available Action list
        availableActionJList.setModel(availableActionListModel);
        updateAvailableActionsList();
        configureAvailableJList();
        JScrollPane availableListScroll = new JScrollPane(availableActionJList);
        // availableActionJList.setBackground(getBackground());
        // availableListScroll.setBackground(null);

        availableListScroll
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        availableListScroll
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        availableListScroll.setBorder(BorderFactory
                .createTitledBorder("Unused Update Actions"));

        // Add lists
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                availableListScroll, currentListScroll);
        split.setResizeWeight(.5);
        add(split, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton customActionButton = new JButton(addCustomAction);
        buttonPanel.add(customActionButton);
        JButton addActionsButton = new JButton(addActionsAction);
        buttonPanel.add(addActionsButton);
        JButton deleteActionsButton = new JButton(deleteActionsAction);
        buttonPanel.add(deleteActionsButton);
        updateAvailableActionsList();
        add(buttonPanel, BorderLayout.SOUTH);

        workspace.getUpdater().getUpdateManager().addListener(listener);

        // TODO: Handle closing event
        // Should remove listener from update manager when this is closed

    }

    /**
     * Listener for update manager changes.
     */
    private UpdateManagerListener listener = new UpdateManagerListener() {

        public void actionAdded(UpdateAction action) {
            updateCurrentActionsList();
        }

        public void actionRemoved(UpdateAction action) {
            updateCurrentActionsList();
        }

        public void actionOrderChanged() {
            updateCurrentActionsList();
        }

        public void availableActionAdded(UpdateAction action) {
            updateAvailableActionsList();
        }

        public void availableActionRemoved(UpdateAction action) {
            updateAvailableActionsList();
        }

    };

    /**
     * Configure the JList.
     */
    private void configureCurrentJList() {
        currentActionJList.setCellRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                    Object updateAction, int index, boolean isSelected,
                    boolean cellHasFocus) {

                JLabel label = new JLabel((index + 1) + ": "
                        + ((UpdateAction) updateAction).getDescription());
                label.setToolTipText(((UpdateAction) updateAction)
                        .getLongDescription());
                if (index == 0) {
                    label.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0,
                            Color.LIGHT_GRAY));
                } else {
                    label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                            Color.LIGHT_GRAY));
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
        });
        currentActionJList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {

                    // When double clicking on custom actions open an editor
                    UpdateAction action = (UpdateAction) currentActionJList
                            .getModel().getElementAt(
                                    currentActionJList.locationToIndex(e
                                            .getPoint()));
                    if (action instanceof UpdateActionCustom) {
                        ScriptEditor panel = new ScriptEditor(
                                ((UpdateActionCustom) action).getScriptString());
                        StandardDialog dialog = ScriptEditor.getDialog(panel);
                        dialog.pack();
                        dialog.setLocationRelativeTo(null);
                        dialog.setVisible(true);
                        if (!dialog.hasUserCancelled()) {
                            ((UpdateActionCustom) action).setScriptString(panel
                                    .getTextArea().getText());
                            ((UpdateActionCustom) action).init();
                        }
                    }
                }

            }
        });
    }

    /**
     * Configure the JList.
     */
    private void configureAvailableJList() {
        availableActionJList.setCellRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                    Object updateAction, int index, boolean isSelected,
                    boolean cellHasFocus) {
                JLabel label = new JLabel(((UpdateAction) updateAction)
                        .getDescription());
                label.setToolTipText(((UpdateAction) updateAction)
                        .getLongDescription());
                if (index == 0) {
                    label.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0,
                            Color.LIGHT_GRAY));
                } else {
                    label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                            Color.LIGHT_GRAY));
                }
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                } else {
                    label.setForeground(list.getForeground());
                    label.setBackground(null);
                }
                label.setEnabled(list.isEnabled());
                label.setFont(list.getFont());
                label.setOpaque(true);
                return label;
            }
        });
        availableActionJList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    UpdateAction action = (UpdateAction) availableActionJList
                            .getModel().getElementAt(
                                    availableActionJList.locationToIndex(e
                                            .getPoint()));
                    if (action instanceof UpdateActionCustom) {
                        ScriptEditor panel = new ScriptEditor(
                                ((UpdateActionCustom) action).getScriptString());
                        StandardDialog dialog = ScriptEditor.getDialog(panel);
                        dialog.pack();
                        dialog.setLocationRelativeTo(null);
                        dialog.setVisible(true);
                        if (!dialog.hasUserCancelled()) {
                            ((UpdateActionCustom) action).setScriptString(panel
                                    .getTextArea().getText());
                            ((UpdateActionCustom) action).init();
                        }
                    }
                }

            }
        });
    }

    /** Action which deletes selected actions. */
    Action deleteActionsAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("minus.png"));
            putValue(NAME, "Remove selected action(s)");
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
            for (Object action : currentActionJList.getSelectedValues()) {
                workspace.getUpdater().getUpdateManager()
                        .moveActionToAvailableList((UpdateAction) action);
            }
            for (Object action : availableActionJList.getSelectedValues()) {
                // Don't allow built in actions to be removed
                if (action instanceof UpdateAllBuffered) {
                    continue;
                } else {
                    workspace.getUpdater().getUpdateManager()
                    .removeAction((UpdateAction) action);
                }
            }
        }
    };

    /** Action which deletes selected actions. */
    Action addActionsAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("plus.png"));
            putValue(NAME, "Add selected action(s)");
            putValue(SHORT_DESCRIPTION, "Add selected actions");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            for (Object action : availableActionJList.getSelectedValues()) {
                workspace.getUpdater().getUpdateManager()
                        .addAction((UpdateAction) action);
            }
        }
    };

    /** Action which allows for creation of custom action. */
    Action addCustomAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("plus.png"));
            putValue(NAME, "Custom action");
            putValue(SHORT_DESCRIPTION, "Add custom action");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            File defaultScript = new File(System.getProperty("user.dir")
                    + "/etc/customWorkspaceUpdateTemplate.bsh");
            ScriptEditor panel = new ScriptEditor(
                    Utils.readFileContents(defaultScript));
            panel.setScriptFile(defaultScript);
            StandardDialog dialog = ScriptEditor.getDialog(panel);
            // Setting script file to null prevents the template script from
            // being saved. Forces "save as"
            // if save button pressed.
            panel.setScriptFile(null);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            if (!dialog.hasUserCancelled()) {
                UpdateActionCustom updateAction = new UpdateActionCustom(
                        workspace.getUpdater(), panel.getTextArea().getText());
                workspace.getUpdater().getUpdateManager()
                        .addAction(updateAction);
            }

        }
    };

    /**
     * Update the JList's model.
     */
    private void updateCurrentActionsList() {
        currentActionListModel.clear();
        for (UpdateAction action : workspace.getUpdater().getUpdateManager()
                .getActionList()) {
            currentActionListModel.addElement(action);
        }
        repaint();
    }

    /**
     * Update the combo box showing potential actions.
     */
    private void updateAvailableActionsList() {
        availableActionListModel.clear();
        for (UpdateAction action : workspace.getUpdater().getUpdateManager()
                .getAvailableActionList()) {
            availableActionListModel.addElement(action);
        }
        repaint();
    }

    /**
     * Handle drag and drop events
     *
     * @return the transfer handler
     */
    private TransferHandler createTransferHandler() {
        return new TransferHandler() {

            public boolean canImport(TransferHandler.TransferSupport info) {
                // if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                // return false;
                // }

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
                int targetIndex = dl.getIndex();
                int sourceIndex = currentActionJList.getSelectedIndex();
                int listSize = workspace.getUpdater().getUpdateManager()
                        .getActionList().size();
                if (targetIndex == listSize) {
                    targetIndex--;
                }
                if (sourceIndex == listSize) {
                    sourceIndex--;
                }
                workspace.getUpdater().getUpdateManager()
                        .swapElements(sourceIndex, targetIndex);
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

}
