package org.simbrain.plot;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JMenuItem;

import org.jfree.data.xy.XYSeries;
import org.simbrain.network.NetworkComponent;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class PlotComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Time series. */
    private final XYSeries series = new XYSeries("Time series");
    private int time = 0;
    private long lastUpdate;
    private static final long UPDATE_INTERVAL = 250;
    
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

    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        // TODO omit fields
        return xstream;
    }
    
    public static PlotComponent open(InputStream input, final String name, final String format) {
        return (PlotComponent) getXStream().fromXML(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        getXStream().toXML(output);
    }
    
    XYSeries getSeries() {
        return series;
    }
    
    public void setValue(double value) {
        long current = System.currentTimeMillis();
        boolean update = current - lastUpdate > UPDATE_INTERVAL;
        series.add(time++, value, update);
        if (update) lastUpdate = current;
    }

    @Override
    public boolean hasChangedSinceLastSave() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

//    void couple(ProducingAttribute<Double> attribute) {
//      Coupling<Double> coupling = new Coupling<Double>(attribute, variable);
//      getWorkspace().addCoupling(coupling);
//    }
    
    Variable getVariable() {
        return variable;
    }
    
    /**
     * {@inheritDoc}
     */
    public Collection<? extends Consumer> getConsumers() {
        return Collections.singleton(variable);
    }

    @Override
    public void update() {
        /* no implementation */
    }
}
