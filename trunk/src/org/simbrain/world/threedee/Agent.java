package org.simbrain.world.threedee;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.jme.bounding.BoundingBox;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;

public class Agent extends MultipleViewElement<Node> {
    private static final Logger LOGGER = Logger.getLogger(Agent.class);
    
    private final float rotationSpeed = 2.5f;
    private final float movementSpeed = 1f;
    private final Vector3f turnAxis = new Vector3f(0f, 1f, 0f);
    private final Vector3f upDownAxis = new Vector3f(1f, 0f, 0f);
    private final Quaternion rotQuat = new Quaternion();
    private final Quaternion upDownQuat = new Quaternion();
    
    private Vector3f direction;
    private Vector3f location;
    private Environment environment;
    private float upDownRot = 0;
    private float rot = 0;
    private float speed = 0f;
    
    private SortedMap<Integer, Input> inputs = new TreeMap<Integer, Input>();
    
    public void addInput(int priority, Input input) {
        inputs.put(priority, input);
    }

    void setEnvironment(Environment environment) {
        this.environment = environment;
    }
    
    boolean first = true;
    
    @Override
    public void update() {
        LOGGER.trace("update");
        speed = 0f;
        
        for (Input input : inputs.values()) {
            LOGGER.trace(input);
            
            if (input.actions.size() > 0) {;
                input.doActions(this);
            
                doUpdates();
                
                return;
            }
        }
    }
    
    private void doUpdates() {
        location = location.add(direction.mult(speed));
        
        rot = (rot + 3600) % 360;
        rotQuat.fromAngleNormalAxis(rot * FastMath.DEG_TO_RAD, turnAxis);
        
        upDownRot = (upDownRot + 3600) % 360;
        upDownQuat.fromAngleAxis(upDownRot * FastMath.DEG_TO_RAD, upDownAxis);
        
        float height = environment.getFloorHeight(location);
        
        if (!Float.isNaN(height)) location.setY(height + 1f);
        
        super.update();
    }
    
    @Override
    protected void initSpatial(Renderer renderer, Node spatial) {
        /* no implementation yet */
    }

    @Override
    public void init(Vector3f direction, Vector3f location) {
        LOGGER.debug("location: "  + location);
        this.direction = direction;
        this.location = location;
        
        super.init(direction, location);
    }
    
    @Override
    protected Node create() {
        Box b = new Box("box", new Vector3f(), 0.35f,0.25f,0.5f);
        b.setModelBound(new BoundingBox());
        b.updateModelBound();
        b.setDefaultColor(ColorRGBA.red);
        Node node = new Node("Player Node");
        node.attachChild(b);
        
        return node;
    }

    @Override
    public void updateSpatial(Node node) {
        node.setLocalRotation(rotQuat.mult(upDownQuat));
        node.setLocalTranslation(location);
    }
    
    public void render(Camera camera) {
        LOGGER.trace("render");
        
        camera.setFrame(location.add(direction.mult(movementSpeed * 3)), rotQuat.mult(upDownQuat));
        camera.update();
    }
    
    public static class Input
    {
        private Set<Action> actions = new HashSet<Action>();
        
        public void set(Action action) {
            LOGGER.trace(action);
            actions.add(action);
        }
        
        public void clear(Action action) {
            actions.remove(action);
        }
        
        private void doActions(Agent agent) {
            for (Action action : actions) {
                action.doAction(agent);
            }
        }
        
        public String toString() {
            return "actions: " + actions.size();
        }
    }
    
    public enum Action {
        LEFT {
            void doAction(Agent agent) {
                agent.rot += agent.rotationSpeed;
            }
        }, 
        
        RIGHT {
            void doAction(Agent agent) {
                agent.rot -= agent.rotationSpeed;
            }
        }, 
        
        FORWARD {
            void doAction(Agent agent) {
                agent.speed = agent.movementSpeed;
//                agent.location = agent.location.add(
//                    agent.direction.mult(agent.movementSpeed));
            }
        },
        
        BACKWARD {
            void doAction(Agent agent) {
                agent.speed = 0f - agent.movementSpeed;
//                agent.location = agent.location.subtract(
//                    agent.direction.mult(agent.movementSpeed));
            }
        },
        
        RISE {
            void doAction(Agent agent) {
                agent.location.setY(agent.location.getY() + 1);
            }
        },
        
        FALL {
            void doAction(Agent agent) {
                agent.location.setY(agent.location.getY() - 1);
            }
        },
        
        UP {
            void doAction(Agent agent) {
                agent.upDownRot += agent.rotationSpeed;
            }
        },
        
        DOWN {
            void doAction(Agent agent) {
                agent.upDownRot -= agent.rotationSpeed;
            }
        },
        
        DROP {
            void doAction(Agent agent) {
                float height = agent.environment.getFloorHeight(agent.location);
                
                if (!Float.isNaN(height)) agent.location.setY(height);
            }
        }; 
        
        /**
         * do not call this from outside this class!
         */
        abstract void doAction(Agent agent);
    }

    public void collision(Collision collision) {
        System.out.println("collision!");
    }

    public Vector3f getDirection() {
        return direction;
    }

    public float getSpeed() {
        return speed;
    }

    public SpatialData getSpatialData() {
        return new SpatialData(location, 4f);
    }
}
