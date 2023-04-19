package org.simbrain.world.imageworld;

import org.simbrain.util.ImageKt;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ImageAlbum stores a list of static images and lets you load, advance through them etc.
 *
 * @author Tim Shea
 */
public class ImageAlbum extends ImageSource implements AttributeContainer, EditableObject {

    /**
     * A list of buffered images that can be stepped through.
     */
    private List<BufferedImage> frames = new ArrayList<>();

    /**
     * Current frame being shown.
     */
    private int frameIndex = 0;

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
     * Add a new image to the album and set the current frame to it.
     */
    public void addImage(BufferedImage image) {
        frames.add(image);
        frameIndex = frames.size() - 1;
        setCurrentImage(image);
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
        getEvents().getImageUpdate().fireAndForget();
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

    /**
     * Returns number of frames in the album
     */
    public int getNumFrames() {
        if (frames == null) {
            return 0;
        }
        return frames.size();
    }

    /**
     * Set album to frame aat provided index.
     */
    public void setFrame(int frameIndex) {
        if (frameIndex >= 0 && frameIndex < frames.size()) {
            setCurrentImage(frames.get(frameIndex));
        }
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public void reset(int width, int height) {
        frames.clear();
        frameIndex = 0;
        setCurrentImage(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), true);
    }

    /**
     * Add the current image world image to the album.
     */
    public void takeSnapshot() {
        var snapshot = ImageKt.copy(getCurrentImage());
        addImage(snapshot);
    }

    public void deleteCurrentImage() {
        if (frames.size() == 0) {
            return;
        }
        if (frames.size() == 1) {
            reset(getCurrentImage().getWidth(), getCurrentImage().getHeight());
            return;
        }
        frames.remove(frameIndex);
        previousFrame();
    }

}
