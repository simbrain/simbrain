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
import org.simbrain.util.piccolo.RotatingSprite;
import org.simbrain.util.piccolo.Sprite;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntityManager;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;
import org.simbrain.world.odorworld.sensors.VisualizableEntityAttribute;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

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
     * Flag to indicate this node's location has changed and should be updated at the next update
     */
    private boolean updateFlag;

    /**
     * Sprite representing this entity.
     */
    public Sprite sprite;

    /**
     * For advancing animation proportional to velocity.
     */
    private double frameCounter = 0;

    /**
     * A map from {@link VisualizableEntityAttribute} (model) to {@link EntityAttributeNode} (view).
     */
    private Map<VisualizableEntityAttribute, EntityAttributeNode> visualizableAttributeMap = new HashMap<>();

    /**
     * Construct an entity node with a back-ref to parent.
     *
     * @param world  parent world
     * @param entity represented entity
     */
    public EntityNode(final OdorWorld world, final OdorWorldEntity entity) {
        this.parent = world;
        this.entity = entity;

        updateImage();
        updateEntityAttributeModel();

        setOffset(entity.getX(), entity.getY());
        entity.addPropertyChangeListener(evt -> {
            if ("propertiesChanged".equals(evt.getPropertyName())) {
                updateImage();
                if (this.entity.isShowSensors()) {
                    visualizableAttributeMap.values().forEach(n -> n.setVisible(true));
                } else {
                    visualizableAttributeMap.values().forEach(n -> n.setVisible(false));
                }
            } else if ("deleted".equals(evt.getPropertyName())) {
                this.removeFromParent();
            } else if ("moved".equals(evt.getPropertyName())) {
                updateFlag = true;
            } else if ("updated".equals(evt.getPropertyName())) {
                update();
            } else if ("sensorAdded".equals(evt.getPropertyName()) || "effectorAdded".equals(evt.getPropertyName())) {
                if (evt.getNewValue() instanceof VisualizableEntityAttribute) {
                    VisualizableEntityAttribute toAdd = (VisualizableEntityAttribute) evt.getNewValue();
                    addAttribute(toAdd);
                }
            } else if ("sensorRemoved".equals(evt.getPropertyName())
                    || "effectorRemoved".equals(evt.getPropertyName())) {
                if (evt.getNewValue() instanceof VisualizableEntityAttribute) {
                    VisualizableEntityAttribute toRemove = (VisualizableEntityAttribute) evt.getNewValue();
                    removeAttribute(toRemove);
                }
            }
        });
    }

    /**
     * Add an {@link VisualizableEntityAttribute}.
     *
     * @param attribute the attribute to add
     */
    private void addAttribute(VisualizableEntityAttribute attribute) {
        visualizableAttributeMap.put(attribute, EntityAttributeNode.getNode(attribute));
        addChild(visualizableAttributeMap.get(attribute));
    }

    /**
     * Remove an {@link VisualizableEntityAttribute}
     *
     * @param attribute the attribute to remove
     */
    private void removeAttribute(VisualizableEntityAttribute attribute) {
        removeChild(visualizableAttributeMap.get(attribute));
        visualizableAttributeMap.remove(attribute);
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
     * Sync all visualizable entity attributes to this node.
     * Should only be called on initialization or deserialization
     */
    private void updateEntityAttributeModel() {

        List<VisualizableEntityAttribute> visualizableEntityAttributeList =
                entity.getSensors().stream()
                        .filter(VisualizableEntityAttribute.class::isInstance)
                        .map(VisualizableEntityAttribute.class::cast)
                        .collect(Collectors.toList());

        visualizableEntityAttributeList.addAll(
                entity.getEffectors().stream()
                        .filter(VisualizableEntityAttribute.class::isInstance)
                        .map(VisualizableEntityAttribute.class::cast)
                        .collect(Collectors.toList())
        );

        for (VisualizableEntityAttribute vp : visualizableEntityAttributeList) {
            EntityAttributeNode currentEntityAttributeNode;
            if (!visualizableAttributeMap.containsKey(vp)) {
                currentEntityAttributeNode = EntityAttributeNode.getNode(vp);
                addChild(currentEntityAttributeNode);
                visualizableAttributeMap.put(vp, currentEntityAttributeNode);
            } else {
                currentEntityAttributeNode = visualizableAttributeMap.get(vp);
            }
            currentEntityAttributeNode.update();
        }
    }

    /**
     * Update all visualizable attribute nodes.
     */
    private void updateAttributesNodes() {
        visualizableAttributeMap.values().forEach(EntityAttributeNode::update);
    }

    /**
     * Initialize the image associated with the object. Only called when
     * changing the image.
     */
    private void updateImage() {

        removeChild(sprite);

        switch (entity.getEntityType()) {
        case SWISS:
            sprite = new Sprite(OdorWorldResourceManager.getStaticImage(DEFAULT_IMAGE));
            break;
        case FLOWER:
            sprite = new Sprite(OdorWorldResourceManager.getStaticImage("Pansy.gif"));
            break;
        case CANDLE:
            sprite = new Sprite(OdorWorldResourceManager.getStaticImage("Candle.png"));
            break;
        case FISH:
            sprite = new Sprite(OdorWorldResourceManager.getStaticImage("Fish.gif"));
            break;
        case MOUSE:
            sprite = new RotatingSprite(RotatingEntityManager.getMouse());
            break;
        case AMY:
        case ARNO:
        case BOY:
        case COW:
        case GIRL:
        case JAKE:
        case LION:
        case STEVE:
        case SUSI:
            sprite = new RotatingSprite(RotatingEntityManager.getRotatingTileset(entity.getEntityType().name()));
            break;
        default:
            break;
        }

        addChild(sprite);
        visualizableAttributeMap.values().forEach(PNode::raiseToTop);
        updateAttributesNodes();
        if (entity.isRotating()) {
            ((RotatingSprite) sprite).updateHeading(entity.getHeading());
        }

    }

    private void update() {

        //TODO: Make sure this is only called once per workspace update

        if (updateFlag) {
            if(entity.isRotating()) {
                ((RotatingSprite) sprite).updateHeading(entity.getHeading());
            }
            setOffset(entity.getX(), entity.getY());
            updateAttributesNodes();
            // repaint(); // TODO: Not clear why this is needed. setOffset fires an event.
            updateFlag = false;
        }
    }

    /**
     * Advancing animation frame based on the velocity of the entity.
     */
    public void advance() {
        double dx;
        double dy;
        if (entity.isManualMode()) {
            dx = entity.getManualMovementVelocity().getX();
            dy = entity.getManualMovementVelocity().getY();
        } else {
            dx = entity.getVelocityX();
            dy = entity.getVelocityY();
        }
        frameCounter += Math.sqrt(dx * dx + dy * dy) / 5;
        int i = 0;
        for (; i < frameCounter; i++) {
            sprite.advance();
        }
        frameCounter -= i;
    }

    /**
     * Set the sprite to frame where the entity is standing still.
     */
    public void resetToStaticFrame() {
        sprite.resetToStaticFrame();
    }

    public OdorWorldEntity getEntity() {
        return entity;
    }


}
