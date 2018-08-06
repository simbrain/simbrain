package org.simbrain.world.imageworld.filters;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;

/**
 * ThresholdOp is a BufferedImageOp for converting an RGB image to a binary
 * (black and white) image based on a constant luminance threshold applied to each
 * pixel.
 *
 * @author Tim Shea
 */
public class ThresholdOp implements BufferedImageOp {

    private static final int white = 0x00FFFFFF;

    private static final int black = 0x00000000;

    private double threshold;

    /**
     * Construct a new ThresholdOp which maps the luminance of each pixel onto
     * either white or black output. Note that luminance is a standard weighted
     * combination of RGB values for the pixel which returns a float between 0 and 1.
     *
     * @param threshold pixels with greater than or equal luminance will be mapped
     *                  to white, all others will be mapped to black
     */
    public ThresholdOp(double threshold) {
        this.threshold = threshold;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double value) {
        threshold = value;
    }

    @Override
    public BufferedImage filter(BufferedImage source, BufferedImage destination) {
        if (destination == null) {
            destination = createCompatibleDestImage(source, null);
        }
        for (int y = 0; y < source.getHeight(); ++y) {
            for (int x = 0; x < source.getWidth(); ++x) {
                double luminance = getLuminance(source.getRGB(x, y));
                destination.setRGB(x, y, luminance >= threshold ? white : black);
            }
        }
        return destination;
    }

    /**
     * @param color a 3-byte RGB color to convert
     * @return the luminance of the color
     */
    private double getLuminance(int color) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = (color >>> 0) & 0xFF;
        return (red * 0.2126 + green * 0.7152 + blue * 0.0722) / 255;
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        return new Rectangle(0, 0, src.getWidth(), src.getHeight());
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage source, ColorModel colorModel) {
        return new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public Point2D getPoint2D(Point2D sourcePoint, Point2D destinationPoint) {
        if (destinationPoint == null) {
            return sourcePoint;
        } else {
            destinationPoint.setLocation(sourcePoint);
            return destinationPoint;
        }
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }
}