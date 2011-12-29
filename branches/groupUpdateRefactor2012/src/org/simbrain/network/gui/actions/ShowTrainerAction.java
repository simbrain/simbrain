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
package org.simbrain.network.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.NetworkSelectionEvent;
import org.simbrain.network.gui.NetworkSelectionListener;
import org.simbrain.network.gui.trainer.TrainerPanel;
import org.simbrain.network.trainers.Backprop;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.genericframe.GenericFrame;

/**
 * Show Trainer object for training selected source and target neurons.
 */
public final class ShowTrainerAction extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /**
     * Construct action.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public ShowTrainerAction(final NetworkPanel networkPanel) {

        super("Show Trainer...");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Trainer.png"));

        updateAction();
        // add a selection listener to update state based on selection
        networkPanel.addSelectionListener(new NetworkSelectionListener() {

            /** @see NetworkSelectionListener */
            public void selectionChanged(final NetworkSelectionEvent event) {
                updateAction();
            }
        });
    }

    /**
     * Only enable the action if there is at least one source and one target
     * neuron.
     */
    private void updateAction() {
        boolean atLeastOneSourceSelected = (networkPanel
                .getSourceModelNeurons().size() > 0);
        boolean atLeastOneTargetSelected = (networkPanel
                .getSelectedModelElements().size() > 0);
        if (atLeastOneSourceSelected && atLeastOneTargetSelected) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        Trainer trainer = new Trainer(networkPanel.getRootNetwork(),
                networkPanel.getSourceModelNeurons(),
                networkPanel.getSelectedModelNeurons(), new Backprop());
        TrainerPanel trainerPanel = new TrainerPanel(networkPanel, trainer);
        GenericFrame frame = networkPanel.displayPanel(trainerPanel);
        trainerPanel.setFrame(frame);
    }
}