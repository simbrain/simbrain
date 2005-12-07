
package org.simbrain.network;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Typesafe enumeration of build modes.
 */
public final class BuildMode {

    /** Name of this build mode. */
    private final String name;


    /**
     * Create a new build mode with the specified name.
     *
     * @param name name of this build mode
     */
    private BuildMode(final String name) {
        this.name = name;
    }


    /**
     * Return true if this build mode is <code>SELECTION</code>.
     *
     * @return true if this build mode is <code>SELECTION</code>
     */
    public boolean isSelection() {
        return (this == SELECTION);
    }

    /**
     * Return true if this build mode is <code>PAN</code>.
     *
     * @return true if this build mode is <code>PAN</code>
     */
    public boolean isPan() {
        return (this == PAN);
    }

    /**
     * Return true if this build mode is <code>ZOOM_IN</code>.
     *
     * @return true if this build mode is <code>ZOOM_IN</code>
     */
    public boolean isZoomIn() {
        return (this == ZOOM_IN);
    }

    /**
     * Return true if this build mode is <code>ZOOM_OUT</code>.
     *
     * @return true if this build mode is <code>ZOOM_OUT</code>
     */
    public boolean isZoomOut() {
        return (this == ZOOM_OUT);
    }

    /**
     * Return true if this build mode is either <code>ZOOM_IN</code>
     * or <code>ZOOM_OUT</code>.
     *
     * @return true if this build mode is either <code>ZOOM_IN</code>
     *    or <code>ZOOM_OUT</code>
     */
    public boolean isZoom() {
        return ((this == ZOOM_IN) || (this == ZOOM_OUT));
    }

    /**
     * Return true if this build mode is <code>BUILD</code>.
     *
     * @return true if this build mode is <code>BUILD</code>
     */
    public boolean isBuild() {
        return (this == BUILD);
    }


    /** Selection build mode. */
    public static final BuildMode SELECTION = new BuildMode("selection");

    /** Pan build mode. */
    public static final BuildMode PAN = new BuildMode("pan");

    /** Zoom in build mode. */
    public static final BuildMode ZOOM_IN = new BuildMode("zoom in");

    /** Zoom out build mode. */
    public static final BuildMode ZOOM_OUT = new BuildMode("zoom out");

    /** Build build mode. */
    public static final BuildMode BUILD = new BuildMode("build");

    /** Private array of build mode values. */
    private static final BuildMode[] values = new BuildMode[] { SELECTION, PAN, ZOOM_IN, ZOOM_OUT, BUILD };

    /** Collection of build mode values. */
    public static final Collection VALUES = Collections.unmodifiableList(Arrays.asList(values));
}