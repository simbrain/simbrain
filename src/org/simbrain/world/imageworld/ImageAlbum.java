package org.simbrain.world.imageworld;

import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * StaticImageSource allows static images (JPG, BMP, PNG) to be loaded and
 * filtered using the ImageSource interface.
 *
 * @author Tim Shea
 */
public class ImageAlbum extends ImageSourceAdapter {

    /**
     * A list of buffered images that can be stepped through.
     */
    private List<BufferedImage> frames;

    /**
     * Current frame being shown.
     */
    private int frameIndex = 0;

    // Note: If it's later desired to track file names see revision 0c8e6a

    /**
     * Construct a new StaticImageSource.
     */
    public ImageAlbum() {
        super();
    }

    public ImageAlbum(String filename, BufferedImage currentImage) {
        super(currentImage);
    }

    /**
     * Load an image from a file and update the current image.
     *
     * @param filename the file to load.
     * @throws IOException upon failure to read the requested file
     */
    @Consumable
    public void loadImage(String filename) throws IOException {
        frames = null;
        if (filename == null || filename.isEmpty()) {
            setCurrentImage(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
        } else {
            BufferedImage image = ImageIO.read(new File(filename));
            setCurrentImage(image);
        }
    }

    /**
     * Load a set of image.
     *
     * @param files the images to load
     */
    public void loadImages(File[] files) {
        List<BufferedImage> list = new ArrayList<>();
        for (File file : files) {
            try {
                BufferedImage read = ImageIO.read(file);
                if (read != null) {
                    list.add(read);
                } else {
                    JOptionPane.showMessageDialog(null, String.format("Could not parse %s", file.getName()));
                    System.err.printf("Could not parse %s", file.getName());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        frames = list;
        setCurrentImage(frames.get(0));
    }

    /**
     * Create image from a provided image icon.
     *
     * @param imageIcon the image icon
     */
    public void loadImage(ImageIcon imageIcon) {
        BufferedImage image = new BufferedImage(imageIcon.getIconWidth(), imageIcon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(imageIcon.getImage(), 0, 0, null);
        graphics.dispose();
        setCurrentImage(image);
    }

    /**
     * Update the current image to the next image in the frame list.
     */
    public void nextFrame() {
        if (frames != null) {
            frameIndex = (frameIndex + 1) % frames.size();
            setCurrentImage(frames.get(frameIndex));
        }
    }

    /**
     * Update the current image to the previous image in the frame list.
     */
    public void previousFrame() {
        if (frames != null) {
            frameIndex = (frameIndex + frames.size() - 1) % frames.size();
            setCurrentImage(frames.get(frameIndex));
        }
    }

}
