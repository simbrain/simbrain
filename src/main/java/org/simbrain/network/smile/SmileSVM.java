package org.simbrain.network.smile;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.events.LocationEvents;
import org.simbrain.network.events.NetworkModelEvents;
import org.simbrain.network.events.NeuronArrayEvents;

import java.awt.geom.Point2D;

public class SmileSVM implements  LocatableModel {

    /**
     * Event support.
     */
    private transient LocationEvents events = new LocationEvents(this);

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public void update() {
    }

    @Override
    public void setBufferValues() {

    }

    @Override
    public void applyBufferValues() {

    }

    @NotNull
    @Override
    public Point2D getLocation() {
        return null;
    }

    @Override
    public void setLocation(@NotNull Point2D location) {

    }

    @NotNull
    @Override
    public LocationEvents getEvents() {
        return events;
    }


}
