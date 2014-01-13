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
package org.simbrain.network.gui.dialogs.group;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>NeuronGroupCreationDialog</b> is a dialog box for creating a bare neuron
 * group.
 */
@SuppressWarnings("serial")
public class NeuronGroupCreationDialog extends StandardDialog {

    /** The panel representing the neuron group to be created. */
    private final NeuronGroupPanel neuronGroupPanel;

    /** Network Panel. */
    private final NetworkPanel networkPanel;

    /**
     * This method is the default constructor.
     *
     * @param panel the parent network panel
     */
    public NeuronGroupCreationDialog(final NetworkPanel panel) {
        this.networkPanel = panel;
        neuronGroupPanel = new NeuronGroupPanel(panel, this);
        setContentPane(neuronGroupPanel);
        setTitle("New Neuron Group");
    }

    /**
     * Called when dialog closes.
     */
    @Override
    protected void closeDialogOk() {
        boolean success = neuronGroupPanel.commitChanges();
        if (!success) { // Something went wrong...
            return; // Do not close the panel or add the group (do not pass go)
        }
        networkPanel.getNetwork().addGroup(neuronGroupPanel.getGroup());
        networkPanel.repaint();
        super.closeDialogOk();
    }

}
