package org.simbrain.world.threedee;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.world.threedee.environment.Environment;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * A representing a 3D world in a workspace.
 * 
 * @author Matt Watson
 */
public class ThreeDeeComponent extends WorkspaceComponent<WorkspaceComponentListener> {
    /** The environment for the 3D world. */
    private Environment environment = new Environment();
    /** The set of agents in the environment. */
    private Set<Agent> agents = new HashSet<Agent>();
    
    /**
     * The bindings that allow agents to be be wrapped as producers
     * and consumers.
     */
    private List<Bindings> bindings = new ArrayList<Bindings>();
    
    /**
     * Creates a new ThreeDeeComponent with the given name.
     * 
     * @param name The name of the component.
     */
    public ThreeDeeComponent(final String name) {
        super(name);
    }
    
    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        // omit fields
        return xstream;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ThreeDeeComponent open(final InputStream input) {
        return (ThreeDeeComponent) getXStream().fromXML(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output) {
        getXStream().toXML(output);
    }
    
    /**
     * Returns the environment for this Component.
     * 
     * @return The Environment for this Component.
     */
    public Environment getEnvironment() {
        return environment;
    }
    
    /**
     * Creates a new Agent and adds it to the Environment.
     * 
     * @return The newly created Agent.
     */
    public Agent createAgent() {
        Agent agent = new Agent("" + agents.size());
        agents.add(agent);
        bindings.add(new Bindings(agent, this));
        environment.add(agent);
        
        return agent;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Producer> getProducers() {
        return Collections.unmodifiableCollection(bindings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Consumer> getConsumers() {
        return Collections.unmodifiableCollection(bindings);
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
        for (Bindings bind : bindings) {
            bind.setOn(true);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void stopped() {
        for (Bindings bind : bindings) {
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
        
        System.out.println("removed: " + consumingAttribute.getParent());
        
        
        if (bindings.contains(consumingAttribute.getParent())) {
            System.out.println("found");
            consumingAttribute.setValue(0d);
        }
    }
}
