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
 * Single frame animation.
 *
 * @author  Michael Heuer
 * @version $Revision$ $Date$
 */
public final class SingleFrameAnimation
    implements Animation
{
    /** Current frame. */
    private final Image currentFrame;


    /**
     * Create a new single frame animation with the
     * specified current frame.
     *
     * @param currentFrame current frame, must not be null
     */
    public SingleFrameAnimation(final Image currentFrame)
    {
        if (currentFrame == null)
        {
            throw new IllegalArgumentException("currentFrame must not be null");
        }
        this.currentFrame = currentFrame;
    }


    @Override
    public boolean advance()
    {
        return false;
    }

    @Override
    public Image getCurrentFrame()
    {
        return currentFrame;
    }
}
