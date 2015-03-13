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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.filechooser.FileFilter;

import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceSerializer;

/**
 * <b>WorkspaceChangedDialog</b> tells the user what components have changed
 * since the last time they saved.
 */
public class WorkspaceChangedDialog extends JDialog {

    /** The default serial version Id. */
    private static final long serialVersionUID = 1L;

    /** The parent desktop. */
    private final SimbrainDesktop desktop;

    /**
     * Constructor for workspace changed dialog.
     *
     * @param desktop The parent desktop.
     */
    public WorkspaceChangedDialog(final SimbrainDesktop desktop) {
        this.desktop = desktop;
        init();
    }

    /** Main Panel. */
    // private JPanel panel;

    /** Whether the user has canceled out of this dialog. */
    private boolean userCancelled = false;

    /**
     * Initialize the panel.
     */
    public void init() {
        // initPanel();
        SpringLayout layout = new SpringLayout();
        Container container = getContentPane();

        container.setLayout(layout);// new BorderLayout());

        JButton yesButton = new JButton(yes);
        JButton noButton = new JButton(no);
        JButton cancelButton = new JButton(cancel);
        getRootPane().setDefaultButton(yesButton);
        // getContentPane().add(BorderLayout.CENTER, addBrowsePanel());

        JPanel textPanel = new JPanel(new GridLayout(2, 0));
        textPanel.add(new JLabel("The current workspace has changed."));
        textPanel.add(new JLabel("Would you like to save it?"));
        // getContentPane().add(BorderLayout.NORTH, northPanel);
        getContentPane().add(textPanel);

        JPanel browsePanel = addBrowsePanel();
        getContentPane().add(browsePanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        buttonPanel.add(cancelButton);
        // getContentPane().add(BorderLayout.SOUTH, buttonPanel);
        getContentPane().add(buttonPanel);

        layout.putConstraint(SpringLayout.NORTH, container, -5,
                SpringLayout.NORTH, textPanel);
        layout.putConstraint(SpringLayout.SOUTH, container, 5,
                SpringLayout.SOUTH, buttonPanel);
        layout.putConstraint(SpringLayout.WEST, container, -5,
                SpringLayout.WEST, browsePanel);
        layout.putConstraint(SpringLayout.EAST, container, 5,
                SpringLayout.EAST, browsePanel);
        layout.putConstraint(SpringLayout.WEST, textPanel, 5,
                SpringLayout.WEST, container);
        layout.putConstraint(SpringLayout.NORTH, browsePanel, 5,
                SpringLayout.SOUTH, textPanel);
        layout.putConstraint(SpringLayout.NORTH, buttonPanel, 5,
                SpringLayout.SOUTH, browsePanel);

        setTitle("Save Resources");

        setLocationRelativeTo(null);
        setModal(true);

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });

        pack();
        height = getSize().height;

        this.addComponentListener(listener);

        setVisible(true);
    }

    int height;

    ComponentListener listener = new ComponentAdapter() {
        public void componentResized(ComponentEvent ce) {
            Dimension size = getSize();
            if (size.height != height) {
                size.height = height;
                setSize(size);
                invalidate();
                validate();
            }
        }
    };

    public JPanel addBrowsePanel() {
        JPanel panel = new JPanel();
        SpringLayout layout = new SpringLayout();

        panel.setLayout(layout);

        final JTextField location = new JTextField("", 25);
        JButton browse = new JButton("Browse");

        panel.add(browse);
        panel.add(location);

        layout.putConstraint(SpringLayout.NORTH, location, 5,
                SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.NORTH, browse, 5, SpringLayout.NORTH,
                panel);
        layout.putConstraint(SpringLayout.WEST, panel, -5, SpringLayout.WEST,
                location);
        layout.putConstraint(SpringLayout.WEST, location, 5, SpringLayout.WEST,
                panel);
        layout.putConstraint(SpringLayout.EAST, panel, 0, SpringLayout.EAST,
                browse);
        layout.putConstraint(SpringLayout.WEST, browse, 5, SpringLayout.EAST,
                location);
        layout.putConstraint(SpringLayout.SOUTH, panel, 0, SpringLayout.SOUTH,
                browse);

        browse.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                JFileChooser chooser = new JFileChooser(location.getText());

                chooser.addChoosableFileFilter(new Filter("zip",
                        "Simbrain Archives"));

                switch (chooser.showOpenDialog(WorkspaceChangedDialog.this)) {

                case JFileChooser.APPROVE_OPTION:
                    location.setText(chooser.getSelectedFile()
                            .getAbsolutePath());
                }
            }
        });

        return panel;
    }

    private static class Filter extends FileFilter {
        String extension;
        String description;

        Filter(String extension, String description) {
            this.extension = extension;
            this.description = description;
        }

        public boolean accept(File f) {
            return f.getName().endsWith('.' + extension);
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    // JPanel checkboxes;
    //
    // public void addCheckboxPanel() {
    // checkboxes = new JPanel();
    // SpringLayout layout = new SpringLayout();
    // JCheckBox last = null;
    //
    // checkboxes.setLayout(layout);
    //
    // for (WorkspaceComponent component :
    // desktop.getWorkspace().getComponentList()) {
    // JLabel label = new JLabel(component.getName());
    // JCheckBox checkbox = new JCheckBox();
    // checkbox.setSelected(true);
    //
    // checkboxes.add(checkbox);
    // checkboxes.add(label);
    //
    // if (last == null) {
    // layout.putConstraint(SpringLayout.NORTH, checkbox,
    // 5, SpringLayout.NORTH, checkboxes);
    // } else {
    // layout.putConstraint(SpringLayout.NORTH, checkbox,
    // 5, SpringLayout.SOUTH, last);
    // }
    //
    // layout.putConstraint(SpringLayout.WEST, checkbox,
    // 20, SpringLayout.WEST, checkboxes);
    // layout.putConstraint(SpringLayout.WEST, label,
    // 5, SpringLayout.EAST, checkbox);
    // layout.putConstraint(SpringLayout.SOUTH, label,
    // 0, SpringLayout.SOUTH, checkbox);
    //
    // last = checkbox;
    // }
    //
    // layout.putConstraint(SpringLayout.SOUTH, checkboxes,
    // 5, SpringLayout.SOUTH, last);
    //
    // panel.add(checkboxes, BorderLayout.CENTER);
    //
    // pack();
    // }

    // void removeCheckboxPanel() {
    // panel.remove(checkboxes);
    // pack();
    // }

    private final Action yes = new AbstractAction("Yes") {
        public void actionPerformed(ActionEvent e) {
            doSaves();
            dispose();
        }
    };

    private final Action no = new AbstractAction("No") {
        public void actionPerformed(ActionEvent e) {
            userCancelled = false;
            dispose();
        }
    };

    private final Action cancel = new AbstractAction("Cancel") {
        public void actionPerformed(ActionEvent e) {
            cancel();
        }
    };

    private void cancel() {
        userCancelled = true;
        dispose();
    }

    /**
     * Save all checked components.
     */
    private void doSaves() {
        Workspace workspace = desktop.getWorkspace();

        WorkspaceSerializer serializer = new WorkspaceSerializer(workspace);

        try {
            FileOutputStream ostream = new FileOutputStream("workspace.zip");

            try {
                serializer.serialize(ostream);
            } finally {
                ostream.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // int i = 0;
        // for (JCheckBox checkBox : checkBoxList) {
        // if (checkBox.isSelected()) {
        // // TODO call save dialog
        // // workspace.getComponentList().get(i).save();
        // }
        // i++;
        // }
        // if (workspaceChecker.isSelected()) {
        // if (workspace.getCurrentFile() != null) {
        // new
        // WorkspaceSerializer(workspace).writeWorkspace(workspace.getCurrentFile());
        // } else {
        // desktop.saveWorkspace();
        // }
        // }
    }

    /**
     * @return Returns the userCancelled.
     */
    public boolean hasUserCancelled() {
        return userCancelled;
    }
}
