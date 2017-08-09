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
package org.simbrain.network.desktop;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Network;
import org.simbrain.network.gui.EditMode;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.NetworkDialog;
import org.simbrain.network.gui.dialogs.connect.QuickConnectPreferencesPanel;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.util.SimbrainPreferences;

/**
 * Overrides the network dialog box to add features that don't work on applets,
 * but that work in the desktop, in particular anything reliant on user
 * preferences.
 *
 * @author Jeff Yoshimi
 *
 */
public class DesktopNetworkDialog extends NetworkDialog {

    /** Restore defaults button. */
    private JButton defaultButton = new JButton("Restore defaults");

    /**
     * Construct the dialog.
     *
     * @param np parent network panel.
     */
    public DesktopNetworkDialog(final NetworkPanel np) {
        super(np);
        this.addButton(defaultButton);
        defaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                restoreDefaults();
            }
        });
        defaultButton
                .setToolTipText("Restore properties (currently only on main panel) to their default values");
    }

    /**
     * Restores all values to their default settings.
     */
    public void restoreDefaults() {
        SimbrainPreferences.restoreDefaultSetting("networkBackgroundColor");
        SimbrainPreferences.restoreDefaultSetting("networkWandRadius");
        SimbrainPreferences.restoreDefaultSetting("networkHotNodeColor");
        SimbrainPreferences.restoreDefaultSetting("networkCoolNodeColor");
        SimbrainPreferences
                .restoreDefaultSetting("networkExcitatorySynapseColor");
        SimbrainPreferences
                .restoreDefaultSetting("networkInhibitorySynapseColor");
        SimbrainPreferences.restoreDefaultSetting("networkSpikingColor");
        SimbrainPreferences.restoreDefaultSetting("networkZeroWeightColor");
        SimbrainPreferences.restoreDefaultSetting("networkSynapseMaxSize");
        SimbrainPreferences.restoreDefaultSetting("networkSynapseMinSize");
        SimbrainPreferences.restoreDefaultSetting("networkNudgeAmount");
        SimbrainPreferences.restoreDefaultSetting("networkSynapseVisibilityThreshold");
        SimbrainPreferences.restoreDefaultSetting("selfConnectionAllowed");

        //Make sure new settings are visible
        ((NetworkPanelDesktop) networkPanel).applyUserPrefsToNetwork();
        networkPropertiesPanel.fillFieldValues();
        networkPropertiesPanel.setIndicatorColor();
        quickConnectPanel.fillFieldValues();
        networkPanel.resetColors();

    }

    /**
     * Sets selected preferences as user defaults. Called when "ok" is pressed.
     * Pulls values from actual Simbrain objects, since the method assumes that
     * before this all dialog values will have been applied via commitChanges.
     */
    public void setDefaults() {
        SimbrainPreferences.putInt("networkBackgroundColor", NetworkPanel
                .getBackgroundColor().getRGB());
        SimbrainPreferences.putDouble("networkNudgeAmount",
                NetworkPanel.getNudgeAmount());
        SimbrainPreferences.putInt("networkSynapseVisibilityThreshold",
                Network.getSynapseVisibilityThreshold());
        SimbrainPreferences.putInt("networkWandRadius",
                EditMode.getWandRadius());
        SimbrainPreferences.putFloat("networkHotNodeColor",
                NeuronNode.getHotColor());
        SimbrainPreferences.putFloat("networkCoolNodeColor",
                NeuronNode.getCoolColor());
        SimbrainPreferences.putInt("networkSpikingColor", NeuronNode
                .getSpikingColor().getRGB());
        SimbrainPreferences.putInt("networkExcitatorySynapseColor",
                SynapseNode.getExcitatoryColor().getRGB());
        SimbrainPreferences.putInt("networkInhibitorySynapseColor",
                SynapseNode.getInhibitoryColor().getRGB());
        SimbrainPreferences.putInt("networkZeroWeightColor", SynapseNode
                .getZeroWeightColor().getRGB());
        SimbrainPreferences.putInt("networkSynapseMinSize",
                SynapseNode.getMinDiameter());
        SimbrainPreferences.putInt("networkSynapseMaxSize",
                SynapseNode.getMaxDiameter());
        SimbrainPreferences.putBoolean("selfConnectionAllowed",
                AllToAll.isSelfConnectionAllowed());
    }

    /**
     * Commit changes to the network (in superclass) and set all defaults.
     */
    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        setDefaults();
    }

}
