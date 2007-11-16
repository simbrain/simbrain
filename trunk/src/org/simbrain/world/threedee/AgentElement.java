package org.simbrain.world.threedee;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;

/**
 * Wraps an Agent and gives it a visible 'body' by extending
 * MultipleViewElement.
 * 
 * @author Matt Watson
 */
public class AgentElement extends MultipleViewElement<Node> {
    /** The up axis. */
    private static Vector3f Y_AXIS = new Vector3f(0f, 1f, 0f);

    /** The agent this element wraps. */
    private final Agent agent;

    /**
     * Creates an new instance for the given agent.
     * 
     * @param agent
     */
    public AgentElement(final Agent agent) {
        this.agent = agent;
    }

    /**
     * Initializes one spatial node.
     */
    @Override
    public void initSpatial(final Renderer renderer, final Node spatial) {
        /* no implementation yet */
    }

    /**
     * Creates a node for this agent.
     */
    @Override
    public Node create() {
        final Box b = new Box("box", new Vector3f(), 0.35f, 0.25f, 0.5f);
        b.setModelBound(new BoundingBox());
        b.updateModelBound();
        b.setDefaultColor(ColorRGBA.red);
        final Node node = new Node("Player Node");
        node.attachChild(b);
        node.setModelBound(new BoundingBox());
        node.updateModelBound();
        return node;
    }

    /** Updates one node based on the agent. */
    @Override
    public void updateSpatial(final Node node) {
        
        if (agent.getLocation() == null) return;
        
        node.lookAt(agent.getLocation().add(agent.getDirection()), Y_AXIS);
        node.setLocalTranslation(agent.getLocation());
    }

    /**
     * Calls agent.collision.
     */
    public void collision(final Collision collision) {
        agent.collision(collision);
    }

    /**
     * Calls agent.getTenative.
     */
    public SpatialData getTentative() {
        return agent.getTenative();
    }

    /**
     * Calls agent.commit.
     */
    public void commit() {
        agent.commit();
    }
}
