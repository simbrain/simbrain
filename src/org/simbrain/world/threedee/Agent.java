package org.simbrain.world.threedee;

import org.apache.log4j.Logger;

import com.jme.math.Vector3f;

public class Agent extends Moveable {
    private static final Logger LOGGER = Logger.getLogger(Agent.class);

    private final String name;
    
    private Environment environment;
    
    private Vector3f direction;
    private Vector3f location;
    
    private Vector3f tenativeLocation;
    private Vector3f tenativeDirection;

    
    public Agent(String name) {
        LOGGER.debug("new agent: " + name);
        this.name = name;
    }
    
    @Override
    protected Vector3f getDirection() {
        return direction;
    }

    @Override
    protected Vector3f getLocation() {
        return location;
    }

    @Override
    public void init(Vector3f direction, Vector3f location) {
        this.direction = direction;
        this.location = location;
        tenativeDirection = direction;
        tenativeLocation = location;
    }

    @Override
    protected void updateDirection(Vector3f direction) {
//        this.direction = direction;
        tenativeDirection = direction;
    }

    @Override
    protected void updateLocation(Vector3f location) {
//        this.location = location;
        tenativeLocation = location;
    }
    
    @Override
    protected void doUpdates() {       
        super.doUpdates();
        
        System.out.println("agent.update");
        
//        if (!collided) {
////            tenativeDirection.addLocal(leftRightQuat.getRotationColumn(2));
////            tenativeDirection.normalizeLocal();
//        }
//      
//        tenativeLocation.addLocal(tenativeDirection.mult(getSpeed()));
//      
        float height = environment.getFloorHeight(tenativeLocation);
      
        if (!Float.isNaN(height)) tenativeLocation.setY(height + 1f);
    }
    
    private boolean collided;
    
    public void collision(Collision collision)
    {
        float speed = getSpeed();
      
        if (speed == 0) return;
      
        System.out.println("collided");
        
        Vector3f colVector = collision.point().subtract(tenativeLocation).normalizeLocal();
        float initialLength = tenativeDirection.length();
      
        tenativeDirection.subtractLocal(colVector);
              
        float finalLength = tenativeDirection.length();
        float newSpeed = (finalLength / initialLength) * speed;
        float movementSpeed = getMovementSpeed();
      
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
        
        setSpeed(speed);
        
        tenativeDirection.setY(0);
        tenativeDirection.normalizeLocal();
      
        direction = (Vector3f) tenativeDirection.clone();
      
        collided = true;
      
        doUpdates();
      
        commit();
        
        collided = false;
    }

    public SpatialData getTenative() {
        return new SpatialData(tenativeLocation, 1.0f);
    }

    public void commit() {
        if (collided) System.out.println("commit");
        direction = tenativeDirection;
        location = tenativeLocation;
    }
    
    void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}

//    
//    
//    
//    private final float rotationSpeed = 2.5f;
//    private final float movementSpeed = .1f;
//    private final Vector3f turnAxis = new Vector3f(0f, 1f, 0f);
//    private final Vector3f upDownAxis = new Vector3f(1f, 0f, 0f);
//    private final Quaternion leftRightQuat = new Quaternion();
//    private final Quaternion upDownQuat = new Quaternion();
    
//    
//    private float upDownRot = 0;
//    private float leftRightRot = 0;
//    
//    private Vector3f tenativeLocation;
//    private Vector3f tenativeDirection;
//    
//    private Vector3f direction;
//    private Vector3f location;
//    private float speed = 0f;
    
//    private SortedMap<Integer, Input> inputs = new TreeMap<Integer, Input>();
//    

//    
//    public void addInput(int priority, Input input) {
//        inputs.put(priority, input);
//    }
//
//    void setEnvironment(Environment environment) {
//        this.environment = environment;
//    }
    
//    private void doUpdates() {       
//        leftRightRot = (leftRightRot + 3600) % 360;
//        leftRightQuat.fromAngleNormalAxis(leftRightRot * FastMath.DEG_TO_RAD, turnAxis);
//        
//        upDownRot = (upDownRot + 3600) % 360;
//        upDownQuat.fromAngleAxis(upDownRot * FastMath.DEG_TO_RAD, upDownAxis);
//        
//        tenativeDirection = (Vector3f) direction.clone();
//        tenativeLocation = (Vector3f) location.clone();
//        
//        //TODO
//        if (!collided) tenativeDirection.addLocal(leftRightQuat.getRotationColumn(2));//, tenativeDirection));
//        tenativeDirection.normalizeLocal();
//        
//        tenativeLocation.addLocal(tenativeDirection.mult(speed));
//        
//        float height = environment.getFloorHeight(tenativeLocation);
//        
//        if (!Float.isNaN(height)) tenativeLocation.setY(height + 2f);
//    }
    
//    public void initSpatial(Renderer renderer, Node spatial) {
//        /* no implementation yet */
//    }
//    
//    public Node create() {
//        Box b = new Box("box", new Vector3f(), 0.35f,0.25f,0.5f);
//        b.setModelBound(new BoundingBox());
//        b.updateModelBound();
//        b.setDefaultColor(ColorRGBA.red);
//        Node node = new Node("Player Node");
//        node.attachChild(b);
//        node.setModelBound(new BoundingBox());
//        node.updateModelBound();
//        return node;
//    }
//
//    Vector3f yAxis = new Vector3f(1f, 0f, 0f);
//    
//    public void updateSpatial(Node node) {
////        node.setLocalRotation(leftRightQuat.mult(upDownQuat));
//        node.lookAt(location.add(direction), yAxis);
//        node.setLocalTranslation(location);
//    }
//    
//    int count = 0;
//    
//    boolean collided = false;
//
//    public void collision(Collision collision) {
//        float speed = getSpeed();
//        
//        if (speed == 0) return;
//        
//        Vector3f colVector = collision.point().subtract(tenativeLocation).normalizeLocal();
//        float initialLength = tenativeDirection.length();
//        
//        tenativeDirection.subtractLocal(colVector);
//                
//        float finalLength = tenativeDirection.length();
//        float newSpeed = (finalLength / initialLength) * speed;
//        float movementSpeed = getMovementSpeed();
//        
//        if (speed > 0) {
//            if (newSpeed > movementSpeed) {
//                speed = movementSpeed;
//            } else {
//                speed = newSpeed;
//            }
//        } else {
//            if (newSpeed < -movementSpeed) {
//                speed = -movementSpeed;
//            } else {
//                speed = newSpeed;
//            }
//        }
//                
//        tenativeDirection.setY(0);
//        tenativeDirection.normalizeLocal();
//        
//        direction = (Vector3f) tenativeDirection.clone();
//        
//        collided = true;
//        
//        doUpdates();
//        
//        commitModel();
//    }
//
//    public SpatialData getTenative() {
//        return new SpatialData(tenativeLocation, 1.0f);
//    }
//    
//    public void commitModel() {
//        updateLocation(tenativeLocation);
//        updateDirection(tenativeDirection);
////        location = tenativeLocation;
////        direction = tenativeDirection;        
//        collided = false;
//    }
//    
//    /*
//     * Viewable methods
//     */
//    public void init(Vector3f direction, Vector3f location) {
//        LOGGER.debug("location: "  + location);
//        LOGGER.debug("direction: "  + direction);
//        LOGGER.debug("direction.normalize: "  + direction.normalize());
//        this.direction = direction;
//        this.location = location;
//        tenativeDirection = direction;
//        tenativeLocation = location;
//    }






















//public SpatialData getSpatialData() {
//return new SpatialData(location, .5f);
//}

//public void render(Camera camera) {
//LOGGER.trace("render");
//camera.setDirection(getDirection());
//camera.setLocation(getLocation());
//camera.setLeft(getDirection().cross(camera.getUp()));
//}





////@Override
//public void updateModel() {
//  LOGGER.trace("update");
//  speed = 0f;
//  
//  for (Input input : inputs.values()) {
//      LOGGER.trace(input);
//      
//      if (input.actions.size() > 0) {;
//          input.doActions(this);
//      
//          doUpdates();
//          
////          super.update();
//          
//          return;
//      }
//  }
//}

//public static class Input
//{
//  private Set<Action> actions = new HashSet<Action>();
//  
//  public void set(Action action) {
//      LOGGER.trace(action);
//      actions.add(action);
//  }
//  
//  public void clear(Action action) {
//      actions.remove(action);
//  }
//  
//  private void doActions(Agent agent) {
//      for (Action action : actions) {
//          action.doAction(agent);
//      }
//  }
//  
//  public String toString() {
//      return "actions: " + actions.size();
//  }
//}
//
//public enum Action {
//  LEFT {
//      void doAction(Agent agent) {
//          agent.leftRightRot += agent.rotationSpeed;
//      }
//  }, 
//  
//  RIGHT {
//      void doAction(Agent agent) {
//          agent.leftRightRot -= agent.rotationSpeed;
//      }
//  }, 
//  
//  FORWARD {
//      void doAction(Agent agent) {
//          agent.speed = agent.movementSpeed;
////          agent.location = agent.location.add(
////              agent.direction.mult(agent.movementSpeed));
//      }
//  },
//  
//  BACKWARD {
//      void doAction(Agent agent) {
//          agent.speed = 0f - agent.movementSpeed;
////          agent.location = agent.location.subtract(
////              agent.direction.mult(agent.movementSpeed));
//      }
//  },
//  
//  RISE {
//      void doAction(Agent agent) {
//          agent.location.setY(agent.location.getY() + 1);
//      }
//  },
//  
//  FALL {
//      void doAction(Agent agent) {
//          agent.location.setY(agent.location.getY() - 1);
//      }
//  },
//  
//  UP {
//      void doAction(Agent agent) {
//          agent.upDownRot += agent.rotationSpeed;
//      }
//  },
//  
//  DOWN {
//      void doAction(Agent agent) {
//          agent.upDownRot -= agent.rotationSpeed;
//      }
//  },
//  
//  DROP {
//      void doAction(Agent agent) {
//          float height = agent.environment.getFloorHeight(agent.location);
//          
//          if (!Float.isNaN(height)) agent.location.setY(height);
//      }
//  }; 
//  
//  /**
//   * do not call this from outside this class!
//   */
//  abstract void doAction(Agent agent);
//}