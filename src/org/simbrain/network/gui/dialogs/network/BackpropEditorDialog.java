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
package org.simbrain.network.gui.dialogs.network;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.trainer.IterativeControlsPanel;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.trainers.*;

/**
 * <b>BackpropDialog</b> is a dialog box for editing a Backprop network.
 */
public class BackpropEditorDialog extends SupervisedTrainingDialog {

    /** Reference to the backprop network being edited. */
    private BackpropNetwork backprop;
    
    /**
     * Make it easy to switch between the new, experimental trainer
     * (BackpropTrainer2), and the old one. Once that's stabilized this code can
     * be removed.
     */
    private boolean useExperimentalTrainer = true;
    private IterableTrainer currentTrainer;

    /**
     * Default constructor.
     *
     * @param np parent panel
     * @param backprop edited network
     */
    public BackpropEditorDialog(final NetworkPanel np,
        final BackpropNetwork backprop) {
        super(np, backprop);
        this.backprop = backprop;
        init();
        initDefaultTabs();
        updateData();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        setTitle("Edit Backprop Network");

        // Trainer tab
        if(useExperimentalTrainer) {
            currentTrainer = new BackpropTrainer2(backprop);
        } else {
            currentTrainer = new BackpropTrainer(backprop);
        }
        IterativeControlsPanel iterativeControls = new IterativeControlsPanel(
            networkPanel, currentTrainer);
        addTab("Train", iterativeControls);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                currentTrainer.commitChanges();
            }
        });
    }

    @Override
    protected void stopTrainer() {
        if (currentTrainer != null) {
            currentTrainer.setUpdateCompleted(true);
            currentTrainer.commitChanges();
        }
    }
    
    @Override
    void updateData() {
        currentTrainer.initData();
    }
}
