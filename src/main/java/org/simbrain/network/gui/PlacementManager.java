package org.simbrain.network.gui;

import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.dl4j.NeuronArray;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.math.SimbrainMath;

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

    private boolean resetSequence = false;

    public Point2D setNextLocationOnto(LocatableModel model) {
        // TODO: neuron group creation template should not use placement manager.
        if (model instanceof NeuronGroup && ((NeuronGroup) model).getId() == null) {
            return current.get();
        }
        previous = current.get();
        Point2D nextLocation;
        if (resetSequence) {
            nextLocation = current.get();
            resetSequence = false;
        } else {
            nextLocation = SimbrainMath.add(current.get(), defaultOffsets.getOrDefault(model.getClass(), DEFAULT_OFFSET));
        }
        model.setLocation(nextLocation);
        current = () -> new Point2D.Double(model.getCenterX(), model.getCenterY());
        return nextLocation;
    }

    public Point2D setNextPasteLocationOnto(List<LocatableModel> models) {
        if (models.isEmpty()) {
            return current.get();
        }

        Supplier<Point2D> modelLocation =
                () -> new Point2D.Double(models.get(0).getCenterX(), models.get(0).getCenterY());

        Point2D newLocation = SimbrainMath.add(SimbrainMath.subtract(current.get(), previous), current.get());

        Point2D delta = SimbrainMath.subtract(newLocation, modelLocation.get());

        previous = current.get();
        for (LocatableModel model : models) {
            model.setLocation(SimbrainMath.add(model.getLocation(), delta));
        }
        current = modelLocation;
        return current.get();
    }

    public Point2D getLocationAndIncrement() {
        Point2D nextLocation;
        if (resetSequence) {
            nextLocation = current.get();
            resetSequence = false;
        } else {
            nextLocation = SimbrainMath.add(current.get(), offset);
        }
        previous = current.get();
        current = () -> nextLocation;
        return nextLocation;
    }

    public void setNextLocationFixed(Point2D location) {
        current = () -> location;
        resetSequence = true;
    }

    public void setCopyModels(List<LocatableModel> models) {
        previous = current.get();
        current = () -> new Point2D.Double(models.get(0).getCenterX(), models.get(0).getCenterY());
    }
}
