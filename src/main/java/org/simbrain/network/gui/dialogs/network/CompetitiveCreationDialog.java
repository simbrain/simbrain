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

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.subnetworks.CompetitiveNetwork;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;

/**
 * <b>CompetitiveDialog</b> is used as an assistant to create Competitive
 * networks.
 */
public class CompetitiveCreationDialog extends StandardDialog {

    /**
     * Competitive properties panel.
     */
    private AnnotatedPropertyEditor competitivePanel;

    /**
     * Creator object
     */
    private CompetitiveNetwork.CompetitiveCreator
            cc = new  CompetitiveNetwork.CompetitiveCreator();

    private NetworkPanel networkPanel;

    public CompetitiveCreationDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        setTitle("New Competitive Network");
        competitivePanel = new AnnotatedPropertyEditor(cc);
        setContentPane(competitivePanel);

        Action helpAction = new ShowHelpAction("Pages/Network/network/competitive.html");
        addButton(new JButton(helpAction));

    }

    @Override
    protected void closeDialogOk() {
        competitivePanel.commitChanges();
        CompetitiveNetwork cn = cc.create();
        networkPanel.getNetwork().addNetworkModelAsync(cn);
        super.closeDialogOk();
    }

}