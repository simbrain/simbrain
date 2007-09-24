package org.simbrain.world.threedee;

//import org.apache.log4j.Logger;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;

public class AgentElement extends MultipleViewElement<Node> {
//    private static final Logger LOGGER = Logger.getLogger(AgentElement.class);
    
    private final Agent agent;
    
    public AgentElement(Agent agent) {
        this.agent = agent;
    }
    
    public void initSpatial(Renderer renderer, Node spatial) {
        /* no implementation yet */
    }
    
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

    Vector3f yAxis = new Vector3f(1f, 0f, 0f);
    
    public void updateSpatial(Node node) {
//        node.setLocalRotation(leftRightQuat.mult(upDownQuat));
        node.lookAt(agent.getLocation().add(agent.getDirection()), yAxis);
        node.setLocalTranslation(agent.getLocation());
    }
    
    int count = 0;
    
    boolean collided = false;

    public void collision(Collision collision) {
        agent.collision(collision);
    }

    public SpatialData getTenative() {
        return agent.getTenative();
    }
    
    public void commit() {
        agent.commit();
    }
}