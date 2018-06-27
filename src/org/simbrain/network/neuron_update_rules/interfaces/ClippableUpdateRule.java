/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.neuron_update_rules.interfaces;

import org.simbrain.util.UserParameter;

/**
 * Interface for update rules which can clip their values above and below upper
 * and lower bounds (clipping can be turned on or off). Thus
 * ClippableUpdateRules are always BoundedUpdateRules (but some
 * BoundedUpdateRules, like sigmoidal, are not Clippable, since they have
 * intrinsic bounds).
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public interface ClippableUpdateRule {

    /**
     * Clip the current activation.
     *
     * @param val the value to clip
     * @return the clipped value
     */
    double clip(double val);

    /**
     * Is clipping turned on or not.
     *
     * @return true if clipping is on; false if it's turned off
     */
    @UserParameter(
        label = "Clipping",
        description = " If a neuron uses clipping, then if its activation exceeds its upper or lower bound, the activation is set to the upper or lower bound that it exceeds. Similarly with weights and their strength",
        defaultValue = "true", order = -1)
    boolean isClipped();

    /**
     * Turn clipping on and off.
     *
     * @param clipping true if clipping should be on; false otherwise
     */
    void setClipped(boolean clipping);

}
