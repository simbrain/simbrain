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
        return entity.getName();
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
    
    //TODO: x, y, dx, dy


}
