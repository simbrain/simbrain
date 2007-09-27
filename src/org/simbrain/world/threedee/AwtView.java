package org.simbrain.world.threedee;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.jme.renderer.Renderer;
import com.jme.util.GameTaskQueue;
import com.jme.util.GameTaskQueueManager;
import com.jmex.awt.SimpleCanvasImpl;

/**
 * This is an implementation of the jME SimpleCanvasImpl 
 * which works with a jME Canvas to allow a view to be
 * displayed in a window.  These are AWT based and therefore
 * don't work with Swing very well.  Possibly this will be
 * addressed in Java 1.7
 * 
 * @author Matt Watson
 */
public class AwtView extends SimpleCanvasImpl {
    private static final Logger LOGGER = Logger.getLogger(AwtView.class);
    
    private static final long serialVersionUID = 1L;
    
    /** The environment this view is displaying */
    private Environment environment;
    /** the viewable that controls what is seen */
    private Viewable viewable;
    
    /**
     * Constructs an instance with the provided Viewable and 
     * Environment at the given width and height
     * 
     * @param viewable the viewable that controls the view
     * @param environment the environment this view displays
     * @param width the width
     * @param height the height
     */
    AwtView(Viewable viewable, Environment environment, int width, int height) {
        super(width, height);
        
        this.environment = environment;
        this.viewable = viewable;
    }
    
    /**
     * returns the renderer for the canvas
     * 
     * @return the renderer for the canvas
     */
    public Renderer getRenderer() {
        return renderer;
    }
    
    /**
     * calls init on the environment and viewable
     */
    @Override
    public void simpleSetup() {
        LOGGER.debug("frustum left: " + cam.getFrustumLeft());
        LOGGER.debug("frustum right: " + cam.getFrustumRight());
        LOGGER.debug("frustum top: " + cam.getFrustumTop());
        LOGGER.debug("frustum bottom: " + cam.getFrustumBottom());
        
        environment.init(renderer, rootNode);
        viewable.init(cam.getDirection(), cam.getLocation());
    }
    
    /**
     * calls update on the camera
     */
    @Override
    public void simpleUpdate() {
//        environment.update();
        
        cam.update();
    }
    
    /**
     * calls render on the viewable
     */
    @Override
    public void simpleRender() {
        viewable.render(cam);
    }
    
    /**
     * captures a snapshot of the current displayed image
     * 
     * @return an IntBuffer that contains the pixel data of
     * the snapshot
     */
    public IntBuffer getBuffer() {
        /* 
         * calling this in naive way will cause an very unhelpful stack trace 
         * to be thrown.  It must be called using the game thread.
         */
        Callable<IntBuffer> exe = new Callable<IntBuffer>() {
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
            return GameTaskQueueManager.getManager().getQueue(GameTaskQueue.RENDER).enqueue(exe).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            System.out.println("timeout?");
            return null;
        }
    }
}
