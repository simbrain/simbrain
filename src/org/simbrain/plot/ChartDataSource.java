package org.simbrain.plot;

import org.simbrain.workspace.Consumable;

/**
 * Helper which encapsulates a source of data for charts.
 */
public interface ChartDataSource {

    /** Get a description of the data source. */
    String getDescription();

    /** Set the value of the data source. The behavior of this method will depend on the type of data source. */
    @Consumable(idMethod="getDescription")
    void setValue(double value);

}
