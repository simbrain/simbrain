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
package org.simbrain.workspace.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceSerializer;


/**
 * <b>WorkspaceChangedDialog</b> tells the user what components have changed
 * since the last time they saved.
 */
public class WorkspaceChangedDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final Workspace workspace;
    
    /**
     * Constructor for workspace changed dialog.
     */
    public WorkspaceChangedDialog(Workspace workspace) {
        this.workspace = workspace;
        init();
    }
    
    /** Main Panel. */
    private LabelledItemPanel panel = new LabelledItemPanel();

    /** Whether the user has cancelled out of this dialog. */
    private boolean userCancelled = false;

    /** List of checkboxes. */
    private ArrayList<JCheckBox> checkBoxList = new ArrayList<JCheckBox>();

    /** Whether the workspace as a whole has changed. */
    private JCheckBox workspaceChecker = new JCheckBox();

    /**
     * Initialize the panel.
     */
    public void init() {
        initPanel();

        this.getContentPane().setLayout(new BorderLayout());

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        getRootPane().setDefaultButton(ok);
        getContentPane().add(ok);
        getContentPane().add(cancel);
        ok.addActionListener(this);
        ok.setActionCommand("ok");
        cancel.addActionListener(this);
        cancel.setActionCommand("cancel");

        getContentPane().add(BorderLayout.CENTER, panel);

        JPanel northPanel = new JPanel(new GridLayout(2, 0));
        northPanel.add(new JLabel(" The following resources have not been saved,  "));
        northPanel.add(new JLabel(" check the ones you want to save:"));
        getContentPane().add(BorderLayout.NORTH, northPanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        getContentPane().add(BorderLayout.SOUTH, buttonPanel);

        setTitle("Save Resources");
        pack();
        setLocationRelativeTo(null);
        setModal(true);
        setVisible(true);
    }

    /**
     * Display information about which components have changed.
     */
    public void initPanel() {

        int i = 0;
        for (WorkspaceComponent component : SimbrainDesktop.getInstance().getWorkspace().getComponentList()) {
            if (component.isChangedSinceLastSave()) {
                JCheckBox checker = new JCheckBox();
                panel.addItem(component.getName() + "." + component.getFileExtension(), checker);
                checkBoxList.add(i++, checker);
            }
        }
        
        // TODO fix
//        if (Workspace.getInstance().hasWorkspaceChanged()) {
//            panel.addItem("Workspace (" + Workspace.getInstance().getTitle() + ")", workspaceChecker);
//        }
    }

    /**
     * Responds to actions performed.
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("cancel")) {
            userCancelled = true;
            dispose();
        } else if (e.getActionCommand().equals("ok")) {
            doSaves();
            dispose();
        }
    }

    /**
     * Save all checked components.
     */
    private void doSaves() {

        int i = 0;
        for (JCheckBox checkBox : checkBoxList) {
            if (checkBox.isSelected()) {
                workspace.getComponentList().get(i).save();
            }
            i++;
        }
        if (workspaceChecker.isSelected()) {
            if (workspace.getCurrentFile() != null) {
                new WorkspaceSerializer(workspace).writeWorkspace(workspace.getCurrentFile());
            } else {
                SimbrainDesktop.getInstance().saveWorkspace();
            }
        }
    }

    /**
     * @return Returns the userCancelled.
     */
    public boolean hasUserCancelled() {
        return userCancelled;
    }
}
