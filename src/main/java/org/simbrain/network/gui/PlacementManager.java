package org.simbrain.network.gui;

import org.simbrain.util.math.SimbrainMath;

import java.awt.geom.Point2D;

/**
 * Manage the placement of new model elements in a {@link org.simbrain.network.gui.NetworkPanel}.
 * <p>
 * New nodes are added wherever the last click occurred, plus an offset. When series of pastes or duplicates occur they
 * are made at that offset.
 * <p>
 * 1) Left click (but did not lasso) somewhere.  This sets a new "anchor" point.
 * <p>
 * 2) Adding or duplicating or pasting new items from there makes them emerge at a set offset from the anchor.
 * <p>
 * 3) That offset is set whenever moving a screen item from one place to another
 */
public class PlacementManager {

    /**
     * Default offset for new points.
     */
    private static Point2D DEFAULT_OFFSET = new Point2D.Double(45, 0);

    /**
     * Anchor point for new paste trails.
     */
    private Point2D anchorPoint = new Point2D.Double(0, 0);;

    /**
     * Delta for paste offset
     */
    private Point2D pasteDelta = DEFAULT_OFFSET;

    /**
     * Returns where the next object should be added to the network panel. Also updates the anchor point.
     */
    public Point2D getLocation() {
        anchorPoint = SimbrainMath.add(anchorPoint, pasteDelta);

        // TODO: Not sure why below is needed.  Setting a neuron collection
        // sets something to infinity
        if (anchorPoint.getX() > Double.MAX_VALUE || anchorPoint.getY() > Double.MAX_VALUE) {
            anchorPoint = DEFAULT_OFFSET;
            pasteDelta.setLocation(45, 0);
        }
        return anchorPoint;
    }

    /**
     * Set the delta fro the last anchor point to here.
     */
    public void setPasteDelta(Point2D end) {
        pasteDelta.setLocation(
                -(anchorPoint.getX() - end.getX()),
                -(anchorPoint.getY() - end.getY()));

    }

    /**
     * Set the anchor point
     *
     * @param anchorPoint the new anchor point
     */
    public void setAnchorPoint(Point2D anchorPoint) {
        this.anchorPoint = anchorPoint;
    }

    /**
     * Set the new anchor point and reset translation to 0 so that the next point goes here.
     */
    public void setFixedAnchorPoint(Point2D position) {
        setAnchorPoint(position);
        pasteDelta.setLocation(0, 0);
    }

    // TODO: Code I changed to consider...

    // Layout Manager call.  See "think"
    //layoutObject.getLayout().setInitialLocation(networkPanel.getLastClickedPosition()); //TODO: Think

    // See WandEventHandler.mousepressed

}
