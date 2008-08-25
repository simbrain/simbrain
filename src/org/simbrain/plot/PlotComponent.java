package org.simbrain.plot;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JMenuItem;

import org.jfree.data.xy.XYSeries;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class PlotComponent extends WorkspaceComponent<WorkspaceComponentListener> {

    /** Consumer list. */
    private ArrayList<DataSource> consumers= new ArrayList<DataSource>();
    
    /** Coupling menu item. Must be reset every time.  */
    //JMenuItem couplingMenuItem;

    /**
     * Construct a new world panel.  Set up the toolbars.  Create an  instance of a world object.
     * @param ws the workspace associated with this frame
     */
    public PlotComponent(String name) {
        super(name);
        defaultInit();
    }
    
    /**
     * Initializes a jfreechart with specific number of data sources.
     *
     * @param name name of component
     * @param numDataSources number of data sources to initialize plot with
     */
    public PlotComponent(final String name, final int numDataSources) {
        super(name);
        addDataSources(numDataSources);
    }
    
    /**
     * Default initialization.
     */
    private void defaultInit() {
        addDataSources(3);
    }
    
    /**
     * Create specified number of set of data sources.
     * Adds these two existing data sources.
     *
     * @param numDataSources number of data sources to initialize plot with
     */
    public void addDataSources(final int numDataSources) {
        int currentSize = consumers.size() + 1;
        for (int i = 0; i < numDataSources; i++) {
            DataSource newAttribute = new DataSource(this, "" + (currentSize + i));
            consumers.add(newAttribute);
        }
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

    
//    /**
//     * Sets the values.
//     * TODO: Check.
//     * 
//     * @param seriesIndex index of consumer
//     * @param value value to set
//     */
//    public void setValue(int seriesIndex, double value) {
//        long current = System.currentTimeMillis();
//        boolean update = current - lastUpdate > UPDATE_INTERVAL;
//        seriesList.get(seriesIndex).add(this.getWorkspace().getTime(), value, update);
//        if (update) lastUpdate = current;
//    }

    @Override
    public boolean hasChangedSinceLastSave() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    public Collection<DataSource> getConsumers() {
        return consumers;
    }

    @Override
    public void update() {
        /* no implementation */
    }
}
