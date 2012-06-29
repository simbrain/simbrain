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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;
import org.simbrain.workspace.Workspace;

/**
 * <b>WorkspaceDialog</b> is a dialog box for setting the properties of the
 * Workspace.
 */
public class WorkspaceDialog extends StandardDialog implements ActionListener {

    /** Reference to workspace. **/
    private Workspace workspace;

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Update manager panel. */
    private UpdateManagerPanel updatePanel;

    /**
     * This method is the default constructor.
     *
     * @param np reference to <code>SimbrainDesktop</code>.
     */
    public WorkspaceDialog(final Workspace workspace) {
        this.workspace = workspace;
        init();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        // Initialize Dialog
        setTitle("Workspace Dialog");
        fillFieldValues();

        // UpdatePanel
        updatePanel = new UpdateManagerPanel(workspace);

        // Set up tab panels
        tabbedPane.addTab("Update", updatePanel);
        setContentPane(tabbedPane);

        LabelledItemPanel miscPanel = new LabelledItemPanel();

        // Panel with a slider bar and label to display simulation speed
        JPanel sliderPanel = new JPanel();
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 500,
                workspace.getUpdateDelay());
        final JLabel speedLabel = new JLabel("" + workspace.getUpdateDelay());
        speedLabel.setPreferredSize(new Dimension(50, 10));
        slider.setToolTipText("Use this to delay simulation speed; "
                + "sometimes useful for demo purposes");
        slider.setMajorTickSpacing(100);
        slider.setPaintTicks(true);
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                workspace.setUpdateDelay(source.getValue());
                speedLabel.setText("" + workspace.getUpdateDelay());
            }
        });
        sliderPanel.add(slider);
        sliderPanel.add(speedLabel);
        sliderPanel.setBorder(null);

        miscPanel.addItem(
                "Simulation delay (millisconds to sleep between iterations)",
                sliderPanel);

        tabbedPane.addTab("Misc.", miscPanel);

        // Add help button
        JButton helpButton = new JButton("Help");
        ShowHelpAction helpAction = new ShowHelpAction(
                "Pages/Workspace/Preferences.html");
        helpButton.setAction(helpAction);
        this.addButton(helpButton);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        this.commitChanges();
        this.setAsDefault();
    }

    @Override
    protected void closeDialogCancel() {
        super.closeDialogCancel();
        this.returnToCurrentPrefs();
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
    }

    /**
     * Commits changes not handled in action performed.
     */
    private void commitChanges() {

    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
    }

}
