package org.simbrain.network.layouts;

import java.awt.geom.Point2D;
import java.util.List;

import org.simbrain.network.core.Neuron;

/**
 * Lay neurons out in a grid.
 * 
 * @author jyoshimi
 */
public class GridLayout implements Layout {

    /** The default number of columns if manual columns are allowed. */
    public static final int DEFAULT_NUM_COLUMNS = 3;

    /** The default horizontal spacing. */
    public static final double DEFAULT_H_SPACING = 50;

    /** The default vertical spacing. */
    public static final double DEFAULT_V_SPACING = 50;

    /** The default allowed state for manual cols. */
    public static final boolean DEFAULT_MANUAL_COLS = false;

    /** Initial x position of line of neurons. */
    private double initialX;

    /** Initial y position of line of neurons. */
    private double initialY;

    /** Number of columns in the layout. */
    private int numColumns = DEFAULT_NUM_COLUMNS;

    /** Horizontal spacing between neurons. */
    private double hSpacing = DEFAULT_H_SPACING;

    /** Vertical spacing between neurons. */
    private double vSpacing = DEFAULT_V_SPACING;

    /** Manually set number of columns in grid. */
    private boolean manualColumns = DEFAULT_MANUAL_COLS;

    /**
     * Create a layout.
     * 
     * @param hSpacing
     *            horizontal spacing between neurons
     * @param vSpacing
     *            vertical spacing between neurons
     * @param numColumns
     *            number of columns of neurons
     */
    public GridLayout(final double hSpacing, final double vSpacing,
	    final int numColumns) {
	this.hSpacing = hSpacing;
	this.vSpacing = vSpacing;
	this.numColumns = numColumns;
    }

    /**
     * Default constructor.
     */
    public GridLayout() {
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
	    Neuron neuron = neurons.get(i);
	    if (i % numCols == 0) {
		rowNum++;
	    }
	    neuron.setX(initialX + (i % numCols) * hSpacing);
	    neuron.setY(initialY + rowNum * vSpacing);
	}
    }

    /**
     * @see Layout
     * @param initialPoint
     *            Initial point
     */
    public void setInitialLocation(final Point2D initialPoint) {
	initialX = initialPoint.getX();
	initialY = initialPoint.getY();
    }

    /**
     * @see Layout
     * @return String
     */
    public String getDescription() {
	return "Grid";
    }

    /**
     * @return the numColumns
     */
    public int getNumColumns() {
	return numColumns;
    }

    /**
     * @param numColumns
     *            the numColumns to set
     */
    public void setNumColumns(final int numColumns) {
	this.numColumns = numColumns;
    }

    /**
     * @return the hSpacing
     */
    public double getHSpacing() {
	return hSpacing;
    }

    /**
     * @param spacing
     *            the hSpacing to set
     */
    public void setHSpacing(final double spacing) {
	hSpacing = spacing;
    }

    /**
     * @return the vSpacing
     */
    public double getVSpacing() {
	return vSpacing;
    }

    /**
     * @param spacing
     *            the vSpacing to set
     */
    public void setVSpacing(final double spacing) {
	vSpacing = spacing;
    }

    /** @override */
    public String toString() {
	return "Grid Layout";
    }

    /**
     * @return the manualColumns
     */
    public boolean isManualColumns() {
	return manualColumns;
    }

    /**
     * @param manualColumns
     *            the manualColumns to set
     */
    public void setManualColumns(boolean manualColumns) {
	this.manualColumns = manualColumns;
    }
}
