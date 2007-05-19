/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
package org.simbrain.workspace;

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

import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.NetworkFrame;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.world.dataworld.DataWorldFrame;
import org.simbrain.world.odorworld.OdorWorldFrame;


/**
 * <b>WorkspaceChangedDialog</b> tells the user what components have changed
 * since the last time they saved.
 */
public class WorkspaceChangedDialog extends JDialog implements ActionListener {

    /** Main Panel. */
    private LabelledItemPanel panel = new LabelledItemPanel();
    /** List of networks that have changed. */
    private ArrayList nCheckBoxList = new ArrayList();
    /** List of odor world check boxes. */
    private ArrayList oCheckBoxList = new ArrayList();
    /** List of data world check boxes. */
    private ArrayList dCheckBoxList = new ArrayList();
    /** List of gauge check boxes. */
    private ArrayList gCheckBoxList = new ArrayList();
    /** List of networks which have changed. */
    private ArrayList networkChangeList = new ArrayList();
    /** list of odor worlds that have changed. */
    private ArrayList odorWorldChangeList = new ArrayList();
    /** List of dataworlds that have changed. */
    private ArrayList dataWorldChangeList = new ArrayList();
    /** List of gauges that have changed. */
    private ArrayList gaugeChangeList = new ArrayList();
    /** Reference to parent workspace. */
    private Workspace parent;
    /** Whether the user has cancelled out of this dialog. */
    private boolean userCancelled = false;
    /** Wehther the workspace as a whole has changed. */
    private JCheckBox workspaceChecker = new JCheckBox();

    /**
     * Constructor for workspace changed dialog.
     *
     * @param parent reference to parent workspace
     */
    public WorkspaceChangedDialog(final Workspace parent) {
        networkChangeList = parent.getNetworkChangeList();
        odorWorldChangeList = parent.getOdorWorldChangeList();
        dataWorldChangeList = parent.getDataWorldChangeList();
        gaugeChangeList = parent.getGaugeChangeList();
        this.parent = parent;
        init();
    }

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
        for (int i = 0; i < networkChangeList.size(); i++) {
            NetworkFrame save = (NetworkFrame) networkChangeList.get(i);
            JCheckBox checker = new JCheckBox();
            panel.addItem("Network: " + save.getTitle(), checker);
            nCheckBoxList.add(i, checker);
        }

        for (int i = 0; i < odorWorldChangeList.size(); i++) {
            OdorWorldFrame save = (OdorWorldFrame) odorWorldChangeList.get(i);
            JCheckBox checker = new JCheckBox();
            panel.addItem("Odor-world: " + save.getTitle(), checker);
            oCheckBoxList.add(i, checker);
        }

        for (int i = 0; i < dataWorldChangeList.size(); i++) {
            DataWorldFrame save = (DataWorldFrame) dataWorldChangeList.get(i);
            JCheckBox checker = new JCheckBox();
            panel.addItem("Data-world: " + save.getTitle(), checker);
            dCheckBoxList.add(i, checker);
        }

        for (int i = 0; i < gaugeChangeList.size(); i++) {
            GaugeFrame save = (GaugeFrame) gaugeChangeList.get(i);
            JCheckBox checker = new JCheckBox();
            panel.addItem("Gauge: " + save.getTitle(), checker);
            gCheckBoxList.add(i, checker);
        }

        if (parent.hasWorkspaceChanged()) {
            panel.addItem("Workspace: " + parent.getTitle(), workspaceChecker);
        }
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
        for (int i = 0; i < nCheckBoxList.size(); i++) {
            JCheckBox test = (JCheckBox) nCheckBoxList.get(i);
            NetworkFrame netFrame = (NetworkFrame) networkChangeList.get(i);

            if (test.isSelected()) {
                netFrame.getNetworkPanel().saveCurrentNetwork();
            }

            netFrame.getNetworkPanel().setChangedSinceLastSave(false);
        }

        for (int i = 0; i < oCheckBoxList.size(); i++) {
            JCheckBox test = (JCheckBox) oCheckBoxList.get(i);
            OdorWorldFrame testWorld = (OdorWorldFrame) odorWorldChangeList.get(i);

            if (test.isSelected()) {
                testWorld.saveWorld(testWorld.getCurrentFile());
            }

            testWorld.setChangedSinceLastSave(false);
        }

        for (int i = 0; i < dCheckBoxList.size(); i++) {
            JCheckBox test = (JCheckBox) dCheckBoxList.get(i);
            DataWorldFrame dataWorldFrame = (DataWorldFrame) dataWorldChangeList.get(i);

            if (test.isSelected()) {
                dataWorldFrame.saveWorld(dataWorldFrame.getCurrentFile());
            }

            dataWorldFrame.setChangedSinceLastSave(false);
        }

        for (int i = 0; i < gCheckBoxList.size(); i++) {
            JCheckBox test = (JCheckBox) gCheckBoxList.get(i);
            GaugeFrame gaugeFrame = (GaugeFrame) gaugeChangeList.get(i);

            if (test.isSelected()) {
                gaugeFrame.save();
            }

            gaugeFrame.setChangedSinceLastSave(false);
        }

        if (workspaceChecker.isSelected()) {
            if (parent.getCurrentFile() != null) {
                WorkspaceSerializer.writeWorkspace(parent, parent.getCurrentFile());
            } else {
                parent.showSaveFileAsDialog();
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
