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

import java.util.List;
import java.util.ArrayList;

/**
 * Multiple frames animation.
 *
 * @author  Michael Heuer
 * @version $Revision$ $Date$
 */
public final class MultipleFramesAnimation
    implements Animation
{
    /** Index to current frame. */
    private int index;

    /** List of frames. */
    private List<Image> frames;


    /**
     * Create a new multiple frames animation with the specified list of frames.
     *
     * <p>
     * The specified list of frames must contain at least one frame.
     * The frames in <code>frames</code> are copied defensively into this class.
     * </p>
     *
     * @param frames list of frames, must not be null and must
     *    contain at least one frame
     *
     * @throws IllegalArgumentException if <code>frames.size() &lt; 1</code>
     */
    public MultipleFramesAnimation(final List<Image> frames)
    {
        if (frames == null)
        {
            throw new IllegalArgumentException("frames must not be null");
        }
        if (frames.size() < 1)
        {
            throw new IllegalArgumentException("frames must contain at least one frame");
        }
        index = 0;
        this.frames = new ArrayList<Image>(frames);
    }


    /**
     * Reset this multiple frames animation.
     */
    public void reset()
    {
        index = 0;
    }

    @Override
    public boolean advance()
    {
        index = Math.min(index + 1, frames.size() - 1);
        return true;
    }

    @Override
    public Image getCurrentFrame()
    {
        return frames.get(index);
    }
}
