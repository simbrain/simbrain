package org.simbrain.world.threedee;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.lwjgl.Sys;

public class TestJPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(TestJPanel.class);
    
    private static final long serialVersionUID = 1L;

    static {
        Sys.initialize();
    }

    /** the timer that controls animation */
    private javax.swing.Timer animationTimer = new javax.swing.Timer(10, new ActionListener() {
        /**
         * Instruct the component to paint its entire bounds
         * 
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            paintImmediately(0, 0, getWidth(), getHeight());
        }
    });

    protected final int width; 
    protected final int height; 

    protected final AwtView view;

//    Implementor implementor;
    
    /**
     * Creates a JPanel that can be rendered to by LWJGL. It sets itself to
     * opaque by default so that Swing knows that we will render our entire
     * bounds and therefore doesn't need to do any checks
     */
    public TestJPanel(final int width, final int height, final AwtView view) {
        this.width = width;
        this.height = height;
        this.view = view;
        setOpaque(true);
        
        animationTimer.start();
    }

    /**
     * override update to avoid clearing
     */
    public void update(Graphics g) {
        paint(g);
    }

    public void stop() {
        LOGGER.debug("Stopping animation timer");
        animationTimer.stop();
    }
    
    public void paintComponent(Graphics g) {
        BufferedImage renderedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        /* capture the result */
        
        IntBuffer intBuffer = view.getBuffer();
        
        if (intBuffer != null) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    renderedImage.setRGB(x, y, intBuffer.get((height - y - 1) * width + x));
                }
            }
            
            intBuffer.flip();
            
            /* draw the result to this component */
            g.drawImage(renderedImage, 0, 0, null);
        } else {
            LOGGER.debug("buffer is null");
        }
    }
}