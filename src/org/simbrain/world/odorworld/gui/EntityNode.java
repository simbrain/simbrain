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
import org.simbrain.util.piccolo.Animation;
import org.simbrain.util.piccolo.LoopedFramesAnimation;
import org.simbrain.util.piccolo.SingleFrameAnimation;
import org.simbrain.util.piccolo.Sprite;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.entities.RotatingEntityManager;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;

import java.awt.geom.Point2D;
import java.util.*;

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

    public ArrayList<Animation> animations;
    public Animation currentAnimation;
    public final LoopedFramesAnimation animation2 = null;
    // Old solution. Not sure whether to keep it..
//    private TreeMap<Double, Image> imageMap = null;
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

        removeChild(sprite);

        switch (entity.getEntityType()) {
        case SWISS:
            this.currentAnimation = new SingleFrameAnimation(OdorWorldResourceManager.getStaticImage(DEFAULT_IMAGE));
            sprite = new Sprite(currentAnimation);
            break;
        case FLOWER:
            this.currentAnimation = new SingleFrameAnimation(OdorWorldResourceManager.getStaticImage("pansy.gif"));
            sprite = new Sprite(currentAnimation);
            break;
        case MOUSE:
            this.animations = RotatingEntityManager.getMouse();
            updateImageBasedOnHeading();
            sprite = new Sprite(currentAnimation);
            break;
        case COW:
            this.animations = RotatingEntityManager.getRotatingTileset("cow");
            updateImageBasedOnHeading();
            sprite = new Sprite(currentAnimation);
            break;
        default:
            break;
        }


        addChild(sprite);


    }


    private void update() {

        //TODO: Make sure this is only called once per workspace update

        //sprite.advance();

        if (updateFlag) {
            if(entity instanceof RotatingEntity) {
                updateImageBasedOnHeading();
            }
            setOffset(entity.getX(), entity.getY());
            repaint(); // TODO: Not clear why this is needed. setOffset fires an event.
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

        if(animations == null) {
            //TODO: Smelly!
            return;
        }

        currentAnimation = RotatingEntityManager.getAnimationByHeading(
                animations,
                ((RotatingEntity)entity).getHeading()
        );
    }

    public void advance() {
        currentAnimation.advance();
    }

    public void resetToStaticFrame() {
        currentAnimation.resetToStaticFrame();
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
