package org.simbrain.plot;

import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;

//TODO: Consider getting rid of this in favor of vector couplings only
/**
 * Helper which encapsulates a source of data for charts.
 */
public interface ChartDataSource extends AttributeContainer {

    /**
     * Get a description of the data source.
     */
    String getDescription();

    /**
     * Set the value of the data source. The behavior of this method will depend on the type of data source.
     */
    @Consumable(idMethod = "getDescription")
    void setValue(double value);

}
