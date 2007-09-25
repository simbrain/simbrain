package org.simbrain.world.threedee;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

public abstract class Moveable implements Viewable {
    private SortedMap<Integer, Input> inputs = Collections.synchronizedSortedMap(new TreeMap<Integer, Input>());
    
    private final float rotationSpeed = 2.5f;
    private final float movementSpeed = .1f;
    
    private float upDownRot = 0;
    private float leftRightRot = 0;
    private Quaternion leftRightQuat = new Quaternion();
    private Quaternion upDownQuat = new Quaternion();
    private final Vector3f xAxis = new Vector3f(1f, 0f, 0f);
    private final Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    
    private float speed = 0f;
    private float upSpeed = 0f;
    
    public void addInput(int priority, Input input) {
        inputs.put(priority, input);
    }

    public abstract void init(Vector3f direction, Vector3f location);

    int counter = 0;
    
    public void render(Camera camera) {
        Vector3f direction = getDirection();
        
        camera.setDirection(direction);
        camera.setLocation(getLocation());
        
        Vector3f left = direction.cross(yAxis).normalizeLocal();
        Vector3f up = left.cross(direction).normalizeLocal();
        
        camera.setLeft(left);
        camera.setUp(up);
        
//        if (update) {
////            System.out.println("direction: " + direction);
////            System.out.println("left:      " + left);
////            System.out.println("up:        " + up);
////            System.out.println("0:         " + leftRightQuat.getRotationColumn(0));
////            System.out.println();
//            
//            update = false;
//        }
        
//        camera.update();
    }

    public void updateView() {
        speed = 0f;
        upSpeed = 0f;
//        straighten();
        
        synchronized(inputs) {
            for (Input input : inputs.values()) {
                
                if (input.actions.size() > 0) {;
                    input.doActions(this);
                
                    doUpdates();
                    
                    return;
                }
            }
        }
    }
    
//    boolean update = false;
    
//    private void straighten()
//    {
//        Vector3f vector = new Vector3f();
//        
//        leftRightQuat = new Quaternion();
//        
//        float difference = getDirection().angleBetween(leftRightQuat.getRotationColumn(2));
//        
//        System.out.println("diff: " + difference);
//        
//        leftRightQuat.fromAngleNormalAxis(difference, yAxis);
//        
//        float angle = leftRightQuat.toAngleAxis(vector);
//        
//        System.out.println("angle: " + angle);
//        System.out.println("degrees: " + angle * FastMath.RAD_TO_DEG);
//        System.out.println("leftRightRot: " + leftRightRot);
//        System.out.println("angle: " + angle);
//        
//        System.out.println("leftRightRot: " + leftRightRot);
//        System.out.println("degrees: " + angle * FastMath.RAD_TO_DEG);
//        
//        leftRightRot = angle * FastMath.RAD_TO_DEG;
//        
//        System.out.println("leftRightRot: " + leftRightRot);
//        
//        float radians = getDirection().angleBetween(leftRightQuat.getRotationColumn(2));
//        
//        System.out.println("angle: " + radians);
//        
//        float degrees = FastMath.RAD_TO_DEG * radians;
//        
//        System.out.println("degrees: " + degrees);
//        
//        leftRightRot -= FastMath.RAD_TO_DEG * radians;
//        
//        leftRightQuat.fromAngleNormalAxis(leftRightRot * FastMath.DEG_TO_RAD, yAxis);
//    }
    
    protected void doUpdates() {
//        System.out.println("update: " + getDirection());
        
//        leftRightQuat = new Quaternion();
//        upDownQuat = new Quaternion();
        
        leftRightRot = (leftRightRot + 3600) % 360;
        leftRightQuat.fromAngleNormalAxis(leftRightRot * FastMath.DEG_TO_RAD, yAxis);
        
        upDownRot = (upDownRot + 3600) % 360;
        upDownQuat.fromAngleAxis(upDownRot * FastMath.DEG_TO_RAD, xAxis);
      
        Vector3f direction = (Vector3f) getDirection().clone();
        Vector3f location = (Vector3f) getLocation().clone();
        
//        System.out.println("angle: " + direction.angleBetween(leftRightQuat.getRotationColumn(2)));
        
        Quaternion sumQuat = leftRightQuat.mult(upDownQuat);
        
//        direction = sumQuat.getRotationColumn(2);
        direction.addLocal(sumQuat.getRotationColumn(2));
        
        direction.normalizeLocal();
        
        location.addLocal(direction.mult(speed));
        location.setY(location.getY() + upSpeed);
        
        updateLocation(location);
        updateDirection(direction);
        
//        update = true;
    }
    
    protected abstract Vector3f getLocation();
    
    protected abstract Vector3f getDirection();
    
    protected abstract void updateLocation(Vector3f location);
    
    protected abstract void updateDirection(Vector3f direction);
    
    protected void setSpeed(float speed) {
        this.speed = speed;
    }
    
    public float getSpeed() {
        return speed;
    }
    
    public float getMovementSpeed() {
        return movementSpeed;
    }
    
    public static class Input {
        private Set<Action> actions = new HashSet<Action>();
        
        public void set(Action action) {
            actions.add(action);
        }
        
        public void clear(Action action) {
            actions.remove(action);
        }
        
        private void doActions(Moveable bird) {
            for (Action action : actions) {
                action.doAction(bird);
            }
        }
        
        public String toString() {
            return "actions: " + actions.size();
        }
    }
    
    public enum Action {
        LEFT {
            void doAction(Moveable agent) {
                agent.leftRightRot += agent.rotationSpeed;
            }
        }, 
        
        RIGHT {
            void doAction(Moveable agent) {
                agent.leftRightRot -= agent.rotationSpeed;
            }
        }, 
        
        FORWARD {
            void doAction(Moveable agent) {
                agent.speed = agent.movementSpeed;
            }
        },
        
        BACKWARD {
            void doAction(Moveable agent) {
                agent.speed = 0f - agent.movementSpeed;
            }
        },
        
        RISE {
            void doAction(Moveable agent) {
                agent.upSpeed = agent.movementSpeed;
            }
        },
        
        FALL {
            void doAction(Moveable agent) {
                agent.upSpeed = 0 - agent.movementSpeed;
            }
        },
        
        UP {
            void doAction(Moveable agent) {
                agent.upDownRot += agent.rotationSpeed;
            }
        },
        
        DOWN {
            void doAction(Moveable agent) {
                agent.upDownRot -= agent.rotationSpeed;
            }
        };
                
        /**
         * do not call this from outside this class!
         */
        abstract void doAction(Moveable agent);
    }
}
