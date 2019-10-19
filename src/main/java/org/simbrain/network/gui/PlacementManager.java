package org.simbrain.network.gui;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.math.SimbrainMath;

import java.awt.geom.Point2D;

/**
 * Manage the placement of new model elements in a {@link org.simbrain.network.gui.NetworkPanel}.
 *
 * New nodes are added wherever the last click occurred, plus an offset.
 *
 * When series of pastes or duplicates occur they are made at that offset.
 *
 * The offset changes based on drag events.
 */
public class PlacementManager {

    /**
     * Default offset for new points.  Assumes x and y will be the same, for now.
     */
    private static final int DEFAULT_NEWPOINT_OFFSET = 100;

    /**
     * Default spacing for new points.
     */
    public static final int DEFAULT_SPACING = 45;

    /**
     * Point where new network objects should be added. Reset when the user clicks anywhere on
     * screen, to the location clicked.
     */
    private Point2D.Double location = new Point2D.Double(0, 0);

    /**
     * Last position clicked in the network.
     */
    private Point2D lastClickedPosition;

    /**
     * Tracks number of pastes that have occurred.  Reset when any non-paste action occurs.
     */
    private double numberOfPastes;

    /**
     * Beginning position used in calculating offsets for multiple pastes.
     */
    private Point2D beginPosition = new Point2D.Double(0, 0);

    /**
     * End position used in calculating offsets for multiple pastes.
     */
    private Point2D endPosition;

    /**
     * Delta for offset
     */
    private Point2D pasteOffset = new Point2D.Double(DEFAULT_SPACING, 0);

    /**
     * Construct a new placement manager.
     */
    public PlacementManager() {
        location.x = DEFAULT_NEWPOINT_OFFSET;
        location.y = DEFAULT_NEWPOINT_OFFSET;
    }

    /**
     * Update where the next placement location should be.
     * By default objects are added to the right of the last object
     * Convenient for quickly making "lines" of objects by repeatedly adding them
     */
    public void update() {
        location.x += DEFAULT_SPACING; // this is for neurons

        // Below was used when adding collections of neurons
        // ----------------
        // x += neurons.get(neurons.size() - 1).getX() + DEFAULT_SPACING + 10

        // Below was used when adding neuron groups or subnetworks
        // to add diagonally below and to the right of the
        // last subnetwork added
        // ---------------
        // neurons.get(neurons.size() - 1).getX() + DEFAULT_SPACING
        // whereToAdd.getX() + DEFAULT_SPACING
        // whereToAdd.getY() + DEFAULT_SPACING)


        //  Generalize to a vector
    }


    /**
     * Returns where the next object should be added to the network panel.
     */
    public Point2D.Double getLocation() {
        return location;
    }

    /**
     * Set the offset used with multiple pastes.
     */
    public void setPasteDelta() {
        if ((beginPosition != null) && (endPosition != null)) {
            pasteOffset.setLocation(
                    beginPosition.getX() - endPosition.getX(),
                    beginPosition.getY() - endPosition.getY());
        }
    }

    //TODO: Rename
    public void setBeginPosition(final Point2D beginPosition) {
        this.beginPosition = beginPosition;
    }

    public void setEndPosition(final Point2D endPosition) {
        this.endPosition = endPosition;
        System.out.println(numberOfPastes);
        if (numberOfPastes == 1) {
            setPasteDelta();
        }
    }

    public Point2D getPasteOffset() {
        return pasteOffset;
    }

    public void setLastClickedPosition(final Point2D lastLeftClicked) {
        this.lastClickedPosition = lastLeftClicked;
    }

    public void resetPastes() {
        numberOfPastes = 0;
    }

    public void incrementPastes() {
        numberOfPastes++;
    }

    // Not sure.  This was in NetworkPanel.setLastClickedPosition

    //// If left clicking somewhere assume not multiple pasting, except after
    //// the first paste,
    //// when one is setting the offset for a string of pastes
    //    if (this.getNumberOfPastes() != 1) {
    //    this.setNumberOfPastes(0);
    //}

    // From mouse event handler, at end of drag
    //// Reset the beginning of a sequence of pastes, but keep the old
    //// paste-offset. This occurs when pasting a sequence, and moving one set
    //// of objects to a new location
    //if (networkPanel.getNumberOfPastes() != 1) {
    //    networkPanel.getPlacementManager().setBeginPosition(SimnetUtils.getUpperLeft(networkPanel.getSelectedModels()));
    //}

    //         // Reset the place new neurons and groups should be added
    //        networkPanel.getWhereToAdd().setLocation(event.getPosition().getX() + NetworkPanel.DEFAULT_SPACING, event.getPosition().getY());

    // TODO: From clipboard, see commented out call to translate
    //
    ///**
    // * Returns the paste offset. Handles complexities of paste-trails.
    // * <p>
    // * Begin where the last object was pasted (default to a standard position if
    // * none has). Add the size of the object. Add the number of recent clicks
    // * times the paste increment.
    // *
    // * @param net       reference to network panel.
    // * @param upperLeft the upper left of the group of objects to be pasted
    // * @param xOrY      whether to return x or y offset.
    // * @return the offset for the pasted items.
    // */
    //private static double getPasteOffset(final NetworkPanel net, final Point2D upperLeft, final String xOrY) {
    //
    //    if (xOrY.equals("X")) {
    //        return (net.getBeginPosition().getX() - upperLeft.getX() - ((net.getNumberOfPastes() + 1) * getPasteIncrement(net, "X")));
    //    } else {
    //        return (net.getBeginPosition().getY() - upperLeft.getY() - ((net.getNumberOfPastes() + 1) * getPasteIncrement(net, "Y")));
    //    }
    //}
    //
    ///**
    // * Private method for handling complexities of paste-trails.
    // * <p>
    // * When first pasting, paste at the default location relative to the
    // * original paste. Otherwise use the paste increment computed by the
    // * network.
    // *
    // * @param net  Reference to network
    // * @param xOrY Whether to look at x or y values
    // * @return the proper paste increment
    // */
    //private static double getPasteIncrement(final NetworkPanel net, final String xOrY) {
    //
    //    if (xOrY.equals("X")) {
    //        if (net.getPasteX() != 0) {
    //            return net.getPasteX();
    //        } else {
    //            return -PASTE_INCREMENT;
    //        }
    //    }
    //
    //    if (xOrY.equals("Y")) {
    //        if (net.getPasteY() != 0) {
    //            return net.getPasteY();
    //        } else {
    //            return -PASTE_INCREMENT;
    //        }
    //    }
    //    return 0;
    //}

    // Call in ScreenElement.showContextMenu
    //networkPanel.getPlacementManager().setLastClickedPosition(canvasPosition);

    // Layout Manager call.  See "think"
    //layoutObject.getLayout().setInitialLocation(networkPanel.getLastClickedPosition()); //TODO: Think

    // See WandEventHandler.mousepressed

    //public Point2D getLastClickedPosition() {
    //    if (lastClickedPosition == null) {
    //        lastClickedPosition = new Point2D.Double(DEFAULT_NEWPOINT_OFFSET, DEFAULT_NEWPOINT_OFFSET);
    //    }
    //    return lastClickedPosition;
    //}



}
