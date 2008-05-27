package org.simbrain.world.threedee;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.simbrain.world.threedee.environment.Environment;

import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.util.GameTaskQueue;
import com.jme.util.GameTaskQueueManager;

/**
 * An implementation of Moveable that provides simple collision handling and
 * hugs the terrain of the environment provided.
 *
 * @author Matt Watson
 */
public class Agent extends Moveable implements Entity {
    /** the radius used to determine whether this agent has collided with another. */
    private static final float COLLISION_RADIUS = 0.5f;
    /** the height that the agent 'hovers' above the terrain. */
    private static final float HOVER_HEIGHT = 0.5f;

    /** the name of this agent. */
    private final String name;
    /** a logger based on this class and the agent name. */
    private Logger logger;
    /** the environment this agent lives in. */
    private Environment environment;
    /** the current location. */
    private volatile Vector direction;
    /** the current direction. */
    private volatile Point location;
    /** tentative direction. */
    private volatile Vector tenativeDirection;
    /** tentative location. */
    private volatile Point tenativeLocation;
    /** determines the limits (x, z) of the world. */
    private int limit;
    /** */
    private AgentBindings bindings;
    /** */
    private Renderer renderer;
    private int width;
    private int height;
    
    /**
     * Create a new Agent with the given name.
     *
     * @param name the agents name.
     * @param component the parent component.
     */
    public Agent(final String name, final ThreeDeeComponent component) {
        logger = Logger.getLogger("" + Agent.class + '.' + name);

        this.name = name;
        this.bindings = new AgentBindings(this, component);
        
        logger.debug("created new Agent: " + name);

        direction = new Vector(0, 0, 10).normalize();
        location = new Point(0, 0, 0);
        tenativeDirection = direction;
        tenativeLocation = location;
    }

    protected Object readResolve() {
        super.readResolve();
        
        logger = Logger.getLogger("" + Agent.class + '.' + name);

        bindings.setInputs();
        
        return this;
    }
    
    /**
     * returns the name of the agent.
     *
     * @return the name of the agent.
     */
    public String getName() {
        return name;
    }

    /**
     * sets the limit (x, z) for the world.
     *
     * @param limit the limit (x, z) for the world.
     */
    public void setLimit(final int limit) {
        this.limit = limit;
    }

    /**
     * returns the current direction.
     *
     * @return the current direction.
     */
    @Override
    public Vector getDirection() {
        return direction;
    }
    
    /**
     * Sets the current direction.
     *
     * @param v the new direction.
     */
    public void setDirection(final Vector v) {        
        this.direction = v;
        this.tenativeDirection = v;
    }

    /**
     * returns the current location.
     *
     * @return the current location.
     */
    @Override
    public Point getLocation() {
        return location;
    }

    /**
     * Sets the current direction.
     *
     * @param p The new location.
     */
    public void setTentativeLocation(final Point p) {
        this.tenativeLocation = p;
    }
    
    /**
     * Sets the current and tentative locations and directions.
     *
     * @param direction the direction vector that controls the view (updateable.)
     * @param location the location of the view (updateable.)
     */
    public void init(Renderer renderer, Camera cam, int width, int height) {//final Vector direction, final Point location) {
        cam.setDirection(direction.toVector3f());
        cam.setLocation(location.toVector3f());
        this.renderer = renderer;
        
        System.out.println("setting w, h: " + width + ", " + height);
        
        this.width = width;
        this.height = height;
    }

    /**
     * Updates the tentative direction.
     *
     * @param direction the new tentative direction.
     */
    @Override
    protected void updateDirection(final Vector direction) {
        tenativeDirection = direction;
    }

    /**
     * Updates the tentative location.
     *
     * @param location the new tentative location.
     */
    @Override
    protected void updateLocation(final Point location) {
        tenativeLocation = location;
    }

    /**
     * Calls the Moveable version and sets the height.
     */
    @Override
    protected void doUpdates() {
        super.doUpdates();

        /*
         * if the agent has gone beyond it's limit
         * move it to the other side the environment
         */
        float x = tenativeLocation.getX();
        float z = tenativeLocation.getZ();

        if (Math.abs(x) > limit) { x = (-1 * x); }
        if (Math.abs(z) > limit) { z = (-1 * z); }

        tenativeLocation = new Point(x, tenativeLocation.getY(), z);
        
        setFloor(environment.getFloorHeight(tenativeLocation));
    }

    /**
     * Updates the height based on the environment's terrain.
     */
    public void setHeight() {
        final float height = environment.getFloorHeight(tenativeLocation);

        if (!Float.isNaN(height)) { tenativeLocation = new Point(tenativeLocation.getX(),
           height + HOVER_HEIGHT, tenativeLocation.getZ()); }
    }

    /**
     * Updates the height based on the environment's terrain.
     */
    public void setFloor(final float height) {
        if (!Float.isNaN(height)) { tenativeLocation = new Point(tenativeLocation.getX(),
           height + HOVER_HEIGHT, tenativeLocation.getZ()); }
    }
    
    /**
     * Implements the logic for collisions.
     *
     * @param collision the collision detail.
     */
    public void collision(final Collision collision) {
        final float speed = getSpeed();

        if (speed == 0) { return; }

        tenativeDirection = getDirection();
        tenativeLocation = getLocation();
    }

    /**
     * Returns the tentative spatial data for this agent. This is used after an
     * update but before a commit.
     *
     * @return the tentative spatial data
     */
    public SpatialData getTentative() {
        return new SpatialData(tenativeLocation, COLLISION_RADIUS);
    }

    /**
     * Sets the location and direction to the tentative values.
     */
    public void commit() {
        direction = tenativeDirection;
        location = tenativeLocation;
    }
    
    /**
     * Sets the environment for the agent.
     *
     * @param environment the agent's environment
     */
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }
    
    /**
     * Returns this agent's environment.
     * 
     * @return This agent's environment.
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * {@inheritDoc}
     */
    public List<Odor> getOdors() {
        return Collections.singletonList(new Odor("red", this));
    }
    
    /**
     * Returns the bindings for this agent.
     * 
     * @return The bindings for this agent.
     */
    public AgentBindings getBindings() {
        return bindings;
    }
    
    public BufferedImage getSnapshot() {
        Callable<Matrix> exe = new Callable<Matrix>() {
            public Matrix call() {
                Matrix matrix = new Matrix(width, height);
                renderer.grabScreenContents(matrix.buffer, 0, 0, width, height);

                System.out.println(Thread.currentThread());
                System.out.println("returning");
                return matrix;
            }
        };
        
//        renderer.grabScreenContents(matrix.buffer, 0, 0, width, height);
        
//        GameTaskQueueManager.getManager().getQueue(GameTaskQueue.RENDER).enqueue(exe);
        Future<Matrix> future = GameTaskQueueManager.getManager().getQueue(GameTaskQueue.RENDER).enqueue(exe);
        
        Matrix matrix;// = future.get();
        
        try {
            System.out.println("getting");
            matrix = future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        
//        try {
//            while (matrix.getDone()) {
//                System.out.println("waiting");
//                Thread.sleep(100);
//            }
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        
        int xOffset = 5;//(mode.getWidth() - width) / 2;
        int yOffset = 31;//(mode.getHeight() - height) / 2;
        
        BufferedImage image = new BufferedImage(width - xOffset, height - yOffset, BufferedImage.TYPE_INT_RGB);
        
        // TODO fix this!
        // Grab each pixel information and set it to the BufferedImage info.
        for (int x = 0; x < width - xOffset; x++) {
            for (int y = 0; y < height - yOffset; y++) {
                int rgb = matrix.get(x, (height - 1) - y);
               
                image.setRGB(x, y, rgb);
            }
        }
        
        return image;
    }
    
    private static class Matrix {
        final IntBuffer buffer;
        final int width;
        final int height;
        
        Matrix(int width, int height) {
            System.out.println("creating: " + width + ", " + height);
            this.buffer = ByteBuffer.allocateDirect(width * height * 4)
                .order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
            this.width = width;
            this.height = height;
        }
        
        int get(final int x, final int y) {
//            System.out.println(x + ", " + y);
            try {
                return buffer.get((y * width) + x);
            } catch (Exception e) {
                System.err.println(x + ", " + y);
                e.printStackTrace();
                throw (RuntimeException) e;
            }
        }
    }

    public int getWidth() {
        return width > 5 ? width - 5 : 0;
    }

    public int getHeight() {
        return height > 31 ? height - 31 : 0;
    }
}
