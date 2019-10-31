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

import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Network;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

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
     * Id of this group.
     */
    @UserParameter(label = "ID", description = "Id of this group", order = -1, editable = false)
    private String id;

    /**
     * Name of this group. Null strings lead to default labeling conventions.
     */
    @UserParameter(label = "Label", description = "Group label", useSetter = true,
        order = 10)
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
     * Support for property change events.
     */
    protected transient PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /**
     * Construct a model group with a reference to its root network.
     *
     * @param net reference to root network.
     */
    public Group(final Network net) {
        parentNetwork = net;
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
    public abstract String getUpdateMethodDescription();

    /**
     * If true, when the group is added to the network its id will not be used as its label.
     */
    private boolean useCustomLabel = false;

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

    public Network getParentNetwork() {
        return parentNetwork;
    }

    public String getId() {
        return id;
    }

    @Producible(defaultVisibility = false)
    public String getLabel() {
        return label;
    }

    /**
     * Set the label. This prevents the group id being used as the label for
     * new groups.  If null or empty labels are sent in then the group label is used.
     */
    @Consumable(defaultVisibility = false)
    public void setLabel(String label) {
        if (label == null  || label.isEmpty()) {
            useCustomLabel = false;
        } else {
            useCustomLabel = true;
        }
        String oldLabel = this.label;
        this.label = label;
        changeSupport.firePropertyChange("label", oldLabel , label);
    }

    public String getStateInfo() {
        return stateInfo;
    }

    public void setStateInfo(String stateInfo) {
        this.stateInfo = stateInfo;
    }

    public Group getParentGroup() {
        return parentGroup;
    }

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

    public boolean isMarkedForDeletion() {
        return markedForDeletion;
    }

    protected void setMarkedForDeletion(boolean markedForDeletion) {
        this.markedForDeletion = markedForDeletion;
    }

    /**
     * Initialize the id for this group. A default label based
     * on the id is also set. This is overridden by
     * {@link Subnetwork} so that sub-groups also are given ids.
     */
    public void initializeId() {
        id = getParentNetwork().getGroupIdGenerator().getId();
        if (!useCustomLabel) {
            label = id.replaceAll("_", " ");
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Label update needs to be reflected in GUI.
     */
    public void fireLabelUpdated() {
        changeSupport.firePropertyChange("label", null , null);
    }

    public void setUseCustomLabel(boolean useCustomLabel) {
        this.useCustomLabel = useCustomLabel;
    }

    public boolean isUseCustomLabel() {
        return useCustomLabel;
    }

}