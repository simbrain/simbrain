
package org.simbrain.network;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.simbrain.resource.ResourceManager;

/**
 * Typesafe enumeration of edit modes.
 */
public final class EditMode {

    /** Name of this edit mode. */
    private final String name;

    /** Cursor center point. */
    private final Point CENTER_POINT = new Point(9, 9);

    /** Cursor for this edit mode. */
    private final Cursor cursor;


    /**
     * Create a new edit mode with the specified name.
     *
     * @param name name of this edit mode
     */
    private EditMode(final String name, final String cursorName) {

        this.name = name;

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = ResourceManager.getImage(cursorName);
        this.cursor = toolkit.createCustomCursor(image, CENTER_POINT, name);
    }


    /**
     * Return the cursor for this edit mode.
     *
     * @return the cursor for this edit mode
     */
    public Cursor getCursor() {
        return cursor;
    }

    /**
     * Return true if this edit mode is <code>SELECTION</code>.
     *
     * @return true if this edit mode is <code>SELECTION</code>
     */
    public boolean isSelection() {
        return (this == SELECTION);
    }

    /**
     * Return true if this edit mode is <code>PAN</code>.
     *
     * @return true if this edit mode is <code>PAN</code>
     */
    public boolean isPan() {
        return (this == PAN);
    }

    /**
     * Return true if this edit mode is <code>ZOOM_IN</code>.
     *
     * @return true if this edit mode is <code>ZOOM_IN</code>
     */
    public boolean isZoomIn() {
        return (this == ZOOM_IN);
    }

    /**
     * Return true if this edit mode is <code>ZOOM_OUT</code>.
     *
     * @return true if this edit mode is <code>ZOOM_OUT</code>
     */
    public boolean isZoomOut() {
        return (this == ZOOM_OUT);
    }

    /**
     * Return true if this edit mode is either <code>ZOOM_IN</code>
     * or <code>ZOOM_OUT</code>.
     *
     * @return true if this edit mode is either <code>ZOOM_IN</code>
     *    or <code>ZOOM_OUT</code>
     */
    public boolean isZoom() {
        return ((this == ZOOM_IN) || (this == ZOOM_OUT));
    }


    /** Selection edit mode. */
    public static final EditMode SELECTION = new EditMode("selection", "Arrow.gif");

    /** Pan edit mode. */
    public static final EditMode PAN = new EditMode("pan", "Pan.gif");

    /** Zoom in edit mode. */
    public static final EditMode ZOOM_IN = new EditMode("zoom in", "ZoomIn.gif");

    /** Zoom out edit mode. */
    public static final EditMode ZOOM_OUT = new EditMode("zoom out", "ZoomOut.gif");

    /** Private array of edit mode values. */
    private static final EditMode[] values = new EditMode[] {SELECTION, PAN, ZOOM_IN, ZOOM_OUT};

    /** Collection of edit mode values. */
    public static final Collection VALUES = Collections.unmodifiableList(Arrays.asList(values));
}