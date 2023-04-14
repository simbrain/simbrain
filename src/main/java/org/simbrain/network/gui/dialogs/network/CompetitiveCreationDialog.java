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
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.subnetworks.CompetitiveGroup;
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
     * Competitive properties panel.
     */
    private AnnotatedPropertyEditor competitivePanel;

    /**
     * Creator object
     */
    private CompetitiveNetwork.CompetitiveCreator
            cc = new  CompetitiveNetwork.CompetitiveCreator();

    /**
     * Layout object.
     */
    private Layout layout = new GridLayout();

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
    public CompetitiveCreationDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        this.networkPanel = networkPanel;
        setTitle("New Competitive Network");
        competitivePanel = new AnnotatedPropertyEditor(cc);
        tabLogic.add(competitivePanel);
        layoutPanel = new AnnotatedPropertyEditor(layout);
        layout  = CompetitiveGroup.DEFAULT_LAYOUT;
        tabLayout.add(layoutPanel);
        tabbedPane.addTab("Logic", tabLogic);
        tabbedPane.addTab("Layout", layoutPanel);
        setContentPane(tabbedPane);

        Action helpAction = new ShowHelpAction("Pages/Network/network/competitive.html");
        addButton(new JButton(helpAction));

    }

    /**
     * Called when dialog closes.
     */
    @Override
    protected void closeDialogOk() {
        competitivePanel.commitChanges();
        CompetitiveNetwork cn = cc.create(networkPanel.getNetwork());
        layoutPanel.commitChanges();
        cn.getCompetitive().setLayout(layout);
        cn.getCompetitive().applyLayout(-5, -85);
        networkPanel.getNetwork().addNetworkModelAsync(cn);
        super.closeDialogOk();
    }

}