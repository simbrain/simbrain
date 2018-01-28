package org.simbrain.world.threedworld;

import org.simbrain.world.imageworld.ImageSource;
import org.simbrain.world.imageworld.ImageSourceListener;

import javax.swing.*;
import java.awt.*;

/**
 * ImagePanel is a resizable canvas for displaying images from an ImageSource.
 *
 * @author Tim Shea
 */
public class ThreeDImagePanel extends JPanel implements ImageSourceListener {
    private static final long serialVersionUID = 2582113543119990412L;

    private ImageSource nextSource;
    private ImageSource currentSource;
    //private BufferStrategy strategy;
    //private AffineTransformOp transformOp;
    //private boolean hasNativePeer = false;
    private boolean reshapeNeeded = true;
    private boolean destroyNeeded = false;
    private boolean resizeSource = false;
    private final Object lock = new Object();

    /**
     * Construct a new ImagePanel.
     */
    public ThreeDImagePanel() {
        super();
        setFocusable(true);
        repaint();
    }

    /**
     * @return Get the source of images for this panel.
     */
    public ImageSource getImageSource() {
        return currentSource;
    }

    /**
     * Assign the source of the images for this panel.
     *
     * @param value The new source to use.
     */
    public void setImageSource(ImageSource value) {
        if (currentSource == null) {
            currentSource = value;
            nextSource = value;
            currentSource.addListener(this);
        } else {
            nextSource = value;
        }
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        synchronized (lock) {
            //hasNativePeer = true;
        }
        requestFocusInWindow();
    }

    @Override
    public void removeNotify() {
        synchronized (lock) {
            //hasNativePeer = false;
        }
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (currentSource == null) {
            return;
        }
        if (currentSource.getCurrentImage() != null) {
            g.drawImage(currentSource.getCurrentImage(), 0, 0, getWidth(), getHeight(), this);
        }
    }

    // /**
    // * Draw the contents of a BufferedImage to the canvas.
    // *
    // * @param image The image to draw.
    // */
    // public void drawImage(BufferedImage image) {
    // Graphics2D graphics2d = (Graphics2D) strategy.getDrawGraphics();
    // if (graphics2d == null) {
    // return;
    // }
    // graphics2d.setRenderingHint(RenderingHints.KEY_RENDERING,
    // RenderingHints.VALUE_RENDER_SPEED);
    // graphics2d.drawImage(image, transformOp, 0, 0);
    // graphics2d.dispose();
    // strategy.show();
    // updateScreen();
    // synchronized (lock) {
    //
    // if (!hasNativePeer) {
    // if (strategy != null) {
    // strategy = null;
    // }
    // return;
    // }
    // if (strategy == null) {
    // try {
    // createBufferStrategy(1,
    // new BufferCapabilities(new ImageCapabilities(true),
    // new ImageCapabilities(true),
    // BufferCapabilities.FlipContents.UNDEFINED));
    // } catch (AWTException ex) {
    // ex.printStackTrace();
    // }
    // strategy = getBufferStrategy();
    // }
    // do {
    // do {
    // Graphics2D graphics2d = (Graphics2D) strategy
    // .getDrawGraphics();
    // if (graphics2d == null) {
    // return;
    // }
    // graphics2d.setRenderingHint(RenderingHints.KEY_RENDERING,
    // RenderingHints.VALUE_RENDER_SPEED);
    // graphics2d.drawImage(image, transformOp, 0, 0);
    // graphics2d.dispose();
    // strategy.show();
    // } while (strategy.contentsRestored());
    // } while (strategy.contentsLost());
    // }
    // }

    /**
     * Set a flag to destroy this ImagePanel on the next update.
     */
    public void destroy() {
        if (currentSource == null) {
            return;
        } else {
            destroyNeeded = true;
        }
    }

    @Override
    public void onImageUpdate(ImageSource source) {
        if (destroyNeeded) {
            currentSource.removeListener(this);
            currentSource = null;
            return;
        }
        if (nextSource != currentSource) {
            currentSource.removeListener(this);
            currentSource = nextSource;
            currentSource.addListener(this);
        }
        if (currentSource.isEnabled()) {
            repaint();
        }
    }

    @Override
    public void onResize(ImageSource source) {
        onImageUpdate(source);
    }

}
