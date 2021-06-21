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

import org.simbrain.network.connectors.Connectable;
import org.simbrain.network.connectors.Connector;
import org.simbrain.network.connectors.WeightMatrix;
import org.simbrain.network.connectors.ZoeZone;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;

import java.util.List;

/**
 * Dialog for creating connectors.
 *
 * @author Jeff Yoshimi
 */
public final class ConnectorDialog extends StandardDialog {

    /**
     * Parent network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Property editor.
     */
    private AnnotatedPropertyEditor mainPanel;

    /**
     * Special object to create new connectors.
     */
    private Connector.ConnectorCreator connectorCreator;

    /**
     * List of incoming sources to this connector.
     */
    private List<Connectable> sources;

    /**
     * List of outgoing targets of this connector.
     */
    private List<Connectable> targets;

    /**
     * For creating a new connetor
     */
    public ConnectorDialog(final NetworkPanel np, List<Connectable> sources, List<Connectable> targets) {

        networkPanel = np;
        setTitle("Create Connector");
        this.sources = sources;
        this.targets = targets;

        connectorCreator = new Connector.ConnectorCreator();
        mainPanel = new AnnotatedPropertyEditor(connectorCreator);

        setContentPane(mainPanel);

        // TODO
        // Set up help button
        // Action helpAction = new ShowHelpAction("Pages/Network/groups/NeuronGroup.html");
        // addButton(new JButton(helpAction));
    }

    /**
     * Commit changes.
     */
    public void commitChanges() {
        var net = networkPanel.getNetwork();
        var widget = (Connector.ConnectorEnum) mainPanel.getWidget("Connector type").getWidgetValue();
        for (Connectable source: sources) {
            for (Connectable target: targets) {
                if (widget == Connector.ConnectorEnum.DENSE) {
                    net.addNetworkModel(new WeightMatrix(net, source, target));
                } else if (widget == Connector.ConnectorEnum.ZOE) {
                    net.addNetworkModel(new ZoeZone(net, source, target));
                }
            }
        }
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

}