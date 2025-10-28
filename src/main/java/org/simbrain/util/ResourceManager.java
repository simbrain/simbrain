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

    public static final int smallIconSize = 18;

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
     * Load an ImageIcon from the resources directory and scale it if necessary.
     *
     * @param path The path of the icon to load within the resources directory.
     * @return Returns a scaled ImageIcon.
     */
    public static ImageIcon getSmallIcon(String path) {
        if (path == null || path.trim().isEmpty()) {
            System.err.println("Could not load icon: invalid path (null or empty)");
            System.err.println("Stack trace:");
            Thread.dumpStack();
            return null;
        }
        path = assertForwardSlash(path);
        try {
            URL url = ClassLoader.getSystemClassLoader().getResource(path);
            if (url == null) {
                System.err.println("Could not load icon: resource not found: " + path);
                return null;
            }
            ImageIcon imageIcon = new ImageIcon(url);
            Image image = imageIcon.getImage().getScaledInstance(smallIconSize, smallIconSize, Image.SCALE_SMOOTH);
            imageIcon.setImage(image);
            return imageIcon;
        } catch (Exception e) {
            System.err.println("Could not load icon: " + path + " (" + e.getMessage() + ")");
            return null;
        }
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
