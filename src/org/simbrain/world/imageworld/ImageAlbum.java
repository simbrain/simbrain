package org.simbrain.world.imageworld;

import org.simbrain.resource.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * Initial naive implementation of an "image album" that allows a current image to cycled through.
 */
public class ImageAlbum extends StaticImageSource {

    List<BufferedImage> images = Arrays.asList(
        getImage(ResourceManager.getImageIcon("Swiss.gif")),
        getImage(ResourceManager.getImageIcon("Bell.gif"))
        );

    int imageImdex = 0;

    public ImageAlbum() {
    }

    @Override
    public BufferedImage getCurrentImage() {
        return images.get(imageImdex);
    }

    public void step() {
        imageImdex++;
        if(imageImdex >= images.size()) {
            imageImdex = 0;
        }
    }

    private BufferedImage getImage(ImageIcon icon) {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(icon.getImage(), 0, 0, null);
        graphics.dispose();
        return image;
    }
}
