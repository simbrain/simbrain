///*
// * Part of Simbrain--a java-based neural network kit
// * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 2 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package org.simbrain.network.gui.dialogs.network;
//
//import org.simbrain.network.gui.NetworkPanel;
//import org.simbrain.network.gui.dialogs.network.CompetitivePropertiesPanel.CompetitivePropsPanelType;
//import org.simbrain.network.layouts.Layout;
//import org.simbrain.network.subnetworks.CompetitiveGroup;
//import org.simbrain.util.StandardDialog;
//import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
//import org.simbrain.util.widgets.ShowHelpAction;
//
//import javax.swing.*;
//
///**
// * <b>CompetitiveDialog</b>. Create competitive networks.
// */
//public class CompetitiveGroupCreationDialog extends StandardDialog {
//
//    /**
//     * Tabbed pane.
//     */
//    private JTabbedPane tabbedPane = new JTabbedPane();
//
//    /**
//     * Logic tab panel.
//     */
//    private JPanel tabLogic = new JPanel();
//
//    /**
//     * Layout tab panel.
//     */
//    private JPanel tabLayout = new JPanel();
//
//    /**
//     * Logic panel.
//     */
//    private CompetitivePropertiesPanel compPropertiesPanel;
//
//
//    private Layout.LayoutObject layoutObject = new Layout.LayoutObject();
//
//    /**
//     * Layout panel.
//     */
//    private AnnotatedPropertyEditor layoutPanel;
//
//    /**
//     * Network Panel.
//     */
//    private NetworkPanel networkPanel;
//
//    /**
//     * This method is the default constructor.
//     *
//     * @param networkPanel Network panel
//     */
//    public CompetitiveGroupCreationDialog(final NetworkPanel networkPanel) {
//        this.networkPanel = networkPanel;
//        init();
//    }
//
//    /**
//     * Initializes all components used in dialog.
//     */
//    private void init() {
//
//        setTitle("New Competitive Group");
//        compPropertiesPanel = CompetitivePropertiesPanel.createCompetitivePropertiesPanel(networkPanel, CompetitivePropsPanelType.CREATE_GROUP);
//
//        // Set up tab panels
//        tabLogic.add(compPropertiesPanel);
//        layoutPanel = new AnnotatedPropertyEditor(layoutObject);
//        layoutObject.setLayout(CompetitiveGroup.DEFAULT_LAYOUT);
//        tabLayout.add(layoutPanel);
//        tabbedPane.addTab("Logic", tabLogic);
//        tabbedPane.addTab("Layout", layoutPanel);
//        setContentPane(tabbedPane);
//
//        // Help action
//        Action helpAction = new ShowHelpAction(compPropertiesPanel.getHelpPath());
//        addButton(new JButton(helpAction));
//
//    }
//
//    /**
//     * Called when dialog closes.
//     */
//    @Override
//    protected void closeDialogOk() {
//        // TODO
//        //compPropertiesPanel.commitChanges();
//        //layoutPanel.commitChanges();
//        //CompetitiveGroup competitive = (CompetitiveGroup) compPropertiesPanel.getGroup();
//        //competitive.setLayout(layoutObject.getLayout());
//        //competitive.applyLayout();
//        //networkPanel.getPlacementManager().addNewModelObject(competitive);
//        //networkPanel.getNetwork().addGroup(competitive);
//        //layoutPanel.commitChanges();
//        //networkPanel.repaint();
//        //super.closeDialogOk();
//    }
//
//}
