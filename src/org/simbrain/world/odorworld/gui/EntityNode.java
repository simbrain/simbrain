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
import org.piccolo2d.nodes.PPath;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.piccolo.RotatingSprite;
import org.simbrain.util.piccolo.Sprite;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntityManager;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;
import org.simbrain.world.odorworld.sensors.VisualizableSensor;

// import java.awt.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private Map<VisualizableSensor, SensorNode> visualizableSensorMap = new HashMap<>();

    private final static int SENSOR_DIAMATER = 6;

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
        syncViewWithModel();

        setOffset(entity.getX(), entity.getY());
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

    private void syncViewWithModel() {
        updateSmellSensorModel();
    }

    private void updateSmellSensorModel() {


        List<VisualizableSensor> visualizableSensorList =
                entity.getSensors().stream()
                        .filter(VisualizableSensor.class::isInstance)
                        .map(VisualizableSensor.class::cast)
                        .collect(Collectors.toList());

        // TODO: let event handler handle removal.
        for (VisualizableSensor vs : visualizableSensorMap.keySet()) {
            if (!visualizableSensorList.contains(vs)) {
                removeChild(visualizableSensorMap.get(vs));
                visualizableSensorMap.remove(vs);
            }
        }

        for (VisualizableSensor vs : visualizableSensorList) {
            SensorNode currentSensorNode;
            if (!visualizableSensorMap.containsKey(vs)) {
                currentSensorNode = vs.getNode();
                addChild(currentSensorNode);
                visualizableSensorMap.put(vs, currentSensorNode);
            } else {
                currentSensorNode = visualizableSensorMap.get(vs);
            }
            currentSensorNode.update();
        }
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
        visualizableSensorMap.values().forEach(i -> i.raiseToTop());
        if(entity.isRotating()) {
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
            syncViewWithModel();
            repaint(); // TODO: Not clear why this is needed. setOffset fires an event.
            updateFlag = false;
        }
    }

    public void advance() {
        double dx = entity.getVelocityX();
        double dy = entity.getVelocityY();
        frameCounter += Math.sqrt(dx * dx + dy * dy);
        int i = 0;
        for (; i < frameCounter; i++) {
            sprite.advance();
        }
        frameCounter -= i;
    }

    public void resetToStaticFrame() {
        sprite.resetToStaticFrame();
    }

    public OdorWorldEntity getEntity() {
        return entity;
    }


}
