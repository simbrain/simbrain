package org.simbrain.world.threedee;

import org.apache.log4j.Logger;

public class View extends ThreeDeeJPanel {
    private static final Logger LOGGER = Logger.getLogger(View.class);
    
    private static final long serialVersionUID = 1L;
    private Environment environment;
    private Agent agent;
    
    View(Agent agent, Environment environment, int width, int height) {
        super(width, height);
        
        this.environment = environment;
        this.agent = agent;
    }
    
    @Override
    public void init() {
        LOGGER.debug("init");
        environment.init(renderer, rootNode);
        agent.init(camera.getDirection(), camera.getLocation());
    }
    
    @Override
    public void update() {
        LOGGER.trace("update");
        environment.update();
    }
    
    @Override
    public void render() {
        LOGGER.trace("render");
        agent.render(camera);
    }
    
//    public IntBuffer getBuffer() {
//        Callable<IntBuffer> exe = new Callable<IntBuffer>() {
//            public IntBuffer call() {
//                try {
//                    IntBuffer buffer = ByteBuffer.allocateDirect(width * height * 4).order(
//                            ByteOrder.LITTLE_ENDIAN).asIntBuffer(); 
//                    renderer.grabScreenContents(buffer, 0, 0, width, height);
//                    
//                    return buffer;
//                } catch (Exception ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//        };
//        
//        try {
//            return GameTaskQueueManager.getManager().getQueue(GameTaskQueue.RENDER).enqueue(exe).get();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } catch (ExecutionException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
