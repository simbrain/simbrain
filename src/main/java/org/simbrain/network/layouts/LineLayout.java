package org.simbrain.network.layouts;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.core.Neuron;
import org.simbrain.util.UserParameter;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Lay neurons out in a line.
 *
 * @author jyoshimi
 */
public class LineLayout extends Layout {

    /**
     * Orientation of the line.
     */
    public enum LineOrientation {
        VERTICAL {
            public String toString() {
                return "Vertical";
            }
        }, HORIZONTAL {
            public String toString() {
                return "Horizontal";
            }
        }
    }

    /**
     * The default orientation of the line.
     */
    public static final LineOrientation DEFAULT_LINE_ORIENTATION = LineOrientation.HORIZONTAL;

    /**
     * The default spacing between neurons when laid out.
     */
    public static final double DEFAULT_SPACING = 40;

    /**
     * Current line orientation.
     */
    @UserParameter(label = "Orientation", description = "Horizontal or Vertical")
    private LineOrientation orientation = DEFAULT_LINE_ORIENTATION;

    /**
     * Spacing between neurons.
     */
    @UserParameter(label = "Spacing", description = "Spacing between neurons")
    private double spacing = DEFAULT_SPACING;

    /**
     * Initial x position of line of neurons.
     */
    private double initialX;

    /**
     * Initial y position of line of neurons.
     */
    private double initialY;

    /**
     * Create a line layout with a specified orientation and default spacing.
     *
     * @param orientation vertical or horizontal
     */
    public LineLayout(LineOrientation orientation) {
        this(DEFAULT_SPACING, orientation);
    }

    /**
     * Create a layout.
     *
     * @param spacing     spacing between neurons
     * @param orientation of the neurons
     */
    public LineLayout(final double spacing, final LineOrientation orientation) {
        this.spacing = spacing;
        this.orientation = orientation;
    }

    /**
     * Create a line layout with all values specified.
     *
     * @param x           initial x position
     * @param y           initial y position
     * @param spacing     spacing between neurons
     * @param orientation vertical or horizontal
     */
    public LineLayout(final double x, final double y, final double spacing, final LineOrientation orientation) {
        initialX = x;
        initialY = y;
        this.spacing = spacing;
        this.orientation = orientation;
    }

    public LineLayout() {
        super();
    }

    @Override
    public void layoutNeurons(final List<Neuron> neurons) {
        if(neurons.size() == 0) {
            return;
        }
        if (orientation == LineOrientation.VERTICAL) {
            double ypos = initialY;
            for (Neuron neuron : neurons) {
                neuron.setLocation(initialX, ypos);
                ypos += spacing;
            }
        } else if (orientation == LineOrientation.HORIZONTAL) {
            double xpos = initialX;
            for (Neuron neuron : neurons) {
                neuron.setLocation(xpos, initialY);
                xpos += spacing;
            }
        }

    }

    @Override
    public void setInitialLocation(final Point2D initialPoint) {
        initialX = initialPoint.getX();
        initialY = initialPoint.getY();
    }

    @Override
    public String getDescription() {
        return "Line";
    }

    @NotNull
    public LineOrientation getOrientation() {
        return orientation;
    }

    /**
     * @param orientation the orientation to set
     */
    public void setOrientation(@NotNull final LineOrientation orientation) {
        this.orientation = orientation;
        // System.out.println("LineLayout orientation: " + this.orientation);
    }

    public double getSpacing() {
        return spacing;
    }

    /**
     * @param spacing the spacing to set
     */
    public void setSpacing(final double spacing) {
        this.spacing = spacing;
    }

    @Override
    public String toString() {
        return "Line Layout";
    }

    @Override
    public LineLayout copy() {
        return  new LineLayout(spacing, orientation);
    }

}
