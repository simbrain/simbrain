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
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
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


    public Sprite sprite;

    public final LoopedFramesAnimation animation2 = null;

    private TreeMap<Double, LoopedFramesAnimation> imageMap = null;


    // TODO: Replace image with imagesource.

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

        setImage();

        this.centerFullBoundsOnPoint(entity.getX(), entity.getY());

        entity.addPropertyChangeListener(evt -> {
            if ("propertiesChanged".equals(evt.getPropertyName())) {
                setImage();
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

    private void setImage() {

        this.removeChild(image);

        switch (entity.getEntityType()) {
        case SWISS:
            this.image = new PImage(OdorWorldResourceManager.getStaticImage(DEFAULT_IMAGE));
            break;
        case FLOWER:
            this.image = new PImage(OdorWorldResourceManager.getStaticImage("pansy.gif"));
            break;
        default:
            break;
        }

        this.addChild(image);

    }

    public OdorWorldEntity getEntity() {
        return entity;
    }

    public void initRotating() {

        Image image1 = OdorWorldResourceManager.getRotatingImage("mouse/Mouse_0.gif");
        Image image2 = OdorWorldResourceManager.getRotatingImage("mouse/Mouse_15.gif");
        List<Image> images = Arrays.asList(image1, image2);

//        animation = new LoopedFramesAnimation(images);
//        sprite = new Sprite(animation2);

        // TODO: get actual image bounds?
//        sprite.setBounds(x, y, 40, 40);
//        this.setPaint(Color.green);
//        this.addChild(sprite);


//        initTreeMap();
//        this.setAnimation(imageMap.get(imageMap.firstKey()));
    }


    /**
     * Initialize the tree map, which associates angles with images /
     * animations.
     */
    private void initTreeMap() {
//        if (entityType == null) {
//            entityType = "Mouse";
//        }
//        if (entityType.equalsIgnoreCase("Circle")) {
//            imageMap = RotatingEntityManager.getCircle();
//        }
//        if (entityType.equalsIgnoreCase("Mouse")) {
//            imageMap = RotatingEntityManager.getMouse();
//        } else if (entityType.equalsIgnoreCase("Amy")) {
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
//        for (Entry<Double, Animation> entry : imageMap.entrySet()) {
//            // System.out.println("" + heading + "-" + entry.getKey());
//            if (heading < entry.getKey()) {
////                setAnimation(entry.getValue());
//                break;
//            }
//        }
    }

}
