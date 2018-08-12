/*

    dsh-piccolo-sprite  Piccolo2D sprite nodes and supporting classes.
    Copyright (c) 2006-2013 held jointly by the individual authors.

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.fsf.org/licensing/licenses/lgpl.html
    > http://www.opensource.org/licenses/lgpl-license.php

*/
package org.simbrain.util.piccolo;

import java.awt.Image;

/**
 * One or more images as frames in an animation.
 *
 * @author  Michael Heuer
 * @version $Revision$ $Date$
 */
public interface Animation
{

    /**
     * Advance this animation one frame.
     *
     * @return true if consumers of this animation should schedule a repaint
     */
    boolean advance();

    /**
     * Return the current frame for this animation.
     * The current frame will not be null.
     *
     * @return the current frame for this animation
     */
    Image getCurrentFrame();
}
