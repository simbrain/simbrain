package org.simbrain.world.threedee;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;

/**
 * Wraps an Agent and gives it a visible 'body' by extending
 * MultipleViewElement
 * 
 * @author Matt Watson
 */
public class AgentElement extends MultipleViewElement<Node> {
    /** the up axis */
    private static Vector3f Y_AXIS = new Vector3f(1f, 0f, 0f);
    
    /** the agent this element wraps */
    private final Agent agent;
    
    /**
     * creates an new instance for the given agent
     * @param agent
     */
    public AgentElement(Agent agent) {
        this.agent = agent;
    }
    
    /**
     * initializes one spatial node
     */
    public void initSpatial(Renderer renderer, Node spatial) {
        /* no implementation yet */
    }
    
    /**
     * creates a node for this agent
     */
    public Node create() {
        Box b = new Box("box", new Vector3f(), 0.35f,0.25f,0.5f);
        b.setModelBound(new BoundingBox());
        b.updateModelBound();
        b.setDefaultColor(ColorRGBA.red);
        Node node = new Node("Player Node");
        node.attachChild(b);
        node.setModelBound(new BoundingBox());
        node.updateModelBound();
        return node;
    }
    
    /** updates one node based on the agent */
    public void updateSpatial(Node node) {
        node.lookAt(agent.getLocation().add(agent.getDirection()), Y_AXIS);
        node.setLocalTranslation(agent.getLocation());
    }
    
    /**
     * calls agent.collision
     */
    public void collision(Collision collision) {
        agent.collision(collision);
    }

    /**
     * calls agent.getTenative
     */
    public SpatialData getTenative() {
        return agent.getTenative();
    }
    
    /**
     * calls agent.commit
     */
    public void commit() {
        agent.commit();
    }
}
