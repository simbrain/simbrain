
package org.simnet.coupling;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Typesafe enumeration of interaction modes.
 */
public final class InteractionMode {

    /** Name of this interaction mode. */
    private final String name;


    /**
     * Create a new interaction mode with the specified name.
     *
     * @param name name of this interaction mode
     */
    private InteractionMode(final String name) {
        this.name = name;
    }


    /**
     * Return true if this interaction mode is <code>WORLD_TO_NETWORK</code>.
     *
     * @return true if this interaction mode is <code>WORLD_TO_NETWORK</code>
     */
    public boolean isWorldToNetwork() {
        return (this == WORLD_TO_NETWORK);
    }

    /**
     * Return true if this interaction mode is <code>NETWORK_TO_WORLD</code>.
     *
     * @return true if this interaction mode is <code>NETWORK_TO_WORLD</code>
     */
    public boolean isNetworkToWorld() {
        return (this == NETWORK_TO_WORLD);
    }

    /**
     * Return true if this interaction mode is <code>BOTH_WAYS</code>.
     *
     * @return true if this interaction mode is <code>BOTH_WAYS</code>
     */
    public boolean isBothWays() {
        return (this == BOTH_WAYS);
    }

    /**
     * Return true if this interaction mode is <code>NEITHER_WAY</code>.
     *
     * @return true if this interaction mode is <code>NEITHER_WAY</code>
     */
    public boolean isNeitherWay() {
        return (this == NEITHER_WAY);
    }


    /**
     * Returns String representation of interaction mode.
     *
     * @return String representing interaction mode
     */
    public String toString() {
        if (isBothWays()) {
            return "both ways";
        } else if (isNeitherWay()) {
            return "neither way";
        } else if (isWorldToNetwork()) {
            return "world to network";
        } else if (isNetworkToWorld()) {
            return "network to world";
        }
        return null;
    }

    /** World to network interaction mode.  Worlds affect networks only. */
    public static final InteractionMode WORLD_TO_NETWORK = new InteractionMode("world to network");

    /** Network to world interaction mode.  Networks affect worlds only. */
    public static final InteractionMode NETWORK_TO_WORLD = new InteractionMode("network to world");

    /** Both ways interaction mode.  Worlds affect networks and vice-versa. */
    public static final InteractionMode BOTH_WAYS = new InteractionMode("both ways");

    /** Neither way interaction mode.  Worlds and networks are decoupled. */
    public static final InteractionMode NEITHER_WAY = new InteractionMode("neither way");

    /** Private array of interaction mode values. */
    private static final InteractionMode[] values = new InteractionMode[]
                                     {BOTH_WAYS, NETWORK_TO_WORLD, WORLD_TO_NETWORK, NEITHER_WAY};

    /** Collection of interaction mode values. */
    public static final Collection VALUES = Collections.unmodifiableList(Arrays.asList(values));
}