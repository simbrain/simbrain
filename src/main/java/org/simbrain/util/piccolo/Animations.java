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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;

import java.awt.geom.AffineTransform;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Static utility methods for creating animations.
 *
 * @author Michael Heuer
 * @version $Revision$ $Date$
 */
public final class Animations {

    /**
     * Private no-arg constructor.
     */
    private Animations() {
        // empty
    }

    // single frame animations

    /**
     * Create and return a new single frame animation from the specified image
     * input stream.
     *
     * @param image image input stream
     * @return a new single frame animation from the specified image
     * @throws IOException if an IO error occurs
     */
    public static SingleFrameAnimation createAnimation(final InputStream image)
        throws IOException {
        BufferedImage bufferedImage = ImageIO.read(image);
        return createAnimation(bufferedImage);
    }

    /**
     * Create and return a new single frame animation from the specified image
     * file.
     *
     * @param image image file
     * @return a new single frame animation from the specified image
     * @throws IOException if an IO error occurs
     */
    public static SingleFrameAnimation createAnimation(final File image)
        throws IOException {
        BufferedImage bufferedImage = ImageIO.read(image);
        return createAnimation(bufferedImage);
    }

    /**
     * Create and return a new single frame animation from the specified image
     * URL.
     *
     * @param image image URL
     * @return a new single frame animation from the specified image
     * @throws IOException if an IO error occurs
     */
    public static SingleFrameAnimation createAnimation(final URL image)
        throws IOException {
        BufferedImage bufferedImage = ImageIO.read(image);
        return createAnimation(bufferedImage);
    }

    /**
     * Create and return a new single frame animation from the specified image.
     *
     * @param image image
     * @return a new single frame animation from the specified image
     */
    public static SingleFrameAnimation createAnimation(final Image image) {
        return new SingleFrameAnimation(image);
    }


    // multiple frame animations from base image

    /**
     * Create and return a new multiple frames animation containing all the
     * frame images specified from <code>baseImage</code>.
     *
     * @param baseImage base image file or URL name
     * @param suffix    image suffix
     * @param frames    number of frames
     * @return a new multiple frames animation containing all the frame images
     * specified from <code>baseImage</code>
     * @throws IOException if an IO error occurs
     */
    public static MultipleFramesAnimation createAnimation(final String baseImage, final String suffix, final int frames)
        throws IOException {
        return new MultipleFramesAnimation(createFrameList(baseImage, suffix, frames));
    }

    /**
     * Create and return a new multiple frames animation containing all the
     * frame images specified from <code>baseImage</code>.
     *
     * @param baseImage base image file
     * @param suffix    image suffix
     * @param frames    number of frames
     * @return a new multiple frames animation containing all the frame images
     * specified from <code>baseImage</code>
     * @throws IOException if an IO error occurs
     */
    public static MultipleFramesAnimation createAnimation(final File baseImage, final String suffix, final int frames)
        throws IOException {
        return new MultipleFramesAnimation(createFrameList(baseImage, suffix, frames));
    }

    /**
     * Create and return a new multiple frames animation containing all the
     * frame images specified from <code>baseImage</code>.
     *
     * @param baseImage base image URL
     * @param suffix    image suffix
     * @param frames    number of frames
     * @return a new multiple frames animation containing all the frame images
     * specified from <code>baseImage</code>
     * @throws IOException if an IO error occurs
     */
    public static MultipleFramesAnimation createAnimation(final URL baseImage, final String suffix, final int frames)
        throws IOException {
        return new MultipleFramesAnimation(createFrameList(baseImage, suffix, frames));
    }


    // looped frame animations from base image

    /**
     * Create and return a new looped frames animation containing all the frame
     * images specified from <code>baseImage</code>.
     *
     * @param baseImage base image file or URL name
     * @param suffix    image suffix
     * @param frames    number of frames
     * @return a new looped frames animation containing all the frame images
     * specified from <code>baseImage</code>
     * @throws IOException if an IO error occurs
     */
    public static LoopedFramesAnimation createLoopedAnimation(final String baseImage,
                                                              final String suffix,
                                                              final int frames)
        throws IOException {
        return new LoopedFramesAnimation(createFrameList(baseImage, suffix, frames));
    }

    /**
     * Create and return a new looped frames animation containing all the frame
     * images specified from <code>baseImage</code>.
     *
     * @param baseImage base image file
     * @param suffix    image suffix
     * @param frames    number of frames
     * @return a new looped frames animation containing all the frame images
     * specified from <code>baseImage</code>
     * @throws IOException if an IO error occurs
     */
    public static LoopedFramesAnimation createLoopedAnimation(final File baseImage,
                                                              final String suffix,
                                                              final int frames)
        throws IOException {
        return new LoopedFramesAnimation(createFrameList(baseImage, suffix, frames));
    }

    /**
     * Create and return a new looped frames animation containing all the frame
     * images specified from <code>baseImage</code>.
     *
     * @param baseImage base image URL
     * @param suffix    image suffix
     * @param frames    number of frames
     * @return a new looped frames animation containing all the frame images
     * specified from <code>baseImage</code>
     * @throws IOException if an IO error occurs
     */
    public static LoopedFramesAnimation createLoopedAnimation(final URL baseImage,
                                                              final String suffix,
                                                              final int frames)
        throws IOException {
        return new LoopedFramesAnimation(createFrameList(baseImage, suffix, frames));
    }


    // multiple frame animations from sprite sheet

    /**
     * Create and return a new multiple frames animation containing all the
     * frame images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames.
     *
     * @param spriteSheet sprite sheet input stream
     * @param x           starting location x
     * @param y           starting location y
     * @param width       frame width
     * @param height      frame height
     * @param frames      number of frames
     * @return a new multiple frames animation containing all the frame images
     * from <code>spriteSheet</code> as specified by the starting location
     * <code>(x, y)</code> and read horizontally the specified number of frames
     * @throws IOException if an IO error occurs
     */
    public static MultipleFramesAnimation createAnimation(final InputStream spriteSheet, final int x, final int y,
                                                          final int width, final int height, final int frames)
        throws IOException {
        return new MultipleFramesAnimation(createFrameList(spriteSheet, x, y, width, height, frames));
    }

    /**
     * Create and return a new multiple frames animation containing all the
     * frame images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames.
     *
     * @param spriteSheet sprite sheet file
     * @param x           starting location x
     * @param y           starting location y
     * @param width       frame width
     * @param height      frame height
     * @param frames      number of frames
     * @return a new multiple frames animation containing all the frame images
     * from <code>spriteSheet</code> as specified by the starting location
     * <code>(x, y)</code> and read horizontally the specified number of frames
     * @throws IOException if an IO error occurs
     */
    public static MultipleFramesAnimation createAnimation(final File spriteSheet, final int x, final int y,
                                                          final int width, final int height, final int frames)
        throws IOException {
        return new MultipleFramesAnimation(createFrameList(spriteSheet, x, y, width, height, frames));
    }

    /**
     * Create and return a new multiple frames animation containing all the
     * frame images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames.
     *
     * @param spriteSheet sprite sheet URL
     * @param x           starting location x
     * @param y           starting location y
     * @param width       frame width
     * @param height      frame height
     * @param frames      number of frames
     * @return a new multiple frames animation containing all the frame images
     * from <code>spriteSheet</code> as specified by the starting location
     * <code>(x, y)</code> and read horizontally the specified number of frames
     * @throws IOException if an IO error occurs
     */
    public static MultipleFramesAnimation createAnimation(final URL spriteSheet, final int x, final int y,
                                                          final int width, final int height, final int frames)
        throws IOException {
        return new MultipleFramesAnimation(createFrameList(spriteSheet, x, y, width, height, frames));
    }

    /**
     * Create and return a new multiple frames animation containing all the
     * frame images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames.
     *
     * @param spriteSheet sprite sheet image
     * @param x           starting location x
     * @param y           starting location y
     * @param width       frame width
     * @param height      frame height
     * @param frames      number of frames
     * @return a new multiple frames animation containing all the frame images
     * from <code>spriteSheet</code> as specified by the starting location
     * <code>(x, y)</code> and read horizontally the specified number of frames
     */
    public static MultipleFramesAnimation createAnimation(final BufferedImage spriteSheet, final int x, final int y,
                                                          final int width, final int height, final int frames) {
        return new MultipleFramesAnimation(createFrameList(spriteSheet, x, y, width, height, frames));
    }


    // looped frame animations from sprite sheet

    /**
     * Create and return a new looped frames animation containing all the frame
     * images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames.
     *
     * @param spriteSheet sprite sheet input stream
     * @param x           starting location x
     * @param y           starting location y
     * @param width       frame width
     * @param height      frame height
     * @param frames      number of frames
     * @return a new looped frames animation containing all the frame images
     * from <code>spriteSheet</code> as specified by the starting location
     * <code>(x, y)</code> and read horizontally the specified number of frames
     * @throws IOException if an IO error occurs
     */
    public static LoopedFramesAnimation createLoopedAnimation(final InputStream spriteSheet, final int x, final int y,
                                                              final int width, final int height, final int frames)
        throws IOException {
        return new LoopedFramesAnimation(createFrameList(spriteSheet, x, y, width, height, frames));
    }

    /**
     * Create and return a new looped frames animation containing all the frame
     * images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames.
     *
     * @param spriteSheet sprite sheet file
     * @param x           starting location x
     * @param y           starting location y
     * @param width       frame width
     * @param height      frame height
     * @param frames      number of frames
     * @return a new looped frames animation containing all the frame images
     * from <code>spriteSheet</code> as specified by the starting location
     * <code>(x, y)</code> and read horizontally the specified number of frames
     * @throws IOException if an IO error occurs
     */
    public static LoopedFramesAnimation createLoopedAnimation(final File spriteSheet, final int x, final int y,
                                                              final int width, final int height, final int frames)
        throws IOException {
        return new LoopedFramesAnimation(createFrameList(spriteSheet, x, y, width, height, frames));
    }

    /**
     * Create and return a new looped frames animation containing all the frame
     * images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames.
     *
     * @param spriteSheet sprite sheet URL
     * @param x           starting location x
     * @param y           starting location y
     * @param width       frame width
     * @param height      frame height
     * @param frames      number of frames
     * @return a new looped frames animation containing all the frame images
     * from <code>spriteSheet</code> as specified by the starting location
     * <code>(x, y)</code> and read horizontally the specified number of frames
     * @throws IOException if an IO error occurs
     */
    public static LoopedFramesAnimation createLoopedAnimation(final URL spriteSheet, final int x, final int y,
                                                              final int width, final int height, final int frames)
        throws IOException {
        return new LoopedFramesAnimation(createFrameList(spriteSheet, x, y, width, height, frames));
    }

    /**
     * Create and return a new looped frames animation containing all the frame
     * images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames.
     *
     * @param spriteSheet sprite sheet image
     * @param x           starting location x
     * @param y           starting location y
     * @param width       frame width
     * @param height      frame height
     * @param frames      number of frames
     * @return a new looped frames animation containing all the frame images
     * from <code>spriteSheet</code> as specified by the starting location
     * <code>(x, y)</code> and read horizontally the specified number of frames
     */
    public static LoopedFramesAnimation createLoopedAnimation(final BufferedImage spriteSheet, final int x, final int y,
                                                              final int width, final int height, final int frames) {
        return new LoopedFramesAnimation(createFrameList(spriteSheet, x, y, width, height, frames));
    }


    // multiple frame animations from frame list

    /**
     * Create and return a new multiple frames animation containing the
     * specified frame images.
     *
     * @param images list of frame images, must not be null
     * @return a new multiple frames animation containing the specified frame
     * images
     */
    public static MultipleFramesAnimation createAnimation(final List<Image> images) {
        return new MultipleFramesAnimation(images);
    }


    // looped frame animations from frame list

    /**
     * Create and return a new looped frames animation containing the specified
     * frame images.
     *
     * @param images list of frame images, must not be null
     * @return a new looped frames animation containing the specified frame
     * images
     */
    public static LoopedFramesAnimation createLoopedAnimation(final List<Image> images) {
        return new LoopedFramesAnimation(images);
    }


    // create frame lists

    /**
     * Create and return an unmodifiable list of images containing all the frame
     * images specified from <code>baseImage</code>.
     *
     * @param baseImage base image file or URL name
     * @param suffix    image suffix
     * @param frames    number of frames
     * @return a new unmodifiable list of images containing all the frame images
     * specified from <code>baseImage</code>
     * @throws IOException if an IO error occurs
     */
    public static List<Image> createFrameList(final String baseImage, final String suffix, final int frames)
        throws IOException {
        if (baseImage == null) {
            throw new IllegalArgumentException("baseImage must not be null");
        }
        List<Image> frameList = null;
        try {
            frameList = createFrameList(new URL(baseImage), suffix, frames);
        } catch (MalformedURLException e) {
            frameList = createFrameList(new File(baseImage), suffix, frames);
        }
        return frameList;
    }

    /**
     * Create and return an unmodifiable list of images containing all the frame
     * images specified from <code>baseImage</code>.
     *
     * @param baseImage base image file
     * @param suffix    image suffix
     * @param frames    number of frames
     * @return a new unmodifiable list of images containing all the frame images
     * specified from <code>baseImage</code>
     * @throws IOException if an IO error occurs
     */
    public static List<Image> createFrameList(final File baseImage, final String suffix, final int frames)
        throws IOException {
        if (baseImage == null) {
            throw new IllegalArgumentException("baseImage must not be null");
        }
        int leadingZeros = (int) (frames / 10) + 1; // is this math correct?
        String format = "%s%0" + leadingZeros + "d%s";
        List<Image> images = new ArrayList<Image>(frames);
        for (int frame = 0; frame < frames; frame++) {
            File file = new File(String.format(format, new Object[] {baseImage.getPath(), frame, suffix}));
            Image image = ImageIO.read(file);
            images.add(image);
        }
        return Collections.unmodifiableList(images);
    }

    /**
     * Create and return an unmodifiable list of images containing all the frame
     * images specified from <code>baseImage</code>.
     *
     * @param baseImage base image URL
     * @param suffix    image suffix
     * @param frames    number of frames
     * @return a new unmodifiable list of images containing all the frame images
     * specified from <code>baseImage</code>
     * @throws IOException if an IO error occurs
     */
    public static List<Image> createFrameList(final URL baseImage, final String suffix, final int frames)
        throws IOException {
        if (baseImage == null) {
            throw new IllegalArgumentException("baseImage must not be null");
        }
        int leadingZeros = (int) (frames / 10) + 1; // is this math correct?
        String format = "%s%0" + leadingZeros + "d%s";
        List<Image> images = new ArrayList<Image>(frames);
        for (int frame = 0; frame < frames; frame++) {
            URL url = new URL(String.format(format, new Object[] {baseImage.getPath(), frame, suffix}));
            Image image = ImageIO.read(url);
            images.add(image);
        }
        return Collections.unmodifiableList(images);
    }

    /**
     * Create and return an unmodifiable list of frame images containing all the
     * frame images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames.
     *
     * @param spriteSheet sprite sheet input stream
     * @param x           starting location x
     * @param y           starting location y
     * @param width       frame width
     * @param height      frame height
     * @param frames      number of frames
     * @return an unmodifiable list of frame images containing all the frame
     * images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames
     * @throws IOException if an IO error occurs
     */
    public static List<Image> createFrameList(final InputStream spriteSheet, final int x, final int y,
                                              final int width, final int height, final int frames)
        throws IOException {
        BufferedImage bufferedImage = ImageIO.read(spriteSheet);
        return createFrameList(bufferedImage, x, y, width, height, frames);
    }

    /**
     * Create and return an unmodifiable list of frame images containing all the
     * frame images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames.
     *
     * @param spriteSheet sprite sheet file
     * @param x           starting location x
     * @param y           starting location y
     * @param width       frame width
     * @param height      frame height
     * @param frames      number of frames
     * @return an unmodifiable list of frame images containing all the frame
     * images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames
     * @throws IOException if an IO error occurs
     */
    public static List<Image> createFrameList(final File spriteSheet, final int x, final int y,
                                              final int width, final int height, final int frames)
        throws IOException {
        BufferedImage bufferedImage = ImageIO.read(spriteSheet);
        return createFrameList(bufferedImage, x, y, width, height, frames);
    }

    /**
     * Create and return an unmodifiable list of frame images containing all the
     * frame images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames.
     *
     * @param spriteSheet sprite sheet file
     * @param x           starting location x
     * @param y           starting location y
     * @param width       frame width
     * @param height      frame height
     * @param frames      number of frames
     * @return an unmodifiable list of frame images containing all the frame
     * images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames
     * @throws IOException if an IO error occurs
     */
    public static List<Image> createFrameList(final URL spriteSheet, final int x, final int y,
                                              final int width, final int height, final int frames)
        throws IOException {
        BufferedImage bufferedImage = ImageIO.read(spriteSheet);
        return createFrameList(bufferedImage, x, y, width, height, frames);
    }

    /**
     * Create and return an unmodifiable list of frame images containing all the
     * frame images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames.
     *
     * @param spriteSheet sprite sheet image
     * @param x           starting location x
     * @param y           starting location y
     * @param width       frame width
     * @param height      frame height
     * @param frames      number of frames
     * @return an unmodifiable list of frame images containing all the frame
     * images from <code>spriteSheet</code> as specified by the starting
     * location <code>(x, y)</code> and read horizontally the specified number
     * of frames
     */
    public static List<Image> createFrameList(final BufferedImage spriteSheet, final int x, final int y,
                                              final int width, final int height, final int frames) {
        if (spriteSheet == null) {
            throw new IllegalArgumentException("spriteSheet must not be null");
        }
        List<Image> images = new ArrayList<Image>(frames);
        for (int frame = 0; frame < frames; frame++) {
            Image subimage = spriteSheet.getSubimage(x + (frame * width), y, width, height);
            images.add(subimage);
        }
        return Collections.unmodifiableList(images);
    }


    // sprite sheet utility methods

    /**
     * Create and return a new sprite sheet image containing all of the
     * specified frame images assembled horizontally.
     *
     * @param frameImages frame images, must not be null
     * @return a new sprite sheet image containing all of the specified frame
     * images assembled horizontally
     */
    public static BufferedImage createSpriteSheet(final List<Image> frameImages) {
        if (frameImages == null) {
            throw new IllegalArgumentException("frameImages must not be null");
        }
        int width = 0;
        int height = 0;
        for (Image image : frameImages) {
            if (image.getWidth(null) > width) {
                width = image.getWidth(null);
            }
            if (image.getHeight(null) > height) {
                height = image.getHeight(null);
            }
        }
        BufferedImage spriteSheet = new BufferedImage(width * frameImages.size(), height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = spriteSheet.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        for (int i = 0, size = frameImages.size(); i < size; i++) {
            Image image = frameImages.get(i);
            int x = width * i + (width / 2) - (image.getWidth(null) / 2);
            int y = (height / 2) - (image.getHeight(null) / 2);
            graphics.drawImage(image, x, y, null);
        }
        graphics.dispose();
        return spriteSheet;
    }

    /**
     * Create and return a new sprite sheet image containing all of the
     * specified frame images specified from <code>baseImage</code> assembled
     * horizontally.
     *
     * @param baseImage base image file or URL name
     * @param suffix    image suffix
     * @param frames    number of frames
     * @return a new sprite sheet image containing all of the specified frame
     * images specified from <code>baseImage</code> assembled horizontally
     * @throws IOException if an IO error occurs
     */
    public static BufferedImage createSpriteSheet(final String baseImage, final String suffix, final int frames)
        throws IOException {
        return createSpriteSheet(createFrameList(baseImage, suffix, frames));
    }

    /**
     * Create and return a new sprite sheet image containing all of the
     * specified frame images specified from <code>baseImage</code> assembled
     * horizontally.
     *
     * @param baseImage base image file
     * @param suffix    image suffix
     * @param frames    number of frames
     * @return a new sprite sheet image containing all of the specified frame
     * images specified from <code>baseImage</code> assembled horizontally
     * @throws IOException if an IO error occurs
     */
    public static BufferedImage createSpriteSheet(final File baseImage, final String suffix, final int frames)
        throws IOException {
        return createSpriteSheet(createFrameList(baseImage, suffix, frames));
    }

    /**
     * Create and return a new sprite sheet image containing all of the
     * specified frame images specified from <code>baseImage</code> assembled
     * horizontally.
     *
     * @param baseImage base image URL
     * @param suffix    image suffix
     * @param frames    number of frames
     * @return a new sprite sheet image containing all of the specified frame
     * images specified from <code>baseImage</code> assembled horizontally
     * @throws IOException if an IO error occurs
     */
    public static BufferedImage createSpriteSheet(final URL baseImage, final String suffix, final int frames)
        throws IOException {
        return createSpriteSheet(createFrameList(baseImage, suffix, frames));
    }

    /**
     * Create and return an unmodifiable list of frame images containing all the
     * frame images from <code>frameImages</code> in the same order flipped
     * horizontally.
     *
     * @param frameImages list of frame images, must not be null
     * @return an unmodifiable list of frame images containing all the frame
     * images from <code>frameImages</code> in the same order flipped
     * horizontally
     */
    public static List<Image> flipHorizontally(final List<Image> frameImages) {
        if (frameImages == null) {
            throw new IllegalArgumentException("frameImages must not be null");
        }
        int width = 0;
        int height = 0;
        for (Image image : frameImages) {
            if (image.getWidth(null) > width) {
                width = image.getWidth(null);
            }
            if (image.getHeight(null) > height) {
                height = image.getHeight(null);
            }
        }
        BufferedImage spriteSheet = new BufferedImage(width * frameImages.size(), height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = spriteSheet.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        for (int i = 0, size = frameImages.size(); i < size; i++) {
            Image image = frameImages.get(i);
            double cx = image.getWidth(null) / 2.0d;
            double cy = image.getHeight(null) / 2.0d;
            double x = width * i + (width / 2.0d) - cx;
            double y = (height / 2.0d) - cy;

            AffineTransform rotate = new AffineTransform();
            rotate.translate(cx, cy);
            rotate.concatenate(new AffineTransform(new double[] {1.0d, 0.0d, 0.0d, -1.0d}));
            rotate.translate(-cx, -cy);
            graphics.setTransform(rotate);
            graphics.drawImage(image, (int) x, (int) y, null);
        }
        graphics.dispose();
        return createFrameList(spriteSheet, 0, 0, width, height, frameImages.size());
    }

    /**
     * Create and return an unmodifiable list of frame images containing all the
     * frame images from <code>frameImages</code> in the same order flipped
     * vertically.
     *
     * @param frameImages list of frame images, must not be null
     * @return an unmodifiable list of frame images containing all the frame
     * images from <code>frameImages</code> in the same order flipped
     * vertically
     */
    public static List<Image> flipVertically(final List<Image> frameImages) {
        if (frameImages == null) {
            throw new IllegalArgumentException("frameImages must not be null");
        }
        int width = 0;
        int height = 0;
        for (Image image : frameImages) {
            if (image.getWidth(null) > width) {
                width = image.getWidth(null);
            }
            if (image.getHeight(null) > height) {
                height = image.getHeight(null);
            }
        }
        BufferedImage spriteSheet = new BufferedImage(width * frameImages.size(), height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = spriteSheet.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        for (int i = 0, size = frameImages.size(); i < size; i++) {
            Image image = frameImages.get(i);
            double cx = image.getWidth(null) / 2.0d;
            double cy = image.getHeight(null) / 2.0d;
            double x = width * i + (width / 2.0d) - cx;
            double y = (height / 2.0d) - cy;

            AffineTransform rotate = new AffineTransform();
            rotate.translate(cx, cy);
            rotate.concatenate(new AffineTransform(new double[] {-1.0d, 0.0d, 0.0d, 1.0d}));
            rotate.translate(-cx, -cy);
            graphics.setTransform(rotate);
            graphics.drawImage(image, (int) x, (int) y, null);
        }
        graphics.dispose();
        return createFrameList(spriteSheet, 0, 0, width, height, frameImages.size());
    }

    /**
     * Create and return an unmodifiable list of frame images created by
     * rotating around the center of specified image 2&#960;/steps times.
     *
     * @param image image to rotate, must not be null
     * @param steps number of steps
     * @return an unmodifiable list of frame images created by rotating around
     * the center of specified image 2&#960;/steps times
     */
    public static List<Image> rotate(final BufferedImage image, final int steps) {
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        int size = Math.max(width, height);
        BufferedImage spriteSheet = new BufferedImage(size * steps, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = spriteSheet.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        double x = width / 2.0d;
        double y = height / 2.0d;
        double r = (2.0d / (double) steps) * Math.PI;
        for (int i = 0; i < steps; i++) {
            AffineTransform rotate = new AffineTransform();
            rotate.translate(size * i, 0.0d);
            rotate.rotate(i * r, x, y);
            graphics.drawRenderedImage(image, rotate);
        }
        graphics.dispose();
        return createFrameList(spriteSheet, 0, 0, size, size, steps);
    }
}