package org.simbrain.world.threedee;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.jme.util.GameTaskQueue;
import com.jme.util.GameTaskQueueManager;
import com.jmex.awt.SimpleCanvasImpl;

public class View extends SimpleCanvasImpl {
    private Environment environment;
    private Agent agent;
    
    View(Agent agent, Environment environment, int width, int height) {
        super(width, height);
        
        this.environment = environment;
        this.agent = agent;
    }
    
    @Override
    public void simpleSetup() {
        environment.init(renderer, rootNode);
        agent.init(cam.getDirection(), cam.getLocation());
    }
    
    @Override
    public void simpleUpdate() {
        environment.update();
        agent.render(cam);
    }
    
    public IntBuffer getBuffer() {
        Callable<IntBuffer> exe = new Callable<IntBuffer>() {
            public IntBuffer call() {
                try {
                    IntBuffer buffer = ByteBuffer.allocateDirect(width * height * 4).order(
                            ByteOrder.LITTLE_ENDIAN).asIntBuffer(); 
                    renderer.grabScreenContents(buffer, 0, 0, width, height);
                    
                    return buffer;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        
        try {
            return GameTaskQueueManager.getManager().getQueue(GameTaskQueue.RENDER).enqueue(exe).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
