package org.simbrain.world.threedee;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.world.threedee.environment.Environment;

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
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void open(final File openFile) {
        // TODO Auto-generated method stub
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
    public void save(final File saveFile) {
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
}
