/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.connect;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.widgets.EditablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * <b>AbstractConnectionPanel</b> is the abstract panel for all specific panels
 * for setting connection properties.
 */
@SuppressWarnings("serial")
public abstract class AbstractConnectionPanel extends EditablePanel {

    /**
     * Main panel.
     */
    protected LabelledItemPanel mainPanel = new LabelledItemPanel();

    /**
     * Default constructor for connection panels.
     */
    public AbstractConnectionPanel() {
        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Populates fields with data from a connect neurons object.
     */
    public abstract void fillFieldValues();

    /**
     * Commit any changes made in the panel to the connection object being
     * represented.
     *
     * @return a success condition.  Not used by all connection panels.
     */
    public abstract boolean commitChanges();

    /**
     * Apply this connection panel's connection to the provided source and
     * target neurons.
     *
     * @param source the source neurons
     * @param target the target neurons
     * @return the newly created synapses
     */
    public abstract List<Synapse> applyConnection(List<Neuron> source, List<Neuron> target);

    /**
     * Get the underlying connection object.
     *
     * @return the ConnectNeurons object
     */
    public abstract ConnectNeurons getConnection();

}
