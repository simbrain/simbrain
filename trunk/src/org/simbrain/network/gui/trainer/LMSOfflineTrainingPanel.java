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
package org.simbrain.network.gui.trainer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.util.genericframe.GenericFrame;

/**
 * Combines an LMS Offline control panel and training set panel to provide the
 * primary access point for training neural networks using offline least mean
 * squares.
 *
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class LMSOfflineTrainingPanel extends JPanel {

    /** Reference to trainer object. */
    private LMSOffline trainer;

    /** Reference to the controls panel. */
    private final LMSOfflineControlPanel controlPanel;

    /** Reference to training set panel. */
    private final TrainingSetPanel trainingSetPanel;

    /** Parent frame. */
    private GenericFrame parentFrame;

    /**
     * Build the panel.
     *
     * @param panel the parent network panel
     * @param trainer the LMSOffline trainer to represent
     */
    public LMSOfflineTrainingPanel(final NetworkPanel panel,
            final LMSOffline trainer) {

        this.trainer = trainer;
        controlPanel = new LMSOfflineControlPanel(panel, trainer);

        // Set up main controls
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controls"));

        // Training Set Panel
        trainingSetPanel = new TrainingSetPanel(trainer.getTrainableNetwork(),
                3);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // Control Panel
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 0;
        gbc.weighty = 0.5;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(controlPanel, gbc);
        // Training Set
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1;
        gbc.weighty = 0.5;
        gbc.gridx = 1;
        gbc.gridy = 0;
        add(trainingSetPanel, gbc);
    }

    /**
     * @param parentFrame the parentFrame to set
     */
    public void setFrame(GenericFrame parentFrame) {
        this.parentFrame = parentFrame;
        controlPanel.setFrame(parentFrame);
        trainingSetPanel.setFrame(parentFrame);
    }

}
