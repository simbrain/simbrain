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
package org.simbrain.world.odorworld.attributes;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

public class EntityWrapper implements Consumer, Producer {

    /** Parent component. */
    WorkspaceComponent parent;
    
    /** Parent entity. */
    OdorWorldEntity entity;
    
    /** The producing attributes. */
    private ArrayList<ProducingAttribute<?>> producingAttributes
        = new ArrayList<ProducingAttribute<?>>();

    /** The consuming attributes. */
    private ArrayList<ConsumingAttribute<?>> consumingAttributes
        = new ArrayList<ConsumingAttribute<?>>();

    public EntityWrapper(WorkspaceComponent parent, OdorWorldEntity entity) {
        super();
        this.parent = parent;
        this.entity = entity;

        XPositionAttribute xPositionAttribute = new XPositionAttribute();
        producingAttributes.add(xPositionAttribute);
        consumingAttributes.add(xPositionAttribute);

        YPositionAttribute yPositionAttribute = new YPositionAttribute();
        producingAttributes.add(yPositionAttribute);
        consumingAttributes.add(yPositionAttribute);
    }

    /**
     * {@inheritDoc}
     */
    public final List<? extends ProducingAttribute<?>> getProducingAttributes() {
        return producingAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public final List<? extends ConsumingAttribute<?>> getConsumingAttributes() {
        return consumingAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return entity.getName() + "-Location-";
    }

    /**
     * {@inheritDoc}
     */
    public WorkspaceComponent getParentComponent() {
        return parent;
    }
    
    /**
     * Attribute for X Position.
     */
    private class XPositionAttribute extends AbstractAttribute 
            implements ProducingAttribute<Double>, ConsumingAttribute<Double> {
        
        /**
         * {@inheritDoc}
         */
        public String getKey() {
            return "X";
        }
        
        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return Double.valueOf(entity.getX());
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            entity.setX(value.floatValue());
        }
        
        /**
         * {@inheritDoc}
         */
        public EntityWrapper getParent() {
            return EntityWrapper.this;
        }
    }
    
    /**
     * Attribute for X Position.
     */
    private class YPositionAttribute extends AbstractAttribute 
            implements ProducingAttribute<Double>, ConsumingAttribute<Double> {
        
        /**
         * {@inheritDoc}
         */
        public String getKey() {
            return "Y";
        }
        
        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return Double.valueOf(entity.getY());
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            entity.setY(value.floatValue());
        }
        
        /**
         * {@inheritDoc}
         */
        public EntityWrapper getParent() {
            return EntityWrapper.this;
        }
        
    }
    
    
    //TODO: x, y, dx, dy


}
