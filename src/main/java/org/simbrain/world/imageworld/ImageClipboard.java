package org.simbrain.world.imageworld;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageClipboard implements ClipboardOwner {

    private class TransferableImage implements Transferable {

        private Image image;

        TransferableImage(Image image) {
            this.image = image;
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (flavor.equals(DataFlavor.imageFlavor)) {
                return image;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor,};
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(DataFlavor.imageFlavor);
        }
    }

    private static boolean hasContents = false;

    // TODO: Discuss
    private ImageWorld world;

    public ImageClipboard(ImageWorld world) {
        this.world = world;
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }

    public void copyImage() {
        // TODO
        // BufferedImage image = world.getCurrentSensorMatrix().getSource().getCurrentImage();
        // Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        // clipboard.setContents(new TransferableImage(image), this);
    }

    public void pasteImage() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                Image image = (Image) contents.getTransferData(DataFlavor.imageFlavor);
                BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
                Graphics graphics = bufferedImage.getGraphics();
                graphics.drawImage(image, 0, 0, null);
                graphics.dispose();
                // TODO
                // world.setImage(bufferedImage);
            } catch (UnsupportedFlavorException | IOException ex) {
                JOptionPane.showMessageDialog(null, "Unable to read image from clipboard.");
            }
        }
    }

}
