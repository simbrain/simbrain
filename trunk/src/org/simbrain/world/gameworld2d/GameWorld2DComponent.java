package org.simbrain.world.gameworld2d;

import java.awt.Dimension;
import java.io.InputStream;
import java.io.OutputStream;

import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.world.dataworld.DataWorldComponent;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

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
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        // TODO omit fields
        return xstream;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DataWorldComponent open(final InputStream input) {
        return (DataWorldComponent) getXStream().fromXML(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output) {
        getXStream().toXML(output);
    }

    /**
     * @return Returns the world.
     */
    public GameWorld2D getWorld() {
        return world;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    @Override
    public void update() {
        
    }
}
