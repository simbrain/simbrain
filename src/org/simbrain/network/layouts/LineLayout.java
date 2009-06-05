package org.simbrain.network.layouts;

import java.awt.geom.Point2D;
import java.util.List;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;

/**
 * Lay neurons out in a line.
 *
 * @author jyoshimi
 */
public class LineLayout implements Layout {

    /** Orientation of the line. */
    public enum LineOrientation {
        VERTICAL {
            public String toString() {
                return "Vertical";
            }
        },
        HORIZONTAL {
            public String toString() {
                return "Horizontal";
            }
        }
    };

    /** Current line orientation. */
    private static LineOrientation orientation = LineOrientation.HORIZONTAL;
    
    /** Initial x position of line of neurons. */
    private double initialX;

    /** Initial y position of line of neurons. */
    private double initialY;

    /** Spacing between neurons. */
    private static double spacing = 40;


    /**
     * Create a layout.
     *
     * @param initialx initial x position
     * @param initialy initial y position
     * @param spacing spacing between neurons
     * @param orientation of the neurons
     */
    public LineLayout(final double initialx, final double initialy,
            final double spacing, final LineOrientation orientation) {
        initialX = initialx;
        initialY = initialy;
        LineLayout.spacing = spacing;
        LineLayout.orientation = orientation;
    }

    /**
     * Create a layout.
     *
     * @param spacing spacing between neurons
     * @param orientation of the neurons
     */
    public LineLayout(final double spacing, final LineOrientation orientation) {
        LineLayout.spacing = spacing;
        LineLayout.orientation = orientation;
    }

    /**
     * Default Constructor.
     */
    public LineLayout() { }

    /**
     * {@inheritDoc}
     */
    public void layoutNeurons(final Network network) {
        layoutNeurons(network.getFlatNeuronList());
    }
    
    /**
     * {@inheritDoc}
     */
    public void layoutNeurons(final List<Neuron> neurons) {
        if (orientation == LineOrientation.VERTICAL) {
            double ypos = initialY;
            for (Neuron neuron : neurons) {
                neuron.setX(initialX);
                neuron.setY(ypos);
                ypos += spacing;
            }
        } else if (orientation == LineOrientation.HORIZONTAL) {
            double xpos = initialX;
            for (Neuron neuron : neurons) {
                neuron.setX(xpos);
                neuron.setY(initialY);
                xpos += spacing;
            }
        }
    }
    
    

    /** @see Layout */
    public void setInitialLocation(final Point2D initialPoint) {
        initialX = initialPoint.getX();
        initialY = initialPoint.getY();
    }

    /** @see Layout */
    public String getLayoutName() {
        return "Line";
    }

    /**
     * @return the orientation
     */
    public static LineOrientation getOrientation() {
        return orientation;
    }

    /**
     * @param orientation the orientation to set
     */
    public static void setOrientation(final LineOrientation orientation) {
        LineLayout.orientation = orientation;
        System.out.println("LineLayout orientation: " + LineLayout.orientation);
    }

    /**
     * @return the spacing
     */
    public static double getSpacing() {
        return spacing;
    }

    /**
     * @param spacing the spacing to set
     */
    public static void setSpacing(final double spacing) {
        LineLayout.spacing = spacing;
    }

    /** @override */
    public String toString() {
        return "Line Layout";
    }
}
