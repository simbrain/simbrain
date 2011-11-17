package org.simbrain.network.layouts;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;

/**
 * Lay neurons out in a grid.
 *
 * @author jyoshimi
 */
public class GridLayout implements Layout {

    /** Initial x position of line of neurons. */
    private double initialX;

    /** Initial y position of line of neurons. */
    private double initialY;

    /** Number of columns in the layout. */
    private static int numColumns = 3;

    /** Horizontal spacing between neurons. */
    private static double hSpacing = 50;

    /** Vertical spacing between neurons. */
    private static double vSpacing = 50;

    /** Manually set number of columns in grid. */
    private static boolean manualColumns = false;

    /**
     * Create a layout.
     *
     * @param hSpacing horizontal spacing between neurons
     * @param vSpacing vertical spacing between neurons
     * @param numColumns number of columns of neurons
     */
    public GridLayout(final double hSpacing, final double vSpacing, final int numColumns) {
        GridLayout.hSpacing = hSpacing;
        GridLayout.vSpacing = vSpacing;
        GridLayout.numColumns = numColumns;
    }

    /**
     * Default constructor.
     */
    public GridLayout() { }

    /**
     * {@inheritDoc}
     */
    public void layoutNeurons(final Network network) {
        ArrayList<Neuron> neurons = network.getFlatNeuronList();
        layoutNeurons(neurons);
    }

    /**
     * {@inheritDoc}
     */
    public void layoutNeurons(final List<Neuron> neurons) {
        int rowNum = 0;
        int numCols = numColumns;
        if (!manualColumns) {
            numCols = (int) Math.sqrt(neurons.size());
        }

        for (int i = 0; i < neurons.size(); i++) {
            Neuron neuron = (Neuron) neurons.get(i);
            if (i % numCols == 0) {
                rowNum++;
            }
            neuron.setX(initialX + (i % numCols) * hSpacing);
            neuron.setY(initialY + rowNum * vSpacing);
        }
    }

    /** @see Layout
     *  @param initialPoint Initial point
     */
    public void setInitialLocation(final Point2D initialPoint) {
        initialX = initialPoint.getX();
        initialY = initialPoint.getY();
    }

    /** @see Layout
     *  @return String
     */
    public String getLayoutName() {
        return "Grid";
    }

    /**
     * @return the numColumns
     */
    public static int getNumColumns() {
        return numColumns;
    }

    /**
     * @param numColumns the numColumns to set
     */
    public static void setNumColumns(final int numColumns) {
        GridLayout.numColumns = numColumns;
    }

    /**
     * @return the hSpacing
     */
    public static double getHSpacing() {
        return hSpacing;
    }

    /**
     * @param spacing the hSpacing to set
     */
    public static void setHSpacing(final double spacing) {
        hSpacing = spacing;
    }

    /**
     * @return the vSpacing
     */
    public static double getVSpacing() {
        return vSpacing;
    }

    /**
     * @param spacing the vSpacing to set
     */
    public static void setVSpacing(final double spacing) {
        vSpacing = spacing;
    }

    /** @override */
    public String toString() {
        return "Grid Layout";
    }

    /**
     * @return the manualColumns
     */
    public static boolean isManualColumns() {
        return manualColumns;
    }

    /**
     * @param manualColumns the manualColumns to set
     */
    public static void setManualColumns(boolean manualColumns) {
        GridLayout.manualColumns = manualColumns;
    }
}
