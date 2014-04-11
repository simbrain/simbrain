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
import java.awt.event.ActionListener;
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
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.TransferHandler;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.scripteditor.ScriptEditor;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.updater.UpdateAction;
import org.simbrain.workspace.updater.UpdateActionCustom;
import org.simbrain.workspace.updater.UpdateActionManager.UpdateManagerListener;

/**
 * Panel for display and ordering of workspace update actions.
 *
 * @author Jeff Yoshimi
 *
 */
public class WorkspaceUpdateManagerPanel extends JPanel {

    /** The JList which represents current actions. */
    private final JList currentActionJList = new JList();

    /** The model object for current actions. */
    private final DefaultListModel currentActionListModel = new DefaultListModel();

    /** Reference to workspace. */
    private final Workspace workspace;

    /** Script directory for custom workspace updates. */
    private static final String SCRIPT_DIR = "."
            + System.getProperty("file.separator") + "scripts"
            + System.getProperty("file.separator") + "updateScripts"
            + System.getProperty("file.separator") + "workspaceUpdate";

    /**
     * Construct workspace update manager panel.
     *
     * @param workspace parent workspace
     * @param parentDialog dialog containing this panel
     */
    public WorkspaceUpdateManagerPanel(final Workspace workspace,
            StandardDialog parentDialog) {

        super(new BorderLayout());
        this.workspace = workspace;

        // Set up the action list
        currentActionJList.setModel(currentActionListModel);
        configureActionList();
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
        JButton upButton = new JButton(ResourceManager.getImageIcon("Up.png"));
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
        JButton upFullButton = new JButton(
                ResourceManager.getImageIcon("UpFull.png"));
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
        JButton downButton = new JButton(
                ResourceManager.getImageIcon("Down.png"));
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
        JButton downFullButton = new JButton(
                ResourceManager.getImageIcon("DownFull.png"));
        downFullButton
                .setToolTipText("Move selected action to bottom of sequence");
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
        if (parentDialog != null) {
            Action helpAction = new ShowHelpAction(
                    "Pages/Workspace/update.html");
            parentDialog.addButton(new JButton(helpAction));
        }

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

    };

    /**
     * Renderer for lists in this panel
     */
    private ListCellRenderer listRenderer = new ListCellRenderer() {
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
        };
    };


    /**
     * Configure the JList.
     */
    private void configureActionList() {
        currentActionJList.setCellRenderer(listRenderer);
        currentActionJList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {

                    // When double clicking on custom actions open an editor
                    UpdateAction action = (UpdateAction) currentActionJList
                            .getModel().getElementAt(
                                    currentActionJList.locationToIndex(e
                                            .getPoint()));
                    if (action instanceof UpdateActionCustom) {
                        openScriptEditorPanel(action);
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
    private void openScriptEditorPanel(UpdateAction action) {
        ScriptEditor panel = new ScriptEditor(
                ((UpdateActionCustom) action).getScriptString(), SCRIPT_DIR);
        StandardDialog dialog = panel.getDialog(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        if (!dialog.hasUserCancelled()) {
            ((UpdateActionCustom) action).setScriptString(panel.getTextArea()
                    .getText());
            ((UpdateActionCustom) action).init();
        }
    }

    /** Action which deletes selected actions. */
    Action deleteActionsAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("minus.png"));
            putValue(NAME, "Remove selected action(s)");
            putValue(SHORT_DESCRIPTION, "Delete selected actions");
            WorkspaceUpdateManagerPanel.this.getInputMap(
                    JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke("BACK_SPACE"), this);
            WorkspaceUpdateManagerPanel.this.getInputMap(
                    JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke("DELETE"), this);
            WorkspaceUpdateManagerPanel.this.getActionMap().put(this, this);
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            for (Object action : currentActionJList.getSelectedValuesList()) {
                workspace.getUpdater().getUpdateManager()
                        .removeAction((UpdateAction) action);
            }
        }
    };


    /** Add a preset action. */
    Action addPresetAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("plus.png"));
            putValue(NAME, "Add action");
            putValue(SHORT_DESCRIPTION, "Add an action to the update sequence");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            final JList availableActionJList = new JList();
            final DefaultListModel listModel = new DefaultListModel();
            JScrollPane availableListScroll = new JScrollPane(
                    availableActionJList);
            availableListScroll
                    .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            availableListScroll
                    .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            availableActionJList.setModel(listModel);
            listModel.clear();
            for (UpdateAction action : workspace.getUpdater()
                    .getUpdateManager().getAvailableActionList()) {
                listModel.addElement(action);
            }
            configureAvailableJList(availableActionJList);
            StandardDialog addActionsDialog = new StandardDialog() {
                @Override
                protected void closeDialogOk() {
                    super.closeDialogOk();
                    for (Object action : availableActionJList
                            .getSelectedValuesList()) {
                        workspace.getUpdater().getUpdateManager().addAction(
                                (UpdateAction) action);
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
                    Utils.readFileContents(defaultScript), SCRIPT_DIR);
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
                UpdateActionCustom updateAction = new UpdateActionCustom(
                        workspace.getUpdater(), panel.getTextArea().getText());
                workspace.getUpdater().getUpdateManager()
                        .addAction(updateAction);
            }

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
                    UpdateAction action = (UpdateAction) availableActionJList
                            .getModel().getElementAt(
                                    availableActionJList.locationToIndex(e
                                            .getPoint()));
                    if (action instanceof UpdateActionCustom) {
                        openScriptEditorPanel((UpdateActionCustom) action);
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
    private void openScriptEditorPanel(UpdateActionCustom action) {
        ScriptEditor panel = new ScriptEditor(
                ((UpdateActionCustom) action).getScriptString(), SCRIPT_DIR);
        StandardDialog dialog = panel.getDialog(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        if (!dialog.hasUserCancelled()) {
            ((UpdateActionCustom) action).setScriptString(panel.getTextArea()
                    .getText());
            ((UpdateActionCustom) action).init();
        }
    }

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
        workspace.getUpdater().getUpdateManager().swapElements(a, b);
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
