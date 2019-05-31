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

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.network.trainers.Trainer.DataNotInitializedException;
import org.simbrain.util.ResourceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Simple trainer control panel for cases where the trainer just involves an
 * apply and randomize button.
 *
 * @author Jeff Yoshimi
 */
public class SimpleTrainerControlPanel extends JPanel {

    /**
     * Reference to trainer object.
     */
    private Trainer trainer;

    /**
     * Reference to network panel.
     */
    private final NetworkPanel panel;

    /**
     * Construct the panel with a trainer specified.
     *
     * @param networkPanel the parent network panel
     * @param trainer      the trainer this panel represents
     */
    public SimpleTrainerControlPanel(final NetworkPanel networkPanel, final Trainer trainer) {

        this.trainer = trainer;
        this.panel = networkPanel;
        init();
    }

    /**
     * Initialize the panel.
     */
    public void init() {
        Box mainPanel = Box.createHorizontalBox();
        mainPanel.add(new JLabel("Train network"));
        mainPanel.add(Box.createHorizontalStrut(5));
        JButton applyButton = new JButton(applyAction);
        applyButton.setHideActionText(true);
        mainPanel.add(applyButton);
        add(mainPanel);

    }

    /**
     * Apply training algorithm.
     */
    private Action applyAction = new AbstractAction() {

        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Play.png"));
            putValue(NAME, "Train network");
            // putValue(SHORT_DESCRIPTION, "Import table from .csv");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            if (trainer == null) {
                return;
            }
            try {
                trainer.apply();
            } catch (DataNotInitializedException e) {
                JOptionPane.showOptionDialog(null, e.getMessage(), "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            }

        }

    };


}