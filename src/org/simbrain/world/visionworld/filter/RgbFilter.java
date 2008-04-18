package org.simbrain.world.visionworld.filter;

import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;
import org.simbrain.world.visionworld.Filter;

/**
 * Filters pixels against the provided thresholds and provides
 * the fraction of those meeting the threshold against the provided
 * lower and upper bound.
 * 
 * <p>Threshold values may be positive and negative.  Negative values
 * are meaningful and represent an upper bound e.g. a red threshold
 * of -100 would match against any pixel whose red value is less than
 * 100.  Positive thresholds are will match if the provided value is
 * greater than or equal to the threshold.
 * 
 * @author Matt Watson
 */
public class RgbFilter implements Filter {
    /** static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(RgbFilter.class);
    
    /** The number of bits in a byte. */
    private static final int BITS_IN_A_BYTE = 8;
    /** Mask for extracting a byte from a larger type. */
    private static final int BYTE_MASK = 0x000000FF;
    
    /** The red threshold. */
    private final int red;
    /** The green threshold. */
    private final int green;
    /** The blue threshold. */
    private final int blue;
    /** The lower bound of the results. */
    private final int lower;
    /** The upper bound of the results. */
    private final int upper;
    
    /**
     * Creates a new filter.
     * 
     * @param red The red threshold.
     * @param green The green threshold.
     * @param blue The blue threshold.
     * @param lower The lower bound of the results.
     * @param upper The upper bound of the results.
     */
    public RgbFilter(final int red, final int green, final int blue,
            final int lower, final int upper) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.lower = lower;
        this.upper = upper;
    }
    
    /**
     * {@inheritDoc}
     */
    public double filter(final BufferedImage image) {
        int pixels = 0;
        final int width = image.getWidth();
        final int height = image.getHeight();
        
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                int b = rgb & BYTE_MASK;
                rgb = rgb >>> BITS_IN_A_BYTE;
                int g = rgb & BYTE_MASK;
                rgb = rgb >>> BITS_IN_A_BYTE;
                int r = rgb & BYTE_MASK;
                
                LOGGER.debug("rgb(" + x + "," + y + "): " + r + ", " + g + ", " + b);
                
                if (eval(r, red) && eval(g, green) && eval(b, blue)) {
                    pixels++;
                }
            }
        }
        
        return scale(pixels, width * height);
    }
    
//    private static final String getString(int x) {
//        StringBuffer buffer = new StringBuffer();
//        int mask = 0x00000001;
//
//        for (int i = 0; i < 32; i++) {
//            buffer.insert(0, x & mask);
//            x = x >>> 1;
//        }
//
//        return buffer.toString();
//    }

    /**
     * Evaluates the given value against the provided threshold.
     * 
     * @param value The value to evaluate.
     * @param threshold The threshold.
     * @return Whether the given value meets the threshold.
     */
    private boolean eval(final int value, final int threshold) {
        if (threshold < 0) {
            return value < Math.abs(threshold);
        } else {
            return value >= threshold;
        }
    }
    
    /**
     * Scales the percentage of bytes that meet the threshold to the provided upper
     * and lower bound.
     * 
     * @param pixels The number of matching pixels.
     * @param total The total number of pixels.
     * @return The scaled value.
     */
    double scale(final int pixels, final int total) {
        double distance = Math.max(upper - lower, 0);
        
        double fraction = ((double) pixels) / total;
        
        LOGGER.debug("pixels: " + pixels + " total: " + total + " scaled: " + fraction * distance);
        
        return fraction * distance;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "RGB Filter";
    }
}
