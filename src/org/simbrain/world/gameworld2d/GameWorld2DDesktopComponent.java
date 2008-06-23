package org.simbrain.world.gameworld2d;

import java.awt.BorderLayout;
import java.io.File;

import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.GenericFrame;

public class GameWorld2DDesktopComponent extends GuiComponent<GameWorld2DComponent> {

    private static final long serialVersionUID = 1L;
    
    private final GameWorld2D world;
    
    /**
     * Construct a new world panel.  Set up the toolbars.  Create an  instance of a world object.
     * @param ws the workspace associated with this frame
     */
    public GameWorld2DDesktopComponent(GenericFrame frame, GameWorld2DComponent component) {
        super(frame, component);
        this.setLayout(new BorderLayout());
        this.world = component.getWorld();
        add("Center", world);
        component.addListener(new BasicComponentListener());
    }

    @Override
    public void postAddInit() {
        world.init();
    }

    /**
     * @return Returns the world.
     */
    public GameWorld2D getWorld() {
        return world;
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }

    @Override
    public void update() {
        world.stop();
        world.player.snapToGrid();
        world.start();
    }
}
