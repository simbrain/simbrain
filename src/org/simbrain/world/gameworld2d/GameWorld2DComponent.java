package org.simbrain.world.gameworld2d;

import java.awt.Dimension;
import java.io.File;

import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

public class GameWorld2DComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Reference to the wrapped game world object. */
    private GameWorld2D world;
    
    /**
     * Construct a new world panel.  Set up the toolbars.  Create an  instance of a world object.
     * @param ws the workspace associated with this frame
     */
    public GameWorld2DComponent(String name) {
        super(name);
        
        world = new GameWorld2D();
        world.initEngineApplet(30,30,10,10,null,null,null);
        world.setPreferredSize(new Dimension(450,400));
    }

    /**
     * @return Returns the world.
     */
    public GameWorld2D getWorld() {
        return world;
    }

//    /**
//     * @param world The world to set.
//     */
//    public void setWorld(GameWorld2D world) {
//        this.world = world;
//    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void save(File saveFile) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void open(File openFile) {
        // TODO Auto-generated method stub
    }

    @Override
    public void update() {
        
    }

//
//    public CouplingContainer getCouplingContainer() {
//        return this;
//    }


}
