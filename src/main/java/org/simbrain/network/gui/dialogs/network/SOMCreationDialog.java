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
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.subnetworks.SOMGroup;
import org.simbrain.network.subnetworks.SOMNetwork;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;

/**
 * <b>SOMDialog</b> is used as an assistant to create SOM networks.
 */
public class SOMCreationDialog extends StandardDialog {

    /**
     * Tabbed pane.
     */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Logic tab panel.
     */
    private JPanel tabLogic = new JPanel();

    /**
     * Layout tab panel.
     */
    private JPanel tabLayout = new JPanel();

    /**
     * SOM properties panel.
     */
    private AnnotatedPropertyEditor somPanel;

    /**
     * Creator object
     */
    private SOMNetwork.SOMCreator sc = new  SOMNetwork.SOMCreator();

    /**
     * Layout object.
     */
    private Layout.LayoutObject layoutObject = new Layout.LayoutObject();

    /**
     * Layout panel.
     */
    private AnnotatedPropertyEditor layoutPanel;

    /**
     * Network Panel.
     */
    private NetworkPanel networkPanel;

    /**
     * This method is the default constructor.
     *
     * @param networkPanel Network panel
     */
    public SOMCreationDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        setTitle("New SOM Network");
        somPanel = new AnnotatedPropertyEditor(sc);
        tabLogic.add(somPanel);
        layoutPanel = new AnnotatedPropertyEditor(layoutObject);
        layoutObject.setLayout(SOMGroup.DEFAULT_LAYOUT);
        tabLayout.add(layoutPanel);
        tabbedPane.addTab("Logic", tabLogic);
        tabbedPane.addTab("Layout", layoutPanel);
        setContentPane(tabbedPane);

        Action helpAction = new ShowHelpAction("Pages/Network/network/somnetwork.html");
        addButton(new JButton(helpAction));
    }

    /**
     * Called when dialog closes.
     */
    @Override
    protected void closeDialogOk() {
        somPanel.commitChanges();
        SOMNetwork som = sc.create(networkPanel.getNetwork());
        layoutPanel.commitChanges();
        som.getSom().setLayout(layoutObject.getLayout());
        som.getSom().applyLayout();
        networkPanel.getNetwork().addSubnetwork(som);
        networkPanel.getPlacementManager().addNewModelObject(som);
        super.closeDialogOk();
    }

}
