package org.simbrain.world.game;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * A consumer and producer that delegates to a game model.
 * 
 * @author Matt Watson
 */
public class GameAttributes implements Producer, Consumer {

    /** The parent component. */
    private final WorkspaceComponent parent;
    /** The game model. */
    private final GameModel model;
    
    /**
     * Creates a new instance with the given parent and model.
     * 
     * @param parent the parent component.
     * @param model the game model.
     */
    public GameAttributes(final WorkspaceComponent parent, final GameModel model) {
        this.parent = parent;
        this.model = model;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return model.getName();
    }
    
    /**
     * {@inheritDoc}
     */
    public WorkspaceComponent getParentComponent() {
        return parent;
    }
    
    /**
     * {@inheritDoc}
     */
    public List<ConsumingAttribute<Double>> getConsumingAttributes() {
        List<ConsumingAttribute<Double>> attributes = new ArrayList<ConsumingAttribute<Double>>();
        
        for (int i = 0; i < model.size(); i++) {
            for (int j = 0; j < model.size(); j++) {
                final int x = i;
                final int y = j;
                
                attributes.add(new ConsumingAttribute<Double>() {

                    public Consumer getParent() {
                        return GameAttributes.this;
                    }

                    public void setValue(final Double value) {
                        model.set(x, y, value);
                    }

                    public String getAttributeDescription() {
                        return model.getName() + " at " + x + "," + y;
                    }

                    public String getKey() {
                        return x + "," + y;
                    }
                });
            }
        }
        
        return attributes;
    }

    public List<? extends ProducingAttribute<?>> getProducingAttributes() {
        // TODO Auto-generated method stub
        return null;
    }
}
