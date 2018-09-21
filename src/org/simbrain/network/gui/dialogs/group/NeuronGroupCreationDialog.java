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

import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;

/**
 * <b>NeuronGroupCreationDialog</b> is a dialog box for creating a bare neuron
 * group.
 */
public class NeuronGroupCreationDialog extends StandardDialog {

    /**
     * The panel representing the neuron group to be created.
     */
    private final AnnotatedPropertyEditor neuronGroupPanel;

    /**
     * Network Panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * "Shell" neuron group that can be used to create the real neuron group.
     */
    private final NeuronGroup.NeuronGroupCreator ngCreator;

    /**
     * This method is the default constructor.
     *
     * @param panel the parent network panel
     */
    public NeuronGroupCreationDialog(final NetworkPanel panel) {
        this.networkPanel = panel;
        ngCreator = new NeuronGroup.NeuronGroupCreator(panel.getNetwork());
        neuronGroupPanel = new AnnotatedPropertyEditor(ngCreator);
        setContentPane(neuronGroupPanel);
        setTitle("New Neuron Group");
        setAlwaysOnTop(true);
    }

    @Override
    protected void closeDialogOk() {
        neuronGroupPanel.commitChanges();
        networkPanel.getNetwork().addGroup(ngCreator.create());
        networkPanel.repaint();
        super.closeDialogOk();
    }

}
