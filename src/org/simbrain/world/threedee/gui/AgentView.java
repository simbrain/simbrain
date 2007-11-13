package org.simbrain.world.threedee.gui;

import org.apache.log4j.Logger;
import org.simbrain.world.threedee.AwtView;
import org.simbrain.world.threedee.Environment;
import org.simbrain.world.threedee.Viewable;

import com.jme.renderer.Renderer;
import com.jme.scene.state.CullState;
import com.jmex.awt.SimpleCanvasImpl;

public class AgentView extends SimpleCanvasImpl {

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
    AgentView(final Viewable viewable, final Environment environment, final int width, final int height) {
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
        viewable.init(cam.getDirection(), cam.getLocation());
    }
    
    /**
     * Calls update on the camera.
     */
    @Override
    public void simpleUpdate() {
        cam.update();
    }
    
    /**
     * Calls render on the viewable.
     */
    @Override
    public void simpleRender() {
        viewable.render(cam);
    }
}
