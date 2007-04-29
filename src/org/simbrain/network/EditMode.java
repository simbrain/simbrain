/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
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
    private static final Point CENTER_POINT = new Point(9, 9);

    /** Cursor for this edit mode. */
    private final Cursor cursor;


    /**
     * Create a new edit mode with the specified name.
     *
     * @param name name of this edit mode
     * @param cursorName cursor name for this edit mode
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
    public static final EditMode SELECTION = new EditMode("selection", "Arrow.png");

    /** Pan edit mode. */
    public static final EditMode PAN = new EditMode("pan", "Pan.png");

    /** Zoom in edit mode. */
    public static final EditMode ZOOM_IN = new EditMode("zoom in", "ZoomIn.png");

    /** Zoom out edit mode. */
    public static final EditMode ZOOM_OUT = new EditMode("zoom out", "ZoomOut.png");

    /** Private array of edit mode values. */
    private static final EditMode[] VALUES_LIST = new EditMode[] {SELECTION, PAN, ZOOM_IN, ZOOM_OUT};

    /** Collection of edit mode values. */
    public static final Collection VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_LIST));
}