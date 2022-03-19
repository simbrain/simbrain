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

import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.updater.UpdateAction;
import org.simbrain.workspace.updater.UpdateActionManager.UpdateManagerListener;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;

/**
 * Panel for display and ordering of workspace update actions.
 *
 * @author Jeff Yoshimi
 */
public class WorkspaceUpdateManagerPanel extends JPanel {

    /**
     * UpdateListener updates the action sequence whenever changes are made or an update is completed.
     */
    private class UpdateListener implements UpdateManagerListener {

        @Override
        public void actionAdded(UpdateAction action) {
            updateCurrentActionsList();
        }

        @Override
        public void actionRemoved(UpdateAction action) {
            updateCurrentActionsList();
        }

        @Override
        public void actionOrderChanged() {
            updateCurrentActionsList();
        }

    }

    /**
     * Provides drag and drop support for reordering update actions in the list.
     */
    private class DragAndDropHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            JList.DropLocation dropLocation = (JList.DropLocation) info.getDropLocation();
            return dropLocation.getIndex() != -1;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            JList.DropLocation dropLocation = (JList.DropLocation) info.getDropLocation();
            int targetIndex = dropLocation.getIndex();
            int sourceIndex = currentActionJList.getSelectedIndex();
            int listSize = workspace.getUpdater().getUpdateManager().getActionList().size();
            if (targetIndex == listSize) {
                targetIndex--;
            }
            if (sourceIndex == listSize) {
                sourceIndex--;
            }
            workspace.getUpdater().getUpdateManager().swapElements(sourceIndex, targetIndex);
            return true;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection("");
        }
    }

    /**
     * Script directory for custom workspace updates.
     */
    private static final String SCRIPT_DIR = "." + System.getProperty("file.separator") + "scripts" + System.getProperty("file.separator") + "updateScripts" + System.getProperty("file.separator") + "workspaceUpdate";

    /**
     * The JList which represents current actions.
     */
    private final JList<UpdateAction> currentActionJList = new JList<>();

    /**
     * The model object for current actions.
     */
    private final DefaultListModel<UpdateAction> currentActionListModel = new DefaultListModel<>();

    /**
     * Reference to workspace.
     */
    private final Workspace workspace;

    /**
     * Listener for update manager changes.
     */
    private UpdateListener listener = new UpdateListener();

    /**
     * Action which deletes selected actions.
     */
    Action deleteActionsAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/minus.png"));
            putValue(NAME, "Remove");
            putValue(SHORT_DESCRIPTION, "Remove the selected update actions.");
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), this);
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), this);
            getActionMap().put(this, this);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            deleteSelectedUpdateActions();
        }
    };

    /**
     * Add a preset action.
     */
    Action addPresetAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/plus.png"));
            putValue(NAME, "Add");
            putValue(SHORT_DESCRIPTION, "Add an available predefined update action to the update sequence.");
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            showAvailableActionsDialog();
        }
    };

    /**
     * Construct workspace update manager panel.
     *
     * @param workspace    parent workspace
     * @param parentDialog dialog containing this panel
     */
    public WorkspaceUpdateManagerPanel(Workspace workspace, StandardDialog parentDialog) {
        super(new BorderLayout());
        this.workspace = workspace;

        // Set up the action list
        currentActionJList.setModel(currentActionListModel);
        configureActionList();
        currentActionJList.setDragEnabled(true);
        currentActionJList.setTransferHandler(new DragAndDropHandler());
        currentActionJList.setDropMode(DropMode.INSERT);
        JScrollPane currentListScroll = new JScrollPane(currentActionJList);
        currentListScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        currentListScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        currentListScroll.setViewportBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));
        updateCurrentActionsList();
        add(currentListScroll, BorderLayout.CENTER);

        // Add buttons
        JPanel buttonPanel = new JPanel();
        JButton addActionsButton = new JButton(addPresetAction);
        buttonPanel.add(addActionsButton);
        JButton deleteActionsButton = new JButton(deleteActionsAction);
        buttonPanel.add(deleteActionsButton);

        // TODO: Make movement actions apply to multiple selections
        JButton upButton = new JButton(ResourceManager.getImageIcon("menu_icons/Up.png"));
        upButton.setToolTipText("Move selected action up in sequence");
        upButton.addActionListener(evt -> moveSelectedUpdateAction(-1));
        buttonPanel.add(upButton);

        JButton upFullButton = new JButton(ResourceManager.getImageIcon("menu_icons/UpFull.png"));
        upFullButton.setToolTipText("Move selected action to top of sequence");
        upFullButton.addActionListener(evt -> moveSelectedUpdateAction(-currentActionJList.getSelectedIndex()));
        buttonPanel.add(upFullButton);

        JButton downButton = new JButton(ResourceManager.getImageIcon("menu_icons/Down.png"));
        downButton.setToolTipText("Move selected action down in sequence");
        downButton.addActionListener(evt -> moveSelectedUpdateAction(1));
        buttonPanel.add(downButton);

        JButton downFullButton = new JButton(ResourceManager.getImageIcon("menu_icons/DownFull.png"));
        downFullButton.setToolTipText("Move selected action to bottom of sequence");
        downFullButton.addActionListener(evt -> moveSelectedUpdateAction(currentActionListModel.getSize() - currentActionJList.getSelectedIndex()));
        buttonPanel.add(downFullButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Help button
        if (parentDialog != null) {
            Action helpAction = new ShowHelpAction("Pages/Workspace/update.html");
            parentDialog.addButton(new JButton(helpAction));
            parentDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent evt) {
                    super.windowClosing(evt);
                    workspace.getUpdater().getUpdateManager().removeListener(listener);
                }
            });
        }

        workspace.getUpdater().getUpdateManager().addListener(listener);
    }

    /**
     * Configure the JList.
     */
    private void configureActionList() {
        currentActionJList.setCellRenderer(this::getUpdateActionCell);
        currentActionJList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // When double clicking on custom actions open an editor
                    int clickedIndex = currentActionJList.locationToIndex(e.getPoint());
                    UpdateAction action = currentActionJList.getModel().getElementAt(clickedIndex);
                }
            }
        });
    }

    /**
     * Return a component based on the provided action in the list.
     */
    private Component getUpdateActionCell(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        UpdateAction action = (UpdateAction) value;
        String text = (index + 1) + ": " + action.getDescription();
        JLabel label = new JLabel(text);
        label.setToolTipText(action.getLongDescription());
        label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        label.setBackground(null);
        setCellColor(label, list, isSelected);
        label.setEnabled(list.isEnabled());
        label.setFont(list.getFont());
        label.setOpaque(true);
        return label;
    }

    private void setCellColor(JLabel label, JList<UpdateAction> list, boolean isSelected) {
        if (isSelected) {
            label.setForeground(list.getSelectionForeground());
            label.setBackground(list.getSelectionBackground());
        } else {
            label.setForeground(list.getForeground());
            label.setBackground(list.getBackground());
        }
    }

    private void deleteSelectedUpdateActions() {
        for (UpdateAction action : currentActionJList.getSelectedValuesList()) {
            workspace.getUpdater().getUpdateManager().removeAction(action);
        }
    }

    private void showAvailableActionsDialog() {
        JList<UpdateAction> availableActionJList = new JList<>();
        DefaultListModel<UpdateAction> listModel = new DefaultListModel<>();
        JScrollPane availableListScroll = new JScrollPane(availableActionJList);
        availableListScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        availableListScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        availableActionJList.setModel(listModel);
        listModel.clear();
        for (UpdateAction action : workspace.getUpdater().getUpdateManager().getAvailableActionList()) {
            listModel.addElement(action);
        }
        configureAvailableJList(availableActionJList);
        StandardDialog addActionsDialog = new StandardDialog() {
            @Override
            protected void closeDialogOk() {
                super.closeDialogOk();
                for (Object action : availableActionJList.getSelectedValuesList()) {
                    workspace.getUpdater().getUpdateManager().addAction((UpdateAction) action);
                }
            }
        };
        addActionsDialog.setTitle("Add Available Update Action");
        addActionsDialog.setContentPane(availableListScroll);
        addActionsDialog.pack();
        addActionsDialog.setLocationRelativeTo(null);
        addActionsDialog.setVisible(true);
    }

    /**
     * Configure the available JList panel.
     */
    private void configureAvailableJList(JList<UpdateAction> availableActionJList) {
        availableActionJList.setCellRenderer(this::getUpdateActionCell);
    }

    /**
     * Update the JList's model.
     */
    private void updateCurrentActionsList() {
        currentActionListModel.clear();
        for (UpdateAction action : workspace.getUpdater().getUpdateManager().getActionList()) {
            currentActionListModel.addElement(action);
        }
        repaint();
    }

    /**
     * Move the selected update action up or down in the list.
     */
    private void moveSelectedUpdateAction(int move) {
        if (move == 0) {
            return;
        }
        int selected = currentActionJList.getSelectedIndex();
        int target = selected + move;
        if (target >= 0 && target < currentActionListModel.getSize()) {
            swap(selected, target);
            currentActionJList.setSelectedIndex(target);
            currentActionJList.ensureIndexIsVisible(target);
        }
    }

    /**
     * Swap two elements int the current action list.
     *
     * @param a index for item a
     * @param b index for item b
     */
    private void swap(int a, int b) {
        UpdateAction aAction = currentActionListModel.getElementAt(a);
        UpdateAction bAction = currentActionListModel.getElementAt(b);
        currentActionListModel.set(a, bAction);
        currentActionListModel.set(b, aAction);
        workspace.getUpdater().getUpdateManager().swapElements(a, b);
    }

}
