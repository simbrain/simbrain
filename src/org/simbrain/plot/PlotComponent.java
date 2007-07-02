package org.simbrain.plot;

import java.awt.BorderLayout;
import java.io.File;
import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

import ptolemy.plot.Plot;

public class PlotComponent extends WorkspaceComponent   {

    /** Reference to the plotter which is being wrapped. */
    private Plot plot;

    /**
     * Construct a new world panel.  Set up the toolbars.  Create an  instance of a world object.
     * @param ws the workspace associated with this frame
     */
    public PlotComponent() {
        super();
        init();
    }

    /**
     * Initializes frame.
     */
    public void init() {
        getContentPane().setLayout(new BorderLayout());
        plot = new Plot();
        plot.samplePlot();
        getContentPane().add("Center", plot);
    }

    @Override
    public int getDefaultHeight() {
        return 200;
    }

    @Override
    public int getDefaultLocationX() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getDefaultLocationY() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getDefaultWidth() {
        return 400;
    }

    @Override
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isChangedSinceLastSave() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void save(File saveFile) {
        // TODO Auto-generated method stub
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    public List<Consumer> getConsumers() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Coupling> getCouplings() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Producer> getProducers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void open(File openFile) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getWindowIndex() {
        // TODO Auto-generated method stub
        return 0;
    }

}
