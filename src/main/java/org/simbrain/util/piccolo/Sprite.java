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

import org.piccolo2d.PNode;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.util.PPaintContext;

import java.awt.*;
import java.util.Collections;
import java.util.Set;

/**
 * Piccolo2D sprite node.
 *
 * @author  Michael Heuer
 * @version $Revision$ $Date$
 */
public class Sprite
    extends PNode
{
    /** Number of frames skipped. */
    private int skipped;

    /** Number of frames to skip, default <code>0</code>. */
    private final int frameSkip;

    /** Current animation for this piccolo sprite node. */
    private Animation currentAnimation;

    /** Set of animations for this piccolo sprite node. */
    private final Set<Animation> animations;


    public Sprite(final Image image) {
        this(Animations.createAnimation(image));
    }

    /**
     * Create a new piccolo sprite node with the specified animation.
     *
     * @param animation animation for this piccolo sprite node, must not be null
     */
    public Sprite(final Animation animation)
    {
        this(animation, Collections.singleton(animation));
    }

    /**
     * Create a new piccolo sprite node from the specified required parameters.
     *
     * <p>The specified current animation must be contained in the set of animations for this piccolo
     * sprite node.</p>
     *
     * <p>The specified set of animations must contain at least one animation.
     * The animations in <code>animations</code> are copied defensively
     * into this class.</p>
     *
     * @param currentAnimation current animation for this piccolo sprite node, must not be null
     *    and must be contained in the set of animations for this piccolo sprite node
     * @param animations set of animations, must not be null and must
     *    contain at least one animation
     *
     * @throws IllegalArgumentException if <code>animations.size() &lt; 1</code>
     */
    public Sprite(final Animation currentAnimation,
                  final Set<Animation> animations)
    {
        this(currentAnimation, animations, 0);
    }

    /**
     * Create a new piccolo sprite node from the specified required parameters.
     *
     * <p>The specified current animation must be contained in the set of animations for this piccolo
     * sprite node.</p>
     *
     * <p>The specified set of animations must contain at least one animation.
     * The animations in <code>animations</code> are copied defensively
     * into this class.</p>
     *
     * @param currentAnimation current animation for this piccolo sprite node, must not be null
     *    and must be contained in the set of animations for this piccolo sprite node
     * @param animations set of animations, must not be null and must
     *    contain at least one animation
     * @param frameSkip number of frames to skip
     *
     * @throws IllegalArgumentException if <code>animations.size() &lt; 1</code>
     */
    public Sprite(final Animation currentAnimation,
                  final Set<Animation> animations,
                  final int frameSkip)
    {
        super();

        if (currentAnimation == null)
        {
            throw new IllegalArgumentException("currentAnimation must not be null");
        }
        if (animations == null)
        {
            throw new IllegalArgumentException("animations must not be null");
        }
        if (animations.size() < 1)
        {
            throw new IllegalArgumentException("animations must contain at least one animation");
        }
        this.animations = animations;
        this.frameSkip = frameSkip;
        setCurrentAnimation(currentAnimation);
        Image currentFrame = currentAnimation.getCurrentFrame();
        double width = currentFrame.getWidth(null);
        double height = currentFrame.getHeight(null);
        setBounds(-width/2, -height/2, width, height);
    }


    /**
     * Advance this piccolo sprite node one frame.
     */
    public final void advance()
    {
        if (skipped < frameSkip)
        {
            skipped++;
        }
        else
        {
            // advance the current animation
            if (currentAnimation.advance())
            {
                // and schedule a repaint
                repaint();
            }
            skipped = 0;
        }
    }

    public void resetToStaticFrame() {
        currentAnimation.resetToStaticFrame();
    }

    /**
     * Return the number of frames to skip.  Defaults to <code>0</code>.
     *
     * @return the number of frames to skip
     */
    public final int getFrameSkip()
    {
        return frameSkip;
    }

    /**
     * Return the current animation for this piccolo sprite node.
     * The current animation will not be null.
     *
     * @return the current animation for this piccolo sprite node
     */
    public final Animation getCurrentAnimation()
    {
        return currentAnimation;
    }

    /**
     * Set the current animation for this piccolo sprite node to <code>currentAnimation</code>.
     * The specified animation must be contained in the set of animations for this piccolo
     * sprite node.
     *
     * <p>This is a bound property.</p>
     *
     * @see #getAnimations
     * @param currentAnimation current animation for this piccolo sprite node, must not be null
     *    and must be contained in the set of animations for this piccolo sprite node
     */
    public final void setCurrentAnimation(final Animation currentAnimation)
    {
        if (currentAnimation == null)
        {
            throw new IllegalArgumentException("currentAnimation must not be null");
        }
        if (!animations.contains(currentAnimation))
        {
            throw new IllegalArgumentException("currentAnimation must be contained in animations");
        }
        Animation oldCurrentAnimation = this.currentAnimation;
        this.currentAnimation = currentAnimation;
        firePropertyChange(-1, "currentAnimation", oldCurrentAnimation, currentAnimation);
    }

    /**
     * Return an unmodifiable set of animations for this piccolo sprite node.  The returned
     * set will not be null and will contain at least one animation.
     *
     * @return an unmodifiable set of animations for this piccolo sprite node
     */
    public final Set<Animation> getAnimations()
    {
        return Collections.unmodifiableSet(animations);
    }

    /**
     * Add the specified animation to the set of animations for this piccolo sprite node.
     * An exception may be thrown if the underlying set prevents <code>animation</code>
     * from being added.
     *
     * @param animation animation to add
     */
    public final void addAnimation(final Animation animation)
    {
        animations.add(animation);
    }

    /**
     * Remove the specified animation from the set of animations for this piccolo sprite node.
     * An exception may be thrown if the underlying set prevents <code>animation</code>
     * from being removed.
     *
     * @param animation animation to remove, must not be the current animation
     *    and must not be the last animation in the set of animations for this piccolo sprite node
     * @throws IllegalStateException if <code>animation</code> is the current
     *    animation or the last animation in the set of animations for this piccolo sprite node
     */
    public final void removeAnimation(final Animation animation)
    {
        if ((animations.size() == 1) && (animations.contains(animation)))
        {
            throw new IllegalStateException("must not remove the last animation from animations");
        }
        if (currentAnimation.equals(animation))
        {
            throw new IllegalStateException("must not remove the current animation");
        }
        animations.remove(animation);
    }

    @Override
    public final void paint(final PPaintContext paintContext)
    {
        Graphics2D g = paintContext.getGraphics();
        Image currentFrame = currentAnimation.getCurrentFrame();
        PBounds bounds = getBoundsReference();

        double w = currentFrame.getWidth(null);
        double h = currentFrame.getHeight(null);

        g.translate(bounds.getX(), bounds.getY());
        g.scale(bounds.getWidth() / w, bounds.getHeight() / h);
        g.drawImage(currentFrame, 0, 0, null);
        g.scale(w / bounds.getWidth(), h / bounds.getHeight());
        g.translate(-1 * bounds.getX(), -1 * bounds.getY());
    }
}
