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
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.trainers.IterableTrainer;
import org.simbrain.util.SimpleFrame;

/**
 * Combines an iterative control panel and training set panel to provide the
 * primary access point for training neural networks using iterative methods.
 * Currently used by backprop and lms.
 *
 * TODO: This is no longer being used and should be considered for deletion (JKY, 11/14)
 *
 * @author Jeff Yoshimi
 */
public class IterativeTrainingPanel extends JPanel {

    /** Reference to Iterative Controls panel. */
    private final IterativeControlsPanel iterativeControls;

    /** Reference to training set panel. */
    private final TrainingSetPanel trainingSetPanel;

    /** Parent frame. */
    private Window parentFrame;

    /**
     * Construct a rule chooser panel.
     *
     * @param networkPanel the parent network panel
     * @param trainer the trainer this panel represents
     */
    public IterativeTrainingPanel(final NetworkPanel networkPanel,
        final IterableTrainer trainer) {

        iterativeControls = new IterativeControlsPanel(networkPanel, trainer);

        // Training Set Panel
        if (trainer != null) {
            trainingSetPanel = new TrainingSetPanel(
                trainer.getTrainableNetwork(), 3);
        } else {
            trainingSetPanel = new TrainingSetPanel();
        }

        // Layout the whole panel
        setLayout(new GridBagLayout());
        GridBagConstraints wholePanelConstraints = new GridBagConstraints();
        // Control Panel
        wholePanelConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        wholePanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        wholePanelConstraints.insets = new Insets(10, 10, 10, 10);
        wholePanelConstraints.weightx = 0;
        wholePanelConstraints.weighty = 0.5;
        wholePanelConstraints.gridx = 0;
        wholePanelConstraints.gridy = 0;
        add(iterativeControls, wholePanelConstraints);
        // Training Set
        wholePanelConstraints.anchor = GridBagConstraints.PAGE_START;
        wholePanelConstraints.fill = GridBagConstraints.BOTH;
        wholePanelConstraints.insets = new Insets(10, 10, 10, 10);
        wholePanelConstraints.weightx = 1;
        wholePanelConstraints.weighty = 0.5;
        wholePanelConstraints.gridx = 1;
        wholePanelConstraints.gridy = 0;
        add(trainingSetPanel, wholePanelConstraints);

    }

    /**
     * @param parentFrame the parentFrame to set
     */
    public void setFrame(Window parentFrame) {
        this.parentFrame = parentFrame;
        trainingSetPanel.setFrame(parentFrame);
    }

    /**
     * Test method for GUI layout.
     * @param args
     */
    public static void main(String args[]) {
        IterativeTrainingPanel test = new IterativeTrainingPanel(null, null);
        // test.errorBar.setValue(5);
        SimpleFrame.displayPanel(test);
    }

}