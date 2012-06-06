package org.simbrain.world.threedee;

import java.awt.Canvas;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import com.jme.system.DisplaySystem;
import com.jmex.awt.JMECanvas;
import com.jmex.awt.JMECanvasImplementor;
import com.jmex.awt.lwjgl.LWJGLCanvas;

/**
 * Helper class that creates a jME canvas and sets it up with.
 *
 * @author Matt Watson
 */
public class CanvasHelper {
    /** The canvas that is created. */
    private final LWJGLCanvas canvas;

    /**
     * Creates an new Canvas helper with the provided height and width using the
     * provided JMECanvasImplementor.
     *
     * @param width The width of the canvas.
     * @param height The height of the canvas.
     * @param impl The 'implementor' instance.
     */
    public CanvasHelper(final int width, final int height,
            final JMECanvasImplementor impl) {
        /* make the canvas */
        canvas = (LWJGLCanvas) DisplaySystem.getDisplaySystem("lwjgl")
                .createCanvas(width, height);

        /* add a listener... if window is resized, we can do something about it */
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent ce) {
                impl.resizeCanvas(width, height);
            }
        });

        /*
         * if canvas.setUpdateInput(false) is not called, these two lines must
         * be called KeyInput.setProvider(KeyInput.INPUT_AWT);
         * AWTMouseInput.setup(canvas, false);
         */

        final JMECanvas jmeCanvas = (canvas);
        jmeCanvas.setImplementor(impl);
        jmeCanvas.setUpdateInput(true);

        canvas.setBounds(0, 0, width, height);

        /* this prevents issues with LWJGL trying to handle input */
        canvas.setFocusable(false);
        canvas.setUpdateInput(false);
    }

    /**
     * Returns the canvas created by this class.
     *
     * @return the canvas created by this class
     */
    public Canvas getCanvas() {
        return canvas;
    }
}