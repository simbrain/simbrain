package org.simbrain.network.smile;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.events.LocationEvents;

import java.awt.geom.Point2D;

public class SmileSVM extends LocatableModel {

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
        System.out.println("test");
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

    @Override
    public String toString() {
        return "SVM Smile object " + getLabel();
    }
}
