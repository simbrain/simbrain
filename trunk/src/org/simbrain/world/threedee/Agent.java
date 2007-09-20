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

public class Agent extends MultipleViewElement<Node> implements Viewable {
    private static final Logger LOGGER = Logger.getLogger(Agent.class);
    
    private final String name;
    
    private final float rotationSpeed = 2.5f;
    private final float movementSpeed = .1f;
    private final Vector3f turnAxis = new Vector3f(0f, 1f, 0f);
    private final Vector3f upDownAxis = new Vector3f(1f, 0f, 0f);
    private final Quaternion leftRightQuat = new Quaternion();
    private final Quaternion upDownQuat = new Quaternion();
    
    private Environment environment;
    private float upDownRot = 0;
    private float leftRightRot = 0;
    
    private Vector3f tenativeLocation;
    private Vector3f tenativeDirection;
    
    private Vector3f direction;
    private Vector3f location;
    private float speed = 0f;
    
    private SortedMap<Integer, Input> inputs = new TreeMap<Integer, Input>();
    
    public Agent(String name) {
        LOGGER.debug("new agent: " + name);
        this.name = name;
    }
    
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
                
                super.update();
                
                return;
            }
        }
    }
    
    private void doUpdates() {
//        location = location.add(direction.mult(speed));
//        rotQuat = new Quaternion();
        
//        System.out.println("update:");
//        
//        System.out.println("\tspeed: " + speed);
//        System.out.println("\tdirection: " + direction);
        
        leftRightRot = (leftRightRot + 3600) % 360;
        leftRightQuat.fromAngleNormalAxis(leftRightRot * FastMath.DEG_TO_RAD, turnAxis);
        
        upDownRot = (upDownRot + 3600) % 360;
        upDownQuat.fromAngleAxis(upDownRot * FastMath.DEG_TO_RAD, upDownAxis);
        
//        System.out.println("\tdirection: " + direction);
        
        tenativeDirection = (Vector3f) direction.clone();
        tenativeLocation = (Vector3f) location.clone();
        
//        System.out.println("\tdirection: " + tenativeDirection);
        
        //TODO
//        direction = rotQuat.getRotationColumn( 2, direction );
        if (!collided) tenativeDirection.addLocal(leftRightQuat.getRotationColumn(2));//, tenativeDirection));
        tenativeDirection.normalizeLocal();
        
//        System.out.println("\tdirection: " + tenativeDirection);
//        System.out.println("\tlocation: " + tenativeLocation);
        
        
        tenativeLocation.addLocal(tenativeDirection.mult(speed));
        
        float height = environment.getFloorHeight(tenativeLocation);
        
        if (!Float.isNaN(height)) tenativeLocation.setY(height + 2f);
    }
    
    @Override
    protected void initSpatial(Renderer renderer, Node spatial) {
        /* no implementation yet */
    }

    @Override
    public void init(Vector3f direction, Vector3f location) {
        LOGGER.debug("location: "  + location);
        LOGGER.debug("direction: "  + direction);
        LOGGER.debug("direction.normalize: "  + direction.normalize());
//        this.direction = direction.normalize();
        this.direction = direction;
        this.location = location;
        tenativeDirection = direction;
        tenativeLocation = location;
        
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
        node.setModelBound(new BoundingBox());
        node.updateModelBound();
        return node;
    }

    @Override
    public void updateSpatial(Node node) {
        node.setLocalRotation(leftRightQuat.mult(upDownQuat));
        node.setLocalTranslation(location);
    }
    
    int count = 0;
    
    public void render(Camera camera) {
        LOGGER.trace("render");
        camera.setDirection(direction);
        camera.setLocation(location);
        camera.setLeft(direction.cross(camera.getUp()));
//        camera.setFrame(location.add(direction.mult(movementSpeed * 3)), leftRightQuat.mult(upDownQuat));
        
//        if (count++ % 100 == 0) {
//            System.out.println("left: " + camera.getLeft());
//            System.out.println("direction: " + camera.getDirection());
//        }
//        camera.setFrame(location, rotQuat.mult(upDownQuat));
        camera.update();
    }
    
    boolean collided = false;

    public void collision(Collision collision) {
        if (speed == 0) return;
        
//        System.out.println(name + ": collision");
        Vector3f colVector = collision.point().subtract(tenativeLocation).normalizeLocal();
        float initialLength = tenativeDirection.length();
        
//        System.out.println("\tinitialLength: " + initialLength);
//        System.out.println("\tpoint: " + collision.point());
//        System.out.println("\tcolVector: " + colVector);
//        System.out.println("\tcolVector.length: " + colVector.length());
//        System.out.println("\tdirection: " + tenativeDirection);
//        System.out.println("\tlocation: " + tenativeLocation);
        
        tenativeDirection.subtractLocal(colVector);
        
//        System.out.println("\tdirection: " + tenativeDirection);
        
        float finalLength = tenativeDirection.length();
        
//        System.out.println("\tfinalLength: " + finalLength);
//        System.out.println("\tdirection: " + tenativeDirection);
        
//        System.out.println("\tspeed: " + speed);
        
        float newSpeed = (finalLength / initialLength) * speed;
        
        if (speed > 0) {
            if (newSpeed > movementSpeed) {
                speed = movementSpeed;
            } else {
                speed = newSpeed;
            }
        } else {
            if (newSpeed < -movementSpeed) {
                speed = -movementSpeed;
            } else {
                speed = newSpeed;
            }
        }
        
//        System.out.println("\tspeed: " + speed);
        
        tenativeDirection.setY(0);
        
        tenativeDirection.normalizeLocal();
        
//        System.out.println("\tdirection: " + tenativeDirection);
        
        direction = (Vector3f) tenativeDirection.clone();
        
        collided = true;
        
        doUpdates();
        
        commit();
        
//        System.out.println("\tdirection: " + tenativeDirection);
//        System.out.println("\tlocation: " + tenativeLocation);
        
//        try {
//            System.in.read();
//            System.in.read();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

//    public Vector3f getDirection() {
//        return direction;
//    }

//    public float getSpeed() {
//        return speed;
//    }

    public SpatialData getTenative() {
        return new SpatialData(tenativeLocation, 1.0f);
    }
    
    public void commit() {
        location = tenativeLocation;
        direction = tenativeDirection;
        
//        if (collided) {
//            System.out.println(name + ": commit");
//            
//            System.out.println("\tdirection: " + tenativeDirection);
//            System.out.println("\tlocation: " + location);
//        }
        
        collided = false;
    }
    
    public SpatialData getSpatialData() {
        return new SpatialData(location, .5f);
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
                agent.leftRightRot += agent.rotationSpeed;
            }
        }, 
        
        RIGHT {
            void doAction(Agent agent) {
                agent.leftRightRot -= agent.rotationSpeed;
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
}
