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

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.JButton;

import org.simbrain.network.gui.NetworkGuiSettings;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.NetworkDialog;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.SimbrainPreferences.PropertyNotFoundException;

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
    public DesktopNetworkDialog(NetworkPanel np) {
        super(np);

        defaultButton.addActionListener(this);
        addButton(defaultButton);

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == defaultButton) {
            SimbrainPreferences.restoreDefaultSetting("networkBackgroundColor");
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
            this.returnToCurrentPrefs();
        }
        super.actionPerformed(e);
    }

    /**
     * Restores the changed fields to their previous values. Called when user
     * cancels out of the dialog.
     */
    public void returnToCurrentPrefs() {
        try {
            NetworkGuiSettings.setBackgroundColor(new Color(SimbrainPreferences
                    .getInt("networkBackgroundColor")));
            NetworkGuiSettings.setHotColor(SimbrainPreferences
                    .getFloat("networkHotNodeColor"));
            NetworkGuiSettings.setCoolColor(SimbrainPreferences
                    .getFloat("networkCoolNodeColor"));
            NetworkGuiSettings.setExcitatoryColor(new Color(SimbrainPreferences
                    .getInt("networkExcitatorySynapseColor")));
            NetworkGuiSettings.setInhibitoryColor(new Color(SimbrainPreferences
                    .getInt("networkInhibitorySynapseColor")));
            NetworkGuiSettings.setSpikingColor(new Color(SimbrainPreferences
                    .getInt("networkSpikingColor")));
            NetworkGuiSettings.setZeroWeightColor(new Color(SimbrainPreferences
                    .getInt("networkZeroWeightColor")));
            NetworkGuiSettings.setMaxDiameter(SimbrainPreferences
                    .getInt("networkSynapseMaxSize"));
            NetworkGuiSettings.setMinDiameter(SimbrainPreferences
                    .getInt("networkSynapseMinSize"));
            NetworkGuiSettings.setNudgeAmount(SimbrainPreferences
                    .getDouble("networkNudgeAmount"));
            networkPanel.resetColors();
            setIndicatorColor();
            networkPanel.resetSynapseDiameters();
            fillFieldValues();
        } catch (PropertyNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets selected preferences as user defaults to be used each time program
     * is launched. Called when "ok" is pressed.
     */
    public void setAsDefault() {
        SimbrainPreferences.putInt("networkBackgroundColor",
                NetworkGuiSettings.getBackgroundColor().getRGB());
        SimbrainPreferences.putFloat("networkHotNodeColor",
                NetworkGuiSettings.getHotColor());
        SimbrainPreferences.putFloat("networkCoolNodeColor",
                NetworkGuiSettings.getCoolColor());
        SimbrainPreferences.putInt("networkExcitatorySynapseColor",
                NetworkGuiSettings.getExcitatoryColor().getRGB());
        SimbrainPreferences.putInt("networkInhibitorySynapseColor",
                NetworkGuiSettings.getExcitatoryColor().getRGB());
        SimbrainPreferences.putInt("networkSpikingColor",
                NetworkGuiSettings.getSpikingColor().getRGB());
        SimbrainPreferences.putInt("networkZeroWeightColor",
                NetworkGuiSettings.getZeroWeightColor().getRGB());
        SimbrainPreferences.putInt("networkSynapseMinSize",
                NetworkGuiSettings.getMinDiameter());
        SimbrainPreferences.putInt("networkSynapseMaxSize",
                NetworkGuiSettings.getMaxDiameter());
        SimbrainPreferences.putDouble("networkNudgeAmount",
                NetworkGuiSettings.getNudgeAmount());
    }

}
