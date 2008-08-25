package org.simbrain.plot;

import java.lang.reflect.Type;
import java.util.ArrayList;

import org.jfree.data.xy.XYSeries;
import org.simbrain.workspace.SingleAttributeConsumer;

/**
 * Represents one source of data in a JFreeChart plot.
 * Currently tied to XYSeries.
 */
public class DataSource extends SingleAttributeConsumer<Double> {

    /** Reference to gauge. */
    private PlotComponent plot;
    
    /** XY Series. */
    private final XYSeries xySeries;
    
    /** Name .*/
    private final String name;
    
    /**
     * @param columnNumber
     */
    public DataSource(final PlotComponent plot, String name) {
        this.plot = plot;
        xySeries= new XYSeries(name);
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Double value) {
        //System.out.println("set value");
        xySeries.add(plot.getWorkspace().getTime(), value.doubleValue());
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "DataSeries " + name;
    }
    
    public Type getType() {
        return Double.TYPE;
    }

    public PlotComponent getParentComponent() {
        return plot;
    }

    public String getAttributeDescription() {
        return getDescription();
    }

    public XYSeries getXySeries() {
        return xySeries;
    }
}
