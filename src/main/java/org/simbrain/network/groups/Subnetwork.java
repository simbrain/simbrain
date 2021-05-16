/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.groups;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.NetworkModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkModelList;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.events.LocationEvents;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.simbrain.network.LocatableModelKt.getCenterLocation;
import static org.simbrain.util.GeomKt.minus;
import static org.simbrain.util.GeomKt.plus;

/**
 * A collection of {@link org.simbrain.network.NetworkModel} objects which functions as a subnetwork within the main
 * root network, which (1) is shown in the GUI with an outline around it and a custom interaction box and (2) has
 * a potentially custom update rule.
 */
public abstract class Subnetwork extends LocatableModel implements EditableObject, AttributeContainer {

    /**
     * Reference to the network this group is a part of.
     */
    private final Network parentNetwork;

    /**
     * Event support.
     */
    protected transient LocationEvents events = new LocationEvents(this);

    /**
     * List of neuron groups.
     */
    private final NetworkModelList modelList = new NetworkModelList();

    /**
     * Whether the GUI should display neuron groups contained in this subnetwork. This will usually be true, but in
     * cases where a subnetwork has just one neuron group it is redundant to display both. So this flag indicates to the
     * GUI that neuron groups in this subnetwork need not be displayed.
     */
    private boolean displayNeuronGroups = true;

    /**
     * Create subnetwork group.
     *
     * @param net parent network.
     */
    public Subnetwork(final Network net) {
        parentNetwork = net;
        setLabel("Subnetwork");
    }

    public void addModel(NetworkModel model) {
        modelList.add(model);
        model.setId(getParentNetwork().getIdManager().getAndIncrementId(model.getClass()));
        parentNetwork.getEvents().onModelRemoved(modelList::remove);
    }

    /**
     * Delete this subnetwork and its children.
     */
    public void delete() {
        modelList.getAll().forEach(NetworkModel::delete);
        events.fireDeleted();
    }

    public NetworkModelList getModelList() {
        return modelList;
    }

    /**
     * Return a "flat" list containing every neuron in every neuron group in this subnetwork.
     *
     * @return the flat neuron list.
     */
    public List<Neuron> getFlatNeuronList() {
        return new ArrayList<>(modelList.get(Neuron.class));
    }

    /**
     * Return a "flat" list containing every synapse in every synapse group in this subnetwork.
     *w
     * @return the flat synapse list.
     */
    public List<Synapse> getFlatSynapseList() {
        return new ArrayList<>(modelList.get(Synapse.class));
    }

    @Override
    public String toString() {
        return getId() + ": " + getClass().getSimpleName() + "\n" + modelList.toString();
    }

    public boolean getEnabled() {
        return false;
    }

    public void setEnabled(boolean enabled) {
    }

    /**
     * Default subnetwork update just updates all neuron and synapse groups. Subclasses with custom update should
     * override this.
     */
    public void update() {
        modelList.getAllInDeserializationOrder().forEach(NetworkModel::update);
    }

    // TODO: Rename to clearLooseActivations
    /**
     * Set all activations to 0.
     */
    public void clearActivations() {
        modelList.get(Neuron.class).forEach(Neuron::clear);
    }

    public Network getParentNetwork() {
        return parentNetwork;
    }

    @Override
    public void postUnmarshallingInit() {
        if (events == null) {
            events = new LocationEvents(this);
        }
        modelList.getAllInDeserializationOrder().forEach(NetworkModel::postUnmarshallingInit);
    }

    @Override
    public void setLocation(@NotNull Point2D location) {
        Point2D delta = minus(location, getLocation());
        getLocatableModels()
                .forEach(lm  -> lm.setLocation(plus(lm.getLocation(), delta)));
    }

    protected List<LocatableModel> getLocatableModels() {
        return modelList.getAll().stream()
                .filter(LocatableModel.class::isInstance)
                .map(LocatableModel.class::cast)
                .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public Point2D getLocation() {
        return getCenterLocation(getLocatableModels());
    }

    @NotNull
    @Override
    public LocationEvents getEvents() {
        return events;
    }
}
