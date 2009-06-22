/**
 * From Developing Games in Java, by David Brackeen.
 */
package org.simbrain.world.odorworld.entities;

import java.awt.Image;
import java.util.ArrayList;

/**
 * The Animation class manages a series of images (frames) and the amount of
 * time to display each frame.
 */
public class Animation {

    private ArrayList frames;
    private int currFrameIndex;
    private long animTime;
    private long totalDuration;


    /**
        Creates a new, empty Animation.
    */
    public Animation() {
        this(new ArrayList(), 0);
    }
    
    public Animation(Image image) {
        this(new ArrayList(), 0);
        this.addFrame(image, 100); // Not sure what to use since this is not used (Check)
    }


    private Animation(ArrayList frames, long totalDuration) {
        this.frames = frames;
        this.totalDuration = totalDuration;
        start();
    }


    /**
        Creates a duplicate of this animation. The list of frames
        are shared between the two Animations, but each Animation
        can be animated independently.
    */
    public Object clone() {
        return new Animation(frames, totalDuration);
    }


    /**
        Adds an image to the animation with the specified
        duration (time to display the image).
    */
    public synchronized void addFrame(Image image,
        long duration)
    {
        totalDuration += duration;
        frames.add(new AnimFrame(image, totalDuration));
    }


    /**
        Starts this animation over from the beginning.
    */
    public synchronized void start() {
        animTime = 0;
        currFrameIndex = 0;
    }


    /**
        Updates this animation's current image (frame), if
        neccesary.
    */
    public synchronized void update(long elapsedTime) {
        if (frames.size() > 1) {
            animTime += elapsedTime;
            System.out.println(animTime);

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
        Gets this Animation's current image. Returns null if this
        animation has no images.
    */
    public synchronized Image getImage() {
        if (frames.size() == 0) {
            return null;
        }
        else {
            return getFrame(currFrameIndex).image;
        }
    }


    private AnimFrame getFrame(int i) {
        return (AnimFrame)frames.get(i);
    }


    private class AnimFrame {

        Image image;
        long endTime;

        public AnimFrame(Image image, long endTime) {
            this.image = image;
            this.endTime = endTime;
        }
    }
}
