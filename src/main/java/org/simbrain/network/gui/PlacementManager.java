package org.simbrain.network.gui;

import org.simbrain.util.math.SimbrainMath;

import java.awt.geom.Point2D;

/**
 * Manage the placement of new model elements in a {@link org.simbrain.network.gui.NetworkPanel}.
 * <p>
 * New nodes are added wherever the last click occurred, plus an offset. The offset is either specified
 * for new object creation, or based on a "paste delta" for copy-paste events.
 * When series of pastes or duplicates occur they grow in a trail using these deltas.
 * <p>
 * 1) Left click (but did not lasso) somewhere.  This sets a new "anchor" point.
 * <p>
 * 2) Adding or duplicating or pasting new items from there makes them emerge at a set offset from the anchor.
 * <p>
 * 3) That offset is either specified (for new objects) or the paste delta (for copy-paste or duplicate events)
 */
public class PlacementManager {

    /**
     * Default offset for new points.
     */
    private static Point2D DEFAULT_OFFSET = new Point2D.Double(45, 0);

    /**
     * Anchor point for new paste trails.
     */
    private Point2D anchorPoint = new Point2D.Double(0, 0);

    /**
     * Beginning location of a paste delta, which should be whatever the last selected and copied
     * items were.
     */
    private Point2D pasteBegin;

    /**
     * Delta for paste offset.  From pasteBegin to wherever the last set of objects were dragged.
     */
    private Point2D pasteDelta = DEFAULT_OFFSET;

    /**
     * Returns where the next object should be added to the network panel.
     * Also updates the anchor point.
     */
    public Point2D getLocation() {

        setAnchorPoint(SimbrainMath.add(anchorPoint, pasteDelta));

        // TODO: Not sure why below is needed.  Copy / pasting a neuron collection
        // sets something to infinity
        if (anchorPoint.getX() > Double.MAX_VALUE || anchorPoint.getY() > Double.MAX_VALUE) {
            setAnchorPoint(DEFAULT_OFFSET);
            pasteDelta.setLocation(45, 0);
        }
        return anchorPoint;
    }

    /**
     * Returns the current anchor point plus a provided x and y offset, and updates to a new
     * anchor point.  Useful for placing things at node specific relative locations on
     * repeated use.
     */
    public Point2D getLocation(double offsetX, double offsetY) {
        setAnchorPoint(new Point2D.Double(anchorPoint.getX()+offsetX,anchorPoint.getY()+offsetY));
        return anchorPoint;
    }

    /**
     * Set the beginning point of a paste delta.
     */
    public void setPasteDeltaBegin(Point2D begin) {
        this.pasteBegin = begin;
    }

    /**
     * Set the end point of the paste delta and update the paste delta.
     */
    public void setPasteDeltaEnd(Point2D pasteEnd) {
        Point2D beginPoint;
        if (pasteBegin == null) {
            beginPoint = anchorPoint;
        } else {
            beginPoint = pasteBegin;
        }
        pasteDelta.setLocation(
                -(beginPoint.getX() - pasteEnd.getX()),
                -(beginPoint.getY() - pasteEnd.getY()));
    }

    /**
     * Set the anchor point
     *
     * @param anchorPoint the new anchor point
     */
    public void setAnchorPoint(Point2D anchorPoint) {
        //System.out.println("anchorPoint = " + anchorPoint);
        this.anchorPoint = anchorPoint;
    }

    /**
     * Set the new anchor point and reset translation to 0 so that the next point goes here.
     */
    public void setFixedAnchorPoint(Point2D position) {
        setAnchorPoint(position);
        pasteDelta.setLocation(0, 0);
    }

}
