package org.simbrain.network.gui;

import org.simbrain.network.LocatableModel;
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

import static org.simbrain.util.PerformanceKt.getCounters;

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
     * Offsets associated with specific types of objects.
     */
    private static Map<Class<? extends LocatableModel>, Point2D> defaultOffsets = new HashMap<>();

    static {
        defaultOffsets.put(Neuron.class, new Point2D.Double(45, 0));
        defaultOffsets.put(NeuronArray.class, new Point2D.Double(0, -145));
        defaultOffsets.put(NeuronGroup.class, new Point2D.Double(50, 50));
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
     * Set to true right after "copying". Allows pastes to grow out from whatever objects were just copied.
     */
    private boolean newCopy = false;

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
        System.out.println(getCounters().get("neuron"));
        return nextLocation;
    }

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
            Point2D newLocation = SimbrainMath.add(SimbrainMath.subtract(anchorPoint.get(), previousAnchorPoint), lastClickedLocation);
            delta = SimbrainMath.subtract(newLocation, modelLocation.get());
            previousAnchorPoint = lastClickedLocation;
            anchorPoint = modelLocation;
            secondPaste = false;
        } else if (newCopy) {
            // Objects were just copied;  update the anchor point so paste trail grows from there.
            delta = SimbrainMath.subtract(anchorPoint.get(), previousAnchorPoint);
            previousAnchorPoint = modelLocation.get();
            anchorPoint = modelLocation;
            newCopy = false;
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

    public void setNewCopy() {
        newCopy = true;
    }

}
