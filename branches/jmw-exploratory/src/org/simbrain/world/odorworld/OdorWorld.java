package org.simbrain.world.odorworld;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.CouplingContainer;
import org.simbrain.workspace.Producer;

//TODO: Model view
public class OdorWorld implements CouplingContainer {


    /** Initial color of wall. */
    private Color wallColor = Color.RED;

    /** The increment of a manual turn. */
    public static final int manualMotionTurnIncrement = 4;

    /** The initial value used in stimulus arrays. */
    private static final int stimInitVal = 10;

    /** The initial orientation for adding agents. */
    public static final int initOrientation = 45;

    /** The width of the world. */
    private int worldWidth = 300;

    /** The height of the world. */
    private int worldHeight = 300;

    /** The initial size of an object. */
    private final int initObjectSize = 35;

    /** The size of an object with an initialization to the constant value. */
    private int objectSize = initObjectSize;

    /** The boolean representing whether or not this world uses local boundaries ("clipping"). */
    private boolean useLocalBounds = false;

    /** The boolean representing whether or not an object is solid (cannot be moved through). */
    private boolean objectInhibitsMovement = true;


    /** The list of all entities in the world. */
    //World entities and entity selection
    private ArrayList<AbstractEntity> abstractEntityList = new ArrayList<AbstractEntity>();

    /** The list of all dead entities. */
    private ArrayList<OdorWorldEntity> deadEntityList = new ArrayList<OdorWorldEntity>();

    /** Current creature within the world. */
    private OdorWorldAgent currentCreature = null;

    /** List of couplings for this world. */
    private ArrayList<Coupling> couplings = new ArrayList<Coupling>();

    /** Name of world. */
    private String worldName;

    /**
     * {@inheritDoc}
     */
    public void postUnmarshallInit() {
        for (OdorWorldEntity entity : this.getEntityList()) {
            entity.setImage(ResourceManager.getImageIcon(entity.getImageName()));
        }
    }

    /**
     * Clears all entities from the world.
     */
    public void clearAllEntities() {
        while (abstractEntityList.size() > 0) {
            removeEntity((AbstractEntity) abstractEntityList.get(0));
        }
    }

    /**
     * Remove the specified world entity.
     *
     * @param entity world entity to delete
     */
    public void removeEntity(final AbstractEntity entity) {
        AbstractEntity e = entity;
        if (e != null) {
            abstractEntityList.remove(e);

            if (e instanceof OdorWorldAgent) {
                final ArrayList a = new ArrayList();
                a.add(e);
                //this.getParentFrame().getWorkspace().removeAgentsFromCouplings(a);
            }
            e = null;
        }
    }

    /**
     * @return the list of entity names
     */
    public ArrayList getEntityNames() {
        final ArrayList temp = new ArrayList();

        for (int i = 0; i < abstractEntityList.size(); i++) {
            final AbstractEntity tempElement = (AbstractEntity) abstractEntityList.get(i);

            if (tempElement instanceof OdorWorldEntity) {
                temp.add(((OdorWorldEntity) tempElement).getName());
            }
        }

        return temp;
    }

    /**
     * Go through entities in this world and find the one with the greatest number of dimensions. This will determine
     * the dimensionality of the proximal stimulus sent to the network
     *
     * @return the number of dimensions in the highest dimensional stimulus
     */
    public int getHighestDimensionalStimulus() {
        Stimulus temp = null;
        int max = 0;

        for (int i = 0; i < getEntityList().size(); i++) {
            temp = ((OdorWorldEntity) getEntityList().get(i)).getStimulus();

            if (temp.getStimulusDimension() > max) {
                max = temp.getStimulusDimension();
            }
        }

        return max;
    }

    /**
     * @return a list of entities
     */
    public ArrayList<OdorWorldEntity> getEntityList() {
        final ArrayList temp = new ArrayList();

        for (int i = 0; i < abstractEntityList.size(); i++) {
            final AbstractEntity tempElement = (AbstractEntity) abstractEntityList.get(i);

            if (tempElement instanceof OdorWorldEntity) {
                temp.add(tempElement);
            }
        }

        return temp;
    }
    /**
     * Add a world object at point p.  Note that it currently has a set of default values specified within the code.
     *
     * @param p the location where the object should be added
     */
    public void addEntity(final Point p) {
        final OdorWorldEntity we = new OdorWorldEntity();
        we.setLocation(p);
        we.setImageName("Swiss.gif");
        we.getStimulus().setStimulusVector(new double[] {stimInitVal, stimInitVal, 0, 0, 0, 0, 0, 0 });
        abstractEntityList.add(we);
    }

    /**
     * @return Returns the agentList.
     */
    public ArrayList getAgentList() {
        final ArrayList ret = new ArrayList();

        for (int i = 0; i < abstractEntityList.size(); i++) {
            final AbstractEntity temp = (AbstractEntity) abstractEntityList.get(i);

            if (temp instanceof OdorWorldAgent) {
                ret.add(temp);
            }
        }

        return ret;
    }

    /**
     * Remove the entity from the dead, return it to the living, and set its bite counter back to a default value.
     *
     * @param e Lazarus
     */
    public void resurrect(final AbstractEntity e) {
        ((OdorWorldEntity) e).reset();
        getAbstractEntityList().add(e);
        getDeadEntityList().remove(e);
    }
    /**
     * Add an agent at point p.
     *
     * @param p the location where the agent should be added
     */
    public void addAgent(final Point p) {
        final OdorWorldAgent a = new OdorWorldAgent(this, "Mouse "
                + (getAgentList().size() + 1), "Mouse.gif", p.x, p.y, initOrientation);
        a.getStimulus().setStimulusVector(new double[] {0, 0, 0, 0, 0, 0, 0, 0 });
        abstractEntityList.add(a);
        //this.getParentFrame().getWorkspace().attachAgentsToCouplings();
     }



    /**
     * Remove all objects from world.
     */
    public void clear() {
        abstractEntityList.clear();
    }

    /**
     * @return the abstractEntityList
     */
    public ArrayList getAbstractEntityList() {
        return abstractEntityList;
    }

    /**
     * @param abstractEntityList the abstractEntityList to set
     */
    public void setAbstractEntityList(ArrayList abstractEntityList) {
        this.abstractEntityList = abstractEntityList;
    }

    /**
     * @return the currentCreature
     */
    public OdorWorldAgent getCurrentCreature() {
        return currentCreature;
    }

    /**
     * @param currentCreature the currentCreature to set
     */
    public void setCurrentCreature(OdorWorldAgent currentCreature) {
        this.currentCreature = currentCreature;
    }

    /**
     * @return the deadEntityList
     */
    public ArrayList getDeadEntityList() {
        return deadEntityList;
    }

    /**
     * @param deadEntityList the deadEntityList to set
     */
    public void setDeadEntityList(ArrayList deadEntityList) {
        this.deadEntityList = deadEntityList;
    }

    /**
     * @return the objectInhibitsMovement
     */
    public boolean isObjectInhibitsMovement() {
        return objectInhibitsMovement;
    }

    /**
     * @param objectInhibitsMovement the objectInhibitsMovement to set
     */
    public void setObjectInhibitsMovement(boolean objectInhibitsMovement) {
        this.objectInhibitsMovement = objectInhibitsMovement;
    }

    /**
     * @return the objectSize
     */
    public int getObjectSize() {
        return objectSize;
    }

    /**
     * @param objectSize the objectSize to set
     */
    public void setObjectSize(int objectSize) {
        this.objectSize = objectSize;
    }

    /**
     * @return the useLocalBounds
     */
    public boolean isUseLocalBounds() {
        return useLocalBounds;
    }

    /**
     * @param useLocalBounds the useLocalBounds to set
     */
    public void setUseLocalBounds(boolean useLocalBounds) {
        this.useLocalBounds = useLocalBounds;
    }

    /**
     * @return the worldName
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * @param worldName the worldName to set
     */
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    /**
     * @return Returns the wallColor.
     */
    public int getWallColor() {
        return wallColor.getRGB();
    }

    /**
     * @param wallColor The wallColor to set.
     */
    public void setWallColor(final int wallColor) {
        this.wallColor = new Color(wallColor);
    }

    /**
     * {@inheritDoc}
     */
    public List<Consumer> getConsumers() {
        return getAgentList();
    }

    /**
     * {@inheritDoc}
     */
    public List<Coupling> getCouplings() {
        return couplings;
    }

    /**
     * {@inheritDoc}
     */
    public List<Producer> getProducers() {
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
}
