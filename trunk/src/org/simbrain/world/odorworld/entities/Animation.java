/**
 * From Developing Games in Java, by David Brackeen.
 */
package org.simbrain.world.odorworld.entities;

import java.awt.Image;
import java.util.ArrayList;

import org.simbrain.resource.ResourceManager;

/**
 * The Animation class manages a series of images (frames) and the amount of
 * time to display each frame.
 * 
 * TODO: Provide a way of loading images from arbitrary locations.
 */
public class Animation {

    /** The frames that comprise this image. */
    private ArrayList<AnimFrame> frames = new ArrayList<AnimFrame>();

    /** Names of images; used for persistence. */
    private String[] imageNames;
    
    /** Current frame index. */
    private int currFrameIndex;
    
    /** Length of animation. */
    private long animTime;
    
    /** Total duration of animation. */
    private long totalDuration;

    /**
     * Creates an animation from a list of images, specified in terms of their
     * file locations.
     * 
     * @param imageLocations array of image locations.
     * @param totalDuration total duration of animation.
     */
    public Animation(final String[] imageLocations, final long totalDuration) {
        this.imageNames = imageLocations;
        initializeImages();
        start();
    }

    /**
     * An animation with a single image; equivalent to a static image.
     * 
     * @param imageLocation file name of image
     */
    public Animation(final String imageLocation) {
      //Duration does not matter in this case, so set it to 1 arbitrarily.
      this(new String[]{imageLocation}, 1); 
    }

//    /**
//     * Creates a duplicate of this animation. The list of frames are shared
//     * between the two Animations, but each Animation can be animated
//     * independently.
//     */
//    public Object clone() {
//        return new Animation(frames, totalDuration);
//    }


    /**
     * Adds an image to the animation with the specified duration (time to
     * display the image).
     */
    public synchronized void addFrame(final Image image, final long duration) {
        totalDuration += duration;
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
    public synchronized void update(final long elapsedTime) {
        if (frames.size() > 1) {
            animTime += elapsedTime;
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
     */
    public synchronized Image getImage() {
        if (frames.size() == 0) {
            return null;
        }
        else {
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
        return (AnimFrame) frames.get(i);
    }


    /**
     * An single frame of an animation.
     */
    private class AnimFrame {

        /** The image for this frame. */
        Image image;

        /** End time. */
        long endTime;

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
     * @return the totalDuration
     */
    public long getTotalDuration() {
        return totalDuration;
    }

    /**
     * Initialize images relative to their locations. Used when opening saved
     * odor world files.
     */
    public void initializeImages() {
        if (frames == null) {
            frames = new ArrayList<AnimFrame>();
        }
        for (int i = 0; i < imageNames.length; i++) {
            long duration = 10;
            if ((imageNames.length > 0) && (totalDuration > 0)) {
                duration = totalDuration / imageNames.length;
            }
            this.addFrame(ResourceManager.getImage(imageNames[i]), duration);
        }
    }
}
