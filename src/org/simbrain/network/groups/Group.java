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
package org.simbrain.network.groups;

import org.simbrain.network.core.Network;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor2.CopyableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

/**
 * <b>Group</b>: a logical group of neurons and / or synapses. Its gui
 * representation is {@link org.simbrain.network.gui.nodes.GroupNode}.
 */
public abstract class Group implements CopyableObject, AttributeContainer {

    /**
     * Reference to the network this group is a part of.
     */
    private final Network parentNetwork;

    /**
     * Name of this group.
     */
    @UserParameter(label = "ID", description = "Id of this group", order = -1, editable = false)
    private String id;

    /**
     * Name of this group. Null strings lead to default labeling conventions.
     */
    @UserParameter(label = "Label", description = "Group label", order = 10)
    private String label = "";

    /**
     * Optional information about the current state of the group. For display in
     * GUI.
     */
    private String stateInfo = "";

    /**
     * Flag which prevents infinite loops when deleting composite groups.
     */
    private boolean markedForDeletion = false;

    /**
     * Parent group of this group, or null if it has none. For group types which
     * have a hierarchy of groups.
     */
    private Group parentGroup;

    /**
     * Construct a model group with a reference to its root network.
     *
     * @param net reference to root network.
     */
    public Group(final Network net) {
        parentNetwork = net;
    }
    public Group(final Network net, String id) {
        parentNetwork = net;
        this.id = id;
        this.label = id;
    }

    /**
     * Whether this group is empty or not.
     *
     * @return true if the group is empty, false otherwise.
     */
    public abstract boolean isEmpty();

    /**
     * @return the number of synapses/neurons in this group as applicable to the
     * group type
     */
    public abstract int size();

    /**
     * Update this group.
     */
    public abstract void update();

    /**
     * Perform necessary deletion cleanup.
     */
    public abstract void delete();

    /**
     * Returns a description of this group's update method, which is displayed
     * in the update manager panel.
     *
     * @return a description of the update method.
     */
    public abstract String getUpdateMethodDesecription();

    @Override
    public String toString() {
        if (label != null) {
            return label;
        } else if (id != null) {
            return id;
        } else {
            return super.toString();
        }
    }

    /**
     * @return the parent
     */
    public Network getParentNetwork() {
        return parentNetwork;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    @Producible(idMethod = "getId", defaultVisibility = false)
    public String getLabel() {
        return label;
    }

    @Consumable(idMethod = "getId", defaultVisibility = false)
    public void setLabel(String label) {
        this.label = label;
        if (parentNetwork != null) {
            parentNetwork.fireGroupParametersChanged(this);
        }
    }

    /**
     * @return the stateInfo
     */
    public String getStateInfo() {
        return stateInfo;
    }

    /**
     * @param stateInfo the stateInfo to set
     */
    public void setStateInfo(String stateInfo) {
        this.stateInfo = stateInfo;
    }

    /**
     * @return the parentGroup
     */
    public Group getParentGroup() {
        return parentGroup;
    }

    /**
     * @param parentGroup the parentGroup to set
     */
    protected void setParentGroup(Group parentGroup) {
        this.parentGroup = parentGroup;
    }

    /**
     * Returns true if this group has a parent group (i.e. it is a sub-group
     * within a larger group).
     *
     * @return true if this group has a parent, false otherwise.
     */
    public boolean hasParentGroup() {
        if (parentGroup == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns true if this group is "top level" (has no parent group).
     * Basically a convenience wrapper around hasParentGroup().
     *
     * @return true if this has a parent group, false otherwise.
     */
    public boolean isTopLevelGroup() {
        if (hasParentGroup()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @return the markedForDeletion
     */
    public boolean isMarkedForDeletion() {
        return markedForDeletion;
    }

    /**
     * @param markedForDeletion the markedForDeletion to set
     */
    protected void setMarkedForDeletion(boolean markedForDeletion) {
        this.markedForDeletion = markedForDeletion;
    }

    /**
     * Set the id for this group.   A default label based
     * on the id is also set.   This is overridden  by
     * subnetowrk so that sub-groups also are given ids.
     */
    public void recursivelySetIds() {

        String id = getParentNetwork().getGroupIdGenerator().getId();
        setId(id);

        // Create a default label based on the id
        if (getLabel() == null) {
            setLabel(id.replaceAll("_", " "));
        }
        //TODO: Below should be part of group initalization.
        if (getLabel().isEmpty()) {
            setLabel(id);
        }
    }

}