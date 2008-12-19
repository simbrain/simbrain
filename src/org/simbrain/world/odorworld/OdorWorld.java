package org.simbrain.world.odorworld;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.util.environment.Agent;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.environment.TwoDEntity;
import org.simbrain.util.environment.TwoDEnvironment;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.odorworld.entities.MovingEntity;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.StaticEntity;
import org.simbrain.world.odorworld.entities.Wall;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Core model class of Odor World, which contains a list of entities in the world.
 * This is the class that is currently serialized.
 */
public class OdorWorld implements TwoDEnvironment {

	// TODO: Make a separate odor world model for persistence

    /** The width of the world. */
    private int worldWidth = 300;

    /** The height of the world. */
    private int worldHeight = 300;

    /** Whether or not sprites wrap around or are halted at the borders */
    private boolean wrapAround = true;

    /** The list of all non-agent entities in the world. */
    private ArrayList<OdorWorldEntity> entityList = new ArrayList<OdorWorldEntity>();

    // TODO: One problem with this this design is that this list can get out of sync.
    /** The list of all non-agent entities in the world. */
    private ArrayList<SmellSource> smellSources = new ArrayList<SmellSource>();

    /** Reference to parent component. */
    private OdorWorldComponent parent;
    
    /** Currently selected creature. */
    private MovingEntity currentCreature;
    
    /**
     * Default constructor.
     *
     * @param parent reference to parent.
     */
    OdorWorld(final OdorWorldComponent parent) {
        setParent(parent);
    }
    
    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(StaticEntity.class, "image");
        xstream.omitField(StaticEntity.class, "parent");
        xstream.omitField(OdorWorld.class, "parent");
        return xstream;
    }

    void setParent(final OdorWorldComponent parent) {
        this.parent = parent;
    }
    
    public WorkspaceComponent<?> getParent() {
        return parent;
    }

    /**
     * Add a world object at point p.  Note that it currently has a set of default values specified within the code.
     *
     * @param p the location where the object should be added
     */
    public void addStaticEntity(final double[] p) {
        StaticEntity object = new StaticEntity(this, "Swiss.gif", new double[]{50,50});
        object.setSmellSource(new SmellSource(this, new double[] {1, 1, 0, 0}, SmellSource.DecayFunction.GAUSSIAN, object.getSuggestedLocation()));
        addEntity(object);
    }
    
    /**
     * Add a world object at point p.  Note that it currently has a set of default values specified within the code.
     *
     * @param p the location where the object should be added
     */
    public void addMovingEntity(final double[] p) {
        final MovingEntity agent = new MovingEntity(this, "temp",  p);
        addEntity(agent);
    }
    
    /**
     * Add an entity.
     *
     * @param entity the entity to add.
     */
    public void addEntity(OdorWorldEntity entity) {
    	entityList.add(entity);
    	if (entity.getSmellSource() != null) {
    		smellSources.add(entity.getSmellSource());
    	}
    }
    
    /**
     * Remove an entity.
     *
     * @param entity the entity to remove.
     */
    public void removeEntity(OdorWorldEntity entity) {
    	entityList.remove(entity);
    	if (entity.getSmellSource() != null) {
    		smellSources.remove(entity.getSmellSource());
    	}
    }
    
    /**
     * Adds a wall to the world.
     */
    public void addWall() {
        Wall wall = new Wall(this);
        addEntity(wall);
        
        // TODO!
//        final Point upperLeft = determineUpperLeft(getWallPoint1(), getWallPoint2());
//
//        newWall.setWidth(Math.abs(getWallPoint2().x - getWallPoint1().x));
//        newWall.setHeight(Math.abs(getWallPoint2().y - getWallPoint1().y));
//        newWall.setX(upperLeft.x);
//        newWall.setY(upperLeft.y);
//
//        newWall.getStimulus().setStimulusVector(new double[] {0, 0, 0, 0, 0, 0, 0, 0 });
//        world.getAbstractEntityList().add(newWall);
//        setWallPoint1(null);
//        setWallPoint2(null);
//
//        drawingWalls = false;
//        this.repaint();
    }

    
    /**
     * Standard method call made to objects after they are deserialized.
     * See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     * 
     * @return Initialized object.
     */
    private Object readResolve() {
        for (OdorWorldEntity entity : entityList) {
        	entity.setParent(this);
        	entity.postSerializationInit();
        }
        return this;
    }
    
    /**
     * Check to see if the creature can move to a given new location.  If it is off screen or on top of a creature,
     * disallow the move.
     *
     * @param possibleCreatureLocation on-screen location to be checked
     *
     * @return true if the move is valid, false otherwise
     */
    public boolean validMove(final OdorWorldEntity toCheck, final Point possibleCreatureLocation) {
    	
    	boolean ret = true;
    	
        // Collisions handled here too
    	// TODO: Bump sensors
    	for (OdorWorldEntity entity : getEntityList()) {
    		if ((entity != toCheck) && (entity.inhibitsMovement())) {
    			if (entity.getLifeCycleObject() != null) {
    				if (entity.getLifeCycleObject().isDead()) {
    					continue;
    				}
     			}
    			if (entity.getBounds().contains(possibleCreatureLocation)) {
    				ret = false;
    				if (entity.getLifeCycleObject() != null) {
        				entity.getLifeCycleObject().bite();    					
    				}
    			}    				
    		}
    	}
        return ret;
    }
    
    
    /**
     * Update world.
     */
    public void update() {
    	for (OdorWorldEntity entity : entityList) {
    		if (entity.getLifeCycleObject() != null) {
    			entity.getLifeCycleObject().update();
    		}
    		entity.update();
    	}
    	checkBounds();
    }
    
    /**
     * Implements a "video-game" world or torus, such that when an object leaves on side of the screen it reappears on
     * the other.
     */
    private void checkBounds() {
    	
        if (wrapAround) {
        	for (OdorWorldEntity entity : entityList) {
        		// For now this means it's a movable entity
        		if (entity instanceof MovingEntity) {
        			double x = entity.getLocation()[0];
        			double y = entity.getLocation()[1];
                    if (x >= worldWidth) {
                    	x -= worldWidth;
                    }
                    if (x < 0) {
                        x += worldWidth;
                    }
                    if (y >= worldWidth) {
                    	y -= worldWidth;
                    }
                    if (y < 0) {
                        y += worldWidth;
                    }
                    entity.setLocation(new double[]{x,y});        			
        		}
        	}
        }
    }
	
    /**
     * {@inheritDoc}
     */
    public List<? extends Consumer> getConsumers() {
    	return getAgentList();
    }

    /**
     * {@inheritDoc}
     */
    public List<? extends Producer> getProducers() {
    	return getAgentList();
    }

    /**
     * @return the worldHeight
     */
    public int getWorldHeight() {
        return worldHeight;
    }

    /**
     * @param worldHeight the worldHeight to set
     */
    public void setWorldHeight(int worldHeight) {
        this.worldHeight = worldHeight;
    }

    /**
     * @return the worldWidth
     */
    public int getWorldWidth() {
        return worldWidth;
    }

    /**
     * @param worldWidth the worldWidth to set
     */
    public void setWorldWidth(int worldWidth) {
        this.worldWidth = worldWidth;
    }

    /**
     * {@inheritDoc}
     */
	public List<TwoDEntity> getTwoDEntityList() {
		return new ArrayList<TwoDEntity> (this.getEntityList());
	}

    /**
     * Clear all entities.
     */
	public void clearAllEntities() {
		entityList.clear();
	}

	/**
	 * @return the entityList
	 */
	public ArrayList<Agent> getAgentList() {
		ArrayList<Agent> ret = new ArrayList<Agent>();
		for(OdorWorldEntity entity : entityList) {
			if (entity instanceof MovingEntity) {
				ret.add(((MovingEntity)entity).getAgent());
			}
		}
		return ret;
	}

	/**
	 * @return the entityList
	 */
	public ArrayList<OdorWorldEntity> getEntityList() {
		return entityList;
	}

	/**
	 * @param entityList the entityList to set
	 */
	public void setEntityList(ArrayList<OdorWorldEntity> entityList) {
		this.entityList = entityList;
	}

    /**
     * {@inheritDoc}
     */
	public List<SmellSource> getSmellSources() {
		return smellSources;
	}
	
	/**
	 * Add a smell source the world.
	 *
	 * @param source the smell source to add.
	 */
	public void addSmellSource(SmellSource source) {
		smellSources.add(source);
	}

	/**
	 * @return the currentCreature
	 */
	public MovingEntity getCurrentCreature() {
		return currentCreature;
	}

	/**
	 * @param currentCreature the currentCreature to set
	 */
	public void setCurrentCreature(MovingEntity currentCreature) {
		this.currentCreature = currentCreature;
	}

}
