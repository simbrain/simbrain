/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.odorworld.gui;

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PImage;
import org.simbrain.util.piccolo.LoopedFramesAnimation;
import org.simbrain.util.piccolo.Sprite;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.entities.RotatingEntityManager;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.TreeMap;

/**
 * Piccolo representation of an {@link OdorWorldEntity}.
 */
public class EntityNode extends PNode {

    /**
     * Parent odor world.
     */
    private final OdorWorld parent;

    /**
     * Model entity being represented.
     */
    private final OdorWorldEntity entity;

    /**
     * Default image.
     */
    private static final String DEFAULT_IMAGE = "Swiss.gif";

    /**
     * Image representing this entity.
     */
    private transient PImage image;

    /**
     * Flag to indicate this node's location has changed and should be updated at the next update
     */
    private boolean updateFlag;

    // TODO: Use?
    public Sprite sprite;
    public final LoopedFramesAnimation animation2 = null;
    // Old solution. Not sure whether to keep it..
    private TreeMap<Double, Image> imageMap = null;
    // TODO: Replace image with image source type of design?

    /**
     * Construct an entity node with a back-ref to parent.
     *
     * @param world  parent world
     * @param entity represented entity
     */
    public EntityNode(final OdorWorld world, final OdorWorldEntity entity) {
        this.parent = world;
        this.entity = entity;

        // this.setPickable(true); //Needed?

        updateImage();

        this.centerFullBoundsOnPoint(entity.getX(), entity.getY());

        entity.addPropertyChangeListener(evt -> {
            if ("propertiesChanged".equals(evt.getPropertyName())) {
                updateImage();
            } else if ("deleted".equals(evt.getPropertyName())) {
                this.removeFromParent();
            } else if ("moved".equals(evt.getPropertyName())) {
                updateFlag = true;
            } else if ("updated".equals(evt.getPropertyName())) {
                update();
            }
        });
    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    public void pushViewPositionToModel() {
        Point2D p = this.getGlobalTranslation();
        entity.setX(p.getX());
        entity.setY(p.getY());
    }

    /**
     * Initialize the image associated with the object.
     */
    private void updateImage() {

        this.removeChild(image);

        switch (entity.getEntityType()) {
        case SWISS:
            this.image = new PImage(OdorWorldResourceManager.getStaticImage(DEFAULT_IMAGE));
            break;
        case FLOWER:
            this.image = new PImage(OdorWorldResourceManager.getStaticImage("pansy.gif"));
            break;
        case MOUSE:
            this.image = new PImage(OdorWorldResourceManager.getRotatingImage("mouse/Mouse_0.gif"));
            break;
        default:
            break;
        }

        // Good enough for now (?), while the world is just 2d
        // things that can sometimes rotate
        if(entity instanceof RotatingEntity) {
            initTreeMap();
        }

        this.addChild(image);

    }

    /**
     * Initialize the tree map, which associates angles with images /
     * animations.
     */
    private void initTreeMap() {
//        if (entityType.equalsIgnoreCase("Circle")) {
//            imageMap = RotatingEntityManager.getCircle();
//        }
        if (entity.getEntityType() == OdorWorldEntity.EntityType.MOUSE) {
            imageMap = RotatingEntityManager.getMouse();
        }
//        else if (entityType.equalsIgnoreCase("Amy")) {
//            imageMap = RotatingEntityManager.getRotatingTileset("amy", 20);
//        } else if (entityType.equalsIgnoreCase("Arnold")) {
//            imageMap = RotatingEntityManager.getRotatingTileset("arno", 20);
//        } else if (entityType.equalsIgnoreCase("Boy")) {
//            imageMap = RotatingEntityManager.getRotatingTileset("boy", 20);
//        } else if (entityType.equalsIgnoreCase("Cow")) {
//            imageMap = RotatingEntityManager.getRotatingTileset("cow", 25);
//        } else if (entityType.equalsIgnoreCase("Girl")) {
//            imageMap = RotatingEntityManager.getRotatingTileset("girl", 20);
//        } else if (entityType.equalsIgnoreCase("Lion")) {
//            imageMap = RotatingEntityManager.getRotatingTileset("lion", 15);
//        } else if (entityType.equalsIgnoreCase("Susi")) {
//            imageMap = RotatingEntityManager.getRotatingTileset("susi", 20);
//        } else if (entityType.equalsIgnoreCase("Jake")) {
//            imageMap = RotatingEntityManager.getRotatingTileset("jake", 20);
//        } else if (entityType.equalsIgnoreCase("Steve")) {
//            imageMap = RotatingEntityManager.getRotatingTileset("steve", 20);
//        }
        update();
    }

    private void update() {

        //sprite.advance();

        if (updateFlag) {
            if(entity instanceof RotatingEntity) {
                updateImageBasedOnHeading();
            }
            centerFullBoundsOnPoint(entity.getX(), entity.getY());
            updateFlag = false;
        }

//
//        if (!isBlocked()) {
//            heading = computeAngle(heading);
//            // System.out.println("heading:" + heading);
//            // TODO: only do this if heading has changed
//            updateImageBasedOnHeading();
//            getAnimation().update();
//        }
    }

    /**
     * The method name says it all...
     */
    private void updateImageBasedOnHeading() {

        if(imageMap == null) {
            //TODO: Smelly!
            return;
        }

        // TODO: Also smelly
        // Exception if entity is not rotating?
        for (Map.Entry<Double, Image> entry : imageMap.entrySet()) {
            // System.out.println("" + ((RotatingEntity)entity).getHeading() + "-" + entry.getKey());
            if ( ((RotatingEntity)entity).getHeading() < entry.getKey()) {

                // TODO: For these events use the changeSupport old / new value thing so it only happens
                // on changes
                if (entry.getValue() != null) {
                    image.setImage(entry.getValue());
                }
                break;
            }
        }
    }

    public OdorWorldEntity getEntity() {
        return entity;
    }

    // PImage image1 = new PImage(OdorWorldResourceManager.getRotatingImage("mouse/Mouse_0.gif"));
    // PImage image2 = new PImage(OdorWorldResourceManager.getRotatingImage("mouse/Mouse_15.gif"));
    // List<Image> images = Arrays.asList(image1, image2);
    // image = image1;
    // animation = new LoopedFramesAnimation(images);
    // sprite = new Sprite(animation2);
    // this.addChild(sprite);


}
