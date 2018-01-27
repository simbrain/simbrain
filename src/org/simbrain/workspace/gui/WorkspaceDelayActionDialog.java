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

import java.awt.*;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.util.StandardDialog;
import org.simbrain.workspace.Workspace;

/**
 * <b>WorkspaceDelayActionDialog</b> is a dialog box for setting the workspace update delay.
 */
public class WorkspaceDelayActionDialog extends StandardDialog  {

    /** Reference to workspace. **/
    private Workspace workspace;

    /**
     * This method is the default constructor.
     *
     * @param workspace reference to parent workspace.
     */
    public WorkspaceDelayActionDialog(Workspace workspace) {
        this.workspace = workspace;
        setupEditorPanel();
        setModal(true);
        setLocationRelativeTo(null);
        pack();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void setupEditorPanel() {
        // Initialize Dialog
        setTitle("Workspace Update Delay");
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        setContentPane(panel);

        JLabel delayLabel = new JLabel("Delay (ms)");
        JSlider delaySlider = new JSlider(JSlider.HORIZONTAL, 0, 500, workspace.getUpdateDelay());
        JLabel delayValueLabel = new JLabel("" + workspace.getUpdateDelay());
        delayValueLabel.setPreferredSize(new Dimension(50, 10));
        delaySlider.setToolTipText("Use this to delay simulation speed. "
                + "This will make it easier to see very fast updates.");
        delaySlider.setMajorTickSpacing(100);
        delaySlider.setPaintTicks(true);
        delaySlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                workspace.setUpdateDelay(source.getValue());
                delayValueLabel.setText("" + source.getValue());
            }
        });
        panel.add(delayLabel);
        panel.add(delaySlider);
        panel.add(delayValueLabel);
        panel.setBorder(null);
    }

}
