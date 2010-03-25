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
package org.simbrain.workspace.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.workspace.Workspace;

/**
 * Opens a workspace property dialog.
 */
public final class ShowPropertyDialogAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /** Reference to Simbrain workspace. */
    private Workspace workspace;

    /**
     * Create a workspace component list of the specified workspace.
     *
     * @param desktop reference to simbrain desktop.
     */
    public ShowPropertyDialogAction(final Workspace workspace) {
        super("Workspace properties...");
        this.workspace = workspace;
        putValue(SHORT_DESCRIPTION, "Show workspace properties dialog.");
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
    }

    /**
     * @see AbstractAction
     * @param event
     *            Action event
     */
    public void actionPerformed(final ActionEvent event) {

        StandardDialog dialog = new StandardDialog();
        dialog.setTitle("Workspace Properties");
        LabelledItemPanel mainPanel = new LabelledItemPanel();

        // Panel with a slider bar and label to display simulation speed
        JPanel sliderPanel = new JPanel();
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 500, workspace.getUpdateDelay());
        final JLabel speedLabel = new JLabel("" + workspace.getUpdateDelay());
        speedLabel.setPreferredSize(new Dimension(50,10));
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

        mainPanel.addItem(
                "Simulation delay (millisconds to sleep between iterations)",
                sliderPanel);
        dialog.setContentPane(mainPanel);
        dialog.setModal(true);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}