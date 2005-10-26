package org.simbrain.world.visionworld;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JInternalFrame;

import org.simbrain.workspace.Workspace;

// Need javadoc
public class VisionWorldFrame extends JInternalFrame implements ComponentListener {

    private VisionWorld world;
//    private static final String FS = "/"; //System.getProperty("file.separator");Separator();
//    private File current_file = null;
//    private String currentDirectory = VisionWorldPreferences.getCurrentDirectory();
    private Workspace workspace;

    // For workspace persistence
    private String path;
    private int xpos;
    private int ypos;
    private int the_width;
    private int the_height;


    //Loader methods for visionworld
    public VisionWorldFrame(Workspace ws) {
        this.workspace = ws;

        world = new VisionWorld(this);

        this.setResizable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setClosable(true);
        this.addComponentListener(this);
        this.getContentPane().add(world);

        this.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);

        setVisible(true);

    }

    public VisionWorld getWorld() {
        return world;
    }

    public void componentResized(ComponentEvent arg0) {
        world.rebuild();
        pack();
    }

    public void componentMoved(ComponentEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void componentShown(ComponentEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void componentHidden(final ComponentEvent e) {
        // TODO Auto-generated method stub

    }







    //filehandling methods

    //menu handling


}
