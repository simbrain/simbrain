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

public class ThreeDeeComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    private Environment environment = new Environment();
    private Set<Agent> agents = new HashSet<Agent>();
    private List<Bindings> bindings = new ArrayList<Bindings>();
    
    public ThreeDeeComponent(String name) {
        super(name);
    }
    
    public Environment getEnvironment() {
        return environment;
    }
    
    public Agent createAgent() {
        Agent agent = new Agent("" + agents.size());
        agents.add(agent);
        bindings.add(new Bindings(agent, this));
        environment.add(agent);
        
        return agent;
    }
    
    @Override
    public Collection<? extends Producer> getProducers() {
        return Collections.unmodifiableCollection(bindings);
    }
    
    @Override
    public Collection<? extends Consumer> getConsumers() {
        return Collections.unmodifiableCollection(bindings);
    }
    
    @Override
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void open(File openFile) {
        // TODO Auto-generated method stub
    }
    
    @Override
    public void close() {
        // TODO Auto-generated method stub
    }
    
    @Override
    public void save(File saveFile) {
        // TODO Auto-generated method stub
    }
    
    @Override
    protected void update() {
        // TODO Auto-generated method stub
    }
}
