package org.simbrain.plot;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JMenuItem;

import org.jfree.data.xy.XYSeries;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

public class PlotComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Time series. */
    private final XYSeries series = new XYSeries("Time series");
    private int time = 0;

    /** Consumer list. */
//    private ArrayList<Consumer> consumers= new ArrayList<Consumer>();
    private final Variable variable;
    
    /** Coupling menu item. Must be reset every time.  */
    JMenuItem couplingMenuItem;

    /**
     * Construct a new world panel.  Set up the toolbars.  Create an  instance of a world object.
     * @param ws the workspace associated with this frame
     */
    public PlotComponent(String name) {
        super(name);
        variable = new Variable(this);
    }

    XYSeries getSeries() {
        return series;
    }
    
    public void setValue(double value) {
        series.add(time++, value);
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

    void couple(ProducingAttribute<Double> attribute) {
      Coupling<Double> coupling = new Coupling<Double>(attribute, variable);
      getWorkspace().addCoupling(coupling);
    }
    
    /**
     * {@inheritDoc}
     */
    public Collection<? extends Consumer> getConsumers() {
        return Collections.singleton(variable);
    }

    @Override
    public void open(File openFile) {
        // TODO Auto-generated method stub
    }

    @Override
    public void update() {
        /* no implementation */
    }
}
