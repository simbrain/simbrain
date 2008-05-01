package org.simbrain.world.threedee;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.simbrain.workspace.Attribute;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.world.threedee.environment.Environment;
import org.simbrain.world.threedee.environment.Terrain;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * A representing a 3D world in a workspace.
 * 
 * @author Matt Watson
 */
public class ThreeDeeComponent extends WorkspaceComponent<WorkspaceComponentListener> {
    private final ThreeDeeModel model;
    
    /**
     * Creates a new ThreeDeeComponent with the given name.
     * 
     * @param name The name of the component.
     */
    public ThreeDeeComponent(final String name) {
        super(name);
        this.model = new ThreeDeeModel();
    }
    
    private ThreeDeeComponent(final String name, final ThreeDeeModel model) {
        super(name);
        this.model = model;
    }
    
    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        
        xstream.registerConverter(new Terrain.TerrainConverter());
        
        xstream.omitField(Environment.class, "parents");
        xstream.omitField(Environment.class, "timer");
        xstream.omitField(Environment.class, "random");
        xstream.omitField(Agent.class, "logger");
        xstream.omitField(Moveable.class, "inputs");
        xstream.omitField(Bindings.class, "component");
        xstream.omitField(MultipleViewElement.class, "spatials");
        
        return xstream;
    }
    
    /**
     * Recreates an instance of this class from a saved component.
     * 
     * @param input The input stream to read from.
     * @param name The name of the workspace component.
     * @param format The format of the input.
     * @return The ThreeDeeComponent created.
     */
    public static ThreeDeeComponent open(final InputStream input,
            final String name, final String format) {
        ThreeDeeComponent component = new ThreeDeeComponent(
            name, (ThreeDeeModel) getXStream().fromXML(input));
        
        for (Bindings bindings : component.model.bindings) {
            bindings.setComponent(component);
        }
        
        return component;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        getXStream().toXML(model, output);
    }
    
    public Collection<Agent> getAgents() {
        return model.agents;
    }
    
    @Override
    public Attribute getAttributeForKey(final String key) {
        String[] keyParts = key.split(":");
        
        for (Bindings b : model.bindings) {
            if (b.getDescription().equals(keyParts[0])) {
                List<Attribute> attributes = new ArrayList<Attribute>();
                
                attributes.addAll(b.getConsumingAttributes());
                attributes.addAll(b.getProducingAttributes());
                
                for (Attribute a : attributes) {
                    if (a.getAttributeDescription().equals(keyParts[1])) {
                        return a;
                    }
                }
                
                return null;
            }
        }
        
        return null;
    }

    @Override
    public String getKeyForAttribute(final Attribute attribute) {
        return attribute.getParent().getDescription()
            + ":" + attribute.getAttributeDescription();
    }
    
    /**
     * Returns the environment for this Component.
     * 
     * @return The Environment for this Component.
     */
    public Environment getEnvironment() {
        return model.environment;
    }
    
    /**
     * Creates a new Agent and adds it to the Environment.
     * 
     * @return The newly created Agent.
     */
    public Agent createAgent() {
        Agent agent = new Agent("" + model.agents.size() + 1, this);
        model.agents.add(agent);
        model.bindings.add(agent.getBindings());//new Bindings(agent, this));
        model.environment.add(agent);
        
        return agent;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Producer> getProducers() {
        return Collections.unmodifiableCollection(model.bindings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Consumer> getConsumers() {
        return Collections.unmodifiableCollection(model.bindings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // TODO Auto-generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void update() {
        for (Bindings bind : model.bindings) {
            bind.setOn(true);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void stopped() {
        for (Bindings bind : model.bindings) {
            bind.setOn(false);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void couplingRemoved(final Coupling<?> coupling) {
        ConsumingAttribute<Double> consumingAttribute
            = (ConsumingAttribute<Double>) coupling.getConsumingAttribute();
        
        if (model.bindings.contains(consumingAttribute.getParent())) {
            consumingAttribute.setValue(0d);
        }
    }
}
