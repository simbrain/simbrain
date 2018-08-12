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
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;

import java.awt.geom.Point2D;

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
     * Construct an entity node with a back-ref to parent.
     *
     * @param world parent world
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
            }
        });
    }

    @Override
    public void offset(double dx, double dy) {
        super.offset(dx, dy);
        // TODO: Don't do this during a drag
        pushViewPositionToModel();
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
            break; // TODO: Check
        }

        this.addChild(image);

    }

    public OdorWorldEntity getEntity() {
        return entity;
    }

}
