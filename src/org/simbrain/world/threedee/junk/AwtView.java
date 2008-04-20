package org.simbrain.world.threedee.junk;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.simbrain.world.threedee.Viewable;
import org.simbrain.world.threedee.environment.Environment;

import com.jme.renderer.Renderer;
import com.jme.scene.state.CullState;
import com.jme.util.GameTaskQueue;
import com.jme.util.GameTaskQueueManager;
import com.jmex.awt.SimpleCanvasImpl;

/**
 * This is an implementation of the jME SimpleCanvasImpl which works with a jME
 * Canvas to allow a view to be displayed in a window. These are AWT based and
 * therefore don't work with Swing very well. Possibly this will be addressed in
 * Java 1.7.
 * 
 * @author Matt Watson
 */
public class AwtView extends SimpleCanvasImpl {
    private static final Logger LOGGER = Logger.getLogger(AwtView.class);

    private static final long serialVersionUID = 1L;

    /** The environment this view is displaying. */
    private final Environment environment;

    /** The viewable that controls what is seen. */
    private final Viewable viewable;

    /**
     * Constructs an instance with the provided Viewable and Environment at the
     * given width and height.
     * 
     * @param viewable the viewable that controls the view
     * @param environment the environment this view displays
     * @param width the width
     * @param height the height
     */
    AwtView(final Viewable viewable, final Environment environment, final int width,
            final int height) {
        super(width, height);

        this.environment = environment;
        this.viewable = viewable;
    }
    
    /**
     * Returns the renderer for the canvas.
     * 
     * @return the renderer for the canvas
     */
    @Override
    public Renderer getRenderer() {
        return renderer;
    }

    /**
     * Calls init on the environment and viewable.
     */
    @Override
    public void simpleSetup() {
        LOGGER.debug("frustum left: " + cam.getFrustumLeft());
        LOGGER.debug("frustum right: " + cam.getFrustumRight());
        LOGGER.debug("frustum top: " + cam.getFrustumTop());
        LOGGER.debug("frustum bottom: " + cam.getFrustumBottom());

        /* 
         * Sets up a cullstate to improve performance
         * This will prevent triangles that are not visible
         * from be rendered.
         */
        CullState cs = renderer.createCullState();
        cs.setCullMode(CullState.CS_BACK);
        rootNode.setRenderState(cs);
        
        environment.init(getRenderer(), rootNode);
//        viewable.init(cam.getDirection(), cam.getLocation());
    }

    /**
     * Calls update on the camera.
     */
    @Override
    public void simpleUpdate() {
        // environment.update();

        cam.update();
    }

    /**
     * Calls render on the viewable.
     */
    @Override
    public void simpleRender() {
        viewable.render(cam);
    }

    /**
     * Captures a snapshot of the current displayed image.
     * 
     * @return an IntBuffer that contains the pixel data of the snapshot
     */
    public IntBuffer getBuffer() {
        /**
         * Calling this in naive way will cause an very unhelpful stack trace to
         * be thrown. It must be called using the game thread.
         */
        final Callable<IntBuffer> exe = new Callable<IntBuffer>() {
            public IntBuffer call() {
                try {
                    System.out.println("creating buffer");
                    IntBuffer buffer = ByteBuffer.allocateDirect(width * height * 4).order(
                            ByteOrder.LITTLE_ENDIAN).asIntBuffer();

                    System.out.println("buffer created");
                    renderer.grabScreenContents(buffer, 0, 0, width, height);

                    System.out.println("finished");
                    return buffer;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };

        try {
            return GameTaskQueueManager.getManager().getQueue(
                   GameTaskQueue.RENDER).enqueue(exe).get(10, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        } catch (final TimeoutException e) {
            LOGGER.debug("enqueue timed out", e);
            return null;
        }
    }
}
