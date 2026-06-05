package org.simbrain.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;

/**
 * <b>ResourceManager</b> provides convenient access to resource files. Pass in paths relative to the resource
 * directory as root.
 */
public class ResourceManager {

    public static final int smallIconSize = Icons.SMALL;

    /**
     * Retrieve an ImageIcon based on its file name.
     *
     * @param path name of the image file to retrieve
     * @return the ImageIcon which can be used with Swing components, etc
     */
    public static ImageIcon getRawImageIcon(String path) {
        path = assertForwardSlash(path);
        URL url = ClassLoader.getSystemClassLoader().getResource(path);
        return new ImageIcon(url);
    }

    /**
     * Retrieve an Image based on its file name.
     *
     * @param path name of the image file to retrieve
     * @return the Image which can be used with Swing components, etc
     */
    public static Image getImage(String path) {
        path = assertForwardSlash(path);
        URL url = ClassLoader.getSystemClassLoader().getResource(path);
        java.awt.Toolkit toolKit = java.awt.Toolkit.getDefaultToolkit();
        return toolKit.getImage(url);
    }

    /**
     * Load an icon at the canonical small size ({@link Icons#SMALL}). Resolves to a crisp,
     * theme-aware {@link com.formdev.flatlaf.extras.FlatSVGIcon} when a matching SVG exists, else
     * a HiDPI-correct downscale of the raster. See {@link Icons} for the resolution rules.
     *
     * @param path The path of the icon to load within the resources directory.
     * @return the resolved icon, or null if the path is blank or the resource is missing.
     */
    public static ImageIcon getSmallIcon(String path) {
        if (path == null || path.trim().isEmpty()) {
            System.err.println("Could not load icon: invalid path (null or empty)");
            System.err.println("Stack trace:");
            Thread.dumpStack();
            return null;
        }
        ImageIcon icon = Icons.small(assertForwardSlash(path));
        if (icon == null) {
            System.err.println("Could not load icon: resource not found: " + path);
        }
        return icon;
    }

    /**
     * Calls to {@link ClassLoader#getResource(String)} require forward slashes, even on Windows.
     */
    public static String assertForwardSlash(String path) {
        return path.replace('\\', '/');
    }

    /**
     * Produces a Scanner from the file resource specified by the 'name' parameter.
     * Returns an Optional of Scanner if the file resource is found, or an empty Optional otherwise.
     */
    private static Optional<Scanner> createScannerFromResource(String name) {
        name = assertForwardSlash(name);
        InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
        return Optional.ofNullable(stream)
                .map(s -> new Scanner(s, StandardCharsets.UTF_8));
    }

    /**
     * Reads the entire contents of the Scanner into a String.
     * Returns an empty string if the Scanner has not got any data.
     */
    private static String readScannerContents(Scanner scanner) {
        return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
    }

    /**
     * Read file contents from a path specified relative to the resource directory (src/main/resources).
     */
    public static String readFileContents(String name) {
        return createScannerFromResource(name)
                .map(ResourceManager::readScannerContents)
                .orElse("");
    }

    /**
     * See {@link org.simbrain.world.odorworld.OdorWorldResourceManager#getBufferedImage(String)}
     */
    public static BufferedImage getBufferedImage(final String name) {
        URL url = ClassLoader.getSystemClassLoader().getResource(name);
        BufferedImage image = null;
        try {
            // source: https://stackoverflow.com/a/44170254
            ImageIO.setUseCache(false);
            image = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

}
