package org.simbrain.world.threedee;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.jme.renderer.Renderer;
import com.jme.util.GameTaskQueue;
import com.jme.util.GameTaskQueueManager;
import com.jmex.awt.SimpleCanvasImpl;

public class AwtView extends SimpleCanvasImpl {
//    private static final Logger LOGGER = Logger.getLogger(AwtView.class);
    
    private static final long serialVersionUID = 1L;
    private Environment environment;
    private Viewable agent;
    
    AwtView(Viewable agent, Environment environment, int width, int height) {
        super(width, height);
        
        this.environment = environment;
        this.agent = agent;
    }
    
    public Renderer getRenderer() {
        return renderer;
    }
    
    @Override
    public void simpleSetup() {
        System.out.println("frustum left: " + cam.getFrustumLeft());
        System.out.println("frustum right: " + cam.getFrustumRight());
        System.out.println("frustum top: " + cam.getFrustumTop());
        System.out.println("frustum bottom: " + cam.getFrustumBottom());
        
        environment.init(renderer, rootNode);
        agent.init(cam.getDirection(), cam.getLocation());
    }
    
    @Override
    public void simpleUpdate() {
//        environment.update();
        
        cam.update();
    }
    
    @Override
    public void simpleRender() {
        agent.render(cam);
    }
    
    public IntBuffer getBuffer() {
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
