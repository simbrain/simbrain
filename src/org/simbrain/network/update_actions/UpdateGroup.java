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
package org.simbrain.network.update_actions;

import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;

/**
 * Loose neurons (neurons not in groups) are updated in accordance with an
 * ordered priority list.
 *
 * @author jyoshimi
 */
public class UpdateGroup implements NetworkUpdateAction {

    /** Reference to group. */
    private final Group group;

    /**
     * @param group group to update
     */
    public UpdateGroup(Group group) {
        this.group = group;
    }

    @Override
    public void invoke() {
        group.update();
    }

    @Override
    public String getDescription() {
        String groupUpdateDescription = group.getUpdateMethodDesecription();
        if (group instanceof Subnetwork) {
            return "Subnetwork:" + group.getLabel() + " ("
                    + groupUpdateDescription + ")";
        } else if (group instanceof NeuronGroup) {
            return "NeuronGroup:" + group.getLabel() + " ("
                    + groupUpdateDescription + ")";
        } else if (group instanceof SynapseGroup) {
            return "SynapseGroup:" + group.getLabel() + " ("
                    + groupUpdateDescription + ")";
        } else {
            return "Group:" + group.getLabel() + " (" + groupUpdateDescription
                    + ")";
        }
    }

    @Override
    public String getLongDescription() {
        return "Update " + group.getLabel();
    }

    /**
     * @return the group
     */
    public Group getGroup() {
        return group;
    }

}
