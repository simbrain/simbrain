package org.simbrain.world.imageworld.filters;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;

import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * ThresholdOp is a BufferedImageOp for converting an RGB image to a binary
 * (black and white) image based on a constant luminance threshold applied to each
 * pixel.
 * @author Tim Shea
 */
public class ThresholdOp implements BufferedImageOp {
    private static int white = 0x00FFFFFF;
    private static int black = 0x00000000;

    private double threshold;

    /**
     * Construct a new ThresholdOp which maps the luminance of each pixel onto
     * either white or black output. Note that luminance is a standard weighted
     * combination of RGB values for the pixel which returns a float between 0 and 1.
     * @param threshold pixels with greater than or equal luminance will be mapped
     * to white, all others will be mapped to black
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
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        if (dest == null) {
            dest = createCompatibleDestImage(src, null);
        }
        for (int y = 0; y < src.getHeight(); ++y) {
            for (int x = 0; x < src.getWidth(); ++x) {
                double luminance = getLuminance(src.getRGB(x, y));
                dest.setRGB(x, y, luminance >= threshold ? white : black);
            }
        }
        return dest;
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
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        return new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            return srcPt;
        } else {
            dstPt.setLocation(srcPt);
            return dstPt;
        }
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }
}