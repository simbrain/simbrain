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

import javax.swing.Action;
import javax.swing.JButton;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.trainer.IterativeControlsPanel;
import org.simbrain.network.subnetworks.SimpleRecurrentNetwork;
import org.simbrain.network.trainers.SRNTrainer;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * Creates a GUI dialog to set the parameters for and then build a simple
 * recurrent network.
 *
 * @author Jeff Yoshimi
 */
public class SRNEditorDialog extends SupervisedTrainingDialog {

    /** Reference to the SRN network being edited. */
    private SimpleRecurrentNetwork srn;

    /**
     * Constructs a labeled item panel dialog for the creation of a simple
     * recurrent network.
     *
     * @param panel the network panel the SRN will be tied to
     */
    public SRNEditorDialog(final NetworkPanel panel, SimpleRecurrentNetwork srn) {
        super(panel, srn);
        this.srn = srn;
        init();
        initDefaultTabs();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        setTitle("Edit Simple Recurrent Network");

        // Trainer tab
        SRNTrainer trainer = new SRNTrainer(srn);
        IterativeControlsPanel iterativeControls = new IterativeControlsPanel(
                networkPanel, trainer);
        tabbedPane.addTab("Train", iterativeControls);

        // Set up help
        Action helpAction = new ShowHelpAction(
                "Pages/Network/network/srn.html");
        addButton(new JButton(helpAction));
    }

}
