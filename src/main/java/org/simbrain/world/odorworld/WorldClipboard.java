package org.simbrain.world.odorworld;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import java.awt.*;

/**
 * <b>WorldClipboard</b> is a static clipboard utility class.
 *
 * @author RJB
 */
public final class WorldClipboard {
    /**
     * Clipboard entity.
     */
    private static OdorWorldEntity clipboardEntity;

    /**
     * Default constructor.
     */
    private WorldClipboard() {
    }

    /**
     * Clear all items from clipboard.
     */
    public static void clearClipboard() {
        setClipboardEntity(null);
    }

    /**
     * Cuts an object from a world.
     *
     * @param selectedEntity Selected entity
     * @param parent         Parent world
     */
    public static void cutItem(final OdorWorldEntity selectedEntity, final OdorWorldPanel parent) {
        setClipboardEntity(selectedEntity);
        // parent.getWorld().getEntityList().remove(selectedEntity);
        parent.repaint();
    }

    /**
     * Paste an object to a world.
     *
     * @param p      Point to paste object
     * @param parent Parent world
     */
    public static void pasteItem(final Point p, final OdorWorldPanel parent) {
        OdorWorldEntity temp = getClipboardEntity();
        // TODO
        if (temp != null) {
            // temp.setParent(parent.getWorld());
            // temp.setX(p.x);
            // temp.setY(p.y);
            // parent.getWorld().getAbstractEntityList().add(temp);
            // parent.repaint();
        }

        copyItem(temp);
    }

    /**
     * Copy an abstract entity from a world.
     *
     * @param entity Entity to copy
     */
    public static void copyItem(final OdorWorldEntity entity) {
        // TODO!
        // if (entity instanceof OdorWorldEntity && !(entity instanceof
        // OdorWorldAgent)) {
        // copyEntity((OdorWorldEntity) entity);
        // } else if (entity instanceof OdorWorldAgent) {
        // copyAgent((OdorWorldAgent) entity);
        // } else if (entity instanceof Wall) {
        // copyWall((Wall) entity);
        // }
    }

    // TODO REDO ALL CLIPBOARD STUFF!

    /**
     * Copy an entity from a world.
     *
     * @param entity Entity to copy
     */
    public static void copyEntity(final OdorWorldEntity entity) {
        // OdorWorldEntity temp = new OdorWorldEntity();
        // temp.setImageName(entity.getImageName());
        // temp.setName("Copy of " + entity.getName());
        // // temp.setStimulus(entity.getStimulus());
        // temp.setImage(entity.getImage().getImage());
        // setClipboardEntity(temp);
    }

    // /**
    // * Copy an agent from a world.
    // *
    // * @param agent Agent to be copied
    // */
    // public static void copyAgent(final MovingEntity agent) {
    // MovingEntity temp = agent.copy();
    // // setClipboardEntity(temp); // TODO
    // }

    // /**
    // * Copy a wall from a world.
    // *
    // * @param wall Wall to copy
    // */
    // public static void copyWall(final Wall wall) {
    // Wall temp = new Wall(wall.getParent());
    // temp.setWidth(wall.getWidth());
    // temp.setHeight(wall.getHeight());
    // // setClipboardEntity(temp);
    // }

    /**
     * @param clipboardEntity The clipboardEntity to set.
     */
    public static void setClipboardEntity(final OdorWorldEntity clipboardEntity) {
        WorldClipboard.clipboardEntity = clipboardEntity;
    }

    /**
     * @return Returns the clipboardEntity.
     */
    public static OdorWorldEntity getClipboardEntity() {
        return clipboardEntity;
    }
}
