package org.simbrain.network.gui;

import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.NetworkTextObject;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.dl4j.NeuronArray;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.math.SimbrainMath;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Manage intelligent placement of new model elements in a {@link org.simbrain.network.gui.NetworkPanel}.
 * <br>
 * Placement is managed using two concepts. First, an anchor point. Second, a delta between the current anchor point and
 * previous anchor point.  There are cases to keep in mind.
 * <br>
 * (1) The anchor point is reset when you click on the screen, to the point you clicked on.
 * <br>
 * (2) Repeatedly adding an object (using new Neuron, etc) adds them at a fixed offset from the anchor point using
 * {@link #defaultOffsets}. With each addition, the current and previous anchor points are updated. See
 * {@link #addNewModelObject(LocatableModel)}.
 * <br>
 * (3) Adding an object using copy-paste or duplicate, adds them using the delta between the current anchor point and
 * the previous anchor point. This allows custom "paste trails" to be created.
 *
 * @author Yulin Li
 * @author Jeff Yoshimi
 */
public class PlacementManager {

    /**
     * A default offset used for subnetworks and other objects not covered in the list below.
     */
    private static Point2D DEFAULT_OFFSET = new Point2D.Double(45, 0);

    /**
     * Offsets associated with specfic types of objects.
     */
    private static Map<Class<? extends LocatableModel>, Point2D> defaultOffsets = new HashMap<>();
    static {
        defaultOffsets.put(Neuron.class, new Point2D.Double(45, 0));
        defaultOffsets.put(NeuronArray.class, new Point2D.Double(0, -145));
        defaultOffsets.put(NeuronGroup.class, new Point2D.Double(50, 50));
        defaultOffsets.put(NetworkTextObject.class, new Point2D.Double(45, 50));
    }

    /**
     * Tells you the location of the most recently placed object.
     */
    private Supplier<Point2D> anchorPoint = () -> new Point2D.Double(0, 0);

    /**
     * Last used anchor point.
     */
    private Point2D previousAnchorPoint = new Point2D.Double(0, 0);

    /**
     * Last location clicked on screen.
     */
    private Point2D lastClickedLocation = new Point2D.Double(0, 0);

    /**
     * Set to true when a location on the screen is clicked.
     */
    private boolean useLastClickedLocation = false;

    /**
     * Second paste after changing location
     */
    private boolean secondPaste = false;

    /**
     * Resets the anchor point and tells the placement manager to place object(s) there.
     */
    public void setLastClickedLocation(Point2D location) {
        lastClickedLocation = location;
        useLastClickedLocation = true;
    }

    /**
     * Add a new model object and use default offsets.
     */
    public Point2D addNewModelObject(LocatableModel model) {
        previousAnchorPoint = anchorPoint.get();
        Point2D nextLocation;
        if (useLastClickedLocation) {
            nextLocation = lastClickedLocation;
            useLastClickedLocation = false;
        } else {
            // Use "default" offset
            nextLocation = SimbrainMath.add(anchorPoint.get(), defaultOffsets.getOrDefault(model.getClass(), DEFAULT_OFFSET));
        }
        model.setLocation(nextLocation);
        anchorPoint = model::getLocation;
        return nextLocation;
    }

    //// Place a group of objects using default offsets
    //Point2D newLocation = SimbrainMath.add(getDefaultOffset(models), anchorPoint.get());
    //delta = SimbrainMath.subtract(newLocation, modelLocation.get());

    /**
     * Paste a list of objects and place it using the delta between the current anchor point and the
     * previous anchor point.
     */
    public void pasteObjects(List<LocatableModel> models) {

        if (models.isEmpty()) {
            anchorPoint.get();
            return;
        }

        Supplier<Point2D> modelLocation =
                () -> new Point2D.Double(SimnetUtils.getMinX(models), SimnetUtils.getMinY(models));

        Point2D delta;

        if (useLastClickedLocation) {
            // Paste objects at last clicked location
            delta = SimbrainMath.subtract(lastClickedLocation, modelLocation.get());
            useLastClickedLocation = false;
            secondPaste = true;
        } else if (secondPaste) {
            // Location was changed during a paste trail
            Point2D newLocation = SimbrainMath.add(SimbrainMath.subtract(anchorPoint.get(), previousAnchorPoint),lastClickedLocation);
            delta = SimbrainMath.subtract(newLocation, modelLocation.get());
            previousAnchorPoint = lastClickedLocation;
            anchorPoint = modelLocation;
            secondPaste = false;
        } else {
            // Standard case: Offset by delta between last and current anchor point
            Point2D newLocation = SimbrainMath.add(SimbrainMath.subtract(anchorPoint.get(), previousAnchorPoint), anchorPoint.get());
            delta = SimbrainMath.subtract(newLocation, modelLocation.get());
            previousAnchorPoint = anchorPoint.get();
            anchorPoint = modelLocation;
        }

        // Update the locations
        for (LocatableModel model : models) {
            model.setLocation(SimbrainMath.add(model.getLocation(), delta));
        }
    }

    /**
     * When an explicit location is needed. TODO: Phase out use of this method and remove when no longer called.
     */
    public Point2D getLocationAndIncrement() {
        Point2D nextLocation;
        if (useLastClickedLocation) {
            nextLocation = anchorPoint.get();
            useLastClickedLocation = false;
        } else {
            nextLocation = SimbrainMath.add(anchorPoint.get(), DEFAULT_OFFSET);
        }
        previousAnchorPoint = anchorPoint.get();
        anchorPoint = () -> nextLocation;
        return nextLocation;
    }

    /**
     * Get offsets for lists of model objects.  Adds the relevant {@link #defaultOffsets} to the width and height of
     * the list of objects.
     */
    private Point2D getDefaultOffset(List<LocatableModel> models) {
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
