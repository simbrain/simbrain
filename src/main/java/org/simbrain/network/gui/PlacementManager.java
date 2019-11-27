package org.simbrain.network.gui;

import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.dl4j.NeuronArray;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.math.SimbrainMath;
import umontreal.iro.lecuyer.simevents.Sim;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PlacementManager {

    /**
     * Default offset for new points.
     */
    private static Point2D DEFAULT_OFFSET = new Point2D.Double(45, 0);

    private static Map<Class<? extends LocatableModel>, Point2D> defaultOffsets = new HashMap<>();

    static {
        defaultOffsets.put(Neuron.class, new Point2D.Double(45, 0));
        defaultOffsets.put(NeuronArray.class, new Point2D.Double(0, -145));
        defaultOffsets.put(NeuronGroup.class, new Point2D.Double(50, 50));
    }

    private Point2D offset = DEFAULT_OFFSET;

    private Point2D previous = new Point2D.Double(0, 0);

    private Supplier<Point2D> current = () -> new Point2D.Double(0, 0);

    private boolean placeOnLastClick = false;

    private boolean copyInit = false;

    public Point2D setNextLocationOnto(LocatableModel model) {
        previous = current.get();
        Point2D nextLocation;
        if (placeOnLastClick) {
            nextLocation = current.get();
            placeOnLastClick = false;
        } else {
            nextLocation = SimbrainMath.add(current.get(), defaultOffsets.getOrDefault(model.getClass(), DEFAULT_OFFSET));
        }
        model.setLocation(nextLocation);
        current = model::getLocation;
        return nextLocation;
    }

    public Point2D setNextPasteLocationOnto(List<LocatableModel> models) {
        if (models.isEmpty()) {
            return current.get();
        }

        Supplier<Point2D> modelLocation =
                () -> new Point2D.Double(SimnetUtils.getMinX(models), SimnetUtils.getMinY(models));

        Point2D delta;

        if (placeOnLastClick) {
            delta = SimbrainMath.subtract(current.get(), modelLocation.get());
            placeOnLastClick = false;
            copyInit = true;
        } else if (copyInit) {
            Point2D newLocation = SimbrainMath.add(getInitialPasteDelta(models), current.get());
            delta = SimbrainMath.subtract(newLocation, modelLocation.get());
            copyInit = false;
        } else {
            Point2D newLocation = SimbrainMath.add(SimbrainMath.subtract(current.get(), previous), current.get());
            delta = SimbrainMath.subtract(newLocation, modelLocation.get());
        }

        previous = current.get();
        for (LocatableModel model : models) {
            model.setLocation(SimbrainMath.add(model.getLocation(), delta));
        }
        current = modelLocation;
        return current.get();
    }

    public Point2D getLocationAndIncrement() {
        Point2D nextLocation;
        if (placeOnLastClick) {
            nextLocation = current.get();
            placeOnLastClick = false;
        } else {
            nextLocation = SimbrainMath.add(current.get(), offset);
        }
        previous = current.get();
        current = () -> nextLocation;
        return nextLocation;
    }

    public void setNextLocationFixed(Point2D location) {
        System.out.println("PlacementManager.setNextLocationFixed");
        current = () -> location;
        placeOnLastClick = true;
    }

    public void setCopyModels(List<LocatableModel> models) {
        setCopyModels(models, true);
    }

    public void setCopyModels(List<LocatableModel> models, boolean resetSequence) {
        if (resetSequence) {
            System.out.println("PlacementManager.setCopyModels");
            previous = current.get();
            current = () -> new Point2D.Double(SimnetUtils.getMinX(models), SimnetUtils.getMinY(models));
            placeOnLastClick = false;
            copyInit = resetSequence;
        }
    }

    private Point2D getInitialPasteDelta(List<LocatableModel> models) {
        Point2D offset = defaultOffsets.getOrDefault(models.get(0).getClass(), DEFAULT_OFFSET);
        Point2D ret = new Point2D.Double();

        double width = SimnetUtils.getWidth(models);
        double dx = offset.getX();

        double height = SimnetUtils.getHeight(models);
        double dy = offset.getY();

        if (offset.getX() < 0) {
            ret.setLocation(-width + dx, ret.getY());
        } else if (offset.getX() == 0) {
            ret.setLocation(0, ret.getY());
        } else {
            ret.setLocation(width + dx, ret.getY());
        }

        if (offset.getY() < 0) {
            ret.setLocation(ret.getX(), -height + dy);
        } else if (offset.getY() == 0) {
            ret.setLocation(ret.getX(), 0);
        } else {
            ret.setLocation(ret.getX(), height + dy);
        }

        return ret;
    }
}
