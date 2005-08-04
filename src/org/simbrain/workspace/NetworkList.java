/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.workspace;

import java.awt.Color;
import java.util.ArrayList;

import org.simbrain.network.NetworkFrame;
import org.simbrain.network.NetworkPreferences;
import org.simbrain.network.pnodes.*;

/**
 * @author yoshimi
 * 
 * Methods that apply to all workspace networks are handled here.
 */
public class NetworkList extends ArrayList {

    /**
     * Repaint all open network panels. Useful when workspace changes happen
     * that need to be broadcast; also essential when default workspace is
     * initially opened.
     */
    public void repaintAllNetworkPanels() {

        for (int j = 0; j < this.size(); j++) {
            NetworkFrame net = (NetworkFrame) this.get(j);
            net.getNetPanel().repaint();
        }

    }

    /**
     * Update background colors of all networks
     */
    public void updateBackgrounds(Color theColor) {

        for (int j = 0; j < this.size(); j++) {
            NetworkFrame net = (NetworkFrame) this.get(j);
            net.getNetPanel().setBackground(theColor);
        }

    }

    /**
     * Update line colors of all networks
     */
    public void updateLines(Color theColor) {

        PNodeLine.setLineColor(theColor);
        for (int j = 0; j < this.size(); j++) {
            NetworkFrame net = (NetworkFrame) this.get(j);
            net.getNetPanel().resetLineColors();
        }
    }

    /**
     * Update hot node colors of all networks
     */
    public void updateHotNode(float theColor) {

        PNodeNeuron.setHotColor(theColor);
        for (int j = 0; j < this.size(); j++) {
            NetworkFrame net = (NetworkFrame) this.get(j);
            net.getNetPanel().renderObjects();
        }
    }

    /**
     * Update cool node colors of all networks
     */
    public void updateCoolNode(float theColor) {

        PNodeNeuron.setCoolColor(theColor);
        for (int j = 0; j < this.size(); j++) {
            NetworkFrame net = (NetworkFrame) this.get(j);
            net.getNetPanel().renderObjects();
        }
    }

    /**
     * Update excitatory weight colors of all networks
     */
    public void updateExcitatory(Color theColor) {

        PNodeWeight.setExcitatoryColor(theColor);
        for (int j = 0; j < this.size(); j++) {
            NetworkFrame net = (NetworkFrame) this.get(j);
            net.getNetPanel().renderObjects();
        }
    }

    /**
     * Update inhibitory weight colors of all networks
     */
    public void updateInhibitory(Color theColor) {

        PNodeWeight.setInhibitoryColor(theColor);
        for (int j = 0; j < this.size(); j++) {
            NetworkFrame net = (NetworkFrame) this.get(j);
            net.getNetPanel().renderObjects();
        }
    }

    /**
     * Updates weight size max of all networks
     */
    public void updateWeightSizeMax(int slider) {

        PNodeWeight.setMaxRadius(slider);
        for (int j = 0; j < this.size(); j++) {
            NetworkFrame net = (NetworkFrame) this.get(j);
            net.getNetPanel().renderObjects();
        }
    }

    /**
     * Updates weight size min of all networks
     */
    public void updateWeightSizeMin(int slider) {

        PNodeWeight.setMinRadius(slider);
        for (int j = 0; j < this.size(); j++) {
            NetworkFrame net = (NetworkFrame) this.get(j);
            net.getNetPanel().renderObjects();
        }
    }

    /**
     * Updates nudge amount of all networks
     */
    public void updateNudge(double nudge) {

        for (int j = 0; j < this.size(); j++) {
            NetworkFrame net = (NetworkFrame) this.get(j);
            net.getNetPanel().setNudgeAmount(nudge);
        }
    }

    /**
     * Updates indent network files of all networks
     */
    public void updateUsingIndent(boolean indent) {

        for (int j = 0; j < this.size(); j++) {
            NetworkFrame net = (NetworkFrame) this.get(j);
            net.getNetPanel().getSerializer().setUsingTabs(indent);
        }
    }

    /**
     * Restore all User Preference-base network properties to their default
     * values
     *  
     */
    public void restoreDefaults() {
        updateBackgrounds(new Color(NetworkPreferences.getBackgroundColor()));
        updateLines(new Color(NetworkPreferences.getLineColor()));
        updateHotNode(NetworkPreferences.getHotColor());
        updateCoolNode(NetworkPreferences.getCoolColor());
        updateExcitatory(new Color(NetworkPreferences.getExcitatoryColor()));
        updateInhibitory(new Color(NetworkPreferences.getInhibitoryColor()));
        updateWeightSizeMax(NetworkPreferences.getMaxRadius());
        updateWeightSizeMin(NetworkPreferences.getMinRadius());
        updateNudge(NetworkPreferences.getNudgeAmount());
        updateUsingIndent(NetworkPreferences.getUsingIndent());
    }

    /**
     * @return a list of networks which have changed since last save
     */
    public ArrayList getChanges() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < size(); i++) {
            NetworkFrame test = (NetworkFrame) get(i);
            if (test.isChangedSinceLastSave()) {
                ret.add(test);
            }
        }

        return ret;

    }
}
