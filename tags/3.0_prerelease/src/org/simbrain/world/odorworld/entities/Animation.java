/**
 * From Developing Games in Java, by David Brackeen.
 */
package org.simbrain.world.odorworld.entities;

import java.awt.Image;
import java.util.ArrayList;

import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;

/**
 * The Animation class manages a series of images (frames) and the amount of
 * time to display each frame.
 *
 * @author David Brackeen
 * @author Lam Nguyen
 * @author Jeff Yoshimi
 */
public class Animation {

    /** The frames that comprise this image. */
    private ArrayList<AnimFrame> frames = new ArrayList<AnimFrame>();

    /** Names of images; used for persistence. */
    private String[] imageNames;

    /** Current frame index. */
    private int currFrameIndex;

    /**
     * Local time for an animation. Every time start is called, animTime resets
     * to 0. Each update increments animTime. When animTime = totalDuration,
     * animTime resets to 0.
     */
    private long animTime;

    /** Total duration of an animation. */
    private long totalDuration;

    /**
     * The duration of each frame. Must be here so that the frame duration can
     * be persisted. Note that his assumes each frame will have the same
     * duration.
     */
    private long frameDuration;

    /**
     * Creates an animation from a list of images, specified in terms of their
     * file locations.
     *
     * @param imageLocations array of image locations.
     * @param frameDuration time to display each frame.
     */
    public Animation(final String[] imageLocations, final long frameDuration) {
        this.imageNames = imageLocations;
        this.frameDuration = frameDuration;
        initializeImages(); // Adds the frames to the animation
        start(); // Resets the animation so that time is at 0.
    }

    /**
     * An animation with a single image; equivalent to a static image.
     *
     * @param imageLocation file name of image
     */
    public Animation(final String imageLocation) {
        // Frame duration does not matter in this case, so set it to 1
        // arbitrarily.
        this(new String[] { imageLocation }, 1);
    }

    /**
     * Adds an image to the animation with the specified duration (time to
     * display the image).
     */
    public synchronized void addFrame(final Image image,
            final long frameDuration) {
        totalDuration += frameDuration;
        frames.add(new AnimFrame(image, totalDuration));
    }

    /**
     * Starts this animation over from the beginning.
     */
    public synchronized void start() {
        animTime = 0;
        currFrameIndex = 0;
    }

    /**
     * Updates this animation's current image (frame), if necessary.
     */
    public synchronized void update() {
        if (frames.size() > 1) {
            animTime += 1;
            if (animTime >= totalDuration) {
                animTime = animTime % totalDuration;
                currFrameIndex = 0;
            }

            while (animTime > getFrame(currFrameIndex).endTime) {
                currFrameIndex++;
            }
        }
    }

    /**
     * Gets this Animation's current image. Returns null if this animation has
     * no images.
     *
     * @return the image associated with the current frame.
     */
    public synchronized Image getImage() {
        if (frames.size() == 0) {
            return null;
        } else {
            return getFrame(currFrameIndex).image;
        }
    }

    /**
     * Get a specified frame.
     *
     * @param i index of frame to get.
     * @return the indicated frame
     */
    private AnimFrame getFrame(final int i) {
        return frames.get(i);
    }

    /**
     * A single frame of an animation.
     */
    private class AnimFrame {

        /** The image for this frame. */
        private Image image;

        /**
         * The "end time" for this frame is when the frame should stop
         * displaying relative to a starting animTime of 0. For example, if
         * there were three frames, and a 5-time-unit period for each, then the
         * end times for the frames would be 5, 10, and 15.
         */
        private long endTime;

        /**
         * Initialize the frame.
         *
         * @param image image
         * @param endTime end time
         */
        public AnimFrame(final Image image, final long endTime) {
            this.image = image;
            this.endTime = endTime;
        }
    }

    /**
     * @return the imageNames
     */
    public String[] getImageLocations() {
        return imageNames;
    }

    /**
     * Initialize images relative to their locations. Used when opening saved
     * odor world files.
     */
    public void initializeImages() {
        if (frames == null) {
            frames = new ArrayList<AnimFrame>();
        }
        if (imageNames.length > 0) {
            for (int i = 0; i < imageNames.length; i++) {
                this.addFrame(OdorWorldResourceManager.getImage(imageNames[i]),
                        frameDuration);
            }
        }

    }

}
