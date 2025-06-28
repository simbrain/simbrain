package org.simbrain.plot.barchart;

import com.thoughtworks.xstream.XStream;
import org.jfree.data.category.DefaultCategoryDataset;
import org.simbrain.util.UserParameter;
import org.simbrain.util.XStreamUtils;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Data for a JFreeChart bar chart.
 */
public class BarChartModel implements AttributeContainer, EditableObject {

    /**
     * JFreeChart dataset for bar charts.
     */
    private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    /**
     * Color of bars in barchart.
     */
    @UserParameter(label = "Bar Color", order = 4)
    private Color barColor = Color.red;

    /**
     * Auto range bar chart.
     */
    @UserParameter(label = "Auto Range", order = 3)
    private boolean autoRange = true;

    /**
     * Maximum range.
     */
    @UserParameter(label = "Upper Bound", order = 2)
    private double upperBound = 10;

    /**
     * Minimum range.
     */
    @UserParameter(label = "Lower Bound", order = 1)
    private double lowerBound = 0;

    /**
     * Names for the bars in the barchart.  Set via coupling events in
     * {@link BarChartComponent}.
     */
    private String[] barNames = {};

    /**
     * Track how many bars there are.  If an array with a different number of
     * components is sent to this component, numBars is updated.
     */
    private int numBars = 0;

    /**
     * Bar chart model constructor.
     */
    public BarChartModel() {
    }

    /**
     * Return JFreeChart category dataset.
     *
     * @return dataset
     */
    public DefaultCategoryDataset getDataset() {
        return dataset;
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = XStreamUtils.getSimbrainXStream();
        return xstream;
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    private Object readResolve() {
        return this;
    }

    public Color getBarColor() {
        return barColor;
    }

    public void setBarColor(final Color barColor) {
        this.barColor = barColor;
    }

    public boolean isAutoRange() {
        return autoRange;
    }

    public void setAutoRange(final boolean autoRange) {
        this.autoRange = autoRange;
    }

    public double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(final double upperBound) {
        this.upperBound = upperBound;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(final double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public void setRange(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * Called by coupling producers via reflection.
     */
    @Consumable()
    public void setBarValues(double[] newPoint) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                // Take care of size mismatches
                if (newPoint.length != numBars) {
                    dataset.clear();
                    numBars = newPoint.length;
                }

                // Write the data
                for (int i = 0; i < newPoint.length; i++) {
                    if (i < barNames.length) {
                        dataset.setValue((Number) newPoint[i], 1, barNames[i]);
                    } else {
                        // TODO: May need to go to this condition for if barNames is empty
                        dataset.setValue((Number) newPoint[i], 1, "" + (i + 1));
                    }
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void setBarNames(String[] names) {
        this.barNames = names;
    }

    @Override
    public String getName() {
        return "Bar chart";
    }

    @Override
    public String getId() {
        return "Bar Chart";
    }
}
