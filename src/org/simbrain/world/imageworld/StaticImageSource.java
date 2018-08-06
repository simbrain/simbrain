package org.simbrain.world.imageworld;

import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * StaticImageSource allows static images (JPG, BMP, PNG) to be loaded and
 * filtered using the ImageSource interface.
 *
 * @author Tim Shea
 */
public class StaticImageSource extends ImageSourceAdapter {
    
    private String[] filenames;
    
    private int fileIndex;

    /**
     * Construct a new StaticImageSource.
     */
    public StaticImageSource() {
        super();
        filenames = new String[]{""};
        fileIndex = 0;
    }

    public StaticImageSource(String filename, BufferedImage currentImage) {
        super(currentImage);
        filenames = new String[]{filename};
        fileIndex = 0;
    }

    /**
     * Get the name of the currently loaded image file.
     */
    @Producible
    public String getFilename() {
        return filenames[fileIndex];
    }

    /**
     * Load the image file.
     */
    @Consumable
    public void loadFilename(String value) {
        if (!getFilename().equals(value)) {
            try {
                loadImage(value);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Image Load Exception");
            }
        }
    }

    /**
     * Load an image from a file and update the current image.
     *
     * @param filename the file to load.
     * @throws IOException upon failure to read the requested file
     */
    public void loadImage(String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            filenames = new String[]{""};
            setCurrentImage(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
        } else {
            BufferedImage image = ImageIO.read(new File(filename));
            filenames = new String[]{filename};
            setCurrentImage(image);
        }
    }

    /**
     * Create image from a provided image icon.
     *
     * @param imageIcon the image icon
     */
    public void loadImage(ImageIcon imageIcon) {
        filenames = new String[]{""};
        BufferedImage image = new BufferedImage(imageIcon.getIconWidth(), imageIcon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(imageIcon.getImage(), 0, 0, null);
        graphics.dispose();
        setCurrentImage(image);
    }

    @Override
    public String toString() {
        return "Image" + filenames[fileIndex];
    }
}
