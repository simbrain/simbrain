package org.simbrain.world.threedee;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

public class ThreeDeeComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    private Environment environment = new Environment();
    private Set<Agent> agents = new HashSet<Agent>();
    
    public ThreeDeeComponent(String name) {
        super(name);
    }
    
    public Environment getEnvironment() {
        return environment;
    }
    
    public Agent createAgent() {
        Agent agent = new Agent("" + agents.size());
        agents.add(agent);
        environment.add(agent);
        
        return agent;
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
